package com.example.playmate.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> loginUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
}