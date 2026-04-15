package com.ranit.botscraft.network;

public class GameSubmitRequest {
    public String gameId;
    public Object answer;

    public GameSubmitRequest(String gameId, Object answer) {
        this.gameId = gameId;
        this.answer = answer;
    }
}
