package com.example.playmate.ui.friends;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.data.model.User;
import com.example.playmate.databinding.ItemFriendBinding;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<User> friendsList;

    public FriendsAdapter(List<User> friendsList) {
        this.friendsList = friendsList;
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
        holder.bind(friend);
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

        void bind(User friend) {
            binding.textViewUsername.setText(friend.getUsername());
            binding.textViewFavoriteGame.setText(friend.getFavoriteGame());
            // TODO: Load profile image if available
        }
    }
} 