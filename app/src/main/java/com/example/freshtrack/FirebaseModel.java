package com.example.freshtrack;

import com.example.freshtrack.models.FoodItem;
import com.example.freshtrack.models.User;
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

public class FirebaseModel {
    private final DatabaseReference databaseReference;
    private static final String FOOD_ITEMS_PATH = "food_items";
    private static final String USERS_PATH = "users";
    private FirebaseFirestore db;

    public FirebaseModel() {
        // Initialize database reference outside try-catch
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseModel", "Error initializing Firestore: " + e.getMessage());
        }
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
        return databaseReference.child(USERS_PATH)
            .orderByChild("username")
            .equalTo(username)
            .get();
    }
}
