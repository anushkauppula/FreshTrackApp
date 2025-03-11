package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.models.FoodItem;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.navigation.NavigationView;
import android.animation.ValueAnimator;

public class DashboardActivity extends AppCompatActivity {
    private TextView userNameText;
    private TextView totalItemsCount;
    private TextView expiringSoonCount;
    private TextView expiredCount;
    private RecyclerView recentActivityRecyclerView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private RecentActivityAdapter recentActivityAdapter;
    private ListenerRegistration recentActivityListener;
    private ListenerRegistration statsListener;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
        ) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };
        
        // Set the custom hamburger icon
        toolbar.setNavigationIcon(R.drawable.menu);
        
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();

        setupNavigationView();

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Initialize views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();

        // Load user data
        loadUserData();

        // Set up real-time listeners
        setupRealtimeListeners();
    }

    private void initializeViews() {
        userNameText = findViewById(R.id.userNameText);
        totalItemsCount = findViewById(R.id.totalItemsCount);
        expiringSoonCount = findViewById(R.id.expiringSoonCount);
        expiredCount = findViewById(R.id.expiredCount);
        recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView);

        // Set up RecyclerView
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(new ArrayList<>());
        recentActivityRecyclerView.setAdapter(recentActivityAdapter);
    }

    private void setupClickListeners() {
        FloatingActionButton addItemFab = findViewById(R.id.addItemFab);
        MaterialCardView viewAllCard = findViewById(R.id.viewAllCard);
        MaterialCardView totalItemsCard = findViewById(R.id.totalItemsCard);
        MaterialCardView expiringSoonCard = findViewById(R.id.expiringSoonCard);
        MaterialCardView expiredCard = findViewById(R.id.expiredCard);

        addItemFab.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        viewAllCard.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivityHome.class);
            startActivity(intent);
        });

        View.OnClickListener statisticsClickListener = v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivityHome.class);
            String filter = "";
            if (v.getId() == R.id.expiringSoonCard) {
                filter = "expiring_soon";
            } else if (v.getId() == R.id.expiredCard) {
                filter = "expired";
            }
            intent.putExtra("FILTER", filter);
            startActivity(intent);
        };

        expiringSoonCard.setOnClickListener(statisticsClickListener);
        expiredCard.setOnClickListener(statisticsClickListener);
        totalItemsCard.setOnClickListener(statisticsClickListener);
    }

    private void loadUserData() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String displayName = firstName != null ? firstName : 
                            (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
                        userNameText.setText(displayName);
                    }
                })
                .addOnFailureListener(e -> userNameText.setText("User"));
        }
    }

    private void setupRealtimeListeners() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            // Setup real-time listener for recent activities
            Query recentItemsQuery = firestore.collection("food_items")
                .whereEqualTo("userId", userId)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .limit(5);

            recentActivityListener = recentItemsQuery.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Toast.makeText(this, "Error loading recent items: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots != null && !snapshots.isEmpty()) {
                    List<FoodItem> recentItems = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        FoodItem item = doc.toObject(FoodItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            recentItems.add(item);
                        }
                    }
                    recentActivityAdapter.updateItems(recentItems);
                }
            });

            // Setup real-time listener for statistics
            statsListener = firestore.collection("food_items")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading statistics: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        updateStatistics(snapshots.getDocuments());
                    }
                });
        }
    }

    private void updateStatistics(List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        int total = documents.size();
        int expiringSoon = 0;
        int expired = 0;
        long currentTime = System.currentTimeMillis();
        long sevenDaysFromNow = currentTime + (7 * 24 * 60 * 60 * 1000); // 7 days in milliseconds

        for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
            FoodItem item = doc.toObject(FoodItem.class);
            if (item != null) {
                long expiryDate = item.getExpiryDate();
                if (expiryDate < currentTime) {
                    expired++;
                } else if (expiryDate <= sevenDaysFromNow) {
                    expiringSoon++;
                }
            }
        }

        // Update the TextViews with animations
        animateTextView(0, total, totalItemsCount);
        animateTextView(0, expiringSoon, expiringSoonCount);
        animateTextView(0, expired, expiredCount);
    }

    private void animateTextView(int start, int end, TextView textView) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(1000); // Animation duration in milliseconds
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });
        animator.start();
    }

    private void setupNavigationView() {
        // Set up header click listener
        View headerView = navigationView.getHeaderView(0);
        TextView titleView = headerView.findViewById(R.id.nav_header_title);
        titleView.setOnClickListener(v -> {
            // Refresh current activity
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                // Refresh dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
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
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recentActivityListener != null) {
            recentActivityListener.remove();
        }
        if (statsListener != null) {
            statsListener.remove();
        }
    }
} 