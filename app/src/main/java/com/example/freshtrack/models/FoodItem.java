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
    private String name;
    private long dateAdded;
    private long expiryDate;
    private String userId;
    private String category;
    private int quantity;
    private String unit;
    private String notes;

    // Empty constructor required for Firestore
    public FoodItem() {
    }

    public FoodItem(String name, long dateAdded, long expiryDate, String userId, 
                   String category, int quantity, String unit, String notes) {
        this.name = name;
        this.dateAdded = dateAdded;
        this.expiryDate = expiryDate;
        this.userId = userId;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.notes = notes;
    }

    // Calculate status based on expiry date
    public int calculateStatus() {
        long currentTime = System.currentTimeMillis();
        long diffInMillies = expiryDate - currentTime;
        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        
        if (diffInDays < 0) {
            return STATUS_EXPIRED;
        } else if (diffInDays <= 1) { // Today or tomorrow
            return STATUS_EXPIRING_SOON;
        } else {
            return STATUS_FRESH;
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
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
} 