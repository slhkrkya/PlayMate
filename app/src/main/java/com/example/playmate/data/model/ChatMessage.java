package com.example.playmate.data.model;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean isRead; // Okundu bilgisi
    private String chatRoomId; // Chat odası ID'si

    public ChatMessage() {
        // Boş constructor Firebase için gerekli
    }

    // MesajId olmadan kurucu (Firebase push sonrası set edilecek)
    public ChatMessage(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false; // Yeni mesajlar başlangıçta okunmamış kabul edilir
        this.chatRoomId = generateChatRoomId(senderId, receiverId);
    }

    // Chat odası ID'si oluşturma
    private String generateChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Getter ve Setter'lar
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
        // SenderId değiştiğinde chatRoomId'yi güncelle
        if (this.receiverId != null) {
            this.chatRoomId = generateChatRoomId(senderId, this.receiverId);
        }
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
        // ReceiverId değiştiğinde chatRoomId'yi güncelle
        if (this.senderId != null) {
            this.chatRoomId = generateChatRoomId(this.senderId, receiverId);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getChatRoomId() {
        if (chatRoomId == null && senderId != null && receiverId != null) {
            chatRoomId = generateChatRoomId(senderId, receiverId);
        }
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
