package com.example.freshtrack;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.freshtrack.adapters.FoodItemAdapter;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.util.Log;
import android.widget.TextView;

public class MainActivityHome extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private FirebaseModel firebaseModel;
    // private FirebaseAuth mAuth;  // Comment out but keep for later
    private RecyclerView recyclerView;
    private FoodItemAdapter adapter;
    private List<FoodItem> foodItems;
    private FloatingActionButton fabAddFood;

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

        // Set up navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        
        drawerToggle = new ActionBarDrawerToggle(
            this, 
            drawerLayout, 
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        
        // Set the custom hamburger icon
        toolbar.setNavigationIcon(R.drawable.menu);
        
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();

        setupNavigationView();

        // Initialize FAB and set click listener
        fabAddFood = findViewById(R.id.fabAddFood);
        fabAddFood.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivityHome.this, AddListActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase
        firebaseModel = new FirebaseModel();
        // mAuth = FirebaseAuth.getInstance();  // Comment out but keep for later

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);
        layoutManager.setStackFromEnd(false);
        recyclerView.setLayoutManager(layoutManager);
        foodItems = new ArrayList<>();
        adapter = new FoodItemAdapter(foodItems);
        recyclerView.setAdapter(adapter);

        // Load food items
        loadFoodItems();

        // Add swipe to delete functionality
        ItemTouchHelper.SimpleCallback swipeToDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable background = new ColorDrawable(getResources().getColor(R.color.crimson, null));
            private final Paint textPaint = new Paint();

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, 
                                  @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, 
                                  int actionState, boolean isCurrentlyActive) {
                
                View itemView = viewHolder.itemView;
                if (dX == 0 && !isCurrentlyActive) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }

                // Set background
                background.setBounds(itemView.getLeft(), itemView.getTop(), 
                                  itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // Setup text paint
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(36);
                
                // Draw "Delete" text based on swipe direction
                String deleteText = "Delete";
                float textY = itemView.getTop() + (itemView.getHeight() / 2f) + 24;
                
                if (dX > 0) { // Swiping right
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    float textX = itemView.getLeft() + 50;
                    c.drawText(deleteText, textX, textY, textPaint);
                } else if (dX < 0) { // Swiping left
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    float textX = itemView.getRight() - 50;
                    c.drawText(deleteText, textX, textY, textPaint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FoodItem foodItem = foodItems.get(position);
                
                // Delete from Firebase
                firebaseModel.deleteFoodItem(foodItem.getId())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivityHome.this, "Item deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivityHome.this, "Error deleting item: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    });
            }
        };

        new ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView);
    }

    private void setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                // Navigate to Dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.nav_settings) {
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set up header click listener
        View headerView = navigationView.getHeaderView(0);
        TextView titleView = headerView.findViewById(R.id.nav_header_title);
        titleView.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Navigate to Dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish(); // Close current activity
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}