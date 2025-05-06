package com.example.playmate.ui.profile;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // SharedPreferences'ten URI'yi al ve yükle
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_prefs", 0);
        String savedUri = prefs.getString("profile_image_uri", null);
        if (savedUri != null) {
            Uri imageUri = Uri.parse(savedUri);
            Glide.with(requireContext())
                    .load(imageUri)
                    .placeholder(R.drawable.ic_defaultprofile)
                    .into(binding.imageViewProfile);
        } else {
            binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
        }

        // Kullanıcı verilerini çek ve göster
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    binding.textUsername.setText(user.getUsername());
                    binding.textFavoriteGame.setText(user.getFavoriteGame());
                    binding.textEmail.setText(user.getEmail());
                    binding.textLocation.setText(user.getLocation());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Kullanıcı verileri alınamadı.", Toast.LENGTH_SHORT).show();
            }
        });

        // Profili Düzenle butonu
        binding.buttonEditProfile.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_nav_profile_to_editProfileFragment)
        );

        return binding.getRoot();
    }
}
