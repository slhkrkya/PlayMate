package com.example.playmate.ui.messages;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playmate.R;
import com.example.playmate.data.model.ChatMessage;
import com.example.playmate.databinding.FragmentMessagesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.playmate.ui.messages.MessagesAdapter.Conversation;

public class MessagesFragment extends Fragment implements MessagesAdapter.OnConversationClickListener {

    private static final String TAG = "MessagesFragment";
    private FragmentMessagesBinding binding;
    private MessagesAdapter adapter;
    private List<Conversation> conversationList = new ArrayList<>();
    private Map<String, Conversation> conversationMap = new HashMap<>();
    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Kullanıcı oturum açmamışsa bir şey yapma veya login ekranına yönlendir
            binding.textViewNoConversations.setVisibility(View.VISIBLE);
            binding.textViewNoConversations.setText("Mesajları görmek için giriş yapmalısınız.");
            return binding.getRoot();
        }

        currentUserId = currentUser.getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");

        setupRecyclerView();
        listenForConversations();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new MessagesAdapter(currentUserId, this);
        binding.recyclerViewConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewConversations.setAdapter(adapter);
    }

    private void listenForConversations() {
        messagesListener = messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationMap.clear();
                
                for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatRoomSnapshot.getKey();

                    // Sadece mevcut kullanıcının dahil olduğu sohbet odalarını işle
                    if (chatRoomId != null && chatRoomId.contains(currentUserId)) {
                        // Son mesajı bulmak için chat odasındaki tüm mesajları al
                        List<ChatMessage> messagesInChatRoom = new ArrayList<>();
                        for (DataSnapshot messageSnapshot : chatRoomSnapshot.getChildren()) {
                            ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                            if (message != null) {
                                messagesInChatRoom.add(message);
                            }
                        }

                        // Mesajları zaman damgasına göre sırala (en son mesaj en sonda)
                        Collections.sort(messagesInChatRoom, Comparator.comparingLong(ChatMessage::getTimestamp));

                        if (!messagesInChatRoom.isEmpty()) {
                            ChatMessage lastMessage = messagesInChatRoom.get(messagesInChatRoom.size() - 1);
                            conversationMap.put(chatRoomId, new Conversation(chatRoomId, lastMessage.getMessage(), lastMessage.getTimestamp()));
                        }
                    }
                }

                // Map'teki konuşmaları listeye çevir ve zaman damgasına göre sırala (en son konuşma en üstte)
                conversationList.clear();
                conversationList.addAll(conversationMap.values());
                Collections.sort(conversationList, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

                adapter.setConversationList(conversationList);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Mesajlar çekilirken hata: " + error.getMessage());
                // Hata durumunda boş durumu göster
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
         if (binding == null) return; // Fragment detached olabilir

        if (conversationList.isEmpty()) {
            binding.textViewNoConversations.setVisibility(View.VISIBLE);
            binding.recyclerViewConversations.setVisibility(View.GONE);
        } else {
            binding.textViewNoConversations.setVisibility(View.GONE);
            binding.recyclerViewConversations.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        // Tıklanan konuşmanın chatRoomId'sini al ve ChatFragment'e geç
        Bundle bundle = new Bundle();
        bundle.putString("chatRoomId", conversation.getChatRoomId());

        // Navigation Component ile ChatFragment'e geç
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_messagesFragment_to_chatFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Listener'ı temizle
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        binding = null;
    }

    // Yardımcı: İki kullanıcı için ortak chat odası ID'si üret (Diğer yerlerdeki ile aynı mantıkta olmalı)
    // ChatRepository'de var, burada tekrar tanımlamaya gerek yok
    // private String getChatRoomId(String uid1, String uid2) {
    //     return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    // }
} 