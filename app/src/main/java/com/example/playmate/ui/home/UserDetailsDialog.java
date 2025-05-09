package com.example.playmate.ui.home;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.playmate.R;
import com.example.playmate.data.model.User;
import com.google.android.material.button.MaterialButton;

public class UserDetailsDialog extends DialogFragment {

    private User user;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onMessageUser(User user);
        void onAddFriend(User user);
    }

    public static UserDetailsDialog newInstance(User user) {
        UserDetailsDialog dialog = new UserDetailsDialog();
        Bundle args = new Bundle();
        args.putString("username", user.getUsername());
        args.putString("locations", user.getLocations());
        args.putString("favoriteGames", user.getFavoriteGame());
        dialog.setArguments(args);
        dialog.user = user;
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUserActionListener) {
            listener = (OnUserActionListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_user_details, null);

        // Initialize views
        TextView textViewUsername = view.findViewById(R.id.textViewUsername);
        TextView textViewLocation = view.findViewById(R.id.textViewLocation);
        TextView textViewGames = view.findViewById(R.id.textViewGames);
        MaterialButton buttonMessage = view.findViewById(R.id.buttonMessage);
        MaterialButton buttonAddFriend = view.findViewById(R.id.buttonAddFriend);

        // Set user information
        textViewUsername.setText(getArguments().getString("username"));
        
        String locations = getArguments().getString("locations");
        if (locations != null && !locations.isEmpty()) {
            StringBuilder locationsText = new StringBuilder();
            String[] locationArray = locations.split(",");
            for (int i = 0; i < locationArray.length; i++) {
                locationsText.append("• ").append(locationArray[i].trim());
                if (i < locationArray.length - 1) {
                    locationsText.append("\n");
                }
            }
            textViewLocation.setText(locationsText.toString());
        } else {
            textViewLocation.setText("Belirtilmemiş");
        }

        String games = getArguments().getString("favoriteGames");
        if (games != null && !games.isEmpty()) {
            StringBuilder gamesList = new StringBuilder();
            for (String game : games.split(",")) {
                gamesList.append("• ").append(game.trim()).append("\n");
            }
            textViewGames.setText(gamesList.toString());
        } else {
            textViewGames.setText("Henüz oyun eklenmemiş");
        }

        // Set up buttons
        buttonMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageUser(user);
            }
            dismiss();
        });

        buttonAddFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddFriend(user);
            }
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }

    public void setListener(OnUserActionListener listener) {
        this.listener = listener;
    }
} 