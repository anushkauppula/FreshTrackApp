package com.example.freshtrack.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.models.UserNotification;
import com.google.firebase.database.DataSnapshot;
import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Received notification broadcast");
        if (intent == null) {
            Log.e("NotificationReceiver", "Intent is null");
            return;
        }

        String itemName = intent.getStringExtra("itemName");
        String userId = intent.getStringExtra("userId");
        String notificationId = intent.getStringExtra("notificationId");

        if (itemName == null || userId == null || notificationId == null) {
            Log.e("NotificationReceiver", "Required extras are missing");
            return;
        }

        // Check if the item expires today before showing notification
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        FirebaseModel firebaseModel = new FirebaseModel();
        firebaseModel.getFoodItemsByUser(userId).get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    FoodItem item = snapshot.getValue(FoodItem.class);
                    if (item != null && item.getName().equals(itemName)) {
                        Calendar expiryCalendar = Calendar.getInstance();
                        expiryCalendar.setTimeInMillis(item.getExpiryDate());
                        expiryCalendar.set(Calendar.HOUR_OF_DAY, 0);
                        expiryCalendar.set(Calendar.MINUTE, 0);
                        expiryCalendar.set(Calendar.SECOND, 0);
                        expiryCalendar.set(Calendar.MILLISECOND, 0);

                        if (expiryCalendar.getTimeInMillis() == today.getTimeInMillis()) {
                            Log.d("NotificationReceiver", "Showing notification for item expiring today: " + itemName);
                            // Show notification immediately
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            notificationHelper.showNotification(itemName, notificationId);

                            // Create notification in database
                            UserNotification notification = new UserNotification(
                                notificationId,
                                userId,
                                itemName,
                                System.currentTimeMillis()
                            );

                            firebaseModel.addNotification(notification)
                                .addOnSuccessListener(aVoid -> 
                                    Log.d("NotificationReceiver", "Notification saved to database"))
                                .addOnFailureListener(e -> {
                                    Log.e("NotificationReceiver", "Failed to add notification: " + e.getMessage());
                                });
                        } else {
                            Log.d("NotificationReceiver", "Item " + itemName + " does not expire today, skipping notification");
                        }
                        break;
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("NotificationReceiver", "Error checking item expiry: " + e.getMessage());
        });
    }
} 