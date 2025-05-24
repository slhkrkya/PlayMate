package com.example.playmate.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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
    private String chatRoomId;

    public ChatFragment() {
        // boş constructor zorunlu
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Navigation Component Safe Args veya Intent Extras ile chatRoomId veya receiverId'yi al
        if (getArguments() != null) {
            chatRoomId = getArguments().getString("chatRoomId");
            String receiverId = getArguments().getString("receiverId"); // FriendsFragment'ten gelebilir

            // Eğer chatRoomId gelmediyse ve receiverId geldiyse, chatRoomId'yi hesapla
            if (chatRoomId == null && receiverId != null) {
                 String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                 if (currentUserId != null) {
                     chatRoomId = getChatRoomId(currentUserId, receiverId);
                     Log.d("ChatFragment", "chatRoomId hesaplandı: " + chatRoomId + " (ReceiverId: " + receiverId + ")");
                 } else {
                     Log.e("ChatFragment", "Mevcut kullanıcı ID null, chatRoomId hesaplanamadı.");
                     // Kullanıcı oturumu açmamışsa veya hata oluştuysa uygun bir aksiyon alınabilir.
                 }
            }
        }
        senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
    }

    // Yardımcı: İki kullanıcı için ortak chat odası ID'si üret (ChatRepository ve MainActivity'deki ile aynı)
    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
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
            Log.d("ChatFragment", "Gönder tuşuna basıldı. Mesaj: " + msg + ", ChatRoomId: " + chatRoomId);

            if (!TextUtils.isEmpty(msg) && chatRoomId != null) {
                // chatRoomId'den receiverId'yi çıkar
                String[] userIds = chatRoomId.split("_");
                String receiverId = userIds[0].equals(senderId) ? userIds[1] : userIds[0];
                Log.d("ChatFragment", "Mesaj gönderiliyor... SenderId: " + senderId + ", ReceiverId: " + receiverId + ", ChatRoomId: " + chatRoomId);

                chatRepository.sendMessage(senderId, receiverId, msg);
                editTextMessage.setText("");
            } else {
                if (TextUtils.isEmpty(msg)) {
                    Log.d("ChatFragment", "Mesaj boş, gönderilmedi.");
                } else if (chatRoomId == null) {
                    Log.e("ChatFragment", "ChatRoomId null, mesaj gönderilemedi.");
                    Toast.makeText(getContext(), "Sohbet odası bilgisi eksik.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Mesajları dinleme
        if (chatRoomId != null && senderId != null) {
            chatRepository.listenForMessages(senderId, chatRoomId, messages -> {
                messageList.clear();
                messageList.addAll(messages);
                chatAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }, true);
        }

        return view;
    }
}
