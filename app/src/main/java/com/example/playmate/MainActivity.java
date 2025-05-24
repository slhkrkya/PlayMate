package com.example.playmate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.playmate.data.model.User;
import com.example.playmate.data.model.ChatMessage;
import com.example.playmate.data.repository.ChatRepository;
import com.example.playmate.service.MyFirebaseMessagingService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.bumptech.glide.Glide;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration appBarConfiguration;
    private DrawerLayout drawerLayout;
    private NavController navController;
    private NavigationView navigationView;

    // Cache for last loaded profile image and username
    private String lastProfileImageBase64 = null;
    private String lastUsername = null;

    // Yeni mesajlar için BroadcastReceiver
    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Yeni mesaj broadcast alındı");
            // Broadcast'ten mesaj detaylarını al
            String chatRoomId = intent.getStringExtra("chatRoomId");
            String senderId = intent.getStringExtra("senderId");
            String messageBody = intent.getStringExtra("body");
            String messageId = intent.getStringExtra("messageId");
            String title = intent.getStringExtra("title");

            if (chatRoomId != null && senderId != null && messageBody != null && messageId != null) {
                Log.d(TAG, "Broadcast ile gelen mesaj detayları: ChatRoomId=" + chatRoomId + 
                          ", SenderId=" + senderId + ", MessageId=" + messageId);

                // Mesajın okunup okunmadığını kontrol et
                FirebaseDatabase.getInstance().getReference("messages")
                        .child(chatRoomId)
                        .child(messageId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            ChatMessage message = snapshot.getValue(ChatMessage.class);
                            if (message != null && !message.isRead()) {
                                // Mesaj okunmamışsa bildirimi göster
                                Log.d(TAG, "Mesaj okunmamış, bildirim gösteriliyor: " + messageId);
                                showSingleNotification(chatRoomId, senderId, messageBody, messageId, title);
                            } else {
                                Log.d(TAG, "Mesaj zaten okunmuş veya bulunamadı, bildirim gösterilmiyor: " + messageId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Mesaj durumu kontrol edilirken hata: " + e.getMessage());
                            // Hata durumunda bildirimi gösterme
                            Log.d(TAG, "Hata nedeniyle bildirim gösterilmiyor");
                        });
            } else {
                Log.e(TAG, "Broadcast ile eksik mesaj bilgisi alındı.");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar'ı bağla ve varsayılan action bar olarak ayarla
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Bildirim izinlerini kontrol et ve logla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Bildirim izni durumu: " + (hasPermission ? "Verildi" : "Verilmedi"));
            
            if (!hasPermission) {
                Log.d(TAG, "Bildirim izni isteniyor...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        } else {
            Log.d(TAG, "Android 13'ten düşük sürüm, bildirim izni gerekmiyor");
        }

        // FCM token durumunu kontrol et
        // SharedPreferences ile "Beni Hatırla" özelliğini kontrol et
        SharedPreferences loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        boolean rememberMe = loginPrefs.getBoolean("remember_me", false);

        // Eğer kullanıcı "beni hatırla" seçmemişse, oturumu sonlandır
        if (!rememberMe) {
            FirebaseAuth.getInstance().signOut();

            // Google hesabı varsa onu da çıkış yaptır
            GoogleSignInClient googleClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            );
            googleClient.signOut();
        }
        // Android 13+ için bildirim izni iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }


        // NavigationController'ı oluştur
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Kullanıcı giriş yapmamışsa login ekranına yönlendir
        if (user == null) {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();
            navController.navigate(R.id.loginFragment, null, navOptions);
        } else {
            // Kullanıcı giriş yapmışsa ve bildirimden geliyorsa chat ekranına yönlendir
             handleNotificationNavigation(getIntent());
             // Kullanıcı giriş yaptıysa navigation header'ı güncelle
             updateNavigationHeader();
        }

        // AppBar ve NavigationDrawer'ı kontrolcüye bağla
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.nav_friends,
                R.id.friendRequestsFragment,
                R.id.nav_profile,
                R.id.editProfileFragment
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Navigation menüsündeki tıklamaları kontrol et
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Kullanıcı çıkış yaparsa tüm oturumları kapat ve SharedPreferences'ı temizle
            if (itemId == R.id.nav_logout) {
                // Önce dinleyicileri temizle
                cleanupOnLogout();
                
                FirebaseAuth.getInstance().signOut();

                GoogleSignInClient googleClient = GoogleSignIn.getClient(this,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                );
                googleClient.signOut();

                // Kaydedilmiş tüm oturum ve profil bilgilerini temizle
                getSharedPreferences("login_prefs", Context.MODE_PRIVATE).edit().clear().apply();
                getSharedPreferences("profile_prefs", Context.MODE_PRIVATE).edit().clear().apply();

                // Login ekranına yönlendir
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build();
                navController.navigate(R.id.loginFragment, null, navOptions);

                drawerLayout.closeDrawers();
                Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show();
                return true;
            }

            // Aynı sayfadaysa geri git, değilse seçilen sayfaya yönlendir
            if (navController.getCurrentDestination().getId() == itemId) {
                navController.popBackStack();
            }
            navController.navigate(itemId);
            drawerLayout.closeDrawers();
            return true;
        });

        // Giriş ve kayıt ekranlarındayken menü çubuğunu gizle
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideMenu = destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment;

            drawerLayout.setDrawerLockMode(hideMenu
                    ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    : DrawerLayout.LOCK_MODE_UNLOCKED);

            if (getSupportActionBar() != null) {
                if (hideMenu) getSupportActionBar().hide();
                else getSupportActionBar().show();
            }
        });
    }

     // onStart içinde kullanıcı bilgilerini çek, header'ı güncelle ve broadcast alıcısını kaydet
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Kullanıcı oturum açmışsa işlemleri başlat
        if (currentUser != null) {
            Log.d(TAG, "Kullanıcı oturum açmış, dinleyiciler başlatılıyor. UserId: " + currentUser.getUid());
            updateNavigationHeader();
            // Broadcast alıcısını kaydet
            LocalBroadcastManager.getInstance(this).registerReceiver(newMessageReceiver,
                    new IntentFilter(MyFirebaseMessagingService.ACTION_NEW_MESSAGE_RECEIVED));
            Log.d(TAG, "Broadcast receiver kaydedildi.");

            // Menu açıldığında header bilgilerini güncelle
            if (drawerLayout != null) {
                drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {
                        updateNavigationHeader();
                    }
                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {}
                    @Override
                    public void onDrawerStateChanged(int newState) {}
                });
            }
            // Uygulama başlatıldığında veya ön plana geldiğinde okunmamış mesajları kontrol et
            checkUnreadMessagesAndNotify();
        } else {
            Log.d(TAG, "Kullanıcı oturum açmamış, dinleyiciler başlatılmadı.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Sadece kullanıcı oturum açmışsa dinleyicileri temizle
        if (currentUser != null) {
            Log.d(TAG, "Kullanıcı oturum açmış, dinleyiciler temizleniyor. UserId: " + currentUser.getUid());
            // Broadcast alıcısını kaydı sil
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newMessageReceiver);
            Log.d(TAG, "Broadcast receiver kaydı silindi.");
        } else {
            Log.d(TAG, "Kullanıcı oturum açmamış, dinleyici temizliği gerekmiyor.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Sadece kullanıcı oturum açmışsa okunmamış mesajları kontrol et
        if (currentUser != null) {
            Log.d(TAG, "Kullanıcı oturum açmış, okunmamış mesajlar kontrol ediliyor. UserId: " + currentUser.getUid());
            checkUnreadMessagesAndNotify();
        } else {
            Log.d(TAG, "Kullanıcı oturum açmamış, okunmamış mesaj kontrolü yapılmıyor.");
        }
    }

    // Kullanıcı çıkış yaptığında tüm dinleyicileri temizle
    private void cleanupOnLogout() {
        Log.d(TAG, "Kullanıcı çıkış yapıyor, tüm dinleyiciler temizleniyor.");
        // Broadcast alıcısını kaydı sil
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newMessageReceiver);
            Log.d(TAG, "Broadcast receiver kaydı silindi.");
        } catch (IllegalArgumentException e) {
            // Receiver zaten kayıtlı değilse hata vermesin
            Log.d(TAG, "Broadcast receiver zaten kayıtlı değil.");
        }
    }

    // Kullanıcı bilgileriyle navigation drawer başlığını güncelle
    private void updateNavigationHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Kullanıcı oturumu açık değilse varsayılan resmi göster ve çık
            View headerView = navigationView.getHeaderView(0);
            ShapeableImageView imageViewProfile = headerView.findViewById(R.id.imageViewProfile);
            TextView textViewUsername = headerView.findViewById(R.id.textViewUsername);
            textViewUsername.setText("Misafir"); // Veya uygun bir metin
             Glide.with(MainActivity.this)
                    .load(R.drawable.ic_defaultprofile)
                    .circleCrop()
                    .into(imageViewProfile);
            lastProfileImageBase64 = null;
            lastUsername = null; // Kullanıcı adı da sıfırlanmalı
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        ShapeableImageView imageViewProfile = headerView.findViewById(R.id.imageViewProfile);
        TextView textViewUsername = headerView.findViewById(R.id.textViewUsername);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User myUser = snapshot.getValue(User.class);
                if (myUser != null) {
                    String username = myUser.getUsername();
                    String base64 = myUser.getProfileImageUrl();

                    // Kullanıcı adını güncelle (sadece değiştiyse)
                    if (lastUsername == null || !lastUsername.equals(username)) {
                        textViewUsername.setText("Hoş geldin, " + username);
                        lastUsername = username;
                    }

                    // Profil resmini yükle
                    if (base64 != null && !base64.trim().isEmpty()) {
                        // Base64 verisi varsa ve değişmişse yükle
                        if (lastProfileImageBase64 == null || !lastProfileImageBase64.equals(base64)) {
                            try {
                                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    Glide.with(MainActivity.this)
                                            .load(bitmap)
                                            .placeholder(R.drawable.ic_defaultprofile)
                                            .error(R.drawable.ic_defaultprofile)
                                            .circleCrop()
                                            .into(imageViewProfile);
                                    lastProfileImageBase64 = base64; // Başarılı yüklemede cache'i güncelle
                                } else {
                                     // Bitmap dönüşümü başarısız olursa varsayılanı göster
                                    Glide.with(MainActivity.this)
                                            .load(R.drawable.ic_defaultprofile)
                                            .circleCrop()
                                            .into(imageViewProfile);
                                    lastProfileImageBase64 = null; // Cache'i temizle
                                }
                            } catch (IllegalArgumentException e) {
                                // Geçersiz Base64 formatı hatası durumunda varsayılanı göster
                                Glide.with(MainActivity.this)
                                        .load(R.drawable.ic_defaultprofile)
                                        .circleCrop()
                                        .into(imageViewProfile);
                                lastProfileImageBase64 = null; // Cache'i temizle
                            } catch (Exception e) {
                                // Diğer beklenmeyen hatalarda varsayılanı göster
                                Glide.with(MainActivity.this)
                                        .load(R.drawable.ic_defaultprofile)
                                        .circleCrop()
                                        .into(imageViewProfile);
                                lastProfileImageBase64 = null; // Cache'i temizle
                            }
                        }
                         // Base64 değişmediyse bir şey yapma, mevcut resim kalır
                    } else {
                        // Base64 boş veya null ise varsayılan resmi göster
                        // Sadece mevcut resim varsayılan değilse veya hiç resim yoksa yükle
                        if (lastProfileImageBase64 != null) { // Eğer daha önce yüklü bir resim varsa temizle
                             Glide.with(MainActivity.this)
                                    .load(R.drawable.ic_defaultprofile)
                                    .circleCrop()
                                    .into(imageViewProfile);
                            lastProfileImageBase64 = null; // Cache'i temizle
                        }
                        // Eğer zaten varsayılan resim yüklüyse tekrar yüklemeye gerek yok
                    }
                } else {
                    // Kullanıcı verisi null ise varsayılan resmi göster
                    Glide.with(MainActivity.this)
                            .load(R.drawable.ic_defaultprofile)
                            .circleCrop()
                            .into(imageViewProfile);
                    lastProfileImageBase64 = null;
                    lastUsername = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase hatası durumunda varsayılan resmi göster
                Glide.with(MainActivity.this)
                        .load(R.drawable.ic_defaultprofile)
                        .circleCrop()
                        .into(imageViewProfile);
                lastProfileImageBase64 = null;
                lastUsername = null;
            }
        });
    }

    // Geri butonuna basıldığında navigation kontrolünü sağla
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
    // Bildirimler için izin uyarısı
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bildirim izni verilmedi. Bazı özellikler çalışmayabilir.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Bildirimden gelen yönlendirmeyi işle
    private void handleNotificationNavigation(Intent intent) {
        if (intent != null && intent.getBooleanExtra("navigateToChat", false)) {
            String chatRoomId = intent.getStringExtra("chatRoomId");

            if (chatRoomId != null) {
                // Kullanıcı giriş yapmışsa chat ekranına yönlendir
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment);
                    if (navHostFragment != null) {
                        NavController navController = navHostFragment.getNavController();

                        // ChatFragment'e yönlendir
                        Bundle args = new Bundle();
                        args.putString("chatRoomId", chatRoomId);

                        // Doğrudan ChatFragment'e git. Navigation Component aynı hedefteyse fragment'i yeniden oluşturmayabilir ya da uygun şekilde güncelleyebilir.
                        // Eğer fragment içinde özel bir güncelleme gerekiyorsa onNewIntent() veya ViewModel kullanılabilir.
                        navController.navigate(R.id.chatFragment, args);
                    }
                }
            }
            // Intent'i temizle
            intent.removeExtra("navigateToChat");
            intent.removeExtra("chatRoomId");
        }
    }

     @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationNavigation(intent);
    }

    // Gelen tek bir mesaj için bildirim göster
    private void showSingleNotification(String chatRoomId, String senderId, String messageBody, String messageId, String notificationTitle) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Kullanıcı oturum açmamış, tekil bildirim gösterilmiyor");
            return;
        }

        // Mesajın alıcısı biz miyiz kontrol et
        String receiverId = currentUser.getUid();
        if (!getChatRoomId(senderId, receiverId).equals(chatRoomId)) {
            Log.d(TAG, "Bu mesajın alıcısı değil, tekil bildirim gösterilmiyor. Gelen ChatRoomId: " + chatRoomId + ", Beklenen ChatRoomId: " + getChatRoomId(senderId, receiverId));
            return;
        }

        // Son bir kez daha mesajın okunup okunmadığını kontrol et
        FirebaseDatabase.getInstance().getReference("messages")
                .child(chatRoomId)
                .child(messageId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null && !message.isRead()) {
                        Log.d(TAG, "Son kontrol: Mesaj okunmamış, bildirim gösteriliyor: " + messageId);
                        showNotificationInternal(chatRoomId, senderId, messageBody, messageId, notificationTitle);
                    } else {
                        Log.d(TAG, "Son kontrol: Mesaj okunmuş veya bulunamadı, bildirim gösterilmiyor: " + messageId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Son mesaj durumu kontrolünde hata: " + e.getMessage());
                    Log.d(TAG, "Hata nedeniyle bildirim gösterilmiyor");
                });
    }

    // Bildirim gösterme işlemini gerçekleştiren iç metod
    private void showNotificationInternal(String chatRoomId, String senderId, String messageBody, String messageId, String notificationTitle) {
        Log.d(TAG, "Bildirim gösteriliyor: ChatRoomId=" + chatRoomId + ", SenderId=" + senderId + ", MessageBody=" + messageBody);

        // Bildirim kanalını oluştur (eğer yoksa)
        createNotificationChannel();

        // Chat ekranına yönlendirme için Intent
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigateToChat", true);
        intent.putExtra("chatRoomId", chatRoomId);
        intent.putExtra("senderId", senderId);

        // Benzersiz RequestCode oluştur (Sohbet odası bazında tek bildirim)
        int requestCode = chatRoomId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Bildirimi oluştur
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "chat_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Bildirimi göster
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(requestCode, builder.build());
                Log.d(TAG, "Bildirim başarıyla gösterildi: " + messageId);
            } else {
                Log.e(TAG, "Bildirim izni yok");
            }
        } catch (Exception e) {
            Log.e(TAG, "Bildirim gösterilirken hata: " + e.getMessage());
        }
    }

    // Tüm sohbet odalarındaki okunmamış mesajları kontrol et ve bildirim göster
    private void checkUnreadMessagesAndNotify() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Kullanıcı oturum açmamış, okunmamış mesaj kontrolü yapılmıyor");
            return;
        }

        Log.d(TAG, "Okunmamış mesajlar (genel kontrol) kontrol ediliyor... Kullanıcı: " + currentUser.getUid());

        // Bildirim kanalını oluştur (eğer yoksa)
        createNotificationChannel();

        ChatRepository chatRepository = new ChatRepository();
        Log.d(TAG, "ChatRepository oluşturuldu, checkUnreadMessages çağrılıyor...");
        
        chatRepository.checkUnreadMessages(currentUser.getUid(), unreadMessages -> {
            Log.d(TAG, "checkUnreadMessages callback çağrıldı. Bulunan mesaj sayısı: " + unreadMessages.size());

            if (!unreadMessages.isEmpty()) {
                Log.d(TAG, "Genel kontrolde okunmamış mesajlar bulundu, bildirimler kontrol ediliyor...");

                // Her bir okunmamış mesaj için son durumu kontrol et
                for (ChatMessage message : unreadMessages) {
                    FirebaseDatabase.getInstance().getReference("messages")
                            .child(message.getChatRoomId())
                            .child(message.getMessageId())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                ChatMessage currentMessage = snapshot.getValue(ChatMessage.class);
                                if (currentMessage != null && !currentMessage.isRead()) {
                                    // Mesaj hala okunmamışsa bildirimi göster
                                    Log.d(TAG, "Mesaj hala okunmamış, bildirim gösteriliyor: " + message.getMessageId());
                                    showNotificationForMessage(currentMessage);
                                } else {
                                    Log.d(TAG, "Mesaj artık okunmuş veya bulunamadı, bildirim gösterilmiyor: " + message.getMessageId());
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Mesaj durumu kontrol edilirken hata: " + e.getMessage());
                                Log.d(TAG, "Hata nedeniyle bildirim gösterilmiyor");
                            });
                }
            } else {
                Log.d(TAG, "Genel kontrolde okunmamış mesaj bulunamadı.");
            }
        });
    }

    // Mesaj için bildirim gösterme yardımcı metodu
    private void showNotificationForMessage(ChatMessage message) {
        FirebaseDatabase.getInstance().getReference("users")
                .child(message.getSenderId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String senderName = snapshot.child("username").getValue(String.class);
                        String notificationTitle = senderName != null ? senderName : "Yeni Mesaj";
                        showSingleNotification(
                                message.getChatRoomId(),
                                message.getSenderId(),
                                message.getMessage(),
                                message.getMessageId(),
                                notificationTitle
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Gönderen adı çekme hatası: " + error.getMessage());
                        showSingleNotification(
                                message.getChatRoomId(),
                                message.getSenderId(),
                                message.getMessage(),
                                message.getMessageId(),
                                "Yeni Mesaj"
                        );
                    }
                });
    }

     // Yardımcı: İki kullanıcı için ortak chat odası ID'si üret (ChatRepository'deki ile aynı)
    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Bildirim kanalı oluşturma (MyFirebaseMessagingService'deki ile aynı mantık)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "chat_channel";
            CharSequence channelName = "Sohbet Bildirimleri";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription("Sohbet mesajları için bildirim kanalı");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Bildirim kanalı oluşturuldu.");
            }
        }
    }

}