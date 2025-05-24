package com.example.playmate.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.playmate.MainActivity;
import com.example.playmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "chat_channel";
    private static final String CHANNEL_NAME = "Sohbet Bildirimleri";
    private static final String TAG = "FCM_SERVICE";
    // MainActivity'e mesaj detaylarını göndermek için kullanılacak eylem
    public static final String ACTION_NEW_MESSAGE_RECEIVED = "com.example.playmate.NEW_MESSAGE_RECEIVED";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token güncellendi: " + token);

        // Token'ı hemen kaydet ve logla
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Token kaydediliyor - UserId: " + currentUser.getUid());
            FirebaseDatabase.getInstance().getReference("tokens")
                    .child(currentUser.getUid())
                    .setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Token başarıyla veritabanına kaydedildi");
                        // Token'ı Cloud Function'a bildir
                        notifyTokenUpdate(currentUser.getUid(), token);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Token veritabanına kaydedilirken hata: " + e.getMessage());
                        // Hata durumunda tekrar dene
                        retryTokenUpdate(currentUser.getUid(), token);
                    });
        } else {
            Log.w(TAG, "Token güncellendi ama kullanıcı oturum açmamış");
        }
    }

    private void notifyTokenUpdate(String userId, String token) {
        // Cloud Function'a token güncellemesini bildir
        FirebaseDatabase.getInstance().getReference("tokenUpdates")
                .child(userId)
                .setValue(token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token güncellemesi Cloud Function'a bildirildi"))
                .addOnFailureListener(e -> Log.e(TAG, "Token güncellemesi bildirilemedi: " + e.getMessage()));
    }

    private void retryTokenUpdate(String userId, String token) {
        // 5 saniye sonra tekrar dene
        new android.os.Handler().postDelayed(() -> {
            Log.d(TAG, "Token kaydı tekrar deneniyor...");
            FirebaseDatabase.getInstance().getReference("tokens")
                    .child(userId)
                    .setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Token başarıyla kaydedildi (yeniden deneme)");
                        notifyTokenUpdate(userId, token);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Token kaydı tekrar başarısız: " + e.getMessage()));
        }, 5000);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived çağrıldı - Message ID: " + remoteMessage.getMessageId());

        // Data payload'u kontrol et
        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) {
            Log.e(TAG, "Boş veya null data payload alındı");
            return;
        }

        Log.d(TAG, "Data payload detayları:");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Log.d(TAG, entry.getKey() + ": " + entry.getValue());
        }

        // Gerekli alanların olup olmadığını kontrol et
        String[] requiredFields = {"chatRoomId", "senderId", "body", "messageId", "timestamp"};
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                Log.e(TAG, "Eksik alan: " + field);
                return;
            }
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Log.d(TAG, "Kullanıcı oturum açmamış, bildirim işlenmiyor");
            return;
        }

        // ChatRoomId'den alıcı ID'sini çıkar
        String chatRoomId = data.get("chatRoomId");
        String senderId = data.get("senderId");
        String receiverId = chatRoomId.replace(senderId + "_", "").replace("_" + senderId, "");

        Log.d(TAG, "Mesaj detayları:" +
                "\nChatRoomId: " + chatRoomId +
                "\nSenderId: " + senderId +
                "\nReceiverId: " + receiverId +
                "\nCurrentUserId: " + currentUserId);

        // Mesajın alıcısı mevcut kullanıcı değilse işlemi durdur
        if (!currentUserId.equals(receiverId)) {
            Log.d(TAG, "Mevcut kullanıcı mesajın alıcısı değil, bildirim gösterilmiyor." +
                    "\nAlıcı ID: " + receiverId +
                    "\nMevcut Kullanıcı ID: " + currentUserId);
            return;
        }

        // MainActivity'e broadcast gönder
        Intent intent = new Intent(ACTION_NEW_MESSAGE_RECEIVED);
        // Data payload'daki tüm bilgileri intent'e ekle
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        intent.putExtra("receiverId", receiverId);

        try {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "Broadcast başarıyla gönderildi: " + ACTION_NEW_MESSAGE_RECEIVED);
        } catch (Exception e) {
            Log.e(TAG, "Broadcast gönderilirken hata: " + e.getMessage());
        }
    }

    // Bildirim kanalı oluşturma (MainActivity ile senkronize olmalı)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
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