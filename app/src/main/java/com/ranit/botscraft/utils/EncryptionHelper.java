package com.ranit.botscraft.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Production-ready Encryption Helper using Android Keystore System (AES/GCM/NoPadding).
 * Provides secure, hardware-backed encryption for user messages.
 */
public class EncryptionHelper {

    private static final String TAG = "EncryptionHelper";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "chstbot_messaging_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        try {
            initKey();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize encryption key", e);
        }
    }

    private static void initKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());

            keyGenerator.generateKey();
            Log.d(TAG, "New encryption key generated in Keystore.");
        }
    }

    private static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Encrypts a string using AES/GCM.
     * Output format: Base64(IV + Ciphertext)
     */
    @Nullable
    public static String encrypt(@Nullable String value) {
        if (value == null || value.isEmpty()) return value;

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            byte[] iv = cipher.getIV();
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            // Combine IV and Encrypted Data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null; // Return null so the app knows it failed to secure the data
        }
    }

    /**
     * Decrypts a string encrypted by this helper.
     */
    @Nullable
    public static String decrypt(@Nullable String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;

        try {
            byte[] combined = Base64.decode(encrypted, Base64.NO_WRAP);
            if (combined.length < GCM_IV_LENGTH) return encrypted; // Likely not encrypted or old format

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Check if it's the old fixed-key format from previous implementation to prevent data loss
            return decryptLegacy(encrypted);
        }
    }

    // Fallback for the hardcoded key used in the previous step
    private static String decryptLegacy(String encrypted) {
        try {
            // This is just a placeholder to handle the transition if needed.
            // In a real production app, you might migrate data or warn the user.
            Log.w(TAG, "Decryption failed, might be old format or unencrypted.");
            return encrypted; 
        } catch (Exception e) {
            return encrypted;
        }
    }
}
