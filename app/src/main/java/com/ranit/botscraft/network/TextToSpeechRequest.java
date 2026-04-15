package com.ranit.botscraft.network;

public class TextToSpeechRequest {
    public String text;
    public String voiceId;

    public TextToSpeechRequest(String text, String voiceId) {
        this.text = text;
        this.voiceId = voiceId;
    }
}
