package com.example.freshtrack;

import com.example.freshtrack.models.FoodItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;
import android.util.Log;

public class FirebaseModel {
    private DatabaseReference mDatabase;
    private static final String FOOD_ITEMS_REF = "food_items";
    private FirebaseFirestore db;

    public FirebaseModel() {
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseModel", "Error initializing Firebase: " + e.getMessage());
        }
    }

    // Add a new food item
    public Task<Void> addFoodItem(FoodItem foodItem) {
        if (mDatabase == null) {
            Log.e("FirebaseModel", "Database reference is null");
            return Tasks.forException(new Exception("Database not initialized"));
        }
        
        String itemId = mDatabase.child(FOOD_ITEMS_REF).push().getKey();
        if (itemId == null) {
            Log.e("FirebaseModel", "Failed to generate item ID");
            return Tasks.forException(new Exception("Failed to generate item ID"));
        }
        
        foodItem.setId(itemId);
        return mDatabase.child(FOOD_ITEMS_REF).child(itemId).setValue(foodItem);
    }

    // Update a food item
    public Task<Void> updateFoodItem(FoodItem foodItem) {
        if (mDatabase == null) {
            return Tasks.forException(new Exception("Database not initialized"));
        }
        return mDatabase.child(FOOD_ITEMS_REF).child(foodItem.getId()).setValue(foodItem);
    }

    // Delete a food item
    public Task<Void> deleteFoodItem(String itemId) {
        if (mDatabase == null) {
            return Tasks.forException(new Exception("Database not initialized"));
        }
        return mDatabase.child(FOOD_ITEMS_REF).child(itemId).removeValue();
    }

    // Get all food items for a specific user
    public Query getFoodItemsByUser(String userId) {
        if (mDatabase == null) {
            Log.e("FirebaseModel", "Database reference is null");
            throw new IllegalStateException("Database not initialized");
        }
        return mDatabase.child(FOOD_ITEMS_REF).orderByChild("userId").equalTo(userId);
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
}
