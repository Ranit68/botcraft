package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.R;
import com.ranit.botscraft.model.Bot;

public class CreateBotStep3Activity extends AppCompatActivity {

    EditText etName, etAge;
    Spinner spGender;
    Button btnNext;
    Bot bot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_bot_step3);

        bot = (Bot) getIntent().getSerializableExtra("bot");

        // 🔐 SAFETY CHECK
        if (bot == null) {
            Toast.makeText(this,
                    "Bot data missing. Please start again.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        spGender = findViewById(R.id.spGender);
        btnNext = findViewById(R.id.btnNext);

        // ✅ SET SPINNER ADAPTER
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.gender_options,
                        android.R.layout.simple_spinner_item
                );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spGender.setAdapter(adapter);
        spGender.setSelection(0); // "Select Gender"

        btnNext.setOnClickListener(v -> goNext());
    }

    private void goNext() {

        String name = etName.getText().toString().trim();
        String ageText = etAge.getText().toString().trim();
        String gender = spGender.getSelectedItem() != null
                ? spGender.getSelectedItem().toString()
                : null;

        // 🛑 VALIDATION
        if (name.isEmpty()) {
            etName.setError("Enter bot name");
            return;
        }

        if (ageText.isEmpty()) {
            etAge.setError("Enter age");
            return;
        }

        if (gender == null || gender.equals("Select Gender")) {
            Toast.makeText(this,
                    "Please select gender",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ SAVE DATA
        bot.name = name;
        bot.age = Integer.parseInt(ageText);
        bot.gender = gender;

        // ➡ NEXT STEP
        Intent i = new Intent(this, CreateBotStep4Activity.class);
        i.putExtra("bot", bot);
        startActivity(i);
    }
}
