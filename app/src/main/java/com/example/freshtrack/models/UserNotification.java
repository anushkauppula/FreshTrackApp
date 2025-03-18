package com.example.freshtrack.models;

public class UserNotification {
    private String id;
    private String userId;
    private String itemName;
    private long timestamp;
    private boolean read;
    private String type; // "EXPIRES_TODAY"

    public UserNotification() {
        // Required empty constructor for Firebase
    }

    public UserNotification(String id, String userId, String itemName, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.timestamp = timestamp;
        this.read = false;
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
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return "UserNotification{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", itemName='" + itemName + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
} 