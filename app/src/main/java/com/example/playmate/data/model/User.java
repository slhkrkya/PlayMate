package com.example.playmate.data.model;

import java.util.Map;
import java.util.HashMap;

public class User {
    private String uid;
    private String username;
    private String email;
    private String favoriteGame;
    private String location;
    private String profileImageUrl; // Yeni alan

    public User() {} // Firebase için boş constructor

    public User(String uid, String username, String email, String favoriteGame, String location, String profileImageUrl) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.favoriteGame = favoriteGame;
        this.location = location;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFavoriteGame() {
        return favoriteGame;
    }

    public void setFavoriteGame(String favoriteGame) {
        this.favoriteGame = favoriteGame;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // Firebase için toMap() metodu
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("username", username);
        result.put("email", email);
        result.put("favoriteGame", favoriteGame);
        result.put("location", location);
        result.put("profileImageUrl", profileImageUrl); // Yeni alan ekleniyor
        return result;
    }
}