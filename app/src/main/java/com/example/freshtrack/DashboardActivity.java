package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.models.User;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import android.animation.ValueAnimator;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

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
    private ValueEventListener statsListener;
    private FirebaseModel firebaseModel;
    private static final String TAG = "DashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            Intent intent = new Intent(DashboardActivity.this, MainActivityAuthentication.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Fresh Track");
        }

        // Initialize views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();

        // Load user data
        loadUserData();

        // Set up real-time listeners
        setupRealtimeListeners();

        // Set up bottom navigation
        setupBottomNavigation();
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
        MaterialCardView viewAllCard = findViewById(R.id.viewAllCard);
        MaterialCardView totalItemsCard = findViewById(R.id.totalItemsCard);
        MaterialCardView expiringSoonCard = findViewById(R.id.expiringSoonCard);
        MaterialCardView expiredCard = findViewById(R.id.expiredCard);

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
            firebaseModel = new FirebaseModel();
            firebaseModel.getUserById(userId)
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            userNameText.setText(user.getFirstName());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data: " + e.getMessage()));
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
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
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
            statsListener = firebaseModel.getFoodItemsByUser(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int fresh = 0;
                        int expiringSoon = 0;
                        int expired = 0;
                        long currentTime = System.currentTimeMillis();
                        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L;

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FoodItem item = snapshot.getValue(FoodItem.class);
                            if (item != null) {
                                long timeUntilExpiry = item.getExpiryDate() - currentTime;
                                
                                if (timeUntilExpiry < 0) {
                                    expired++;
                                } else if (timeUntilExpiry <= threeDaysInMillis) {
                                    expiringSoon++;
                                } else {
                                    fresh++;
                                }
                            }
                        }

                        // Update UI with animations
                        updateCountsWithAnimation(fresh, expiringSoon, expired);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error loading food stats: " + error.getMessage());
                    }
                });
        }
    }

    private void updateCountsWithAnimation(int fresh, int expiringSoon, int expired) {
        if (totalItemsCount != null) {
            animateTextView(0, fresh, totalItemsCount);
        } else {
            Log.e(TAG, "totalItemsCount TextView is null!");
        }
        
        if (expiringSoonCount != null) {
            animateTextView(0, expiringSoon, expiringSoonCount);
        }
        
        if (expiredCount != null) {
            animateTextView(0, expired, expiredCount);
        }
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

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            // Already on home, do nothing or refresh
            recreate();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recentActivityListener != null) {
            recentActivityListener.remove();
        }
        if (statsListener != null) {
            //statsListener.removeEventListener(statsListener);
        }
    }
} 