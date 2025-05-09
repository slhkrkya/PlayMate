package com.example.playmate.ui.auth;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        binding.buttonRegister.setOnClickListener(v -> {
            String email           = binding.editTextEmail.getText().toString().trim();
            String password        = binding.editTextPassword.getText().toString();
            String passwordConfirm = binding.editTextPasswordConfirm.getText().toString();

            // 1) Boş alan kontrolü
            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(getContext(), "Lütfen tüm alanları doldurunuz.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) Şifre eşleşme kontrolü
            if (!password.equals(passwordConfirm)) {
                binding.editTextPasswordConfirm.setError("Şifreler eşleşmiyor");
                binding.editTextPasswordConfirm.requestFocus();
                return;
            }

            // 3) Kayıt isteği
            viewModel.register(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(getContext(), "Kayıt başarılı!", Toast.LENGTH_SHORT).show();

                        // FirebaseUser'dan UID al, kullanıcı veritabanına kaydet
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference userRef = FirebaseDatabase
                                .getInstance()
                                .getReference("users")
                                .child(uid);

                        // User modeli: User(String uid, String username, String email, String favoriteGame, String location, String profileImageUrl)
                        User user = new User(uid, null, email, null, null,null);
                        userRef.setValue(user);

                        // Giriş ekranına dön
                        Navigation.findNavController(binding.getRoot())
                                .navigate(R.id.action_registerFragment_to_loginFragment);
                    })
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Hata")
                                .setMessage(e.getMessage())
                                .setPositiveButton("Tamam", null)
                                .show();
                    });
        });

        binding.textViewGoToLogin.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_registerFragment_to_loginFragment);
        });

        return binding.getRoot();
    }
}
