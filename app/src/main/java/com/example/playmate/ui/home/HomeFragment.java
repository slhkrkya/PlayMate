package com.example.playmate.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> filteredUserList;
    private String selectedGame = null;

    private DatabaseReference usersRef;
    private String currentUserId;

    private static final String[] GAMES = {
            "Tümünü Göster",
            "League of Legends",
            "Valorant",
            "Counter Strike",
            "Minecraft",
            "REPO",
            "Delta Force",
            "Call of Duty"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Giriş yapan kullanıcının UID'sini al
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firebase'ten 'users' referansını al
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // RecyclerView için yapı kur
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        adapter = new UserAdapter(filteredUserList, user -> {
            // Şimdilik kullanıcıya tıklanınca Toast göster
            Toast.makeText(getContext(), user.getUsername() + " seçildi", Toast.LENGTH_SHORT).show();
        });

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(adapter);

        // Oyun Arkadaşı Arama butonu için click listener
        binding.buttonFindGamePartner.setOnClickListener(v -> showGameSelectionDialog());

        // Kullanıcı verilerini çek
        getUsersFromFirebase();

        return binding.getRoot();
    }

    private void showGameSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Oyun Seçin");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                GAMES
        );

        builder.setSingleChoiceItems(adapter, -1, (dialog, which) -> {
            if (which == 0) {
                // "Tümünü Göster" seçildi
                selectedGame = null;
                showAllUsers();
            } else {
                selectedGame = GAMES[which];
                filterUsersByGame(selectedGame);
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showAllUsers() {
        filteredUserList.clear();
        filteredUserList.addAll(userList);
        adapter.notifyDataSetChanged();
    }

    private void filterUsersByGame(String game) {
        filteredUserList.clear();
        for (User user : userList) {
            if (user.getFavoriteGame() != null && user.getFavoriteGame().contains(game)) {
                filteredUserList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
        
        if (filteredUserList.isEmpty()) {
            Toast.makeText(getContext(), game + " oyununu oynayan arkadaş bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUsersFromFirebase() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        userList.add(user);
                    }
                }
                // Eğer bir oyun seçiliyse, filtrelemeyi tekrar uygula
                if (selectedGame != null) {
                    filterUsersByGame(selectedGame);
                } else {
                    showAllUsers();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Veri alınamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}