package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.freshtrack.adapters.FoodItemAdapter;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MainActivityHome extends AppCompatActivity {
    private FirebaseModel firebaseModel;
    private RecyclerView recyclerView;
    private FoodItemAdapter adapter;
    private List<FoodItem> foodItems;
    private EditText searchBox;
    private static final String PREFS_NAME = "LayoutPrefs";
    private static final String KEY_LAYOUT = "layout_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Fresh Track");
        }

        // Initialize Firebase
        firebaseModel = new FirebaseModel();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        foodItems = new ArrayList<>();
        adapter = new FoodItemAdapter(foodItems);
        
        // Set layout based on user preference
        setLayoutManager();
        recyclerView.setAdapter(adapter);

        // Initialize search functionality
        searchBox = findViewById(R.id.searchBox);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up bottom navigation
        setupBottomNavigation();

        // Load food items
        loadFoodItems();
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityHome.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityHome.this, AddListActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadFoodItems() {
        if (firebaseModel == null) {
            firebaseModel = new FirebaseModel();
        }
        
        if (foodItems == null) {
            foodItems = new ArrayList<>();
        }

        // Temporarily use a fixed userId
        String userId = "testUser123";
        
        firebaseModel.getFoodItemsByUser(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        foodItems.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                FoodItem item = snapshot.getValue(FoodItem.class);
                                if (item != null) {
                                    // Ensure the item has an ID
                                    if (item.getId() == null) {
                                        item.setId(snapshot.getKey());
                                    }
                                    foodItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e("MainActivityHome", "Error parsing food item: " + e.getMessage());
                                // Continue to next item if one fails
                                continue;
                            }
                        }
                        
                        // Update adapter on UI thread
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.updateItems(foodItems);
                            }
                        });
                    } catch (Exception e) {
                        Log.e("MainActivityHome", "Error in onDataChange: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivityHome.this, 
                                "Error loading items: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MainActivityHome", "Database error: " + databaseError.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivityHome.this, 
                            "Error loading items: " + databaseError.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void setLayoutManager() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String layoutType = prefs.getString(KEY_LAYOUT, "list");
        
        if (layoutType.equals("grid")) {
            // Use GridLayoutManager with 2 columns
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            recyclerView.setLayoutManager(layoutManager);
        } else {
            // Use LinearLayoutManager for list view
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setReverseLayout(false);
            layoutManager.setStackFromEnd(false);
            recyclerView.setLayoutManager(layoutManager);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if layout preference has changed
        setLayoutManager();
    }
}