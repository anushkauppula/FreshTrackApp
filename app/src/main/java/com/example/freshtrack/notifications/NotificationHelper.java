package com.example.freshtrack.notifications;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.freshtrack.R;
import com.example.freshtrack.NotificationsActivity;
import java.util.Calendar;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.models.UserNotification;

public class NotificationHelper {
    private static final String CHANNEL_ID = "freshtrack_notifications";
    private final Context context;
    private final NotificationManager notificationManager;
    private final FirebaseModel firebaseModel;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.firebaseModel = new FirebaseModel();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "FreshTrack Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for expiring food items");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);
            channel.setLightColor(context.getResources().getColor(R.color.expiring_soon_orange));
            channel.setShowBadge(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotification(String userId, String itemName, String notificationId, long expiryTime) {
        Log.d("NotificationHelper", "Checking item: " + itemName);

        // Calculate days until expiry
        Calendar today = Calendar.getInstance();
        Calendar expiryCalendar = Calendar.getInstance();
        expiryCalendar.setTimeInMillis(expiryTime);
        
        // Reset time part to compare just dates
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        expiryCalendar.set(Calendar.HOUR_OF_DAY, 0);
        expiryCalendar.set(Calendar.MINUTE, 0);
        expiryCalendar.set(Calendar.SECOND, 0);
        expiryCalendar.set(Calendar.MILLISECOND, 0);

        // Only show notification if item expires today
        if (expiryCalendar.getTimeInMillis() == today.getTimeInMillis()) {
            Log.d("NotificationHelper", "Item " + itemName + " expires today, showing notification");
            // Create and save notification to database first
            UserNotification notification = new UserNotification(
                notificationId,
                userId,
                itemName,
                System.currentTimeMillis()
            );

            firebaseModel.addNotification(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NotificationHelper", "Notification saved to database, showing notification");
                    showNotification(itemName, notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationHelper", "Failed to save notification: " + e.getMessage());
                    // Show notification anyway even if save fails
                    showNotification(itemName, notificationId);
                });
            return;
        }
        
        // If not expiring today, schedule for the morning of expiry day
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.setTimeInMillis(expiryTime);
        notificationTime.set(Calendar.HOUR_OF_DAY, 4); // 4 AM
        notificationTime.set(Calendar.MINUTE, 0);
        notificationTime.set(Calendar.SECOND, 0);

        Log.d("NotificationHelper", "Notification scheduled for: " + notificationTime.getTime());

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("itemName", itemName);
        intent.putExtra("userId", userId);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.getTimeInMillis(),
                    pendingIntent
                );
            }
            Log.d("NotificationHelper", "Alarm set successfully for " + itemName);
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error setting alarm: " + e.getMessage());
        }
    }

    private void showNotification(String itemName, String notificationId) {
        Log.d("NotificationHelper", "Building notification for: " + itemName);
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("NOTIFICATION_ID", notificationId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Food Item Expires Today!")
            .setContentText(itemName + " expires today")
            .setColor(context.getResources().getColor(R.color.expiring_soon_orange))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        try {
            int uniqueId = (itemName + notificationId).hashCode();
            Log.d("NotificationHelper", "Attempting to show notification with ID: " + uniqueId);
            notificationManager.notify(uniqueId, builder.build());
            Log.d("NotificationHelper", "Notification sent successfully for " + itemName);
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error showing notification: " + e.getMessage());
        }
    }
} 