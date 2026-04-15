package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ranit.botscraft.R;
import com.ranit.botscraft.network.GameStartRequest;
import com.ranit.botscraft.network.GameStartResponse;
import com.ranit.botscraft.network.GameSubmitRequest;
import com.ranit.botscraft.network.GameSubmitResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameGuessFragment extends Fragment {

    private LinearLayout llHints;
    private EditText etGuessAnswer;
    private Button btnStartGuess, btnSubmitGuess;
    private ProgressBar progressGuess;
    private String currentGameId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_guess, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llHints = view.findViewById(R.id.llHints);
        etGuessAnswer = view.findViewById(R.id.etGuessAnswer);
        btnStartGuess = view.findViewById(R.id.btnStartGuess);
        btnSubmitGuess = view.findViewById(R.id.btnSubmitGuess);
        progressGuess = view.findViewById(R.id.progressGuess);

        btnStartGuess.setOnClickListener(v -> startGame());
        btnSubmitGuess.setOnClickListener(v -> submitAnswer());
    }

    private void startGame() {
        setLoading(true);
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            RetrofitClient.getService().startGame(token, new GameStartRequest("guess", null))
                    .enqueue(new Callback<GameStartResponse>() {
                        @Override
                        public void onResponse(Call<GameStartResponse> call, Response<GameStartResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                currentGameId = response.body().gameId;
                                displayHints(response.body().gameData.getAsJsonArray("hints"));
                            } else {
                                Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<GameStartResponse> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void displayHints(JsonArray hints) {
        llHints.removeAllViews();
        for (int i = 0; i < hints.size(); i++) {
            TextView tv = new TextView(getContext());
            tv.setText("Hint " + (i + 1) + ": " + hints.get(i).getAsString());
            tv.setPadding(0, 12, 0, 12);
            tv.setTextColor(getContext().getColor(R.color.text_main));
            llHints.addView(tv);
        }
        btnStartGuess.setVisibility(View.GONE);
        etGuessAnswer.setVisibility(View.VISIBLE);
        btnSubmitGuess.setVisibility(View.VISIBLE);
    }

    private void submitAnswer() {
        String answer = etGuessAnswer.getText().toString().trim();
        if (answer.isEmpty()) return;

        setLoading(true);
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            RetrofitClient.getService().submitGame(token, new GameSubmitRequest(currentGameId, answer))
                    .enqueue(new Callback<GameSubmitResponse>() {
                        @Override
                        public void onResponse(Call<GameSubmitResponse> call, Response<GameSubmitResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                handleGameResult(response.body());
                            }
                        }

                        @Override
                        public void onFailure(Call<GameSubmitResponse> call, Throwable t) {
                            setLoading(false);
                        }
                    });
        });
    }

    private void handleGameResult(GameSubmitResponse result) {
        if (result.win) {
            Toast.makeText(getContext(), "Correct! Reward: " + result.reward + " Credits", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Wrong answer! Better luck next time.", Toast.LENGTH_LONG).show();
        }
        resetUI();
    }

    private void resetUI() {
        currentGameId = null;
        etGuessAnswer.setText("");
        etGuessAnswer.setVisibility(View.GONE);
        btnSubmitGuess.setVisibility(View.GONE);
        btnStartGuess.setVisibility(View.VISIBLE);
        llHints.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText("Start a new game to see hints!");
        tv.setTextColor(getContext().getColor(R.color.text_label));
        llHints.addView(tv);
    }

    private void setLoading(boolean loading) {
        progressGuess.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnStartGuess.setEnabled(!loading);
        btnSubmitGuess.setEnabled(!loading);
    }
}
