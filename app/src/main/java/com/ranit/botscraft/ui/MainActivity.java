package com.ranit.botscraft.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.utils.NotificationHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityLog";
    private static final String BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"; // Test ID

    private View navHome, navChat, navCreate, navProfile;
    private View bgHome, bgChat, bgCreate, bgProfile;
    private ImageView icHome, icChat, icCreate, icProfile;
    private TextView tvHome, tvChat, tvCreate, tvProfile;
    
    private View llCredits;
    private TextView tvUserCredits;
    private ShapeableImageView imgUserProfile;
    private FrameLayout adContainer;
    private AdView mAdView;
    private ListenerRegistration userListener;

    private String currentTab = "";
    private final Map<String, Integer> tabIndices = new HashMap<>();
    private boolean promoShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTabIndices();
        initViews();
        setupNavigation();
        setupCredits();

        if (savedInstanceState == null) {
            selectTab("HOME");
        }
        
        // Start the notification cycle
        NotificationHelper.scheduleNextNotification(this);
    }

    private void initTabIndices() {
        tabIndices.put("HOME", 0);
        tabIndices.put("CHAT", 1);
        tabIndices.put("CREATE", 2);
        tabIndices.put("PROFILE", 3);
    }

    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navChat = findViewById(R.id.navChat);
        navCreate = findViewById(R.id.navCreate);
        navProfile = findViewById(R.id.navProfile);
        
        bgHome = findViewById(R.id.bgHome);
        bgChat = findViewById(R.id.bgChat);
        bgCreate = findViewById(R.id.bgCreate);
        bgProfile = findViewById(R.id.bgProfile);

        icHome = findViewById(R.id.icHome);
        icChat = findViewById(R.id.icChat);
        icCreate = findViewById(R.id.icCreate);
        icProfile = findViewById(R.id.icProfile);

        tvHome = findViewById(R.id.tvHome);
        tvChat = findViewById(R.id.tvChat);
        tvCreate = findViewById(R.id.tvCreate);
        tvProfile = findViewById(R.id.tvProfile);
        
        llCredits = findViewById(R.id.llCredits);
        tvUserCredits = findViewById(R.id.tvUserCreditsMain);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        adContainer = findViewById(R.id.adContainer);
    }

    private void setupCredits() {
        if (llCredits != null) {
            llCredits.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, BuyCreditsActivity.class));
            });
        }

        String uid = FirebaseManager.getUserId();
        if (uid != null) {
            userListener = FirebaseManager.getFirestore().collection("users").document(uid)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;
                        if (snapshot != null && snapshot.exists()) {
                            User user = snapshot.toObject(User.class);
                            if (user != null) {
                                if (tvUserCredits != null) tvUserCredits.setText(user.credits + " CR");
                                
                                // Update Toolbar Profile Image & Border
                                updateToolbarProfile(user);
                                
                                // Banner Ad Logic
                                handleBannerAd(user);

                                // Show promo dialog only for Free users who haven't seen it in this session
                                if (!promoShown && ("free".equals(user.plan) || user.plan == null)) {
                                    showSubscriptionPromo();
                                    promoShown = true;
                                }
                            }
                        }
                    });
        }
    }

    private void handleBannerAd(User user) {
        String plan = user.plan != null ? user.plan : "free";
        if ("free".equals(plan)) {
            if (mAdView == null) {
                mAdView = new AdView(this);
                mAdView.setAdUnitId(BANNER_AD_ID);
                mAdView.setAdSize(AdSize.BANNER);
                adContainer.removeAllViews();
                adContainer.addView(mAdView);
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
            adContainer.setVisibility(View.VISIBLE);
        } else {
            adContainer.setVisibility(View.GONE);
            if (mAdView != null) {
                mAdView.destroy();
                mAdView = null;
            }
        }
    }

    private void updateToolbarProfile(User user) {
        if (imgUserProfile == null) return;

        // Load image
        if (user.imageUrl != null && !user.imageUrl.isEmpty()) {
            Glide.with(this).load(user.imageUrl).circleCrop().into(imgUserProfile);
        } else {
            imgUserProfile.setImageResource(R.drawable.user);
        }

        // Apply Border based on plan
        String plan = user.plan != null ? user.plan : "free";
        if ("premium".equals(plan)) {
            imgUserProfile.setStrokeWidth(4f);
            imgUserProfile.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C0C0C0"))); // Silver
        } else if ("ultra".equals(plan)) {
            imgUserProfile.setStrokeWidth(4f);
            imgUserProfile.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFD700"))); // Gold
        } else {
            imgUserProfile.setStrokeWidth(0f);
        }
    }

    private void showSubscriptionPromo() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_subscription_promo, null);
        dialog.setContentView(view);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        }

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnContinueFree).setOnClickListener(v -> dialog.dismiss());

        Button btnUpgrade = view.findViewById(R.id.btnUpgradeNow);
        btnUpgrade.setOnClickListener(v -> {
            dialog.dismiss();
            selectTab("PROFILE");
        });

        dialog.show();
    }

    private void setupNavigation() {
        if (navHome != null) navHome.setOnClickListener(v -> selectTab("HOME"));
        if (navChat != null) navChat.setOnClickListener(v -> selectTab("CHAT"));
        if (navCreate != null) navCreate.setOnClickListener(v -> selectTab("CREATE"));
        if (navProfile != null) navProfile.setOnClickListener(v -> selectTab("PROFILE"));
    }

    private void selectTab(String tab) {
        if (currentTab.equals(tab)) return;
        
        Log.d(TAG, "Selecting tab: " + tab);
        try {
            int oldIndex = tabIndices.getOrDefault(currentTab, -1);
            int newIndex = tabIndices.getOrDefault(tab, 0);

            resetNavUI();
            Fragment fragment = null;

            switch (tab) {
                case "HOME":
                    fragment = new HomeFragment();
                    highlightTab(bgHome, icHome, tvHome);
                    break;
                case "CHAT":
                    fragment = new ChatFragment();
                    highlightTab(bgChat, icChat, tvChat);
                    break;
                case "CREATE":
                    fragment = new CreateBotStep1Fragment();
                    highlightTab(bgCreate, icCreate, tvCreate);
                    break;
                case "PROFILE":
                    fragment = new ProfileFragment();
                    highlightTab(bgProfile, icProfile, tvProfile);
                    break;
            }

            if (fragment != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                
                if (oldIndex != -1) {
                    if (newIndex > oldIndex) {
                        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                    } else {
                        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                }
                
                transaction.replace(R.id.fragment_container, fragment)
                        .commit();
            }
            
            currentTab = tab;
        } catch (Exception e) {
            Log.e(TAG, "Error in selectTab: " + e.getMessage(), e);
        }
    }

    private void resetNavUI() {
        if (bgHome != null) bgHome.setVisibility(View.INVISIBLE);
        if (bgChat != null) bgChat.setVisibility(View.INVISIBLE);
        if (bgCreate != null) bgCreate.setVisibility(View.INVISIBLE);
        if (bgProfile != null) bgProfile.setVisibility(View.INVISIBLE);

        int inactiveColor = ContextCompat.getColor(this, R.color.text_label);
        if (icHome != null) icHome.setColorFilter(inactiveColor);
        if (icChat != null) icChat.setColorFilter(inactiveColor);
        if (icCreate != null) icCreate.setColorFilter(inactiveColor);
        if (icProfile != null) icProfile.setColorFilter(inactiveColor);
        
        if (tvHome != null) tvHome.setTextColor(inactiveColor);
        if (tvChat != null) tvChat.setTextColor(inactiveColor);
        if (tvCreate != null) tvCreate.setTextColor(inactiveColor);
        if (tvProfile != null) tvProfile.setTextColor(inactiveColor);
    }

    private void highlightTab(View bg, ImageView ic, TextView tv) {
        if (bg != null) bg.setVisibility(View.VISIBLE);
        if (ic != null) ic.setColorFilter(ContextCompat.getColor(this, R.color.white));
        if (tv != null) tv.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
