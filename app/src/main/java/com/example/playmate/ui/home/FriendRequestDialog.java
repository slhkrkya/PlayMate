package com.example.playmate.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.playmate.R;
import com.example.playmate.data.model.FriendRequest;
import com.example.playmate.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FriendRequestDialog extends DialogFragment {

    private static final String ARG_REQUEST_ID = "requestId";
    private static final String ARG_SENDER_ID = "senderId";

    private String requestId;
    private String senderId;
    private DatabaseReference friendRequestsRef;
    private DatabaseReference usersRef;

    public static FriendRequestDialog newInstance(String requestId, String senderId) {
        FriendRequestDialog dialog = new FriendRequestDialog();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        args.putString(ARG_SENDER_ID, senderId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
            senderId = getArguments().getString(ARG_SENDER_ID);
        }
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_friend_request, container, false);

        TextView textViewSender = view.findViewById(R.id.textViewSender);
        Button buttonAccept = view.findViewById(R.id.buttonAccept);
        Button buttonReject = view.findViewById(R.id.buttonReject);

        // Gönderen kullanıcının bilgilerini al
        usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User sender = snapshot.getValue(User.class);
                if (sender != null) {
                    textViewSender.setText(sender.getUsername() + " sizinle arkadaş olmak istiyor");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Kullanıcı bilgileri alınamadı", Toast.LENGTH_SHORT).show();
            }
        });

        buttonAccept.setOnClickListener(v -> updateRequestStatus("accepted"));
        buttonReject.setOnClickListener(v -> updateRequestStatus("rejected"));

        return view;
    }

    private void updateRequestStatus(String status) {
        String currentUserId = getCurrentUserId();

        friendRequestsRef.child(requestId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    if ("accepted".equals(status)) {
                        // Her iki kullanıcıya da arkadaş olarak ekle
                        usersRef.child(senderId).child("friends").child(currentUserId).setValue(true);
                        usersRef.child(currentUserId).child("friends").child(senderId).setValue(true);
                    }

                    String message = status.equals("accepted") ? "Arkadaşlık isteği kabul edildi" : "Arkadaşlık isteği reddedildi";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "İşlem başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}