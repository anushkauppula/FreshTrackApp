package com.example.freshtrack.models;

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
    private int status;

    // Empty constructor required for Firebase
    public FoodItem() {
    }

    public FoodItem(String foodName, String expiryDate, String userId) {
        this.foodName = foodName;
        this.expiryDate = expiryDate;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        calculateStatus();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void calculateStatus() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date expiry = sdf.parse(expiryDate);
            Date today = new Date();
            
            // If the date is already passed
            if (expiry.before(today)) {
                this.status = STATUS_EXPIRED;
                return;
            }

            // Calculate days until expiry
            long diffInMillies = expiry.getTime() - today.getTime();
            long daysUntilExpiry = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            // If expiring within 3 days
            if (daysUntilExpiry <= 3) {
                this.status = STATUS_EXPIRING_SOON;
            } else {
                this.status = STATUS_FRESH;
            }
        } catch (ParseException e) {
            this.status = STATUS_FRESH; // Default status if date parsing fails
        }
    }

    public String getStatusText() {
        switch (status) {
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
        switch (status) {
            case STATUS_EXPIRED:
                return android.graphics.Color.RED;
            case STATUS_EXPIRING_SOON:
                return android.graphics.Color.parseColor("#FFA500"); // Orange
            case STATUS_FRESH:
                return android.graphics.Color.GREEN;
            default:
                return android.graphics.Color.GRAY;
        }
    }
} 