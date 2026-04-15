package com.ranit.botscraft.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.SelectionAdapter;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.SelectionItem;
import com.ranit.botscraft.viewmodel.BotViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreateBotStep4Fragment extends Fragment {

    private RecyclerView rvRelationship;
    private EditText etPersonality, etAppearance;
    private TextView tvPreviewName, tvPreviewRelationship, btnBack;
    private ImageView imgBotPreview;
    private Button btnNext;
    private BotViewModel botViewModel;
    private List<SelectionItem> relationshipItems;
    private SelectionAdapter relationshipAdapter;

    public CreateBotStep4Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_bot_step4, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        botViewModel = new ViewModelProvider(requireActivity()).get(BotViewModel.class);
        Bot botData = botViewModel.getBotData().getValue();
        if (botData == null) botData = new Bot();
        final Bot bot = botData;

        rvRelationship = view.findViewById(R.id.rvRelationship);
        etPersonality = view.findViewById(R.id.etPersonality);
        etAppearance = view.findViewById(R.id.etAppearance);
        tvPreviewName = view.findViewById(R.id.tvPreviewName);
        tvPreviewRelationship = view.findViewById(R.id.tvPreviewRelationship);
        imgBotPreview = view.findViewById(R.id.imgBotPreview);
        btnNext = view.findViewById(R.id.btnNext);
        btnBack = view.findViewById(R.id.btnBack);

        tvPreviewName.setText(bot.name != null ? bot.name : "Bot Name");
        if (bot.relationship != null) tvPreviewRelationship.setText(bot.relationship);
        
        if (bot.imageUrl != null) {
            Glide.with(this).load(bot.imageUrl).circleCrop().into(imgBotPreview);
        }

        setupRelationshipList(bot);
        setupInputListeners(bot);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnNext.setOnClickListener(v -> {
            if (validateInputs(bot)) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CreateBotStep5Fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void setupRelationshipList(final Bot bot) {
        relationshipItems = new ArrayList<>();
        relationshipItems.add(new SelectionItem("friend", "Friend"));
        relationshipItems.add(new SelectionItem("girlfriend", "Girlfriend"));
        relationshipItems.add(new SelectionItem("mom", "Mom"));
        relationshipItems.add(new SelectionItem("step_mom", "Step Mom"));
        relationshipItems.add(new SelectionItem("step_sister", "Step Sister"));
        relationshipItems.add(new SelectionItem("step_brother", "Step Brother"));
        relationshipItems.add(new SelectionItem("sister", "Sister"));
        relationshipItems.add(new SelectionItem("teacher", "Teacher"));
        relationshipItems.add(new SelectionItem("colleague", "Colleague"));
        
        SelectionItem addItem = new SelectionItem("add_custom", "+ Custom");
        relationshipItems.add(addItem);

        rvRelationship.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        relationshipAdapter = new SelectionAdapter(relationshipItems, R.layout.item_model_type, item -> {
            if (item.id.equals("add_custom")) {
                showCustomRelationshipDialog(bot);
            } else {
                bot.relationship = item.label;
                tvPreviewRelationship.setText(item.label);
            }
        });
        rvRelationship.setAdapter(relationshipAdapter);
    }

    private void showCustomRelationshipDialog(final Bot bot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Custom Relationship");
        final EditText input = new EditText(getContext());
        input.setHint("e.g. Secret Agent");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String customRel = input.getText().toString().trim();
            if (!customRel.isEmpty()) {
                bot.relationship = customRel;
                tvPreviewRelationship.setText(customRel);
                
                SelectionItem newItem = new SelectionItem(customRel.toLowerCase(), customRel);
                relationshipItems.add(relationshipItems.size() - 1, newItem);
                relationshipAdapter.notifyItemInserted(relationshipItems.size() - 2);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupInputListeners(final Bot bot) {
        etPersonality.setText(bot.personality);
        etAppearance.setText(bot.description);

        etPersonality.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                bot.personality = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etAppearance.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                bot.description = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateInputs(Bot bot) {
        if (bot.relationship == null || bot.relationship.isEmpty()) {
            Toast.makeText(getContext(), "Please select a relationship", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (bot.personality == null || bot.personality.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please describe bot's personality", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
