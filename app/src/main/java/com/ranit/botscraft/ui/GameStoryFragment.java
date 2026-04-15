package com.ranit.botscraft.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ranit.botscraft.R;
import com.ranit.botscraft.network.GameStartRequest;
import com.ranit.botscraft.network.GameStartResponse;
import com.ranit.botscraft.network.GameSubmitRequest;
import com.ranit.botscraft.network.GameSubmitResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameStoryFragment extends Fragment {

    private static final String TAG = "GameStoryFragment";
    private TextView tvStoryScene;
    private LinearLayout llChoices;
    private Button btnStartStory;
    private ProgressBar progressStory;
    private String selectedMode = "romantic";
    private String currentGameId;
    
    private Button btnRomantic, btnHorror, btnSciFi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_story, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStoryScene = view.findViewById(R.id.tvStoryScene);
        llChoices = view.findViewById(R.id.llChoices);
        btnStartStory = view.findViewById(R.id.btnStartStory);
        progressStory = view.findViewById(R.id.progressStory);
        
        btnRomantic = view.findViewById(R.id.btnModeRomantic);
        btnHorror = view.findViewById(R.id.btnModeHorror);
        btnSciFi = view.findViewById(R.id.btnModeSciFi);

        btnRomantic.setOnClickListener(v -> updateSelectedMode("romantic"));
        btnHorror.setOnClickListener(v -> updateSelectedMode("horror"));
        btnSciFi.setOnClickListener(v -> updateSelectedMode("scifi"));

        btnStartStory.setOnClickListener(v -> {
            Log.d(TAG, "Start Story button clicked. Mode: " + selectedMode);
            startStory();
        });
        
        updateSelectedMode("romantic"); // Set default UI
    }

    private void updateSelectedMode(String mode) {
        selectedMode = mode;
        Log.d(TAG, "Mode selected: " + mode);
        
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.accent_purple);
        int unselectedColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        int selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white);
        int unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.text_main);

        btnRomantic.setBackgroundTintList(ColorStateList.valueOf(mode.equals("romantic") ? selectedColor : unselectedColor));
        btnRomantic.setTextColor(mode.equals("romantic") ? selectedTextColor : unselectedTextColor);
        
        btnHorror.setBackgroundTintList(ColorStateList.valueOf(mode.equals("horror") ? selectedColor : unselectedColor));
        btnHorror.setTextColor(mode.equals("horror") ? selectedTextColor : unselectedTextColor);
        
        btnSciFi.setBackgroundTintList(ColorStateList.valueOf(mode.equals("scifi") ? selectedColor : unselectedColor));
        btnSciFi.setTextColor(mode.equals("scifi") ? selectedTextColor : unselectedTextColor);
    }

    private void startStory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not logged in");
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Log.d(TAG, "Fetching ID token...");
        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            Log.d(TAG, "ID token received. Starting game request...");
            
            RetrofitClient.getService().startGame(token, new GameStartRequest("story", selectedMode))
                    .enqueue(new Callback<GameStartResponse>() {
                        @Override
                        public void onResponse(Call<GameStartResponse> call, Response<GameStartResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "Game started successfully. ID: " + response.body().gameId);
                                currentGameId = response.body().gameId;
                                try {
                                    String scene = response.body().gameData.get("scene").getAsString();
                                    JsonArray choices = response.body().gameData.getAsJsonArray("choices");
                                    displayStory(scene, choices);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing game data: " + e.getMessage());
                                    Toast.makeText(getContext(), "Error loading story data", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Start game failed: " + response.code() + " " + response.message());
                                try {
                                    Log.e(TAG, "Error body: " + response.errorBody().string());
                                } catch (Exception ignored) {}
                                Toast.makeText(getContext(), "Failed to start story: " + response.message(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<GameStartResponse> call, Throwable t) {
                            setLoading(false);
                            Log.e(TAG, "Start game network error", t);
                            Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e -> {
            setLoading(false);
            Log.e(TAG, "Failed to get ID token", e);
            Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void displayStory(String scene, JsonArray choices) {
        tvStoryScene.setText(scene);
        llChoices.removeAllViews();
        Log.d(TAG, "Displaying story with " + choices.size() + " choices.");
        
        for (int i = 0; i < choices.size(); i++) {
            final int index = i;
            Button btn = new Button(getContext());
            btn.setText(choices.get(i).getAsString());
            btn.setAllCaps(false);
            // Optional: add some styling to choice buttons
            btn.setOnClickListener(v -> {
                Log.d(TAG, "Choice clicked: " + index);
                submitChoice(index);
            });
            llChoices.addView(btn);
        }
        btnStartStory.setVisibility(View.GONE);
    }

    private void submitChoice(int index) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentGameId == null) {
            Log.e(TAG, "Submit choice failed: User null or no game ID");
            return;
        }

        setLoading(true);
        Log.d(TAG, "Submitting choice " + index + " for game " + currentGameId);
        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            RetrofitClient.getService().submitGame(token, new GameSubmitRequest(currentGameId, index))
                    .enqueue(new Callback<GameSubmitResponse>() {
                        @Override
                        public void onResponse(Call<GameSubmitResponse> call, Response<GameSubmitResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "Choice submitted successfully. Win: " + response.body().win);
                                handleResult(response.body());
                            } else {
                                Log.e(TAG, "Submit choice failed: " + response.code());
                                Toast.makeText(getContext(), "Failed to submit choice", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<GameSubmitResponse> call, Throwable t) {
                            setLoading(false);
                            Log.e(TAG, "Submit choice network error", t);
                            Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void handleResult(GameSubmitResponse result) {
        if (result.win) {
            Toast.makeText(getContext(), "Great choice! You won " + result.reward + " Credits", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Bad ending! Try again.", Toast.LENGTH_LONG).show();
        }
        resetUI();
    }

    private void resetUI() {
        Log.d(TAG, "Resetting UI");
        currentGameId = null;
        tvStoryScene.setText("Choose a theme and start your story adventure!");
        llChoices.removeAllViews();
        btnStartStory.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressStory.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnStartStory.setEnabled(!loading);
        btnRomantic.setEnabled(!loading);
        btnHorror.setEnabled(!loading);
        btnSciFi.setEnabled(!loading);
    }
}
