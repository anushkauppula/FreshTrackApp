package com.example.freshtrack.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.example.freshtrack.R;
import com.example.freshtrack.adapters.FoodItemAdapter;
import com.example.freshtrack.FirebaseModel;
import com.example.freshtrack.models.FoodItem;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
    private final FoodItemAdapter adapter;
    private final Context context;
    private final FirebaseModel firebaseModel;
    private final ColorDrawable background;
    private final Paint textPaint;

    public SwipeToDeleteCallback(FoodItemAdapter adapter, Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.context = context;
        this.firebaseModel = new FirebaseModel();
        
        background = new ColorDrawable(Color.RED);
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        adapter.handleSwipeDeletion(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        // Draw the red background
        background.setBounds(
            itemView.getLeft(),
            itemView.getTop(),
            itemView.getRight(),
            itemView.getBottom()
        );
        background.draw(c);

        // Calculate position for "Delete" text
        float textY = itemView.getTop() + (itemHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        c.drawText("Delete", itemView.getWidth() / 2f, textY, textPaint);
    }
} 