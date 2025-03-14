package com.example.freshtrack.models;

public class UserSettings {
    private String userId;
    private String layoutType;  // "list" or "grid"
    private String theme;       // "light", "dark", or "system"
    private boolean showDeleteConfirmation;
    private int expiryNotificationDays;  // Days before expiry to notify
    private boolean notificationsEnabled;

    // Required empty constructor for Firebase
    public UserSettings() {}

    public UserSettings(String userId) {
        this.userId = userId;
        this.layoutType = "list";  // default value
        this.theme = "system";     // default value
        this.showDeleteConfirmation = true;
        this.expiryNotificationDays = 3;
        this.notificationsEnabled = true;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(String layoutType) {
        this.layoutType = layoutType;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isShowDeleteConfirmation() {
        return showDeleteConfirmation;
    }

    public void setShowDeleteConfirmation(boolean showDeleteConfirmation) {
        this.showDeleteConfirmation = showDeleteConfirmation;
    }

    public int getExpiryNotificationDays() {
        return expiryNotificationDays;
    }

    public void setExpiryNotificationDays(int expiryNotificationDays) {
        this.expiryNotificationDays = expiryNotificationDays;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
} 