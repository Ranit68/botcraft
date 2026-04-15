package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;
import com.ranit.botscraft.R;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.User;
import com.ranit.botscraft.network.PurchaseVerificationRequest;
import com.ranit.botscraft.network.PurchaseVerificationResponse;
import com.ranit.botscraft.network.RetrofitClient;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyCreditsActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private static final String TAG = "BuyCreditsActivity";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    public static final String SKU_100 = "credits_100";
    public static final String SKU_360 = "credits_360";
    public static final String SKU_700 = "credits_700";
    public static final String SKU_1500 = "credits_1500";

    private ImageView btnBack;
    private TextView tvCurrentBalance;
    private View btnBuy49, btnBuy129, btnBuy249, btnBuy499, btnWatchAd;
    private ListenerRegistration userListener;
    private RewardedAd rewardedAd;
    private boolean isLoadingAd = false;
    private BillingClient billingClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_credits);

        initViews();
        setupBillingClient();
        setupListeners();
        observeCredits();
        loadRewardedAd();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        btnBuy49 = findViewById(R.id.btnBuy49);
        btnBuy129 = findViewById(R.id.btnBuy129);
        btnBuy249 = findViewById(R.id.btnBuy249);
        btnBuy499 = findViewById(R.id.btnBuy499);
        btnWatchAd = findViewById(R.id.btnWatchAd);
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client ready");
                }
            }
            @Override
            public void onBillingServiceDisconnected() {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBuy49.setOnClickListener(v -> launchBillingFlow(SKU_100));
        btnBuy129.setOnClickListener(v -> launchBillingFlow(SKU_360));
        btnBuy249.setOnClickListener(v -> launchBillingFlow(SKU_700));
        btnBuy499.setOnClickListener(v -> launchBillingFlow(SKU_1500));
        btnWatchAd.setOnClickListener(v -> showRewardedAd());
    }

    private void launchBillingFlow(String productId) {
        if (!billingClient.isReady()) return;

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetailsList.get(0))
                                .build()))
                        .build();
                billingClient.launchBillingFlow(this, flowParams);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    verifyOnBackend(purchase);
                }
            }
        }
    }

    private void verifyOnBackend(Purchase purchase) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            PurchaseVerificationRequest req = new PurchaseVerificationRequest(purchase.getPurchaseToken(), purchase.getProducts().get(0));
            RetrofitClient.getService().verifyPurchase(token, req).enqueue(new Callback<PurchaseVerificationResponse>() {
                @Override
                public void onResponse(Call<PurchaseVerificationResponse> call, Response<PurchaseVerificationResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        Toast.makeText(BuyCreditsActivity.this, "Purchased Successfully!", Toast.LENGTH_SHORT).show();
                        consume(purchase);
                    }
                }
                @Override public void onFailure(Call<PurchaseVerificationResponse> call, Throwable t) {}
            });
        });
    }

    private void consume(Purchase purchase) {
        billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(), (res, token) -> {});
    }

    private void loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) return;
        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoadingAd = false;
                rewardedAd = null;
                Log.e(TAG, "Ad failed to load: " + loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                isLoadingAd = false;
                rewardedAd = ad;
                Log.d(TAG, "Ad loaded successfully");
            }
        });
    }

    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> grantAdReward());
            rewardedAd = null;
            loadRewardedAd();
        } else {
            Toast.makeText(this, "Ad is still loading, please try again in a moment", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    private void grantAdReward() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        user.getIdToken(true).addOnSuccessListener(result -> {
            RetrofitClient.getService().rewardCredit("Bearer " + result.getToken()).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) Toast.makeText(BuyCreditsActivity.this, "Earned +20 Credits!", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        });
    }

    private void observeCredits() {
        String uid = FirebaseManager.getUserId();
        if (uid != null) {
            userListener = FirebaseManager.getFirestore().collection("users").document(uid).addSnapshotListener((snap, e) -> {
                if (snap != null && snap.exists()) {
                    User user = snap.toObject(User.class);
                    if (user != null) tvCurrentBalance.setText("Balance: " + user.credits);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (billingClient != null) billingClient.endConnection();
    }
}
