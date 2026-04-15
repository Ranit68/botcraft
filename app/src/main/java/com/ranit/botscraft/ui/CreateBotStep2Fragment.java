package com.ranit.botscraft.ui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.SelectionAdapter;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.SelectionItem;
import com.ranit.botscraft.viewmodel.BotViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreateBotStep2Fragment extends Fragment {

    private static final String TAG = "VOICE_LOG";

    private RecyclerView rvHairColor, rvHairStyle, rvSkinTone, rvModelType, rvBodyType;
    private Button btnContinue;
    private TextView btnBack;
    private BotViewModel botViewModel;
    private MediaPlayer mediaPlayer;

    public CreateBotStep2Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_bot_step2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        botViewModel = new ViewModelProvider(requireActivity()).get(BotViewModel.class);

        rvHairColor = view.findViewById(R.id.rvHairColor);
        rvHairStyle = view.findViewById(R.id.rvHairStyle);
        rvSkinTone = view.findViewById(R.id.rvSkinTone);
        rvModelType = view.findViewById(R.id.rvModelType);
        rvBodyType = view.findViewById(R.id.rvBodyType);
        btnContinue = view.findViewById(R.id.btnContinue);
        btnBack = view.findViewById(R.id.btnBack);

        setupRecyclerViews();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnContinue.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreateBotStep3Fragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupRecyclerViews() {
        Bot bot = botViewModel.getBotData().getValue();
        if (bot == null) bot = new Bot();
        final Bot finalBot = bot;

        List<SelectionItem> hairColors = new ArrayList<>();
        hairColors.add(SelectionItem.color("black", "Black", "#000000"));
        hairColors.add(SelectionItem.color("brown", "Brown", "#4B2C20"));
        hairColors.add(SelectionItem.color("blonde", "Blonde", "#E4C079"));
        hairColors.add(SelectionItem.color("red", "Red", "#8B0000"));
        rvHairColor.setAdapter(new SelectionAdapter(hairColors, R.layout.item_hair_color, item -> finalBot.hairColor = item.id));

        List<SelectionItem> hairStyles = new ArrayList<>();
        hairStyles.add(new SelectionItem("short", "Short"));
        hairStyles.add(new SelectionItem("long", "Long"));
        hairStyles.add(new SelectionItem("curly", "Curly"));
        hairStyles.add(new SelectionItem("bald", "Bald"));
        rvHairStyle.setAdapter(new SelectionAdapter(hairStyles, R.layout.item_hair_style, item -> finalBot.hairStyle = item.id));

        List<SelectionItem> skinTones = new ArrayList<>();
        skinTones.add(SelectionItem.color("light", "Light", "#F3D9C1"));
        skinTones.add(SelectionItem.color("medium", "Medium", "#D5AC85"));
        skinTones.add(SelectionItem.color("dark", "Dark", "#8D5524"));
        rvSkinTone.setAdapter(new SelectionAdapter(skinTones, R.layout.item_skin_tone, item -> finalBot.skinTone = item.id));

        List<SelectionItem> models = new ArrayList<>();
        models.add(new SelectionItem("asian", "Asian"));
        models.add(new SelectionItem("indian", "Indian"));
        models.add(new SelectionItem("arab", "Arab"));
        models.add(new SelectionItem("euro", "European"));
        rvModelType.setAdapter(new SelectionAdapter(models, R.layout.item_model_type, item -> finalBot.modelType = item.id));

        List<SelectionItem> bodies = new ArrayList<>();
        bodies.add(new SelectionItem("slim", "Slim"));
        bodies.add(new SelectionItem("fit", "Fit"));
        bodies.add(new SelectionItem("athletic", "Athletic"));
        bodies.add(new SelectionItem("curvy", "Curvy"));
        rvBodyType.setAdapter(new SelectionAdapter(bodies, R.layout.item_body_type, item -> finalBot.bodyType = item.id));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }
}
