package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.R;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.repository.BotRepository;

public class CreateBotActivity extends AppCompatActivity {

    EditText etName, etGender, etAge, etRelation, etPersonality, etDescription;
    Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bot);


        btnCreate.setOnClickListener(v -> createBot());
    }

    private void createBot() {

        Bot bot = new Bot();
        bot.name = etName.getText().toString();
        bot.gender = etGender.getText().toString();
        bot.age = Integer.parseInt(etAge.getText().toString());
        bot.relationship = etRelation.getText().toString();
        bot.personality = etPersonality.getText().toString();
        bot.description = etDescription.getText().toString();

        BotRepository.createBot(bot, success -> {
            if (success) {
                Toast.makeText(this, "Bot Created", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to create bot", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
