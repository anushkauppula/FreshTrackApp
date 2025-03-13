package com.example.freshtrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "LayoutPrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LAYOUT = "layout_type";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        //MaterialCardView accountSettingsCard = findViewById(R.id.accountSettingsCard);
        MaterialCardView signOutCard = findViewById(R.id.signOutCard);

//        accountSettingsCard.setOnClickListener(v -> {
//            startActivity(new Intent(SettingsActivity.this, AccountSettingsActivity.class));
//        });

        signOutCard.setOnClickListener(v -> signOut());

        // Setup click listeners
        findViewById(R.id.btnLayout).setOnClickListener(v -> showLayoutSelectionDialog());
        findViewById(R.id.btnTheme).setOnClickListener(v -> showThemeSelectionDialog());

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

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            // Already in settings, do nothing
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
}