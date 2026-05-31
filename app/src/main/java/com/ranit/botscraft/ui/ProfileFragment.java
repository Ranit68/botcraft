package com.ranit.botscraft.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.BlockedBotAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.PurchaseVerificationRequest;
import com.ranit.botscraft.network.PurchaseVerificationResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.ranit.botscraft.util.ImageDataHolder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.common.collect.ImmutableList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements PurchasesUpdatedListener {
    private static final String TAG = "ProfileFragment";
    
    public static final String PLAN_PREMIUM = "plan_premium";
    public static final String PLAN_ULTRA = "plan_ultra";

    private ShapeableImageView imgProfile;
    private TextView tvUserName, tvUserEmail, tvPlanName, tvCreditsValue, tvBotsCount, tvChatsCount, tvImagesCount;
    private ProgressBar pbCredits;
    private MaterialSwitch switchTheme, switchNotifications, switchPinLock, switchBiometric;
    private View btnLogout, cvImagesCount, cvBotsCount, btnUpgrade, btnEditProfile, btnBlockedBots, btnAccountSettings, btnPrivacySecurity, llBiometric;
    private FirebaseFirestore db;
    private String uid;
    private User currentUser;
    private ListenerRegistration userListener, botsListener, chatsListener, imagesListener;
    
    private BillingClient billingClient;
    private Dialog upgradeDialog;
    private Uri selectedImageUri;
    private ImageView dialogProfileImage;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (dialogProfileImage != null && uri != null) {
                        Glide.with(this).load(uri).circleCrop().into(dialogProfileImage);
                        selectedImageUri = uri;
                    }
                }
            }
    );

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseManager.getFirestore();
        uid = FirebaseManager.getUserId();
        initViews(view);
        setupBillingClient();
        setupListeners();
        startListening();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client Setup Success");
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing Service Disconnected");
            }
        });
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.imgProfile);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvPlanName = view.findViewById(R.id.tvPlanName);
        tvCreditsValue = view.findViewById(R.id.tvCreditsValue);
        tvBotsCount = view.findViewById(R.id.tvBotsCount);
        tvChatsCount = view.findViewById(R.id.tvChatsCount);
        tvImagesCount = view.findViewById(R.id.tvImagesCount);
        pbCredits = view.findViewById(R.id.pbCredits);
        switchTheme = view.findViewById(R.id.switchTheme);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchPinLock = view.findViewById(R.id.switchPinLock);
        switchBiometric = view.findViewById(R.id.switchBiometric);
        llBiometric = view.findViewById(R.id.llBiometric);
        checkBiometricSupport();
        btnLogout = view.findViewById(R.id.btnLogout);
        cvImagesCount = view.findViewById(R.id.cvImagesCount);
        cvBotsCount = view.findViewById(R.id.cvBotsCount);
        btnUpgrade = view.findViewById(R.id.btnUpgrade);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnBlockedBots = view.findViewById(R.id.btnBlockedBots);
        btnAccountSettings = view.findViewById(R.id.btnAccountSettings);
        btnPrivacySecurity = view.findViewById(R.id.btnPrivacySecurity);

        if (getContext() != null) {
            int nightModeFlags = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switchTheme.setChecked(nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
        }
    }

    private void setupListeners() {
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (uid != null) {
                db.collection("users").document(uid).update("notificationsEnabled", isChecked);
            }
        });

        if (switchPinLock != null) {
            switchPinLock.setOnCheckedChangeListener((button, isChecked) -> {
                if (currentUser == null) return;
                if (isChecked && (currentUser.pinCode == null || currentUser.pinCode.isEmpty())) {
                    showSetPinDialog();
                } else {
                    db.collection("users").document(uid).update("securityEnabled", isChecked);
                    saveSecurityPreference(isChecked);
                }
            });
        }

        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((button, isChecked) -> {
                if (uid != null) {
                    db.collection("users").document(uid).update("biometricEnabled", isChecked);
                }
            });
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cvImagesCount.setOnClickListener(v -> openImageViewer());
        if (cvBotsCount != null) cvBotsCount.setOnClickListener(v -> showCreatedBotsBottomSheet());
        if (btnUpgrade != null) btnUpgrade.setOnClickListener(v -> showUpgradeDialog());
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        if (btnBlockedBots != null) btnBlockedBots.setOnClickListener(v -> showBlockedBotsBottomSheet());
        if (btnAccountSettings != null) btnAccountSettings.setOnClickListener(v -> showDeleteAccountDialog());
        if (btnPrivacySecurity != null) btnPrivacySecurity.setOnClickListener(v -> showPrivacySecurityDialog());
    }

    private void showUpgradeDialog() {
        if (currentUser == null) return;

        upgradeDialog = new Dialog(requireContext());
        upgradeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        upgradeDialog.setContentView(R.layout.dialog_upgrade_plan);
        if (upgradeDialog.getWindow() != null) {
            upgradeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            upgradeDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnFree = upgradeDialog.findViewById(R.id.btnPlanFree);
        View btnPremium = upgradeDialog.findViewById(R.id.btnPlanPremium);
        View btnUltra = upgradeDialog.findViewById(R.id.btnPlanUltra);
        
        RadioButton rbPremium = upgradeDialog.findViewById(R.id.rbPremium);
        RadioButton rbUltra = upgradeDialog.findViewById(R.id.rbUltra);
        
        TextView tvFreeBadge = upgradeDialog.findViewById(R.id.tvFreeBadge);
        TextView tvPremiumBadge = upgradeDialog.findViewById(R.id.tvPremiumBadge);
        TextView tvUltraBadge = upgradeDialog.findViewById(R.id.tvUltraBadge);
        
        MaterialButton btnConfirm = upgradeDialog.findViewById(R.id.btnUpgradeConfirm);
        ProgressBar loading = upgradeDialog.findViewById(R.id.loading);
        ImageView btnClose = upgradeDialog.findViewById(R.id.btnClose);

        final String[] selectedPlan = {""};
        String currentPlan = (currentUser.plan == null) ? "free" : currentUser.plan;

        tvFreeBadge.setVisibility("free".equals(currentPlan) ? View.VISIBLE : View.GONE);
        tvPremiumBadge.setVisibility("premium".equals(currentPlan) ? View.VISIBLE : View.GONE);
        tvUltraBadge.setVisibility("ultra".equals(currentPlan) ? View.VISIBLE : View.GONE);

        btnPremium.setOnClickListener(v -> {
            if ("ultra".equals(currentPlan)) return;
            selectedPlan[0] = PLAN_PREMIUM;
            btnPremium.setBackgroundResource(R.drawable.bg_plan_selected);
            btnUltra.setBackgroundResource(R.drawable.bg_plan_unselected);
            if (rbPremium != null) rbPremium.setChecked(true);
            if (rbUltra != null) rbUltra.setChecked(false);
        });

        btnUltra.setOnClickListener(v -> {
            selectedPlan[0] = PLAN_ULTRA;
            btnUltra.setBackgroundResource(R.drawable.bg_plan_selected);
            btnPremium.setBackgroundResource(R.drawable.bg_plan_unselected);
            if (rbUltra != null) rbUltra.setChecked(true);
            if (rbPremium != null) rbPremium.setChecked(false);
        });

        btnClose.setOnClickListener(v -> upgradeDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (selectedPlan[0].isEmpty() || selectedPlan[0].equals(currentPlan)) return;
            launchBillingFlow(selectedPlan[0]);
        });
        upgradeDialog.show();
    }

    private void launchBillingFlow(String productId) {
        if (!billingClient.isReady()) {
            Toast.makeText(getContext(), "Billing not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                )).build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                ProductDetails productDetails = productDetailsList.get(0);
                
                List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                                .build()
                );

                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();

                billingClient.launchBillingFlow(requireActivity(), flowParams);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    verifySubscriptionOnBackend(purchase);
                }
            }
        }
    }

    private void verifySubscriptionOnBackend(Purchase purchase) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            String productId = purchase.getProducts().get(0);
            Log.d(TAG, "Verifying Subscription: " + productId);

            PurchaseVerificationRequest request = new PurchaseVerificationRequest(purchase.getPurchaseToken(), productId);

            RetrofitClient.getService().verifyPurchase(token, request).enqueue(new Callback<PurchaseVerificationResponse>() {
                @Override
                public void onResponse(Call<PurchaseVerificationResponse> call, Response<PurchaseVerificationResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        Toast.makeText(getContext(), "Upgrade Successful!", Toast.LENGTH_LONG).show();
                        if (upgradeDialog != null) upgradeDialog.dismiss();
                        
                        if (!purchase.isAcknowledged()) {
                            acknowledgeSubscription(purchase);
                        }
                    } else {
                        String errorMsg = "Verification failed";
                        try {
                            if (response.errorBody() != null) errorMsg = response.errorBody().string();
                        } catch (Exception ignored) {}
                        Log.e(TAG, "Server error: " + errorMsg);
                        Toast.makeText(getContext(), "Upgrade Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onFailure(Call<PurchaseVerificationResponse> call, Throwable t) {
                    Log.e(TAG, "Network failure", t);
                    Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void acknowledgeSubscription(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Subscription acknowledged successfully");
            }
        });
    }

    private void startListening() {
        if (uid == null) return;
        userListener = db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                currentUser = doc.toObject(User.class);
                if (currentUser != null) {
                    tvUserName.setText(currentUser.name);
                    tvUserEmail.setText(currentUser.email);
                    String plan = currentUser.plan != null ? currentUser.plan : "free";
                    tvPlanName.setText(plan.toUpperCase() + " PLAN");
                    if (currentUser.imageUrl != null && !currentUser.imageUrl.isEmpty()) {
                        Glide.with(this).load(currentUser.imageUrl).circleCrop().into(imgProfile);
                    } else {
                        imgProfile.setImageResource(R.drawable.user);
                    }
                    updateVisualsForPlan(plan);
                    tvCreditsValue.setText(currentUser.credits + " available");
                    pbCredits.setProgress(currentUser.credits);
                    switchNotifications.setChecked(currentUser.notificationsEnabled);
                    
                    if (switchPinLock != null) {
                        switchPinLock.setChecked(currentUser.securityEnabled);
                    }
                    if (switchBiometric != null) {
                        switchBiometric.setChecked(currentUser.biometricEnabled);
                    }
                }
            }
        });
        botsListener = db.collection("bots").whereEqualTo("ownerId", uid).addSnapshotListener((snap, e) -> {
            if (snap != null) tvBotsCount.setText(String.valueOf(snap.size()));
        });
        chatsListener = db.collection("chats").whereEqualTo("userId", uid).addSnapshotListener((snap, e) -> {
            if (snap != null) tvChatsCount.setText(String.valueOf(snap.size()));
        });
        imagesListener = db.collection("usage").whereEqualTo("userId", uid).whereEqualTo("type", "image").addSnapshotListener((snap, e) -> {
            if (snap != null) tvImagesCount.setText(String.valueOf(snap.size()));
        });
    }

    private void updateVisualsForPlan(String plan) {
        if ("premium".equals(plan)) {
            imgProfile.setStrokeWidth(4f);
            imgProfile.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C0C0C0")));
        } else if ("ultra".equals(plan)) {
            imgProfile.setStrokeWidth(4f);
            imgProfile.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFD700")));
        } else {
            imgProfile.setStrokeWidth(0f);
        }
    }

    private void showPrivacySecurityDialog() {
        String[] options = {"Privacy Policy", "Terms of Service", "About Encryption"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy & Security")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showLocalInfoDialog("Privacy Policy", getPrivacyPolicyText()); break;
                        case 1: showLocalInfoDialog("Terms of Service", getTermsOfServiceText()); break;
                        case 2: showLocalInfoDialog("About Encryption", getEncryptionText()); break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showLocalInfoDialog(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        ScrollView scrollView = new ScrollView(requireContext());
        TextView textView = new TextView(requireContext());
        textView.setText(content);
        textView.setPadding(40, 20, 40, 20);
        textView.setLineSpacing(0, 1.2f);
        textView.setTextColor(Color.parseColor(isDarkMode() ? "#FFFFFF" : "#000000"));
        scrollView.addView(textView);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private String getPrivacyPolicyText() {
        return "Privacy Policy\n\n" +
                "At BotCraft, we value your privacy. We collect your email and name via Firebase Authentication to manage your account and personalize your experience.\n\n" +
                "1. Data Collection: We store your basic profile information and any AI-generated content (images and chats) securely in our cloud database.\n\n" +
                "2. Usage: Your data is used solely to provide and improve the AI companion services. We do not sell your personal data to third parties.\n\n" +
                "3. Security: We use industry-standard security measures to protect your information.\n\n" +
                "4. Controls: You can update your profile or delete your account at any time from this menu.";
    }

    private String getTermsOfServiceText() {
        return "Terms of Service\n\n" +
                "By using BotCraft, you agree to the following terms:\n\n" +
                "1. Age Requirement: You must be at least 18 years old to use this application.\n\n" +
                "2. Content Safety: You agree not to use our AI to generate illegal, harmful, or non-consensual content. Violation of this rule will lead to immediate account termination.\n\n" +
                "3. Credits & Plans: Digital credits and subscription plans are non-refundable.\n\n" +
                "4. Responsibility: You are responsible for maintaining the confidentiality of your account credentials.";
    }

    private String getEncryptionText() {
        return "Data Security & Encryption\n\n" +
                "Your conversations and data are important to us. BotCraft uses secure cloud storage and industry-standard TLS encryption for data in transit.\n\n" +
                "Key Security Features:\n" +
                "• Secure Authentication: Powered by Google Firebase.\n" +
                "• Private Storage: Your chat history is stored in a private database accessible only via your authenticated account.\n" +
                "• AI Privacy: Your messages are processed by Grok AI but are not used for training external public models without your explicit consent.";
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? All your data, credits, and AI companions will be permanently removed. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || uid == null) return;
        db.collection("users").document(uid).delete().addOnSuccessListener(aVoid -> {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                }
            });
        });
    }

    private void showBlockedBotsBottomSheet() {
        if (currentUser == null || currentUser.blockedBots == null || currentUser.blockedBots.isEmpty()) {
            Toast.makeText(getContext(), "No blocked bots found", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Theme_Chstbot_Dialog);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_voice_list, null);
        dialog.setContentView(view);
        
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("Blocked Bots");

        RecyclerView rv = view.findViewById(R.id.rvVoices);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<Bot> blockedList = new ArrayList<>();
        BlockedBotAdapter adapter = new BlockedBotAdapter(blockedList, bot -> {
            db.collection("users").document(uid).update("blockedBots", FieldValue.arrayRemove(bot.botId))
                    .addOnSuccessListener(aVoid -> {
                        blockedList.remove(bot);
                        if (blockedList.isEmpty()) dialog.dismiss();
                        else rv.getAdapter().notifyDataSetChanged();
                    });
        });
        rv.setAdapter(adapter);

        for (String bid : currentUser.blockedBots) {
            db.collection("bots").document(bid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Bot b = doc.toObject(Bot.class);
                    if (b != null) {
                        b.botId = doc.getId();
                        blockedList.add(b);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
        dialog.show();
    }

    private void showCreatedBotsBottomSheet() {
        if (uid == null) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Theme_Chstbot_Dialog);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_voice_list, null);
        dialog.setContentView(view);
        
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("My AI Companions");

        RecyclerView rv = view.findViewById(R.id.rvVoices);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<Bot> myBots = new ArrayList<>();
        CreatedBotAdapter adapter = new CreatedBotAdapter(myBots, bot -> {
            dialog.dismiss();
            bot.sanitizeForIntent();
            Intent intent = new Intent(getActivity(), BotProfileActivity.class);
            intent.putExtra("bot", bot);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        db.collection("bots").whereEqualTo("ownerId", uid).get().addOnSuccessListener(snap -> {
            if (snap != null && !snap.isEmpty()) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                    Bot b = doc.toObject(Bot.class);
                    if (b != null) {
                        b.botId = doc.getId();
                        myBots.add(b);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "You haven't created any bots yet", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }

    private static class CreatedBotAdapter extends RecyclerView.Adapter<CreatedBotAdapter.VH> {
        private final List<Bot> list;
        private final OnBotClickListener listener;

        interface OnBotClickListener { void onBotClick(Bot bot); }

        CreatedBotAdapter(List<Bot> list, OnBotClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_bot, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Bot bot = list.get(position);
            holder.tvName.setText(bot.name);
            holder.tvDetails.setText(bot.relationship + " • " + bot.age + "y");
            
            if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(bot.imageUrl).circleCrop().into(holder.img);
            } else {
                holder.img.setImageResource(R.drawable.user);
            }

            holder.itemView.setOnClickListener(v -> listener.onBotClick(bot));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName, tvDetails;
            VH(View v) {
                super(v);
                img = v.findViewById(R.id.imgMyBot);
                tvName = v.findViewById(R.id.tvMyBotName);
                tvDetails = v.findViewById(R.id.tvMyBotDetails);
            }
        }
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_profile);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogProfileImage = dialog.findViewById(R.id.imgEditProfile);
        EditText etName = dialog.findViewById(R.id.etEditName);
        RadioGroup rgGender = dialog.findViewById(R.id.rgGender);
        RadioButton rbMale = dialog.findViewById(R.id.rbMale);
        RadioButton rbFemale = dialog.findViewById(R.id.rbFemale);
        RadioButton rbOther = dialog.findViewById(R.id.rbOther);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveProfile);

        etName.setText(currentUser.name);

        // Pre-select gender from DB
        if (currentUser.gender != null) {
            if (currentUser.gender.equalsIgnoreCase("Male")) rbMale.setChecked(true);
            else if (currentUser.gender.equalsIgnoreCase("Female")) rbFemale.setChecked(true);
            else if (currentUser.gender.equalsIgnoreCase("Other")) rbOther.setChecked(true);
        }

        if (currentUser.imageUrl != null && !currentUser.imageUrl.isEmpty()) {
            Glide.with(this).load(currentUser.imageUrl).circleCrop().into(dialogProfileImage);
        }
        
        dialogProfileImage.setOnClickListener(v -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name cannot be empty");
                return;
            }

            String gender = "Other";
            int checkedId = rgGender.getCheckedRadioButtonId();
            if (checkedId == R.id.rbMale) gender = "Male";
            else if (checkedId == R.id.rbFemale) gender = "Female";

            if (selectedImageUri != null) uploadProfileImage(selectedImageUri, name, gender, dialog);
            else updateProfileData(name, gender, null, dialog);
        });
        dialog.show();
    }

    private void uploadProfileImage(Uri uri, String name, String gender, Dialog dialog) {
        StorageReference ref = FirebaseManager.getStorage().getReference().child("profiles/" + uid + ".jpg");
        ref.putFile(uri).addOnSuccessListener(ts -> ref.getDownloadUrl().addOnSuccessListener(dUri -> updateProfileData(name, gender, dUri.toString(), dialog)));
    }

    private void updateProfileData(String name, String gender, @Nullable String imageUrl, Dialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("gender", gender);
        if (imageUrl != null) updates.put("imageUrl", imageUrl);
        db.collection("users").document(uid).update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showSetPinDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_profile); // Reusing layout for simplicity or create new
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Custom PIN dialog would be better, but let's use a quick one
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Security PIN");
        
        final EditText input = new EditText(requireContext());
        input.setHint("4-digit PIN");
        input.setGravity(android.view.Gravity.CENTER);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(4) });
        input.setLetterSpacing(0.5f);
        builder.setView(input);

        builder.setPositiveButton("Set", (d, w) -> {
            String pin = input.getText().toString().trim();
            if (pin.length() == 4) {
                db.collection("users").document(uid).update(
                    "pinCode", pin,
                    "securityEnabled", true
                ).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "PIN Set Successfully", Toast.LENGTH_SHORT).show();
                    saveSecurityPreference(true);
                });
            } else {
                Toast.makeText(getContext(), "PIN too short", Toast.LENGTH_SHORT).show();
                switchPinLock.setChecked(false);
            }
        });
        builder.setNegativeButton("Cancel", (d, w) -> switchPinLock.setChecked(false));
        builder.setCancelable(false);
        builder.show();
    }

    private void openImageViewer() {
        if (uid == null) return;
        db.collection("usage")
                .whereEqualTo("userId", uid)
                .whereEqualTo("type", "image")
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    
                    // Sort by timestamp descending (latest first)
                    java.util.Collections.sort(docs, (d1, d2) -> {
                        com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
                        com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1);
                    });

                    ArrayList<String> urls = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        String url = doc.getString("imageBase64");
                        if (url != null) urls.add(url);
                    }
                    
                    if (!urls.isEmpty()) {
                        ImageDataHolder.setImages(urls);
                        startActivity(new Intent(getActivity(), ImageViewerActivity.class));
                    } else {
                        Toast.makeText(getContext(), "No generated images found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSecurityPreference(boolean enabled) {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("app_security", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("security_enabled", enabled).apply();
    }

    private void checkBiometricSupport() {
        if (getContext() == null) return;
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            if (llBiometric != null) llBiometric.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (botsListener != null) botsListener.remove();
        if (chatsListener != null) chatsListener.remove();
        if (imagesListener != null) imagesListener.remove();
        if (billingClient != null) billingClient.endConnection();
    }
}
