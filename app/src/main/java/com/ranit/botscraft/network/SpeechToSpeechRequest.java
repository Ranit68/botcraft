package com.ranit.botscraft.network;

import com.google.gson.annotations.SerializedName;

public class SpeechToSpeechRequest {
    @SerializedName("audioBase64")
    public String audioBase64;
    
    public String voiceId;

    public SpeechToSpeechRequest(String audioBase64, String voiceId) {
        this.audioBase64 = audioBase64;
        this.voiceId = voiceId;
    }
}
