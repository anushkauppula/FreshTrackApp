package com.example.freshtrack.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
                    handleItemDeletion(foodItems.get(position));
                }
            });
        }

        private void handleItemDeletion(FoodItem item) {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean dontShowDialog = preferences.getBoolean(KEY_DONT_SHOW_AGAIN, false);

            if (dontShowDialog) {
                // If user chose to not show dialog, delete directly
                deleteItem(item);
            } else {
                showDeleteConfirmationDialog(item);
            }
        }

        private void deleteItem(FoodItem item) {
            // Store the item temporarily for undo
            String itemName = item.getName();
            String itemId = item.getId();
            String userId = item.getUserId(); // Get the user ID from the item

            // Remove the item from the list temporarily
            foodItemsFiltered.remove(item);
            notifyDataSetChanged(); // Update the UI immediately

            // Show Snackbar for undo
            Snackbar snackbar = Snackbar.make(((MainActivityHome) context).findViewById(R.id.bottomNav),
                    itemName + " deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        // Restore the item
                        foodItemsFiltered.add(item);
                        notifyItemInserted(foodItemsFiltered.size() - 1);
                        firebaseModel.addFoodItem(item); // Re-add to Firebase
                    });

            // Customize Snackbar layout
            snackbar.setActionTextColor(context.getResources().getColor(android.R.color.holo_blue_light));
            snackbar.setAnchorView(((MainActivityHome) context).findViewById(R.id.bottomNav));
            snackbar.show();

            // Delete the item from Firebase after a delay if not undone
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        // If the Snackbar was dismissed without clicking "UNDO", delete the item
                        firebaseModel.deleteFoodItem(userId, itemId) // Pass both userId and itemId
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Item deleted permanently", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to delete item: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            });
        }

        private void showDeleteConfirmationDialog(FoodItem item) {
            // Inflate custom layout for dialog
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null);
            CheckBox checkBox = dialogView.findViewById(R.id.checkboxDontShowAgain);
            TextView messageText = dialogView.findViewById(R.id.dialogMessage);
            messageText.setText("Are you sure you want to delete " + item.getName() + "?");

            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setView(dialogView)
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Save checkbox preference
                        if (checkBox.isChecked()) {
                            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            preferences.edit().putBoolean(KEY_DONT_SHOW_AGAIN, true).apply();
                        }
                        // Delete the item
                        deleteItem(item);
                    })
                    .setNegativeButton("Cancel", null);

            builder.create().show();
        }
    }
}