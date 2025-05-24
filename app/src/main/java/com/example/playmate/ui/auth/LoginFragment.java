package com.example.playmate.ui.auth;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    private NavController navController;
    private SharedPreferences prefs;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LoginFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        prefs = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        binding.checkBoxRememberMe.setChecked(rememberMe);

        binding.editTextEmail.setText(prefs.getString("user_email", ""));
        binding.editTextPassword.setText(prefs.getString("user_password", ""));

        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Boş alan bırakmayın!");
                return;
            }

            viewModel.login(email, password)
                    .addOnSuccessListener(authResult -> {
                        showToast("Giriş başarılı!");

                        // "Beni Hatırla" seçimini kaydet
                        boolean remember = binding.checkBoxRememberMe.isChecked();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("remember_me", remember);
                        if (remember) {
                            editor.putString("user_email", email);
                            editor.putString("user_password", password);
                        } else {
                            editor.remove("user_email");
                            editor.remove("user_password");
                        }
                        editor.apply();

                        // ✅ FCM token al ve Realtime DB'ye kaydet
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w("FCM_TOKEN", "Token alınamadı", task.getException());
                                        return;
                                    }

                                    String token = task.getResult();
                                    Log.d("FCM_TOKEN", "Token: " + token);

                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    if (currentUser != null) {
                                        FirebaseDatabase.getInstance().getReference("tokens")
                                                .child(currentUser.getUid())
                                                .setValue(token);
                                    }
                                });

                        // Ana sayfaya yönlendir
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build();
                        navController.navigate(R.id.action_loginFragment_to_homeFragment, null, navOptions);
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && getContext() != null) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Hata")
                                    .setMessage("Giriş başarısız: " + e.getMessage())
                                    .setPositiveButton("Tamam", null)
                                    .show();
                        }
                    });
        });

        binding.textViewGoToRegister.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_registerFragment));

        // Google Sign-In ayarları
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        binding.imageButtonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.textViewGoToGoogle.setOnClickListener(v -> signInWithGoogle());

        return binding.getRoot();
    }

    private void signInWithGoogle() {
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
                showToast("Google Giriş Hatası");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        if (!isAdded() || getContext() == null) {
            Log.d(TAG, "firebaseAuthWithGoogle: Fragment not attached");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (!isAdded() || getContext() == null) {
                        Log.d(TAG, "onComplete: Fragment not attached");
                        return;
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        showToast("Google ile giriş başarılı!");

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

                        // Check current destination before navigating
                        if (navController.getCurrentDestination() != null) {
                            int currentDestination = navController.getCurrentDestination().getId();
                            if (currentDestination == R.id.loginFragment) {
                                // Only navigate if we're on the login fragment
                                NavOptions navOptions = new NavOptions.Builder()
                                        .setPopUpTo(R.id.loginFragment, true)
                                        .build();
                                navController.navigate(R.id.action_loginFragment_to_homeFragment, null, navOptions);
                            } else {
                                Log.d(TAG, "Navigation skipped - already on destination: " + currentDestination);
                            }
                        }
                    } else {
                        showToast("Google Girişi Başarısız!");
                    }
                });
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}