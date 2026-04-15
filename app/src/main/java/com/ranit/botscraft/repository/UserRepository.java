package com.ranit.botscraft.repository;

import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    public static void createUserIfNotExist() {

        String uid = FirebaseManager.getUserId();

        if (uid == null) return;

        FirebaseFirestore db = FirebaseManager.getFirestore();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {

                        User user = new User();

                        // 🔹 Basic
                        user.uid = uid;
                        user.plan = "free";
                        user.active = true;

                        // 🔥 HYBRID SYSTEM INIT
                        user.credits = 20; // 🎁 starter credits

                        user.dailyMessageCount = 0;
                        user.dailyImageCount = 0;
                        user.dailyVoiceCount = 0;

                        // 🔥 PLAN LIMITS (FREE)
                        user.maxBots = 1;
                        user.maxMessagesPerDay = 15;
                        user.maxImagesPerDay = 1;
                        user.maxVoicePerDay = 0;

                        // 🔁 RESET TRACKING
                        user.lastResetDate = System.currentTimeMillis();

                        // 🕒 META
                        user.createdAt = System.currentTimeMillis();

                        db.collection("users")
                                .document(uid)
                                .set(user);
                    }
                });
    }
}