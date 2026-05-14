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
import com.webstudio.easybrowser.models.Bookmark;

import java.util.List;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {
    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_GRID = 1;

    private List<Bookmark> bookmarks;
    private OnBookmarkClickListener listener;
    private int viewType;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Bookmark bookmark);
        void onBookmarkLongClick(Bookmark bookmark);
    }

    public BookmarksAdapter(List<Bookmark> bookmarks, OnBookmarkClickListener listener) {
        this.bookmarks = bookmarks;
        this.listener = listener;
        this.viewType = VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        if (this.viewType == VIEW_TYPE_GRID) {
            layoutId = R.layout.item_bookmark_grid;
        } else {
            layoutId = R.layout.item_bookmark_list;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);
        holder.bind(bookmark);
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private ImageView favicon;
        private TextView title;
        private TextView url;

        BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.favicon);
            title = itemView.findViewById(R.id.title);
            url = itemView.findViewById(R.id.url);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onBookmarkClick(bookmarks.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onBookmarkLongClick(bookmarks.get(position));
                    return true;
                }
                return false;
            });
        }

        void bind(Bookmark bookmark) {
            title.setText(bookmark.getTitle());
            url.setText(bookmark.getUrl());

            // Load favicon
            if (bookmark.getFavicon() != null && !bookmark.getFavicon().isEmpty()) {
                Glide.with(favicon)
                        .load(bookmark.getFavicon())
                        .placeholder(R.drawable.ic_bookmarks)
                        .error(R.drawable.ic_bookmarks)
                        .into(favicon);
            } else {
                // Try to get favicon from URL
                String faviconUrl = getFaviconUrl(bookmark.getUrl());
                Glide.with(favicon)
                        .load(faviconUrl)
                        .placeholder(R.drawable.ic_bookmarks)
                        .error(R.drawable.ic_bookmarks)
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