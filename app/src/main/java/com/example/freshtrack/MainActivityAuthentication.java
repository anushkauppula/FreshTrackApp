package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivityAuthentication extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignUp;
    private TextView tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_authentication);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
        }

        // Login button click listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Navigate to SignUp Activity
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityAuthentication.this, MainActivitySignup.class);
            startActivity(intent);
        });

        // Navigate to ForgotPassword Activity
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityAuthentication.this, MainActivityForgotPassword.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required!");
            return;
        }

        // Authenticate user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivityAuthentication.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Toast.makeText(MainActivityAuthentication.this, "Authentication Failed: "
                                + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivityAuthentication.this, DashboardActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }
}
