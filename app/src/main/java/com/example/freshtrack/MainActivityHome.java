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

public class MainActivityHome extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
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

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup Toolbar and Navigation Drawer Toggle
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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

    private void loadFoodItems() {
        /* Comment out authentication check for now
        if (mAuth.getCurrentUser() == null) {
            // Handle not logged in case
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        */

        // Temporarily use a fixed userId
        String userId = "testUser123";
        
        firebaseModel.getFoodItemsByUser(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    foodItems.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        FoodItem item = snapshot.getValue(FoodItem.class);
                        if (item != null) {
                            item.calculateStatus();
                            foodItems.add(item);
                        }
                    }
                    adapter.updateItems(foodItems);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(MainActivityHome.this, 
                        "Error loading items: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            // Handle notification icon click
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_settings) {
            Toast.makeText(this, "Settings Selected", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}