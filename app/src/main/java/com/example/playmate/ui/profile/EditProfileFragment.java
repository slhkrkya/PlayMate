package com.example.playmate.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    binding.imageViewProfileEdit.setImageURI(selectedImageUri);

                    // URI'yi SharedPreferences'e kaydet
                    SharedPreferences prefs = requireActivity().getSharedPreferences("profile_prefs", 0);
                    prefs.edit().putString("profile_image_uri", selectedImageUri.toString()).apply();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);

        // Profil Resmi Seçme
        binding.buttonChangeImage.setOnClickListener(v -> openImagePicker());

        // Profil Kaydetme
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());

        return binding.getRoot();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Profil Resmi Seç"));
    }

    private void saveProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.get().addOnSuccessListener(snapshot -> {
            User currentUser = snapshot.getValue(User.class);
            if (currentUser == null) {
                Toast.makeText(getContext(), "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            // SharedPreferences'ten image URI'yi al
            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_prefs", 0);
            String imageUri = prefs.getString("profile_image_uri", currentUser.getProfileImageUrl());

            // EditText’lerden veri al (boş değilse güncelle)
            String newUsername = binding.editTextUsername.getText().toString().trim();
            String newFavoriteGame = binding.editTextFavoriteGame.getText().toString().trim();
            String newLocation = binding.editTextLocation.getText().toString().trim();

            String finalUsername = newUsername.isEmpty() ? currentUser.getUsername() : newUsername;
            String finalFavoriteGame = newFavoriteGame.isEmpty() ? currentUser.getFavoriteGame() : newFavoriteGame;
            String finalLocation = newLocation.isEmpty() ? currentUser.getLocation() : newLocation;

            // Yeni kullanıcı nesnesi
            User updatedUser = new User(
                    currentUser.getUid(),
                    finalUsername,
                    currentUser.getEmail(),
                    finalFavoriteGame,
                    finalLocation,
                    imageUri
            );

            userRef.updateChildren(updatedUser.toMap()).addOnSuccessListener(unused -> {
                Toast.makeText(getContext(), "Profil güncellendi!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(binding.getRoot())
                        .navigate(R.id.action_editProfileFragment_to_homeFragment);
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Güncelleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Veri alınamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}