package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.TabGroupAdapter;
import com.webstudio.easybrowser.databinding.ActivityTabManagerBinding;
import com.webstudio.easybrowser.models.Bookmark;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.AppSettings;
import com.webstudio.easybrowser.utils.EasyMotion;
import com.webstudio.easybrowser.utils.InactiveTabPolicy;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.TabActionContract;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TabManagerActivity extends AppCompatActivity implements TabGroupAdapter.Listener {
    public static final String RESULT_GROUPS_CHANGED = "groups_changed";
    public static final String RESULT_GROUP_MUTATION_TAB_IDS = "group_mutation_tab_ids";
    public static final String RESULT_GROUP_MUTATION_GROUP_IDS = "group_mutation_group_ids";
    public static final String RESULT_GROUP_MUTATION_GROUP_NAMES = "group_mutation_group_names";
    public static final String RESULT_GROUP_MUTATION_GROUP_COLORS = "group_mutation_group_colors";
    public static final String RESULT_CREATED_PRIVATE_GROUP_IDS = "created_private_group_ids";
    public static final String RESULT_CREATED_PRIVATE_GROUP_TAB_IDS = "created_private_group_tab_ids";
    public static final String RESULT_CREATED_PRIVATE_GROUP_TAB_COUNTS = "created_private_group_tab_counts";
    public static final String RESULT_CREATED_PRIVATE_GROUP_NAMES = "created_private_group_names";
    public static final String RESULT_CREATED_PRIVATE_GROUP_COLORS = "created_private_group_colors";
    public static final String RESULT_REORDERED_PRIVATE_TAB_IDS = "reordered_private_tab_ids";
    public static final String RESULT_PINNED_TAB_IDS = "pinned_tab_ids";
    public static final String RESULT_UNPINNED_TAB_IDS = "unpinned_tab_ids";
    public static final String RESULT_LOCKED_TAB_IDS = "locked_tab_ids";
    public static final String RESULT_UNLOCKED_TAB_IDS = "unlocked_tab_ids";

    private ActivityTabManagerBinding binding;
    private TabGroupsViewModel viewModel;
    private BookmarkRepository bookmarkRepository;
    private AppSettings appSettings;
    private TabGroupAdapter adapter;
    private ItemTouchHelper overviewItemTouchHelper;
    private ActivityResultLauncher<Intent> groupTabsLauncher;
    private ActivityResultLauncher<Intent> inactiveTabsLauncher;
    private final ArrayList<String> closedTabIds = new ArrayList<>();
    private final ArrayList<String> restoredInactiveTabIds = new ArrayList<>();
    private final ArrayList<String> groupMutationTabIds = new ArrayList<>();
    private final ArrayList<String> groupMutationGroupIds = new ArrayList<>();
    private final ArrayList<String> groupMutationGroupNames = new ArrayList<>();
    private final ArrayList<Integer> groupMutationGroupColors = new ArrayList<>();
    private final ArrayList<String> createdPrivateGroupIds = new ArrayList<>();
    private final ArrayList<String> createdPrivateGroupTabIds = new ArrayList<>();
    private final ArrayList<Integer> createdPrivateGroupTabCounts = new ArrayList<>();
    private final ArrayList<String> createdPrivateGroupNames = new ArrayList<>();
    private final ArrayList<Integer> createdPrivateGroupColors = new ArrayList<>();
    private final ArrayList<String> reorderedPrivateTabIds = new ArrayList<>();
    private final ArrayList<String> pinnedTabIds = new ArrayList<>();
    private final ArrayList<String> unpinnedTabIds = new ArrayList<>();
    private final ArrayList<String> lockedTabIds = new ArrayList<>();
    private final ArrayList<String> unlockedTabIds = new ArrayList<>();
    private final Map<String, PendingClosedTab> pendingClosedTabs = new LinkedHashMap<>();
    private final Set<String> selectedGroupIds = new LinkedHashSet<>();
    private final Set<String> selectedTabIds = new LinkedHashSet<>();
    private final ArrayList<String> allTabIds = new ArrayList<>();
    private final List<Tab> runtimeTabs = new ArrayList<>();
    private final List<TabGroup> currentGroups = new ArrayList<>();
    private final List<Tab> currentStandaloneTabs = new ArrayList<>();
    private String selectedTabId;
    private String restoreUrl;
    private boolean restorePrivateTab;
    private Boolean createPrivateTab;
    private String createTabGroupId;
    private String createTabGroupName;
    private int createTabGroupColor;
    private boolean groupsChanged;
    private boolean resultSet;
    private boolean gridMode = true;
    private boolean privateMode = false;
    private int regularCount = 0;
    private int privateCount = 0;
    private String currentTabId;
    private String searchQuery = "";
    private boolean selectionMode;
    private boolean overviewOrderDirty;
    private boolean animateNextOverviewChange;
    private boolean pendingInitialOverviewScroll = true;
    private static final int TAB_MENU_NEW_TAB = 1;
    private static final int TAB_MENU_NEW_PRIVATE_TAB = 2;
    private static final int TAB_MENU_NEW_TAB_GROUP = 3;
    private static final int TAB_MENU_CLOSE_ALL = 4;
    private static final int TAB_MENU_SELECT_TABS = 5;
    private static final int TAB_MENU_DELETE_BROWSING_DATA = 6;
    private static final int TAB_MENU_SETTINGS = 7;

    private static final class TabManagerPalette {
        final int background;
        final int panel;
        final int panelSelected;
        final int search;
        final int text;
        final int textSecondary;
        final int chrome;
        final int fab;
        final int onFab;
        final TabGroupAdapter.ThemeColors adapterColors;

        TabManagerPalette(int background, int panel, int panelSelected, int search,
                          int text, int textSecondary, int chrome, int fab, int onFab,
                          TabGroupAdapter.ThemeColors adapterColors) {
            this.background = background;
            this.panel = panel;
            this.panelSelected = panelSelected;
            this.search = search;
            this.text = text;
            this.textSecondary = textSecondary;
            this.chrome = chrome;
            this.fab = fab;
            this.onFab = onFab;
            this.adapterColors = adapterColors;
        }
    }

    private static class PendingClosedTab {
        final Tab tab;
        final int index;

        PendingClosedTab(Tab tab, int index) {
            this.tab = tab;
            this.index = index;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenshotProtection.apply(this);
        binding = ActivityTabManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyTabManagerSystemBars();
        currentTabId = getIntent().getStringExtra(TabsActivity.EXTRA_CURRENT_TAB_ID);
        appSettings = new AppSettings(this);
        gridMode = !"list".equals(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsKeys.PREF_TAB_LAYOUT_MODE, "grid"));
        restoreRuntimeTabsFromIntent(getIntent());
        viewModel = new ViewModelProvider(this).get(TabGroupsViewModel.class);
        bookmarkRepository = new BookmarkRepository(this);
        setupActivityResult();
        setupTopBar();
        setupSelectionBar();
        setupRecycler();
        setupSearch();
        applyTabManagerThemeChrome();
        binding.fabNewTab.setOnClickListener(this::showTabManagerActionMenu);
        binding.fabNewTab.setOnLongClickListener(v -> {
            showTabManagerActionMenu(v);
            return true;
        });
        refreshCounts();
        loadGroups();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTabManagerThemeChrome();
    }

    private void applyTabManagerSystemBars() {
        TabManagerPalette colors = createTabManagerPalette();
        SystemBarUtils.apply(this, colors.chrome, colors.chrome,
                ThemeEngine.useDarkSystemBarIcons(colors.chrome));
    }

    private void applyTabManagerThemeChrome() {
        TabManagerPalette colors = createTabManagerPalette();
        int chrome = colors.chrome;
        int foreground = ThemeEngine.foregroundFor(chrome);
        applyTabManagerSystemBars();
        binding.getRoot().setBackgroundColor(colors.background);
        binding.topBar.setBackgroundColor(chrome);
        binding.selectionBar.setBackgroundColor(chrome);
        binding.groupsRecycler.setBackgroundColor(colors.background);
        binding.emptyView.setTextColor(colors.textSecondary);
        binding.searchLayout.setBackground(createRoundedDrawable(colors.search, dp(28)));
        binding.searchInput.setTextColor(colors.text);
        binding.searchInput.setHintTextColor(colors.textSecondary);
        binding.modeSelector.setBackground(createRoundedDrawable(colors.panel, dp(30)));
        binding.regularSegment.setBackground(createRoundedDrawable(colors.panelSelected, dp(24)));
        binding.regularTabCount.setBackground(createRoundedStrokeDrawable(
                Color.TRANSPARENT, foreground, dp(4), dp(2)));
        binding.inactiveItemsCard.setCardBackgroundColor(colors.panel);
        binding.inactiveItemsTitle.setTextColor(colors.text);
        binding.inactiveItemsSubtitle.setTextColor(colors.textSecondary);
        binding.backButton.setColorFilter(foreground);
        binding.viewToggle.setColorFilter(foreground);
        binding.privateSegment.setColorFilter(foreground);
        binding.selectionClose.setColorFilter(foreground);
        binding.regularTabCount.setTextColor(foreground);
        binding.selectionCount.setTextColor(foreground);
        binding.actionMove.setTextColor(foreground);
        binding.actionMerge.setTextColor(foreground);
        binding.actionDelete.setTextColor(foreground);
        binding.actionArchive.setTextColor(foreground);
        binding.fabNewTab.setBackgroundTintList(ColorStateList.valueOf(colors.fab));
        binding.fabNewTab.setImageTintList(ColorStateList.valueOf(colors.onFab));
        if (adapter != null) {
            adapter.setThemeColors(colors.adapterColors);
        }
    }

    private TabManagerPalette createTabManagerPalette() {
        ThemeEngine.Palette themePalette = ThemeEngine.homePalette(this);
        boolean light = isEffectiveLightMode();
        int fab = themePalette.accent;
        int onFab = ThemeEngine.foregroundFor(fab);
        if (light) {
            return new TabManagerPalette(
                    0xFFFAFAFA,
                    0xFFFFFFFF,
                    0xFFEEF3FF,
                    0xFFF1F2F4,
                    0xFF313851,
                    0xFF6F7482,
                    ThemeEngine.settingsChromeColor(this),
                    fab,
                    onFab,
                    TabGroupAdapter.ThemeColors.light());
        }
        return new TabManagerPalette(
                0xFF101A1D,
                0xFF172529,
                0xFF213238,
                0xFF18272C,
                0xFFDCE8E6,
                0xFF9AA8AC,
                0xFF172529,
                themePalette.accent,
                onFab,
                TabGroupAdapter.ThemeColors.dark());
    }

    private boolean isEffectiveLightMode() {
        String mode = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsKeys.PREF_THEME_MODE, "system");
        if ("light".equals(mode)) {
            return true;
        }
        if ("dark".equals(mode)) {
            return false;
        }
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_YES;
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createRoundedStrokeDrawable(int color, int strokeColor,
                                                        int radius, int strokeWidth) {
        GradientDrawable drawable = createRoundedDrawable(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupActivityResult() {
        groupTabsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) {
                        loadGroups();
                        return;
                    }
                    Intent data = result.getData();
                    ArrayList<String> childClosed =
                            data.getStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS);
                    if (childClosed != null) {
                        closedTabIds.addAll(childClosed);
                    }
                    if (data.getBooleanExtra(RESULT_GROUPS_CHANGED, false)) {
                        groupsChanged = true;
                    }
                    appendCreatedPrivateGroups(data);
                    appendGroupMutations(data);
                    appendReorderedPrivateTabs(data);
                    appendPinMutations(data);
                    appendLockMutations(data);
                    if (data.hasExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB)) {
                        createPrivateTab = data.getBooleanExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, false);
                        createTabGroupId = data.getStringExtra(GroupTabsActivity.EXTRA_GROUP_ID);
                        createTabGroupName = data.getStringExtra(GroupTabsActivity.EXTRA_GROUP_NAME);
                        createTabGroupColor = data.getIntExtra(GroupTabsActivity.EXTRA_GROUP_COLOR,
                                TabRepository.getDefaultGroupColor(this));
                        finishWithResult();
                        return;
                    }
                    restoreUrl = data.getStringExtra(TabsActivity.RESULT_RESTORE_URL);
                    if (restoreUrl != null) {
                        finishWithResult();
                        return;
                    }
                    selectedTabId = data.getStringExtra(TabsActivity.RESULT_SELECTED_TAB_ID);
                    if (selectedTabId != null) {
                        finishWithResult();
                    } else {
                        loadGroups();
                    }
                });
        inactiveTabsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) {
                        updateInactiveItemsCard();
                        return;
                    }
                    Intent data = result.getData();
                    ArrayList<String> restored =
                            data.getStringArrayListExtra(InactiveTabsActivity.RESULT_RESTORED_TAB_IDS);
                    if (restored != null && !restored.isEmpty()) {
                        restoredInactiveTabIds.addAll(restored);
                        touchRuntimeTabs(restored);
                        persistRestoredTabs(restored);
                    }
                    ArrayList<String> childClosed =
                            data.getStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS);
                    if (childClosed != null && !childClosed.isEmpty()) {
                        closedTabIds.addAll(childClosed);
                        for (String tabId : childClosed) {
                            removeRuntimeTab(tabId);
                        }
                        viewModel.deleteTabs(childClosed, () -> {
                            groupsChanged = true;
                            refreshCounts();
                            loadGroups();
                            updateInactiveItemsCard();
                        });
                        return;
                    }
                    selectedTabId = data.getStringExtra(TabsActivity.RESULT_SELECTED_TAB_ID);
                    if (selectedTabId != null) {
                        finishWithResult();
                        return;
                    }
                    refreshCounts();
                    loadGroups();
                    updateInactiveItemsCard();
                });
    }

    private void restoreRuntimeTabsFromIntent(Intent intent) {
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
        boolean[] lockedStates = intent.getBooleanArrayExtra(TabsActivity.EXTRA_TAB_LOCKED_STATES);
        int[] groupColors = intent.getIntArrayExtra(TabsActivity.EXTRA_TAB_GROUP_COLORS);
        int[] positions = intent.getIntArrayExtra(TabsActivity.EXTRA_TAB_POSITIONS);
        long[] createdAt = intent.getLongArrayExtra(TabsActivity.EXTRA_TAB_CREATED_AT);
        long[] lastAccessed = intent.getLongArrayExtra(TabsActivity.EXTRA_TAB_LAST_ACCESSED);
        if (ids == null || titles == null || urls == null || privateStates == null) {
            return;
        }
        allTabIds.clear();
        runtimeTabs.clear();
        int count = Math.min(Math.min(ids.size(), titles.size()),
                Math.min(urls.size(), privateStates.length));
        for (int i = 0; i < count; i++) {
            Tab tab = new Tab(ids.get(i), null, titles.get(i), urls.get(i), privateStates[i]);
            String groupId = valueAt(groupIds, i);
            if (!groupId.isEmpty()) {
                tab.setGroupId(groupId);
                tab.setGroupName(valueAt(groupNames, i));
                tab.setGroupColor(groupColors != null && i < groupColors.length ? groupColors[i] : 0);
            }
            tab.setThumbnailPath(valueAt(thumbnails, i));
            tab.setFaviconUri(valueAt(favicons, i));
            String parentId = valueAt(parentIds, i);
            if (!parentId.isEmpty()) {
                tab.setParentTabId(parentId);
            }
            tab.setPosition(positions != null && i < positions.length ? positions[i] : i);
            tab.setPinned(pinnedStates != null && i < pinnedStates.length && pinnedStates[i]);
            tab.setLocked(lockedStates != null && i < lockedStates.length && lockedStates[i]);
            if (createdAt != null && i < createdAt.length && createdAt[i] > 0) {
                tab.setCreatedAt(createdAt[i]);
            }
            if (lastAccessed != null && i < lastAccessed.length && lastAccessed[i] > 0) {
                tab.setLastAccessed(lastAccessed[i]);
            }
            runtimeTabs.add(tab);
            allTabIds.add(tab.getId());
        }
        refreshCountsFromRuntime();
    }

    private void setupTopBar() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.regularSegment.setOnClickListener(v -> {
            privateMode = false;
            clearSelection();
            loadGroups();
        });
        binding.privateSegment.setOnClickListener(v -> {
            privateMode = true;
            clearSelection();
            loadGroups();
        });
        binding.groupSegment.setVisibility(View.GONE);
        updateModeSelector();
        binding.inactiveItemsCard.setOnClickListener(v -> openInactiveItems());
    }

    private void setupSelectionBar() {
        binding.selectionClose.setOnClickListener(v -> clearSelection());
        binding.actionDelete.setOnClickListener(v -> deleteSelectedGroups());
        binding.actionMerge.setOnClickListener(v -> mergeSelectedGroups());
        binding.actionMove.setOnClickListener(v -> moveSelectedGroups());
        binding.actionArchive.setVisibility(View.GONE);
    }

    private void setupRecycler() {
        adapter = new TabGroupAdapter(this);
        binding.groupsRecycler.setAdapter(adapter);
        EasyMotion.configurePremiumItemAnimator(binding.groupsRecycler);
        overviewItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = canReorderOverview()
                        ? ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                        : 0;
                return makeMovementFlags(dragFlags, ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (!canReorderOverview()) {
                    return false;
                }
                boolean moved = adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                overviewOrderDirty = overviewOrderDirty || moved;
                return moved;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                TabGroup group = adapter.getGroupAt(position);
                if (group != null) {
                    adapter.notifyItemChanged(position);
                    onCloseGroup(group);
                    return;
                }
                Tab tab = adapter.getTabAt(position);
                if (tab != null) {
                    onCloseTab(tab);
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    animateDraggedItem(viewHolder.itemView, true);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                animateDraggedItem(viewHolder.itemView, false);
                if (overviewOrderDirty) {
                    overviewOrderDirty = false;
                    requestOverviewAnimation();
                    persistOverviewOrder(adapter.getOrderedTabsForPersistence());
                }
            }
        });
        overviewItemTouchHelper.attachToRecyclerView(binding.groupsRecycler);
        adapter.setCollapsedGroupIds(appSettings.getCollapsedGroupIds());
        updateLayoutManager();
        updateViewToggleIcon();
        binding.viewToggle.setOnClickListener(v -> {
            gridMode = !gridMode;
            TransitionManager.beginDelayedTransition(binding.groupsRecycler,
                    EasyMotion.premiumLayoutTransition());
            updateLayoutManager();
            adapter.setGridMode(gridMode);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(SettingsKeys.PREF_TAB_LAYOUT_MODE, gridMode ? "grid" : "list")
                    .apply();
            updateViewToggleIcon();
        });
    }

    private void updateLayoutManager() {
        if (gridMode) {
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return adapter != null && adapter.getItemCount() == 1 ? 2 : 1;
                }
            });
            binding.groupsRecycler.setLayoutManager(layoutManager);
        } else {
            binding.groupsRecycler.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void updateViewToggleIcon() {
        binding.viewToggle.setImageResource(gridMode
                ? R.drawable.ic_view_list
                : R.drawable.ic_view_grid);
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString() : "";
                loadGroups();
            }
        });
    }

    private void loadGroups() {
        updateModeSelector();
        updateInactiveItemsCard();
        if (privateMode) {
            RuntimeOverview overview = buildRuntimeOverview(true, searchQuery);
            currentGroups.clear();
            currentGroups.addAll(overview.groups);
            currentStandaloneTabs.clear();
            currentStandaloneTabs.addAll(overview.standaloneTabs);
            beginOverviewTransitionIfNeeded();
            adapter.setCollapsedGroupIds(appSettings.getCollapsedGroupIds());
            adapter.submitOverview(overview.groups, overview.standaloneTabs);
            adapter.setGridMode(gridMode);
            adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
            boolean empty = overview.groups.isEmpty() && overview.standaloneTabs.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.groupsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            scrollToInitialOverviewTargetIfNeeded();
            return;
        }
        viewModel.loadOverview(privateMode, searchQuery, (groups, standaloneTabs) -> {
            RuntimeOverview overview = filterInactiveFromOverview(groups, standaloneTabs);
            currentGroups.clear();
            currentGroups.addAll(overview.groups);
            currentStandaloneTabs.clear();
            currentStandaloneTabs.addAll(overview.standaloneTabs);
            beginOverviewTransitionIfNeeded();
            adapter.setCollapsedGroupIds(appSettings.getCollapsedGroupIds());
            adapter.submitOverview(overview.groups, overview.standaloneTabs);
            adapter.setGridMode(gridMode);
            adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
            boolean empty = overview.groups.isEmpty() && overview.standaloneTabs.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.groupsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            scrollToInitialOverviewTargetIfNeeded();
        });
    }

    private void scrollToInitialOverviewTargetIfNeeded() {
        if (!pendingInitialOverviewScroll || adapter == null || binding == null) {
            return;
        }
        int itemCount = adapter.getItemCount();
        if (itemCount == 0) {
            return;
        }
        pendingInitialOverviewScroll = false;
        int targetPosition = adapter.findPositionForTabId(currentTabId);
        if (targetPosition == RecyclerView.NO_POSITION) {
            targetPosition = itemCount - 1;
        }
        final int boundedPosition = Math.max(0, Math.min(targetPosition, itemCount - 1));
        binding.groupsRecycler.post(() -> {
            RecyclerView.LayoutManager layoutManager = binding.groupsRecycler.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager)
                        .scrollToPositionWithOffset(boundedPosition, 0);
            } else {
                binding.groupsRecycler.scrollToPosition(boundedPosition);
            }
        });
    }

    private void refreshCounts() {
        if (runtimeTabs.isEmpty()) {
            viewModel.loadCounts((loadedRegularCount, loadedPrivateCount) -> {
                regularCount = loadedRegularCount;
                privateCount = loadedPrivateCount;
                normalizeModeAfterCounts();
                updateModeSelector();
            });
            return;
        }
        refreshCountsFromRuntime();
        normalizeModeAfterCounts();
        updateModeSelector();
    }

    private void normalizeModeAfterCounts() {
        if (privateMode && privateCount == 0) {
            privateMode = false;
            clearSelection();
        }
    }

    private void refreshCountsFromRuntime() {
        regularCount = 0;
        privateCount = 0;
        for (Tab tab : runtimeTabs) {
            if (closedTabIds.contains(tab.getId())) {
                continue;
            }
            if (tab.isPrivate()) {
                privateCount++;
            } else {
                regularCount++;
            }
        }
    }

    private void updateInactiveItemsCard() {
        if (binding == null || binding.inactiveItemsCard == null) {
            return;
        }
        InactiveOverview overview = buildInactiveOverview(false, "");
        int count = overview.displayItemCount();
        binding.inactiveItemsCard.setVisibility(!privateMode && count > 0 ? View.VISIBLE : View.GONE);
        binding.inactiveItemsTitle.setText(getString(R.string.inactive_items_title, count));
        binding.inactiveItemsSubtitle.setText(getInactiveSummaryText());
    }

    private String getInactiveSummaryText() {
        int days = getInactiveThresholdDays();
        boolean archiveDuplicates = shouldArchiveDuplicateTabs();
        if (days > 0) {
            return archiveDuplicates
                    ? getString(R.string.inactive_items_summary_days)
                    : getString(R.string.inactive_items_summary_age_only, days);
        }
        return archiveDuplicates
                ? getString(R.string.inactive_items_summary_duplicates)
                : getString(R.string.inactive_items_summary_disabled);
    }

    private void updateModeSelector() {
        binding.regularTabCount.setText(String.valueOf(Math.max(regularCount, 0)));
        binding.privateSegment.setVisibility(privateCount > 0 ? View.VISIBLE : View.GONE);
        binding.regularSegment.setSelected(!privateMode);
        binding.privateSegment.setSelected(privateMode);
        binding.regularSegment.setBackgroundResource(!privateMode
                ? R.drawable.bg_tab_mode_selected
                : android.R.color.transparent);
        binding.privateSegment.setBackgroundResource(privateMode
                ? R.drawable.bg_tab_mode_selected
                : android.R.color.transparent);
    }

    private RuntimeOverview buildRuntimeOverview(boolean isPrivate, String query) {
        long cutoff = !isPrivate ? getInactiveCutoffMillis() : -1L;
        Set<String> duplicateUrls = !isPrivate
                ? duplicateTabIds(false)
                : new LinkedHashSet<>();
        Map<String, TabGroup> groupsById = new LinkedHashMap<>();
        List<Tab> standaloneTabs = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (tab.isPrivate() != isPrivate || closedTabIds.contains(tab.getId())) {
                continue;
            }
            if (!isPrivate && isInactiveTab(tab, cutoff, duplicateUrls)) {
                continue;
            }
            if (!matchesQuery(tab, query)) {
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
                group = new TabGroup(groupId, name, color, isPrivate,
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
                for (Tab tab : group.getTabs()) {
                    standaloneTabs.add(tab);
                }
            }
        }
        return new RuntimeOverview(groups, standaloneTabs);
    }

    private RuntimeOverview filterInactiveFromOverview(List<TabGroup> groups,
                                                       List<Tab> standaloneTabs) {
        long cutoff = getInactiveCutoffMillis();
        Set<String> duplicateUrls = duplicateTabIds(false);
        List<TabGroup> activeGroups = new ArrayList<>();
        List<Tab> activeStandaloneTabs = new ArrayList<>();

        if (standaloneTabs != null) {
            for (Tab tab : standaloneTabs) {
                if (!isInactiveTab(tab, cutoff, duplicateUrls)) {
                    activeStandaloneTabs.add(tab);
                }
            }
        }

        if (groups != null) {
            for (TabGroup group : groups) {
                if (group == null || group.getTabs() == null) {
                    continue;
                }
                TabGroup activeGroup = new TabGroup(
                        group.getGroupId(),
                        group.getGroupName(),
                        group.getGroupColor(),
                        group.isPrivate(),
                        group.getCreatedAt(),
                        group.getUpdatedAt());
                for (Tab tab : group.getTabs()) {
                    if (!isInactiveTab(tab, cutoff, duplicateUrls)) {
                        activeGroup.getTabs().add(tab);
                    }
                }
                if (activeGroup.getTabCount() >= 2) {
                    activeGroups.add(activeGroup);
                } else {
                    activeStandaloneTabs.addAll(activeGroup.getTabs());
                }
            }
        }

        return new RuntimeOverview(activeGroups, activeStandaloneTabs);
    }

    private InactiveOverview buildInactiveOverview(boolean isPrivate, String query) {
        long cutoff = getInactiveCutoffMillis();
        Set<String> duplicateUrls = duplicateTabIds(isPrivate);
        Map<String, TabGroup> groupsById = new LinkedHashMap<>();
        List<Tab> standaloneTabs = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (tab.isPrivate() != isPrivate || closedTabIds.contains(tab.getId())) {
                continue;
            }
            if (!isInactiveTab(tab, cutoff, duplicateUrls) || !matchesQuery(tab, query)) {
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
                group = new TabGroup(groupId, name, color, isPrivate,
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

    private Set<String> duplicateTabIds(boolean isPrivate) {
        if (!shouldArchiveDuplicateTabs()) {
            return new LinkedHashSet<>();
        }
        List<Tab> eligible = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (tab.isPrivate() != isPrivate || closedTabIds.contains(tab.getId())) {
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
        return InactiveTabPolicy.cutoffMillis(getInactiveThresholdDays());
    }

    private int getInactiveThresholdDays() {
        String value = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsKeys.PREF_INACTIVE_TAB_DAYS, "21");
        if (SettingsKeys.VALUE_OFF.equals(value)) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 21;
        }
    }

    private boolean matchesQuery(Tab tab, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.US);
        return contains(tab.getTitle(), needle) || contains(tab.getUrl(), needle)
                || contains(UrlUtils.getDisplayHost(tab.getUrl()), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.US).contains(needle);
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index);
    }

    private void requestOverviewAnimation() {
        animateNextOverviewChange = true;
    }

    private void beginOverviewTransitionIfNeeded() {
        if (!animateNextOverviewChange) {
            return;
        }
        animateNextOverviewChange = false;
        if (binding == null || binding.groupsRecycler == null) {
            return;
        }
        Transition transition = EasyMotion.premiumLayoutTransition();
        TransitionManager.beginDelayedTransition(binding.groupsRecycler, transition);
    }

    private void animateDraggedItem(View view, boolean lifted) {
        if (view == null) {
            return;
        }
        view.animate()
                .scaleX(lifted ? 1.03f : 1f)
                .scaleY(lifted ? 1.03f : 1f)
                .alpha(lifted ? 0.96f : 1f)
                .setDuration(lifted ? 120 : 160)
                .setInterpolator(lifted ? EasyMotion.EMPHASIZED : EasyMotion.STANDARD)
                .start();
    }

    private void animateGroupCreationFeedback() {
        if (binding != null) {
            EasyMotion.pulse(binding.fabNewTab);
        }
    }

    private static final class RuntimeOverview {
        final List<TabGroup> groups;
        final List<Tab> standaloneTabs;

        RuntimeOverview(List<TabGroup> groups, List<Tab> standaloneTabs) {
            this.groups = groups;
            this.standaloneTabs = standaloneTabs;
        }
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

    private void openInactiveItems() {
        if (inactiveTabsLauncher == null) {
            return;
        }
        InactiveOverview overview = buildInactiveOverview(false, "");
        if (overview.displayItemCount() == 0) {
            return;
        }
        Intent intent = new Intent(this, InactiveTabsActivity.class)
                .putExtra(InactiveTabsActivity.EXTRA_INACTIVE_DAYS, getInactiveThresholdDays());
        putRuntimeTabsExtras(intent, getRuntimeTabsForMode(false));
        inactiveTabsLauncher.launch(intent);
    }

    private void touchRuntimeTabs(List<String> tabIds) {
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String tabId : tabIds) {
            Tab tab = findRuntimeTab(tabId);
            if (tab != null) {
                tab.setLastAccessed(now);
            }
        }
        groupsChanged = true;
    }

    private void persistRestoredTabs(List<String> tabIds) {
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        final int[] remaining = {0};
        for (String tabId : tabIds) {
            Tab tab = findRuntimeTab(tabId);
            if (tab != null && !tab.isPrivate()) {
                remaining[0]++;
            }
        }
        if (remaining[0] == 0) {
            return;
        }
        for (String tabId : tabIds) {
            Tab tab = findRuntimeTab(tabId);
            if (tab == null || tab.isPrivate()) {
                continue;
            }
            viewModel.saveTab(tab, () -> {
                remaining[0]--;
                if (remaining[0] == 0) {
                    groupsChanged = true;
                    updateInactiveItemsCard();
                }
            });
        }
    }

    @Override
    public void onOpenGroup(TabGroup group, View sourceView) {
        if (isSelectionMode()) {
            toggleGroupSelection(group);
            return;
        }
        Intent intent = new Intent(this, GroupTabsActivity.class)
                .putExtra(GroupTabsActivity.EXTRA_GROUP_ID, group.getGroupId())
                .putExtra(GroupTabsActivity.EXTRA_GROUP_NAME, group.getGroupName())
                .putExtra(GroupTabsActivity.EXTRA_GROUP_COLOR, group.getGroupColor())
                .putExtra(GroupTabsActivity.EXTRA_IS_PRIVATE, group.isPrivate())
                .putExtra(GroupTabsActivity.EXTRA_CURRENT_TAB_ID, currentTabId)
                .putExtra(GroupTabsActivity.EXTRA_TRANSITION_NAME,
                        GroupTabsActivity.transitionNameForGroup(group.getGroupId()));
        if (group.isPrivate()) {
            putRuntimeTabsExtras(intent, getRuntimeTabsForMode(true));
        }
        groupTabsLauncher.launch(intent);
    }

    @Override
    public void onOpenTab(Tab tab) {
        if (isSelectionMode()) {
            toggleTabSelection(tab);
            return;
        }
        selectedTabId = tab.getId();
        finishWithResult();
    }

    @Override
    public void onRenameGroup(TabGroup group) {
        TabGroupDialogHelper.show(this, R.string.rename_group, group.getGroupName(),
                group.getGroupColor(), true, (name, color) -> {
                    if (group.isPrivate()) {
                        applyRuntimeGroupMetadata(group, name, color);
                        Toast.makeText(this, R.string.group_updated, Toast.LENGTH_SHORT).show();
                        loadGroups();
                        return;
                    }
                    viewModel.renameGroup(group.getGroupId(), name, () ->
                            viewModel.updateGroupColor(group.getGroupId(), color, () -> {
                                groupsChanged = true;
                                Toast.makeText(this, R.string.group_updated, Toast.LENGTH_SHORT).show();
                                loadGroups();
                            }));
                });
    }

    @Override
    public void onDeleteGroup(TabGroup group) {
        String name = group.getGroupName() != null ? group.getGroupName() : getString(R.string.tab_group);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_group)
                .setMessage(getString(R.string.delete_group_confirm, name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ArrayList<Tab> closableTabs = getClosableTabs(group.getTabs());
                    if (closableTabs.isEmpty()) {
                        Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (closableTabs.size() < group.getTabs().size()) {
                        Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
                    }
                    for (Tab tab : closableTabs) {
                        addClosedTabId(tab.getId());
                    }
                    requestOverviewAnimation();
                    if (group.isPrivate()) {
                        removeRuntimeTabs(closableTabs);
                        groupsChanged = true;
                        refreshCounts();
                        loadGroups();
                        return;
                    }
                    if (closableTabs.size() == group.getTabs().size()) {
                        viewModel.deleteGroup(group.getGroupId(), () -> {
                            groupsChanged = true;
                            Toast.makeText(this, R.string.group_deleted, Toast.LENGTH_SHORT).show();
                            loadGroups();
                        });
                        return;
                    }
                    viewModel.deleteTabs(getTabIds(closableTabs), () -> {
                        groupsChanged = true;
                        refreshCounts();
                        loadGroups();
                    });
                })
                .show();
    }

    @Override
    public void onChangeGroupColor(TabGroup group) {
        TabGroupDialogHelper.show(this, R.string.change_group_color, group.getGroupName(),
                group.getGroupColor(), false, (ignoredName, color) -> {
                    if (group.isPrivate()) {
                        applyRuntimeGroupMetadata(group, group.getGroupName(), color);
                        loadGroups();
                        return;
                    }
                    viewModel.updateGroupColor(group.getGroupId(), color, () -> {
                        groupsChanged = true;
                        loadGroups();
                    });
                });
    }

    @Override
    public void onMoveTabToGroup(Tab tab, TabGroup targetGroup) {
        if (tab == null || targetGroup == null || tab.isPrivate() != targetGroup.isPrivate()) {
            return;
        }
        if (tab.isPrivate()) {
            requestOverviewAnimation();
            moveRuntimeTabToGroup(tab, targetGroup);
            return;
        }
        requestOverviewAnimation();
        viewModel.moveTabToGroup(tab.getId(), targetGroup.getGroupId(), () -> {
            groupsChanged = true;
            Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
            refreshCounts();
            loadGroups();
        });
    }

    @Override
    public void onCreateGroupFromTabs(Tab firstTab, Tab secondTab) {
        if (firstTab == null || secondTab == null
                || firstTab.getId().equals(secondTab.getId())
                || firstTab.isPrivate() != secondTab.isPrivate()) {
            return;
        }
        if (firstTab.getGroupId() != null
                && firstTab.getGroupId().equals(secondTab.getGroupId())) {
            return;
        }
        if (firstTab.isPrivate()) {
            requestOverviewAnimation();
            createRuntimeGroupForTabs(firstTab, secondTab);
            return;
        }
        requestOverviewAnimation();
        ArrayList<String> tabIds = new ArrayList<>();
        tabIds.add(firstTab.getId());
        tabIds.add(secondTab.getId());
        viewModel.createGroupForTabs(tabIds,
                getString(R.string.tab_group),
                TabRepository.getDefaultGroupColor(this),
                false,
                () -> {
                    groupsChanged = true;
                    animateGroupCreationFeedback();
                    Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show();
                    refreshCounts();
                    loadGroups();
                });
    }

    @Override
    public void onCloseGroup(TabGroup group) {
        onDeleteGroup(group);
    }

    @Override
    public void onToggleGroupCollapsed(TabGroup group) {
        if (group == null) {
            return;
        }
        boolean collapsed = !appSettings.isGroupCollapsed(group.getGroupId());
        appSettings.setGroupCollapsed(group.getGroupId(), collapsed);
        Transition transition = new AutoTransition();
        transition.setDuration(collapsed ? 190 : EasyMotion.DURATION_MEDIUM);
        transition.setInterpolator(EasyMotion.STANDARD);
        TransitionManager.beginDelayedTransition(binding.groupsRecycler, transition);
        adapter.setCollapsedGroupIds(appSettings.getCollapsedGroupIds());
        Toast.makeText(this,
                collapsed ? R.string.group_collapsed : R.string.group_expanded,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCloseTab(Tab tab) {
        closeTab(tab, true);
    }

    @Override
    public void onGroupLongClick(TabGroup group) {
        toggleGroupSelection(group);
    }

    @Override
    public void onTabLongClick(Tab tab, View anchor) {
        if (isSelectionMode()) {
            toggleTabSelection(tab);
            return;
        }
        showTabActionMenu(tab, anchor);
    }

    @Override
    public void onAddTabToGroup(Tab tab) {
        showAddTabToGroupDialog(tab);
    }

    @Override
    public void onRemoveTabFromGroup(Tab tab) {
        removeTabFromGroup(tab);
    }

    @Override
    public void onBookmarkTab(Tab tab) {
        bookmarkTab(tab);
    }

    @Override
    public void onShareTab(Tab tab) {
        shareTab(tab);
    }

    @Override
    public void onDuplicateTab(Tab tab) {
        restoreUrl = tab.getUrl();
        restorePrivateTab = tab.isPrivate();
        finishWithResult();
    }

    @Override
    public void onPinTab(Tab tab) {
        if (tab == null) {
            return;
        }
        setTabPinned(tab, !tab.isPinned());
    }

    private boolean canReorderOverview() {
        return !isSelectionMode() && (searchQuery == null || searchQuery.trim().isEmpty());
    }

    private void persistOverviewOrder(List<Tab> orderedTabs) {
        if (orderedTabs == null || orderedTabs.size() < 2) {
            return;
        }
        for (int i = 0; i < orderedTabs.size(); i++) {
            orderedTabs.get(i).setPosition(i);
        }
        groupsChanged = true;
        if (privateMode) {
            reorderedPrivateTabIds.clear();
            for (Tab tab : orderedTabs) {
                if (tab.isPrivate()) {
                    reorderedPrivateTabIds.add(tab.getId());
                    Tab runtimeTab = findRuntimeTab(tab.getId());
                    if (runtimeTab != null) {
                        runtimeTab.setPosition(tab.getPosition());
                    }
                }
            }
            return;
        }
        viewModel.updateTabPositions(orderedTabs, () -> groupsChanged = true);
    }

    private void showTabActionMenu(Tab tab, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        addTabMenuItem(menu, 1, R.string.add_tab_to_group, R.drawable.ic_view_grid);
        addTabMenuItem(menu, 7, R.string.duplicate_tab, R.drawable.ic_duplicate_tab);
        addTabMenuItem(menu, 2, R.string.add_to_bookmarks, R.drawable.ic_bookmark_border);
        addTabMenuItem(menu, 3, R.string.share, R.drawable.ic_share);
        addTabMenuItem(menu, 4, tab.isPinned() ? R.string.unpin_tab : R.string.pin_tab,
                R.drawable.ic_pin);
        addTabMenuItem(menu, 8, tab.isLocked() ? R.string.unlock_tab : R.string.lock_tab,
                R.drawable.ic_lock);
        addTabMenuItem(menu, 5, R.string.select_tab, R.drawable.ic_edit);
        addTabMenuItem(menu, 6, R.string.close_tab_sentence, R.drawable.ic_close);
        forceShowMenuIcons(menu);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                onAddTabToGroup(tab);
                return true;
            } else if (item.getItemId() == 7) {
                onDuplicateTab(tab);
                return true;
            } else if (item.getItemId() == 2) {
                onBookmarkTab(tab);
                return true;
            } else if (item.getItemId() == 3) {
                onShareTab(tab);
                return true;
            } else if (item.getItemId() == 4) {
                onPinTab(tab);
                return true;
            } else if (item.getItemId() == 8) {
                setTabLocked(tab, !tab.isLocked());
                return true;
            } else if (item.getItemId() == 5) {
                selectSingleTab(tab);
                return true;
            } else if (item.getItemId() == 6) {
                onCloseTab(tab);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void addTabMenuItem(PopupMenu menu, int itemId, int titleRes, int iconRes) {
        MenuItem item = menu.getMenu().add(Menu.NONE, itemId, Menu.NONE, titleRes);
        item.setIcon(iconRes);
    }

    private void forceShowMenuIcons(PopupMenu menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            menu.setForceShowIcon(true);
            return;
        }
        try {
            java.lang.reflect.Field field = menu.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object helper = field.get(menu);
            java.lang.reflect.Method method = helper.getClass()
                    .getDeclaredMethod("setForceShowIcon", boolean.class);
            method.invoke(helper, true);
        } catch (Exception ignored) {
            // Older platform implementations may not expose this hook.
        }
    }

    private void selectSingleTab(Tab tab) {
        if (tab == null) {
            return;
        }
        selectedGroupIds.clear();
        selectedTabIds.clear();
        selectedTabIds.add(tab.getId());
        selectionMode = true;
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        updateSelectionUi();
    }

    private void setTabPinned(Tab tab, boolean pinned) {
        if (tab == null) {
            return;
        }
        List<Tab> orderedTabs = getRuntimeTabsForMode(tab.isPrivate());
        Tab changedTab = null;
        for (int i = orderedTabs.size() - 1; i >= 0; i--) {
            Tab candidate = orderedTabs.get(i);
            if (tab.getId().equals(candidate.getId())) {
                changedTab = candidate;
                orderedTabs.remove(i);
                break;
            }
        }
        if (changedTab == null) {
            return;
        }
        changedTab.setPinned(pinned);
        recordPinnedMutation(changedTab);
        orderedTabs.add(pinned ? firstUnpinnedIndex(orderedTabs) : orderedTabs.size(), changedTab);
        for (int i = 0; i < orderedTabs.size(); i++) {
            orderedTabs.get(i).setPosition(i);
        }
        groupsChanged = true;
        requestOverviewAnimation();
        if (tab.isPrivate()) {
            reorderedPrivateTabIds.clear();
            for (Tab orderedTab : orderedTabs) {
                reorderedPrivateTabIds.add(orderedTab.getId());
            }
            loadGroups();
            Toast.makeText(this,
                    pinned ? R.string.tab_pinned : R.string.tab_unpinned,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveTab(changedTab, () -> viewModel.updateTabPositions(orderedTabs, () -> {
            groupsChanged = true;
            loadGroups();
            Toast.makeText(this,
                    pinned ? R.string.tab_pinned : R.string.tab_unpinned,
                    Toast.LENGTH_SHORT).show();
        }));
    }

    private void setTabLocked(Tab tab, boolean locked) {
        if (tab == null) {
            return;
        }
        Tab runtimeTab = findRuntimeTab(tab.getId());
        Tab changedTab = runtimeTab != null ? runtimeTab : tab;
        changedTab.setLocked(locked);
        recordLockedMutation(changedTab);
        groupsChanged = true;
        requestOverviewAnimation();
        if (tab.isPrivate()) {
            loadGroups();
            Toast.makeText(this,
                    locked ? R.string.tab_locked : R.string.tab_unlocked,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveTab(changedTab, () -> {
            loadGroups();
            Toast.makeText(this,
                    locked ? R.string.tab_locked : R.string.tab_unlocked,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void recordLockedMutation(Tab tab) {
        if (tab == null) {
            return;
        }
        if (tab.isLocked()) {
            unlockedTabIds.remove(tab.getId());
            if (!lockedTabIds.contains(tab.getId())) {
                lockedTabIds.add(tab.getId());
            }
        } else {
            lockedTabIds.remove(tab.getId());
            if (!unlockedTabIds.contains(tab.getId())) {
                unlockedTabIds.add(tab.getId());
            }
        }
    }

    private int firstUnpinnedIndex(List<Tab> tabs) {
        if (tabs == null) {
            return 0;
        }
        for (int i = 0; i < tabs.size(); i++) {
            if (!tabs.get(i).isPinned()) {
                return i;
            }
        }
        return tabs.size();
    }

    private void recordPinnedMutation(Tab tab) {
        if (tab == null) {
            return;
        }
        if (tab.isPinned()) {
            unpinnedTabIds.remove(tab.getId());
            if (!pinnedTabIds.contains(tab.getId())) {
                pinnedTabIds.add(tab.getId());
            }
        } else {
            pinnedTabIds.remove(tab.getId());
            if (!unpinnedTabIds.contains(tab.getId())) {
                unpinnedTabIds.add(tab.getId());
            }
        }
    }

    private void closeTab(Tab tab, boolean refresh) {
        if (tab == null) {
            return;
        }
        if (tab.isLocked()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (refresh) {
            requestOverviewAnimation();
        }
        pendingClosedTabs.put(tab.getId(), new PendingClosedTab(tab, indexOfRuntimeTab(tab.getId())));
        addClosedTabId(tab.getId());
        removeRuntimeTab(tab.getId());
        showClosedTabUndoSnackbar(tab);
        if (tab.isPrivate()) {
            groupsChanged = true;
            if (refresh) {
                refreshCounts();
                loadGroups();
            }
            return;
        }
        viewModel.deleteTab(tab.getId(), () -> {
            groupsChanged = true;
            if (refresh) {
                refreshCounts();
                loadGroups();
            }
        });
    }

    private void showClosedTabUndoSnackbar(Tab tab) {
        if (!PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsKeys.PREF_SHOW_TAB_UNDO, true)) {
            return;
        }
        if (binding == null || tab == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(binding.getRoot(),
                getString(R.string.closed_tab_snackbar, getTabLabel(tab)),
                Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(binding.fabNewTab);
        snackbar.setAction(R.string.undo, v -> undoClosedTab(tab.getId()));
        snackbar.show();
    }

    private void undoClosedTab(String tabId) {
        PendingClosedTab pending = pendingClosedTabs.remove(tabId);
        if (pending == null) {
            return;
        }
        closedTabIds.remove(tabId);
        insertRuntimeTab(pending.tab, pending.index);
        groupsChanged = true;
        if (pending.tab.isPrivate()) {
            refreshCounts();
            loadGroups();
            return;
        }
        List<Tab> tabsToSave = tabsToPersistForUndo(pending.tab);
        if (tabsToSave.isEmpty()) {
            refreshCounts();
            loadGroups();
            return;
        }
        final int[] remaining = {tabsToSave.size()};
        for (Tab tab : tabsToSave) {
            viewModel.saveTab(tab, () -> {
                remaining[0]--;
                if (remaining[0] == 0) {
                    refreshCounts();
                    loadGroups();
                }
            });
        }
    }

    private List<Tab> tabsToPersistForUndo(Tab restoredTab) {
        List<Tab> tabsToSave = new ArrayList<>();
        if (restoredTab == null || restoredTab.isPrivate()) {
            return tabsToSave;
        }
        String groupId = restoredTab.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            tabsToSave.add(restoredTab);
            return tabsToSave;
        }
        for (Tab tab : runtimeTabs) {
            if (!tab.isPrivate() && groupId.equals(tab.getGroupId())) {
                tabsToSave.add(tab);
            }
        }
        if (tabsToSave.size() < 2) {
            restoredTab.setGroupId(null);
            restoredTab.setGroupName(null);
            restoredTab.setGroupColor(0);
            tabsToSave.clear();
            tabsToSave.add(restoredTab);
        }
        return tabsToSave;
    }

    private String getTabLabel(Tab tab) {
        if (tab.getTitle() != null && !tab.getTitle().trim().isEmpty()) {
            return tab.getTitle();
        }
        return getString(R.string.tab_title_default);
    }

    private void showAddTabToGroupDialog(Tab tab) {
        List<TabGroup> targets = new ArrayList<>();
        for (TabGroup group : currentGroups) {
            if (group.isPrivate() == tab.isPrivate()
                    && (tab.getGroupId() == null || !tab.getGroupId().equals(group.getGroupId()))) {
                targets.add(group);
            }
        }
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.no_tab_groups, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            names[i] = targets.get(i).getGroupName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_to_group)
                .setItems(names, (dialog, which) -> {
                    TabGroup target = targets.get(which);
                    requestOverviewAnimation();
                    if (tab.isPrivate()) {
                        tab.setGroupId(target.getGroupId());
                        tab.setGroupName(target.getGroupName());
                        tab.setGroupColor(target.getGroupColor());
                        recordGroupMutation(tab, target.getGroupId(),
                                target.getGroupName(), target.getGroupColor());
                        groupsChanged = true;
                        refreshCounts();
                        loadGroups();
                        return;
                    }
                    viewModel.moveTabToGroup(tab.getId(), target.getGroupId(), () -> {
                        groupsChanged = true;
                        Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                        refreshCounts();
                        loadGroups();
                    });
                })
                .show();
    }

    private void removeTabFromGroup(Tab tab) {
        if (tab == null || tab.getGroupId() == null) {
            return;
        }
        requestOverviewAnimation();
        if (tab.isPrivate()) {
            tab.setGroupId(null);
            tab.setGroupName(null);
            tab.setGroupColor(0);
            recordGroupMutation(tab, "", "", 0);
            groupsChanged = true;
            refreshCounts();
            loadGroups();
            return;
        }
        viewModel.removeTabFromGroup(tab.getId(), () -> {
            groupsChanged = true;
            refreshCounts();
            loadGroups();
        });
    }

    private void createRuntimeGroupForTabs(Tab firstTab, Tab secondTab) {
        Tab firstRuntimeTab = findRuntimeTab(firstTab.getId());
        Tab secondRuntimeTab = findRuntimeTab(secondTab.getId());
        if (firstRuntimeTab == null || secondRuntimeTab == null) {
            return;
        }
        String groupName = getString(R.string.tab_group);
        int groupColor = TabRepository.getDefaultGroupColor(this);
        String groupId = UUID.randomUUID().toString();
        applyRuntimeTabGroup(firstRuntimeTab, groupId, groupName, groupColor);
        applyRuntimeTabGroup(secondRuntimeTab, groupId, groupName, groupColor);
        recordCreatedPrivateGroup(groupId, groupName, groupColor, firstRuntimeTab, secondRuntimeTab);
        groupsChanged = true;
        animateGroupCreationFeedback();
        Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show();
        refreshCounts();
        loadGroups();
    }

    private void moveRuntimeTabToGroup(Tab tab, TabGroup targetGroup) {
        Tab runtimeTab = findRuntimeTab(tab.getId());
        if (runtimeTab == null) {
            return;
        }
        applyRuntimeTabGroup(runtimeTab, targetGroup.getGroupId(),
                targetGroup.getGroupName(), targetGroup.getGroupColor());
        recordGroupMutation(runtimeTab, targetGroup.getGroupId(),
                targetGroup.getGroupName(), targetGroup.getGroupColor());
        groupsChanged = true;
        Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
        refreshCounts();
        loadGroups();
    }

    private void applyRuntimeTabGroup(Tab tab, String groupId, String groupName, int groupColor) {
        tab.setGroupId(groupId);
        tab.setGroupName(groupName);
        tab.setGroupColor(groupColor);
    }

    private void applyRuntimeGroupMetadata(TabGroup group, String groupName, int groupColor) {
        if (group == null) {
            return;
        }
        for (Tab tab : group.getTabs()) {
            Tab runtimeTab = findRuntimeTab(tab.getId());
            if (runtimeTab != null) {
                applyRuntimeTabGroup(runtimeTab, group.getGroupId(), groupName, groupColor);
                recordGroupMutation(runtimeTab, group.getGroupId(), groupName, groupColor);
            }
        }
        groupsChanged = true;
    }

    private void bookmarkTab(Tab tab) {
        if (tab == null || tab.getUrl() == null || tab.getUrl().trim().isEmpty()) {
            return;
        }
        Bookmark bookmark = new Bookmark(
                tab.getTitle() != null && !tab.getTitle().trim().isEmpty()
                        ? tab.getTitle()
                        : tab.getUrl(),
                tab.getUrl());
        bookmark.setFavicon(tab.getFaviconUri());
        bookmarkRepository.addBookmark(bookmark, new BookmarkRepository.BookmarkCallback() {
            @Override public void onBookmarksLoaded(List<Bookmark> bookmarks) {}
            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
                runOnUiThread(() -> Toast.makeText(TabManagerActivity.this,
                        R.string.bookmark_added_message, Toast.LENGTH_SHORT).show());
            }
            @Override public void onBookmarkRemoved(Bookmark bookmark) {}
        });
    }

    private void shareTab(Tab tab) {
        if (tab == null || tab.getUrl() == null) {
            return;
        }
        if (tab.isPrivate()) {
            Toast.makeText(this, R.string.private_share_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, tab.getUrl());
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private void addClosedTabId(String tabId) {
        if (tabId != null && !closedTabIds.contains(tabId)) {
            closedTabIds.add(tabId);
        }
    }

    private ArrayList<Tab> getClosableTabs(List<Tab> tabs) {
        ArrayList<Tab> closableTabs = new ArrayList<>();
        if (tabs == null) {
            return closableTabs;
        }
        for (Tab tab : tabs) {
            if (tab != null && !tab.isLocked()) {
                closableTabs.add(tab);
            }
        }
        return closableTabs;
    }

    private ArrayList<String> getTabIds(List<Tab> tabs) {
        ArrayList<String> ids = new ArrayList<>();
        if (tabs == null) {
            return ids;
        }
        for (Tab tab : tabs) {
            if (tab != null) {
                addUniqueTabId(ids, tab.getId());
            }
        }
        return ids;
    }

    private void addUniqueTabId(ArrayList<String> tabIds, String tabId) {
        if (tabId != null && !tabIds.contains(tabId)) {
            tabIds.add(tabId);
        }
    }

    private boolean isTabLocked(String tabId) {
        Tab runtimeTab = findRuntimeTab(tabId);
        if (runtimeTab != null) {
            return runtimeTab.isLocked();
        }
        for (Tab tab : currentStandaloneTabs) {
            if (tabId != null && tabId.equals(tab.getId())) {
                return tab.isLocked();
            }
        }
        for (TabGroup group : currentGroups) {
            for (Tab tab : group.getTabs()) {
                if (tabId != null && tabId.equals(tab.getId())) {
                    return tab.isLocked();
                }
            }
        }
        return false;
    }

    private void removeRuntimeTab(String tabId) {
        if (tabId == null) {
            return;
        }
        for (int i = runtimeTabs.size() - 1; i >= 0; i--) {
            if (tabId.equals(runtimeTabs.get(i).getId())) {
                runtimeTabs.remove(i);
            }
        }
    }

    private int indexOfRuntimeTab(String tabId) {
        if (tabId == null) {
            return runtimeTabs.size();
        }
        for (int i = 0; i < runtimeTabs.size(); i++) {
            if (tabId.equals(runtimeTabs.get(i).getId())) {
                return i;
            }
        }
        return runtimeTabs.size();
    }

    private void insertRuntimeTab(Tab tab, int index) {
        if (tab == null || findRuntimeTab(tab.getId()) != null) {
            return;
        }
        int boundedIndex = Math.max(0, Math.min(index, runtimeTabs.size()));
        runtimeTabs.add(boundedIndex, tab);
    }

    private void removeRuntimeTabs(List<Tab> tabs) {
        if (tabs == null) {
            return;
        }
        for (Tab tab : tabs) {
            removeRuntimeTab(tab.getId());
        }
    }

    private void recordGroupMutation(Tab tab, String groupId, String groupName, int groupColor) {
        groupMutationTabIds.add(tab.getId());
        groupMutationGroupIds.add(groupId != null ? groupId : "");
        groupMutationGroupNames.add(groupName != null ? groupName : "");
        groupMutationGroupColors.add(groupColor);
    }

    private void recordCreatedPrivateGroup(String groupId, String groupName, int groupColor,
                                           Tab firstTab, Tab secondTab) {
        ArrayList<Tab> tabs = new ArrayList<>();
        tabs.add(firstTab);
        tabs.add(secondTab);
        recordCreatedPrivateGroup(groupId, groupName, groupColor, tabs);
    }

    private void recordCreatedPrivateGroup(String groupId, String groupName, int groupColor,
                                           List<Tab> tabs) {
        if (tabs == null || tabs.size() < 2) {
            return;
        }
        createdPrivateGroupIds.add(groupId);
        createdPrivateGroupNames.add(groupName != null ? groupName : "");
        createdPrivateGroupColors.add(groupColor);
        createdPrivateGroupTabCounts.add(tabs.size());
        for (Tab tab : tabs) {
            createdPrivateGroupTabIds.add(tab.getId());
        }
    }

    private void appendCreatedPrivateGroups(Intent data) {
        ArrayList<String> groupIds =
                data.getStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_IDS);
        ArrayList<String> tabIds =
                data.getStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_TAB_IDS);
        ArrayList<Integer> tabCounts =
                data.getIntegerArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_TAB_COUNTS);
        ArrayList<String> groupNames =
                data.getStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_NAMES);
        ArrayList<Integer> groupColors =
                data.getIntegerArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_COLORS);
        if (groupIds == null || tabIds == null || tabCounts == null
                || groupNames == null || groupColors == null) {
            return;
        }
        createdPrivateGroupIds.addAll(groupIds);
        createdPrivateGroupTabIds.addAll(tabIds);
        createdPrivateGroupTabCounts.addAll(tabCounts);
        createdPrivateGroupNames.addAll(groupNames);
        createdPrivateGroupColors.addAll(groupColors);
        int cursor = 0;
        int count = Math.min(Math.min(groupIds.size(), tabCounts.size()),
                Math.min(groupNames.size(), groupColors.size()));
        for (int i = 0; i < count; i++) {
            int tabCount = Math.max(0, tabCounts.get(i));
            if (cursor + tabCount > tabIds.size()) {
                break;
            }
            for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                Tab tab = findRuntimeTab(tabIds.get(cursor + tabIndex));
                if (tab != null) {
                    tab.setGroupId(groupIds.get(i));
                    tab.setGroupName(groupNames.get(i));
                    tab.setGroupColor(groupColors.get(i));
                }
            }
            cursor += tabCount;
        }
    }

    private void appendGroupMutations(Intent data) {
        ArrayList<String> tabIds = data.getStringArrayListExtra(RESULT_GROUP_MUTATION_TAB_IDS);
        ArrayList<String> groupIds = data.getStringArrayListExtra(RESULT_GROUP_MUTATION_GROUP_IDS);
        ArrayList<String> groupNames = data.getStringArrayListExtra(RESULT_GROUP_MUTATION_GROUP_NAMES);
        ArrayList<Integer> groupColors = data.getIntegerArrayListExtra(RESULT_GROUP_MUTATION_GROUP_COLORS);
        if (tabIds == null || groupIds == null || groupNames == null || groupColors == null) {
            return;
        }
        groupMutationTabIds.addAll(tabIds);
        groupMutationGroupIds.addAll(groupIds);
        groupMutationGroupNames.addAll(groupNames);
        groupMutationGroupColors.addAll(groupColors);
        int count = Math.min(Math.min(tabIds.size(), groupIds.size()),
                Math.min(groupNames.size(), groupColors.size()));
        for (int i = 0; i < count; i++) {
            Tab tab = findRuntimeTab(tabIds.get(i));
            if (tab == null) {
                continue;
            }
            String groupId = groupIds.get(i);
            if (groupId == null || groupId.trim().isEmpty()) {
                tab.setGroupId(null);
                tab.setGroupName(null);
                tab.setGroupColor(0);
            } else {
                tab.setGroupId(groupId);
                tab.setGroupName(groupNames.get(i));
                tab.setGroupColor(groupColors.get(i));
            }
        }
    }

    private void appendReorderedPrivateTabs(Intent data) {
        ArrayList<String> tabIds = data.getStringArrayListExtra(RESULT_REORDERED_PRIVATE_TAB_IDS);
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        reorderedPrivateTabIds.clear();
        reorderedPrivateTabIds.addAll(tabIds);
        for (int i = 0; i < tabIds.size(); i++) {
            Tab tab = findRuntimeTab(tabIds.get(i));
            if (tab != null) {
                tab.setPosition(i);
            }
        }
    }

    private void appendPinMutations(Intent data) {
        appendPinMutationIds(data.getStringArrayListExtra(RESULT_PINNED_TAB_IDS), true);
        appendPinMutationIds(data.getStringArrayListExtra(RESULT_UNPINNED_TAB_IDS), false);
    }

    private void appendLockMutations(Intent data) {
        appendLockMutationIds(data.getStringArrayListExtra(RESULT_LOCKED_TAB_IDS), true);
        appendLockMutationIds(data.getStringArrayListExtra(RESULT_UNLOCKED_TAB_IDS), false);
    }

    private void appendLockMutationIds(List<String> tabIds, boolean locked) {
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        for (String tabId : tabIds) {
            if (locked) {
                unlockedTabIds.remove(tabId);
                if (!lockedTabIds.contains(tabId)) {
                    lockedTabIds.add(tabId);
                }
            } else {
                lockedTabIds.remove(tabId);
                if (!unlockedTabIds.contains(tabId)) {
                    unlockedTabIds.add(tabId);
                }
            }
            Tab runtimeTab = findRuntimeTab(tabId);
            if (runtimeTab != null) {
                runtimeTab.setLocked(locked);
            }
        }
    }

    private void appendPinMutationIds(List<String> tabIds, boolean pinned) {
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        for (String tabId : tabIds) {
            if (pinned) {
                unpinnedTabIds.remove(tabId);
                if (!pinnedTabIds.contains(tabId)) {
                    pinnedTabIds.add(tabId);
                }
            } else {
                pinnedTabIds.remove(tabId);
                if (!unpinnedTabIds.contains(tabId)) {
                    unpinnedTabIds.add(tabId);
                }
            }
            Tab runtimeTab = findRuntimeTab(tabId);
            if (runtimeTab != null) {
                runtimeTab.setPinned(pinned);
            }
        }
    }

    private void putRuntimeTabsExtras(Intent intent, List<Tab> tabs) {
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> groupNames = new ArrayList<>();
        ArrayList<String> groupIds = new ArrayList<>();
        ArrayList<String> thumbnails = new ArrayList<>();
        ArrayList<String> favicons = new ArrayList<>();
        ArrayList<String> parentIds = new ArrayList<>();
        boolean[] privateStates = new boolean[tabs.size()];
        boolean[] pinnedStates = new boolean[tabs.size()];
        boolean[] lockedStates = new boolean[tabs.size()];
        int[] groupColors = new int[tabs.size()];
        int[] positions = new int[tabs.size()];
        long[] createdAt = new long[tabs.size()];
        long[] lastAccessed = new long[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            ids.add(tab.getId());
            titles.add(tab.getTitle());
            urls.add(tab.getUrl());
            groupNames.add(tab.getGroupName() != null ? tab.getGroupName() : "");
            groupIds.add(tab.getGroupId() != null ? tab.getGroupId() : "");
            thumbnails.add(tab.getThumbnailPath() != null ? tab.getThumbnailPath() : "");
            favicons.add(tab.getFaviconUri() != null ? tab.getFaviconUri() : "");
            parentIds.add(tab.getParentTabId() != null ? tab.getParentTabId() : "");
            privateStates[i] = tab.isPrivate();
            pinnedStates[i] = tab.isPinned();
            lockedStates[i] = tab.isLocked();
            groupColors[i] = tab.getGroupColor();
            positions[i] = tab.getPosition();
            createdAt[i] = tab.getCreatedAt();
            lastAccessed[i] = tab.getLastAccessed();
        }
        intent.putStringArrayListExtra(TabsActivity.EXTRA_TAB_IDS, ids)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_TITLES, titles)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_URLS, urls)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUPS, groupNames)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUP_IDS, groupIds)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_THUMBNAILS, thumbnails)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_FAVICONS, favicons)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_PARENT_IDS, parentIds)
                .putExtra(TabsActivity.EXTRA_TAB_PRIVATE_STATES, privateStates)
                .putExtra(TabsActivity.EXTRA_TAB_PINNED_STATES, pinnedStates)
                .putExtra(TabsActivity.EXTRA_TAB_LOCKED_STATES, lockedStates)
                .putExtra(TabsActivity.EXTRA_TAB_GROUP_COLORS, groupColors)
                .putExtra(TabsActivity.EXTRA_TAB_POSITIONS, positions)
                .putExtra(TabsActivity.EXTRA_TAB_CREATED_AT, createdAt)
                .putExtra(TabsActivity.EXTRA_TAB_LAST_ACCESSED, lastAccessed);
    }

    private List<Tab> getRuntimeTabsForMode(boolean isPrivate) {
        List<Tab> tabs = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (tab.isPrivate() == isPrivate && !closedTabIds.contains(tab.getId())) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    private Tab findRuntimeTab(String tabId) {
        if (tabId == null) {
            return null;
        }
        for (Tab tab : runtimeTabs) {
            if (tabId.equals(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    private void toggleGroupSelection(TabGroup group) {
        if (selectedGroupIds.contains(group.getGroupId())) {
            selectedGroupIds.remove(group.getGroupId());
        } else {
            selectedGroupIds.add(group.getGroupId());
        }
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        updateSelectionUi();
    }

    private void toggleTabSelection(Tab tab) {
        if (selectedTabIds.contains(tab.getId())) {
            selectedTabIds.remove(tab.getId());
        } else {
            selectedTabIds.add(tab.getId());
        }
        adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        updateSelectionUi();
    }

    private void clearSelection() {
        selectedGroupIds.clear();
        selectedTabIds.clear();
        selectionMode = false;
        if (adapter != null) {
            adapter.setSelectedIds(selectedGroupIds, selectedTabIds);
        }
        updateSelectionUi();
    }

    private void updateSelectionUi() {
        boolean selecting = isSelectionMode();
        binding.selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        binding.topBar.setVisibility(selecting ? View.GONE : View.VISIBLE);
        binding.searchLayout.setVisibility(selecting ? View.GONE : View.VISIBLE);
        binding.viewToggle.setVisibility(selecting ? View.GONE : View.VISIBLE);
        if (selecting) {
            binding.selectionCount.setText(getString(R.string.selected_tabs_count,
                    selectedGroupIds.size() + selectedTabIds.size()));
        }
    }

    private void showTabManagerActionMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        addTabMenuItem(menu, TAB_MENU_NEW_TAB, R.string.new_tab, R.drawable.ic_add);
        addTabMenuItem(menu, TAB_MENU_NEW_PRIVATE_TAB, R.string.new_private_tab,
                R.drawable.ic_incognito);
        addTabMenuItem(menu, TAB_MENU_NEW_TAB_GROUP, R.string.new_tab_group,
                R.drawable.ic_view_grid);
        addTabMenuItem(menu, TAB_MENU_CLOSE_ALL, R.string.close_all_tabs,
                R.drawable.ic_close);
        addTabMenuItem(menu, TAB_MENU_SELECT_TABS, R.string.select_tabs,
                R.drawable.ic_check);
        addTabMenuItem(menu, TAB_MENU_DELETE_BROWSING_DATA, R.string.delete_browsing_data,
                R.drawable.ic_clear);
        addTabMenuItem(menu, TAB_MENU_SETTINGS, R.string.settings,
                R.drawable.ic_settings);
        forceShowMenuIcons(menu);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case TAB_MENU_NEW_TAB:
                    createPrivateTab = false;
                    finishWithResult();
                    return true;
                case TAB_MENU_NEW_PRIVATE_TAB:
                    createPrivateTab = true;
                    finishWithResult();
                    return true;
                case TAB_MENU_NEW_TAB_GROUP:
                    createPrivateTab = privateMode;
                    createTabGroupId = UUID.randomUUID().toString();
                    createTabGroupName = getString(R.string.tab_group);
                    createTabGroupColor = TabRepository.getDefaultGroupColor(this);
                    groupsChanged = true;
                    finishWithResult();
                    return true;
                case TAB_MENU_CLOSE_ALL:
                    confirmCloseAllTabs();
                    return true;
                case TAB_MENU_SELECT_TABS:
                    selectionMode = true;
                    updateSelectionUi();
                    return true;
                case TAB_MENU_DELETE_BROWSING_DATA:
                    startActivity(new Intent(this, SettingsActivity.class)
                            .putExtra(SettingsActivity.EXTRA_OPEN_CLEAR_DATA, true));
                    return true;
                case TAB_MENU_SETTINGS:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void confirmCloseAllTabs() {
        if (allTabIds.isEmpty()) {
            return;
        }
        ArrayList<String> closableIds = new ArrayList<>();
        for (String tabId : allTabIds) {
            if (!isTabLocked(tabId)) {
                closableIds.add(tabId);
            }
        }
        if (closableIds.isEmpty()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.close_all_tabs)
                .setMessage(R.string.close_all_tabs_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.close_all_tabs, (dialog, which) -> {
                    if (closableIds.size() < allTabIds.size()) {
                        Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
                    }
                    closedTabIds.clear();
                    closedTabIds.addAll(closableIds);
                    groupsChanged = true;
                    finishWithResult();
                })
                .show();
    }

    private void deleteSelectedGroups() {
        if (!isSelectionMode()) {
            return;
        }
        ArrayList<String> selectedGroupIdsToDelete = new ArrayList<>();
        ArrayList<String> selectedTabIdsToDelete = new ArrayList<>();
        ArrayList<Tab> privateTabsToRemove = new ArrayList<>();
        boolean skippedLockedTabs = false;
        for (TabGroup group : getSelectedGroups()) {
            ArrayList<Tab> closableTabs = getClosableTabs(group.getTabs());
            if (closableTabs.size() < group.getTabs().size()) {
                skippedLockedTabs = true;
            }
            if (closableTabs.isEmpty()) {
                continue;
            }
            boolean deleteWholeGroup = closableTabs.size() == group.getTabs().size();
            for (Tab tab : closableTabs) {
                addClosedTabId(tab.getId());
                if (!deleteWholeGroup) {
                    addUniqueTabId(selectedTabIdsToDelete, tab.getId());
                }
            }
            if (deleteWholeGroup) {
                addUniqueTabId(selectedGroupIdsToDelete, group.getGroupId());
            }
            privateTabsToRemove.addAll(closableTabs);
        }
        for (String selectedTabId : selectedTabIds) {
            if (isTabLocked(selectedTabId)) {
                skippedLockedTabs = true;
                continue;
            }
            addClosedTabId(selectedTabId);
            addUniqueTabId(selectedTabIdsToDelete, selectedTabId);
            Tab selectedTab = findRuntimeTab(selectedTabId);
            if (selectedTab != null) {
                privateTabsToRemove.add(selectedTab);
            }
        }
        if (selectedGroupIdsToDelete.isEmpty() && selectedTabIdsToDelete.isEmpty()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (skippedLockedTabs) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
        }
        requestOverviewAnimation();
        if (privateMode) {
            removeRuntimeTabs(privateTabsToRemove);
            groupsChanged = true;
            clearSelection();
            refreshCounts();
            loadGroups();
            return;
        }
        Runnable afterDelete = () -> {
            groupsChanged = true;
            clearSelection();
            refreshCounts();
            loadGroups();
        };
        if (!selectedGroupIdsToDelete.isEmpty()) {
            viewModel.deleteGroups(selectedGroupIdsToDelete, () -> {
                if (!selectedTabIdsToDelete.isEmpty()) {
                    viewModel.deleteTabs(selectedTabIdsToDelete, afterDelete);
                } else {
                    afterDelete.run();
                }
            });
        } else {
            viewModel.deleteTabs(selectedTabIdsToDelete, afterDelete);
        }
    }

    private void mergeSelectedGroups() {
        List<TabGroup> selected = getSelectedGroups();
        if (selected.size() == 1 && !selectedTabIds.isEmpty()) {
            TabGroup target = selected.get(0);
            requestOverviewAnimation();
            viewModel.moveTabsToGroup(new ArrayList<>(selectedTabIds), target.getGroupId(), () -> {
                groupsChanged = true;
                clearSelection();
                Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                loadGroups();
            });
            return;
        }
        if (selectedTabIds.size() >= 2) {
            requestOverviewAnimation();
            viewModel.createGroupForTabs(new ArrayList<>(selectedTabIds),
                    getString(R.string.tab_group),
                    TabRepository.getDefaultGroupColor(this),
                    privateMode,
                    () -> {
                        groupsChanged = true;
                        clearSelection();
                        animateGroupCreationFeedback();
                        Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show();
                        loadGroups();
                    });
            return;
        }
        if (selected.size() < 2) {
            return;
        }
        TabGroup target = selected.get(0);
        ArrayList<String> sources = new ArrayList<>();
        for (TabGroup group : selected) {
            sources.add(group.getGroupId());
        }
        requestOverviewAnimation();
        viewModel.moveGroupsToGroup(sources, target.getGroupId(), true, () -> {
            groupsChanged = true;
            clearSelection();
            Toast.makeText(this, R.string.groups_merged, Toast.LENGTH_SHORT).show();
            loadGroups();
        });
    }

    private void moveSelectedGroups() {
        List<TabGroup> targets = new ArrayList<>();
        for (TabGroup group : currentGroups) {
            if (!selectedGroupIds.contains(group.getGroupId())) {
                targets.add(group);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        String[] names = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            names[i] = targets.get(i).getGroupName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.move_to_group)
                .setItems(names, (dialog, which) -> {
                    TabGroup target = targets.get(which);
                    if (!selectedTabIds.isEmpty() && selectedGroupIds.isEmpty()) {
                        requestOverviewAnimation();
                        viewModel.moveTabsToGroup(new ArrayList<>(selectedTabIds),
                                target.getGroupId(), () -> {
                                    groupsChanged = true;
                                    clearSelection();
                                    Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                                    loadGroups();
                                });
                        return;
                    }
                    requestOverviewAnimation();
                    viewModel.moveGroupsToGroup(new ArrayList<>(selectedGroupIds),
                            target.getGroupId(), true, () -> {
                                groupsChanged = true;
                                clearSelection();
                                Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                                loadGroups();
                            });
                })
                .show();
    }

    private void archiveSelectedGroups() {
        if (selectedGroupIds.isEmpty()) {
            return;
        }
        TabGroup archive = new TabGroup(getString(R.string.archive),
                TabRepository.getDefaultGroupColor(this));
        archive.setPrivate(privateMode);
        viewModel.saveGroup(archive, () -> viewModel.moveGroupsToGroup(
                new ArrayList<>(selectedGroupIds), archive.getGroupId(), true, () -> {
                    groupsChanged = true;
                    clearSelection();
                    Toast.makeText(this, R.string.groups_archived, Toast.LENGTH_SHORT).show();
                    loadGroups();
                }));
    }

    private List<TabGroup> getSelectedGroups() {
        List<TabGroup> selected = new ArrayList<>();
        for (TabGroup group : currentGroups) {
            if (selectedGroupIds.contains(group.getGroupId())) {
                selected.add(group);
            }
        }
        return selected;
    }

    private boolean isSelectionMode() {
        return selectionMode || !selectedGroupIds.isEmpty() || !selectedTabIds.isEmpty();
    }

    private void finishWithResult() {
        setTabsResult();
        finish();
    }

    private void setTabsResult() {
        Intent result = new Intent();
        result.putExtra(TabActionContract.EXTRA_ACTIONS, createResultActionsPayload());
        result.putStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS, closedTabIds);
        result.putExtra(RESULT_GROUPS_CHANGED, groupsChanged);
        if (selectedTabId != null) {
            result.putExtra(TabsActivity.RESULT_SELECTED_TAB_ID, selectedTabId);
        }
        if (restoreUrl != null) {
            result.putExtra(TabsActivity.RESULT_RESTORE_URL, restoreUrl);
            result.putExtra(TabsActivity.RESULT_RESTORE_PRIVATE, restorePrivateTab);
        }
        if (createPrivateTab != null) {
            result.putExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, createPrivateTab);
            if (createTabGroupId != null) {
                result.putExtra(GroupTabsActivity.EXTRA_GROUP_ID, createTabGroupId);
                result.putExtra(GroupTabsActivity.EXTRA_GROUP_NAME, createTabGroupName);
                result.putExtra(GroupTabsActivity.EXTRA_GROUP_COLOR, createTabGroupColor);
            }
        }
        if (!groupMutationTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_GROUP_MUTATION_TAB_IDS, groupMutationTabIds);
            result.putStringArrayListExtra(RESULT_GROUP_MUTATION_GROUP_IDS, groupMutationGroupIds);
            result.putStringArrayListExtra(RESULT_GROUP_MUTATION_GROUP_NAMES, groupMutationGroupNames);
            result.putIntegerArrayListExtra(RESULT_GROUP_MUTATION_GROUP_COLORS, groupMutationGroupColors);
        }
        if (!createdPrivateGroupIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_IDS, createdPrivateGroupIds);
            result.putStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_TAB_IDS, createdPrivateGroupTabIds);
            result.putIntegerArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_TAB_COUNTS, createdPrivateGroupTabCounts);
            result.putStringArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_NAMES, createdPrivateGroupNames);
            result.putIntegerArrayListExtra(RESULT_CREATED_PRIVATE_GROUP_COLORS, createdPrivateGroupColors);
        }
        if (!reorderedPrivateTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_REORDERED_PRIVATE_TAB_IDS, reorderedPrivateTabIds);
        }
        if (!pinnedTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_PINNED_TAB_IDS, pinnedTabIds);
        }
        if (!unpinnedTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_UNPINNED_TAB_IDS, unpinnedTabIds);
        }
        if (!lockedTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_LOCKED_TAB_IDS, lockedTabIds);
        }
        if (!unlockedTabIds.isEmpty()) {
            result.putStringArrayListExtra(RESULT_UNLOCKED_TAB_IDS, unlockedTabIds);
        }
        if (!restoredInactiveTabIds.isEmpty()) {
            result.putStringArrayListExtra(InactiveTabsActivity.RESULT_RESTORED_TAB_IDS, restoredInactiveTabIds);
        }
        setResult(RESULT_OK, result);
        resultSet = true;
    }

    private String createResultActionsPayload() {
        ArrayList<TabActionContract.Action> actions = new ArrayList<>();
        if (!closedTabIds.isEmpty()) {
            actions.add(TabActionContract.closeTabs(closedTabIds));
        }
        if (!restoredInactiveTabIds.isEmpty()) {
            actions.add(TabActionContract.restoreInactiveTabs(restoredInactiveTabIds));
        }
        if (createPrivateTab != null) {
            actions.add(TabActionContract.createTab(createPrivateTab,
                    createTabGroupId,
                    createTabGroupName,
                    createTabGroupColor));
        }
        if (restoreUrl != null) {
            actions.add(TabActionContract.restoreUrl(restoreUrl, restorePrivateTab));
        }
        if (!createdPrivateGroupIds.isEmpty()) {
            int cursor = 0;
            int count = Math.min(Math.min(createdPrivateGroupIds.size(),
                            createdPrivateGroupTabCounts.size()),
                    Math.min(createdPrivateGroupNames.size(), createdPrivateGroupColors.size()));
            for (int i = 0; i < count; i++) {
                int tabCount = Math.max(0, createdPrivateGroupTabCounts.get(i));
                if (cursor + tabCount > createdPrivateGroupTabIds.size()) {
                    break;
                }
                ArrayList<String> groupTabIds = new ArrayList<>();
                for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                    groupTabIds.add(createdPrivateGroupTabIds.get(cursor + tabIndex));
                }
                cursor += tabCount;
                actions.add(TabActionContract.createPrivateGroup(
                        createdPrivateGroupIds.get(i),
                        createdPrivateGroupNames.get(i),
                        createdPrivateGroupColors.get(i),
                        groupTabIds));
            }
        }
        if (!groupMutationTabIds.isEmpty()) {
            int count = Math.min(Math.min(groupMutationTabIds.size(), groupMutationGroupIds.size()),
                    Math.min(groupMutationGroupNames.size(), groupMutationGroupColors.size()));
            for (int i = 0; i < count; i++) {
                actions.add(TabActionContract.setGroup(
                        groupMutationTabIds.get(i),
                        groupMutationGroupIds.get(i),
                        groupMutationGroupNames.get(i),
                        groupMutationGroupColors.get(i)));
            }
        }
        if (!reorderedPrivateTabIds.isEmpty()) {
            actions.add(TabActionContract.reorderPrivateTabs(reorderedPrivateTabIds));
        }
        if (!pinnedTabIds.isEmpty()) {
            actions.add(TabActionContract.setPinned(pinnedTabIds, true));
        }
        if (!unpinnedTabIds.isEmpty()) {
            actions.add(TabActionContract.setPinned(unpinnedTabIds, false));
        }
        if (!lockedTabIds.isEmpty()) {
            actions.add(TabActionContract.setLocked(lockedTabIds, true));
        }
        if (!unlockedTabIds.isEmpty()) {
            actions.add(TabActionContract.setLocked(unlockedTabIds, false));
        }
        if (groupsChanged) {
            actions.add(TabActionContract.groupsChanged());
        }
        if (selectedTabId != null) {
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
}
