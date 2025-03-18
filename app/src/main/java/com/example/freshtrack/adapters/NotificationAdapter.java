package com.example.freshtrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.freshtrack.R;
import com.example.freshtrack.models.UserNotification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private final List<UserNotification> notifications;
    private final SimpleDateFormat dateFormat;

    public NotificationAdapter(List<UserNotification> notifications) {
        this.notifications = notifications;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserNotification notification = notifications.get(position);
        holder.itemNameText.setText(notification.getItemName());
        holder.timeText.setText(dateFormat.format(new Date(notification.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView itemNameText;
        final TextView timeText;

        ViewHolder(View view) {
            super(view);
            itemNameText = view.findViewById(R.id.itemNameText);
            timeText = view.findViewById(R.id.timeText);
        }
    }
} 