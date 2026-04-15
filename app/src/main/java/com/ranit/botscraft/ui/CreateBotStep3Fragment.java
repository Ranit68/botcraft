package com.ranit.botscraft.ui;

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
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateBotStep3Fragment extends Fragment {

    private RecyclerView rvPersonality, rvMoodStyle, rvLanguage;
    private EditText etOccupation, etYourName;
    private View llCategories;
    private TextView tvSelectedCategories;
    private Button btnContinue;
    private TextView btnBack;
    private ImageView imgPreviewBot;
    private BotViewModel botViewModel;

    private final String[] categoryOptions = {
        "Bold", "Normal", "Professional", "Friendly", "Romantic", 
        "Flirty", "Angry", "Indian", "Arab", "European", "Japanese", "Shy", "Mature"
    };
    private final boolean[] selectedItems = new boolean[categoryOptions.length];
    private final List<Integer> userSelectedItems = new ArrayList<>();

    public CreateBotStep3Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_bot_step3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        botViewModel = new ViewModelProvider(requireActivity()).get(BotViewModel.class);
        Bot bot = botViewModel.getBotData().getValue();

        rvPersonality = view.findViewById(R.id.rvPersonality);
        rvMoodStyle = view.findViewById(R.id.rvMoodStyle);
        rvLanguage = view.findViewById(R.id.rvLanguage);
        imgPreviewBot = view.findViewById(R.id.imgPreviewBot);
        
        etOccupation = view.findViewById(R.id.etOccupation);
        etYourName = view.findViewById(R.id.etYourName);
        llCategories = view.findViewById(R.id.llCategories);
        tvSelectedCategories = view.findViewById(R.id.tvSelectedCategories);
        
        btnContinue = view.findViewById(R.id.btnContinue);
        btnBack = view.findViewById(R.id.btnBack);

        if (bot != null && bot.imageUrl != null) {
            Glide.with(this).load(bot.imageUrl).circleCrop().into(imgPreviewBot);
        }

        rvMoodStyle.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvLanguage.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        setupRecyclerViews();
        setupInputListeners();
        setupCategorySelector();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnContinue.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreateBotStep4Fragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupCategorySelector() {
        llCategories.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Chstbot_MaterialAlertDialog);
            builder.setTitle("Select Categories (Max 3)");
            builder.setCancelable(false);

            builder.setMultiChoiceItems(categoryOptions, selectedItems, (dialogInterface, i, b) -> {
                if (b) {
                    if (userSelectedItems.size() < 3) {
                        userSelectedItems.add(i);
                        Collections.sort(userSelectedItems);
                    } else {
                        Toast.makeText(getContext(), "You can select max 3 categories", Toast.LENGTH_SHORT).show();
                        ((AlertDialog) dialogInterface).getListView().setItemChecked(i, false);
                        selectedItems[i] = false;
                    }
                } else {
                    userSelectedItems.remove(Integer.valueOf(i));
                }
            });

            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                StringBuilder stringBuilder = new StringBuilder();
                List<String> categories = new ArrayList<>();
                for (int j = 0; j < userSelectedItems.size(); j++) {
                    String cat = categoryOptions[userSelectedItems.get(j)];
                    categories.add(cat.toLowerCase());
                    stringBuilder.append(cat);
                    if (j != userSelectedItems.size() - 1) {
                        stringBuilder.append(", ");
                    }
                }
                tvSelectedCategories.setText(stringBuilder.toString());
                
                Bot bot = botViewModel.getBotData().getValue();
                if (bot != null) bot.categories = categories;
            });

            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

            builder.setNeutralButton("Clear All", (dialogInterface, i) -> {
                for (int j = 0; j < selectedItems.length; j++) {
                    selectedItems[j] = false;
                }
                userSelectedItems.clear();
                tvSelectedCategories.setText("");
                
                Bot bot = botViewModel.getBotData().getValue();
                if (bot != null) bot.categories = new ArrayList<>();
            });

            builder.show();
        });
    }

    private void setupRecyclerViews() {
        Bot bot = botViewModel.getBotData().getValue();
        if (bot == null) bot = new Bot();
        final Bot finalBot = bot;

        List<SelectionItem> personalities = new ArrayList<>();
        personalities.add(new SelectionItem("friendly", "Friendly", android.R.drawable.btn_star));
        personalities.add(new SelectionItem("angry", "Angry", android.R.drawable.ic_delete));
        personalities.add(new SelectionItem("flirty", "Flirty", android.R.drawable.btn_star_big_on));
        personalities.add(new SelectionItem("shy", "Shy", android.R.drawable.ic_menu_view));
        rvPersonality.setAdapter(new SelectionAdapter(personalities, R.layout.item_personality, item -> finalBot.personality = item.id));

        List<SelectionItem> moods = new ArrayList<>();
        moods.add(new SelectionItem("dominant", "Dominant"));
        moods.add(new SelectionItem("punisher", "Punisher"));
        moods.add(new SelectionItem("soft", "Soft"));
        moods.add(new SelectionItem("professional", "Professional"));
        rvMoodStyle.setAdapter(new SelectionAdapter(moods, R.layout.item_model_type, item -> finalBot.moodStyle = item.id));

        List<SelectionItem> languages = new ArrayList<>();
        languages.add(new SelectionItem("en", "English"));
        languages.add(new SelectionItem("es", "Spanish"));
        languages.add(new SelectionItem("fr", "French"));
        languages.add(new SelectionItem("hi", "Hindi"));
        rvLanguage.setAdapter(new SelectionAdapter(languages, R.layout.item_model_type, item -> finalBot.language = item.id));
    }

    private void setupInputListeners() {
        Bot bot = botViewModel.getBotData().getValue();
        if (bot == null) return;

        if (etOccupation != null) {
            etOccupation.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    bot.occupation = s.toString();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (etYourName != null) {
            etYourName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    bot.userName = s.toString();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
}
