package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ranit.botscraft.ForgotPasswordActivity;
import com.ranit.botscraft.R;
import com.ranit.botscraft.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String WEB_CLIENT_ID = "618801578812-pb9ok8edttc4r5ptjqq659lkm1vm93ht.apps.googleusercontent.com";

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister, forget, tvVerificationNotice;
    View btnGoogle;
    ImageView ivPasswordToggle;

    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;
    
    private boolean isPasswordVisible = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Launcher result code: " + result.getResultCode());
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            Log.d(TAG, "Google Sign-In success, getting ID token...");
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Log.e(TAG, "Google Sign-In failed: No ID Token found");
                            Toast.makeText(this, "Google Sign-In failed: No ID Token", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed. Code: " + e.getStatusCode() + " Msg: " + e.getMessage());
                        // Common Codes: 10 = Developer Error (SHA-1/Package Mismatch), 7 = Network, 12500 = Sign-in failed
                        String friendlyError = "Google sign in failed: " + e.getStatusCode();
                        if (e.getStatusCode() == 10) friendlyError = "Developer Error (10): Check SHA-1 in Firebase Console";
                        Toast.makeText(this, friendlyError, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Google Launcher data is null. Result Code: " + result.getResultCode());
                }
            }
    );

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        initViews();
        setupGoogleSignIn();
        setupListeners();

        if (getIntent().getBooleanExtra("showVerificationNotice", false)) {
            tvVerificationNotice.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        forget = findViewById(R.id.tvForgotPassword);
        btnGoogle = findViewById(R.id.btnGoogle);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        tvVerificationNotice = findViewById(R.id.tvVerificationNotice);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forget.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        
        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_hidden);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_visible);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: starting Firebase auth...");
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth with Google SUCCESS");
                        FirebaseUser user = auth.getCurrentUser();
                        checkUserInFirestore(user);
                    } else {
                        Log.e(TAG, "Firebase Auth with Google FAILED", task.getException());
                        setLoading(false);
                        Toast.makeText(this, "Authentication Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "checkUserInFirestore: User is null");
            setLoading(false);
            return;
        }
        Log.d(TAG, "Checking user in Firestore: " + user.getUid());
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User exists in Firestore, redirecting to Main");
                        setLoading(false);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Log.d(TAG, "User NOT in Firestore, creating new profile...");
                        createNewUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore check FAILED", e);
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewUserProfile(FirebaseUser user) {
        String username = user.getDisplayName();
        if (username == null || username.isEmpty()) {
            username = user.getEmail() != null ? user.getEmail().split("@")[0] : "User" + user.getUid().substring(0, 5);
        }

        User newUser = new User();
        newUser.uid = user.getUid();
        newUser.name = username;
        newUser.username = username;
        newUser.email = user.getEmail();
        newUser.imageUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        newUser.plan = "free";
        newUser.subscriptionStart = System.currentTimeMillis();
        newUser.subscriptionEnd = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000); 
        newUser.credits = 50;
        newUser.dailyMessageCount = 0;
        newUser.dailyImageCount = 0;
        newUser.dailyVoiceCount = 0;
        newUser.maxBots = 1;
        newUser.maxMessagesPerDay = 15;
        newUser.maxImagesPerDay = 1;
        newUser.maxVoicePerDay = 0;
        newUser.lastResetDate = System.currentTimeMillis();
        newUser.createdAt = System.currentTimeMillis();
        newUser.active = true;
        newUser.blockedBots = new ArrayList<>();
        newUser.notificationsEnabled = true;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Profile creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Processing..." : "Login");
        btnGoogle.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            etEmail.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    if (auth.getCurrentUser() == null) {
                        setLoading(false);
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!auth.getCurrentUser().isEmailVerified()) {
                        auth.signOut();
                        setLoading(false);
                        tvVerificationNotice.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Please verify your email before login", Toast.LENGTH_LONG).show();
                        return;
                    }

                    tvVerificationNotice.setVisibility(View.GONE);
                    setLoading(false);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
