package com.example.playmate.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private UserAdapter adapter;
    private List<User> userList;

    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Giriş yapan kullanıcının UID'sini al
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firebase'ten 'users' referansını al
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // RecyclerView için yapı kur
        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, user -> {
            // Şimdilik kullanıcıya tıklanınca Toast göster
            Toast.makeText(getContext(), user.getUsername() + " seçildi", Toast.LENGTH_SHORT).show();
        });

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(adapter);

        // Kullanıcı verilerini çek
        getUsersFromFirebase();

        return binding.getRoot();
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
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Veri alınamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}