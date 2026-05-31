package com.ranit.botscraft.ui;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Start Moving Gradient Animation
        View root = findViewById(R.id.splash_root);
        if (root != null) {
            // Check current theme and set appropriate animation
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                root.setBackgroundResource(R.drawable.splash_gradient_dark);
            } else {
                root.setBackgroundResource(R.drawable.splash_gradient_light);
            }

            AnimationDrawable animationDrawable = (AnimationDrawable) root.getBackground();
            animationDrawable.setEnterFadeDuration(2000);
            animationDrawable.setExitFadeDuration(2000);
            animationDrawable.start();
        }

        // Increased delay slightly to show off the animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null){
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            } else {
                if (isSecurityEnabled()) {
                    startActivity(new Intent(SplashActivity.this, SecurityActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
            }
            finish();
            // Standard transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 3500);
    }

    private boolean isSecurityEnabled() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_security", MODE_PRIVATE);
        return prefs.getBoolean("security_enabled", false);
    }
}
