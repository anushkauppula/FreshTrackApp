package com.example.freshtrack.models;

public class UserNotification {
    private String id;
    private String userId;
    private String itemName;
    private long timestamp;
    private boolean isRead;
    private String type; // "EXPIRES_TODAY"

    public UserNotification() {
        // Required empty constructor for Firebase
    }

    public UserNotification(String id, String userId, String itemName, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.timestamp = timestamp;
        this.isRead = false;
        this.type = "EXPIRES_TODAY";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
} 