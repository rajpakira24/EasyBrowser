package com.webstudio.easybrowser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.Suggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * URL-bar suggestion rows: leading type icon (bookmark/history/search), page title, and the URL
 * as a single ellipsized secondary line — search suggestions show just the query text.
 */
public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    private List<Suggestion> suggestions = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public SuggestionsAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Suggestion suggestion = suggestions.get(position);
        holder.icon.setImageResource(iconFor(suggestion.getType()));
        holder.title.setText(suggestion.getTitle());
        String url = suggestion.getUrl();
        if (url != null && !url.trim().isEmpty()) {
            holder.url.setText(url);
            holder.url.setVisibility(View.VISIBLE);
        } else {
            holder.url.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v ->
                listener.onSuggestionClick(suggestion.getNavigationText()));
    }

    private int iconFor(Suggestion.Type type) {
        switch (type) {
            case BOOKMARK:
                return R.drawable.ic_bookmarks;
            case HISTORY:
                return R.drawable.ic_history;
            default:
                return R.drawable.ic_search;
        }
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView url;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.suggestion_icon);
            title = itemView.findViewById(R.id.suggestion_title);
            url = itemView.findViewById(R.id.suggestion_url);
        }
    }
}
