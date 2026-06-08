package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;

import java.util.concurrent.Executor;

public class SecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        View btnFingerprint = findViewById(R.id.btnFingerprint);
        View btnLogout = findViewById(R.id.btnLogoutSecurity);

        if (btnFingerprint != null) {
            btnFingerprint.setOnClickListener(v -> showBiometricPrompt());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(SecurityActivity.this, LoginActivity.class));
                finish();
            });
        }

        fetchSecuritySettings();
    }

    private void fetchSecuritySettings() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) {
            finish();
            return;
        }

        FirebaseManager.getFirestore().collection("users").document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                if (!user.biometricEnabled) {
                    unlockApp();
                } else {
                    showBiometricPrompt();
                }
            } else {
                unlockApp();
            }
        }).addOnFailureListener(e -> unlockApp());
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Don't show toast for user cancellation
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(SecurityActivity.this, errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                unlockApp();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("App Locked")
                .setSubtitle("Use biometric or screen lock to continue")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        biometricPrompt.authenticate(builder.build());
    }

    private void unlockApp() {
        startActivity(new Intent(SecurityActivity.this, MainActivity.class));
        finish();
    }
}
