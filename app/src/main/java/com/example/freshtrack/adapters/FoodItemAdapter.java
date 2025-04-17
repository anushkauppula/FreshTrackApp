package com.example.freshtrack.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.MainActivityHome;
import com.example.freshtrack.R;
import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.models.FoodItem;
import com.google.android.material.snackbar.Snackbar;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.freshtrack.models.UserSettings;

public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodItemViewHolder> {
    private List<FoodItem> foodItems;
    private List<FoodItem> foodItemsFiltered; // For filtered results
    private FirebaseModel firebaseModel;
    private static final String PREFS_NAME = "DeleteDialogPrefs";
    private static final String KEY_DONT_SHOW_AGAIN = "dontShowDeleteDialog";
    private Context context;

    public FoodItemAdapter(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
        this.foodItemsFiltered = new ArrayList<>(foodItems);
        this.firebaseModel = new FirebaseModel();
    }

    // Update the filter method to search by both name and category
    public void filter(String query) {
        foodItemsFiltered.clear();
        if (query.isEmpty()) {
            foodItemsFiltered.addAll(foodItems);
        } else {
            String searchQuery = query.toLowerCase();
            for (FoodItem item : foodItems) {
                // Check if either the name or category contains the search query
                if (item.getName().toLowerCase().contains(searchQuery) || 
                    item.getCategory().toLowerCase().contains(searchQuery)) {
                    foodItemsFiltered.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get context from parent view
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_food, parent, false);
        return new FoodItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodItemViewHolder holder, int position) {
        FoodItem item = foodItemsFiltered.get(position);
        holder.tvFoodName.setText(item.getName());
        
        // Format and set expiry date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String expiryDate = sdf.format(new Date(item.getExpiryDate()));
        holder.tvExpiryDate.setText("Expires: " + expiryDate);
        
        holder.tvCategory.setText("Category: " + item.getCategory());
        holder.tvWeight.setText("Weight: " + item.getWeight());
        holder.tvCount.setText("Count: " + item.getCount());

        // Calculate status based on expiry date
        long currentTime = System.currentTimeMillis();
        
        // Reset current time to start of day (midnight)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get expiry date calendar
        Calendar expiryCalendar = Calendar.getInstance();
        expiryCalendar.setTimeInMillis(item.getExpiryDate());
        expiryCalendar.set(Calendar.HOUR_OF_DAY, 0);
        expiryCalendar.set(Calendar.MINUTE, 0);
        expiryCalendar.set(Calendar.SECOND, 0);
        expiryCalendar.set(Calendar.MILLISECOND, 0);

        // Calculate days until expiry
        long daysUntilExpiry = (expiryCalendar.getTimeInMillis() - today.getTimeInMillis()) / (24 * 60 * 60 * 1000);

        TextView statusView = holder.tvStatus;
        if (daysUntilExpiry < 0) {
            // Expired (before today)
            statusView.setText("Expired");
            statusView.setBackgroundColor(ContextCompat.getColor(context, R.color.expired_red));
        } else if (daysUntilExpiry <= 2) {
            // Expiring Soon (today, tomorrow, or day after tomorrow)
            statusView.setText("Expiring Soon");
            statusView.setBackgroundColor(ContextCompat.getColor(context, R.color.expiring_soon_orange));
        } else {
            // Fresh (3 or more days until expiry)
            statusView.setText("Fresh");
            statusView.setBackgroundColor(ContextCompat.getColor(context, R.color.fresh_green));
        }
        statusView.setTextColor(Color.WHITE);
        statusView.setPadding(8, 4, 8, 4);
    }

    @Override
    public int getItemCount() {
        return foodItemsFiltered.size(); // Use filtered list size
    }

    public void updateItems(List<FoodItem> newItems) {
        this.foodItems = newItems;

        // Sort the items by dateAdded in descending order
        Collections.sort(this.foodItems, new Comparator<FoodItem>() {
            @Override
            public int compare(FoodItem item1, FoodItem item2) {
                return Long.compare(item2.getDateAdded(), item1.getDateAdded()); // Most recent first
            }
        });

        this.foodItemsFiltered = new ArrayList<>(this.foodItems);
        notifyDataSetChanged();
    }

    public List<FoodItem> getFoodItems() {
        return foodItems;
    }

    // Add public method to handle deletion from outside
    public void handleSwipeDeletion(int position) {
        if (position != RecyclerView.NO_POSITION && position < foodItems.size()) {
            FoodItem item = foodItems.get(position);
            handleItemDeletion(item, context, position);
        }
    }

    private void showDeleteConfirmationDialog(FoodItem item, Context context, int swipedPosition, boolean showDialog) {
        if (!showDialog) {
            deleteItem(item, context);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null);
        CheckBox checkboxDontShowAgain = dialogView.findViewById(R.id.checkboxDontShowAgain);
        TextView messageText = dialogView.findViewById(R.id.dialogMessage);
        messageText.setText("Are you sure you want to delete " + item.getName() + "?");

        builder.setView(dialogView)
            .setTitle("Delete Confirmation")
            .setPositiveButton("Ok", (dialog, which) -> {
                // Perform deletion
                deleteItem(item, context);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // If this was triggered by a swipe, restore the view
                if (swipedPosition != -1) {
                    notifyItemChanged(swipedPosition);
                }
            })
            .show();

        // If the checkbox is checked, turn off the toggle in settings
        checkboxDontShowAgain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Turn off the toggle in settings
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    firebaseModel.updateUserSettings(userId, Collections.singletonMap("showDeleteConfirmation", false));
                }
            }
        });
    }

    public void deleteItem(FoodItem item, Context context) {
        // Remove the item from the list
        foodItems.remove(item);
        notifyDataSetChanged(); // Notify the adapter to refresh the view

        // Optionally, delete the item from the Firebase database
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firebaseModel.deleteFoodItem(userId, item.getId()) // Assuming FoodItem has a method getId()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e("FoodItemAdapter", "Error deleting item: " + e.getMessage());
                Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show();
            });
    }

    void handleItemDeletion(FoodItem item, Context context, int swipedPosition) {
        // Get the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Check the "Show Delete Confirmation" setting from the database
            firebaseModel.getUserSettings(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            UserSettings settings = dataSnapshot.getValue(UserSettings.class);
                            if (settings != null) {
                                boolean showDeleteConfirmation = settings.isShowDeleteConfirmation();
                                showDeleteConfirmationDialog(item, context, swipedPosition, showDeleteConfirmation);
                            }
                        } else {
                            // If user settings do not exist, delete directly
                            deleteItem(item, context);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FoodItemAdapter", "Error loading user settings: " + error.getMessage());
                        // Optionally delete directly if there's an error
                        deleteItem(item, context);
                    }
                });
        }
    }

    class FoodItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFoodName;
        private TextView tvExpiryDate;
        private TextView tvStatus;
        private TextView tvCategory;
        private TextView tvWeight;
        private TextView tvCount;
        private Context context;

        public FoodItemViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvCount = itemView.findViewById(R.id.tvCount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handleItemDeletion(foodItems.get(position), context, position);
                }
            });
        }
    }
}