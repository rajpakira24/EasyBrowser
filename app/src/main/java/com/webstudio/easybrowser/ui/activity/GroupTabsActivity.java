package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.GroupTabsAdapter;
import com.webstudio.easybrowser.adapters.TabItemTouchHelperCallback;
import com.webstudio.easybrowser.databinding.ActivityGroupTabsBinding;
import com.webstudio.easybrowser.models.Bookmark;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.EasyMotion;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SystemBarUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GroupTabsActivity extends AppCompatActivity implements GroupTabsAdapter.Listener {
    public static final String TRANSITION_GROUP_CARD_PREFIX = "group_card_transition_";
    public static final String EXTRA_TRANSITION_NAME = "transition_name";
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_GROUP_NAME = "group_name";
    public static final String EXTRA_GROUP_COLOR = "group_color";
    public static final String EXTRA_IS_PRIVATE = "is_private";
    public static final String EXTRA_CURRENT_TAB_ID = "current_tab_id";
    private static final int MENU_SELECT_TABS = 1;
    private static final int MENU_EDIT_GROUP_NAME = 2;
    private static final int MENU_EDIT_GROUP_COLOR = 3;
    private static final int MENU_CLOSE_GROUP = 4;
    private static final int MENU_DELETE_GROUP = 5;
    private static final int SELECTION_MENU_SELECT_ALL = 10;
    private static final int SELECTION_MENU_CLOSE_TABS = 11;
    private static final int SELECTION_MENU_UNGROUP_TABS = 12;
    private static final int SELECTION_MENU_BOOKMARK_TABS = 13;
    private static final int SELECTION_MENU_SHARE_TABS = 14;
    private static final int TAB_MENU_MOVE_TO_GROUP = 20;
    private static final int TAB_MENU_BOOKMARK = 21;
    private static final int TAB_MENU_SHARE = 22;
    private static final int TAB_MENU_PIN = 23;
    private static final int TAB_MENU_SELECT = 24;
    private static final int TAB_MENU_CLOSE = 25;
    private static final int TAB_MENU_DUPLICATE = 26;
    private static final int TAB_MENU_LOCK = 27;

    private ActivityGroupTabsBinding binding;
    private GroupTabsViewModel viewModel;
    private BookmarkRepository bookmarkRepository;
    private GroupTabsAdapter adapter;
    private final ArrayList<String> closedTabIds = new ArrayList<>();
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
    private final List<Tab> runtimeTabs = new ArrayList<>();
    private final List<Tab> currentTabs = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private String groupId;
    private String groupName;
    private int groupColor;
    private boolean privateGroup;
    private String currentTabId;
    private String selectedTabId;
    private String restoreUrl;
    private boolean restorePrivateTab;
    private Boolean createPrivateTab;
    private boolean createTabInCurrentGroup;
    private boolean groupsChanged;
    private boolean resultSet;
    private boolean selectionMode;
    private boolean initialTabsAnimated;
    private boolean animateNextTabListChange;
    private String currentTabCountText;
    private boolean editingGroupTitle;
    private String inlineRenameOriginalText;
    private PopupWindow colorPopup;
    private PopupMenu activeTabActionMenu;
    private ItemTouchHelper tabsItemTouchHelper;
    private boolean groupTabDragInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureOverlayWindow();
        setupWindowTransitions();
        ScreenshotProtection.apply(this);
        binding = ActivityGroupTabsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupOverlayDismiss();
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);
        groupColor = getIntent().getIntExtra(EXTRA_GROUP_COLOR,
                TabRepository.getDefaultGroupColor(this));
        privateGroup = getIntent().getBooleanExtra(EXTRA_IS_PRIVATE, false);
        currentTabId = getIntent().getStringExtra(EXTRA_CURRENT_TAB_ID);
        if (groupId == null) {
            finish();
            return;
        }
        String transitionName = getIntent().getStringExtra(EXTRA_TRANSITION_NAME);
        ViewCompat.setTransitionName(binding.groupHeader,
                transitionName != null ? transitionName : transitionNameForGroup(groupId));
        restoreRuntimeTabsFromIntent(getIntent());
        viewModel = new ViewModelProvider(this).get(GroupTabsViewModel.class);
        bookmarkRepository = new BookmarkRepository(this);
        setupRecycler();
        setupToolbar();
        animateOverlayIn();
        loadTabs();
    }

    private void setupToolbar() {
        binding.groupTitle.setText(R.string.tab_group);
        tintCircle(binding.groupColorIndicator, groupColor);
        binding.groupColorIndicator.setOnClickListener(v -> showChangeGroupColorDialog());
        binding.groupTitle.setOnClickListener(v -> showRenameGroupDialog());
        binding.groupTitleInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN);
            if (done) {
                finishInlineRename(true);
                return true;
            }
            return false;
        });
        binding.groupTitleInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && editingGroupTitle) {
                finishInlineRename(true);
            }
        });
        binding.backButton.setOnClickListener(v -> {
            if (!selectedIds.isEmpty()) {
                clearSelection();
            } else if (editingGroupTitle) {
                finishInlineRename(true);
            } else {
                finishWithTransition();
            }
        });
        binding.addTabButton.setOnClickListener(this::showNewTabMenu);
        binding.addTabButton.setOnLongClickListener(v -> {
            showAddLongPressMenu(v);
            return true;
        });
        binding.groupMenuButton.setOnClickListener(this::showGroupMenu);
        updateSelectionUi();
    }

    public static String transitionNameForGroup(String groupId) {
        return TRANSITION_GROUP_CARD_PREFIX + (groupId != null ? groupId : "");
    }

    private void setupRecycler() {
        adapter = new GroupTabsAdapter(this);
        adapter.setCurrentTabId(currentTabId);
        adapter.setGroupColor(groupColor);
        binding.tabsRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        binding.tabsRecycler.setAdapter(adapter);
        EasyMotion.configurePremiumItemAnimator(binding.tabsRecycler);
        binding.tabsRecycler.setAlpha(0f);
        binding.tabsRecycler.setTranslationY(dp(12));
        tabsItemTouchHelper = new ItemTouchHelper(new TabItemTouchHelperCallback(adapter));
        tabsItemTouchHelper.attachToRecyclerView(binding.tabsRecycler);
    }

    private void setupWindowTransitions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        getWindow().setEnterTransition(new Fade().setDuration(120));
        getWindow().setReturnTransition(new Fade().setDuration(120));
    }

    private void configureOverlayWindow() {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.dimAmount = 0.62f;
        getWindow().setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SystemBarUtils.apply(this, Color.TRANSPARENT, Color.TRANSPARENT, false);
        }
    }

    private void setupOverlayDismiss() {
        binding.getRoot().setOnClickListener(v -> {
            if (editingGroupTitle) {
                finishInlineRename(true);
            } else {
                finishWithTransition();
            }
        });
        binding.groupPanel.setOnClickListener(v -> {
            // Consume clicks inside the panel so only the dimmed outside area dismisses it.
        });
    }

    private void animateOverlayIn() {
        binding.groupPanel.setAlpha(0f);
        binding.groupPanel.setScaleX(0.98f);
        binding.groupPanel.setScaleY(0.98f);
        binding.groupPanel.post(() -> binding.groupPanel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(EasyMotion.DURATION_MEDIUM)
                .setInterpolator(EasyMotion.EMPHASIZED)
                .start());
    }

    private void requestTabListAnimation() {
        animateNextTabListChange = true;
    }

    private void beginTabListTransitionIfNeeded() {
        if (!animateNextTabListChange) {
            return;
        }
        animateNextTabListChange = false;
        if (binding == null || binding.tabsRecycler == null) {
            return;
        }
        Transition transition = EasyMotion.premiumLayoutTransition();
        TransitionManager.beginDelayedTransition(binding.tabsRecycler, transition);
    }

    private void animateInitialTabsIfNeeded(boolean empty) {
        if (initialTabsAnimated || empty || binding == null || binding.tabsRecycler == null) {
            return;
        }
        initialTabsAnimated = true;
        binding.tabsRecycler.post(() -> binding.tabsRecycler.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(EasyMotion.DURATION_MEDIUM)
                .setInterpolator(EasyMotion.EMPHASIZED)
                .start());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadTabs() {
        if (privateGroup && !runtimeTabs.isEmpty()) {
            List<Tab> tabs = getRuntimeGroupTabs();
            if (groupsChanged && tabs.isEmpty()) {
                finishWithResult();
                return;
            }
            currentTabs.clear();
            currentTabs.addAll(tabs);
            beginTabListTransitionIfNeeded();
            adapter.submitAnimatedList(tabs);
            currentTabCountText = getResources()
                    .getQuantityString(R.plurals.tab_count, tabs.size(), tabs.size());
            binding.groupTabCount.setText(currentTabCountText);
            updateSelectionUi();
            boolean empty = tabs.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.tabsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            animateInitialTabsIfNeeded(empty);
            return;
        }
        viewModel.loadTabs(groupId, tabs -> {
            if (groupsChanged && tabs.isEmpty()) {
                finishWithResult();
                return;
            }
            currentTabs.clear();
            currentTabs.addAll(tabs);
            beginTabListTransitionIfNeeded();
            adapter.submitAnimatedList(tabs);
            currentTabCountText = getResources()
                    .getQuantityString(R.plurals.tab_count, tabs.size(), tabs.size());
            binding.groupTabCount.setText(currentTabCountText);
            updateSelectionUi();
            boolean empty = tabs.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.tabsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            animateInitialTabsIfNeeded(empty);
        });
    }

    @Override
    public void onTabClick(Tab tab) {
        if (isSelectionMode()) {
            toggleSelection(tab);
            return;
        }
        selectedTabId = tab.getId();
        finishWithResult();
    }

    @Override
    public void onTabLongClick(Tab tab, View anchor, RecyclerView.ViewHolder viewHolder) {
        if (isSelectionMode()) {
            toggleSelection(tab);
            return;
        }
        finishInlineRename(true);
        dismissColorPopup();
        showTabActionMenu(tab, anchor);
        if (tabsItemTouchHelper != null && viewHolder != null) {
            tabsItemTouchHelper.startDrag(viewHolder);
        }
    }

    @Override
    public void onCloseTab(Tab tab) {
        if (tab != null && tab.isLocked()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            loadTabs();
            return;
        }
        requestTabListAnimation();
        addClosedTabId(tab.getId());
        selectedIds.remove(tab.getId());
        if (privateGroup) {
            String sourceGroupId = tab.getGroupId();
            removeRuntimeTab(tab.getId());
            normalizeRuntimeGroup(sourceGroupId);
            groupsChanged = true;
            updateSelectionUi();
            loadTabs();
            return;
        }
        viewModel.deleteTab(tab.getId(), () -> {
            groupsChanged = true;
            updateSelectionUi();
            loadTabs();
        });
    }

    @Override
    public void onMoveTab(Tab tab) {
        showMoveDialog(tab);
    }

    @Override
    public void onRemoveFromGroup(Tab tab) {
        requestTabListAnimation();
        if (privateGroup) {
            String sourceGroupId = tab.getGroupId();
            tab.setGroupId(null);
            tab.setGroupName(null);
            tab.setGroupColor(0);
            recordGroupMutation(tab, "", "", 0);
            normalizeRuntimeGroup(sourceGroupId);
            groupsChanged = true;
            loadTabs();
            return;
        }
        viewModel.removeTabFromGroup(tab.getId(), () -> {
            groupsChanged = true;
            loadTabs();
        });
    }

    @Override
    public void onPinTab(Tab tab) {
        if (tab == null) {
            return;
        }
        boolean pinned = !tab.isPinned();
        tab.setPinned(pinned);
        recordPinnedMutation(tab, pinned);
        groupsChanged = true;
        if (privateGroup) {
            Tab runtimeTab = findRuntimeTab(tab.getId());
            if (runtimeTab != null) {
                runtimeTab.setPinned(pinned);
            }
            loadTabs();
            Toast.makeText(this,
                    pinned ? R.string.tab_pinned : R.string.tab_unpinned,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveTab(tab, () -> {
            loadTabs();
            Toast.makeText(this,
                    pinned ? R.string.tab_pinned : R.string.tab_unpinned,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void onLockTab(Tab tab) {
        if (tab == null) {
            return;
        }
        boolean locked = !tab.isLocked();
        tab.setLocked(locked);
        recordLockedMutation(tab, locked);
        groupsChanged = true;
        if (privateGroup) {
            Tab runtimeTab = findRuntimeTab(tab.getId());
            if (runtimeTab != null) {
                runtimeTab.setLocked(locked);
            }
            loadTabs();
            Toast.makeText(this,
                    locked ? R.string.tab_locked : R.string.tab_unlocked,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveTab(tab, () -> {
            loadTabs();
            Toast.makeText(this,
                    locked ? R.string.tab_locked : R.string.tab_unlocked,
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDuplicateTab(Tab tab) {
        restoreUrl = tab.getUrl();
        restorePrivateTab = tab.isPrivate();
        finishWithResult();
    }

    @Override
    public void onShareTab(Tab tab) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, tab.getUrl());
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    @Override
    public void onTabsReordered(List<Tab> tabs) {
        if (privateGroup) {
            reorderedPrivateTabIds.clear();
            for (int i = 0; i < tabs.size(); i++) {
                Tab tab = tabs.get(i);
                tab.setPosition(i);
                reorderedPrivateTabIds.add(tab.getId());
            }
            groupsChanged = true;
            return;
        }
        viewModel.updateTabPositions(tabs, () -> groupsChanged = true);
    }

    @Override
    public void onTabDragStarted(Tab tab) {
        groupTabDragInProgress = true;
        showRemoveDropTarget();
    }

    @Override
    public void onTabDragMoved(Tab tab) {
        dismissActiveTabActionMenu();
    }

    @Override
    public boolean onTabDragFinished(Tab tab, View itemView) {
        boolean droppedOnRemoveTarget = isItemOverRemoveTarget(itemView);
        groupTabDragInProgress = false;
        hideRemoveDropTarget();
        if (droppedOnRemoveTarget && tab != null) {
            onRemoveFromGroup(tab);
            return true;
        }
        return false;
    }

    private void showMoveDialog(Tab tab) {
        if (privateGroup) {
            showRuntimeMoveDialog(tab);
            return;
        }
        viewModel.loadGroups(privateGroup, groups -> {
            List<TabGroup> targets = new ArrayList<>();
            for (TabGroup group : groups) {
                if (!group.getGroupId().equals(groupId)) {
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
                    .setTitle(R.string.move_to_group)
                    .setItems(names, (dialog, which) -> {
                        TabGroup target = targets.get(which);
                        requestTabListAnimation();
                        viewModel.moveTabToGroup(tab.getId(), target.getGroupId(), () -> {
                            groupsChanged = true;
                            Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                            loadTabs();
                        });
                    })
                    .show();
        });
    }

    private void toggleSelection(Tab tab) {
        if (selectedIds.contains(tab.getId())) {
            selectedIds.remove(tab.getId());
        } else {
            selectedIds.add(tab.getId());
        }
        selectionMode = true;
        adapter.setSelectedIds(selectedIds);
        updateSelectionUi();
    }

    private void clearSelection() {
        selectedIds.clear();
        selectionMode = false;
        adapter.setSelectedIds(selectedIds);
        updateSelectionUi();
    }

    private void updateSelectionUi() {
        if (editingGroupTitle) {
            return;
        }
        boolean selecting = isSelectionMode();
        binding.groupColorIndicator.setVisibility(selecting ? View.GONE : View.VISIBLE);
        binding.addTabButton.setVisibility(selecting ? View.GONE : View.VISIBLE);
        binding.groupTitleUnderline.setVisibility(selecting ? View.GONE : View.VISIBLE);
        binding.groupTitle.setOnClickListener(selecting ? null : v -> showRenameGroupDialog());
        binding.groupTitle.setText(selecting
                ? getString(R.string.selection_items_count, selectedIds.size())
                : getHeaderTitleText());
        if (adapter != null) {
            adapter.setSelectionMode(selecting);
            adapter.setSelectedIds(selectedIds);
        }
    }

    private void showGroupMenu(View anchor) {
        if (isSelectionMode()) {
            showSelectionMenu(anchor);
            return;
        }
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, MENU_SELECT_TABS, Menu.NONE, R.string.select_tabs);
        menu.getMenu().add(Menu.NONE, MENU_EDIT_GROUP_NAME, Menu.NONE, R.string.edit_group_name);
        menu.getMenu().add(Menu.NONE, MENU_EDIT_GROUP_COLOR, Menu.NONE, R.string.edit_group_colour);
        menu.getMenu().add(Menu.NONE, MENU_CLOSE_GROUP, Menu.NONE, R.string.close_group);
        menu.getMenu().add(Menu.NONE, MENU_DELETE_GROUP, Menu.NONE, R.string.delete_group);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SELECT_TABS) {
                enterSelectionMode();
                return true;
            } else if (item.getItemId() == MENU_EDIT_GROUP_NAME) {
                showRenameGroupDialog();
                return true;
            } else if (item.getItemId() == MENU_EDIT_GROUP_COLOR) {
                showChangeGroupColorDialog();
                return true;
            } else if (item.getItemId() == MENU_CLOSE_GROUP) {
                confirmCloseGroupTabs();
                return true;
            } else if (item.getItemId() == MENU_DELETE_GROUP) {
                confirmDeleteGroup();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void enterSelectionMode() {
        finishInlineRename(true);
        dismissColorPopup();
        selectionMode = true;
        updateSelectionUi();
    }

    private void showSelectionMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, SELECTION_MENU_SELECT_ALL, Menu.NONE, R.string.select_all_tabs)
                .setIcon(R.drawable.ic_check);
        menu.getMenu().add(Menu.NONE, SELECTION_MENU_CLOSE_TABS, Menu.NONE, R.string.close_tabs)
                .setIcon(R.drawable.ic_close);
        menu.getMenu().add(Menu.NONE, SELECTION_MENU_UNGROUP_TABS, Menu.NONE, R.string.ungroup_tabs)
                .setIcon(R.drawable.ic_view_grid);
        menu.getMenu().add(Menu.NONE, SELECTION_MENU_BOOKMARK_TABS, Menu.NONE, R.string.bookmark_tabs)
                .setIcon(R.drawable.ic_bookmark_border);
        menu.getMenu().add(Menu.NONE, SELECTION_MENU_SHARE_TABS, Menu.NONE, R.string.share_tabs)
                .setIcon(R.drawable.ic_share);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == SELECTION_MENU_SELECT_ALL) {
                selectAllTabs();
                return true;
            } else if (item.getItemId() == SELECTION_MENU_CLOSE_TABS) {
                closeSelectedTabs();
                return true;
            } else if (item.getItemId() == SELECTION_MENU_UNGROUP_TABS) {
                ungroupSelectedTabs();
                return true;
            } else if (item.getItemId() == SELECTION_MENU_BOOKMARK_TABS) {
                bookmarkSelectedTabs();
                return true;
            } else if (item.getItemId() == SELECTION_MENU_SHARE_TABS) {
                shareSelectedTabs();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showTabActionMenu(Tab tab, View anchor) {
        dismissActiveTabActionMenu();
        PopupMenu menu = new PopupMenu(this, anchor);
        activeTabActionMenu = menu;
        addTabMenuItem(menu, TAB_MENU_MOVE_TO_GROUP, R.string.move_tab_to_group, R.drawable.ic_view_grid);
        addTabMenuItem(menu, TAB_MENU_DUPLICATE, R.string.duplicate_tab, R.drawable.ic_duplicate_tab);
        addTabMenuItem(menu, TAB_MENU_BOOKMARK, R.string.add_to_bookmarks, R.drawable.ic_bookmark_border);
        addTabMenuItem(menu, TAB_MENU_SHARE, R.string.share, R.drawable.ic_share);
        addTabMenuItem(menu, TAB_MENU_PIN, tab.isPinned() ? R.string.unpin_tab : R.string.pin_tab,
                R.drawable.ic_pin);
        addTabMenuItem(menu, TAB_MENU_LOCK, tab.isLocked() ? R.string.unlock_tab : R.string.lock_tab,
                R.drawable.ic_lock);
        addTabMenuItem(menu, TAB_MENU_SELECT, R.string.select_tab, R.drawable.ic_edit);
        addTabMenuItem(menu, TAB_MENU_CLOSE, R.string.close_tab_sentence, R.drawable.ic_close);
        forceShowMenuIcons(menu);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == TAB_MENU_MOVE_TO_GROUP) {
                onMoveTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_DUPLICATE) {
                onDuplicateTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_BOOKMARK) {
                bookmarkTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_SHARE) {
                onShareTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_PIN) {
                onPinTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_LOCK) {
                onLockTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_SELECT) {
                selectSingleTab(tab);
                return true;
            } else if (item.getItemId() == TAB_MENU_CLOSE) {
                onCloseTab(tab);
                return true;
            }
            return false;
        });
        menu.setOnDismissListener(dismissedMenu -> {
            if (activeTabActionMenu == menu) {
                activeTabActionMenu = null;
            }
            if (!groupTabDragInProgress) {
                hideRemoveDropTarget();
            }
        });
        menu.show();
    }

    private void dismissActiveTabActionMenu() {
        if (activeTabActionMenu == null) {
            return;
        }
        PopupMenu menu = activeTabActionMenu;
        activeTabActionMenu = null;
        menu.dismiss();
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
        selectedIds.clear();
        selectedIds.add(tab.getId());
        selectionMode = true;
        updateSelectionUi();
    }

    private void bookmarkTab(Tab tab) {
        if (tab == null || tab.getUrl() == null || tab.getUrl().trim().isEmpty()) {
            return;
        }
        Bookmark bookmark = new Bookmark(getTabTitle(tab), tab.getUrl());
        bookmark.setFavicon(tab.getFaviconUri());
        bookmarkRepository.addBookmark(bookmark, new BookmarkRepository.BookmarkCallback() {
            @Override public void onBookmarksLoaded(List<Bookmark> bookmarks) {}
            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
                runOnUiThread(() -> Toast.makeText(GroupTabsActivity.this,
                        R.string.bookmark_added_message, Toast.LENGTH_SHORT).show());
            }
            @Override public void onBookmarkRemoved(Bookmark bookmark) {}
        });
    }

    private void showRemoveDropTarget() {
        if (binding == null || binding.removeDropTarget == null
                || binding.removeDropTarget.getVisibility() == View.VISIBLE) {
            return;
        }
        binding.removeDropTarget.animate().cancel();
        binding.removeDropTarget.setAlpha(0f);
        binding.removeDropTarget.setVisibility(View.VISIBLE);
        binding.removeDropTarget.animate()
                .alpha(1f)
                .setDuration(120)
                .start();
    }

    private void hideRemoveDropTarget() {
        if (binding == null || binding.removeDropTarget == null
                || binding.removeDropTarget.getVisibility() != View.VISIBLE) {
            return;
        }
        binding.removeDropTarget.animate().cancel();
        binding.removeDropTarget.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    if (binding != null && binding.removeDropTarget != null) {
                        binding.removeDropTarget.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private boolean isItemOverRemoveTarget(View itemView) {
        if (binding == null || itemView == null || binding.removeDropTarget == null
                || binding.removeDropTarget.getVisibility() != View.VISIBLE) {
            return false;
        }
        Rect targetRect = new Rect();
        if (!binding.removeDropTarget.getGlobalVisibleRect(targetRect)) {
            return false;
        }
        int[] itemLocation = new int[2];
        itemView.getLocationOnScreen(itemLocation);
        Rect itemRect = new Rect(itemLocation[0], itemLocation[1],
                itemLocation[0] + itemView.getWidth(),
                itemLocation[1] + itemView.getHeight());
        return Rect.intersects(targetRect, itemRect)
                || targetRect.contains(itemRect.centerX(), itemRect.centerY());
    }

    private void selectAllTabs() {
        selectionMode = true;
        selectedIds.clear();
        for (Tab tab : currentTabs) {
            selectedIds.add(tab.getId());
        }
        updateSelectionUi();
    }

    private void closeSelectedTabs() {
        List<Tab> tabs = getSelectedTabs();
        if (tabs.isEmpty()) {
            Toast.makeText(this, R.string.select_tabs_first, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Tab> closableTabs = new ArrayList<>();
        for (Tab tab : tabs) {
            if (tab != null && !tab.isLocked()) {
                closableTabs.add(tab);
            }
        }
        if (closableTabs.isEmpty()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (closableTabs.size() < tabs.size()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
        }
        requestTabListAnimation();
        ArrayList<String> closableIds = new ArrayList<>();
        for (Tab tab : closableTabs) {
            addClosedTabId(tab.getId());
            closableIds.add(tab.getId());
        }
        groupsChanged = true;
        if (privateGroup) {
            for (Tab tab : closableTabs) {
                removeRuntimeTab(tab.getId());
            }
            normalizeRuntimeGroup(groupId);
            clearSelection();
            loadTabs();
            return;
        }
        viewModel.deleteTabs(closableIds, () -> {
            clearSelection();
            loadTabs();
        });
    }

    private void ungroupSelectedTabs() {
        List<Tab> tabs = getSelectedTabs();
        if (tabs.isEmpty()) {
            Toast.makeText(this, R.string.select_tabs_first, Toast.LENGTH_SHORT).show();
            return;
        }
        requestTabListAnimation();
        groupsChanged = true;
        if (privateGroup) {
            for (Tab tab : tabs) {
                tab.setGroupId(null);
                tab.setGroupName(null);
                tab.setGroupColor(0);
                recordGroupMutation(tab, "", "", 0);
            }
            normalizeRuntimeGroup(groupId);
            clearSelection();
            loadTabs();
            return;
        }
        ArrayList<String> ids = new ArrayList<>(selectedIds);
        int[] remaining = {ids.size()};
        for (String tabId : ids) {
            viewModel.removeTabFromGroup(tabId, () -> {
                remaining[0]--;
                if (remaining[0] == 0) {
                    clearSelection();
                    loadTabs();
                }
            });
        }
    }

    private void bookmarkSelectedTabs() {
        List<Tab> tabs = getSelectedTabs();
        if (tabs.isEmpty()) {
            Toast.makeText(this, R.string.select_tabs_first, Toast.LENGTH_SHORT).show();
            return;
        }
        BookmarkRepository.BookmarkCallback callback = new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {
            }

            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
            }

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {
            }
        };
        for (Tab tab : tabs) {
            Bookmark bookmark = new Bookmark(getTabTitle(tab), tab.getUrl());
            bookmark.setId(tab.getId() + "_" + System.currentTimeMillis());
            bookmark.setFavicon(tab.getFaviconUri());
            bookmarkRepository.addBookmark(bookmark, callback);
        }
        Toast.makeText(this, R.string.bookmarks_added, Toast.LENGTH_SHORT).show();
    }

    private void shareSelectedTabs() {
        List<Tab> tabs = getSelectedTabs();
        if (tabs.isEmpty()) {
            Toast.makeText(this, R.string.select_tabs_first, Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder text = new StringBuilder();
        for (Tab tab : tabs) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(getTabTitle(tab));
            if (tab.getUrl() != null && !tab.getUrl().trim().isEmpty()) {
                text.append('\n').append(tab.getUrl());
            }
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private List<Tab> getSelectedTabs() {
        List<Tab> tabs = new ArrayList<>();
        for (Tab tab : currentTabs) {
            if (selectedIds.contains(tab.getId())) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    private String getTabTitle(Tab tab) {
        return tab.getTitle() != null && !tab.getTitle().trim().isEmpty()
                ? tab.getTitle()
                : getString(R.string.new_tab);
    }

    private void showRenameGroupDialog() {
        startInlineRename();
    }

    private void showChangeGroupColorDialog() {
        showColorPalettePopup();
    }

    private void startInlineRename() {
        if (isSelectionMode()) {
            return;
        }
        dismissColorPopup();
        inlineRenameOriginalText = getEditableHeaderTitleText();
        editingGroupTitle = true;
        binding.groupTitleInput.setText(inlineRenameOriginalText);
        binding.groupTitleInput.setSelection(binding.groupTitleInput.getText().length());
        binding.groupTitle.setVisibility(View.GONE);
        binding.groupTitleInput.setVisibility(View.VISIBLE);
        binding.groupTitleUnderline.setBackgroundColor(resolveGroupColor());
        binding.groupTitleInput.requestFocus();
        showKeyboard(binding.groupTitleInput);
    }

    private void finishInlineRename(boolean save) {
        if (!editingGroupTitle) {
            return;
        }
        String newName = binding.groupTitleInput.getText() != null
                ? binding.groupTitleInput.getText().toString().trim()
                : "";
        editingGroupTitle = false;
        hideKeyboard(binding.groupTitleInput);
        binding.groupTitleInput.clearFocus();
        binding.groupTitleInput.setVisibility(View.GONE);
        binding.groupTitle.setVisibility(View.VISIBLE);
        binding.groupTitleUnderline.setBackgroundColor(
                ContextCompat.getColor(this, R.color.border_color));
        if (save && !newName.equals(inlineRenameOriginalText)) {
            updateGroupNameOnly(newName);
        } else {
            updateSelectionUi();
        }
    }

    private String getHeaderTitleText() {
        if (hasCustomGroupName()) {
            return groupName.trim();
        }
        return currentTabCountText != null && !currentTabCountText.isEmpty()
                ? currentTabCountText
                : getString(R.string.tab_group);
    }

    private String getEditableHeaderTitleText() {
        return getHeaderTitleText();
    }

    private boolean hasCustomGroupName() {
        return groupName != null
                && !groupName.trim().isEmpty()
                && !groupName.trim().equals(getString(R.string.tab_group));
    }

    private void updateGroupNameOnly(String name) {
        String resolvedName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : getString(R.string.tab_group);
        groupName = resolvedName;
        if (privateGroup) {
            applyRuntimeGroupMetadata(resolvedName, groupColor);
            loadTabs();
            return;
        }
        viewModel.updateGroupName(groupId, resolvedName, () -> {
            groupsChanged = true;
            loadTabs();
        });
    }

    private void showColorPalettePopup() {
        finishInlineRename(true);
        if (colorPopup != null && colorPopup.isShowing()) {
            colorPopup.dismiss();
            return;
        }
        LinearLayout palette = new LinearLayout(this);
        palette.setOrientation(LinearLayout.VERTICAL);
        palette.setPadding(dp(20), dp(16), dp(20), dp(16));
        palette.setBackground(createRoundedRect(
                ContextCompat.getColor(this, R.color.colorSurface), dp(22)));

        int[] groupColorPalette = TabRepository.getDefaultGroupColors(this);
        int index = 0;
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (rowIndex > 0) {
                rowParams.topMargin = dp(12);
            }
            row.setLayoutParams(rowParams);
            int rowCount = rowIndex == 0 ? 5 : 4;
            for (int i = 0; i < rowCount && index < groupColorPalette.length; i++) {
                int color = groupColorPalette[index++];
                row.addView(createColorSwatch(color));
            }
            palette.addView(row);
        }

        colorPopup = new PopupWindow(palette, dp(330), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        colorPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        colorPopup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorPopup.setElevation(dp(8));
        }
        colorPopup.setOnDismissListener(() -> colorPopup = null);
        binding.groupPanel.post(() -> {
            int[] panelLocation = new int[2];
            binding.groupPanel.getLocationOnScreen(panelLocation);
            colorPopup.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    0, panelLocation[1] + dp(20));
        });
    }

    private View createColorSwatch(int color) {
        FrameLayout shell = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(46));
        params.setMargins(dp(6), 0, dp(6), 0);
        shell.setLayoutParams(params);
        if (color == groupColor) {
            shell.setBackground(createOval(Color.TRANSPARENT, color, dp(3)));
        }
        View dot = new View(this);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER);
        dot.setBackground(createOval(color, Color.TRANSPARENT, 0));
        shell.addView(dot, dotParams);
        shell.setOnClickListener(v -> {
            updateGroupColor(color);
            dismissColorPopup();
        });
        return shell;
    }

    private void updateGroupMetadata(String name, int color) {
        String resolvedName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : getString(R.string.tab_group);
        groupName = resolvedName;
        groupColor = color;
        tintCircle(binding.groupColorIndicator, groupColor);
        if (privateGroup) {
            applyRuntimeGroupMetadata(resolvedName, color);
            Toast.makeText(this, R.string.group_updated, Toast.LENGTH_SHORT).show();
            loadTabs();
            return;
        }
        viewModel.updateGroupName(groupId, resolvedName, () ->
                viewModel.updateGroupColor(groupId, color, () -> {
                    groupsChanged = true;
                    Toast.makeText(this, R.string.group_updated, Toast.LENGTH_SHORT).show();
                    loadTabs();
                }));
    }

    private void updateGroupColor(int color) {
        groupColor = color;
        tintCircle(binding.groupColorIndicator, groupColor);
        adapter.setGroupColor(groupColor);
        for (Tab tab : currentTabs) {
            tab.setGroupColor(groupColor);
        }
        if (privateGroup) {
            applyRuntimeGroupMetadata(groupName, color);
            loadTabs();
            return;
        }
        viewModel.updateGroupColor(groupId, color, () -> {
            groupsChanged = true;
            loadTabs();
        });
    }

    private void dismissColorPopup() {
        if (colorPopup != null) {
            colorPopup.dismiss();
            colorPopup = null;
        }
    }

    private int resolveGroupColor() {
        return groupColor != 0 ? groupColor : TabRepository.getDefaultGroupColor(this);
    }

    private GradientDrawable createRoundedRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createOval(int fillColor, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private void showKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean isSelectionMode() {
        return selectionMode || !selectedIds.isEmpty();
    }

    private void showNewTabMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.new_tab_in_group);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.new_tab);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.new_private_tab);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                createPrivateTab = privateGroup;
                createTabInCurrentGroup = true;
                finishWithResult();
                return true;
            } else if (item.getItemId() == 2) {
                createPrivateTab = false;
                createTabInCurrentGroup = false;
                finishWithResult();
                return true;
            } else if (item.getItemId() == 3) {
                createPrivateTab = true;
                createTabInCurrentGroup = false;
                finishWithResult();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showAddLongPressMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.select_tabs);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.create_group_from_selection);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.close_all_tabs);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                enterSelectionMode();
                return true;
            } else if (item.getItemId() == 2) {
                createGroupFromSelection();
                return true;
            } else if (item.getItemId() == 3) {
                confirmCloseGroupTabs();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmCloseGroupTabs() {
        if (currentTabs.isEmpty()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.close_all_tabs)
                .setMessage(R.string.close_all_tabs_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.close_all_tabs, (dialog, which) -> {
                    deleteCurrentGroup();
                })
                .show();
    }

    private void confirmDeleteGroup() {
        String name = groupName != null && !groupName.trim().isEmpty()
                ? groupName
                : getString(R.string.tab_group);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_group)
                .setMessage(getString(R.string.delete_group_confirm, name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCurrentGroup())
                .show();
    }

    private void deleteCurrentGroup() {
        for (Tab tab : currentTabs) {
            addClosedTabId(tab.getId());
        }
        groupsChanged = true;
        if (privateGroup) {
            for (Tab tab : new ArrayList<>(currentTabs)) {
                removeRuntimeTab(tab.getId());
            }
            finishWithResult();
            return;
        }
        ArrayList<String> tabIds = new ArrayList<>();
        for (Tab tab : currentTabs) {
            tabIds.add(tab.getId());
        }
        viewModel.deleteTabs(tabIds, () ->
                viewModel.deleteGroup(groupId, () -> {
                    Toast.makeText(this, R.string.group_deleted, Toast.LENGTH_SHORT).show();
                    finishWithResult();
                }));
    }

    private void createGroupFromSelection() {
        if (selectedIds.size() < 2) {
            Toast.makeText(this, R.string.select_tabs_first, Toast.LENGTH_SHORT).show();
            return;
        }
        TabGroupDialogHelper.show(this, R.string.create_group, "",
                TabRepository.getDefaultGroupColor(this), true, (name, color) -> {
                    ArrayList<String> ids = new ArrayList<>(selectedIds);
                    requestTabListAnimation();
                    if (privateGroup) {
                        createRuntimeGroupFromSelection(ids, name, color);
                        return;
                    }
                    viewModel.createGroupForTabs(ids, name, color, privateGroup, () -> {
                        groupsChanged = true;
                        clearSelection();
                        Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show();
                        loadTabs();
                    });
                });
    }

    private void createRuntimeGroupFromSelection(List<String> ids, String name, int color) {
        List<Tab> selectedTabs = new ArrayList<>();
        for (String id : ids) {
            Tab tab = findRuntimeTab(id);
            if (tab != null) {
                selectedTabs.add(tab);
            }
        }
        if (selectedTabs.size() < 2) {
            return;
        }
        String sourceGroupId = groupId;
        String newGroupId = UUID.randomUUID().toString();
        String newGroupName = name != null && !name.trim().isEmpty()
                ? name
                : getString(R.string.tab_group);
        for (Tab tab : selectedTabs) {
            applyRuntimeTabGroup(tab, newGroupId, newGroupName, color);
        }
        recordCreatedPrivateGroup(newGroupId, newGroupName, color, selectedTabs);
        normalizeRuntimeGroup(sourceGroupId);
        groupsChanged = true;
        clearSelection();
        Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show();
        loadTabs();
    }

    private void showRuntimeMoveDialog(Tab tab) {
        List<TabGroup> targets = new ArrayList<>();
        for (TabGroup group : getRuntimeGroups()) {
            if (!group.getGroupId().equals(groupId)) {
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
                .setTitle(R.string.move_to_group)
                .setItems(names, (dialog, which) -> {
                    Tab runtimeTab = findRuntimeTab(tab.getId());
                    if (runtimeTab == null) {
                        return;
                    }
                    requestTabListAnimation();
                    String sourceGroupId = runtimeTab.getGroupId();
                    TabGroup target = targets.get(which);
                    applyRuntimeTabGroup(runtimeTab, target.getGroupId(),
                            target.getGroupName(), target.getGroupColor());
                    recordGroupMutation(runtimeTab, target.getGroupId(),
                            target.getGroupName(), target.getGroupColor());
                    normalizeRuntimeGroup(sourceGroupId);
                    groupsChanged = true;
                    Toast.makeText(this, R.string.tabs_moved, Toast.LENGTH_SHORT).show();
                    loadTabs();
                })
                .show();
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
        if (ids == null || titles == null || urls == null || privateStates == null) {
            return;
        }
        int count = Math.min(Math.min(ids.size(), titles.size()),
                Math.min(urls.size(), privateStates.length));
        for (int i = 0; i < count; i++) {
            Tab tab = new Tab(ids.get(i), null, titles.get(i), urls.get(i), privateStates[i]);
            String tabGroupId = valueAt(groupIds, i);
            if (!tabGroupId.isEmpty()) {
                tab.setGroupId(tabGroupId);
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
            runtimeTabs.add(tab);
        }
    }

    private List<Tab> getRuntimeGroupTabs() {
        List<Tab> tabs = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (groupId.equals(tab.getGroupId())) {
                tabs.add(tab);
            }
        }
        Collections.sort(tabs, (first, second) -> {
            if (first.isPinned() != second.isPinned()) {
                return first.isPinned() ? -1 : 1;
            }
            return Integer.compare(first.getPosition(), second.getPosition());
        });
        return tabs;
    }

    private List<TabGroup> getRuntimeGroups() {
        Map<String, TabGroup> groupsById = new LinkedHashMap<>();
        for (Tab tab : runtimeTabs) {
            if (!tab.isPrivate() || tab.getGroupId() == null || tab.getGroupId().trim().isEmpty()) {
                continue;
            }
            TabGroup group = groupsById.get(tab.getGroupId());
            if (group == null) {
                String name = tab.getGroupName() != null && !tab.getGroupName().trim().isEmpty()
                        ? tab.getGroupName()
                        : getString(R.string.tab_group);
                int color = tab.getGroupColor() != 0 ? tab.getGroupColor() : groupColor;
                group = new TabGroup(tab.getGroupId(), name, color,
                        true, tab.getCreatedAt(), tab.getLastAccessed());
                groupsById.put(tab.getGroupId(), group);
            }
            group.getTabs().add(tab);
        }
        List<TabGroup> groups = new ArrayList<>();
        for (TabGroup group : groupsById.values()) {
            if (group.getTabCount() >= 2) {
                groups.add(group);
            }
        }
        return groups;
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

    private void applyRuntimeTabGroup(Tab tab, String targetGroupId,
                                      String targetGroupName, int targetGroupColor) {
        tab.setGroupId(targetGroupId);
        tab.setGroupName(targetGroupName);
        tab.setGroupColor(targetGroupColor);
    }

    private void applyRuntimeGroupMetadata(String name, int color) {
        String resolvedName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : getString(R.string.tab_group);
        for (Tab tab : runtimeTabs) {
            if (groupId.equals(tab.getGroupId())) {
                applyRuntimeTabGroup(tab, groupId, resolvedName, color);
                recordGroupMutation(tab, groupId, resolvedName, color);
            }
        }
        groupName = resolvedName;
        groupColor = color;
        groupsChanged = true;
    }

    private void normalizeRuntimeGroup(String changedGroupId) {
        if (changedGroupId == null || changedGroupId.trim().isEmpty()) {
            return;
        }
        List<Tab> remaining = new ArrayList<>();
        for (Tab tab : runtimeTabs) {
            if (changedGroupId.equals(tab.getGroupId())) {
                remaining.add(tab);
            }
        }
        if (remaining.size() >= 2) {
            return;
        }
        for (Tab tab : remaining) {
            tab.setGroupId(null);
            tab.setGroupName(null);
            tab.setGroupColor(0);
            recordGroupMutation(tab, "", "", 0);
        }
    }

    private void addClosedTabId(String tabId) {
        if (tabId != null && !closedTabIds.contains(tabId)) {
            closedTabIds.add(tabId);
        }
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

    private void recordGroupMutation(Tab tab, String groupId, String groupName, int groupColor) {
        groupMutationTabIds.add(tab.getId());
        groupMutationGroupIds.add(groupId != null ? groupId : "");
        groupMutationGroupNames.add(groupName != null ? groupName : "");
        groupMutationGroupColors.add(groupColor);
    }

    private void recordPinnedMutation(Tab tab, boolean pinned) {
        if (tab == null) {
            return;
        }
        String tabId = tab.getId();
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
    }

    private void recordLockedMutation(Tab tab, boolean locked) {
        if (tab == null) {
            return;
        }
        String tabId = tab.getId();
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

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index);
    }

    private void finishWithResult() {
        setTabsResult();
        finishWithTransition();
    }

    private void finishWithTransition() {
        if (!resultSet) {
            setTabsResult();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    private void setTabsResult() {
        Intent result = new Intent();
        result.putStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS, closedTabIds);
        result.putExtra(TabManagerActivity.RESULT_GROUPS_CHANGED, groupsChanged);
        if (selectedTabId != null) {
            result.putExtra(TabsActivity.RESULT_SELECTED_TAB_ID, selectedTabId);
        }
        if (restoreUrl != null) {
            result.putExtra(TabsActivity.RESULT_RESTORE_URL, restoreUrl);
            result.putExtra(TabsActivity.RESULT_RESTORE_PRIVATE, restorePrivateTab);
        }
        if (createPrivateTab != null) {
            result.putExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, createPrivateTab);
            if (createTabInCurrentGroup) {
                result.putExtra(EXTRA_GROUP_ID, groupId);
                result.putExtra(EXTRA_GROUP_NAME, groupName);
                result.putExtra(EXTRA_GROUP_COLOR, groupColor);
            }
        }
        if (!groupMutationTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_GROUP_MUTATION_TAB_IDS,
                    groupMutationTabIds);
            result.putStringArrayListExtra(TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_IDS,
                    groupMutationGroupIds);
            result.putStringArrayListExtra(TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_NAMES,
                    groupMutationGroupNames);
            result.putIntegerArrayListExtra(TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_COLORS,
                    groupMutationGroupColors);
        }
        if (!createdPrivateGroupIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_IDS,
                    createdPrivateGroupIds);
            result.putStringArrayListExtra(TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_TAB_IDS,
                    createdPrivateGroupTabIds);
            result.putIntegerArrayListExtra(TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_TAB_COUNTS,
                    createdPrivateGroupTabCounts);
            result.putStringArrayListExtra(TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_NAMES,
                    createdPrivateGroupNames);
            result.putIntegerArrayListExtra(TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_COLORS,
                    createdPrivateGroupColors);
        }
        if (!reorderedPrivateTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_REORDERED_PRIVATE_TAB_IDS,
                    reorderedPrivateTabIds);
        }
        if (!pinnedTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_PINNED_TAB_IDS, pinnedTabIds);
        }
        if (!unpinnedTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_UNPINNED_TAB_IDS, unpinnedTabIds);
        }
        if (!lockedTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_LOCKED_TAB_IDS, lockedTabIds);
        }
        if (!unlockedTabIds.isEmpty()) {
            result.putStringArrayListExtra(TabManagerActivity.RESULT_UNLOCKED_TAB_IDS, unlockedTabIds);
        }
        setResult(RESULT_OK, result);
        resultSet = true;
    }

    private void tintCircle(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color != 0
                ? color
                : ContextCompat.getColor(this, R.color.gray));
        view.setBackground(drawable);
    }

    @Override
    public void finish() {
        if (!resultSet) {
            setTabsResult();
        }
        super.finish();
    }
}
