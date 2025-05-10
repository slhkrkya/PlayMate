package com.example.playmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.playmate.data.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private DrawerLayout drawerLayout;
    private NavController navController;
    private NavigationView navigationView;

    // Cache for last loaded profile image and username
    private String lastProfileImageBase64 = null;
    private String lastUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar'ı bağla ve varsayılan action bar olarak ayarla
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

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
        }

        // AppBar ve NavigationDrawer'ı kontrolcüye bağla
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.nav_profile, R.id.editProfileFragment
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Navigation menüsündeki tıklamaları kontrol et
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Kullanıcı çıkış yaparsa tüm oturumları kapat ve SharedPreferences'ı temizle
            if (itemId == R.id.nav_logout) {
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

    // onStart içinde kullanıcı bilgilerini çek ve header'ı güncelle
    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            updateNavigationHeader();
        }

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
    }

    // Kullanıcı bilgileriyle navigation drawer başlığını güncelle
    private void updateNavigationHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

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

                    // Only update username if changed
                    if (lastUsername == null || !lastUsername.equals(username)) {
                        textViewUsername.setText("Hoş geldin, " + username);
                        lastUsername = username;
                    }

                    // Only update image if changed
                    if (base64 != null && !base64.trim().isEmpty() && (lastProfileImageBase64 == null || !lastProfileImageBase64.equals(base64))) {
                        try {
                            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            Glide.with(MainActivity.this)
                                    .load(bitmap)
                                    .placeholder(R.drawable.ic_defaultprofile)
                                    .error(R.drawable.ic_defaultprofile)
                                    .circleCrop()
                                    .into(imageViewProfile);
                            lastProfileImageBase64 = base64;
                        } catch (Exception e) {
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.ic_defaultprofile)
                                    .circleCrop()
                                    .into(imageViewProfile);
                            lastProfileImageBase64 = null;
                        }
                    } else if (base64 == null || base64.trim().isEmpty()) {
                        if (lastProfileImageBase64 != null) {
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.ic_defaultprofile)
                                    .circleCrop()
                                    .into(imageViewProfile);
                            lastProfileImageBase64 = null;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Glide.with(MainActivity.this)
                        .load(R.drawable.ic_defaultprofile)
                        .circleCrop()
                        .into(imageViewProfile);
            }
        });
    }

    // Geri butonuna basıldığında navigation kontrolünü sağla
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}