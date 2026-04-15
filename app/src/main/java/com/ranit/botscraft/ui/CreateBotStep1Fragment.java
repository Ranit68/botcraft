package com.ranit.botscraft.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.network.ApiService;
import com.ranit.botscraft.network.ImageRequest;
import com.ranit.botscraft.network.ImageResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.ranit.botscraft.viewmodel.BotViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.StorageReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBotStep1Fragment extends Fragment {

    private static final int PICK_IMAGE = 101;
    private static final String TAG = "APIIMAGE";

    private ImageView imgBotProfile;
    private EditText etBotName, etAIPrompt;
    private Button btnGenerateAI, btnContinue, btnGallery, btnRunGenerate;
    private LinearLayout llAIPrompt, btnMale, btnFemale, btnOther;
    private TextView tvAgeValue;
    private SeekBar sbAge;

    private String selectedGender = "MALE";
    private BotViewModel botViewModel;

    public CreateBotStep1Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_bot_step1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        botViewModel = new ViewModelProvider(requireActivity()).get(BotViewModel.class);

        initViews(view);
        setupListeners();
        updateGenderUI();
        
        Bot bot = botViewModel.getBotData().getValue();
        if (bot != null) {
            etBotName.setText(bot.name);
            sbAge.setProgress((int) bot.age);
            tvAgeValue.setText(String.valueOf(bot.age));
            selectedGender = bot.gender != null ? bot.gender : "MALE";
            updateGenderUI();
            if (bot.imageUrl != null) {
                imgBotProfile.setImageTintList(null);
                Glide.with(this).load(bot.imageUrl).transform(new CircleCrop()).into(imgBotProfile);
            }
        }
    }

    private void initViews(View view) {
        imgBotProfile = view.findViewById(R.id.imgBotProfile);
        etBotName = view.findViewById(R.id.etBotName);
        etAIPrompt = view.findViewById(R.id.etAIPrompt);
        btnGenerateAI = view.findViewById(R.id.btnGenerateAI);
        btnContinue = view.findViewById(R.id.btnContinue);
        btnGallery = view.findViewById(R.id.btnGallery);
        btnRunGenerate = view.findViewById(R.id.btnRunGenerate);
        llAIPrompt = view.findViewById(R.id.llAIPrompt);
        tvAgeValue = view.findViewById(R.id.tvAgeValue);
        sbAge = view.findViewById(R.id.sbAge);

        btnMale = view.findViewById(R.id.btnMale);
        btnFemale = view.findViewById(R.id.btnFemale);
        btnOther = view.findViewById(R.id.btnOther);
    }

    private void setupListeners() {
        btnGenerateAI.setOnClickListener(v -> {
            llAIPrompt.setVisibility(
                    llAIPrompt.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        btnRunGenerate.setOnClickListener(v -> generateAIImage());
        btnGallery.setOnClickListener(v -> openGallery());

        btnMale.setOnClickListener(v -> { selectedGender = "MALE"; updateGenderUI(); });
        btnFemale.setOnClickListener(v -> { selectedGender = "FEMALE"; updateGenderUI(); });
        btnOther.setOnClickListener(v -> { selectedGender = "OTHER"; updateGenderUI(); });

        sbAge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 18) progress = 18;
                tvAgeValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnContinue.setOnClickListener(v -> validateAndNext());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void updateGenderUI() {
        btnMale.setBackgroundResource(selectedGender.equals("MALE") ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
        btnFemale.setBackgroundResource(selectedGender.equals("FEMALE") ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
        btnOther.setBackgroundResource(selectedGender.equals("OTHER") ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
    }

    private void validateAndNext() {
        String name = etBotName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter bot name", Toast.LENGTH_SHORT).show();
            return;
        }

        Bot bot = botViewModel.getBotData().getValue();
        if (bot == null) bot = new Bot();

        bot.name = name;
        bot.gender = selectedGender;
        bot.age = sbAge.getProgress();
        
        botViewModel.updateBot(bot);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, new CreateBotStep2Fragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE && data != null) {
                Uri selectedImageUri = data.getData();
                if (isAdded() && selectedImageUri != null) {
                    setBotImage(selectedImageUri.toString());
                }
            }
        }
    }

    private void setBotImage(String url) {
        imgBotProfile.setImageTintList(null);
        Glide.with(requireContext()).load(url).transform(new CircleCrop()).into(imgBotProfile);
        Bot bot = botViewModel.getBotData().getValue();
        if (bot == null) bot = new Bot();
        bot.imageUrl = url;
        botViewModel.updateBot(bot);
    }

    private void generateAIImage() {
        String userPrompt = etAIPrompt.getText().toString().trim();
        if (userPrompt.isEmpty()) {
            Toast.makeText(getContext(), "Enter prompt first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        imgBotProfile.setAlpha(0.3f);
        btnRunGenerate.setEnabled(false);
        btnRunGenerate.setText("Generating...");

        String finalPrompt = "Square profile avatar, headshot, centers face, realistic art, high detail, clean background, " + userPrompt;

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            Log.d("TOKEN", "Token OK, calling API...");
            callImageAPI(result.getToken(), finalPrompt);
        }).addOnFailureListener(e -> {
            Log.e("TOKEN", "Auth Token Error: " + e.getMessage());
            resetUI();
        });
    }

    private void callImageAPI(String token, String prompt) {
        ApiService api = RetrofitClient.getService();
        api.generateImage("Bearer " + token, new ImageRequest(prompt)).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Success, received image base64");
                    String base64 = response.body().getImageBase64();
                    if (base64 != null && !base64.isEmpty()) {
                        uploadGeneratedImageToCloud(base64);
                    } else {
                        Log.e(TAG, "Empty image data in response");
                        resetUI();
                    }
                } else {
                    String errorMsg = "Generation failed";
                    if (response.code() == 403) {
                        errorMsg = "Daily image limit reached. Upgrade your plan or add credits.";
                    }
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "API ERROR: " + response.code() + " | " + errorBody);
                    } catch (Exception ignored) {}
                    resetUI();
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ImageResponse> call, Throwable t) {
                Log.e(TAG, "Network Failure: " + t.getMessage());
                resetUI();
            }
        });
    }

    private void uploadGeneratedImageToCloud(String base64) {
        try {
            String data = base64.trim().replace("\n", "").replace("\r", "");
            if (data.contains(",")) data = data.split(",")[1];
            
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            Log.d(TAG, "Decoded " + bytes.length + " bytes, starting upload...");
            
            String path = "temp/" + System.currentTimeMillis() + ".jpg";
            StorageReference ref = FirebaseManager.getStorage().getReference().child(path);
            
            ref.putBytes(bytes).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                String cloudUrl = uri.toString();
                Log.d(TAG, "Upload success! Cloud URL: " + cloudUrl);
                
                Bot bot = botViewModel.getBotData().getValue();
                if (bot == null) bot = new Bot();
                bot.imageUrl = cloudUrl;
                botViewModel.updateBot(bot);
                
                if (isAdded()) {
                    imgBotProfile.setImageTintList(null);
                    Glide.with(requireContext()).load(cloudUrl).transform(new CircleCrop()).into(imgBotProfile);
                }
                resetUI();
            })).addOnFailureListener(e -> {
                Log.e(TAG, "UPLOAD ERROR: " + e.getMessage());
                resetUI();
                Toast.makeText(getContext(), "Cloud save failed", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Base64 Decode Error: " + e.getMessage());
            resetUI();
        }
    }

    private void resetUI() {
        if (!isAdded()) return;
        imgBotProfile.setAlpha(1f);
        btnRunGenerate.setEnabled(true);
        btnRunGenerate.setText("Generate");
    }
}
