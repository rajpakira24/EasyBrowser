package com.webstudio.easybrowser.adapters;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.TabThumbnailCache;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── View types ──────────────────────────────────────────────────────────
    private static final int TYPE_GROUP = 0;
    private static final int TYPE_INDIVIDUAL = 1;
    private static final int TYPE_CHILD = 2;

    // ── Group accent colors (Brave-like palette) ─────────────────────────────
    // ── Internal display-item model ──────────────────────────────────────────
    private static class DisplayItem {
        final int type;
        // GROUP fields
        String groupName;
        List<Tab> groupTabs;
        int groupColor;
        // INDIVIDUAL fields
        Tab tab;

        static DisplayItem group(String name, List<Tab> tabs, int color) {
            DisplayItem d = new DisplayItem(TYPE_GROUP);
            d.groupName = name;
            d.groupTabs = tabs;
            d.groupColor = color;
            return d;
        }

        static DisplayItem individual(Tab tab) {
            DisplayItem d = new DisplayItem(TYPE_INDIVIDUAL);
            d.tab = tab;
            return d;
        }

        static DisplayItem child(Tab tab) {
            DisplayItem d = new DisplayItem(TYPE_CHILD);
            d.tab = tab;
            return d;
        }

        private DisplayItem(int type) {
            this.type = type;
        }
    }

    // ── State ────────────────────────────────────────────────────────────────
    private List<DisplayItem> items = new ArrayList<>();
    private String currentTabId;
    private final OnTabClickListener listener;

    public interface OnTabClickListener {
        void onTabClick(Tab tab);
        void onCloseTab(Tab tab);
        void onGroupClick(String groupName, List<Tab> groupTabs);
    }

    public TabsAdapter(List<Tab> tabs, String currentTabId, OnTabClickListener listener) {
        this.currentTabId = currentTabId;
        this.listener = listener;
        setTabs(tabs);
    }

    public void updateTabs(List<Tab> tabs, String currentTabId) {
        this.currentTabId = currentTabId;
        setTabs(tabs);
        notifyDataSetChanged();
    }

    /** Returns span count for position: 1 (half-width) for groups, 2 (full-width) for individual/child. */
    public int getSpanSize(int position) {
        if (position < 0 || position >= items.size()) return 2;
        return items.get(position).type == TYPE_GROUP ? 1 : 2;
    }

    public boolean isIndividualTab(int position) {
        return position >= 0 && position < items.size()
                && (items.get(position).type == TYPE_INDIVIDUAL
                    || items.get(position).type == TYPE_CHILD);
    }

    public boolean isGroup(int position) {
        return position >= 0 && position < items.size()
                && items.get(position).type == TYPE_GROUP;
    }

    public Tab getIndividualTab(int position) {
        return isIndividualTab(position) ? items.get(position).tab : null;
    }

    public String getGroupName(int position) {
        return isGroup(position) ? items.get(position).groupName : null;
    }

    // ── Build display items from flat tab list ────────────────────────────────
    private void setTabs(List<Tab> tabs) {
        items = new ArrayList<>();

        // Build id→tab lookup and parent→children map
        Map<String, Tab> idMap = new LinkedHashMap<>();
        Map<String, List<Tab>> childrenMap = new LinkedHashMap<>();
        for (Tab tab : tabs) {
            idMap.put(tab.getId(), tab);
        }
        for (Tab tab : tabs) {
            String pid = tab.getParentTabId();
            if (pid != null && idMap.containsKey(pid) && tab.getGroupName() == null) {
                if (!childrenMap.containsKey(pid)) childrenMap.put(pid, new ArrayList<>());
                childrenMap.get(pid).add(tab);
            }
        }

        // Determine root ancestors: tabs that are not a descendant of any other tab in the list
        java.util.Set<String> descendantIds = new java.util.HashSet<>();
        for (Map.Entry<String, List<Tab>> e : childrenMap.entrySet()) {
            for (Tab child : e.getValue()) {
                descendantIds.add(child.getId());
            }
        }

        // Manual groups (groupName set): maintain insertion order
        Map<String, List<Tab>> groupMap = new LinkedHashMap<>();
        List<Tab> topLevel = new ArrayList<>();

        for (Tab tab : tabs) {
            String group = tab.getGroupName();
            if (group != null && !group.isEmpty()) {
                if (!groupMap.containsKey(group)) groupMap.put(group, new ArrayList<>());
                groupMap.get(group).add(tab);
            } else if (!descendantIds.contains(tab.getId())) {
                topLevel.add(tab);
            }
        }

        // Emit groups first
        for (Map.Entry<String, List<Tab>> entry : groupMap.entrySet()) {
            int color = firstGroupColor(entry.getValue());
            items.add(DisplayItem.group(entry.getKey(), entry.getValue(), color));
        }

        // Emit top-level tabs, each followed by all descendants (BFS flattened as TYPE_CHILD)
        for (Tab tab : topLevel) {
            items.add(DisplayItem.individual(tab));
            collectDescendants(tab.getId(), childrenMap, items);
        }
    }

    private void collectDescendants(String parentId, Map<String, List<Tab>> childrenMap,
                                    List<DisplayItem> out) {
        List<Tab> children = childrenMap.get(parentId);
        if (children == null) return;
        for (Tab child : children) {
            out.add(DisplayItem.child(child));
            collectDescendants(child.getId(), childrenMap, out);
        }
    }

    // ── RecyclerView.Adapter overrides ───────────────────────────────────────
    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP) {
            View v = inflater.inflate(R.layout.item_tab_group, parent, false);
            return new GroupViewHolder(v);
        } else if (viewType == TYPE_CHILD) {
            View v = inflater.inflate(R.layout.item_tab_child, parent, false);
            return new ChildViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_tab, parent, false);
            return new IndividualViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        if (item.type == TYPE_GROUP) {
            ((GroupViewHolder) holder).bind(item);
        } else if (item.type == TYPE_CHILD) {
            ((ChildViewHolder) holder).bind(item.tab);
        } else {
            ((IndividualViewHolder) holder).bind(item.tab);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── Group ViewHolder ─────────────────────────────────────────────────────
    class GroupViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView groupCard;
        final LinearLayout groupHeader;
        final TextView groupNameText;
        final TextView groupCountBadge;
        final ImageButton closeGroupBtn;
        final LinearLayout miniRow2;
        final View rowDivider;
        // Cells
        final FrameLayout cell0, cell1, cell2, cell3;
        final TextView cell0Text, cell1Text, cell2Text, cell3Text;
        final TextView plusBadge;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupCard = itemView.findViewById(R.id.group_card);
            groupHeader = itemView.findViewById(R.id.group_header);
            groupNameText = itemView.findViewById(R.id.group_name_text);
            groupCountBadge = itemView.findViewById(R.id.group_count_badge);
            closeGroupBtn = itemView.findViewById(R.id.close_group_btn);
            miniRow2 = itemView.findViewById(R.id.mini_row_2);
            rowDivider = itemView.findViewById(R.id.row_divider);
            cell0 = itemView.findViewById(R.id.cell_0);
            cell1 = itemView.findViewById(R.id.cell_1);
            cell2 = itemView.findViewById(R.id.cell_2);
            cell3 = itemView.findViewById(R.id.cell_3);
            cell0Text = itemView.findViewById(R.id.cell_0_text);
            cell1Text = itemView.findViewById(R.id.cell_1_text);
            cell2Text = itemView.findViewById(R.id.cell_2_text);
            cell3Text = itemView.findViewById(R.id.cell_3_text);
            plusBadge = itemView.findViewById(R.id.plus_badge);
        }

        void bind(DisplayItem item) {
            int color = item.groupColor != 0
                    ? item.groupColor
                    : TabRepository.getDefaultGroupColor(itemView.getContext());
            List<Tab> groupTabs = item.groupTabs;
            int count = groupTabs.size();

            // Card stroke + header background
            groupCard.setStrokeColor(color);
            groupHeader.setBackgroundColor(color);
            groupNameText.setText(item.groupName);
            groupCountBadge.setText(String.valueOf(count));
            groupNameText.setOnClickListener(v ->
                    listener.onGroupClick(item.groupName, new ArrayList<>(groupTabs)));

            // Tinted cell background (group color at ~18% opacity)
            int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
            int cellBg = Color.argb(46, r, g, b);

            // Row 2 visibility
            boolean showRow2 = count > 2;
            miniRow2.setVisibility(showRow2 ? View.VISIBLE : View.GONE);
            rowDivider.setVisibility(showRow2 ? View.VISIBLE : View.GONE);

            // Bind cells 0, 1 (always shown)
            bindCell(cell0, cell0Text, 0, groupTabs, cellBg);
            bindCell(cell1, cell1Text, 1, groupTabs, cellBg);

            // Bind cells 2, 3 (only when row 2 is visible)
            if (showRow2) {
                bindCell(cell2, cell2Text, 2, groupTabs, cellBg);
                cell3.setBackgroundColor(cellBg);
                if (count > 4) {
                    // +N overflow badge
                    cell3Text.setVisibility(View.INVISIBLE);
                    plusBadge.setVisibility(View.VISIBLE);
                    plusBadge.setText("+" + (count - 3));
                    plusBadge.setBackgroundColor(color);
                } else {
                    plusBadge.setVisibility(View.GONE);
                    cell3Text.setVisibility(View.VISIBLE);
                    cell3Text.setText(count >= 4 ? getDomain(groupTabs.get(3).getUrl()) : "");
                }
            }

            // Click: tapping a cell switches to that specific tab
            cell0.setOnClickListener(count > 0 ? v -> listener.onTabClick(groupTabs.get(0)) : null);
            cell1.setOnClickListener(count > 1 ? v -> listener.onTabClick(groupTabs.get(1)) : null);
            cell2.setOnClickListener(showRow2 && count > 2 ? v -> listener.onTabClick(groupTabs.get(2)) : null);
            cell3.setOnClickListener(showRow2 && count == 4 ? v -> listener.onTabClick(groupTabs.get(3)) : null);

            groupCard.setOnClickListener(v ->
                    listener.onGroupClick(item.groupName, new ArrayList<>(groupTabs)));

            // X button → close all tabs in this group
            closeGroupBtn.setOnClickListener(v -> {
                for (Tab t : new ArrayList<>(groupTabs)) listener.onCloseTab(t);
            });
        }

        private void bindCell(FrameLayout cell, TextView text, int idx,
                List<Tab> tabs, int cellBg) {
            cell.setBackgroundColor(cellBg);
            if (idx < tabs.size()) {
                text.setVisibility(View.VISIBLE);
                text.setText(getDomain(tabs.get(idx).getUrl()));
            } else {
                text.setVisibility(View.INVISIBLE);
                text.setText("");
            }
        }
    }

    // ── Individual tab ViewHolder ────────────────────────────────────────────
    class IndividualViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView currentBadge;
        final ImageView favicon;
        final ImageView previewImage;
        final View previewPlaceholder;
        final TextView host;
        final TextView title;
        final TextView url;
        final ImageButton closeBtn;

        IndividualViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tab_card);
            currentBadge = itemView.findViewById(R.id.current_badge);
            favicon = itemView.findViewById(R.id.favicon);
            previewImage = itemView.findViewById(R.id.preview_image);
            previewPlaceholder = itemView.findViewById(R.id.preview_placeholder);
            host = itemView.findViewById(R.id.host);
            title = itemView.findViewById(R.id.title);
            url = itemView.findViewById(R.id.url);
            closeBtn = itemView.findViewById(R.id.close_tab);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onTabClick(items.get(pos).tab);
            });
            closeBtn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onCloseTab(items.get(pos).tab);
            });
        }

        void bind(Tab tab) {
            String displayTitle = tab.isPrivate()
                    ? itemView.getContext().getString(R.string.private_tab_label, tab.getTitle())
                    : (tab.getTitle() != null && !tab.getTitle().isEmpty()
                            ? tab.getTitle() : itemView.getContext().getString(R.string.new_tab));
            title.setText(displayTitle);

            String domain = getDomain(tab.getUrl());
            url.setText(UrlUtils.isInternalPageUrl(tab.getUrl())
                    ? itemView.getContext().getString(R.string.home) : domain);
            host.setText(domain);

            boolean isCurrent = tab.getId().equals(currentTabId);
            currentBadge.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            card.setStrokeColor(ContextCompat.getColor(itemView.getContext(),
                    isCurrent ? R.color.colorPrimary : R.color.border_color));
            card.setStrokeWidth(isCurrent ? 3 : 1);
            closeBtn.setImageResource(tab.isLocked() ? R.drawable.ic_lock : R.drawable.ic_close);
            closeBtn.setContentDescription(itemView.getContext().getString(
                    tab.isLocked() ? R.string.tab_locked : R.string.close_tab));

            if (tab.getFavicon() != null) {
                Glide.with(favicon).load(tab.getFavicon())
                        .placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(favicon);
            } else {
                favicon.setImageResource(R.mipmap.ic_launcher);
            }

            Bitmap thumb = tab.isPrivate() ? null : TabThumbnailCache.get(tab.getId());
            if (thumb != null) {
                previewImage.setImageBitmap(thumb);
                previewImage.setVisibility(View.VISIBLE);
                previewPlaceholder.setVisibility(View.GONE);
            } else {
                previewImage.setImageDrawable(null);
                previewImage.setVisibility(View.GONE);
                previewPlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }

    // ── Child tab ViewHolder (indented, shows parent relationship) ───────────
    class ChildViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView currentBadge;
        final ImageView favicon;
        final ImageView previewImage;
        final View previewPlaceholder;
        final TextView host;
        final TextView title;
        final TextView url;
        final ImageButton closeBtn;

        ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tab_card);
            currentBadge = itemView.findViewById(R.id.current_badge);
            favicon = itemView.findViewById(R.id.favicon);
            previewImage = itemView.findViewById(R.id.preview_image);
            previewPlaceholder = itemView.findViewById(R.id.preview_placeholder);
            host = itemView.findViewById(R.id.host);
            title = itemView.findViewById(R.id.title);
            url = itemView.findViewById(R.id.url);
            closeBtn = itemView.findViewById(R.id.close_tab);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onTabClick(items.get(pos).tab);
            });
            closeBtn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onCloseTab(items.get(pos).tab);
            });
        }

        void bind(Tab tab) {
            String displayTitle = tab.getTitle() != null && !tab.getTitle().isEmpty()
                    ? tab.getTitle() : itemView.getContext().getString(R.string.new_tab);
            title.setText(displayTitle);

            String domain = getDomain(tab.getUrl());
            url.setText(UrlUtils.isInternalPageUrl(tab.getUrl())
                    ? itemView.getContext().getString(R.string.home) : domain);
            host.setText(domain);

            boolean isCurrent = tab.getId().equals(currentTabId);
            currentBadge.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            card.setStrokeColor(ContextCompat.getColor(itemView.getContext(),
                    isCurrent ? R.color.colorPrimary : R.color.border_color));
            card.setStrokeWidth(isCurrent ? 3 : 1);
            closeBtn.setImageResource(tab.isLocked() ? R.drawable.ic_lock : R.drawable.ic_close);
            closeBtn.setContentDescription(itemView.getContext().getString(
                    tab.isLocked() ? R.string.tab_locked : R.string.close_tab));

            if (tab.getFavicon() != null) {
                Glide.with(favicon).load(tab.getFavicon())
                        .placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(favicon);
            } else {
                favicon.setImageResource(R.mipmap.ic_launcher);
            }

            Bitmap thumb = TabThumbnailCache.get(tab.getId());
            if (thumb != null) {
                previewImage.setImageBitmap(thumb);
                previewImage.setVisibility(View.VISIBLE);
                previewPlaceholder.setVisibility(View.GONE);
            } else {
                previewImage.setImageDrawable(null);
                previewImage.setVisibility(View.GONE);
                previewPlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static String getDomain(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty() || "about:blank".equals(rawUrl)) return "";
        if (UrlUtils.isInternalPageUrl(rawUrl)) return "Home";
        try {
            String host = Uri.parse(rawUrl).getHost();
            if (host == null || host.isEmpty()) return rawUrl;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return rawUrl;
        }
    }

    private static int firstGroupColor(List<Tab> tabs) {
        if (tabs == null) {
            return 0;
        }
        for (Tab tab : tabs) {
            if (tab != null && tab.getGroupColor() != 0) {
                return tab.getGroupColor();
            }
        }
        return 0;
    }
}
