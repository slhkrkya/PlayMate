package com.example.playmate.ui.friends;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentFriendsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FriendsFragment extends Fragment {

    private static final String TAG = "FriendsFragment";
    private FragmentFriendsBinding binding;
    private FriendsAdapter adapter;
    private List<User> friendsList;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        friendsList = new ArrayList<>();

        adapter = new FriendsAdapter(
                friendsList,
                friend -> {
                    // Arkadaşı silmeden önce kullanıcıya sor
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Arkadaşı Sil")
                            .setMessage(friend.getUsername() + " adlı kişiyi silmek istiyor musunuz?")
                            .setPositiveButton("Evet", (dialog, which) -> removeFriend(friend.getUid()))
                            .setNegativeButton("İptal", null)
                            .show();
                },
                friend -> {
                    // Navigation Component ile sohbet başlat (args ile receiverId gönder)
                    Bundle bundle = new Bundle();
                    bundle.putString("receiverId", friend.getUid());

                    NavHostFragment.findNavController(FriendsFragment.this)
                            .navigate(R.id.chatFragment, bundle);
                }
        );

        binding.recyclerViewFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFriends.setAdapter(adapter);

        loadFriends();

        return binding.getRoot();
    }

    private void loadFriends() {
        Log.d(TAG, "Loading friends for user: " + currentUserId);

        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    DataSnapshot friendsSnapshot = snapshot.child("friends");
                    if (friendsSnapshot.exists()) {
                        friendsList.clear();
                        int totalFriends = (int) friendsSnapshot.getChildrenCount();
                        AtomicInteger loadedFriends = new AtomicInteger(0);

                        for (DataSnapshot friendSnap : friendsSnapshot.getChildren()) {
                            String friendId = friendSnap.getKey();
                            if (friendId != null) {
                                usersRef.child(friendId).get().addOnSuccessListener(dataSnapshot -> {
                                    User friend = dataSnapshot.getValue(User.class);
                                    if (friend != null) {
                                        friendsList.add(friend);
                                    }

                                    if (loadedFriends.incrementAndGet() == totalFriends) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading friend: " + e.getMessage());
                                    if (loadedFriends.incrementAndGet() == totalFriends) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                });
                            }
                        }
                    } else {
                        updateEmptyState();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user data: " + error.getMessage());
                Toast.makeText(getContext(), "Arkadaşlar yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void removeFriend(String friendId) {
        DatabaseReference currentUserRef = usersRef.child(currentUserId).child("friends").child(friendId);
        DatabaseReference friendUserRef = usersRef.child(friendId).child("friends").child(currentUserId);

        currentUserRef.removeValue().addOnSuccessListener(unused -> {
            friendUserRef.removeValue().addOnSuccessListener(unused2 -> {
                Toast.makeText(getContext(), "Arkadaş silindi", Toast.LENGTH_SHORT).show();
                loadFriends(); // Listeyi yenile
            });
        });
    }

    private void updateEmptyState() {
        if (friendsList.isEmpty()) {
            binding.textViewNoFriends.setVisibility(View.VISIBLE);
            binding.recyclerViewFriends.setVisibility(View.GONE);
        } else {
            binding.textViewNoFriends.setVisibility(View.GONE);
            binding.recyclerViewFriends.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}