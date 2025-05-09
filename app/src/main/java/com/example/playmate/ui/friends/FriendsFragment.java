package com.example.playmate.ui.friends;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Setup RecyclerView
        friendsList = new ArrayList<>();
        adapter = new FriendsAdapter(friendsList);
        binding.recyclerViewFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFriends.setAdapter(adapter);

        // Load friends
        loadFriends();

        return binding.getRoot();
    }

    private void loadFriends() {
        Log.d(TAG, "Loading friends for user: " + currentUserId);
        
        // First, get the current user's data to access their friends list
        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    // Get the friends node
                    DataSnapshot friendsSnapshot = snapshot.child("friends");
                    if (friendsSnapshot.exists()) {
                        friendsList.clear();
                        int totalFriends = (int) friendsSnapshot.getChildrenCount();
                        AtomicInteger loadedFriends = new AtomicInteger(0);
                        
                        Log.d(TAG, "Found " + totalFriends + " friends");
                        
                        for (DataSnapshot friendSnap : friendsSnapshot.getChildren()) {
                            String friendId = friendSnap.getKey();
                            if (friendId != null) {
                                // Get friend's user data
                                usersRef.child(friendId).get().addOnSuccessListener(dataSnapshot -> {
                                    User friend = dataSnapshot.getValue(User.class);
                                    if (friend != null) {
                                        friendsList.add(friend);
                                        int currentLoaded = loadedFriends.incrementAndGet();
                                        Log.d(TAG, "Loaded friend: " + friend.getUsername());
                                        
                                        if (currentLoaded == totalFriends) {
                                            adapter.notifyDataSetChanged();
                                            updateEmptyState();
                                        }
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading friend data: " + e.getMessage());
                                    int currentLoaded = loadedFriends.incrementAndGet();
                                    if (currentLoaded == totalFriends) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                });
                            }
                        }
                    } else {
                        Log.d(TAG, "No friends found");
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