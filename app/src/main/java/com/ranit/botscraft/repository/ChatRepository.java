package com.ranit.botscraft.repository;

import com.ranit.botscraft.model.ChatMessage;
import com.ranit.botscraft.utils.EncryptionHelper;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private static DatabaseReference getChatRef(String uid, String botId) {
        return FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(uid)
                .child(botId);
    }

    public static void sendMessage(String uid, String botId, ChatMessage message) {
        // Encrypt the message text before sending to Realtime Database
        ChatMessage encryptedMsg = new ChatMessage(message.role, EncryptionHelper.encrypt(message.text), message.imageUrl);
        encryptedMsg.timestamp = message.timestamp;
        getChatRef(uid, botId).push().setValue(encryptedMsg);
    }

    public static ValueEventListener listenMessages(
            String uid,
            String botId,
            ChatCallback callback
    ) {
        DatabaseReference ref = getChatRef(uid, botId);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<ChatMessage> messages = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage msg = child.getValue(ChatMessage.class);
                    if (msg != null) {
                        // Decrypt the message text for local use
                        msg.text = EncryptionHelper.decrypt(msg.text);
                        messages.add(msg);
                    }
                }

                callback.onUpdate(messages);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        ref.orderByChild("timestamp").addValueEventListener(listener);

        return listener;
    }

    public interface ChatCallback {
        void onUpdate(List<ChatMessage> messages);
    }
}