package com.example.playmate.ui.friends;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.FriendRequest;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.ItemFriendRequestBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder> {
    private static final int TYPE_PENDING = 1;
    private static final int TYPE_REJECTED = 2;

    private final List<FriendRequest> pendingRequests;
    private final List<FriendRequest> rejectedRequests;
    private final OnRequestActionListener acceptListener;
    private final OnRequestActionListener rejectListener;
    private final OnRequestActionListener resendListener;
    private final DatabaseReference usersRef;

    public interface OnRequestActionListener {
        void onRequestAction(FriendRequest request);
    }

    public FriendRequestsAdapter(
            List<FriendRequest> pendingRequests,
            List<FriendRequest> rejectedRequests,
            OnRequestActionListener acceptListener,
            OnRequestActionListener rejectListener,
            OnRequestActionListener resendListener) {
        this.pendingRequests = pendingRequests;
        this.rejectedRequests = rejectedRequests;
        this.acceptListener = acceptListener;
        this.rejectListener = rejectListener;
        this.resendListener = resendListener;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public int getItemViewType(int position) {
        if (position < pendingRequests.size()) {
            return TYPE_PENDING;
        }
        return TYPE_REJECTED;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendRequestBinding binding = ItemFriendRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequest request;
        boolean isPending = position < pendingRequests.size();
        
        if (isPending) {
            request = pendingRequests.get(position);
            holder.bind(request, true, acceptListener, rejectListener);
        } else {
            request = rejectedRequests.get(position - pendingRequests.size());
            holder.bind(request, false, null, resendListener);
        }

        // Kullanıcı bilgilerini yükle
        loadUserInfo(request.getSenderId(), holder);
    }

    private void loadUserInfo(String userId, RequestViewHolder holder) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    holder.updateUserInfo(user.getUsername(), user.getProfileImageUrl());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hata durumunda kullanıcı adını gösterme
            }
        });
    }

    @Override
    public int getItemCount() {
        return pendingRequests.size() + rejectedRequests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendRequestBinding binding;

        RequestViewHolder(ItemFriendRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendRequest request, boolean isPending,
                 OnRequestActionListener acceptListener,
                 OnRequestActionListener actionListener) {
            
            if (isPending) {
                binding.buttonAction1.setText("Kabul Et");
                binding.buttonAction2.setText("Reddet");
                binding.buttonAction1.setVisibility(View.VISIBLE);
                binding.buttonAction2.setVisibility(View.VISIBLE);
                binding.buttonAction1.setOnClickListener(v -> acceptListener.onRequestAction(request));
                binding.buttonAction2.setOnClickListener(v -> actionListener.onRequestAction(request));
            } else {
                binding.buttonAction1.setText("Tekrar Gönder"); // Bu aslında görünür olmayacak ama metni ayarlayalım
                binding.buttonAction1.setVisibility(View.GONE);
                binding.buttonAction2.setVisibility(View.GONE);
                // Reddedilen istekler için tekrar gönder butonunu gizle
                // binding.buttonAction1.setVisibility(ViewGroup.GONE);
                // binding.buttonAction1.setOnClickListener(v -> actionListener.onRequestAction(request)); // Listener'ı da kaldırmış olduk
            }
        }

        void updateUserInfo(String username, String profileImageUrl) {
            binding.textViewUsername.setText(username + " sizinle arkadaş olmak istiyor");
            // Profil resmini yükle
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(profileImageUrl, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    // Glide veya başka bir resim yükleme kütüphanesi kullanıyorsanız onu kullanın
                    // Eğer kullanmıyorsanız, bu kısmı elle bitmap yükleme ile değiştirebilirsiniz.
                    // Şimdilik Bitmap ile yüklüyorum:
                     binding.imageViewSenderProfile.setImageBitmap(bitmap);
                } catch (IllegalArgumentException e) {
                    // Base64 formatı hatalı ise varsayılan resmi göster
                    binding.imageViewSenderProfile.setImageResource(R.drawable.ic_defaultprofile);
                }
            } else {
                // Profil resmi yoksa varsayılan resmi göster
                 binding.imageViewSenderProfile.setImageResource(R.drawable.ic_defaultprofile);
            }
        }
    }
} 