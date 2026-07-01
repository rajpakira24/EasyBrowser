package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.TabsAdapter;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.TabActionContract;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.ArrayList;
import java.util.List;

public class TabsActivity extends AppCompatActivity implements TabsAdapter.OnTabClickListener {
    private static final int MENU_CREATE_GROUP = 1;

    public static final String EXTRA_TAB_IDS = "tab_ids";
    public static final String EXTRA_TAB_TITLES = "tab_titles";
    public static final String EXTRA_TAB_URLS = "tab_urls";
    public static final String EXTRA_TAB_PRIVATE_STATES = "tab_private_states";
    public static final String EXTRA_CURRENT_TAB_ID = "current_tab_id";
    public static final String RESULT_SELECTED_TAB_ID = "selected_tab_id";
    public static final String RESULT_CLOSED_TAB_IDS = "closed_tab_ids";
    public static final String RESULT_CREATE_PRIVATE_TAB = "create_private_tab";
    public static final String RESULT_RESTORE_URL = "restore_url";
    public static final String RESULT_RESTORE_PRIVATE = "restore_private";
    public static final String EXTRA_CLOSED_TAB_TITLES = "closed_tab_titles";
    public static final String EXTRA_CLOSED_TAB_URLS = "closed_tab_urls";
    public static final String EXTRA_TAB_GROUPS = "tab_groups";
    public static final String EXTRA_TAB_GROUP_IDS = "tab_group_ids_extra";
    public static final String EXTRA_TAB_GROUP_COLORS = "tab_group_colors";
    public static final String EXTRA_TAB_THUMBNAILS = "tab_thumbnail_paths";
    public static final String EXTRA_TAB_FAVICONS = "tab_favicon_uris";
    public static final String EXTRA_TAB_POSITIONS = "tab_positions";
    public static final String EXTRA_TAB_PARENT_IDS = "tab_parent_ids";
    public static final String EXTRA_TAB_CREATED_AT = "tab_created_at";
    public static final String EXTRA_TAB_LAST_ACCESSED = "tab_last_accessed";
    public static final String EXTRA_TAB_PINNED_STATES = "tab_pinned_states";
    public static final String EXTRA_TAB_LOCKED_STATES = "tab_locked_states";
    public static final String RESULT_TAB_GROUP_IDS = "tab_group_ids";
    public static final String RESULT_TAB_GROUP_NAMES = "tab_group_names";

    private RecyclerView tabsRecycler;
    private TabsAdapter adapter;
    private TextView tabCount;
    private TextView emptyView;
    private final List<Tab> tabs = new ArrayList<>();
    private final ArrayList<String> closedTabIds = new ArrayList<>();
    private String currentTabId;
    private String selectedTabId;
    private String restoreUrl;
    private Boolean createPrivateTab;
    private boolean showingPrivateTabs = false;
    private boolean groupsChanged = false;
    private Tab draggedTab;
    private int dragTargetPosition = RecyclerView.NO_POSITION;
    private boolean resultSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenshotProtection.apply(this);
        setContentView(R.layout.activity_tabs);

