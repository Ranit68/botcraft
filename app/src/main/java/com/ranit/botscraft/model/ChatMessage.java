package com.ranit.botscraft.model;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class ChatMessage implements Serializable {
    public String role; // "user" or "assistant"
    public String text;
    public String imageUrl;
    public long timestamp;
    public boolean isLocked;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String role, String text) {
        this.role = role;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String role, String text, String imageUrl) {
        this.role = role;
        this.text = text;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
    }
}
