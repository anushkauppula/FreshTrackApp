package com.example.freshtrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSettingsActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private TextView tvEmail, tvMobile;
    private MaterialButton btnChangeEmail, btnChangeMobile, btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserData();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initializeViews() {
        tvEmail = findViewById(R.id.tvEmail);
        tvMobile = findViewById(R.id.tvMobile);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangeMobile = findViewById(R.id.btnChangeMobile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void loadUserData() {
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            
            // Load mobile number from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String mobile = documentSnapshot.getString("mobile");
                            tvMobile.setText(mobile != null ? mobile : "Not set");
                        }
                    });
        }
    }

    private void setupClickListeners() {
        btnChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        btnChangeMobile.setOnClickListener(v -> showChangeMobileDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void showChangeEmailDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        TextView tvCurrentEmail = dialogView.findViewById(R.id.tvCurrentEmail);
        TextView tvNewEmail = dialogView.findViewById(R.id.tvNewEmail);

        tvCurrentEmail.setText(currentUser.getEmail());

        new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Send Verification", (dialog, which) -> {
                    String newEmail = tvNewEmail.getText().toString().trim();
                    if (!newEmail.isEmpty()) {
                        currentUser.updateEmail(newEmail)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        currentUser.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(this, 
                                                            "Verification email sent", 
                                                            Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(this, 
                                                            "Failed to send verification email", 
                                                            Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(this, 
                                            "Failed to update email", 
                                            Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeMobileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_mobile, null);
        TextView tvCurrentMobile = dialogView.findViewById(R.id.tvCurrentMobile);
        TextView tvNewMobile = dialogView.findViewById(R.id.tvNewMobile);

        tvCurrentMobile.setText(tvMobile.getText());

        new AlertDialog.Builder(this)
                .setTitle("Change Mobile Number")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newMobile = tvNewMobile.getText().toString().trim();
                    if (!newMobile.isEmpty()) {
                        db.collection("users").document(currentUser.getUid())
                                .update("mobile", newMobile)
                                .addOnSuccessListener(aVoid -> {
                                    tvMobile.setText(newMobile);
                                    Toast.makeText(this, 
                                        "Mobile number updated", 
                                        Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, 
                                        "Failed to update mobile number", 
                                        Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete user data from Firestore
                    db.collection("users").document(currentUser.getUid())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Delete Firebase Auth account
                                currentUser.delete()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(this, 
                                                    "Account deleted successfully", 
                                                    Toast.LENGTH_SHORT).show();
                                                // Navigate to login screen
                                                Intent intent = new Intent(this, MainActivityAuthentication.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(this, 
                                                    "Failed to delete account", 
                                                    Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, 
                                    "Failed to delete account data", 
                                    Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(AccountSettingsActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AccountSettingsActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AccountSettingsActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
}