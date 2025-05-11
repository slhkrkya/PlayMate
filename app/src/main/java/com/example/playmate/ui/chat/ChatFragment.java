package com.example.playmate.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playmate.R;
import com.example.playmate.data.model.ChatMessage;
import com.example.playmate.data.repository.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;

    private ChatRepository chatRepository;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private String senderId;
    private String receiverId;

    public ChatFragment() {
        // boş constructor zorunlu
    }

    // newInstance tanımı
    public static ChatFragment newInstance(String receiverId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("receiverId", receiverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            receiverId = getArguments().getString("receiverId");
        }
        senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChat);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);

        chatAdapter = new ChatAdapter(messageList, senderId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatAdapter);

        // Mesaj gönderme
        buttonSend.setOnClickListener(v -> {
            String msg = editTextMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                chatRepository.sendMessage(senderId, receiverId, msg);
                editTextMessage.setText("");
            }
        });

        // Mesajları dinleme
        chatRepository.listenForMessages(senderId, receiverId, messages -> {
            messageList.clear();
            messageList.addAll(messages);
            chatAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageList.size() - 1);
        });

        return view;
    }
}
