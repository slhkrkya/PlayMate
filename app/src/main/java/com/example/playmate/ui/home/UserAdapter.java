package com.example.playmate.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, favoriteGameText, locationText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.textViewUsername);
            favoriteGameText = itemView.findViewById(R.id.textViewGame);
            locationText = itemView.findViewById(R.id.textViewLocation);
        }

        public void bind(User user, OnUserClickListener listener) {
            usernameText.setText(user.getUsername());
            favoriteGameText.setText("Favori Oyun: " + user.getFavoriteGame());
            locationText.setText("Konum: " + user.getLocation());

            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
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
}