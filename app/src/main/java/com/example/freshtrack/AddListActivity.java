package com.example.freshtrack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;

public class AddListActivity extends AppCompatActivity {

    private EditText etFoodName;
    private EditText etExpiryDate;
    private Spinner spinnerCategory;
    private EditText etWeight;
    private EditText etCount;
    private Button btnSave;
    private FirebaseModel firebaseModel;
    // private FirebaseAuth mAuth;  // Comment out but keep for later

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Item");
        }

        // Initialize Firebase
        firebaseModel = new FirebaseModel();
        // mAuth = FirebaseAuth.getInstance();  // Comment out but keep for later

        // Initialize views
        etFoodName = findViewById(R.id.etFoodName);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etWeight = findViewById(R.id.etWeight);
        etCount = findViewById(R.id.etCount);
        btnSave = findViewById(R.id.btnSave);

        // Setup category spinner
        setupCategorySpinner();

        // Setup date picker
        etExpiryDate.setOnClickListener(v -> showDatePicker());

        // Setup save button
        btnSave.setOnClickListener(v -> saveItem());

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void setupCategorySpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.food_categories, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerCategory.setAdapter(adapter);
    }

    private void showDatePicker() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    // Create Calendar instance with selected date
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Format date as MM/dd/yyyy
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    String formattedDate = dateFormat.format(selectedCalendar.getTime());
                    
                    // Set the formatted date
                    etExpiryDate.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void saveItem() {
        String foodName = etFoodName.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String weight = etWeight.getText().toString().trim();
        String count = etCount.getText().toString().trim();

        if (foodName.isEmpty()) {
            etFoodName.setError("Please enter food name");
            return;
        }

        if (expiryDate.isEmpty()) {
            etExpiryDate.setError("Please select expiry date");
            return;
        }

        /* Comment out authentication check for now
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
        */
        
        // Temporarily use a fixed userId
        String userId = "testUser123";
        
        // Parse the expiry date string to timestamp
        long dateAdded = System.currentTimeMillis();
        long expiryTimestamp;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            expiryTimestamp = sdf.parse(expiryDate).getTime();
        } catch (Exception e) {
            Log.e("AddListActivity", "Error parsing date", e);
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FoodItem foodItem = new FoodItem(
            foodName,              // name
            dateAdded,            // dateAdded (current timestamp)
            expiryTimestamp,      // expiryDate
            userId,               // userId
            category,             // category (from spinner)
            1,                    // quantity (default)
            "piece",              // unit (default)
            "",                   // notes (empty by default)
            weight,               // weight (from input)
            count                 // count (from input)
        );
        
        // Add log before saving
        Log.d("AddListActivity", "Attempting to save food item: " + foodName);
        
        firebaseModel.addFoodItem(foodItem)
            .addOnSuccessListener(aVoid -> {
                Log.d("AddListActivity", "Food item saved successfully");
                Toast.makeText(this, "Food item saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e("AddListActivity", "Error saving food item", e);
                Toast.makeText(this, "Error saving food item: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
        /* Comment out auth else block
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
        }
        */
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, MainActivityHome.class);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            // Already in Add page, do nothing or refresh
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AddListActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
}