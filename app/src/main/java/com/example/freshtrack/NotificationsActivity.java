package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.adapters.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.freshtrack.models.UserNotification;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView notificationsRecyclerView;
    private TextView noNotificationsText;
    private FirebaseModel firebaseModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notifications");
        }

        mAuth = FirebaseAuth.getInstance();
        firebaseModel = new FirebaseModel();
        initializeViews();
        loadNotifications();
        setupBottomNavigation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notifications, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            clearAllNotifications();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllNotifications() {
        String userId = mAuth.getCurrentUser().getUid();
        firebaseModel.clearAllNotifications(userId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                // The UI will be automatically updated through the ValueEventListener
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationsActivity", "Error clearing notifications: " + e.getMessage());
                Toast.makeText(this, "Failed to clear notifications", Toast.LENGTH_SHORT).show();
            });
    }

    private void initializeViews() {
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        noNotificationsText = findViewById(R.id.noNotificationsText);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadNotifications() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Load all notifications for this user
        firebaseModel.getNotificationsByUser(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserNotification> notifications = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserNotification notification = snapshot.getValue(UserNotification.class);
                    if (notification != null) {
                        notifications.add(notification);
                    }
                }

                // Sort notifications by timestamp (most recent first)
                notifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                updateNotificationsUI(notifications);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("NotificationsActivity", "Error loading notifications: " + databaseError.getMessage());
                noNotificationsText.setVisibility(View.VISIBLE);
                noNotificationsText.setText("Error loading notifications");
                notificationsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void updateNotificationsUI(List<UserNotification> notifications) {
        if (notifications.isEmpty()) {
            noNotificationsText.setVisibility(View.VISIBLE);
            noNotificationsText.setText("No notifications");
            notificationsRecyclerView.setVisibility(View.GONE);
        } else {
            noNotificationsText.setVisibility(View.GONE);
            notificationsRecyclerView.setVisibility(View.VISIBLE);
            notificationsRecyclerView.setAdapter(new NotificationAdapter(notifications));
        }
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        View btnHome = bottomNav.findViewById(R.id.btnHome);
        View btnDashboard = bottomNav.findViewById(R.id.btnDashboard);
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnNotifications = bottomNav.findViewById(R.id.btnNotifications);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, MainActivityHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, AddListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnNotifications.setOnClickListener(v -> {
            // Already on Notifications, do nothing
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
} 