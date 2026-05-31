package com.ranit.botscraft.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.ImageRequest;
import com.ranit.botscraft.network.ImageResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BotProfileActivity extends AppCompatActivity {

    private static final String TAG = "CHSTBOT_DEBUG";
    private static final int PICK_WALLPAPER = 1002;

    private ImageView imgBotProfile, imgChatWallpaper, btnBack;
    private TextView tvBotName, tvBotTagline, tvBotAbout, tvBotAge, tvBotGender, tvBotModel, tvBotLanguage;
    private Button btnStartChat, btnChangeBackground, btnRunWallpaperGen, btnPickWallpaperGallery;
    private EditText etWallpaperPrompt;
    private LinearLayout llPromptArea;
    private View cvCustomization, loadingOverlay;

    private Bot bot;
    private User currentUser;
    private ListenerRegistration userListener;
    private boolean isOwner = false;
    private byte[] pendingImageData = null; 

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_profile);

        bot = (Bot) getIntent().getSerializableExtra("bot");
        if (bot == null || bot.botId == null) {
            finish();
            return;
        }

        String currentUserId = FirebaseManager.getUserId();
        isOwner = bot.ownerId != null && bot.ownerId.equals(currentUserId);

        initViews();
        displayBotData();
        setupListeners();
        listenToUserUpdates();
    }

    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        userListener = FirebaseManager.getFirestore().collection("users").document(uid).addSnapshotListener((snap, e) -> {
            if (snap != null && snap.exists()) {
                currentUser = snap.toObject(User.class);
            }
        });
    }

    private void initViews() {
        imgBotProfile = findViewById(R.id.imgBotProfile);
        imgChatWallpaper = findViewById(R.id.imgChatWallpaper);
        btnBack = findViewById(R.id.btnBack);
        
        tvBotName = findViewById(R.id.tvBotName);
        tvBotTagline = findViewById(R.id.tvBotTagline);
        tvBotAbout = findViewById(R.id.tvBotAbout);
        tvBotAge = findViewById(R.id.tvBotAge);
        tvBotGender = findViewById(R.id.tvBotGender);
        tvBotModel = findViewById(R.id.tvBotModel);
        tvBotLanguage = findViewById(R.id.tvBotLanguage);

        btnStartChat = findViewById(R.id.btnStartChat);
        btnChangeBackground = findViewById(R.id.btnChangeBackground);
        btnRunWallpaperGen = findViewById(R.id.btnRunWallpaperGen);
        btnPickWallpaperGallery = findViewById(R.id.btnPickWallpaperGallery);
        
        etWallpaperPrompt = findViewById(R.id.etWallpaperPrompt);
        llPromptArea = findViewById(R.id.llPromptArea);
        cvCustomization = findViewById(R.id.cvCustomization);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        if (isOwner) {
            cvCustomization.setVisibility(View.VISIBLE);
        }
    }

    private void displayBotData() {
        tvBotName.setText(bot.getDisplayName());
        tvBotAbout.setText(bot.getDisplayDescription());
        tvBotAge.setText(bot.age + " Years");
        tvBotGender.setText(bot.gender);
        tvBotModel.setText(bot.modelType);
        tvBotLanguage.setText(bot.language != null ? bot.language : "English");
        tvBotTagline.setText(bot.relationship);

        if (bot.imageUrl != null) Glide.with(this).load(bot.imageUrl).into(imgBotProfile);
        if (bot.chatBackgroundUrl != null) Glide.with(this).load(bot.chatBackgroundUrl).into(imgChatWallpaper);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnStartChat.setOnClickListener(v -> {
            if (bot != null) {
                bot.sanitizeForIntent(); 
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("bot", bot);
                startActivity(intent);
            }
        });

        if (isOwner) {
            tvBotName.setOnClickListener(v -> showEditDialog("name", "Name", bot.name));
            tvBotTagline.setOnClickListener(v -> showEditDialog("relationship", "Relationship", bot.relationship));
            tvBotAbout.setOnClickListener(v -> showEditDialog("personality", "About", bot.personality));
            tvBotAge.setOnClickListener(v -> showEditDialog("age", "Age", String.valueOf(bot.age)));
            tvBotGender.setOnClickListener(v -> showEditDialog("gender", "Gender", bot.gender));
            tvBotModel.setOnClickListener(v -> showEditDialog("modelType", "Model Type", bot.modelType));
            tvBotLanguage.setOnClickListener(v -> showEditDialog("language", "Language", bot.language));

            imgBotProfile.setOnClickListener(v -> showProfileRegenDialog());

            btnChangeBackground.setOnClickListener(v -> {
                llPromptArea.setVisibility(llPromptArea.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            btnRunWallpaperGen.setOnClickListener(v -> generateWallpaper());
            if (btnPickWallpaperGallery != null) {
                btnPickWallpaperGallery.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_WALLPAPER);
                });
            }
        }
    }

    private boolean canGenerateImage() {
        if (currentUser == null) return false;
        String plan = currentUser.plan != null ? currentUser.plan : "free";
        int limit = plan.equals("ultra") ? 8 : (plan.equals("premium") ? 3 : 1);
        return currentUser.dailyImageCount < limit || currentUser.credits >= 1;
    }

    private void showEditDialog(String fieldKey, String label, String currentValue) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_field, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Chstbot_Dialog).setView(dialogView).create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);
        Button btnUpdate = dialogView.findViewById(R.id.btnDialogUpdate);

        tvTitle.setText("Edit " + label);
        etInput.setText(currentValue);

        btnUpdate.setOnClickListener(v -> {
            String newValue = etInput.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateBotField(fieldKey, newValue);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateBotField(String key, Object value) {
        loadingOverlay.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseManager.getFirestore();
        
        Object finalValue = value;
        if (key.equals("age")) {
            try { finalValue = Long.parseLong(value.toString()); } catch (Exception e) { finalValue = 18L; }
        }

        final Object valToUpdate = finalValue;
        db.collection("bots").document(bot.botId)
                .update(key, finalValue)
                .addOnSuccessListener(aVoid -> {
                    loadingOverlay.setVisibility(View.GONE);
                    if (key.equals("name")) bot.name = (String) valToUpdate;
                    else if (key.equals("personality")) bot.personality = (String) valToUpdate;
                    else if (key.equals("relationship")) bot.relationship = (String) valToUpdate;
                    else if (key.equals("age")) bot.age = (Long) valToUpdate;
                    else if (key.equals("gender")) bot.gender = (String) valToUpdate;
                    else if (key.equals("modelType")) bot.modelType = (String) valToUpdate;
                    else if (key.equals("language")) bot.language = (String) valToUpdate;
                    
                    displayBotData();
                    Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void showProfileRegenDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_regen_profile, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Chstbot_Dialog).setView(view).create();
        EditText etPrompt = view.findViewById(R.id.etRegenPrompt);
        Button btnGen = view.findViewById(R.id.btnRegenGenerate);
        Button btnSave = view.findViewById(R.id.btnRegenSave);
        btnGen.setOnClickListener(v -> {
            if (!canGenerateImage()) {
                Toast.makeText(this, "Daily photo limit reached. Upgrade for more!", Toast.LENGTH_LONG).show();
                return;
            }
            String promptText = etPrompt.getText().toString().trim();
            if (promptText.isEmpty()) return;
            generateNewProfileImage(promptText);
        });
        btnSave.setOnClickListener(v -> {
            if (pendingImageData != null) {
                uploadImageToStorage(pendingImageData, "imageUrl", dialog);
            } else {
                Toast.makeText(this, "Generate a preview first", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void generateNewProfileImage(final String userPrompt) {
        loadingOverlay.setVisibility(View.VISIBLE);
        final String faceRef = (bot.imageUrl != null && bot.imageUrl.startsWith("http")) ? bot.imageUrl : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("A realistic 8k portrait photo of ").append(bot.name).append(", a ").append(bot.age).append(" year old ").append(bot.gender);
        if (bot.hairStyle != null) sb.append(", with ").append(bot.hairStyle).append(" hair");
        if (bot.hairColor != null) sb.append(", ").append(bot.hairColor).append(" color");
        if (bot.skinTone != null) sb.append(", ").append(bot.skinTone).append(" skin");
        if (bot.bodyType != null) sb.append(", ").append(bot.bodyType).append(" figure");
        
        if (!faceRef.isEmpty()) {
            sb.append(". ESSENTIAL FACE REFERENCE: Ensure face perfectly matches: ").append(faceRef);
        }
        
        sb.append(". Character is ").append(userPrompt);
        sb.append(". Cinematic lighting, professional photography, masterpiece, ultra-realistic.");
        
        final String finalPrompt = sb.toString();

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            final String token = "Bearer " + result.getToken();
            RetrofitClient.getService().generateImage(token, new ImageRequest(finalPrompt)).enqueue(new Callback<ImageResponse>() {
                @Override
                public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                    loadingOverlay.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        String b64 = response.body().getImageBase64();
                        if (b64 != null) {
                            String data = b64.contains(",") ? b64.split(",")[1] : b64;
                            pendingImageData = Base64.decode(data, Base64.DEFAULT);
                            Glide.with(BotProfileActivity.this).load(pendingImageData).into(imgBotProfile);
                        }
                    } else if (response.code() == 403) {
                        Toast.makeText(BotProfileActivity.this, "Daily limit reached. Upgrade for more!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(BotProfileActivity.this, "Generation failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<ImageResponse> call, Throwable t) { 
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(BotProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void generateWallpaper() {
        if (!canGenerateImage()) {
            Toast.makeText(this, "Daily photo limit reached. Upgrade for more!", Toast.LENGTH_LONG).show();
            return;
        }
        final String userPrompt = etWallpaperPrompt.getText().toString().trim();
        if (userPrompt.isEmpty()) return;
        loadingOverlay.setVisibility(View.VISIBLE);
        final String wallpaperPrompt = "Full-screen vertical 9:16 portrait smartphone background wallpaper, aesthetic, high quality, " + userPrompt;
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            final String token = "Bearer " + result.getToken();
            RetrofitClient.getService().generateImage(token, new ImageRequest(wallpaperPrompt)).enqueue(new Callback<ImageResponse>() {
                @Override
                public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String b64 = response.body().getImageBase64();
                        if (b64 != null) {
                            String data = b64.contains(",") ? b64.split(",")[1] : b64;
                            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                            uploadImageToStorage(bytes, "chatBackgroundUrl", null);
                        }
                    } else {
                        loadingOverlay.setVisibility(View.GONE);
                        if (response.code() == 403) {
                            Toast.makeText(BotProfileActivity.this, "Daily limit reached!", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                @Override public void onFailure(Call<ImageResponse> call, Throwable t) { loadingOverlay.setVisibility(View.GONE); }
            });
        });
    }

    private void uploadImageToStorage(byte[] data, String firestoreKey, AlertDialog dialog) {
        loadingOverlay.setVisibility(View.VISIBLE);
        String path = "bots/" + bot.botId + "/" + firestoreKey + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseManager.getStorage().getReference().child(path);
        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            String downloadUrl = uri.toString();
            FirebaseFirestore.getInstance().collection("bots").document(bot.botId)
                    .update(firestoreKey, downloadUrl)
                    .addOnSuccessListener(aVoid -> {
                        loadingOverlay.setVisibility(View.GONE);
                        if (firestoreKey.equals("imageUrl")) bot.imageUrl = downloadUrl;
                        else bot.chatBackgroundUrl = downloadUrl;
                        
                        displayBotData();
                        pendingImageData = null;
                        if (dialog != null) dialog.dismiss();
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    });
        })).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Cloud upload failed", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_WALLPAPER) {
                uploadWallpaperUri(data.getData());
            }
        }
    }

    private void uploadWallpaperUri(Uri uri) {
        loadingOverlay.setVisibility(View.VISIBLE);
        String path = "bots/" + bot.botId + "/chatBackgroundUrl_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseManager.getStorage().getReference().child(path);
        ref.putFile(uri).addOnSuccessListener(ts -> ref.getDownloadUrl().addOnSuccessListener(dUri -> {
            String downloadUrl = dUri.toString();
            FirebaseFirestore.getInstance().collection("bots").document(bot.botId)
                    .update("chatBackgroundUrl", downloadUrl)
                    .addOnSuccessListener(aVoid -> {
                        loadingOverlay.setVisibility(View.GONE);
                        bot.chatBackgroundUrl = downloadUrl;
                        displayBotData();
                        Toast.makeText(this, "Wallpaper updated!", Toast.LENGTH_SHORT).show();
                    });
        })).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
    }
}
