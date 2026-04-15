package com.ranit.botscraft.model;

import java.io.Serializable;

public class Conversation implements Serializable {
    public Bot bot;
    public String lastMessage;
    public long lastTimestamp;

    public Conversation(Bot bot, String lastMessage, long lastTimestamp) {
        this.bot = bot;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }
}
