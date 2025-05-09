package com.example.playmate.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playmate.data.model.User;
import com.example.playmate.data.model.FriendRequest;
import com.example.playmate.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment implements UserDetailsDialog.OnUserActionListener {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> filteredUserList;
    private String selectedGame = null;

    private DatabaseReference usersRef;
    private DatabaseReference friendRequestsRef;
    private String currentUserId;
    private ValueEventListener usersListener;
    private ValueEventListener friendRequestsListener;

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

        // Firebase referanslarını al
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests");

        // RecyclerView için yapı kur
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        adapter = new UserAdapter(filteredUserList, user -> {
            UserDetailsDialog dialog = UserDetailsDialog.newInstance(user);
            dialog.setListener(this);
            dialog.show(getChildFragmentManager(), "UserDetailsDialog");
        });

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(adapter);

        // Oyun Arkadaşı Arama butonu için click listener
        binding.buttonFindGamePartner.setOnClickListener(v -> showGameSelectionDialog());

        // Kullanıcı verilerini çek
        getUsersFromFirebase();

        // Gelen arkadaşlık isteklerini dinle
        listenForFriendRequests();

        return binding.getRoot();
    }

    @Override
    public void onMessageUser(User user) {
        // TODO: Implement messaging functionality
        Toast.makeText(getContext(), user.getUsername() + " kullanıcısına mesaj gönderme özelliği yakında eklenecek", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddFriend(User user) {
        // Arkadaşlık isteği gönderme
        sendFriendRequest(user);
    }

    private void sendFriendRequest(User user) {
        // Add log and toast to confirm method is triggered
        Log.d(TAG, "Attempting to send friend request to: " + user.getUid());
        Toast.makeText(getContext(), "Attempting to send friend request...", Toast.LENGTH_SHORT).show();
        // Önce mevcut istekleri kontrol et
        friendRequestsRef.orderByChild("senderId")
                .equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean requestExists = false;
                        for (DataSnapshot requestSnap : snapshot.getChildren()) {
                            FriendRequest request = requestSnap.getValue(FriendRequest.class);
                            if (request != null && request.getReceiverId().equals(user.getUid())) {
                                requestExists = true;
                                break;
                            }
                        }

                        if (requestExists) {
                            Toast.makeText(getContext(), "Bu kullanıcıya zaten istek gönderilmiş", Toast.LENGTH_SHORT).show();
                        } else {
                            // Yeni istek oluştur
                            String requestId = friendRequestsRef.push().getKey();
                            FriendRequest newRequest = new FriendRequest(currentUserId, user.getUid());
                            newRequest.setRequestId(requestId);

                            // Debug için log ekle
                            Log.d(TAG, "Sending friend request: " + newRequest.toMap());

                            friendRequestsRef.child(requestId).setValue(newRequest.toMap())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Arkadaşlık isteği gönderildi", Toast.LENGTH_SHORT).show();
                                        // Debug için log ekle
                                        Log.d(TAG, "Friend request saved successfully with ID: " + requestId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "İstek gönderilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        // Debug için log ekle
                                        Log.e(TAG, "Error saving friend request: " + e.getMessage());
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });
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
        usersListener = new ValueEventListener() {
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
                Log.e(TAG, "Error loading users: " + error.getMessage());
            }
        };
        usersRef.addValueEventListener(usersListener);
    }

    private void listenForFriendRequests() {
        Log.d(TAG, "Starting to listen for friend requests for user: " + currentUserId);

        friendRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Received friend requests update. Count: " + snapshot.getChildrenCount());

                for (DataSnapshot requestSnap : snapshot.getChildren()) {
                    FriendRequest request = requestSnap.getValue(FriendRequest.class);

                    if (request == null) {
                        Log.e(TAG, "Request null! DataSnapshot: " + requestSnap);
                        continue;
                    }

                    if (request.getStatus() == null || request.getSenderId() == null || request.getReceiverId() == null) {
                        Log.e(TAG, "Missing fields in request, skipping.");
                        continue;
                    }

                    Log.d(TAG, "Request found: " + request.toMap());

                    if ("pending".equals(request.getStatus())) {
                        if (!requestSnap.hasChild("shown")) {
                            showFriendRequestDialog(request);
                            requestSnap.getRef().child("shown").setValue(true)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Marked request as shown: " + request.getRequestId())
                                    )
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error marking request as shown: " + e.getMessage())
                                    );
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading friend requests: " + error.getMessage());
            }
        };

        friendRequestsRef.orderByChild("receiverId")
                .equalTo(currentUserId)
                .addValueEventListener(friendRequestsListener);
    }

    private void showFriendRequestDialog(FriendRequest request) {
        FriendRequestDialog dialog = FriendRequestDialog.newInstance(request.getRequestId(), request.getSenderId());
        dialog.show(getChildFragmentManager(), "FriendRequestDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firebase listeners
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (friendRequestsListener != null) {
            friendRequestsRef.removeEventListener(friendRequestsListener);
        }
        binding = null;
    }
}