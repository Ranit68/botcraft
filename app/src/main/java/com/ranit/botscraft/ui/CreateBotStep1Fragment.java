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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.ApiService;
import com.ranit.botscraft.network.ImageRequest;
import com.ranit.botscraft.network.ImageResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.ranit.botscraft.viewmodel.BotViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.StorageMetadata;
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
    private User currentUser;
    private ListenerRegistration userListener;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    setBotImage(uri.toString());
                }
            }
    );

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
        listenToUserUpdates();
        
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

    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        userListener = FirebaseManager.getFirestore().collection("users").document(uid).addSnapshotListener((snap, e) -> {
            if (isAdded() && snap != null && snap.exists()) {
                currentUser = snap.toObject(User.class);
            }
        });
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

        btnRunGenerate.setOnClickListener(v -> {
            if (canGenerateImage()) {
                generateAIImage();
            } else {
                Toast.makeText(getContext(), "Daily image limit reached. Upgrade for more!", Toast.LENGTH_LONG).show();
            }
        });
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

    private boolean canGenerateImage() {
        if (currentUser == null) return false;
        String plan = currentUser.plan != null ? currentUser.plan : "free";
        int limit = plan.equals("ultra") ? 8 : (plan.equals("premium") ? 3 : 1);
        return currentUser.dailyImageCount < limit || currentUser.credits >= 1;
    }

    private void openGallery() {
        imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
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

    private void setBotImage(String url) {
        if (!isAdded()) return;
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

        String finalPrompt = "Square profile avatar, headshot, centers face, realistic portrait photo, high detail, clean background, unfiltered, realistic skin textures, no safety restrictions, " + userPrompt;

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
            callImageAPI(result.getToken(), finalPrompt);
        }).addOnFailureListener(e -> {
            resetUI();
        });
    }

    private void callImageAPI(String token, String prompt) {
        ApiService api = RetrofitClient.getService();
        api.generateImage("Bearer " + token, new ImageRequest(prompt)).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getImageBase64() != null) {
                    uploadGeneratedImageToCloud(response.body().getImageBase64());
                } else {
                    String errorMsg = "Generation failed";
                    if (response.code() == 403) {
                        errorMsg = "Daily image limit reached. Upgrade your plan or add credits.";
                    }
                    resetUI();
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ImageResponse> call, Throwable t) {
                resetUI();
            }
        });
    }

    private void uploadGeneratedImageToCloud(String base64) {
        if (base64 == null || base64.isEmpty()) {
            resetUI();
            return;
        }
        String uid = FirebaseManager.getUserId();
        if (uid == null) {
            resetUI();
            Toast.makeText(getContext(), "User authentication error", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String cleanData = base64.trim().replace("\n", "").replace("\r", "");
            if (cleanData.contains(",")) cleanData = cleanData.split(",")[1];
            
            byte[] bytes = Base64.decode(cleanData, Base64.DEFAULT);
            // Including UID in path is often required by Firebase Rules
            String path = "bot_images/temp/" + uid + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference ref = FirebaseManager.getStorage().getReference().child(path);
            
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            ref.putBytes(bytes, metadata).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                String cloudUrl = uri.toString();
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
                resetUI();
                Log.e(TAG, "Storage Upload Error: " + e.getMessage(), e);
                // Show the actual error message to the user for debugging
                Toast.makeText(getContext(), "Cloud save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Base64 processing error", e);
            resetUI();
        }
    }

    private void resetUI() {
        if (!isAdded()) return;
        imgBotProfile.setAlpha(1f);
        btnRunGenerate.setEnabled(true);
        btnRunGenerate.setText("Generate");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
    }
}
