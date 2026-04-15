package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String WEB_CLIENT_ID = "618801578812-pb9ok8edttc4r5ptjqq659lkm1vm93ht.apps.googleusercontent.com";

    EditText etUsername, etEmail, etPassword, etConfirmPassword;
    Button btnRegister;
    TextView tvLogin;
    LinearLayout btnGoogle;

    FirebaseAuth auth;
    FirebaseFirestore db;
    GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupGoogleSignIn();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        checkUserInFirestore(user);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser user) {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        setLoading(false);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        createNewUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
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

        newUser.plan = "free";
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

        db.collection("users").document(user.getUid()).set(newUser)
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
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Creating..." : "Sign Up");
        btnGoogle.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }

    private boolean isTempEmail(String email) {
        String[] blocked = {"tempmail.com", "10minutemail.com", "mailinator.com", "guerrillamail.com", "yopmail.com"};
        for (String domain : blocked) {
            if (email.toLowerCase().endsWith(domain)) return true;
        }
        return false;
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();

        if (username.isEmpty()) {
            etUsername.setError("Enter username");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            return;
        }
        if (isTempEmail(email)) {
            etEmail.setError("Temporary emails are not allowed");
            return;
        }
        if (pass.length() < 6) {
            etPassword.setError("Password must be 6+ characters");
            return;
        }
        if (!pass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        setLoading(false);
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    result.getUser().sendEmailVerification();
                    String uid = result.getUser().getUid();

                    User user = new User();
                    user.uid = uid;
                    user.name = username;
                    user.username = username;
                    user.email = email;
                    user.plan = "free";
                    user.credits = 50;
                    user.dailyMessageCount = 0;
                    user.dailyImageCount = 0;
                    user.dailyVoiceCount = 0;
                    user.maxBots = 1;
                    user.maxMessagesPerDay = 15;
                    user.maxImagesPerDay = 1;
                    user.maxVoicePerDay = 0;
                    user.lastResetDate = System.currentTimeMillis();
                    user.createdAt = System.currentTimeMillis();
                    user.active = true;

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(unused -> {
                                setLoading(false);
                                Toast.makeText(this, "Account created! Please verify your email, then login.", Toast.LENGTH_LONG).show();
                                auth.signOut();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (auth.getCurrentUser() != null) auth.getCurrentUser().delete();
                                setLoading(false);
                                Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
