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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(userList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView locationText;
        private final ImageView gameIcon;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.textViewUsername);
            locationText = itemView.findViewById(R.id.textViewLocation);
            gameIcon     = itemView.findViewById(R.id.imageViewGameIcon);
        }

        public void bind(User user, OnUserClickListener listener) {
            // Username & Location
            usernameText.setText(user.getUsername());
            locationText.setText(user.getLocation());

            // Split CSV and get first game
            String favs = user.getFavoriteGame(); // e.g. "REPO,Counter Strike,…"
            String firstGame = "";
            if (favs != null && !favs.isEmpty()) {
                String[] parts = favs.split(",");
                firstGame = parts[0].trim();
            }

            // Find and set drawable resource
            int iconRes = getIconResFor(firstGame);
            gameIcon.setImageResource(iconRes);

            // Handle click
            itemView.setOnClickListener(v -> listener.onUserClick(user));
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
