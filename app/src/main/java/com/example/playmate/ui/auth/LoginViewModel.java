package com.example.playmate.ui.auth;

import androidx.lifecycle.ViewModel;
import com.example.playmate.data.repository.AuthRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class LoginViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public LoginViewModel() {
        authRepository = new AuthRepository();
    }

    public Task<AuthResult> login(String email, String password) {
        return authRepository.loginUser(email, password);
    }
}