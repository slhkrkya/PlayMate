package com.example.playmate.ui.friends;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.ItemFriendBinding;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<User> friendsList;
    private final OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onRemoveFriend(User friend);
    }

    public FriendsAdapter(List<User> friendsList, OnFriendActionListener listener) {
        this.friendsList = friendsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FriendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friendsList.get(position);
        holder.bind(friend, listener);
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendBinding binding;

        FriendViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User friend, OnFriendActionListener listener) {
            binding.textViewUsername.setText(friend.getUsername());
            binding.textViewFavoriteGame.setText(friend.getFavoriteGame());

            // Profil resmini Base64'ten çöz ve yükle
            String base64 = friend.getProfileImageUrl();
            if (base64 != null && !base64.trim().isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    binding.imageViewProfile.setImageBitmap(bitmap);
                } catch (Exception e) {
                    binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                }
            } else {
                binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
            }

            // Silme butonu
            binding.buttonRemoveFriend.setOnClickListener(v -> listener.onRemoveFriend(friend));
        }
    }
}
