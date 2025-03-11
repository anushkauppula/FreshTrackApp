package com.example.freshtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshtrack.models.FoodItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
    private List<FoodItem> items;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public RecentActivityAdapter(List<FoodItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<FoodItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemNameText;
        private final TextView itemDateText;
        private final TextView itemExpiryText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.itemNameText);
            itemDateText = itemView.findViewById(R.id.itemDateText);
            itemExpiryText = itemView.findViewById(R.id.itemExpiryText);
        }

        public void bind(FoodItem item) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            
            itemNameText.setText(item.getName());
            
            String addedDate = dateFormat.format(new Date(item.getDateAdded()));
            String expiryDate = dateFormat.format(new Date(item.getExpiryDate()));
            
            itemDateText.setText(String.format("Added: %s", addedDate));
            itemExpiryText.setText(String.format("Expires: %s", expiryDate));

            if (item.getQuantity() > 0) {
                String nameWithQuantity = String.format("%s (%d %s)", 
                    item.getName(), 
                    item.getQuantity(), 
                    item.getUnit() != null ? item.getUnit() : "");
                itemNameText.setText(nameWithQuantity);
            }
        }
    }
} 