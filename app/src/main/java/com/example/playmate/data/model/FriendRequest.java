package com.example.playmate.data.model;

import java.util.HashMap;
import java.util.Map;

public class FriendRequest {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String status; // "pending", "accepted", "rejected"
    private long timestamp;

    public FriendRequest() {
        // Required empty constructor for Firebase
    }

    public FriendRequest(String senderId, String receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("senderId", senderId);
        result.put("receiverId", receiverId);
        result.put("status", status);
        result.put("timestamp", timestamp);
        return result;
    }
} 