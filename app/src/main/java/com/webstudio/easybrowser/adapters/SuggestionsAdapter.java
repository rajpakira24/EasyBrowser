package com.webstudio.easybrowser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    private List<String> suggestions = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public SuggestionsAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(dp(parent, 16), dp(parent, 12), dp(parent, 16), dp(parent, 12));
        tv.setTextSize(15);
        tv.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT));
        tv.setBackground(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 0,
                parent.getResources().getDisplayMetrics()) >= 0
                ? getSelectableBackground(parent) : null);
        return new ViewHolder(tv);
    }

    private android.graphics.drawable.Drawable getSelectableBackground(ViewGroup parent) {
        android.util.TypedValue value = new android.util.TypedValue();
        parent.getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, value, true);
        return parent.getContext().getDrawable(value.resourceId);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        ((TextView) holder.itemView).setText(suggestion);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    private int dp(ViewGroup parent, int value) {
        return Math.round(value * parent.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
