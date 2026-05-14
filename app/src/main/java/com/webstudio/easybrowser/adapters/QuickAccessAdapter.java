package com.webstudio.easybrowser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.QuickAccessItem;
import java.util.List;

public class QuickAccessAdapter extends RecyclerView.Adapter<QuickAccessAdapter.ViewHolder> {
    private List<QuickAccessItem> items;
    private OnQuickAccessClickListener listener;

    public interface OnQuickAccessClickListener {
        void onQuickAccessClick(QuickAccessItem item);
        void onQuickAccessLongClick(QuickAccessItem item, View view);
    }

    public QuickAccessAdapter(List<QuickAccessItem> items, OnQuickAccessClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_access, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickAccessItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<QuickAccessItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView favicon;
        private TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.favicon);
            title = itemView.findViewById(R.id.title);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onQuickAccessClick(items.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onQuickAccessLongClick(items.get(position), v);
                    return true;
                }
                return false;
            });
        }

        void bind(QuickAccessItem item) {
            title.setText(item.getTitle());

            if (item.getFaviconUrl() != null) {
                Glide.with(favicon)
                        .load(item.getFaviconUrl())
                        .placeholder(R.drawable.ic_globe)
                        .error(R.drawable.ic_globe)
                        .into(favicon);
            } else {
                favicon.setImageResource(R.drawable.ic_globe);
            }
        }
    }
}