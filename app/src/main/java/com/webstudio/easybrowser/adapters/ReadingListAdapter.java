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
import com.webstudio.easybrowser.models.ReadingListItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReadingListAdapter extends RecyclerView.Adapter<ReadingListAdapter.ViewHolder> {
    private List<ReadingListItem> items;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(ReadingListItem item);
        void onItemLongClick(ReadingListItem item);
    }

    public ReadingListAdapter(List<ReadingListItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<ReadingListItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reading_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView favicon;
        private final TextView title;
        private final TextView url;
        private final TextView savedDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.favicon);
            title = itemView.findViewById(R.id.title);
            url = itemView.findViewById(R.id.url);
            savedDate = itemView.findViewById(R.id.saved_date);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onItemClick(items.get(pos));
            });
            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(items.get(pos));
                    return true;
                }
                return false;
            });
        }

        void bind(ReadingListItem item) {
            title.setText(item.getTitle() != null ? item.getTitle() : item.getUrl());
            url.setText(item.getUrl());
            savedDate.setText(dateFormat.format(new Date(item.getSavedAt())));

            String faviconUrl = getFaviconUrl(item.getUrl());
            Glide.with(favicon)
                    .load(faviconUrl)
                    .placeholder(R.drawable.ic_history)
                    .error(R.drawable.ic_history)
                    .into(favicon);
        }

        private String getFaviconUrl(String url) {
            try {
                java.net.URL parsed = new java.net.URL(url);
                return parsed.getProtocol() + "://" + parsed.getHost() + "/favicon.ico";
            } catch (Exception e) { return ""; }
        }
    }
}
