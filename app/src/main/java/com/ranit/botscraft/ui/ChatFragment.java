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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.ConversationAdapter;
import com.ranit.botscraft.adapter.MyBotsAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.Conversation;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.utils.EncryptionHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
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
    private RecyclerView rvConversations, rvMyBots;
    private TextView tvEmpty, tvMyBotsTitle;
    private View skeletonChat;
    private NestedScrollView nestedScrollView;
    
    private ConversationAdapter adapter;
    private MyBotsAdapter myBotsAdapter;
    
    private final List<Conversation> conversationList = new ArrayList<>();
    private final List<Bot> myBotsList = new ArrayList<>();
    private final Map<String, Bot> botCache = new HashMap<>();
    
    private ListenerRegistration chatListener;
    private ListenerRegistration userListener;
    private User currentUser;

    // Pagination for My Bots
    private boolean isMyBotsLoading = false;
    private boolean isMyBotsLastPage = false;
    private DocumentSnapshot myBotsLastVisible;
    private static final int PAGE_SIZE = 10;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvConversations = view.findViewById(R.id.rvConversations);
        rvMyBots = view.findViewById(R.id.rvMyBots);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvMyBotsTitle = view.findViewById(R.id.tvMyBotsTitle);
        skeletonChat = view.findViewById(R.id.skeletonChat);
        nestedScrollView = view.findViewById(R.id.nestedScrollViewChat);

        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.setNestedScrollingEnabled(false); 
        adapter = new ConversationAdapter(conversationList, this);
        rvConversations.setAdapter(adapter);

        rvMyBots.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        myBotsAdapter = new MyBotsAdapter(myBotsList, this::openChat);
        rvMyBots.setAdapter(myBotsAdapter);

        // Lazy loading for My Bots
        rvMyBots.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollHorizontally(1) && !isMyBotsLoading && !isMyBotsLastPage) {
                    loadMyBots(false);
                }
            }
        });

        listenToUserUpdates();
        loadMyBots(true);
    }

    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        userListener = FirebaseManager.getFirestore().collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAdded()) return;
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    currentUser = snapshot.toObject(User.class);
                    if (chatListener == null) {
                        listenToConversations();
                    }
                });
    }

    private void loadMyBots(boolean initial) {
        String uid = FirebaseManager.getUserId();
        if (uid == null || isMyBotsLoading) return;

        isMyBotsLoading = true;
        FirebaseFirestore db = FirebaseManager.getFirestore();
        Query query = db.collection("bots")
                .whereEqualTo("ownerId", uid)
                .limit(PAGE_SIZE);

        if (!initial && myBotsLastVisible != null) {
            query = query.startAfter(myBotsLastVisible);
        }

        query.get().addOnSuccessListener(snapshots -> {
            if (!isAdded()) return;
            if (initial) myBotsList.clear();

            if (!snapshots.isEmpty()) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Bot bot = doc.toObject(Bot.class);
                    if (bot != null) {
                        bot.botId = doc.getId();
                        myBotsList.add(bot);
                    }
                }
                myBotsLastVisible = snapshots.getDocuments().get(snapshots.size() - 1);
                isMyBotsLastPage = snapshots.size() < PAGE_SIZE;
            } else {
                isMyBotsLastPage = true;
            }

            isMyBotsLoading = false;
            updateMyBotsUI();
        }).addOnFailureListener(e -> isMyBotsLoading = false);
    }

    private void updateMyBotsUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                boolean hasBots = !myBotsList.isEmpty();
                if (tvMyBotsTitle != null) tvMyBotsTitle.setVisibility(hasBots ? View.VISIBLE : View.GONE);
                if (rvMyBots != null) rvMyBots.setVisibility(hasBots ? View.VISIBLE : View.GONE);
                if (myBotsAdapter != null) myBotsAdapter.notifyDataSetChanged();
            });
        }
    }

    private void openChat(Bot bot) {
        if (getActivity() == null || bot == null) return;
        bot.sanitizeForIntent(); // Prevent TransactionTooLargeException
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("bot", bot);
        startActivity(intent);
    }

    private void listenToConversations() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        Query query = db.collection("chats")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100); 

        chatListener = query.addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            if (error != null) {
                if (skeletonChat != null) skeletonChat.setVisibility(View.GONE);
                return;
            }
            if (value == null) return;

            if (value.isEmpty()) {
                updateList(new HashMap<>());
                return;
            }

            Map<String, ConversationData> latestMessages = new HashMap<>();
            List<String> botIdsToFetch = new ArrayList<>();

            for (DocumentSnapshot doc : value.getDocuments()) {
                String botId = doc.getString("botId");
                if (botId == null) continue;

                if (currentUser != null && currentUser.blockedBots != null && currentUser.blockedBots.contains(botId)) continue;

                if (!latestMessages.containsKey(botId)) {
                    String rawMsg = doc.getString("text");
                    if (rawMsg == null) rawMsg = doc.getString("reply");
                    
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
                fetchBotsInBatch(botIdsToFetch, latestMessages);
            }
        });
    }

    private void fetchBotsInBatch(List<String> botIds, Map<String, ConversationData> latestMessages) {
        if (botIds.isEmpty()) {
            updateList(latestMessages);
            return;
        }

        FirebaseFirestore db = FirebaseManager.getFirestore();
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < botIds.size(); i += 10) {
            batches.add(botIds.subList(i, Math.min(i + 10, botIds.size())));
        }

        AtomicInteger completedBatches = new AtomicInteger(0);
        for (List<String> batch : batches) {
            db.collection("bots").whereIn(FieldPath.documentId(), batch).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Bot bot = doc.toObject(Bot.class);
                        if (bot != null) {
                            bot.botId = doc.getId();
                            botCache.put(bot.botId, bot);
                        }
                    }
                }
                if (completedBatches.incrementAndGet() == batches.size()) {
                    updateList(latestMessages);
                }
            });
        }
    }

    private void updateList(Map<String, ConversationData> latestMessages) {
        List<Conversation> newList = new ArrayList<>();
        for (Map.Entry<String, ConversationData> entry : latestMessages.entrySet()) {
            Bot bot = botCache.get(entry.getKey());
            if (bot != null && (currentUser == null || currentUser.blockedBots == null || !currentUser.blockedBots.contains(bot.botId))) {
                newList.add(new Conversation(bot, entry.getValue().message, entry.getValue().timestamp));
            }
        }
        Collections.sort(newList, (c1, c2) -> Long.compare(c2.lastTimestamp, c1.lastTimestamp));

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                conversationList.clear();
                conversationList.addAll(newList);
                if (skeletonChat != null) skeletonChat.setVisibility(View.GONE);
                if (rvConversations != null) rvConversations.setVisibility(View.VISIBLE);

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
        openChat(bot);
    }

    @Override
    public void onProfileClick(Bot bot) {
        if (bot == null) return;
        bot.sanitizeForIntent(); // Prevent TransactionTooLargeException
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
