package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.freshtrack.models.User;
import com.example.freshtrack.models.UserSettings;

public class MainActivityAuthentication extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText usernameEditText;
    private Button signUpButton;
    private FirebaseAuth mAuth;
    private FirebaseModel firebaseModel;

    private static final String TAG = "Authentication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_authentication);

        // Initialize Firebase Auth and Model
        mAuth = FirebaseAuth.getInstance();
        firebaseModel = new FirebaseModel();

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        signUpButton = findViewById(R.id.signUpButton);

        // Initialize password toggle
        ImageButton togglePassword = findViewById(R.id.togglePasswordVisibility);
        togglePassword.setOnClickListener(v -> {
            if (passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                passwordEditText.setTransformationMethod(null);
                togglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                passwordEditText.setTransformationMethod(new PasswordTransformationMethod());
                togglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            passwordEditText.setSelection(passwordEditText.length());
        });

        // Set up click listener for signup
        signUpButton.setOnClickListener(v -> createAccount());

        // Add at the end of onCreate
        TextView loginPrompt = findViewById(R.id.loginPrompt);
        loginPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityAuthentication.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go to home activity
            startActivity(new Intent(this, MainActivityHome.class));
            finish();
        }
    }

    private void createAccount() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();

        // Add debug logging
        Log.d(TAG, "Attempting to create account with email: " + email);
        Log.d(TAG, "Username: " + username);

        if (!validateForm(email, password, firstName, lastName, username)) {
            return;
        }

        // Show progress to user
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Create user profile
                        createNewUser(firebaseUser.getUid(), email, firstName, lastName, username);
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    Toast.makeText(MainActivityAuthentication.this,
                        "Authentication failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void createNewUser(String userId, String email, String firstName, String lastName, String username) {
        User newUser = new User(userId, firstName, lastName, username, email);
        UserSettings newUserSettings = new UserSettings(userId);

        firebaseModel.createUser(newUser)
            .addOnSuccessListener(aVoid -> {
                // After creating user, create their settings
                firebaseModel.createUserSettings(newUserSettings)
                    .addOnSuccessListener(aVoid2 -> {
                        Toast.makeText(MainActivityAuthentication.this, 
                            "Account created successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivityAuthentication.this, DashboardActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating user settings: " + e.getMessage());
                        Toast.makeText(MainActivityAuthentication.this, 
                            "Error creating user settings", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user: " + e.getMessage());
                Toast.makeText(MainActivityAuthentication.this, 
                    "Error creating account", Toast.LENGTH_SHORT).show();
            });
    }

    private boolean validateForm(String email, String password, String firstName, 
                               String lastName, String username) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Required.");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            valid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters.");
            valid = false;
        }

        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("Required.");
            valid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Required.");
            valid = false;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Required.");
            valid = false;
        }

        return valid;
    }
}
