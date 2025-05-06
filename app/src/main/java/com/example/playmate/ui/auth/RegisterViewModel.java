package com.example.playmate.ui.auth;

import androidx.lifecycle.ViewModel;
import com.example.playmate.data.repository.AuthRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class RegisterViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public RegisterViewModel() {
        authRepository = new AuthRepository();
    }

    public Task<AuthResult> register(String email, String password) {
        return authRepository.registerUser(email, password);
    }
}