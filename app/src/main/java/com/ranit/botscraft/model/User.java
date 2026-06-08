package com.ranit.botscraft.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

    // Firebase
    public String uid;
    public String email;
    public String name;
    public String username;
    public String imageUrl; 
    public String gender; // Added for personalization

    // Subscription
    public String plan; // free | premium | ultra
    public long subscriptionStart;
    public long subscriptionEnd;

    // 🔥 HYBRID SYSTEM
    public int credits; // extra usage credits

    // Usage tracking (daily)
    public int dailyMessageCount;
    public int dailyImageCount;
    public int dailyVoiceCount;

    // Limits (based on plan)
    public int maxBots;
    public int maxMessagesPerDay;
    public int maxImagesPerDay;
    public int maxVoicePerDay;

    // Reset tracking
    public long lastResetDate;

    // Metadata
    public long createdAt;
    public boolean active;

    // Blocking system
    public List<String> blockedBots = new ArrayList<>();

    // Notification settings
    public boolean notificationsEnabled = true; // Default to true

    // Security settings
    public boolean biometricEnabled = false;

    public User() {
    }
}
