package com.example.playmate.ui.chat;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.ChatMessage;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messageList;
    private final String currentUserId;

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder.getItemViewType() == TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Yardımcı: Chat Room ID'yi hesapla
    private String getChatRoomId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Gönderilen mesaj görünümü
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageSent);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());

            itemView.setOnLongClickListener(v -> {
                showDeleteDialog(v, message);
                return true;
            });
        }
    }

    // Alınan mesaj görünümü
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageReceived);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());

            if (message.getSenderId().equals(currentUserId)) {
                itemView.setOnLongClickListener(v -> {
                    showDeleteDialog(v, message);
                    return true;
                });
            }
        }
    }

    private void showDeleteDialog(View view, ChatMessage message) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Mesajı Sil")
                .setMessage("Bu mesajı silmek istiyor musunuz?")
                .setPositiveButton("Evet", (dialog, which) -> deleteMessage(message))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void deleteMessage(ChatMessage message) {
        if (message.getMessageId() == null) return;

        String chatRoomId = getChatRoomId(message.getSenderId(), message.getReceiverId());
        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("messages")
                .child(chatRoomId)
                .child(message.getMessageId());

        messageRef.removeValue();
    }
}
