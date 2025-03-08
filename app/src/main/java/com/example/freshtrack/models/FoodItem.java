package com.example.freshtrack.models;

import android.graphics.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FoodItem {
    public static final int STATUS_EXPIRED = 0;
    public static final int STATUS_EXPIRING_SOON = 1;
    public static final int STATUS_FRESH = 2;
    
    private String id;
    private String foodName;
    private String expiryDate;
    private String userId;
    private long timestamp;

    // Empty constructor required for Firebase
    public FoodItem() {
    }

    public FoodItem(String foodName, String expiryDate, String userId) {
        this.foodName = foodName;
        this.expiryDate = expiryDate;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    // Calculate status based on expiry date
    public int calculateStatus() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Date expDate = sdf.parse(expiryDate);
            Date today = new Date();
            
            // Reset hours, minutes, seconds and millis for accurate day comparison
            today.setHours(0);
            today.setMinutes(0);
            today.setSeconds(0);
            
            // Calculate days difference
            long diffInMillies = expDate.getTime() - today.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            
            if (diffInDays < 0) {
                return STATUS_EXPIRED;
            } else if (diffInDays <= 1) { // Today or tomorrow
                return STATUS_EXPIRING_SOON;
            } else {
                return STATUS_FRESH;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return STATUS_FRESH; // Default to fresh if there's an error
        }
    }

    public String getStatusText() {
        switch (calculateStatus()) {
            case STATUS_EXPIRED:
                return "Expired";
            case STATUS_EXPIRING_SOON:
                return "Expiring Soon";
            case STATUS_FRESH:
                return "Fresh";
            default:
                return "Unknown";
        }
    }

    public int getStatusColor() {
        switch (calculateStatus()) {
            case STATUS_EXPIRED:
                return Color.RED; // Use same red as delete background
            case STATUS_EXPIRING_SOON:
                return Color.parseColor("#FFA500"); // Orange
            case STATUS_FRESH:
                return Color.GREEN;
            default:
                return Color.GRAY;
        }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 