package com.example.freshtrack.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.NotificationsActivity;
import com.example.freshtrack.R;
import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.models.UserNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;

import java.util.Calendar;

public class ExpiryCheckWorker extends Worker {
    private static final String CHANNEL_ID = "FoodExpiryChannel";
    private final Context context;
    private final FirebaseModel firebaseModel;

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.firebaseModel = new FirebaseModel();
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return Result.failure();
        }
        String userId = auth.getCurrentUser().getUid();

        createNotificationChannel();
        checkExpiringItems(userId);
        
        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Food Expiry Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for expiring and expired food items");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkExpiringItems(String userId) {
        NotificationHelper notificationHelper = new NotificationHelper(context);
        firebaseModel.getFoodItemsByUser(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot == null || !dataSnapshot.exists()) {
                    return;
                }

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

                        // Schedule notification for expiry day
                        if (expiryCalendar.after(today)) {
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
        });
    }
} 