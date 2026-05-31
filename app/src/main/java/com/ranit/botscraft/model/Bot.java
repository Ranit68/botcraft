package com.ranit.botscraft.model;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@IgnoreExtraProperties
public class Bot implements Serializable {
    public String botId; 
    public String ownerId;
    public String name;
    public String gender;
    public long age = 18;
    public String hairColor;
    public String hairStyle;
    public String skinTone;
    public String modelType;
    public String bodyType;
    public String relationship;
    public String personality;
    public String description;
    public String systemPrompt;
    public String moodStyle;
    public String occupation;
    public String userName;
    public String language;
    public long maxTokens = 300;
    public String model = "grok-4-1-fast-non-reasoning";
    public boolean memoryEnabled = true;
    public String imageUrl;
    public String chatBackgroundUrl; 
    public long createdAt;
    public boolean active;
    public String voiceId;
    public List<String> categories = new ArrayList<>();
    public Bot() {}
    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : "AI Companion";
    }
    public String getDisplayDescription() {
        if (personality != null && !personality.isEmpty()) return personality;
        return (description != null) ? description : "Digital companion";
    }
    public void sanitizeForIntent() {
        // Prevents TransactionTooLargeException by clearing/truncating large fields before Intent
        if (systemPrompt != null && systemPrompt.length() > 500) systemPrompt = systemPrompt.substring(0, 500);
        if (description != null && description.length() > 500) description = description.substring(0, 500);
        if (personality != null && personality.length() > 500) personality = personality.substring(0, 500);
        if (imageUrl != null && imageUrl.length() > 2000) imageUrl = null;
        if (chatBackgroundUrl != null && chatBackgroundUrl.length() > 2000) chatBackgroundUrl = null;
    }
}