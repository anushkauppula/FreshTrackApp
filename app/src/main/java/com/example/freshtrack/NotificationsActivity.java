package com.example.freshtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.adapters.NotificationAdapter;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DataSnapshot;
import com.example.freshtrack.models.UserNotification;
import java.util.Calendar;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView notificationsRecyclerView;
    private TextView noNotificationsText;
    private FirebaseModel firebaseModel;

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

        firebaseModel = new FirebaseModel();
        initializeViews();
        loadNotifications();
        setupBottomNavigation();
    }

    private void initializeViews() {
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        noNotificationsText = findViewById(R.id.noNotificationsText);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadNotifications() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Load items expiring today
        firebaseModel.getFoodItemsByUser(userId).get().addOnSuccessListener(dataSnapshot -> {
            List<UserNotification> notifications = new ArrayList<>();
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                FoodItem item = snapshot.getValue(FoodItem.class);
                if (item != null) {
                    Calendar expiryCalendar = Calendar.getInstance();
                    expiryCalendar.setTimeInMillis(item.getExpiryDate());
                    expiryCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    expiryCalendar.set(Calendar.MINUTE, 0);
                    expiryCalendar.set(Calendar.SECOND, 0);
                    expiryCalendar.set(Calendar.MILLISECOND, 0);

                    if (expiryCalendar.getTimeInMillis() == today.getTimeInMillis()) {
                        UserNotification notification = new UserNotification(
                            item.getId(),
                            userId,
                            item.getName(),
                            System.currentTimeMillis()
                        );
                        notifications.add(notification);
                    }
                }
            }
            updateNotificationsUI(notifications);
        });
    }

    private void updateNotificationsUI(List<UserNotification> notifications) {
        if (notifications.isEmpty()) {
            noNotificationsText.setVisibility(View.VISIBLE);
            noNotificationsText.setText("No items expiring today");
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
        View btnAdd = bottomNav.findViewById(R.id.btnAdd);
        View btnNotifications = bottomNav.findViewById(R.id.btnNotifications);
        View btnSettings = bottomNav.findViewById(R.id.btnSettings);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, AddListActivity.class);
            startActivity(intent);
            finish();
        });

        btnNotifications.setOnClickListener(v -> {
            // Already on notifications page
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(NotificationsActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
} 