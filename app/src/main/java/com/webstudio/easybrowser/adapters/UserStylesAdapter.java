package com.webstudio.easybrowser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.webstudio.easybrowser.R;

import java.util.List;

public class UserStylesAdapter extends RecyclerView.Adapter<UserStylesAdapter.ViewHolder> {

    public static class UserStyleEntry {
        public String hostname;
        public String css;
        public boolean enabled;

        public UserStyleEntry(String hostname, String css, boolean enabled) {
            this.hostname = hostname;
            this.css = css;
            this.enabled = enabled;
        }
    }

    public interface OnItemInteractionListener {
        void onEnabledChanged(UserStyleEntry entry, boolean enabled);
        void onItemLongClick(UserStyleEntry entry);
    }

    private List<UserStyleEntry> entries;
    private final OnItemInteractionListener listener;

    public UserStylesAdapter(List<UserStyleEntry> entries, OnItemInteractionListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    public void setEntries(List<UserStyleEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_style, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() { return entries.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView hostname;
        private final TextView cssPreview;
        private final SwitchMaterial switchEnabled;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            hostname = itemView.findViewById(R.id.hostname);
            cssPreview = itemView.findViewById(R.id.css_preview);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(entries.get(pos));
                    return true;
                }
                return false;
            });
        }

        void bind(UserStyleEntry entry) {
            hostname.setText(entry.hostname);
            cssPreview.setText(entry.css);
            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(entry.enabled);
            switchEnabled.setOnCheckedChangeListener((btn, checked) -> listener.onEnabledChanged(entry, checked));
        }
    }
}
