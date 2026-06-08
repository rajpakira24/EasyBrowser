package com.webstudio.easybrowser.adapters;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.databinding.ItemTabGroupCardBinding;
import com.webstudio.easybrowser.managers.TabThumbnailManager;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabGroupAdapter extends RecyclerView.Adapter<TabGroupAdapter.ViewHolder> {
    private static final String GROUP_TRANSITION_PREFIX = "group_card_transition_";
    private static final int MENU_OPEN = 1;
    private static final int MENU_RENAME = 2;
    private static final int MENU_DELETE = 3;
    private static final int MENU_COLOR = 4;
    private static final int MENU_CLOSE_GROUP = 5;
    private static final int MENU_TOGGLE_COLLAPSE = 6;

    public interface Listener {
        void onOpenGroup(TabGroup group, View sourceView);
        void onOpenTab(Tab tab);
        void onRenameGroup(TabGroup group);
        void onDeleteGroup(TabGroup group);
        void onChangeGroupColor(TabGroup group);
        void onMoveTabToGroup(Tab tab, TabGroup targetGroup);
        void onCreateGroupFromTabs(Tab firstTab, Tab secondTab);
        void onCloseGroup(TabGroup group);
        void onToggleGroupCollapsed(TabGroup group);
        void onCloseTab(Tab tab);
        void onGroupLongClick(TabGroup group);
        void onTabLongClick(Tab tab, View anchor);
        void onAddTabToGroup(Tab tab);
        void onRemoveTabFromGroup(Tab tab);
        void onBookmarkTab(Tab tab);
        void onShareTab(Tab tab);
        void onDuplicateTab(Tab tab);
        void onPinTab(Tab tab);
    }

    private static class DisplayItem {
        final TabGroup group;
        final Tab tab;

        DisplayItem(TabGroup group, Tab tab) {
            this.group = group;
            this.tab = tab;
        }

        boolean isGroup() {
            return group != null;
        }

        String stableId() {
            return isGroup() ? "group:" + group.getGroupId() : "tab:" + tab.getId();
        }
    }

    private final Listener listener;
    private final List<DisplayItem> items = new ArrayList<>();
    private final Set<String> selectedGroupIds = new HashSet<>();
    private final Set<String> selectedTabIds = new HashSet<>();
    private final Set<String> collapsedGroupIds = new HashSet<>();
    private boolean gridMode = true;
    private boolean showGroupCloseButton;

    public TabGroupAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submitOverview(List<TabGroup> groups, List<Tab> standaloneTabs) {
        List<DisplayItem> newItems = new ArrayList<>();
        if (standaloneTabs != null) {
            for (Tab tab : standaloneTabs) {
                newItems.add(new DisplayItem(null, tab));
            }
        }
        if (groups != null) {
            for (TabGroup group : groups) {
                if (group.getTabCount() >= 2) {
                    newItems.add(new DisplayItem(group, null));
                }
            }
        }
        Collections.sort(newItems, (first, second) -> {
            if (itemPinned(first) != itemPinned(second)) {
                return itemPinned(first) ? -1 : 1;
            }
            int positionCompare = Integer.compare(itemPosition(first), itemPosition(second));
            if (positionCompare != 0) {
                return positionCompare;
            }
            return first.stableId().compareTo(second.stableId());
        });
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    public void setGridMode(boolean gridMode) {
        this.gridMode = gridMode;
        notifyDataSetChanged();
    }

    public void setShowGroupCloseButton(boolean showGroupCloseButton) {
        this.showGroupCloseButton = showGroupCloseButton;
        notifyDataSetChanged();
    }

    public void setCollapsedGroupIds(Set<String> groupIds) {
        Set<String> nextIds = new HashSet<>();
        if (groupIds != null) {
            nextIds.addAll(groupIds);
        }
        if (collapsedGroupIds.equals(nextIds)) {
            return;
        }
        collapsedGroupIds.clear();
        collapsedGroupIds.addAll(nextIds);
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<String> groupIds, Set<String> tabIds) {
        selectedGroupIds.clear();
        if (groupIds != null) {
            selectedGroupIds.addAll(groupIds);
        }
        selectedTabIds.clear();
        if (tabIds != null) {
            selectedTabIds.addAll(tabIds);
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).stableId().hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTabGroupCardBinding binding = ItemTabGroupCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public TabGroup getGroupAt(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position).group;
    }

    public Tab getTabAt(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position).tab;
    }

    public int findPositionForTabId(String tabId) {
        if (tabId == null || tabId.trim().isEmpty()) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < items.size(); i++) {
            DisplayItem item = items.get(i);
            if (!item.isGroup()) {
                if (item.tab != null && tabId.equals(item.tab.getId())) {
                    return i;
                }
                continue;
            }
            if (item.group == null || item.group.getTabs() == null) {
                continue;
            }
            for (Tab tab : item.group.getTabs()) {
                if (tab != null && tabId.equals(tab.getId())) {
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0
                || fromPosition >= items.size() || toPosition >= items.size()
                || fromPosition == toPosition) {
            return false;
        }
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public List<Tab> getOrderedTabsForPersistence() {
        List<Tab> orderedTabs = new ArrayList<>();
        for (DisplayItem item : items) {
            if (item.isGroup()) {
                orderedTabs.addAll(item.group.getTabs());
            } else if (item.tab != null) {
                orderedTabs.add(item.tab);
            }
        }
        return orderedTabs;
    }

    private static int itemPosition(DisplayItem item) {
        if (item == null) {
            return Integer.MAX_VALUE;
        }
        if (!item.isGroup()) {
            return item.tab != null ? item.tab.getPosition() : Integer.MAX_VALUE;
        }
        int minPosition = Integer.MAX_VALUE;
        if (item.group.getTabs() != null) {
            for (Tab tab : item.group.getTabs()) {
                minPosition = Math.min(minPosition, tab.getPosition());
            }
        }
        return minPosition;
    }

    private static boolean itemPinned(DisplayItem item) {
        if (item == null) {
            return false;
        }
        if (!item.isGroup()) {
            return item.tab != null && item.tab.isPinned();
        }
        if (item.group != null && item.group.getTabs() != null) {
            for (Tab tab : item.group.getTabs()) {
                if (tab != null && tab.isPinned()) {
                    return true;
                }
            }
        }
        return false;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTabGroupCardBinding binding;

        ViewHolder(ItemTabGroupCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DisplayItem item) {
            if (item.isGroup()) {
                bindGroup(item.group);
            } else {
                bindTab(item.tab);
            }
        }

        private void bindGroup(TabGroup group) {
            resetCardTransform();
            ViewCompat.setTransitionName(binding.groupCard, GROUP_TRANSITION_PREFIX + group.getGroupId());
            int count = group.getTabCount();
            binding.headerFavicon.setVisibility(View.GONE);
            binding.groupColorIndicator.setVisibility(View.VISIBLE);
            binding.groupColorBorder.setVisibility(View.GONE);
            String defaultGroupTitle = itemView.getContext().getString(R.string.tab_group);
            String groupTitle = !TextUtils.isEmpty(group.getGroupName())
                    ? group.getGroupName()
                    : defaultGroupTitle;
            String countText = itemView.getResources()
                    .getQuantityString(R.plurals.tab_count, count, count);
            String titleSummary = buildTitleSummary(group.getTabs());
            boolean hasCustomTitle = !TextUtils.isEmpty(group.getGroupName())
                    && !defaultGroupTitle.equals(group.getGroupName());
            boolean collapsed = collapsedGroupIds.contains(group.getGroupId());
            binding.tabCount.setText(hasCustomTitle ? groupTitle + " - " + countText : countText);
            binding.groupCard.setContentDescription(TextUtils.isEmpty(titleSummary)
                    ? groupTitle + ", " + countText
                    : groupTitle + ", " + countText + ", " + titleSummary);
            tintCircle(binding.groupColorIndicator, group.getGroupColor());
            binding.groupCard.setCardBackgroundColor(
                    cardBackgroundForGroup(itemView.getContext(), group.getGroupColor()));
            applyGroupTextColors();
            if (collapsed) {
                binding.moreCount.setVisibility(View.GONE);
            } else {
                configureGroupCollage(count, group.getGroupColor());
                bindPreview(binding.preview1, binding.favicon1, binding.title1, group.getTabs(), 0);
                bindPreview(binding.preview2, binding.favicon2, binding.title2, group.getTabs(), 1);
                bindPreview(binding.preview3, binding.favicon3, binding.title3, group.getTabs(), 2);
                if (count == 3) {
                    clearEmptyImageOnlyPreview(binding.preview4);
                } else {
                    bindImageOnlyPreview(binding.preview4, group.getTabs(), 3);
                }
                int extra = count - 4;
                binding.moreCount.setVisibility(extra > 0 ? View.VISIBLE : View.GONE);
                if (extra > 0) {
                    binding.moreCount.setText("+" + extra);
                }
            }
            applyMode(selectedGroupIds.contains(group.getGroupId()), group.getGroupColor(), collapsed);
            bindGroupOpenTargets(group);
            bindGroupLongPressTargets(group);
            binding.pinnedIcon.setVisibility(View.GONE);
            binding.groupOverflow.setVisibility(showGroupCloseButton ? View.GONE : View.VISIBLE);
            binding.groupOverflow.setOnClickListener(showGroupCloseButton ? null : v -> showOverflow(v, group));
            binding.cardClose.setVisibility(showGroupCloseButton ? View.VISIBLE : View.GONE);
            binding.cardClose.setColorFilter(ContextCompat.getColor(
                    itemView.getContext(), R.color.tab_manager_text_secondary));
            binding.cardClose.setContentDescription(itemView.getContext().getString(R.string.close_group));
            binding.cardClose.setOnClickListener(showGroupCloseButton
                    ? v -> listener.onCloseGroup(group)
                    : null);
            binding.previewGrid.setOnLongClickListener(null);
            binding.groupCard.setOnDragListener((v, event) -> handleDragEvent(event, group));
        }

        private void bindTab(Tab tab) {
            resetCardTransform();
            ViewCompat.setTransitionName(binding.groupCard, null);
            binding.headerFavicon.setVisibility(View.VISIBLE);
            loadFavicon(binding.headerFavicon, tab);
            binding.groupColorIndicator.setVisibility(View.GONE);
            binding.groupColorBorder.setVisibility(View.GONE);
            String host = UrlUtils.getDisplayHost(tab.getUrl());
            String title = !TextUtils.isEmpty(getDisplayTitle(tab))
                    ? getDisplayTitle(tab)
                    : itemView.getContext().getString(R.string.new_tab);
            binding.tabCount.setText(title);
            binding.tabCount.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.tab_manager_text));
            binding.groupCard.setContentDescription(!TextUtils.isEmpty(host)
                    ? title + ", " + host
                    : title);
            binding.groupCard.setCardBackgroundColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.tab_manager_card));
            configureStandalonePreview();
            bindStandalonePreview(tab);
            binding.moreCount.setVisibility(View.GONE);
            applyMode(selectedTabIds.contains(tab.getId()), ContextCompat.getColor(
                    itemView.getContext(), R.color.tab_manager_text_secondary), false);
            bindTabOpenTargets(tab);
            bindTabLongPressTargets(tab);
            binding.pinnedIcon.setVisibility(tab.isPinned() ? View.VISIBLE : View.GONE);
            binding.groupOverflow.setVisibility(View.GONE);
            binding.groupOverflow.setOnClickListener(null);
            binding.cardClose.setVisibility(View.VISIBLE);
            binding.cardClose.setColorFilter(ContextCompat.getColor(
                    itemView.getContext(), R.color.tab_manager_text_secondary));
            binding.cardClose.setContentDescription(itemView.getContext().getString(R.string.close_tab));
            binding.cardClose.setOnClickListener(v -> listener.onCloseTab(tab));
            binding.groupCard.setOnDragListener((v, event) -> handleTabDropEvent(event, tab));
        }

        private void bindGroupOpenTargets(TabGroup group) {
            View.OnClickListener openListener =
                    v -> listener.onOpenGroup(group, binding.groupCard);
            bindOpenTargets(openListener);
        }

        private void bindTabOpenTargets(Tab tab) {
            View.OnClickListener openListener = v -> listener.onOpenTab(tab);
            bindOpenTargets(openListener);
        }

        private void bindOpenTargets(View.OnClickListener openListener) {
            binding.groupCard.setOnClickListener(openListener);
            binding.groupCardContent.setOnClickListener(openListener);
            binding.groupInfoRow.setOnClickListener(openListener);
            binding.groupTextColumn.setOnClickListener(openListener);
            binding.tabCount.setOnClickListener(openListener);
            binding.previewGrid.setOnClickListener(openListener);
            binding.previewCell1.setOnClickListener(openListener);
            binding.previewCell2.setOnClickListener(openListener);
            binding.previewCell3.setOnClickListener(openListener);
            binding.previewCell4.setOnClickListener(openListener);
            binding.preview1.setOnClickListener(openListener);
            binding.preview2.setOnClickListener(openListener);
            binding.preview3.setOnClickListener(openListener);
            binding.preview4.setOnClickListener(openListener);
            binding.headerFavicon.setOnClickListener(openListener);
            binding.groupColorIndicator.setOnClickListener(openListener);
        }

        private void bindGroupLongPressTargets(TabGroup group) {
            View.OnLongClickListener longClickListener = v -> {
                listener.onGroupLongClick(group);
                return true;
            };
            binding.groupCard.setOnLongClickListener(longClickListener);
            binding.groupCardContent.setOnLongClickListener(longClickListener);
            binding.groupInfoRow.setOnLongClickListener(longClickListener);
            binding.groupTextColumn.setOnLongClickListener(longClickListener);
            binding.tabCount.setOnLongClickListener(longClickListener);
            binding.groupColorIndicator.setOnLongClickListener(longClickListener);
            binding.headerFavicon.setOnLongClickListener(null);
            binding.previewCell1.setOnLongClickListener(null);
            binding.previewCell2.setOnLongClickListener(null);
            binding.previewCell3.setOnLongClickListener(null);
            binding.previewCell4.setOnLongClickListener(null);
        }

        private void bindTabLongPressTargets(Tab tab) {
            View.OnLongClickListener longClickListener = v -> {
                listener.onTabLongClick(tab, v);
                return true;
            };
            binding.groupCard.setOnLongClickListener(longClickListener);
            binding.groupCardContent.setOnLongClickListener(longClickListener);
            binding.groupInfoRow.setOnLongClickListener(longClickListener);
            binding.groupTextColumn.setOnLongClickListener(longClickListener);
            binding.tabCount.setOnLongClickListener(longClickListener);
            binding.headerFavicon.setOnLongClickListener(longClickListener);
            binding.previewGrid.setOnLongClickListener(longClickListener);
            binding.previewCell1.setOnLongClickListener(longClickListener);
            binding.previewCell2.setOnLongClickListener(longClickListener);
            binding.previewCell3.setOnLongClickListener(longClickListener);
            binding.previewCell4.setOnLongClickListener(longClickListener);
            binding.preview1.setOnLongClickListener(longClickListener);
            binding.preview2.setOnLongClickListener(longClickListener);
            binding.preview3.setOnLongClickListener(longClickListener);
            binding.preview4.setOnLongClickListener(longClickListener);
        }

        private void bindPreview(ImageView imageView, ImageView faviconView,
                                 TextView titleView, List<Tab> tabs, int index) {
            if (tabs == null || index >= tabs.size()) {
                clearPreview(imageView, faviconView, titleView);
                return;
            }
            bindPreview(imageView, faviconView, titleView, tabs.get(index));
        }

        private void bindPreview(ImageView imageView, ImageView faviconView,
                                 TextView titleView, Tab tab) {
            if (tab == null) {
                clearPreview(imageView, faviconView, titleView);
                return;
            }
            imageView.setVisibility(View.VISIBLE);
            TabThumbnailManager.loadThumbnail(imageView, tab);
            loadFavicon(faviconView, tab);
            titleView.setText("");
            titleView.setVisibility(View.GONE);
            imageView.setOnLongClickListener(v -> startTabDrag(v, tab));
        }

        private void clearPreview(ImageView imageView, ImageView faviconView, TextView titleView) {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.VISIBLE);
            imageView.setOnLongClickListener(null);
            faviconView.setImageDrawable(null);
            faviconView.setVisibility(View.GONE);
            titleView.setText("");
            titleView.setVisibility(View.GONE);
        }

        private void bindStandalonePreview(Tab tab) {
            if (tab == null) {
                clearPreview(binding.preview1, binding.favicon1, binding.title1);
                return;
            }
            binding.preview1.setVisibility(View.VISIBLE);
            TabThumbnailManager.loadThumbnail(binding.preview1, tab);
            binding.preview1.setOnLongClickListener(v -> startTabDrag(v, tab));
            binding.favicon1.setImageDrawable(null);
            binding.favicon1.setVisibility(View.GONE);
            binding.title1.setText("");
            binding.title1.setVisibility(View.GONE);
        }

        private void bindImageOnlyPreview(ImageView imageView, List<Tab> tabs, int index) {
            if (tabs == null || index >= tabs.size()) {
                clearImageOnlyPreview(imageView);
                return;
            }
            imageView.setVisibility(View.VISIBLE);
            Tab tab = tabs.get(index);
            TabThumbnailManager.loadThumbnail(imageView, tab);
            imageView.setOnLongClickListener(v -> startTabDrag(v, tab));
        }

        private void clearImageOnlyPreview(ImageView imageView) {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.VISIBLE);
            imageView.setOnLongClickListener(null);
        }

        private void clearEmptyImageOnlyPreview(ImageView imageView) {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
            imageView.setOnLongClickListener(null);
        }

        private void configureGroupCollage(int count, int groupColor) {
            int cellColor = previewCellBackgroundForGroup(itemView.getContext(), groupColor);
            setCellBackground(binding.previewCell1, cellColor, false, false);
            setCellBackground(binding.previewCell2, cellColor, false, false);
            setCellBackground(binding.previewCell3, cellColor, true, false);
            setCellBackground(binding.previewCell4, cellColor, false, true);
            binding.previewCell1.setVisibility(View.VISIBLE);
            binding.previewCell2.setVisibility(View.VISIBLE);
            binding.previewCell3.setVisibility(count >= 3 ? View.VISIBLE : View.GONE);
            binding.previewCell4.setVisibility(count >= 3 ? View.VISIBLE : View.GONE);
            binding.moreCount.setVisibility(View.GONE);
            if (count == 2) {
                setCellBackground(binding.previewCell1, cellColor, true, false);
                setCellBackground(binding.previewCell2, cellColor, false, true);
                setCellSpec(binding.previewCell1, 0, 2, 0, 1);
                setCellSpec(binding.previewCell2, 0, 2, 1, 1);
                clearPreview(binding.preview3, binding.favicon3, binding.title3);
                clearImageOnlyPreview(binding.preview4);
            } else if (count == 3) {
                setCellSpec(binding.previewCell1, 0, 1, 0, 1);
                setCellSpec(binding.previewCell2, 0, 1, 1, 1);
                setCellSpec(binding.previewCell3, 1, 1, 0, 1);
                setCellSpec(binding.previewCell4, 1, 1, 1, 1);
                clearEmptyImageOnlyPreview(binding.preview4);
            } else {
                setCellSpec(binding.previewCell1, 0, 1, 0, 1);
                setCellSpec(binding.previewCell2, 0, 1, 1, 1);
                setCellSpec(binding.previewCell3, 1, 1, 0, 1);
                setCellSpec(binding.previewCell4, 1, 1, 1, 1);
            }
        }

        private void configureStandalonePreview() {
            binding.previewCell1.setBackgroundResource(R.drawable.bg_tab_group_preview_cell_bottom_both);
            binding.previewCell1.setVisibility(View.VISIBLE);
            binding.previewCell2.setVisibility(View.GONE);
            binding.previewCell3.setVisibility(View.GONE);
            binding.previewCell4.setVisibility(View.GONE);
            setCellSpec(binding.previewCell1, 0, 2, 0, 2);
            clearPreview(binding.preview2, binding.favicon2, binding.title2);
            clearPreview(binding.preview3, binding.favicon3, binding.title3);
            clearImageOnlyPreview(binding.preview4);
        }

        private void setCellSpec(FrameLayout cell, int row, int rowSpan,
                                 int column, int columnSpan) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row, rowSpan, 1f),
                    GridLayout.spec(column, columnSpan, 1f));
            params.width = 0;
            params.height = 0;
            int gap = dp(2);
            params.setMargins(gap, gap, gap, gap);
            cell.setLayoutParams(params);
        }

        private void applyMode(boolean selected, int color, boolean collapsed) {
            ViewGroup.LayoutParams previewParams = binding.previewGrid.getLayoutParams();
            previewParams.height = dp(collapsed ? 0 : (gridMode ? 190 : 142));
            binding.previewGrid.setLayoutParams(previewParams);
            binding.previewGrid.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            binding.groupCard.setStrokeColor(selected ? color : Color.TRANSPARENT);
            binding.groupCard.setStrokeWidth(selected ? dp(3) : 0);
        }

        private void applyGroupTextColors() {
            int textColor = ContextCompat.getColor(itemView.getContext(), R.color.tab_manager_text);
            binding.tabCount.setTextColor(textColor);
            binding.groupOverflow.setColorFilter(textColor);
            binding.moreCount.setTextColor(textColor);
        }

        private void setCellBackground(FrameLayout cell, int color,
                                       boolean largeBottomLeft, boolean largeBottomRight) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(color);
            float standard = dp(10);
            float large = dp(18);
            drawable.setCornerRadii(new float[] {
                    standard, standard,
                    standard, standard,
                    largeBottomRight ? large : standard, largeBottomRight ? large : standard,
                    largeBottomLeft ? large : standard, largeBottomLeft ? large : standard
            });
            cell.setBackground(drawable);
        }

        private boolean handleDragEvent(DragEvent event, TabGroup targetGroup) {
            Object state = event.getLocalState();
            if (!(state instanceof Tab)) {
                return false;
            }
            Tab draggedTab = (Tab) state;
            boolean allowed = targetGroup.isPrivate() == draggedTab.isPrivate()
                    && !targetGroup.getGroupId().equals(draggedTab.getGroupId());
            if (event.getAction() == DragEvent.ACTION_DROP) {
                setDropTargetHighlighted(false, selectedGroupIds.contains(targetGroup.getGroupId()),
                        targetGroup.getGroupColor());
                if (allowed) {
                    listener.onMoveTabToGroup(draggedTab, targetGroup);
                }
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                setDropTargetHighlighted(allowed, selectedGroupIds.contains(targetGroup.getGroupId()),
                        targetGroup.getGroupColor());
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED
                    || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                setDropTargetHighlighted(false, selectedGroupIds.contains(targetGroup.getGroupId()),
                        targetGroup.getGroupColor());
                return true;
            }
            return event.getAction() == DragEvent.ACTION_DRAG_STARTED
                    || event.getAction() == DragEvent.ACTION_DRAG_LOCATION;
        }

        private boolean handleTabDropEvent(DragEvent event, Tab targetTab) {
            Object state = event.getLocalState();
            if (!(state instanceof Tab)) {
                return false;
            }
            Tab draggedTab = (Tab) state;
            boolean allowed = !draggedTab.getId().equals(targetTab.getId())
                    && draggedTab.isPrivate() == targetTab.isPrivate();
            if (event.getAction() == DragEvent.ACTION_DROP) {
                setDropTargetHighlighted(false, selectedTabIds.contains(targetTab.getId()),
                        ContextCompat.getColor(itemView.getContext(),
                                R.color.tab_manager_text_secondary));
                if (allowed) {
                    listener.onCreateGroupFromTabs(draggedTab, targetTab);
                }
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                setDropTargetHighlighted(allowed, selectedTabIds.contains(targetTab.getId()),
                        ContextCompat.getColor(itemView.getContext(),
                                R.color.tab_manager_text_secondary));
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED
                    || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                setDropTargetHighlighted(false, selectedTabIds.contains(targetTab.getId()),
                        ContextCompat.getColor(itemView.getContext(),
                                R.color.tab_manager_text_secondary));
                return true;
            }
            return event.getAction() == DragEvent.ACTION_DRAG_STARTED
                    || event.getAction() == DragEvent.ACTION_DRAG_LOCATION;
        }

        private void setDropTargetHighlighted(boolean highlighted, boolean selected, int color) {
            if (highlighted) {
                binding.groupCard.setStrokeColor(color);
                binding.groupCard.setStrokeWidth(dp(3));
                binding.groupCard.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(120)
                        .start();
            } else {
                binding.groupCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start();
                binding.groupCard.setStrokeColor(selected ? color : Color.TRANSPARENT);
                binding.groupCard.setStrokeWidth(selected ? dp(3) : 0);
            }
        }

        private void resetCardTransform() {
            binding.groupCard.animate().cancel();
            binding.groupCard.setScaleX(1f);
            binding.groupCard.setScaleY(1f);
        }

        private boolean startTabDrag(View view, Tab tab) {
            ClipData data = ClipData.newPlainText("tab_id", tab.getId());
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(data, shadow, tab, 0);
            } else {
                view.startDrag(data, shadow, tab, 0);
            }
            return true;
        }

        private void showOverflow(View anchor, TabGroup group) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.getMenu().add(Menu.NONE, MENU_OPEN, Menu.NONE, R.string.open_group);
            menu.getMenu().add(Menu.NONE, MENU_RENAME, Menu.NONE, R.string.rename_group);
            menu.getMenu().add(Menu.NONE, MENU_COLOR, Menu.NONE, R.string.change_group_color);
            menu.getMenu().add(Menu.NONE, MENU_TOGGLE_COLLAPSE, Menu.NONE,
                    collapsedGroupIds.contains(group.getGroupId())
                            ? R.string.expand_group
                            : R.string.collapse_group);
            menu.getMenu().add(Menu.NONE, MENU_CLOSE_GROUP, Menu.NONE, R.string.close_group);
            menu.getMenu().add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.delete_group);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == MENU_OPEN) {
                    listener.onOpenGroup(group, null);
                    return true;
                } else if (item.getItemId() == MENU_RENAME) {
                    listener.onRenameGroup(group);
                    return true;
                } else if (item.getItemId() == MENU_COLOR) {
                    listener.onChangeGroupColor(group);
                    return true;
                } else if (item.getItemId() == MENU_TOGGLE_COLLAPSE) {
                    listener.onToggleGroupCollapsed(group);
                    return true;
                } else if (item.getItemId() == MENU_CLOSE_GROUP) {
                    listener.onCloseGroup(group);
                    return true;
                } else if (item.getItemId() == MENU_DELETE) {
                    listener.onDeleteGroup(group);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private void loadFavicon(ImageView imageView, Tab tab) {
            imageView.setVisibility(View.VISIBLE);
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

    private static void tintCircle(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        int strokeWidth = Math.round(3 * view.getResources().getDisplayMetrics().density);
        drawable.setStroke(strokeWidth,
                ContextCompat.getColor(view.getContext(), R.color.colorSurface));
        view.setBackground(drawable);
    }

    private static int cardBackgroundForGroup(Context context, int color) {
        if (color == 0) {
            return ContextCompat.getColor(context, R.color.tab_manager_panel_selected);
        }
        return blendWithBase(color, ContextCompat.getColor(context, R.color.colorSurface), 0.24f);
    }

    private static int previewCellBackgroundForGroup(Context context, int color) {
        if (color == 0) {
            return ContextCompat.getColor(context, R.color.tab_manager_preview);
        }
        return blendWithBase(color, ContextCompat.getColor(context, R.color.colorSurface), 0.12f);
    }

    private static int blendWithBase(int color, int baseColor, float colorWeight) {
        float clampedWeight = Math.max(0f, Math.min(1f, colorWeight));
        int red = Math.round(Color.red(baseColor) * (1f - clampedWeight)
                + Color.red(color) * clampedWeight);
        int green = Math.round(Color.green(baseColor) * (1f - clampedWeight)
                + Color.green(color) * clampedWeight);
        int blue = Math.round(Color.blue(baseColor) * (1f - clampedWeight)
                + Color.blue(color) * clampedWeight);
        return Color.rgb(red, green, blue);
    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<DisplayItem> oldItems;
        private final List<DisplayItem> newItems;

        DiffCallback(List<DisplayItem> oldItems, List<DisplayItem> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldItems.get(oldItemPosition).stableId()
                    .equals(newItems.get(newItemPosition).stableId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DisplayItem oldItem = oldItems.get(oldItemPosition);
            DisplayItem newItem = newItems.get(newItemPosition);
            if (oldItem.isGroup() != newItem.isGroup()) {
                return false;
            }
            if (oldItem.isGroup()) {
                return oldItem.group.getGroupColor() == newItem.group.getGroupColor()
                        && oldItem.group.getTabCount() == newItem.group.getTabCount()
                        && safeEquals(groupSignature(oldItem.group), groupSignature(newItem.group));
            }
            return safeEquals(oldItem.tab.getTitle(), newItem.tab.getTitle())
                    && safeEquals(oldItem.tab.getThumbnailPath(), newItem.tab.getThumbnailPath())
                    && safeEquals(oldItem.tab.getFaviconUri(), newItem.tab.getFaviconUri())
                    && safeEquals(oldItem.tab.getUrl(), newItem.tab.getUrl());
        }
    }

    private static boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static String buildTitleSummary(List<Tab> tabs) {
        if (tabs == null || tabs.isEmpty()) {
            return "";
        }
        List<String> titles = new ArrayList<>();
        for (Tab tab : tabs) {
            String title = getDisplayTitle(tab);
            if (!TextUtils.isEmpty(title)) {
                titles.add(title);
            }
            if (titles.size() == 2) {
                break;
            }
        }
        return TextUtils.join(", ", titles);
    }

    private static String getDisplayTitle(Tab tab) {
        if (tab == null) {
            return "";
        }
        if (!TextUtils.isEmpty(tab.getTitle())) {
            return tab.getTitle();
        }
        String host = UrlUtils.getDisplayHost(tab.getUrl());
        return !TextUtils.isEmpty(host) ? host : "";
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

    private static String groupSignature(TabGroup group) {
        if (group == null || group.getTabs() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int count = Math.min(4, group.getTabs().size());
        for (int i = 0; i < count; i++) {
            Tab tab = group.getTabs().get(i);
            builder.append(tab.getId()).append('|')
                    .append(tab.getTitle()).append('|')
                    .append(tab.getUrl()).append('|')
                    .append(tab.getThumbnailPath()).append('|')
                    .append(tab.getFaviconUri()).append(';');
        }
        return builder.toString();
    }
}
