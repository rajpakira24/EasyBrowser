package com.webstudio.easybrowser.adapters;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class TabItemTouchHelperCallback extends ItemTouchHelper.Callback {
    public interface ReorderAdapter {
        boolean onItemMove(int fromPosition, int toPosition);
        void onItemMoveFinished();

        default int getDragFlags() {
            return ItemTouchHelper.UP | ItemTouchHelper.DOWN
                    | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        }

        default int getSwipeFlags() {
            return 0;
        }

        default void onDragStarted(RecyclerView.ViewHolder viewHolder) {
        }

        default void onDragMoved(RecyclerView.ViewHolder viewHolder) {
        }

        default boolean onDragFinished(RecyclerView.ViewHolder viewHolder) {
            return false;
        }

        default void onItemSwiped(int position, int direction) {
        }

        default boolean isLongPressDragEnabled() {
            return true;
        }
    }

    private final ReorderAdapter adapter;
    private boolean dragging;
    private boolean movedDuringDrag;
    private boolean dragMovementNotified;

    public TabItemTouchHelperCallback(ReorderAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(adapter.getDragFlags(), adapter.getSwipeFlags());
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return adapter.isLongPressDragEnabled();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        boolean moved = adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        movedDuringDrag = movedDuringDrag || moved;
        return moved;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemSwiped(viewHolder.getAdapterPosition(), direction);
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            dragging = true;
            movedDuringDrag = false;
            dragMovementNotified = false;
            adapter.onDragStarted(viewHolder);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive) {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                && dragging
                && !dragMovementNotified
                && (Math.abs(dX) > 8f || Math.abs(dY) > 8f)) {
            dragMovementNotified = true;
            adapter.onDragMoved(viewHolder);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        boolean wasDragging = dragging;
        boolean handledDrop = wasDragging && adapter.onDragFinished(viewHolder);
        super.clearView(recyclerView, viewHolder);
        if (wasDragging && movedDuringDrag && !handledDrop) {
            adapter.onItemMoveFinished();
        }
        dragging = false;
        movedDuringDrag = false;
        dragMovementNotified = false;
    }
}
