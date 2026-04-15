package com.ranit.botscraft.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.RetrofitClient;
import com.ranit.botscraft.network.SpinResponse;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameSpinFragment extends Fragment {

    private static final String TAG = "GameSpinFragment";
    private ImageView ivWheel, ivRewardIcon;
    private MaterialButton btnSpin;
    private TextView tvUserCredits, tvRemainingSpins, tvRewardText, tvSpinCost;
    private LinearLayout llRewardView;
    private ProgressBar progressSpin;
    private View btnBack;
    
    private boolean isSpinning = false;
    private long userCredits = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_spin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivWheel = view.findViewById(R.id.ivWheel);
        ivRewardIcon = view.findViewById(R.id.ivRewardIcon);
        btnSpin = view.findViewById(R.id.btnSpin);
        tvUserCredits = view.findViewById(R.id.tvUserCredits);
        tvRemainingSpins = view.findViewById(R.id.tvRemainingSpins);
        tvRewardText = view.findViewById(R.id.tvRewardText);
        tvSpinCost = view.findViewById(R.id.tvSpinCost);
        llRewardView = view.findViewById(R.id.llRewardView);
        progressSpin = view.findViewById(R.id.progressSpin);
        btnBack = view.findViewById(R.id.btnBack);

        btnSpin.setOnClickListener(v -> checkCreditsAndSpin());
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        
        fetchUserCredits();
        
        // Initial state
        llRewardView.setVisibility(View.GONE);
    }

    private void fetchUserCredits() {
        String uid = FirebaseManager.getUserId();
        if (uid != null) {
            FirebaseManager.getFirestore().collection("users").document(uid)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                userCredits = user.credits;
                                tvUserCredits.setText(userCredits + " CR");
                            }
                        }
                    });
        }
    }

    private void checkCreditsAndSpin() {
        if (isSpinning) return;
        
        if (userCredits < 200) {
            showLowCreditsDialog();
        } else {
            startSpin();
        }
    }

    private void showLowCreditsDialog() {
        new AlertDialog.Builder(getContext(), R.style.Theme_Chstbot_Dialog)
                .setTitle("Insufficient Credits")
                .setMessage("You need 200 credits to spin the wheel. Would you like to buy more?")
                .setPositiveButton("Buy Credits", (dialog, which) -> {
                    startActivity(new Intent(getContext(), BuyCreditsActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startSpin() {
        Log.d(TAG, "Requesting spin from server...");
        setLoading(true);
        llRewardView.setVisibility(View.GONE);

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            RetrofitClient.getService().spinWheel(token).enqueue(new Callback<SpinResponse>() {
                @Override
                public void onResponse(Call<SpinResponse> call, Response<SpinResponse> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Spin successful: " + response.body().rewardName);
                        performWheelAnimation(response.body());
                    } else {
                        String error = "Failed to spin";
                        if (response.code() == 403) error = "Not enough credits or daily limit reached";
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<SpinResponse> call, Throwable t) {
                    setLoading(false);
                    Log.e(TAG, "Network error during spin", t);
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void performWheelAnimation(SpinResponse result) {
        isSpinning = true;
        btnSpin.setEnabled(false);

        // Calculate rotation based on reward from server
        // Better Luck: 0-72, 50 CR: 72-144, 100 CR: 144-216, 200 CR: 216-288, Free Character: 288-360
        // (Simplified logic for visualization)
        int rewardDegrees = 0;
        if ("Better Luck".equalsIgnoreCase(result.rewardName)) rewardDegrees = 36;
        else if (result.rewardName.contains("50")) rewardDegrees = 108;
        else if (result.rewardName.contains("100")) rewardDegrees = 180;
        else if (result.rewardName.contains("200")) rewardDegrees = 252;
        else rewardDegrees = 324; // Character

        int finalRotation = 3600 + (360 - rewardDegrees); 
        
        ivWheel.animate()
                .rotation(finalRotation)
                .setDuration(4000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    isSpinning = false;
                    btnSpin.setEnabled(true);
                    displayResult(result);
                })
                .start();
    }

    private void displayResult(SpinResponse result) {
        userCredits = result.remainingCredits;
        tvUserCredits.setText(result.remainingCredits + " CR");
        tvRemainingSpins.setText(result.spinsRemaining + " SPINS REMAINING TODAY");
        
        llRewardView.setVisibility(View.VISIBLE);
        tvRewardText.setText(result.rewardName);

        if ("credits".equals(result.rewardType)) {
            ivRewardIcon.setImageResource(R.drawable.ic_credits);
        } else if (result.rewardType.contains("character")) {
            ivRewardIcon.setImageResource(R.drawable.ic_bot_placeholder);
        } else {
            ivRewardIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
    }

    private void setLoading(boolean loading) {
        progressSpin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSpin.setEnabled(!loading);
        btnBack.setEnabled(!loading);
    }
}
