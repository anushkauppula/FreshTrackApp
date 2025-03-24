package com.example.freshtrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.freshtrack.models.UserSettings;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.Collections;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "LayoutPrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LAYOUT = "layout_type";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseModel firebaseModel;
    private Switch switchShowDeleteConfirmation;
    private Switch switchNotificationsEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firebaseModel = new FirebaseModel();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize UI components
        switchShowDeleteConfirmation = findViewById(R.id.switchShowDeleteConfirmation);
        switchNotificationsEnabled = findViewById(R.id.switchNotificationsEnabled);
        MaterialCardView signOutCard = findViewById(R.id.signOutCard);

        // Load current user settings
        loadUserSettings();

        // Setup click listeners
        findViewById(R.id.btnLayout).setOnClickListener(v -> showLayoutSelectionDialog());
        findViewById(R.id.btnTheme).setOnClickListener(v -> showThemeSelectionDialog());

        // Listener for Show Delete Confirmation toggle
        switchShowDeleteConfirmation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String userId = currentUser.getUid();
            firebaseModel.updateUserSettings(userId, Collections.singletonMap("showDeleteConfirmation", isChecked))
                .addOnSuccessListener(aVoid -> {
                    if (isChecked) {
                        // Show the delete confirmation dialog if the toggle is turned on
                        showDeleteConfirmationDialog();
                    }
                    Toast.makeText(SettingsActivity.this, "Show Delete Confirmation updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsActivity", "Error updating Show Delete Confirmation: " + e.getMessage());
                });
        });

        // Listener for Notifications Enabled toggle
        switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String userId = currentUser.getUid();
            firebaseModel.updateUserSettings(userId, Collections.singletonMap("notificationsEnabled", isChecked))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, "Notifications Enabled updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsActivity", "Error updating Notifications Enabled: " + e.getMessage());
                });
        });

        signOutCard.setOnClickListener(v -> signOut());

        setupBottomNavigation();
    }

    private void showLayoutSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layout_selection, null);
        RadioGroup layoutRadioGroup = dialogView.findViewById(R.id.layoutRadioGroup);

        // Set current selection
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentLayout = prefs.getString(KEY_LAYOUT, "list");
        if (currentLayout.equals("grid")) {
            layoutRadioGroup.check(R.id.radioGrid);
        } else {
            layoutRadioGroup.check(R.id.radioList);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Layout")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedId = layoutRadioGroup.getCheckedRadioButtonId();
                    String layout = selectedId == R.id.radioGrid ? "grid" : "list";
                    prefs.edit().putString(KEY_LAYOUT, layout).apply();
                    Toast.makeText(this, "Layout preference saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_theme_selection, null);
        RadioGroup themeRadioGroup = dialogView.findViewById(R.id.themeRadioGroup);

        // Set current selection
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(KEY_THEME, "system");
        switch (currentTheme) {
            case "light":
                themeRadioGroup.check(R.id.radioLight);
                break;
            case "dark":
                themeRadioGroup.check(R.id.radioDark);
                break;
            default:
                themeRadioGroup.check(R.id.radioSystemDefault);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedId = themeRadioGroup.getCheckedRadioButtonId();
                    String theme;
                    if (selectedId == R.id.radioLight) {
                        theme = "light";
                    } else if (selectedId == R.id.radioDark) {
                        theme = "dark";
                    } else {
                        theme = "system";
                    }
                    prefs.edit().putString(KEY_THEME, theme).apply();
                    Toast.makeText(this, "Theme preference saved", Toast.LENGTH_SHORT).show();
                    recreate(); // Recreate activity to apply theme
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadUserSettings() {
        String userId = currentUser.getUid();
        firebaseModel.getUserSettings(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        UserSettings settings = dataSnapshot.getValue(UserSettings.class);
                        if (settings != null) {
                            switchShowDeleteConfirmation.setChecked(settings.isShowDeleteConfirmation());
                            switchNotificationsEnabled.setChecked(settings.isNotificationsEnabled());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("SettingsActivity", "Error loading user settings: " + error.getMessage());
                }
            });
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnDashboard = bottomNav.findViewById(R.id.btnDashboard);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnNotifications = bottomNav.findViewById(R.id.btnNotifications);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivityHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AddListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, NotificationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnSettings.setOnClickListener(v -> {
            // Already on Settings, do nothing
        });
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Logic to show the delete confirmation dialog
        // This can be similar to the one in FoodItemAdapter
    }
}