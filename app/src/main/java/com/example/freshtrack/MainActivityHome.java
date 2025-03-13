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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            firebaseModel.getFoodItemsByUser(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        foodItems.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FoodItem item = snapshot.getValue(FoodItem.class);
                            if (item != null) {
                                foodItems.add(item);
                            }
                        }
                        adapter.updateItems(foodItems);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MainActivityHome", "Error loading items: " + error.getMessage());
                    }
                });
        }
    }
}