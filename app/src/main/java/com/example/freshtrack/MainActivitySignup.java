package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivitySignup extends AppCompatActivity {

    private EditText signupemail, signuppassword;
    private Button btnSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_signup);
        mAuth = FirebaseAuth.getInstance();
        signupemail = findViewById(R.id.signupemail);
        signuppassword = findViewById(R.id.signuppassword);
        btnSignUp = findViewById(R.id.signup);
        btnSignUp.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = signupemail.getText().toString().trim();
        String password = signuppassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            signupemail.setError("Email is required!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            signuppassword.setError("Password is required!");
            return;
        }

        if (password.length() < 6) {
            signuppassword.setError("Password must be at least 6 characters!");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivitySignup.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivitySignup.this, MainActivityAuthentication.class));
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Toast.makeText(MainActivitySignup.this, "Registration Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
