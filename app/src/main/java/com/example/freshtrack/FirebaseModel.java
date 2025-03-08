package com.example.freshtrack;

import com.example.freshtrack.models.FoodItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class FirebaseModel {
    private DatabaseReference mDatabase;
    private static final String FOOD_ITEMS_REF = "food_items";

    public FirebaseModel() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // Add a new food item
    public Task<Void> addFoodItem(FoodItem foodItem) {
        String itemId = mDatabase.child(FOOD_ITEMS_REF).push().getKey();
        foodItem.setId(itemId);
        return mDatabase.child(FOOD_ITEMS_REF).child(itemId).setValue(foodItem);
    }

    // Update a food item
    public Task<Void> updateFoodItem(FoodItem foodItem) {
        return mDatabase.child(FOOD_ITEMS_REF).child(foodItem.getId()).setValue(foodItem);
    }

    // Delete a food item
    public Task<Void> deleteFoodItem(String itemId) {
        return mDatabase.child(FOOD_ITEMS_REF).child(itemId).removeValue();
    }

    // Get all food items for a specific user
    public Query getFoodItemsByUser(String userId) {
        return mDatabase.child(FOOD_ITEMS_REF).orderByChild("userId").equalTo(userId);
    }
}
