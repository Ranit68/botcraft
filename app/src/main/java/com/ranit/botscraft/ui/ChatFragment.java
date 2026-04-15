package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.ConversationAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.Conversation;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.utils.EncryptionHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatFragment extends Fragment implements ConversationAdapter.OnConversationClickListener {

    private static final String TAG = "ChatFragmentLog";
    private RecyclerView rvConversations;
    private TextView tvEmpty;
    private View skeletonChat;
    private ConversationAdapter adapter;
    private final List<Conversation> conversationList = new ArrayList<>();
    private final Map<String, Bot> botCache = new HashMap<>();
    private ListenerRegistration chatListener;
    private ListenerRegistration userListener;
    private User currentUser;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Initializing views");

        rvConversations = view.findViewById(R.id.rvConversations);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        skeletonChat = view.findViewById(R.id.skeletonChat);

        if (rvConversations == null) {
            Log.e(TAG, "RecyclerView not found in layout!");
            return;
        }

        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(conversationList, this);
        rvConversations.setAdapter(adapter);

        listenToUserUpdates();
    }

    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        userListener = FirebaseManager.getFirestore().collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    currentUser = snapshot.toObject(User.class);
                    // Start/Restart listening to conversations when user data (blocked list) is available
                    if (chatListener == null) {
                        listenToConversations();
                    } else {
                        // Refresh current list based on new blocked status
                        refreshUI();
                    }
                });
    }

    private void listenToConversations() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        Query query = db.collection("chats")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        chatListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                if (skeletonChat != null) skeletonChat.setVisibility(View.GONE);
                return;
            }
            if (value == null) {
                if (skeletonChat != null) skeletonChat.setVisibility(View.GONE);
                return;
            }

            if (value.isEmpty()) {
                updateList(new HashMap<>());
                return;
            }

            Map<String, ConversationData> latestMessages = new HashMap<>();
            List<String> botIdsToFetch = new ArrayList<>();

            for (DocumentSnapshot doc : value.getDocuments()) {
                String botId = doc.getString("botId");
                if (botId == null) continue;

                // Skip if bot is blocked
                if (currentUser != null && currentUser.blockedBots.contains(botId)) continue;

                if (!latestMessages.containsKey(botId)) {
                    String rawMsg = doc.getString("reply");
                    if (rawMsg == null) rawMsg = doc.getString("message");
                    
                    String lastMsg = EncryptionHelper.decrypt(rawMsg);
                    if (lastMsg == null || lastMsg.isEmpty()) {
                        lastMsg = doc.contains("imageUrl") ? "Image" : "";
                    }
                    
                    long timestamp = System.currentTimeMillis();
                    Object createdAtObj = doc.get("createdAt");
                    if (createdAtObj instanceof Timestamp) {
                        timestamp = ((Timestamp) createdAtObj).toDate().getTime();
                    }

                    latestMessages.put(botId, new ConversationData(lastMsg, timestamp));
                    if (!botCache.containsKey(botId)) {
                        botIdsToFetch.add(botId);
                    }
                }
            }

            if (botIdsToFetch.isEmpty()) {
                updateList(latestMessages);
            } else {
                fetchBotsAndRefresh(botIdsToFetch, latestMessages);
            }
        });
    }

    private void refreshUI() {
        // Triggered when user blocks a bot - we need to re-filter the existing messages
        if (chatListener != null) {
            // Simplest way is to let the listener trigger again or manually re-filter
            // but since it's a snapshot listener, we can just wait for next update or
            // for now, we'll let it be as blocking happens in ChatActivity then returns here.
        }
    }

    private void fetchBotsAndRefresh(List<String> botIds, Map<String, ConversationData> latestMessages) {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        AtomicInteger count = new AtomicInteger(0);
        int total = botIds.size();

        for (String bid : botIds) {
            db.collection("bots").document(bid).get().addOnCompleteListener(task -> {
                try {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Bot bot = task.getResult().toObject(Bot.class);
                        if (bot != null) {
                            bot.botId = task.getResult().getId();
                            botCache.put(bid, bot);
                        }
                    }
                } finally {
                    if (count.incrementAndGet() == total) {
                        updateList(latestMessages);
                    }
                }
            });
        }
    }

    private void updateList(Map<String, ConversationData> latestMessages) {
        List<Conversation> newList = new ArrayList<>();
        for (Map.Entry<String, ConversationData> entry : latestMessages.entrySet()) {
            Bot bot = botCache.get(entry.getKey());
            // Double check blocking status
            if (bot != null && (currentUser == null || !currentUser.blockedBots.contains(bot.botId))) {
                newList.add(new Conversation(bot, entry.getValue().message, entry.getValue().timestamp));
            }
        }
        Collections.sort(newList, (c1, c2) -> Long.compare(c2.lastTimestamp, c1.lastTimestamp));

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                conversationList.clear();
                conversationList.addAll(newList);
                if (skeletonChat != null) skeletonChat.setVisibility(View.GONE);
                rvConversations.setVisibility(View.VISIBLE);

                if (tvEmpty != null) {
                    tvEmpty.setVisibility(conversationList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onChatClick(Bot bot) {
        if (bot == null) return;
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("bot", bot);
        startActivity(intent);
    }

    @Override
    public void onProfileClick(Bot bot) {
        if (bot == null) return;
        Intent intent = new Intent(getActivity(), BotProfileActivity.class);
        intent.putExtra("bot", bot);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) chatListener.remove();
        if (userListener != null) userListener.remove();
    }

    private static class ConversationData {
        String message;
        long timestamp;
        ConversationData(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
