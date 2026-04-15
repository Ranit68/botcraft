package com.ranit.botscraft.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.ChatAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.ChatMessage;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.*;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.OnChatInteractionListener {

    private static final String TAG = "CHAT_LOG";
    private static final String INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712";
    
    private static final List<String> RESTRICTED_KEYWORDS = Arrays.asList(
            "porn", "sex", "naked", "xxx", "nude", "pussy", "dick", "cock", "fuck", "blowjob",
            "rape", "incest", "pedophile", "drugs", "cocaine", "heroin", "meth", "suicide", "kill yourself"
    );

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageView imgBotHeader, btnBack, btnMenu;
    private TextView tvBotNameHeader;
    private ConstraintLayout mainLayout;
    private View llInputArea, llLimitArea;
    @Nullable private Bot bot;
    private String botId = "";
    @Nullable private User currentUser;
    @Nullable private ListenerRegistration chatListener;
    @Nullable private ListenerRegistration botListener;
    @Nullable private ListenerRegistration userListener;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private boolean isWaitingForReply = false;
    private InterstitialAd mInterstitialAd;
    private boolean limitAdShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Object botObj = getIntent().getSerializableExtra("bot");
        if (botObj instanceof Bot) {
            bot = (Bot) botObj;
            botId = bot.botId != null ? bot.botId : "";
        }

        if (bot == null || botId.isEmpty()) { finish(); return; }

        initViews();
        listenToUserUpdates();
        setupChat();
        listenToBotUpdates();
        listenToMessages();
        loadWallpaper();
        loadInterstitialAd();
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) { mInterstitialAd = interstitialAd; }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mInterstitialAd = null; }
        });
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainChatLayout);
        recyclerView = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        imgBotHeader = findViewById(R.id.imgBotHeader);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        tvBotNameHeader = findViewById(R.id.tvBotNameHeader);
        llInputArea = findViewById(R.id.llInputArea);
        llLimitArea = findViewById(R.id.llLimitArea);

        updateBotUI();
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Clear Chat");
        popup.getMenu().add("Report Bot");
        popup.getMenu().add("Block Bot");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "Clear Chat": clearChat(); return true;
                case "Report Bot": showReportDialog(); return true;
                case "Block Bot": showBlockConfirmation(); return true;
            }
            return false;
        });
        popup.show();
    }

    private void showReportDialog() {
        String[] reasons = {"Inappropriate content", "Harassment", "Spam", "Illegal content", "Other"};
        new AlertDialog.Builder(this)
                .setTitle("Report Bot")
                .setItems(reasons, (dialog, which) -> submitReport(reasons[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReport(String reason) {
        String uid = FirebaseManager.getUserId();
        if (uid == null || botId.isEmpty()) return;

        Map<String, Object> report = new HashMap<>();
        report.put("userId", uid);
        report.put("botId", botId);
        report.put("reason", reason);
        report.put("timestamp", FieldValue.serverTimestamp());

        FirebaseManager.getFirestore().collection("reports").add(report)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Bot reported. We will review it.", Toast.LENGTH_SHORT).show());
    }

    private void showBlockConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Block Bot")
                .setMessage("Are you sure you want to block this bot?")
                .setPositiveButton("Block", (dialog, which) -> blockBot())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void blockBot() {
        String uid = FirebaseManager.getUserId();
        if (uid == null || botId.isEmpty()) return;

        FirebaseManager.getFirestore().collection("users").document(uid)
                .update("blockedBots", FieldValue.arrayUnion(botId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bot blocked", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void clearChat() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("chats").whereEqualTo("userId", uid).whereEqualTo("botId", botId).get().addOnSuccessListener(qs -> {
            WriteBatch b = db.batch();
            for (DocumentSnapshot doc : qs.getDocuments()) b.delete(doc.getReference());
            b.commit().addOnSuccessListener(a -> {
                messageList.clear();
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        userListener = FirebaseManager.getFirestore().collection("users").document(uid).addSnapshotListener((snap, e) -> {
            if (snap != null && snap.exists()) {
                currentUser = snap.toObject(User.class);
                checkLimits();
            }
        });
    }

    private void checkLimits() {
        if (currentUser == null) return;
        int max = "free".equals(currentUser.plan) ? 5 : ("premium".equals(currentUser.plan) ? 25 : 75);
        boolean canMessage = (currentUser.dailyMessageCount < max) || (currentUser.credits >= 1);
        
        llInputArea.setVisibility(canMessage ? View.VISIBLE : View.GONE);
        llLimitArea.setVisibility(canMessage ? View.GONE : View.VISIBLE);

        if (!canMessage && "free".equals(currentUser.plan) && !limitAdShown) {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(this);
                limitAdShown = true;
                loadInterstitialAd();
            }
        } else if (canMessage) {
            limitAdShown = false;
        }
    }

    private void updateBotUI() {
        if (bot == null) return;
        tvBotNameHeader.setText(bot.getDisplayName());
        if (bot.imageUrl != null) Glide.with(this).load(bot.imageUrl).circleCrop().into(imgBotHeader);
    }

    private void listenToBotUpdates() {
        botListener = FirebaseManager.getFirestore().collection("bots").document(botId).addSnapshotListener((snap, e) -> {
            if (snap != null && snap.exists()) { bot = snap.toObject(Bot.class); updateBotUI(); }
        });
    }

    private void setupChat() {
        adapter = new ChatAdapter(messageList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override public void onVoiceClick(ChatMessage msg) {}
    @Override public void onImageClick(String url) {}

    private void listenToMessages() {
        String uid = FirebaseManager.getUserId();
        chatListener = FirebaseManager.getFirestore().collection("chats")
                .whereEqualTo("userId", uid).whereEqualTo("botId", botId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((val, e) -> {
                    if (val != null) {
                        messageList.clear();
                        boolean hasReplyFromAssistant = false;
                        for (DocumentSnapshot doc : val.getDocuments()) {
                            String role = doc.getString("role");
                            String text = doc.getString("text");
                            if (role != null) {
                                messageList.add(new ChatMessage(role, text));
                                if ("assistant".equals(role)) hasReplyFromAssistant = true;
                            }
                        }
                        if (hasReplyFromAssistant) { isWaitingForReply = false; adapter.setTyping(false); }
                        else if (isWaitingForReply) { adapter.setTyping(true); }
                        else { adapter.setTyping(false); }
                        adapter.notifyDataSetChanged();
                        scrollToBottom();
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() || bot == null || currentUser == null) return;

        for (String word : RESTRICTED_KEYWORDS) {
            if (text.toLowerCase().contains(word)) {
                Toast.makeText(this, "Restricted content.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        isWaitingForReply = true;
        etMessage.setText("");
        adapter.setTyping(true);
        scrollToBottom();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid); data.put("botId", botId); data.put("role", "user"); data.put("text", text); data.put("createdAt", FieldValue.serverTimestamp());
        FirebaseManager.getFirestore().collection("chats").add(data);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(res -> {
            ChatRequest req = new ChatRequest(botId, text, bot.systemPrompt, new ArrayList<>());
            RetrofitClient.getService().chatWithBot("Bearer " + res.getToken(), req).enqueue(new Callback<ChatResponse>() {
                @Override public void onResponse(Call<ChatResponse> c, Response<ChatResponse> r) {
                    if (!r.isSuccessful()) { isWaitingForReply = false; adapter.setTyping(false); }
                }
                @Override public void onFailure(Call<ChatResponse> c, Throwable t) { isWaitingForReply = false; adapter.setTyping(false); }
            });
        });
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void loadWallpaper() {
        if (bot != null && bot.chatBackgroundUrl != null) {
            Glide.with(this).load(bot.chatBackgroundUrl).into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                @Override public void onResourceReady(@NonNull android.graphics.drawable.Drawable r, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> t) { mainLayout.setBackground(r); }
                @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable p) {}
            });
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
        if (botListener != null) botListener.remove();
        if (userListener != null) userListener.remove();
    }
}
