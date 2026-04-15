package com.ranit.botscraft.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.viewmodel.BotViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

public class CreateBotStep5Fragment extends Fragment {

    private static final String TAG = "CreateBotStep5";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ID

    private ImageView imgBotFinal;
    private TextView tvFinalName, tvFinalDetails, tvFinalRelation, btnBack;
    private Button btnCreate;
    private View loadingOverlay;
    private BotViewModel botViewModel;
    private InterstitialAd mInterstitialAd;
    private User currentUser;

    public CreateBotStep5Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_bot_step5, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        botViewModel = new ViewModelProvider(requireActivity()).get(BotViewModel.class);
        Bot bot = botViewModel.getBotData().getValue();

        imgBotFinal = view.findViewById(R.id.imgBotFinal);
        tvFinalName = view.findViewById(R.id.tvFinalName);
        tvFinalDetails = view.findViewById(R.id.tvFinalDetails);
        tvFinalRelation = view.findViewById(R.id.tvFinalRelation);
        btnCreate = view.findViewById(R.id.btnCreate);
        btnBack = view.findViewById(R.id.btnBack);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        fetchUserAndLoadAd();

        if (bot != null) displayBotReview(bot);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnCreate.setOnClickListener(v -> {
            if (bot != null) checkRequirementsAndProceed(bot);
            else Toast.makeText(getContext(), "Error: Bot data missing", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchUserAndLoadAd() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        FirebaseManager.getFirestore().collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            currentUser = doc.toObject(User.class);
            if (currentUser != null) {
                if (!"ultra".equals(currentUser.plan)) {
                    loadInterstitialAd();
                }
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(requireContext(), AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        });
    }

    private void showInterstitialAd(Runnable onDismiss) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    onDismiss.run();
                }
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    onDismiss.run();
                }
            });
            mInterstitialAd.show(requireActivity());
        } else {
            onDismiss.run();
        }
    }

    private void displayBotReview(Bot bot) {
        tvFinalName.setText(bot.name);
        tvFinalDetails.setText(bot.relationship + " • " + bot.age + " years old");
        if (bot.modelType != null) tvFinalRelation.setText(capitalize(bot.modelType));
        if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
            Glide.with(this).load(bot.imageUrl).placeholder(android.R.drawable.ic_menu_gallery).into(imgBotFinal);
        }
    }

    private void checkRequirementsAndProceed(Bot bot) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { navigateToLogin(); return; }

        setLoading(true);
        user.reload().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                FirebaseUser freshUser = FirebaseAuth.getInstance().getCurrentUser();
                if (freshUser != null && freshUser.isEmailVerified()) {
                    // Re-fetch user from Firestore to ensure latest plan status
                    FirebaseManager.getFirestore().collection("users").document(freshUser.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                if (!isAdded()) return;
                                currentUser = doc.toObject(User.class);
                                checkBotLimitAndSave(bot);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                checkBotLimitAndSave(bot);
                            });
                } else {
                    setLoading(false);
                    showVerificationDialog(freshUser != null ? freshUser : user);
                }
            } else {
                setLoading(false);
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        if (getActivity() == null) return;
        startActivity(new Intent(getActivity(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getActivity().finish();
    }

    private void showVerificationDialog(FirebaseUser user) {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Verify Your Email")
                .setMessage("Please verify your email to proceed.")
                .setPositiveButton("Verified", (dialog, which) -> {
                    Bot b = botViewModel.getBotData().getValue();
                    if (b != null) checkRequirementsAndProceed(b);
                }).show();
    }

    private void checkBotLimitAndSave(Bot bot) {
        String uid = FirebaseManager.getUserId();
        if (uid == null || currentUser == null) {
            setLoading(false);
            return;
        }
        
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("bots").whereEqualTo("ownerId", uid).get().addOnSuccessListener(botSnap -> {
            if (!isAdded()) return;
            int count = botSnap.size();
            String plan = currentUser.plan != null ? currentUser.plan : "free";
            
            // Correct limits: Free = 1, Premium = 3, Ultra = Unlimited
            int limit = "premium".equals(plan) ? 3 : ("ultra".equals(plan) ? Integer.MAX_VALUE : 1);

            if (count >= limit) {
                setLoading(false);
                showUpgradeRequiredDialog(plan);
            } else {
                uploadImageAndSaveBot(bot, currentUser.name);
            }
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(getContext(), "Error checking limits", Toast.LENGTH_SHORT).show();
        });
    }

    private void showUpgradeRequiredDialog(String currentPlan) {
        if (getContext() == null) return;
        
        Dialog dialog = new Dialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_limit_reached, null);
        dialog.setContentView(view);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvDesc = view.findViewById(R.id.tvLimitDescription);
        if ("free".equals(currentPlan)) {
            tvDesc.setText("Free users can only create 1 bot. Upgrade to Premium to create up to 3 bots!");
        } else if ("premium".equals(currentPlan)) {
            tvDesc.setText("Premium users can create up to 3 bots. Upgrade to Ultra for unlimited creations!");
        }

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnNotNow).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnUpgradeLimit).setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to profile or subscription screen
            // Assuming Main's PROFILE tab handles upgrades
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.putExtra("ACTION", "OPEN_UPGRADE");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        dialog.show();
    }

    private void uploadImageAndSaveBot(Bot bot, String currentUserName) {
        if (bot.imageUrl != null && bot.imageUrl.startsWith("data:image")) {
            try {
                String b64 = bot.imageUrl.substring(bot.imageUrl.indexOf(",") + 1);
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                StorageReference ref = FirebaseManager.getStorage().getReference().child("bots/" + System.currentTimeMillis() + ".jpg");
                ref.putBytes(data).addOnSuccessListener(snapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    bot.imageUrl = uri.toString();
                    saveBotToFirestore(bot, currentUserName);
                })).addOnFailureListener(e -> { bot.imageUrl = null; saveBotToFirestore(bot, currentUserName); });
            } catch (Exception e) { bot.imageUrl = null; saveBotToFirestore(bot, currentUserName); }
        } else saveBotToFirestore(bot, currentUserName);
    }

    private void saveBotToFirestore(Bot bot, String currentUserName) {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        bot.ownerId = FirebaseManager.getUserId();
        bot.createdAt = System.currentTimeMillis();
        bot.active = true;
        bot.model = "grok-4-1-fast-non-reasoning";
        
        StringBuilder prompt = new StringBuilder("SYSTEM: ").append(bot.name).append(", ").append(bot.age).append("yo human.\n");
        prompt.append(bot.personality).append(". ").append(bot.description).append("\n");
        bot.systemPrompt = prompt.toString();
        
        db.collection("bots").add(bot).addOnSuccessListener(ref -> {
            bot.botId = ref.getId();
            ref.update("botId", bot.botId).addOnSuccessListener(aVoid -> {
                setLoading(false);
                if (isAdded()) {
                    showInterstitialAd(() -> showSuccessDialog(bot));
                }
            });
        }).addOnFailureListener(e -> { setLoading(false); Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
    }

    private void showSuccessDialog(Bot bot) {
        if (!isAdded()) return;
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_Chstbot_FullScreenDialog);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bot_created, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        
        ImageView img = view.findViewById(R.id.imgBotCreated);
        TextView tv = view.findViewById(R.id.tvBotNameCreated);
        Button btn = view.findViewById(R.id.btnGoToProfile);
        
        tv.setText(bot.name + " is ready!");
        if (bot.imageUrl != null) Glide.with(this).load(bot.imageUrl).into(img);
        
        btn.setOnClickListener(v -> {
            dialog.dismiss();
            bot.sanitizeForIntent();
            startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("bot", bot));
            if (getActivity() != null) getActivity().finish();
        });
        dialog.show();
    }

    private void setLoading(boolean loading) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnCreate != null) btnCreate.setEnabled(!loading);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
