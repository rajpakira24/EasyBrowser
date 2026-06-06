package com.webstudio.easybrowser.adapters;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.utils.UrlUtils;
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

            Glide.with(favicon).clear(favicon);
            String faviconUrl = UrlUtils.getFaviconUrl(item.getUrl());
            String fallbackUrl = chooseFallbackFaviconUrl(item.getFaviconUrl(),
                    UrlUtils.getDirectFaviconUrl(item.getUrl()),
                    faviconUrl);
            if (TextUtils.isEmpty(faviconUrl)) {
                faviconUrl = fallbackUrl;
                fallbackUrl = null;
            }
            if (!TextUtils.isEmpty(faviconUrl)) {
                RequestBuilder<Drawable> request = Glide.with(favicon)
                        .load(faviconUrl)
                        .placeholder(R.drawable.ic_globe);
                if (!TextUtils.isEmpty(fallbackUrl)) {
                    request.error(Glide.with(favicon)
                            .load(fallbackUrl)
                            .placeholder(R.drawable.ic_globe)
                            .error(R.drawable.ic_globe));
                } else {
                    request.error(R.drawable.ic_globe);
                }
                request.into(favicon);
            } else {
                favicon.setImageResource(R.drawable.ic_globe);
            }
        }

        private String chooseFallbackFaviconUrl(String savedUrl, String directUrl, String primaryUrl) {
            if (!TextUtils.isEmpty(savedUrl) && !savedUrl.equals(primaryUrl)) {
                return savedUrl;
            }
            if (!TextUtils.isEmpty(directUrl) && !directUrl.equals(primaryUrl)) {
                return directUrl;
            }
            return null;
        }
    }
}
