package com.example.playmate.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.ItemUserBinding;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> userList;
    private final OnUserClickListener listener;

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;
        private final OnUserClickListener listener;

        public UserViewHolder(ItemUserBinding binding, OnUserClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        public void bind(User user) {
            binding.textViewUsername.setText(user.getUsername());
            
            // Handle multiple locations
            String locations = user.getLocations();
            if (locations != null && !locations.isEmpty()) {
                String[] locationArray = locations.split(",");
                if (locationArray.length > 0) {
                    // Show first location as primary
                    binding.textViewLocation.setText(locationArray[0].trim());
                } else {
                    binding.textViewLocation.setText("Belirtilmemiş");
                }
            } else {
                binding.textViewLocation.setText("Belirtilmemiş");
            }

            // Split CSV and get first game
            String favs = user.getFavoriteGame(); // e.g. "REPO,Counter Strike,…"
            String firstGame = "";
            if (favs != null && !favs.isEmpty()) {
                String[] parts = favs.split(",");
                firstGame = parts[0].trim();
            }

            // Find and set drawable resource
            int iconRes = getIconResFor(firstGame);
            binding.imageViewGameIcon.setImageResource(iconRes);

            // Handle click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }

        @DrawableRes
        private static int getIconResFor(String gameName) {
            switch (gameName) {
                case "League of Legends":
                    return R.drawable.ic_league_of_legends;
                case "Valorant":
                    return R.drawable.ic_valorant;
                case "Counter Strike":
                    return R.drawable.ic_counter_strike;
                case "Minecraft":
                    return R.drawable.ic_minecraft;
                case "REPO":
                    return R.drawable.ic_repo;
                case "Delta Force":
                    return R.drawable.ic_delta_force;
                case "Call of Duty":
                    return R.drawable.ic_call_of_duty;
                default:
                    return R.drawable.ic_default_game;
            }
        }
    }
}
