package com.example.freshtrack;

import android.app.Application;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.freshtrack.notifications.ExpiryCheckWorker;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

public class FreshTrackApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            scheduleExpiryCheck();
        } catch (Exception e) {
            Log.e("FreshTrackApp", "Failed to schedule expiry check: " + e.getMessage());
        }
    }

    private void scheduleExpiryCheck() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return; // Don't schedule if user is not logged in
        }

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        // Cancel any existing work
        WorkManager.getInstance(this).cancelAllWork();
        
        // Schedule immediate work
        WorkManager.getInstance(this).enqueue(
            new OneTimeWorkRequest.Builder(ExpiryCheckWorker.class)
                .setConstraints(constraints)
                .build()
        );
    }
} 