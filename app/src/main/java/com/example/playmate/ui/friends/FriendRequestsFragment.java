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

import com.example.playmate.R;
import com.example.playmate.data.model.FriendRequest;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.FragmentFriendRequestsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsFragment extends Fragment {
    private static final String TAG = "FriendRequestsFragment";
    private FragmentFriendRequestsBinding binding;
    private FriendRequestsAdapter adapter;
    private List<FriendRequest> pendingRequests;
    private List<FriendRequest> rejectedRequests;
    private DatabaseReference friendRequestsRef;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendRequestsBinding.inflate(inflater, container, false);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        pendingRequests = new ArrayList<>();
        rejectedRequests = new ArrayList<>();

        setupRecyclerView();
        loadFriendRequests();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new FriendRequestsAdapter(
            pendingRequests,
            rejectedRequests,
            this::handleAcceptRequest,
            this::handleRejectRequest,
            this::handleResendRequest
        );

        binding.recyclerViewRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRequests.setAdapter(adapter);
    }

    private void loadFriendRequests() {
        if (!isAdded() || getContext() == null) return;

        // Bekleyen istekleri yükle
        friendRequestsRef.orderByChild("receiverId")
                .equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pendingRequests.clear();
                        rejectedRequests.clear();

                        for (DataSnapshot requestSnap : snapshot.getChildren()) {
                            FriendRequest request = requestSnap.getValue(FriendRequest.class);
                            if (request != null) {
                                if ("pending".equals(request.getStatus())) {
                                    pendingRequests.add(request);
                                } else if ("rejected".equals(request.getStatus())) {
                                    rejectedRequests.add(request);
                                }
                            }
                        }

                        if (isAdded() && getContext() != null) {
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading friend requests: " + error.getMessage());
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "İstekler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleAcceptRequest(FriendRequest request) {
        friendRequestsRef.child(request.getRequestId()).child("status").setValue("accepted")
                .addOnSuccessListener(aVoid -> {
                    // Her iki kullanıcıya da arkadaş olarak ekle
                    usersRef.child(request.getSenderId()).child("friends").child(currentUserId).setValue(true);
                    usersRef.child(currentUserId).child("friends").child(request.getSenderId()).setValue(true);
                    
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Arkadaşlık isteği kabul edildi", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "İşlem başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleRejectRequest(FriendRequest request) {
        friendRequestsRef.child(request.getRequestId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Arkadaşlık isteği reddedildi ve silindi", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "İşlem başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleResendRequest(FriendRequest request) {
        // Önce eski isteği sil
        friendRequestsRef.child(request.getRequestId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Yeni istek oluştur
                    String newRequestId = friendRequestsRef.push().getKey();
                    FriendRequest newRequest = new FriendRequest(currentUserId, request.getReceiverId());
                    newRequest.setRequestId(newRequestId);

                    friendRequestsRef.child(newRequestId).setValue(newRequest.toMap())
                            .addOnSuccessListener(aVoid2 -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Arkadaşlık isteği yeniden gönderildi", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "İstek gönderilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "İşlem başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (pendingRequests.isEmpty() && rejectedRequests.isEmpty()) {
            binding.textViewEmpty.setVisibility(View.VISIBLE);
            binding.recyclerViewRequests.setVisibility(View.GONE);
        } else {
            binding.textViewEmpty.setVisibility(View.GONE);
            binding.recyclerViewRequests.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 