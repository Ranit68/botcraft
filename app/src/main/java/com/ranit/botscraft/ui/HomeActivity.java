package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.BotAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    RecyclerView recyclerBots;
    BotAdapter adapter;
    List<Bot> botList = new ArrayList<>();
    ListenerRegistration botListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        FirebaseManager.getAuth()
                .getCurrentUser()
                .getIdToken(true)
                .addOnSuccessListener(result -> {
                    android.util.Log.d("TOKEN", result.getToken());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("TOKEN", "Token error", e);
                });




        recyclerBots = findViewById(R.id.recyclerBots);
        recyclerBots.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BotAdapter(botList, new BotAdapter.OnBotClickListener() {
            @Override
            public void onChatClick(Bot bot) {
                if (bot != null) {
                    bot.sanitizeForIntent();
                    Intent i = new Intent(HomeActivity.this, ChatActivity.class);
                    i.putExtra("bot", bot);
                    startActivity(i);
                }
            }

            @Override
            public void onProfileClick(Bot bot) {
                if (bot != null) {
                    bot.sanitizeForIntent();
                    Intent i = new Intent(HomeActivity.this, BotProfileActivity.class);
                    i.putExtra("bot", bot);
                    startActivity(i);
                }
            }
        });
        recyclerBots.setAdapter(adapter);

        findViewById(R.id.fabCreateBot)
                .setOnClickListener(v -> checkBotCreationPermission());

        loadBots();
    }

    private void loadBots() {

        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        botListener = FirebaseManager.getFirestore()
                .collection("bots")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null) return;

                    botList.clear();
                    snap.getDocuments().forEach(doc -> {
                        Bot bot = doc.toObject(Bot.class);
                        if (bot != null) {
                            bot.botId = doc.getId();
                            botList.add(bot);
                        }
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    private void checkBotCreationPermission() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        FirebaseManager.getFirestore()
                .collection("bots")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(botSnapshot -> {

                    int botCount = botSnapshot.size();

                    FirebaseManager.getFirestore()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                if (!doc.exists()) {
                                    createDefaultUser(uid, botCount);
                                    return;
                                }

                                User user = doc.toObject(User.class);
                                if (user == null || user.plan == null) {
                                    createDefaultUser(uid, botCount);
                                    return;
                                }

                                if (!canCreateBot(user.plan, botCount)) {
                                    Toast.makeText(this,
                                            "Upgrade required",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    startActivity(
                                            new Intent(this, CreateBotStep1Activity.class)
                                    );
                                }
                            });
                });
    }

    private void createDefaultUser(String uid, int botCount) {
        User user = new User();
        user.uid = uid;
        user.plan = "free";
        user.createdAt = System.currentTimeMillis();

        FirebaseManager.getFirestore()
                .collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(v -> {
                    if (botCount < 1) {
                        startActivity(
                                new Intent(this, CreateBotStep1Activity.class)
                        );
                    }
                });
    }
    private boolean canCreateBot(String plan, int count) {
        if (plan == null) plan = "free";
        if (plan.equals("free")) return count < 1;
        if (plan.equals("premium")) return count < 3;
        return true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (botListener != null) botListener.remove();
    }
}
