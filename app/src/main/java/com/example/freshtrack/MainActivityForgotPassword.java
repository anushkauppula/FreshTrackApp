package com.example.freshtrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivityForgotPassword extends AppCompatActivity {

    private EditText etEmail;
    private Button btnRecover;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        etEmail = findViewById(R.id.forgotpassword);
        btnRecover = findViewById(R.id.passwordrecoverbutton);

        // Recover button click listener
        btnRecover.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required!");
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivityForgotPassword.this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivityForgotPassword.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
