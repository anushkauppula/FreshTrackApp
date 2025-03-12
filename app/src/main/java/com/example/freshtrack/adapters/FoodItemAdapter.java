package com.example.freshtrack.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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

public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodItemViewHolder> {
    private List<FoodItem> foodItems;
    private List<FoodItem> foodItemsFiltered; // For filtered results
    private FirebaseModel firebaseModel;
    private static final String PREFS_NAME = "DeleteDialogPrefs";
    private static final String KEY_DONT_SHOW_AGAIN = "dontShowDeleteDialog";

    public FoodItemAdapter(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
        this.foodItemsFiltered = new ArrayList<>(foodItems);
        this.firebaseModel = new FirebaseModel();
    }

    // Add this method for filtering
    public void filter(String query) {
        foodItemsFiltered.clear();
        if (query.isEmpty()) {
            foodItemsFiltered.addAll(foodItems);
        } else {
            String searchQuery = query.toLowerCase();
            for (FoodItem item : foodItems) {
                if (item.getName().toLowerCase().contains(searchQuery)) {
                    foodItemsFiltered.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new FoodItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodItemViewHolder holder, int position) {
        FoodItem item = foodItemsFiltered.get(position); // Use filtered list
        holder.bind(item);
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
        private Context context;

        public FoodItemViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);

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
            snackbar.setAnchorView(((MainActivityHome) context).findViewById(R.id.bottomNav)); // Set anchor to bottom nav
            snackbar.show();

            // Delete the item from Firebase after a delay if not undone
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        // If the Snackbar was dismissed without clicking "UNDO", delete the item
                        firebaseModel.deleteFoodItem(itemId)
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

        public void bind(FoodItem item) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

            tvFoodName.setText(item.getName());
            tvExpiryDate.setText("Expires: " + dateFormat.format(new Date(item.getExpiryDate())));
            
            // Set category, weight, and count
            TextView tvCategory = itemView.findViewById(R.id.tvCategory);
            TextView tvWeight = itemView.findViewById(R.id.tvWeight);
            TextView tvCount = itemView.findViewById(R.id.tvCount);

            tvCategory.setText("Category: " + item.getCategory());
            tvWeight.setText("Weight: " + (item.getWeight() != null ? item.getWeight() + " lbs" : "N/A"));
            tvCount.setText("Count: " + (item.getCount() != null ? item.getCount() : "N/A"));

            // Set status with appropriate background and text color
            TextView tvStatus = this.tvStatus;
            long daysUntilExpiry = (item.getExpiryDate() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
            
            if (daysUntilExpiry < 0) {
                tvStatus.setText("Expired");
                tvStatus.setBackground(itemView.getContext().getDrawable(R.drawable.tag_expired));
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.white));
            } else if (daysUntilExpiry <= 3) {
                tvStatus.setText("Expiring Soon");
                tvStatus.setBackground(itemView.getContext().getDrawable(R.drawable.tag_expiring));
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.white));
            } else {
                tvStatus.setText("Fresh");
                tvStatus.setBackground(itemView.getContext().getDrawable(R.drawable.tag_fresh));
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.white));
            }
        }
    }
}