package com.ranit.botscraft.repository;

import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.google.firebase.firestore.CollectionReference;

public class BotRepository {

    private static CollectionReference botRef =
            FirebaseManager.getFirestore().collection("bots");

    public static void createBot(Bot bot, Callback callback) {
        bot.ownerId = FirebaseManager.getUserId();
        bot.maxTokens = 100; // free plan

        botRef.add(bot)
                .addOnSuccessListener(r -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public interface Callback {
        void onComplete(boolean success);
    }
}
