package com.ranit.botscraft.network;

import com.ranit.botscraft.model.ChatMessage;
import java.util.List;

public class ChatRequest {
    public String botId;
    public String message;
    public String systemPrompt;
    public List<ChatMessage> history;

    public ChatRequest(String botId, String message, List<ChatMessage> history) {
        this.botId = botId;
        this.message = message;
        this.history = history;
    }

    public ChatRequest(String botId, String message, String systemPrompt, List<ChatMessage> history) {
        this.botId = botId;
        this.message = message;
        this.systemPrompt = systemPrompt;
        this.history = history;
    }
}
