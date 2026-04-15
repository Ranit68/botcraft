package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ranit.botscraft.R;
import com.ranit.botscraft.model.Bot;

public class CreateBotStep2Activity extends AppCompatActivity {

    EditText etPersonality, etRelation;
    Button btnNext;
    Bot bot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_bot_step2);

        bot = (Bot) getIntent().getSerializableExtra("bot");

        bot = (Bot) getIntent().getSerializableExtra("bot");

        if (bot == null) {
            Toast.makeText(this,
                    "Bot data lost. Please start again.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        etPersonality = findViewById(R.id.etPersonality);
        etRelation = findViewById(R.id.etRelation);
        btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            bot.personality = etPersonality.getText().toString();
            bot.relationship = etRelation.getText().toString();

            Intent i = new Intent(this, CreateBotStep3Activity.class);
            i.putExtra("bot", bot);
            startActivity(i);
        });
    }
}
