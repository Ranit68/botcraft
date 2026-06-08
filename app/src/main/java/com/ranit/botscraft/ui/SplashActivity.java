package com.ranit.botscraft.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove EdgeToEdge for better compatibility on some devices
        setContentView(R.layout.activity_splash);

        // Start Moving Gradient Animation
        View root = findViewById(R.id.splash_root);
        if (root != null) {
            try {
                int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                int resId = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) 
                        ? R.drawable.splash_gradient_dark 
                        : R.drawable.splash_gradient_light;
                
                root.setBackgroundResource(resId);

                Drawable background = root.getBackground();
                if (background instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) background;
                    animationDrawable.setEnterFadeDuration(2000);
                    animationDrawable.setExitFadeDuration(2000);
                    animationDrawable.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Animation error", e);
            }
        }

        // Increased delay slightly to show off the animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;

            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                } else {
                    if (isBiometricEnabled(user.getUid())) {
                        startActivity(new Intent(SplashActivity.this, SecurityActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    }
                }
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } catch (Exception e) {
                Log.e(TAG, "Navigation error", e);
                // Fallback to Login to avoid being stuck
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 3000);
    }

    private boolean isBiometricEnabled(String uid) {
        if (uid == null) return false;
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("app_security", Context.MODE_PRIVATE);
            return prefs.getBoolean("biometric_enabled_" + uid, false);
        } catch (Exception e) {
            return false;
        }
    }
}
