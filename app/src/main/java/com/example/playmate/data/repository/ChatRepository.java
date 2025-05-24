package com.example.playmate.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.playmate.data.model.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRepository {

    private final DatabaseReference databaseReference;
    private final DatabaseReference usersRef;

    public ChatRepository() {
        databaseReference = FirebaseDatabase.getInstance().getReference("messages");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    // Mesaj gönderme
    public void sendMessage(String senderId, String receiverId, String messageText) {
        String chatRoomId = getChatRoomId(senderId, receiverId);
        Log.d("ChatRepository", "sendMessage çağrıldı. Sender: " + senderId + ", Receiver: " + receiverId + ", ChatRoomId: " + chatRoomId);

        // Firebase push referansı ile ID oluştur
        DatabaseReference newMessageRef = databaseReference.child(chatRoomId).push();
        String messageId = newMessageRef.getKey();
        Log.d("ChatRepository", "Yeni mesaj ID: " + messageId);

        // Model nesnesi
        ChatMessage chatMessage = new ChatMessage(senderId, receiverId, messageText, System.currentTimeMillis());
        // isRead alanı ChatMessage constructor'ında false olarak ayarlanıyor.
        chatMessage.setMessageId(messageId);

        // Firebase'e gönder
        newMessageRef.setValue(chatMessage)
                .addOnSuccessListener(aVoid -> Log.d("ChatRepository", "Mesaj başarıyla Firebase'e yazıldı."))
                .addOnFailureListener(e -> Log.e("ChatRepository", "Mesaj Firebase'e yazılırken hata oluştu: " + e.getMessage(), e));
    }

    // Mesajları dinleme
    // Bu metod aynı zamanda karşı kullanıcının gönderdiği mesajları okundu olarak işaretleyebilir.
    public void listenForMessages(String currentUserId, String chatRoomId, final MessageCallback callback, boolean markAsRead) {
        databaseReference.child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                List<String> unreadMessageIds = new ArrayList<>();

                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    ChatMessage message = msgSnap.getValue(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(msgSnap.getKey());
                        // Karşı kullanıcının gönderdiği ve okunmamış mesajları işaretle
                        if (markAsRead && !message.getSenderId().equals(currentUserId) && !message.isRead()) {
                            unreadMessageIds.add(message.getMessageId());
                            Log.d("ChatRepository", "Okunmamış mesaj bulundu: " + message.getMessageId());
                        }
                        messages.add(message);
                    }
                }

                // Sadece markAsRead true ise mesajları okundu olarak işaretle
                if (markAsRead && !unreadMessageIds.isEmpty()) {
                    Log.d("ChatRepository", "Mesajlar okundu olarak işaretleniyor. Mesaj sayısı: " + unreadMessageIds.size());
                    markMessagesAsRead(chatRoomId, unreadMessageIds);
                }

                Collections.sort(messages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                callback.onMessagesReceived(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatRepository", "Mesaj dinleme hatası: " + error.getMessage());
            }
        });
    }

    // Eski metodu geriye dönük uyumluluk için tutuyoruz
    public void listenForMessages(String currentUserId, String chatRoomId, final MessageCallback callback) {
        listenForMessages(currentUserId, chatRoomId, callback, true);
    }

    // Belirli mesajları okundu olarak işaretleme
    private void markMessagesAsRead(String chatRoomId, List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;

        DatabaseReference chatRef = databaseReference.child(chatRoomId);
        for (String messageId : messageIds) {
            chatRef.child(messageId).child("isRead").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ChatRepository", "Mesaj okundu olarak işaretlendi: " + messageId);
                        // Mesajın diğer alanlarını da güncelle
                        chatRef.child(messageId).get().addOnSuccessListener(snapshot -> {
                            ChatMessage message = snapshot.getValue(ChatMessage.class);
                            if (message != null) {
                                message.setRead(true);
                                chatRef.child(messageId).setValue(message)
                                        .addOnSuccessListener(aVoid2 -> Log.d("ChatRepository", "Mesaj tam olarak güncellendi: " + messageId))
                                        .addOnFailureListener(e -> Log.e("ChatRepository", "Mesaj güncelleme hatası: " + messageId + " - " + e.getMessage()));
                            }
                        });
                    })
                    .addOnFailureListener(e -> Log.e("ChatRepository", "Mesaj okundu hatası: " + messageId + " - " + e.getMessage()));
        }
    }

    // Yardımcı: İki kullanıcı için ortak chat odası ID'si üret
    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Callback interface
    public interface MessageCallback {
        void onMessagesReceived(List<ChatMessage> messages);
    }

    // -------------- Yeni Okunmamış Mesaj Kontrol Metodları --------------

    // Tüm sohbet odalarındaki okunmamış mesajları kontrol et ve geri dön
    public void checkUnreadMessages(String currentUserId, final UnreadMessagesCallback callback) {
        // Kullanıcının dahil olduğu tüm sohbet odalarını bulmalıyız.
        // Basit bir yaklaşımla, kullanıcının ID'sini içeren tüm chatRoomId'lerini kontrol edebiliriz.
        // Daha verimli bir yapı için kullanıcıların chat odası listesini tutmak gerekebilir.

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> unreadMessages = new ArrayList<>();
                for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatRoomSnapshot.getKey();
                    if (chatRoomId != null && chatRoomId.contains(currentUserId)) {
                        // Bu sohbet odasındaki okunmamış mesajları kontrol et
                        for (DataSnapshot messageSnapshot : chatRoomSnapshot.getChildren()) {
                            ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                            if (message != null) {
                                // Mesaj mevcut kullanıcıya ait değilse ve okunmamışsa
                                if (!message.getSenderId().equals(currentUserId) && !message.isRead()) {
                                    message.setMessageId(messageSnapshot.getKey()); // Mesaj ID'sini ekle
                                     unreadMessages.add(message);
                                }
                            }
                        }
                    }
                }
                 callback.onUnreadMessagesChecked(unreadMessages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatRepository", "Okunmamış mesaj kontrol hatası: " + error.getMessage());
                 callback.onUnreadMessagesChecked(Collections.emptyList()); // Hata durumunda boş liste dön
            }
        });
    }

    // Okunmamış mesajları geri döndürmek için Callback interface'i
    public interface UnreadMessagesCallback {
        void onUnreadMessagesChecked(List<ChatMessage> unreadMessages);
    }
}
