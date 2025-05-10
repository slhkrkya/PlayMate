package com.example.playmate.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // 1) Firebase'den kullanıcıyı çek ve profil bilgilerini göster
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                User user = snap.getValue(User.class);
                if (user == null) return;

                // PROFİL FOTOĞRAFI (Base64)
                String base64 = user.getProfileImageUrl();
                if (base64 != null && !base64.trim().isEmpty()) {
                    try {
                        byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        binding.imageViewProfile.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                    }
                } else {
                    binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                }

                // Kullanıcı Adı
                binding.textUsername.setText("Kullanıcı Adı: " + user.getUsername());

                // Email
                binding.textEmail.setText("Email: " + user.getEmail());

                // Konumlar
                if (user.getLocations() != null && !user.getLocations().isEmpty()) {
                    StringBuilder locationsText = new StringBuilder("Konumlar: ");
                    String[] locations = user.getLocations().split(",");
                    for (int i = 0; i < locations.length; i++) {
                        locationsText.append(locations[i].trim());
                        if (i < locations.length - 1) {
                            locationsText.append(", ");
                        }
                    }
                    binding.textLocation.setText(locationsText.toString());
                } else {
                    binding.textLocation.setText("Konumlar: Belirtilmemiş");
                }

                // Favori Oyunlar
                if (user.getFavoriteGame() != null && !user.getFavoriteGame().isEmpty()) {
                    StringBuilder gamesText = new StringBuilder("Favori Oyunlar: ");
                    String[] games = user.getFavoriteGame().split(",");
                    for (int i = 0; i < games.length; i++) {
                        gamesText.append(games[i].trim());
                        if (i < games.length - 1) {
                            gamesText.append(", ");
                        }
                    }
                    binding.textFavoriteGame.setText(gamesText.toString());
                } else {
                    binding.textFavoriteGame.setText("Favori Oyunlar: Belirtilmemiş");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Veri yüklenirken hata: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 2) Profili düzenle ekranına geçiş
        binding.buttonEditProfile.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_nav_profile_to_editProfileFragment)
        );

        return binding.getRoot();
    }
}