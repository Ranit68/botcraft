package com.ranit.botscraft.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.utils.NotificationHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Random;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting notification work...");
        
        // Always schedule the next one to keep the cycle going
        NotificationHelper.scheduleNextNotification(getApplicationContext());

        String uid = FirebaseManager.getUserId();
        if (uid == null) return Result.success();

        FirebaseFirestore db = FirebaseManager.getFirestore();

        // Check if user has notifications enabled
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                User user = userDoc.toObject(User.class);
                if (user != null && !user.notificationsEnabled) {
                    Log.d(TAG, "Notifications are disabled for this user. Skipping.");
                    return;
                }
                
                // If enabled, proceed with finding a bot to notify about
                db.collection("chats")
                        .whereEqualTo("userId", uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                String botId = queryDocumentSnapshots.getDocuments().get(0).getString("botId");
                                if (botId != null) {
                                    fetchBotAndNotify(botId, uid);
                                } else {
                                    showGeneralNotification(uid);
                                }
                            } else {
                                showGeneralNotification(uid);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching recent chat", e);
                            showGeneralNotification(uid);
                        });
            }
        });

        return Result.success();
    }

    private void fetchBotAndNotify(String botId, String uid) {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            final String userName = (name != null) ? name : "there";

            db.collection("bots").document(botId).get().addOnSuccessListener(botDoc -> {
                String bName = botDoc.getString("name");
                final String botName = (bName != null) ? bName : "Your companion";

                String[] templates = {
                        "Hey " + userName + ", " + botName + " is waiting for you! ❤️",
                        "New message from " + botName + "? Check it out, " + userName + "!",
                        "Don't keep " + botName + " waiting...",
                        "Hey " + userName + ", " + botName + " misses you! Come back."
                };

                String message = templates[new Random().nextInt(templates.length)];
                NotificationHelper.showNotification(getApplicationContext(), "BotCraft", message);
            });
        });
    }

    private void showGeneralNotification(String uid) {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");
            String greeting = (userName != null) ? "Hey " + userName + "! " : "Hey! ";

            String[] templates = {
                    greeting + "Create your dream AI companion today! ✨",
                    "New bots are waiting to meet you on BotCraft!",
                    greeting + "Start a new conversation now!",
                    "Craft a bot exactly how you want it, " + (userName != null ? userName : "today") + "."
            };
            String message = templates[new Random().nextInt(templates.length)];
            NotificationHelper.showNotification(getApplicationContext(), "BotCraft", message);
        });
    }
}
