package com.webstudio.easybrowser.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.databinding.ItemGroupTabCardBinding;
import com.webstudio.easybrowser.managers.TabThumbnailManager;
import com.webstudio.easybrowser.models.Tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupTabsAdapter extends ListAdapter<Tab, GroupTabsAdapter.ViewHolder>
        implements TabItemTouchHelperCallback.ReorderAdapter {
    private static final int MENU_MOVE = 1;
    private static final int MENU_PIN = 2;
    private static final int MENU_DUPLICATE = 3;
    private static final int MENU_SHARE = 4;
    private static final int MENU_CLOSE = 5;
    private static final int MENU_REMOVE_FROM_GROUP = 6;

    public interface Listener {
        void onTabClick(Tab tab);
        void onTabLongClick(Tab tab, View anchor, RecyclerView.ViewHolder viewHolder);
        void onCloseTab(Tab tab);
        void onMoveTab(Tab tab);
        void onRemoveFromGroup(Tab tab);
        void onPinTab(Tab tab);
        void onDuplicateTab(Tab tab);
        void onShareTab(Tab tab);
        void onTabsReordered(List<Tab> tabs);
        void onTabDragStarted(Tab tab);
        void onTabDragMoved(Tab tab);
        boolean onTabDragFinished(Tab tab, View itemView);
    }

    private final Listener listener;
    private final Set<String> selectedIds = new HashSet<>();
    private String currentTabId;
    private int groupColor;
    private boolean selectionMode;
    private Tab activeDragTab;

    public GroupTabsAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setCurrentTabId(String currentTabId) {
        this.currentTabId = currentTabId;
        notifyDataSetChanged();
    }

    public void setGroupColor(int groupColor) {
        this.groupColor = groupColor;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupTabCardBinding binding = ItemGroupTabCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (selectionMode || fromPosition == RecyclerView.NO_POSITION
                || toPosition == RecyclerView.NO_POSITION) {
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

    @Override
    public int getDragFlags() {
        return selectionMode ? 0 : (ItemTouchHelper.UP | ItemTouchHelper.DOWN
                | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public int getSwipeFlags() {
        return selectionMode ? 0 : (ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onDragStarted(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        activeDragTab = getItem(position);
        listener.onTabDragStarted(activeDragTab);
    }

    @Override
    public void onDragMoved(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        Tab tab = position == RecyclerView.NO_POSITION ? activeDragTab : getItem(position);
        listener.onTabDragMoved(tab);
    }

    @Override
    public boolean onDragFinished(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        Tab tab = position == RecyclerView.NO_POSITION ? activeDragTab : getItem(position);
        boolean handled = listener.onTabDragFinished(tab, viewHolder.itemView);
        activeDragTab = null;
        return handled;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= getCurrentList().size()) {
            return;
        }
        Tab tab = getItem(position);
        List<Tab> updated = new ArrayList<>(getCurrentList());
        updated.remove(position);
        submitList(updated);
        listener.onCloseTab(tab);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemGroupTabCardBinding binding;

        ViewHolder(ItemGroupTabCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Tab tab) {
            String title = tab.getTitle() != null && !tab.getTitle().trim().isEmpty()
                    ? tab.getTitle()
                    : itemView.getContext().getString(R.string.new_tab);
            binding.tabTitle.setText(title);
            TabThumbnailManager.loadThumbnail(binding.thumbnail, tab);
            loadFavicon(binding.favicon, tab);
            boolean selected = selectedIds.contains(tab.getId());
            int activeColor = groupColor != 0 ? groupColor : tab.getGroupColor();
            binding.pinnedIcon.setVisibility(tab.isPinned() ? View.VISIBLE : View.GONE);
            bindSelectionState(tab, selected, activeColor);
            binding.tabCard.setOnClickListener(v -> listener.onTabClick(tab));
            binding.tabCard.setOnLongClickListener(v -> {
                listener.onTabLongClick(tab, v, this);
                return true;
            });
            int closeIcon = tab.isLocked() ? R.drawable.ic_lock : R.drawable.ic_close;
            int closeDescription = tab.isLocked() ? R.string.lock_tab : R.string.close_tab;
            binding.closeButton.setImageResource(closeIcon);
            binding.closeButton.setContentDescription(itemView.getContext().getString(closeDescription));
            binding.closeButton.setOnClickListener(v -> listener.onCloseTab(tab));
            binding.overflowButton.setOnClickListener(v -> {
                if (selectionMode) {
                    listener.onTabClick(tab);
                } else {
                    listener.onCloseTab(tab);
                }
            });
        }

        private void bindSelectionState(Tab tab, boolean selected, int activeColor) {
            binding.selectionOverlay.setVisibility(View.GONE);
            int cardColor = ContextCompat.getColor(itemView.getContext(),
                    R.color.group_tab_card_background);
            int textColor = ContextCompat.getColor(itemView.getContext(), R.color.tab_manager_text);
            int secondaryTextColor = ContextCompat.getColor(itemView.getContext(),
                    R.color.tab_manager_text_secondary);
            int borderColor = ContextCompat.getColor(itemView.getContext(),
                    R.color.group_tab_card_stroke);
            binding.tabCard.setCardBackgroundColor(selectionMode && selected
                    ? activeColor
                    : cardColor);
            if (selectionMode) {
                binding.tabCard.setStrokeColor(selected ? activeColor : borderColor);
                binding.tabCard.setStrokeWidth(selected ? dp(2) : dp(1));
                binding.overflowButton.setImageResource(selected ? R.drawable.ic_check : 0);
                binding.overflowButton.setColorFilter(selected ? activeColor : Color.TRANSPARENT);
                binding.overflowButton.setBackground(createSelectionCircle(selected, textColor, borderColor));
                binding.overflowButton.setPadding(dp(8), dp(8), dp(8), dp(8));
                return;
            }
            binding.tabCard.setStrokeColor(tab.getId().equals(currentTabId)
                    ? activeColor
                    : borderColor);
            binding.tabCard.setStrokeWidth(tab.getId().equals(currentTabId) ? dp(2) : dp(1));
            binding.overflowButton.setImageResource(tab.isLocked() ? R.drawable.ic_lock : R.drawable.ic_close);
            binding.overflowButton.setContentDescription(itemView.getContext().getString(
                    tab.isLocked() ? R.string.lock_tab : R.string.close_tab));
            binding.overflowButton.setColorFilter(secondaryTextColor);
            binding.overflowButton.setBackground(null);
            binding.overflowButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        }

        private GradientDrawable createSelectionCircle(boolean selected, int textColor, int borderColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(selected ? textColor : Color.TRANSPARENT);
            drawable.setStroke(dp(2), selected ? textColor : borderColor);
            return drawable;
        }

        private void showOverflow(View anchor, Tab tab) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.getMenu().add(Menu.NONE, MENU_MOVE, Menu.NONE, R.string.move_to_group);
            menu.getMenu().add(Menu.NONE, MENU_REMOVE_FROM_GROUP, Menu.NONE, R.string.remove_from_group);
            menu.getMenu().add(Menu.NONE, MENU_PIN, Menu.NONE, R.string.pin);
            menu.getMenu().add(Menu.NONE, MENU_DUPLICATE, Menu.NONE, R.string.duplicate_tab);
            menu.getMenu().add(Menu.NONE, MENU_SHARE, Menu.NONE, R.string.share);
            menu.getMenu().add(Menu.NONE, MENU_CLOSE, Menu.NONE, R.string.close_tab);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == MENU_MOVE) {
                    listener.onMoveTab(tab);
                    return true;
                } else if (item.getItemId() == MENU_REMOVE_FROM_GROUP) {
                    listener.onRemoveFromGroup(tab);
                    return true;
                } else if (item.getItemId() == MENU_PIN) {
                    listener.onPinTab(tab);
                    return true;
                } else if (item.getItemId() == MENU_DUPLICATE) {
                    listener.onDuplicateTab(tab);
                    return true;
                } else if (item.getItemId() == MENU_SHARE) {
                    listener.onShareTab(tab);
                    return true;
                } else if (item.getItemId() == MENU_CLOSE) {
                    listener.onCloseTab(tab);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private void loadFavicon(android.widget.ImageView imageView, Tab tab) {
            if (tab.getFavicon() != null) {
                imageView.setImageBitmap(tab.getFavicon());
                return;
            }
            String faviconUri = tab.getFaviconUri();
            String fallback = !TextUtils.isEmpty(faviconUri) ? faviconUri : getFaviconUrl(tab.getUrl());
            if (TextUtils.isEmpty(fallback)) {
                imageView.setImageResource(R.mipmap.ic_launcher);
                return;
            }
            Glide.with(imageView)
                    .load(fallback)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        }

        private String getFaviconUrl(String rawUrl) {
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
                            && safeEquals(oldItem.getThumbnailPath(), newItem.getThumbnailPath())
                            && oldItem.getPosition() == newItem.getPosition()
                            && oldItem.isPinned() == newItem.isPinned()
                            && oldItem.getGroupColor() == newItem.getGroupColor()
                            && safeEquals(oldItem.getGroupId(), newItem.getGroupId());
                }
            };

    private static boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
