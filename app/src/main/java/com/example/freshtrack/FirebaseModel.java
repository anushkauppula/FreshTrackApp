package com.example.freshtrack;

import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.models.User;
import com.example.freshtrack.models.UserNotification;
import com.example.freshtrack.models.UserSettings;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;
import android.util.Log;
import java.util.HashMap;

public class FirebaseModel {
    private final DatabaseReference databaseReference;
    private static final String FOOD_ITEMS_PATH = "food_items";
    private static final String USERS_PATH = "users";
    private static final String USER_SETTINGS_PATH = "user_settings";
    private FirebaseFirestore db;
    private final FirebaseDatabase database;
    private final DatabaseReference foodItemsRef;
    private final DatabaseReference notificationsRef;

    public FirebaseModel() {
        // Initialize database reference outside try-catch
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseModel", "Error initializing Firestore: " + e.getMessage());
        }

        database = FirebaseDatabase.getInstance();
        foodItemsRef = database.getReference("food_items");
        notificationsRef = database.getReference("user_notifications");
    }

    // User operations
    public Task<Void> createUser(User user) {
        return databaseReference.child(USERS_PATH)
            .child(user.getId())
            .setValue(user);
    }

    public Task<DataSnapshot> getUserById(String userId) {
        return databaseReference.child(USERS_PATH)
            .child(userId)
            .get();
    }

    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        return databaseReference.child(USERS_PATH)
            .child(userId)
            .updateChildren(updates);
    }

    // Modified food item operations to include user ID
    public Task<Void> addFoodItem(FoodItem foodItem) {
        String key = databaseReference.child(FOOD_ITEMS_PATH).push().getKey();
        if (key != null) {
            foodItem.setId(key);
            return databaseReference.child(FOOD_ITEMS_PATH)
                .child(foodItem.getUserId()) // Organize by user ID
                .child(key)
                .setValue(foodItem);
        }
        return Tasks.forException(new Exception("Failed to generate key"));
    }

    public Query getFoodItemsByUser(String userId) {
        return databaseReference.child(FOOD_ITEMS_PATH)
            .child(userId)
            .orderByChild("dateAdded");
    }

    public Task<Void> deleteFoodItem(String userId, String itemId) {
        return databaseReference.child(FOOD_ITEMS_PATH)
            .child(userId)
            .child(itemId)
            .removeValue();
    }

    public Task<DocumentSnapshot> getUserProfile(String userId) {
        if (db == null) {
            return Tasks.forException(new Exception("Firestore not initialized"));
        }
        return db.collection("users")
                .document(userId)
                .get();
    }

    public Task<Void> updateUserProfile(String userId, Map<String, Object> profileData) {
        if (db == null) {
            return Tasks.forException(new Exception("Firestore not initialized"));
        }
        return db.collection("users")
                .document(userId)
                .set(profileData);
    }

    public Task<DataSnapshot> getUserByUsername(String username) {
        Log.d("FirebaseModel", "Searching for user with username: " + username);
        // Query all users and check username
        return databaseReference.child(USERS_PATH)
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                Log.d("FirebaseModel", "Got users data, exists: " + dataSnapshot.exists());
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            Log.d("FirebaseModel", "Checking user: " + user.getUsername());
                            if (user.getUsername().equals(username)) {
                                Log.d("FirebaseModel", "Found matching user with email: " + user.getEmail());
                            }
                        }
                    }
                }
            })
            .addOnFailureListener(e -> 
                Log.e("FirebaseModel", "Error searching for user: " + e.getMessage()));
    }

    // User Settings operations
    public Task<Void> createUserSettings(UserSettings settings) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(settings.getUserId())
            .setValue(settings);
    }

    public Task<DataSnapshot> getUserSettings(String userId) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .get();
    }

    public Task<Void> updateUserSettings(String userId, Map<String, Object> updates) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .updateChildren(updates);
    }

    public Task<Void> updateLayoutType(String userId, String layoutType) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .child("layoutType")
            .setValue(layoutType);
    }

    public Task<Void> updateTheme(String userId, String theme) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .child("theme")
            .setValue(theme);
    }

    public Task<Void> updateNotificationSettings(String userId, boolean enabled, int days) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationsEnabled", enabled);
        updates.put("expiryNotificationDays", days);
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .updateChildren(updates);
    }

    public Task<Void> updateDeleteConfirmation(String userId, boolean showConfirmation) {
        return databaseReference.child(USER_SETTINGS_PATH)
            .child(userId)
            .child("showDeleteConfirmation")
            .setValue(showConfirmation);
    }

    public String getNewNotificationId() {
        return notificationsRef.push().getKey();
    }

    public Task<Void> addNotification(UserNotification notification) {
        return notificationsRef.child(notification.getId()).setValue(notification);
    }

    public Query getNotificationsByUser(String userId) {
        return notificationsRef.orderByChild("userId").equalTo(userId);
    }

    public Task<Void> markNotificationAsRead(String notificationId) {
        return notificationsRef.child(notificationId).child("read").setValue(true);
    }
}
