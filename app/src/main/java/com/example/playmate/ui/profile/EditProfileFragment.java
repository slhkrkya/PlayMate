package com.example.playmate.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentEditProfileBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private Uri selectedImageUri;
    private List<String> selectedGames = new ArrayList<>();
    private String selectedLocation = "";
    private static final String[] GAMES = {
            "League of Legends",
            "Valorant",
            "Counter Strike",
            "Minecraft",
            "REPO",
            "Delta Force",
            "Call of Duty"
    };

    private static final String[] LOCATIONS = {
            "Türkiye",
            "EU",
            "NA"
    };

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

        // Initialize Spinner for games
        ArrayAdapter<String> gamesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                GAMES
        );
        gamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFavoriteGames.setAdapter(gamesAdapter);

        // Initialize Spinner for locations
        ArrayAdapter<String> locationsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                LOCATIONS
        );
        locationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLocation.setAdapter(locationsAdapter);

        // Handle game selection
        binding.spinnerFavoriteGames.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGame = GAMES[position];
                if (!selectedGames.contains(selectedGame)) {
                    selectedGames.add(selectedGame);
                    addGameChip(selectedGame);
                    updateSpinnerTitle();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Handle location selection
        binding.spinnerLocation.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocation = LOCATIONS[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Profil Resmi Seçme
        binding.buttonChangeImage.setOnClickListener(v -> openImagePicker());

        // Profil Kaydetme
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());

        // Load existing data
        loadExistingData();

        return binding.getRoot();
    }

    private void loadExistingData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        
        userRef.get().addOnSuccessListener(snapshot -> {
            User currentUser = snapshot.getValue(User.class);
            if (currentUser != null) {
                // Load games
                if (currentUser.getFavoriteGame() != null) {
                    String[] existingGames = currentUser.getFavoriteGame().split(",");
                    for (String game : existingGames) {
                        if (!game.trim().isEmpty()) {
                            selectedGames.add(game.trim());
                            addGameChip(game.trim());
                        }
                    }
                    updateSpinnerTitle();
                }

                // Load location
                if (currentUser.getLocation() != null) {
                    selectedLocation = currentUser.getLocation();
                    int locationIndex = Arrays.asList(LOCATIONS).indexOf(selectedLocation);
                    if (locationIndex != -1) {
                        binding.spinnerLocation.setSelection(locationIndex);
                    }
                }
            }
        });
    }

    private void addGameChip(String game) {
        Chip chip = new Chip(requireContext());
        chip.setText(game);
        chip.setCloseIconVisible(true);
        
        // Make close icon more visible
        chip.setCloseIconSize(getResources().getDimension(R.dimen.chip_close_icon_size));
        chip.setCloseIconTint(ContextCompat.getColorStateList(requireContext(), android.R.color.white));
        
        // Chip background
        chip.setChipBackgroundColorResource(R.color.lapis_lazuli);
        
        // Text color
        chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        
        // Add padding to make chip more spacious
        chip.setChipMinHeight(getResources().getDimension(R.dimen.chip_min_height));
        chip.setPadding(
            getResources().getDimensionPixelSize(R.dimen.chip_padding_horizontal),
            getResources().getDimensionPixelSize(R.dimen.chip_padding_vertical),
            getResources().getDimensionPixelSize(R.dimen.chip_padding_horizontal),
            getResources().getDimensionPixelSize(R.dimen.chip_padding_vertical)
        );

        chip.setOnCloseIconClickListener(v -> {
            selectedGames.remove(game);
            binding.chipGroupSelectedGames.removeView(chip);
            updateSpinnerTitle();
        });

        binding.chipGroupSelectedGames.addView(chip);
    }

    private void updateSpinnerTitle() {
        if (selectedGames.isEmpty()) {
            binding.spinnerFavoriteGames.setPrompt("Favori Oyunlar Seçin");
        } else {
            binding.spinnerFavoriteGames.setPrompt(selectedGames.size() + " oyun seçildi");
        }
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

            // EditText'lerden veri al (boş değilse güncelle)
            String newUsername = binding.editTextUsername.getText().toString().trim();
            String finalUsername = newUsername.isEmpty() ? currentUser.getUsername() : newUsername;

            // Seçili oyunları virgülle ayırarak birleştir
            String selectedGamesString = String.join(",", selectedGames);

            // Yeni kullanıcı nesnesi
            User updatedUser = new User(
                    currentUser.getUid(),
                    finalUsername,
                    currentUser.getEmail(),
                    selectedGamesString,
                    selectedLocation,
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