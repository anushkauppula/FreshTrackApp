package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.freshtrack.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText loginIdentifier;
    private EditText loginPassword;
    private Button loginButton;
    private TextView signupPrompt;
    private FirebaseAuth mAuth;
    private FirebaseModel firebaseModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firebaseModel = new FirebaseModel();

        loginIdentifier = findViewById(R.id.loginIdentifier);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        signupPrompt = findViewById(R.id.signupPrompt);

        // Initialize password toggle
        ImageButton togglePassword = findViewById(R.id.toggleLoginPasswordVisibility);
        togglePassword.setOnClickListener(v -> {
            if (loginPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                loginPassword.setTransformationMethod(null);
                togglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                loginPassword.setTransformationMethod(new PasswordTransformationMethod());
                togglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            loginPassword.setSelection(loginPassword.length());
        });

        loginButton.setOnClickListener(v -> attemptLogin());
        signupPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivityAuthentication.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String identifier = loginIdentifier.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (!validateForm(identifier, password)) {
            return;
        }

        // Show loading state
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        // First try to find user by username
        firebaseModel.getUserByUsername(identifier)
            .addOnSuccessListener(dataSnapshot -> {
                boolean userFound = false;
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null && user.getUsername() != null && 
                            user.getUsername().equals(identifier)) {
                            Log.d(TAG, "Found user by username: " + user.getUsername());
                            userFound = true;
                            loginWithEmail(user.getEmail(), password);
                            break;
                        }
                    }
                }
                
                if (!userFound) {
                    Log.d(TAG, "No user found with username: " + identifier + ", trying as email");
                    loginWithEmail(identifier, password);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error searching for user: " + e.getMessage());
                loginWithEmail(identifier, password);
            });
    }

    private void loginWithEmail(String email, String password) {
        Log.d(TAG, "Attempting login with email: " + email);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, 
                        "Authentication failed. Please check your credentials and try again.",
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private boolean validateForm(String identifier, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(identifier)) {
            loginIdentifier.setError("Required.");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Required.");
            valid = false;
        }

        return valid;
    }
} 