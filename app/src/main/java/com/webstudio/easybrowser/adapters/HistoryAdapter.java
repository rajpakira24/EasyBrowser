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
import com.webstudio.easybrowser.models.HistoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Item> historyItems;
    private OnHistoryClickListener listener;
    private SimpleDateFormat timeFormat;

    public static class Item {
        private final String headerTitle;
        private final HistoryItem historyItem;

        private Item(String headerTitle, HistoryItem historyItem) {
            this.headerTitle = headerTitle;
            this.historyItem = historyItem;
        }

        public static Item header(String title) {
            return new Item(title, null);
        }

        public static Item history(HistoryItem item) {
            return new Item(null, item);
        }

        public boolean isHeader() {
            return historyItem == null;
        }

        public HistoryItem getHistoryItem() {
            return historyItem;
        }
    }

    public interface OnHistoryClickListener {
        void onHistoryClick(HistoryItem item);
        void onHistoryLongClick(HistoryItem item);
    }

    public HistoryAdapter(List<Item> historyItems, OnHistoryClickListener listener) {
        this.historyItems = historyItems;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return historyItems.get(position).isHeader() ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = historyItems.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerTitle);
        } else if (holder instanceof HistoryViewHolder) {
            ((HistoryViewHolder) holder).bind(item.historyItem);
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView headerText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_text);
        }

        void bind(String title) {
            headerText.setText(title);
        }
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView favicon;
        private TextView title;
        private TextView url;
        private TextView visitTime;
        private TextView visitCount;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.favicon);
            title = itemView.findViewById(R.id.title);
            url = itemView.findViewById(R.id.url);
            visitTime = itemView.findViewById(R.id.visit_time);
            visitCount = itemView.findViewById(R.id.visit_count);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onHistoryClick(historyItems.get(position).historyItem);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onHistoryLongClick(historyItems.get(position).historyItem);
                    return true;
                }
                return false;
            });
        }

        void bind(HistoryItem item) {
            title.setText(item.getTitle());
            url.setText(item.getUrl());
            visitTime.setText(timeFormat.format(new Date(item.getVisitTime())));

            if (item.getVisitCount() > 1) {
                visitCount.setVisibility(View.VISIBLE);
                visitCount.setText(String.valueOf(item.getVisitCount()));
            } else {
                visitCount.setVisibility(View.GONE);
            }

            // Load favicon
            if (item.getFavicon() != null && !item.getFavicon().isEmpty()) {
                Glide.with(favicon)
                        .load(item.getFavicon())
                        .placeholder(R.drawable.ic_history)
                        .error(R.drawable.ic_history)
                        .into(favicon);
            } else {
                // Try to get favicon from URL
                String faviconUrl = getFaviconUrl(item.getUrl());
                Glide.with(favicon)
                        .load(faviconUrl)
                        .placeholder(R.drawable.ic_history)
                        .error(R.drawable.ic_history)
                        .into(favicon);
            }
        }

        private String getFaviconUrl(String url) {
            try {
                java.net.URL parsedUrl = new java.net.URL(url);
                String domain = parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
                return domain + "/favicon.ico";
            } catch (Exception e) {
                return "";
            }
        }
    }
}
