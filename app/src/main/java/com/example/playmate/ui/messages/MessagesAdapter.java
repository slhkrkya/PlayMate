package com.example.playmate.ui.messages;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.ChatMessage;
import com.example.playmate.data.model.User;
import com.example.playmate.databinding.ItemConversationBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ConversationViewHolder> {

    private List<Conversation> conversationList = new ArrayList<>();
    private OnConversationClickListener listener;
    private String currentUserId;
    private DatabaseReference usersRef;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public MessagesAdapter(String currentUserId, OnConversationClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    public void setConversationList(List<Conversation> conversationList) {
        this.conversationList = conversationList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ConversationViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.bind(conversation, currentUserId, usersRef);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;
        private final OnConversationClickListener listener;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private Conversation currentConversation; // Konuşma objesini tutmak için

        ConversationViewHolder(ItemConversationBinding binding, OnConversationClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && currentConversation != null) {
                    listener.onConversationClick(currentConversation);
                }
            });
        }

        void bind(Conversation conversation, String currentUserId, DatabaseReference usersRef) {
            this.currentConversation = conversation; // Konuşma objesini kaydet

            binding.textViewLastMessage.setText(conversation.getLastMessage());
            binding.textViewTimestamp.setText(dateFormat.format(conversation.getTimestamp()));

            // Diğer kullanıcının ID'sini bul
            String otherUserId = conversation.getChatRoomId().replace(currentUserId, "").replace("_", "");

            // Diğer kullanıcının bilgilerini çek ve göster
            usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User otherUser = snapshot.getValue(User.class);
                    if (otherUser != null) {
                        binding.textViewUsername.setText(otherUser.getUsername());
                        // Profil resmini yükle
                        String profileImageUrl = otherUser.getProfileImageUrl();
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(profileImageUrl, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    binding.imageViewProfile.setImageBitmap(bitmap);
                                } else {
                                     binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                                }
                            } catch (IllegalArgumentException e) {
                                binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                            }
                        } else {
                             binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Kullanıcı bilgileri çekilemezse varsayılanları göster
                    binding.textViewUsername.setText("Bilinmeyen Kullanıcı");
                    binding.imageViewProfile.setImageResource(R.drawable.ic_defaultprofile);
                }
            });
        }
    }

    // Konuşma verilerini tutmak için yardımcı sınıf
    public static class Conversation {
        private String chatRoomId;
        private String lastMessage;
        private long timestamp;

        public Conversation() {
            // Boş constructor Firebase için gerekli
        }

        public Conversation(String chatRoomId, String lastMessage, long timestamp) {
            this.chatRoomId = chatRoomId;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
        }

        public String getChatRoomId() {
            return chatRoomId;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
} 