package com.example.freshtrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;

public class AddListActivity extends AppCompatActivity {

    private EditText etFoodName;
    private EditText etExpiryDate;
    private Button btnSave;
    private FirebaseModel firebaseModel;
    // private FirebaseAuth mAuth;  // Comment out but keep for later

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);

        // Initialize Firebase
        firebaseModel = new FirebaseModel();
        // mAuth = FirebaseAuth.getInstance();  // Comment out but keep for later

        // Initialize views
        etFoodName = findViewById(R.id.etFoodName);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        btnSave = findViewById(R.id.btnSave);

        // Setup date picker
        etExpiryDate.setOnClickListener(v -> showDatePicker());

        // Setup save button
        btnSave.setOnClickListener(v -> saveItem());
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
        FoodItem foodItem = new FoodItem(foodName, expiryDate, userId);
        
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
}