        restoreTabsFromIntent(getIntent());
        if (tabs.isEmpty()) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        showingPrivateTabs = isCurrentTabPrivate();
        MaterialButtonToggleGroup filterGroup = findViewById(R.id.tab_filter_group);
        filterGroup.check(showingPrivateTabs ? R.id.btn_private_tabs : R.id.btn_public_tabs);
        setupRecyclerView();
        setupDragAndDrop();
        setupTabFilter();
        setupFab();
        refreshTabs();
        updateTabCount();
        loadRecentlyClosed(getIntent());
    }

    private void restoreTabsFromIntent(Intent intent) {
        ArrayList<String> ids = intent.getStringArrayListExtra(EXTRA_TAB_IDS);
        ArrayList<String> titles = intent.getStringArrayListExtra(EXTRA_TAB_TITLES);
        ArrayList<String> urls = intent.getStringArrayListExtra(EXTRA_TAB_URLS);
        ArrayList<String> groups = intent.getStringArrayListExtra(EXTRA_TAB_GROUPS);
        ArrayList<String> parentIds = intent.getStringArrayListExtra(EXTRA_TAB_PARENT_IDS);
        boolean[] privateStates = intent.getBooleanArrayExtra(EXTRA_TAB_PRIVATE_STATES);
        boolean[] pinnedStates = intent.getBooleanArrayExtra(EXTRA_TAB_PINNED_STATES);
        boolean[] lockedStates = intent.getBooleanArrayExtra(EXTRA_TAB_LOCKED_STATES);
        currentTabId = intent.getStringExtra(EXTRA_CURRENT_TAB_ID);
        if (ids == null || titles == null || urls == null || privateStates == null) {
            return;
        }
        int count = Math.min(Math.min(ids.size(), titles.size()), Math.min(urls.size(), privateStates.length));
        for (int i = 0; i < count; i++) {
            Tab tab = new Tab(ids.get(i), null, titles.get(i), urls.get(i), privateStates[i]);
            if (groups != null && i < groups.size() && !groups.get(i).isEmpty()) {
                tab.setGroupName(groups.get(i));
            }
            if (parentIds != null && i < parentIds.size() && !parentIds.get(i).isEmpty()) {
                tab.setParentTabId(parentIds.get(i));
            }
            tab.setPinned(pinnedStates != null && i < pinnedStates.length && pinnedStates[i]);
            tab.setLocked(lockedStates != null && i < lockedStates.length && lockedStates[i]);
            tabs.add(tab);
        }
    }

    private void initViews() {
        tabsRecycler = findViewById(R.id.tabs_recycler);
        tabCount = findViewById(R.id.tab_count);
        emptyView = findViewById(R.id.empty_view);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(Menu.NONE, MENU_CREATE_GROUP, Menu.NONE, R.string.create_group)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_CREATE_GROUP) {
                showCreateGroupDialog();
                return true;
            }
            return false;
        });
        ThemeEngine.applyChrome(this, toolbar);
        tabCount.setTextColor(ThemeEngine.foregroundFor(ThemeEngine.settingsChromeColor(this)));
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter != null ? adapter.getSpanSize(position) : 2;
            }
        });
        tabsRecycler.setLayoutManager(gridLayoutManager);
        adapter = new TabsAdapter(getFilteredTabs(), getCurrentTabId(), this);
        tabsRecycler.setAdapter(adapter);
    }

    private void setupDragAndDrop() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (adapter != null && adapter.isIndividualTab(position)) {
                    return makeMovementFlags(ItemTouchHelper.UP
                            | ItemTouchHelper.DOWN
                            | ItemTouchHelper.LEFT
                            | ItemTouchHelper.RIGHT, 0);
                }
                return makeMovementFlags(0, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int sourcePosition = viewHolder.getAdapterPosition();
                int targetPosition = target.getAdapterPosition();
                draggedTab = adapter.getIndividualTab(sourcePosition);
                dragTargetPosition = targetPosition;
                return adapter.isGroup(targetPosition) || adapter.isIndividualTab(targetPosition);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (draggedTab != null && dragTargetPosition != RecyclerView.NO_POSITION) {
                    handleTabDrop(draggedTab, dragTargetPosition);
                }
                draggedTab = null;
                dragTargetPosition = RecyclerView.NO_POSITION;
            }
        });
        helper.attachToRecyclerView(tabsRecycler);
    }

    private void handleTabDrop(Tab sourceTab, int targetPosition) {
        if (sourceTab == null || sourceTab.getGroupName() != null) {
            return;
        }
        if (adapter.isGroup(targetPosition)) {
            String targetGroup = adapter.getGroupName(targetPosition);
            if (targetGroup != null && !targetGroup.isEmpty()) {
                sourceTab.setGroupName(targetGroup);
                groupsChanged = true;
                refreshTabs();
                updateTabCount();
            }
            return;
        }
        Tab targetTab = adapter.getIndividualTab(targetPosition);
        if (targetTab == null || targetTab == sourceTab || targetTab.getGroupName() != null) {
            return;
        }
        String groupName = getNextDefaultGroupName();
        sourceTab.setGroupName(groupName);
        targetTab.setGroupName(groupName);
        groupsChanged = true;
        refreshTabs();
        updateTabCount();
    }

    private String getNextDefaultGroupName() {
        java.util.Set<String> existingGroups = new java.util.HashSet<>();
        for (Tab tab : tabs) {
            if (tab.getGroupName() != null && !tab.getGroupName().isEmpty()) {
                existingGroups.add(tab.getGroupName());
            }
        }
        int index = 1;
        String baseName = getString(R.string.tab_group);
        String groupName;
        do {
            groupName = baseName + " " + index;
            index++;
        } while (existingGroups.contains(groupName));
        return groupName;
    }

    private void setupTabFilter() {
        MaterialButtonToggleGroup filterGroup = findViewById(R.id.tab_filter_group);
        filterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showingPrivateTabs = checkedId == R.id.btn_private_tabs;
            AnalyticsManager.logIncognitoModeToggled(this, showingPrivateTabs);
            refreshTabs();
        });
    }

    private void setupFab() {
        FloatingActionButton fabNewTab = findViewById(R.id.fab_new_tab);
        fabNewTab.setOnClickListener(v -> createNewTab(showingPrivateTabs));
    }

    private void createNewTab(boolean isPrivate) {
        createPrivateTab = isPrivate;
        finishWithResult();
    }

    private void showCreateGroupDialog() {
        Tab target = getCurrentVisibleTab();
        if (target == null || target.isPrivate()) {
            Toast.makeText(this, R.string.no_public_tabs, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(this);
        input.setHint(R.string.group_name_hint);
        String existing = target.getGroupName();
        if (existing != null) {
            input.setText(existing);
            input.selectAll();
        }
        input.setSingleLine(true);
        int padding = dp(16);
        input.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_group)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String groupName = input.getText().toString().trim();
                    target.setGroupName(groupName.isEmpty() ? null : groupName);
                    groupsChanged = true;
                    refreshTabs();
                    updateTabCount();
                })
                .setNeutralButton(R.string.no_group, (dialog, which) -> {
                    target.setGroupName(null);
                    groupsChanged = true;
                    refreshTabs();
                    updateTabCount();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private Tab getCurrentVisibleTab() {
        for (Tab tab : tabs) {
            if (tab.getId().equals(currentTabId) && tab.isPrivate() == showingPrivateTabs) {
                return tab;
            }
        }
        List<Tab> filteredTabs = getFilteredTabs();
        return filteredTabs.isEmpty() ? null : filteredTabs.get(0);
    }

    private void updateTabCount() {
        int publicCount = 0;
        int privateCount = 0;
        java.util.Set<String> groups = new java.util.LinkedHashSet<>();
        for (Tab tab : tabs) {
            if (tab.isPrivate()) {
                privateCount++;
            } else {
                publicCount++;
                if (tab.getGroupName() != null && !tab.getGroupName().isEmpty()) {
                    groups.add(tab.getGroupName());
                }
            }
        }
        String countText = getString(R.string.tab_filter_count, publicCount, privateCount);
        if (!groups.isEmpty()) {
            countText += " · " + groups.size() + " " + getString(R.string.tab_group);
        }
        tabCount.setText(countText);
    }

    private List<Tab> getFilteredTabs() {
        List<Tab> filtered = new ArrayList<>();
        for (Tab tab : tabs) {
            if (tab.isPrivate() == showingPrivateTabs) {
                filtered.add(tab);
            }
        }
        return filtered;
    }

    private void refreshTabs() {
        if (adapter == null) {
            return;
        }
        List<Tab> filteredTabs = getFilteredTabs();
        adapter.updateTabs(filteredTabs, getCurrentTabId());
        emptyView.setText(showingPrivateTabs ? R.string.no_private_tabs : R.string.no_public_tabs);
        emptyView.setVisibility(filteredTabs.isEmpty() ? View.VISIBLE : View.GONE);
        tabsRecycler.setVisibility(filteredTabs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTabClick(Tab tab) {
        selectedTabId = tab.getId();
        finishWithResult();
    }

    @Override
    public void onCloseTab(Tab tab) {
        if (tab != null && tab.isLocked()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            refreshTabs();
            return;
        }
        closedTabIds.add(tab.getId());
        tabs.remove(tab);
        if (tab.getId().equals(currentTabId)) {
            currentTabId = tabs.isEmpty() ? null : tabs.get(tabs.size() - 1).getId();
        }
        if (showingPrivateTabs && getFilteredTabs().isEmpty()) {
            showingPrivateTabs = false;
            MaterialButtonToggleGroup filterGroup = findViewById(R.id.tab_filter_group);
            filterGroup.check(R.id.btn_public_tabs);
        }
        refreshTabs();
        updateTabCount();
    }

    @Override
    public void onGroupClick(String groupName, List<Tab> groupTabs) {
        if (groupTabs == null || groupTabs.isEmpty()) {
            return;
        }
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(8);
        list.setPadding(horizontalPadding, dp(4), horizontalPadding, 0);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(groupName)
                .setView(list)
                .setNegativeButton(R.string.cancel, null);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        for (Tab tab : groupTabs) {
            list.addView(createGroupTabRow(tab, dialog));
        }
        dialog.show();
    }

    private View createGroupTabRow(Tab tab, androidx.appcompat.app.AlertDialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(4), dp(10));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));

        TextView title = new TextView(this);
        title.setText(tab.getTitle() != null && !tab.getTitle().isEmpty()
                ? tab.getTitle()
                : getString(R.string.new_tab));
        title.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        title.setTextSize(16);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(title);

        TextView url = new TextView(this);
        url.setText(tab.getUrl());
        url.setTextColor(ContextCompat.getColor(this, R.color.gray));
        url.setTextSize(13);
        url.setMaxLines(2);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        urlParams.setMargins(0, dp(3), 0, 0);
        textColumn.addView(url, urlParams);

        ImageButton closeButton = new ImageButton(this);
        closeButton.setImageResource(R.drawable.ic_close);
        closeButton.setBackground(ContextCompat.getDrawable(this, getSelectableItemBackgroundBorderless()));
        closeButton.setContentDescription(getString(R.string.close_tab));
        closeButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.addView(closeButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        row.setOnClickListener(v -> {
            selectedTabId = tab.getId();
            dialog.dismiss();
            finishWithResult();
        });
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            onCloseTab(tab);
        });

        return row;
    }

    private int getSelectableItemBackgroundBorderless() {
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        return outValue.resourceId;
    }

    private String getCurrentTabId() {
        return currentTabId;
    }

    private boolean isCurrentTabPrivate() {
        for (Tab tab : tabs) {
            if (tab.getId().equals(currentTabId)) {
                return tab.isPrivate();
            }
        }
        return false;
    }

    private void finishWithResult() {
        setTabsResult();
        finish();
    }

    private void loadRecentlyClosed(Intent intent) {
        ArrayList<String> titles = intent.getStringArrayListExtra(EXTRA_CLOSED_TAB_TITLES);
        ArrayList<String> urls = intent.getStringArrayListExtra(EXTRA_CLOSED_TAB_URLS);
        if (titles == null || urls == null || titles.isEmpty()) return;

        View section = findViewById(R.id.section_recently_closed);
        LinearLayout list = findViewById(R.id.recently_closed_list);
        section.setVisibility(View.VISIBLE);

        int count = Math.min(titles.size(), urls.size());
        for (int i = 0; i < count; i++) {
            String title = titles.get(i);
            String url = urls.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(16), dp(8), dp(16), dp(8));
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textCol.setLayoutParams(textParams);

            TextView titleView = new TextView(this);
            titleView.setText(title != null && !title.isEmpty() ? title : url);
            titleView.setTextSize(14);
            titleView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
            titleView.setMaxLines(1);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(titleView);

            TextView urlView = new TextView(this);
            urlView.setText(url);
            urlView.setTextSize(11);
            urlView.setTextColor(ContextCompat.getColor(this, R.color.gray));
            urlView.setMaxLines(1);
            urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(urlView);

            row.addView(textCol);

            TextView restoreBtn = new TextView(this);
            restoreBtn.setText(R.string.restore_tab);
            restoreBtn.setTextSize(13);
            restoreBtn.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            restoreBtn.setPadding(dp(8), 0, 0, 0);
            row.addView(restoreBtn);

            String capturedUrl = url;
            restoreBtn.setOnClickListener(v -> {
                restoreUrl = capturedUrl;
                finishWithResult();
            });

            list.addView(row);
        }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void setTabsResult() {
        Intent result = new Intent();
        result.putExtra(TabActionContract.EXTRA_ACTIONS, createResultActionsPayload());
        result.putStringArrayListExtra(RESULT_CLOSED_TAB_IDS, closedTabIds);
        if (selectedTabId != null) {
            result.putExtra(RESULT_SELECTED_TAB_ID, selectedTabId);
        }
        if (restoreUrl != null) {
            result.putExtra(RESULT_RESTORE_URL, restoreUrl);
        }
        if (createPrivateTab != null) {
            result.putExtra(RESULT_CREATE_PRIVATE_TAB, createPrivateTab);
        }
        if (groupsChanged) {
            ArrayList<String> ids = new ArrayList<>();
            ArrayList<String> groupNames = new ArrayList<>();
            for (Tab tab : tabs) {
                ids.add(tab.getId());
                groupNames.add(tab.getGroupName() != null ? tab.getGroupName() : "");
            }
            result.putStringArrayListExtra(RESULT_TAB_GROUP_IDS, ids);
            result.putStringArrayListExtra(RESULT_TAB_GROUP_NAMES, groupNames);
        }
        setResult(RESULT_OK, result);
        resultSet = true;
    }

    private String createResultActionsPayload() {
        ArrayList<TabActionContract.Action> actions = new ArrayList<>();
        if (!closedTabIds.isEmpty()) {
            actions.add(TabActionContract.closeTabs(closedTabIds));
        }
        if (restoreUrl != null && !restoreUrl.trim().isEmpty()) {
            actions.add(TabActionContract.restoreUrl(restoreUrl, false));
        }
        if (createPrivateTab != null) {
            actions.add(TabActionContract.createTab(createPrivateTab, null, null, 0));
        }
        if (groupsChanged) {
            actions.add(TabActionContract.groupsChanged());
        }
        if (selectedTabId != null && !selectedTabId.trim().isEmpty()) {
            actions.add(TabActionContract.selectTab(selectedTabId));
        }
        return TabActionContract.serialize(actions);
    }

    @Override
    public void finish() {
        if (!resultSet) {
            setTabsResult();
        }
        super.finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
