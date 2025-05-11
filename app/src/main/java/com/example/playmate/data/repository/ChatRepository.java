package com.example.playmate.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.playmate.data.model.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRepository {

    private final DatabaseReference databaseReference;

    public ChatRepository() {
        databaseReference = FirebaseDatabase.getInstance().getReference("messages");
    }

    // Mesaj gönderme
    public void sendMessage(String senderId, String receiverId, String messageText) {
        String chatRoomId = getChatRoomId(senderId, receiverId);

        // Firebase push referansı ile ID oluştur
        DatabaseReference newMessageRef = databaseReference.child(chatRoomId).push();
        String messageId = newMessageRef.getKey();

        // Model nesnesi
        ChatMessage chatMessage = new ChatMessage(senderId, receiverId, messageText, System.currentTimeMillis());
        chatMessage.setMessageId(messageId); // ID'yi sete ekle

        // Firebase'e gönder
        newMessageRef.setValue(chatMessage)
                .addOnSuccessListener(aVoid -> Log.d("ChatRepository", "Mesaj gönderildi."))
                .addOnFailureListener(e -> Log.e("ChatRepository", "Mesaj gönderme hatası: " + e.getMessage()));
    }

    // Mesajları dinleme
    public void listenForMessages(String senderId, String receiverId, final MessageCallback callback) {
        String chatRoomId = getChatRoomId(senderId, receiverId);

        databaseReference.child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    ChatMessage message = msgSnap.getValue(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(msgSnap.getKey()); // push id'yi messageId'ye yaz
                        messages.add(message);
                    }
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

    // Yardımcı: İki kullanıcı için ortak chat odası ID'si üret
    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Callback interface
    public interface MessageCallback {
        void onMessagesReceived(List<ChatMessage> messages);
    }
}
