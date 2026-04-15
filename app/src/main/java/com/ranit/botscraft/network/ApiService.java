package com.ranit.botscraft.network;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface ApiService {

    // =====================================
    // 💬 CHAT API
    // =====================================
    @POST("chatWithBot")
    Call<ChatResponse> chatWithBot(
            @Header("Authorization") String token,
            @Body ChatRequest body
    );

    // =====================================
    // 🎨 IMAGE GENERATION
    // =====================================
    @POST("generateImage")
    Call<ImageResponse> generateImage(
            @Header("Authorization") String token,
            @Body ImageRequest body
    );

    // =====================================
    // 🔊 VOICE API
    // =====================================
    @POST("getVoices")
    Call<VoiceListResponse> getVoices(
            @Header("Authorization") String token
    );

    @POST("textToSpeech")
    Call<AudioResponse> textToSpeech(
            @Header("Authorization") String token,
            @Body TextToSpeechRequest body
    );

    @POST("speechToSpeech")
    Call<AudioResponse> speechToSpeech(
            @Header("Authorization") String token,
            @Body SpeechToSpeechRequest body
    );

    // =====================================
    // 💳 SUBSCRIPTION & CREDITS
    // =====================================
    @POST("upgradePlan")
    Call<UpgradeResponse> upgradePlan(
            @Header("Authorization") String token,
            @Body UpgradePlanRequest body
    );

    @POST("verifyPurchase")
    Call<PurchaseVerificationResponse> verifyPurchase(
            @Header("Authorization") String token,
            @Body PurchaseVerificationRequest body
    );

    @POST("rewardCredit")
    Call<ResponseBody> rewardCredit(
            @Header("Authorization") String token
    );

    // =====================================
    // 🎮 GAMING API
    // =====================================
    @POST("startGame")
    Call<GameStartResponse> startGame(
            @Header("Authorization") String token,
            @Body GameStartRequest body
    );

    @POST("submitGame")
    Call<GameSubmitResponse> submitGame(
            @Header("Authorization") String token,
            @Body GameSubmitRequest body
    );

    @POST("spinWheel")
    Call<SpinResponse> spinWheel(
            @Header("Authorization") String token
    );
}
