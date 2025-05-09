package com.example.playmate.ui.auth;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.playmate.R;
import com.example.playmate.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    private NavController navController;
    private SharedPreferences prefs;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // ViewBinding ve ViewModel başlatılıyor
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // SharedPreferences başlat ve önceki seçimleri yükle
        prefs = requireActivity()
                .getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

        boolean rememberMe = prefs.getBoolean("remember_me", false);
        binding.checkBoxRememberMe.setChecked(rememberMe);

        // Kaydedilmiş e-postayı yükle
        String savedEmail = prefs.getString("user_email", "");
        if (!savedEmail.isEmpty()) {
            binding.editTextEmail.setText(savedEmail);
        }

        // Kaydedilmiş şifreyi yükle
        String savedPass = prefs.getString("user_password", "");
        if (!savedPass.isEmpty()) {
            binding.editTextPassword.setText(savedPass);
        }

        // Normal e-posta/şifre girişi için buton
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Boş alan bırakmayın!", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.login(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(getContext(), "Giriş başarılı!", Toast.LENGTH_SHORT).show();

                        // Seçimi SharedPreferences'e kaydet
                        boolean remember = binding.checkBoxRememberMe.isChecked();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("remember_me", remember);
                        if (remember) {
                            editor.putString("user_email", email);
                            editor.putString("user_password",password);
                        } else {
                            editor.remove("user_email");
                            editor.remove("user_password");
                        }
                        editor.apply();

                        // Backstack temizlenerek ana sayfaya yönlendir
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build();
                        navController.navigate(R.id.action_loginFragment_to_homeFragment, null, navOptions);
                    })
                    .addOnFailureListener(e -> new AlertDialog.Builder(getContext())
                            .setTitle("Hata")
                            .setMessage("Giriş başarısız: " + e.getMessage())
                            .setPositiveButton("Tamam", null)
                            .show()
                    );
        });

        // Kayıt ekranına geçiş
        binding.textViewGoToRegister.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_registerFragment)
        );

        // Google Sign-In butonları
        binding.imageButtonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.textViewGoToGoogle.setOnClickListener(v -> signInWithGoogle());

        // Google Sign-In yapılandırması
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        return binding.getRoot();
    }

    private void signInWithGoogle() {
        // Önceki oturumu kapatıp yeni seçim ekranını aç
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(getContext(), "Google Giriş Hatası", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        Toast.makeText(getContext(), "Google ile giriş başarılı!", Toast.LENGTH_SHORT).show();

                        // Seçimi SharedPreferences'e kaydet
                        boolean remember = binding.checkBoxRememberMe.isChecked();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("remember_me", remember);
                        if (remember) {
                            editor.putString("user_email", user.getEmail());
                            editor.putString("user_name", user.getDisplayName());
                            if (user.getPhotoUrl() != null) {
                                editor.putString("user_profile_url", user.getPhotoUrl().toString());
                            }
                        } else {
                            editor.remove("user_email");
                            editor.remove("user_name");
                            editor.remove("user_profile_url");
                        }
                        editor.apply();

                        // Backstack temizlenerek ana sayfaya yönlendir
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build();
                        navController.navigate(R.id.action_loginFragment_to_homeFragment, null, navOptions);
                    } else {
                        Toast.makeText(getContext(), "Google Girişi Başarısız!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}