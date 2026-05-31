package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;

import java.util.concurrent.Executor;

public class SecurityActivity extends AppCompatActivity {

    private EditText etPin;
    private String correctPin;
    private boolean isBiometricEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        etPin = findViewById(R.id.etPinEntry);
        View btnUnlock = findViewById(R.id.btnUnlock);
        View btnFingerprint = findViewById(R.id.btnFingerprint);

        fetchSecuritySettings();

        etPin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 4) {
                    validatePin(s.toString());
                }
            }
        });

        btnUnlock.setOnClickListener(v -> validatePin(etPin.getText().toString()));
        btnFingerprint.setOnClickListener(v -> showBiometricPrompt());
    }

    private void validatePin(String entered) {
        if (correctPin != null && entered.equals(correctPin)) {
            unlockApp();
        } else if (entered.length() == 4) {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
            etPin.setText("");
        }
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
                correctPin = user.pinCode;
                isBiometricEnabled = user.biometricEnabled;

                if (isBiometricEnabled) {
                    findViewById(R.id.btnFingerprint).setVisibility(View.VISIBLE);
                    showBiometricPrompt();
                }
            }
        });
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
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

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Unlock")
                .setSubtitle("Use your fingerprint to unlock BotCraft")
                .setNegativeButtonText("Use PIN")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void unlockApp() {
        startActivity(new Intent(SecurityActivity.this, MainActivity.class));
        finish();
    }
}
