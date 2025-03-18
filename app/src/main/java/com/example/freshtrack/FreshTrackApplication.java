package com.example.freshtrack;

import android.app.Application;
import android.util.Log;
import androidx.work.*;
import androidx.core.content.ContextCompat;
import com.example.freshtrack.notifications.DailyNotificationWorker;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.List;
import com.google.common.util.concurrent.ListenableFuture;

public class FreshTrackApplication extends Application {
    private static final String TAG = "FreshTrackApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            scheduleDailyNotifications();
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule daily notifications: " + e.getMessage());
        }
    }

    private void scheduleDailyNotifications() {
        // Set up work constraints
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        // Schedule daily notification check at 9:46 PM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4); // 4 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
        Log.d(TAG, "Scheduling daily notifications with initial delay: " + initialDelay + "ms");
        
        PeriodicWorkRequest dailyCheckRequest =
            new PeriodicWorkRequest.Builder(DailyNotificationWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("daily_notification_check")
                .build();

        // Replace any existing work with the new schedule
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "DailyNotificationCheck",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyCheckRequest
            );

        Log.d(TAG, "Daily notification check scheduled successfully");

        // For debugging: Get work info
        WorkManager.getInstance(this)
            .getWorkInfosByTag("daily_notification_check")
            .addListener(() -> {
                try {
                    ListenableFuture<List<WorkInfo>> workInfosFuture = 
                        WorkManager.getInstance(this).getWorkInfosByTag("daily_notification_check");
                    List<WorkInfo> workInfos = workInfosFuture.get();
                    if (workInfos != null && !workInfos.isEmpty()) {
                        Log.d(TAG, "Work status: " + workInfos.get(0).getState());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking work status: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(this));
    }
} 