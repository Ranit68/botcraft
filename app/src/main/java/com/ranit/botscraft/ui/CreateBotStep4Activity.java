package com.ranit.botscraft.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class CreateBotStep4Activity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;

    EditText etPrompt;
    Button btnGenerate, btnFinish;
    ImageView imgBot;
    ProgressBar progressBar;

    Bot bot;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_bot_step4);

        bot = (Bot) getIntent().getSerializableExtra("bot");
        if (bot == null) {
            Toast.makeText(this, "Bot data missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        imgBot = findViewById(R.id.imgBot);
        etPrompt = findViewById(R.id.etPrompt);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnFinish = findViewById(R.id.btnFinish);
        progressBar = findViewById(R.id.progressBar);

        imgBot.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnFinish.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToFirebase();
            } else {
                saveBotToFirestore();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(this).load(selectedImageUri).into(imgBot);
        }
    }

    private void uploadImageToFirebase() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        String fileName = "bot_images/" + UUID.randomUUID().toString();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileName);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        bot.imageUrl = task.getResult().toString();
                        saveBotToFirestore();
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveBotToFirestore() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        bot.ownerId = uid;
        bot.createdAt = System.currentTimeMillis();
        String docId = (bot.botId != null) ? bot.botId : UUID.randomUUID().toString();
        
        FirebaseFirestore.getInstance()
                .collection("bots")
                .document(docId)
                .set(bot)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Bot Created Successfully", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
