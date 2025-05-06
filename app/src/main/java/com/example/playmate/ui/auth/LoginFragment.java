package com.example.playmate.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

    // Google Sign-In Client
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;  // Request code

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Giriş butonu
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

                        // Beni Hatırla işaretliyse SharedPreferences'e kaydet
                        if (binding.checkBoxRememberMe.isChecked()) {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("login_prefs", 0);
                            prefs.edit().putBoolean("remember_me", true).apply();
                        }

                        // Yönlendirme
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build();

                        Navigation.findNavController(binding.getRoot())
                                .navigate(R.id.action_loginFragment_to_homeFragment, null, navOptions);
                    })
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Hata")
                                .setMessage("Giriş başarısız: " + e.getMessage())
                                .setPositiveButton("Tamam", null)
                                .show();
                    });
        });

        // Kayıt ekranına yönlendirme
        binding.textViewGoToRegister.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_loginFragment_to_registerFragment);
        });

        // Google Sign-In Başlatma
        binding.buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Google Sign-In Config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        return binding.getRoot();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        Toast.makeText(getContext(), "Google ile giriş başarılı!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot())
                                .navigate(R.id.action_loginFragment_to_homeFragment);
                    } else {
                        Toast.makeText(getContext(), "Google Girişi Başarısız!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}