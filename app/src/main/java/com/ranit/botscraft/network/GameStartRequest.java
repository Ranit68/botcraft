package com.ranit.botscraft.network;

public class GameStartRequest {
    public String gameType;
    public String mode;

    public GameStartRequest(String gameType, String mode) {
        this.gameType = gameType;
        this.mode = mode;
    }
}
