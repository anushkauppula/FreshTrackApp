package com.example.freshtrack.notifications;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DailyNotificationWorker extends Worker {
    private static final String TAG = "DailyNotificationWorker";

    public DailyNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Log.d(TAG, "Starting daily notification check");
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in");
            return Result.success();
        }

        String userId = currentUser.getUid();
        FirebaseModel firebaseModel = new FirebaseModel();
        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        
        // Use CountDownLatch to wait for Firebase operation to complete
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {true};

        firebaseModel.getFoodItemsByUser(userId).get()
            .addOnSuccessListener(dataSnapshot -> {
                try {
                    processItems(dataSnapshot, userId, notificationHelper);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing items: " + e.getMessage());
                    success[0] = false;
                }
                latch.countDown();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting food items: " + e.getMessage());
                success[0] = false;
                latch.countDown();
            });

        try {
            // Wait for Firebase operation to complete (max 30 seconds)
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Operation timed out");
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    private void processItems(DataSnapshot dataSnapshot, String userId, NotificationHelper notificationHelper) {
        if (!dataSnapshot.exists()) {
            Log.d(TAG, "No items found");
            return;
        }

        FirebaseModel firebaseModel = new FirebaseModel();
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
                    String notificationId = firebaseModel.getNewNotificationId();
                    if (notificationId != null) {
                        notificationHelper.scheduleNotification(
                            userId,
                            item.getName(),
                            notificationId,
                            item.getExpiryDate()
                        );
                    }
                }
            }
        }
    }
} 