package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.TabGroupAdapter;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.InactiveTabPolicy;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InactiveTabsActivity extends AppCompatActivity implements TabGroupAdapter.Listener {
    public static final String EXTRA_INACTIVE_DAYS = "inactive_days";
    public static final String RESULT_RESTORED_TAB_IDS = "restored_inactive_tab_ids";

    private final List<Tab> tabs = new ArrayList<>();
    private final ArrayList<String> closedTabIds = new ArrayList<>();
    private final ArrayList<String> restoredTabIds = new ArrayList<>();
    private final Set<String> selectedGroupIds = new LinkedHashSet<>();
    private final Set<String> selectedTabIds = new LinkedHashSet<>();

    private TabGroupAdapter adapter;
    private TextView titleText;
    private TextView infoText;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private MaterialButton closeAllButton;
    private boolean selectionMode;
    private boolean resultSet;
    private int inactiveDays;
    private String selectedTabId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenshotProtection.apply(this);
        setContentView(R.layout.activity_inactive_tabs);
        applySystemBars();
        inactiveDays = getIntent().getIntExtra(EXTRA_INACTIVE_DAYS, 21);
        restoreTabsFromIntent(getIntent());
        initializeViews();
        reload();
    }

    private void applySystemBars() {
        int chrome = ThemeEngine.settingsChromeColor(this);
        SystemBarUtils.apply(this, chrome, chrome, ThemeEngine.useDarkSystemBarIcons(chrome));
    }

    private void initializeViews() {
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton menuButton = findViewById(R.id.menu_button);
        ImageButton infoClose = findViewById(R.id.info_close);
        View infoCard = findViewById(R.id.info_card);
        titleText = findViewById(R.id.title_text);
        infoText = findViewById(R.id.info_text);
        emptyView = findViewById(R.id.empty_view);
        recyclerView = findViewById(R.id.inactive_recycler);
        closeAllButton = findViewById(R.id.close_all_button);

        applyInactiveThemeChrome(backButton, menuButton, infoClose);
        backButton.setOnClickListener(v -> finish());
        menuButton.setOnClickListener(this::showOverflowMenu);
        infoClose.setOnClickListener(v -> infoCard.setVisibility(View.GONE));
        closeAllButton.setOnClickListener(v -> closeSelectedOrAll());

        adapter = new TabGroupAdapter(this);
        adapter.setGridMode(true);
        adapter.setShowGroupCloseButton(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void applyInactiveThemeChrome(ImageButton backButton, ImageButton menuButton,
                                          ImageButton infoClose) {
        ThemeEngine.Palette palette = ThemeEngine.homePalette(this);
        int chrome = ThemeEngine.settingsChromeColor(this);
        int foreground = ThemeEngine.foregroundFor(chrome);
        View topBar = findViewById(R.id.top_bar);
        if (topBar != null) {
            topBar.setBackgroundColor(chrome);
        }
        tintTopButton(backButton, foreground);
        tintTopButton(menuButton, foreground);
        tintTopButton(infoClose, palette.accent);
        if (titleText != null) {
            titleText.setTextColor(foreground);
        }
        if (closeAllButton != null) {
            closeAllButton.setBackgroundTintList(ColorStateList.valueOf(palette.accent));
            closeAllButton.setTextColor(ThemeEngine.foregroundFor(palette.accent));
        }
    }

    private void tintTopButton(ImageButton button, int color) {
        if (button != null) {
            button.setColorFilter(color);
        }
    }

    private void restoreTabsFromIntent(Intent intent) {
        ArrayList<String> ids = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_IDS);
        ArrayList<String> titles = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_TITLES);
        ArrayList<String> urls = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_URLS);
        ArrayList<String> groupNames = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUPS);
        ArrayList<String> groupIds = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUP_IDS);
        ArrayList<String> thumbnails = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_THUMBNAILS);
        ArrayList<String> favicons = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_FAVICONS);
        ArrayList<String> parentIds = intent.getStringArrayListExtra(TabsActivity.EXTRA_TAB_PARENT_IDS);
        boolean[] privateStates = intent.getBooleanArrayExtra(TabsActivity.EXTRA_TAB_PRIVATE_STATES);
        boolean[] pinnedStates = intent.getBooleanArrayExtra(TabsActivity.EXTRA_TAB_PINNED_STATES);
        int[] groupColors = intent.getIntArrayExtra(TabsActivity.EXTRA_TAB_GROUP_COLORS);
        int[] positions = intent.getIntArrayExtra(TabsActivity.EXTRA_TAB_POSITIONS);
        long[] createdAt = intent.getLongArrayExtra(TabsActivity.EXTRA_TAB_CREATED_AT);
        long[] lastAccessed = intent.getLongArrayExtra(TabsActivity.EXTRA_TAB_LAST_ACCESSED);
        if (ids == null || titles == null || urls == null || privateStates == null) {
            return;
        }
        int count = Math.min(Math.min(ids.size(), titles.size()),
                Math.min(urls.size(), privateStates.length));
        for (int i = 0; i < count; i++) {
            Tab tab = new Tab(ids.get(i), null, titles.get(i), urls.get(i), privateStates[i]);
            String groupId = valueAt(groupIds, i);
            if (!groupId.isEmpty()) {
                tab.setGroupId(groupId);
                tab.setGroupName(valueAt(groupNames, i));
                tab.setGroupColor(groupColors != null && i < groupColors.length
                        ? groupColors[i]
                        : TabRepository.getDefaultGroupColor(this));
            }
            tab.setThumbnailPath(valueAt(thumbnails, i));
            tab.setFaviconUri(valueAt(favicons, i));
            String parentId = valueAt(parentIds, i);
            if (!parentId.isEmpty()) {
                tab.setParentTabId(parentId);
            }
            tab.setPosition(positions != null && i < positions.length ? positions[i] : i);
            tab.setPinned(pinnedStates != null && i < pinnedStates.length && pinnedStates[i]);
            if (createdAt != null && i < createdAt.length && createdAt[i] > 0) {
                tab.setCreatedAt(createdAt[i]);
            }
            if (lastAccessed != null && i < lastAccessed.length && lastAccessed[i] > 0) {
                tab.setLastAccessed(lastAccessed[i]);
            }
            tabs.add(tab);
        }
    }

    private void reload() {
        InactiveOverview overview = buildInactiveOverview();
        int count = overview.displayItemCount();
        titleText.setText(getResources().getQuantityString(
                R.plurals.inactive_items_count, count, count));
        boolean archiveDuplicates = shouldArchiveDuplicateTabs();
        if (inactiveDays > 0) {
            infoText.setText(archiveDuplicates
                    ? getString(R.string.inactive_items_info_days, inactiveDays)
                    : getString(R.string.inactive_items_info_age_only, inactiveDays));
        } else if (archiveDuplicates) {
            infoText.setText(R.string.inactive_items_info_duplicates);
        } else {
            infoText.setText(R.string.inactive_items_info_disabled);
        }
        adapter.submitOverview(overview.groups, overview.standaloneTabs);
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        boolean empty = count == 0;
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        closeAllButton.setVisibility(empty ? View.GONE : View.VISIBLE);
        closeAllButton.setText(isSelectionMode() ? R.string.close_selected : R.string.close_all);
    }

    private InactiveOverview buildInactiveOverview() {
        long cutoff = getInactiveCutoffMillis();
        Set<String> duplicateUrls = duplicateTabIds();
        Map<String, TabGroup> groupsById = new LinkedHashMap<>();
        List<Tab> standaloneTabs = new ArrayList<>();
        for (Tab tab : tabs) {
            if (closedTabIds.contains(tab.getId())
                    || restoredTabIds.contains(tab.getId())
                    || !isInactiveTab(tab, cutoff, duplicateUrls)) {
                continue;
            }
            String groupId = tab.getGroupId();
            if (groupId == null || groupId.trim().isEmpty()) {
                standaloneTabs.add(tab);
                continue;
            }
            TabGroup group = groupsById.get(groupId);
            if (group == null) {
                String name = tab.getGroupName() != null && !tab.getGroupName().trim().isEmpty()
                        ? tab.getGroupName()
                        : getString(R.string.tab_group);
                int color = tab.getGroupColor() != 0
                        ? tab.getGroupColor()
                        : TabRepository.getDefaultGroupColor(this);
                group = new TabGroup(groupId, name, color, tab.isPrivate(),
                        tab.getCreatedAt(), tab.getLastAccessed());
                groupsById.put(groupId, group);
            }
            group.getTabs().add(tab);
        }
        List<TabGroup> groups = new ArrayList<>();
        for (TabGroup group : groupsById.values()) {
            if (group.getTabCount() >= 2) {
                groups.add(group);
            } else {
                standaloneTabs.addAll(group.getTabs());
            }
        }
        return new InactiveOverview(groups, standaloneTabs);
    }

    private boolean isInactiveTab(Tab tab, long cutoff, Set<String> surplusDuplicateTabIds) {
        return InactiveTabPolicy.isInactive(tab, cutoff, surplusDuplicateTabIds);
    }

    private Set<String> duplicateTabIds() {
        if (!shouldArchiveDuplicateTabs()) {
            return new LinkedHashSet<>();
        }
        List<Tab> eligible = new ArrayList<>();
        for (Tab tab : tabs) {
            if (closedTabIds.contains(tab.getId()) || restoredTabIds.contains(tab.getId())) {
                continue;
            }
            eligible.add(tab);
        }
        return InactiveTabPolicy.surplusDuplicateTabIds(eligible);
    }

    private boolean shouldArchiveDuplicateTabs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsKeys.PREF_ARCHIVE_DUPLICATE_TABS, true);
    }

    private long getInactiveCutoffMillis() {
        return InactiveTabPolicy.cutoffMillis(inactiveDays);
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.restore_all);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.select_items);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.settings);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                restoreAll();
                return true;
            } else if (item.getItemId() == 2) {
                selectionMode = true;
                adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
                reload();
                return true;
            } else if (item.getItemId() == 3) {
                Intent intent = new Intent(this, SettingsSubpageActivity.class)
                        .putExtra(SettingsSubpageActivity.EXTRA_PAGE,
                                SettingsSubpageActivity.PAGE_INACTIVE_TABS);
                startActivity(intent);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void restoreAll() {
        for (Tab tab : getCurrentInactiveTabs()) {
            addUnique(restoredTabIds, tab.getId());
        }
        finishWithResult();
    }

    private void closeSelectedOrAll() {
        List<Tab> target = isSelectionMode() ? getSelectedTabs() : getCurrentInactiveTabs();
        if (target.isEmpty()) {
            return;
        }
        for (Tab tab : target) {
            addUnique(closedTabIds, tab.getId());
        }
        clearSelection();
        finishWithResult();
    }

    private List<Tab> getCurrentInactiveTabs() {
        InactiveOverview overview = buildInactiveOverview();
        List<Tab> result = new ArrayList<>();
        for (TabGroup group : overview.groups) {
            result.addAll(group.getTabs());
        }
        result.addAll(overview.standaloneTabs);
        return result;
    }

    private List<Tab> getSelectedTabs() {
        List<Tab> result = new ArrayList<>();
        InactiveOverview overview = buildInactiveOverview();
        for (TabGroup group : overview.groups) {
            if (selectedGroupIds.contains(group.getGroupId())) {
                result.addAll(group.getTabs());
            }
        }
        for (Tab tab : overview.standaloneTabs) {
            if (selectedTabIds.contains(tab.getId())) {
                result.add(tab);
            }
        }
        return result;
    }

    private void clearSelection() {
        selectedGroupIds.clear();
        selectedTabIds.clear();
        selectionMode = false;
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
    }

    private boolean isSelectionMode() {
        return selectionMode || !selectedGroupIds.isEmpty() || !selectedTabIds.isEmpty();
    }

    @Override
    public void onOpenGroup(TabGroup group, View sourceView) {
        if (group == null) {
            return;
        }
        if (isSelectionMode()) {
            toggleGroupSelection(group);
            return;
        }
        for (Tab tab : group.getTabs()) {
            addUnique(restoredTabIds, tab.getId());
        }
        finishWithResult();
    }

    @Override
    public void onOpenTab(Tab tab) {
        if (tab == null) {
            return;
        }
        if (isSelectionMode()) {
            toggleTabSelection(tab);
            return;
        }
        addUnique(restoredTabIds, tab.getId());
        selectedTabId = tab.getId();
        finishWithResult();
    }

    @Override
    public void onCloseGroup(TabGroup group) {
        if (group == null) {
            return;
        }
        for (Tab tab : group.getTabs()) {
            addUnique(closedTabIds, tab.getId());
        }
        reload();
    }

    @Override
    public void onCloseTab(Tab tab) {
        if (tab != null) {
            addUnique(closedTabIds, tab.getId());
            reload();
        }
    }

    @Override
    public void onGroupLongClick(TabGroup group) {
        toggleGroupSelection(group);
    }

    @Override
    public void onTabLongClick(Tab tab, View anchor) {
        toggleTabSelection(tab);
    }

    private void toggleGroupSelection(TabGroup group) {
        if (group == null) {
            return;
        }
        if (selectedGroupIds.contains(group.getGroupId())) {
            selectedGroupIds.remove(group.getGroupId());
        } else {
            selectedGroupIds.add(group.getGroupId());
        }
        selectionMode = isSelectionMode();
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        reload();
    }

    private void toggleTabSelection(Tab tab) {
        if (tab == null) {
            return;
        }
        if (selectedTabIds.contains(tab.getId())) {
            selectedTabIds.remove(tab.getId());
        } else {
            selectedTabIds.add(tab.getId());
        }
        selectionMode = isSelectionMode();
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        reload();
    }

    private void finishWithResult() {
        setInactiveResult();
        finish();
    }

    private void setInactiveResult() {
        Intent result = new Intent();
        result.putStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS, closedTabIds);
        result.putStringArrayListExtra(RESULT_RESTORED_TAB_IDS, restoredTabIds);
        if (selectedTabId != null) {
            result.putExtra(TabsActivity.RESULT_SELECTED_TAB_ID, selectedTabId);
        }
        setResult(RESULT_OK, result);
        resultSet = true;
    }

    @Override
    public void finish() {
        if (!resultSet) {
            setInactiveResult();
        }
        super.finish();
    }

    private void addUnique(ArrayList<String> target, String value) {
        if (value != null && !target.contains(value)) {
            target.add(value);
        }
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index);
    }

    private static final class InactiveOverview {
        final List<TabGroup> groups;
        final List<Tab> standaloneTabs;

        InactiveOverview(List<TabGroup> groups, List<Tab> standaloneTabs) {
            this.groups = groups;
            this.standaloneTabs = standaloneTabs;
        }

        int displayItemCount() {
            int groupCount = groups != null ? groups.size() : 0;
            int tabCount = standaloneTabs != null ? standaloneTabs.size() : 0;
            return groupCount + tabCount;
        }
    }

    @Override public void onRenameGroup(TabGroup group) {}
    @Override public void onDeleteGroup(TabGroup group) { onCloseGroup(group); }
    @Override public void onChangeGroupColor(TabGroup group) {}
    @Override public void onMoveTabToGroup(Tab tab, TabGroup targetGroup) {}
    @Override public void onCreateGroupFromTabs(Tab firstTab, Tab secondTab) {}
    @Override public void onToggleGroupCollapsed(TabGroup group) {}
    @Override public void onAddTabToGroup(Tab tab) {}
    @Override public void onRemoveTabFromGroup(Tab tab) {}
    @Override public void onBookmarkTab(Tab tab) {}
    @Override public void onShareTab(Tab tab) {
        Toast.makeText(this, R.string.share, Toast.LENGTH_SHORT).show();
    }
    @Override public void onDuplicateTab(Tab tab) {}
    @Override public void onPinTab(Tab tab) {}
}
