package com.webstudio.easybrowser.adapters;

import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.databinding.ItemQuickTabChipBinding;
import com.webstudio.easybrowser.models.Tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuickTabStripAdapter extends ListAdapter<Tab, QuickTabStripAdapter.ViewHolder>
        implements TabItemTouchHelperCallback.ReorderAdapter {
    public interface Listener {
        void onTabClick(Tab tab);
        void onTabClose(Tab tab);
        void onTabLongClick(Tab tab, View anchor);
        void onTabsReordered(List<Tab> tabs);
    }

    private final Listener listener;
    private String currentTabId;

    public QuickTabStripAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submitTabs(java.util.List<Tab> tabs, String currentTabId) {
        this.currentTabId = currentTabId;
        submitList(tabs != null ? new ArrayList<>(tabs) : new ArrayList<>(),
                this::notifyDataSetChanged);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuickTabChipBinding binding = ItemQuickTabChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        List<Tab> updated = new ArrayList<>(getCurrentList());
        Collections.swap(updated, fromPosition, toPosition);
        submitList(updated);
        return true;
    }

    @Override
    public void onItemMoveFinished() {
        listener.onTabsReordered(new ArrayList<>(getCurrentList()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemQuickTabChipBinding binding;

        ViewHolder(ItemQuickTabChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Tab tab) {
            boolean active = tab.getId().equals(currentTabId);
            String label = tab.getTitle() != null && !tab.getTitle().trim().isEmpty()
                    ? tab.getTitle()
                    : itemView.getContext().getString(tab.isPrivate()
                    ? R.string.private_tab
                    : R.string.new_tab);
            loadFavicon(binding.chipFavicon, tab);
            int groupColor = tab.isPrivate()
                    ? ContextCompat.getColor(itemView.getContext(), R.color.tab_manager_text_secondary)
                    : tab.getGroupColor();
            int highlightColor = groupColor != 0
                    ? groupColor
                    : ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary);
            binding.chipCard.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                    active ? R.color.colorSurface : R.color.search_bar_background));
            binding.chipCard.setCardElevation(0);
            binding.chipCard.setStrokeColor(active ? highlightColor : Color.TRANSPARENT);
            binding.chipCard.setStrokeWidth(active ? dp(3) : 0);
            binding.chipCard.setContentDescription(active
                    ? itemView.getContext().getString(R.string.active_tab)
                    : label);
            binding.chipClose.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.chipClose.setOnClickListener(active ? v -> listener.onTabClose(tab) : null);
            binding.chipClose.bringToFront();
            binding.chipCard.setOnClickListener(v -> listener.onTabClick(tab));
            binding.chipCard.setOnLongClickListener(v -> {
                listener.onTabLongClick(tab, v);
                return true;
            });
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private void loadFavicon(ImageView imageView, Tab tab) {
            Glide.with(imageView).clear(imageView);
            if (tab.getFavicon() != null) {
                imageView.setImageBitmap(tab.getFavicon());
                return;
            }
            String faviconUri = tab.getFaviconUri();
            String fallback = !TextUtils.isEmpty(faviconUri) ? faviconUri : getFaviconUrl(tab.getUrl());
            if (TextUtils.isEmpty(fallback)) {
                imageView.setImageResource(R.drawable.ic_globe);
                return;
            }
            Glide.with(imageView)
                    .load(fallback)
                    .placeholder(R.drawable.ic_globe)
                    .error(R.drawable.ic_globe)
                    .into(imageView);
        }
    }

    private static String getFaviconUrl(String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return null;
        }
        Uri uri = Uri.parse(rawUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(host)
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return null;
        }
        return scheme + "://" + host + "/favicon.ico";
    }

    private static final DiffUtil.ItemCallback<Tab> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Tab>() {
                @Override
                public boolean areItemsTheSame(@NonNull Tab oldItem, @NonNull Tab newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Tab oldItem, @NonNull Tab newItem) {
                    return safeEquals(oldItem.getTitle(), newItem.getTitle())
                            && safeEquals(oldItem.getUrl(), newItem.getUrl())
                            && safeEquals(oldItem.getFaviconUri(), newItem.getFaviconUri())
                            && oldItem.isPrivate() == newItem.isPrivate()
                            && oldItem.isPinned() == newItem.isPinned()
                            && oldItem.getGroupColor() == newItem.getGroupColor()
                            && oldItem.getPosition() == newItem.getPosition();
                }
            };

    private static boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
