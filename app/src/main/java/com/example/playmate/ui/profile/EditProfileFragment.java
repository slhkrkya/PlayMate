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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private Uri selectedImageUri;
    private List<String> selectedGames = new ArrayList<>();
    private List<String> selectedLocations = new ArrayList<>();

    private static final String[] GAMES = {
            "Favori oyun seçin...",
            "League of Legends",
            "Valorant",
            "Counter Strike",
            "Minecraft",
            "REPO",
            "Delta Force",
            "Call of Duty"
    };

    private static final String[] LOCATIONS = {
            "Konum seçin...",
            "Türkiye",
            "EU",
            "NA"
    };

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    binding.imageViewProfileEdit.setImageURI(selectedImageUri);

                    SharedPreferences prefs = requireActivity().getSharedPreferences("profile_prefs", 0);
                    prefs.edit().putString("profile_image_uri", selectedImageUri.toString()).apply();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);

        ArrayAdapter<String> gamesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                GAMES
        );
        gamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFavoriteGames.setAdapter(gamesAdapter);

        ArrayAdapter<String> locationsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                LOCATIONS
        );
        locationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLocation.setAdapter(locationsAdapter);

        binding.spinnerFavoriteGames.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;

                String selectedGame = GAMES[position];
                if (!selectedGames.contains(selectedGame)) {
                    selectedGames.add(selectedGame);
                    addGameChip(selectedGame);
                    updateSpinnerTitle();
                }
                binding.spinnerFavoriteGames.setSelection(0); // geri sıfırla
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerLocation.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;

                String selectedLocation = LOCATIONS[position];
                if (!selectedLocations.contains(selectedLocation)) {
                    selectedLocations.add(selectedLocation);
                    addLocationChip(selectedLocation);
                    updateLocationSpinnerTitle();
                }
                binding.spinnerLocation.setSelection(0); // geri sıfırla
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.buttonChangeImage.setOnClickListener(v -> openImagePicker());
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
        loadExistingData();

        return binding.getRoot();
    }

    private void loadExistingData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.get().addOnSuccessListener(snapshot -> {
            User currentUser = snapshot.getValue(User.class);
            if (currentUser != null) {
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

                if (currentUser.getLocations() != null) {
                    String[] existingLocations = currentUser.getLocations().split(",");
                    for (String location : existingLocations) {
                        if (!location.trim().isEmpty()) {
                            selectedLocations.add(location.trim());
                            addLocationChip(location.trim());
                        }
                    }
                    updateLocationSpinnerTitle();
                }
            }
        });
    }

    private void addGameChip(String game) {
        Chip chip = new Chip(requireContext());
        chip.setText(game);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedGames.remove(game);
            binding.chipGroupSelectedGames.removeView(chip);
            updateSpinnerTitle();
        });
        binding.chipGroupSelectedGames.addView(chip);
    }

    private void addLocationChip(String location) {
        Chip chip = new Chip(requireContext());
        chip.setText(location);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedLocations.remove(location);
            binding.chipGroupSelectedLocations.removeView(chip);
            updateLocationSpinnerTitle();
        });
        binding.chipGroupSelectedLocations.addView(chip);
    }

    private void updateSpinnerTitle() {
        if (selectedGames.isEmpty()) {
            binding.spinnerFavoriteGames.setPrompt("Favori Oyun Seçin");
        } else {
            binding.spinnerFavoriteGames.setPrompt("Başka Oyun Ekle");
        }
    }

    private void updateLocationSpinnerTitle() {
        if (selectedLocations.isEmpty()) {
            binding.spinnerLocation.setPrompt("Konum Seçin");
        } else {
            binding.spinnerLocation.setPrompt("Başka Konum Ekle");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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

            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_prefs", 0);
            String imageUri = prefs.getString("profile_image_uri", currentUser.getProfileImageUrl());

            String newUsername = binding.editTextUsername.getText().toString().trim();
            String finalUsername = newUsername.isEmpty() ? currentUser.getUsername() : newUsername;

            String selectedGamesString = String.join(",", selectedGames);
            String selectedLocationsString = String.join(",", selectedLocations);

            User updatedUser = new User(
                    currentUser.getUid(),
                    finalUsername,
                    currentUser.getEmail(),
                    selectedGamesString,
                    selectedLocationsString,
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