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

import com.example.freshtrack.adapters.FoodItemAdapter;
import com.example.freshtrack.models.FoodItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MainActivityHome extends AppCompatActivity {
    private FirebaseModel firebaseModel;
    private RecyclerView recyclerView;
    private FoodItemAdapter adapter;
    private List<FoodItem> foodItems;
    private FloatingActionButton fabAddFood;
    private EditText searchBox;

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

        // Initialize FAB and set click listener
        fabAddFood = findViewById(R.id.fabAddFood);
        fabAddFood.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivityHome.this, AddListActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase
        firebaseModel = new FirebaseModel();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);
        layoutManager.setStackFromEnd(false);
        recyclerView.setLayoutManager(layoutManager);
        foodItems = new ArrayList<>();
        adapter = new FoodItemAdapter(foodItems);
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

        // Load food items
        loadFoodItems();
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
}