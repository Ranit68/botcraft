package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateBotStep1Activity extends AppCompatActivity {

    private EditText etBotName, etAIPrompt;
    private Button btnGenerateAI, btnRunGenerate, btnGallery, btnContinue;
    private LinearLayout llAIPrompt;
    private TextView tvAgeValue, btnBack;
    private SeekBar sbAge;
    
    private String selectedGender = "MALE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_bot_step1);

        // Initialize Views
        etBotName = findViewById(R.id.etBotName);
        etAIPrompt = findViewById(R.id.etAIPrompt);
        btnGenerateAI = findViewById(R.id.btnGenerateAI);
        btnRunGenerate = findViewById(R.id.btnRunGenerate);
        btnGallery = findViewById(R.id.btnGallery);
        btnContinue = findViewById(R.id.btnContinue);
        llAIPrompt = findViewById(R.id.llAIPrompt);
        tvAgeValue = findViewById(R.id.tvAgeValue);
        sbAge = findViewById(R.id.sbAge);
        btnBack = findViewById(R.id.btnBack);

        checkBotCreationLimit();

        // Toggle AI Prompt visibility
        btnGenerateAI.setOnClickListener(v -> {
            if (llAIPrompt.getVisibility() == View.VISIBLE) {
                llAIPrompt.setVisibility(View.GONE);
            } else {
                llAIPrompt.setVisibility(View.VISIBLE);
            }
        });

        // Age SeekBar listener
        sbAge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvAgeValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnBack.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> goNext());
    }

    private void checkBotCreationLimit() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            User user = userDoc.toObject(User.class);
            if (user == null) return;

            db.collection("bots").whereEqualTo("ownerId", uid).get().addOnSuccessListener(botSnap -> {
                int currentBotCount = botSnap.size();
                String plan = user.plan != null ? user.plan : "free";
                int limit = 1;

                if ("premium".equals(plan)) limit = 3;
                else if ("ultra".equals(plan)) limit = Integer.MAX_VALUE;

                if (currentBotCount >= limit) {
                    Toast.makeText(this, "Bot creation limit reached for " + plan + " plan (" + limit + ")", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        });
    }

    private void goNext() {
        String name = etBotName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a bot name", Toast.LENGTH_SHORT).show();
            return;
        }

        Bot bot = new Bot();
        bot.name = name;
        bot.gender = selectedGender;
        bot.age = sbAge.getProgress();

        // Continue to step 2...
        Toast.makeText(this, "Identity saved. Moving to Step 2...", Toast.LENGTH_SHORT).show();
    }
}
