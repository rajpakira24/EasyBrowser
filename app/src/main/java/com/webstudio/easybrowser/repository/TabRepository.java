package com.webstudio.easybrowser.repository;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.core.content.ContextCompat;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;
import com.webstudio.easybrowser.database.relation.TabGroupWithTabs;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

public class TabRepository {
    private final Context appContext;
    private final AppDatabase database;
    private final Executor executor;

    public interface GroupsCallback {
        void onGroupsLoaded(List<TabGroup> groups);
    }

    public interface TabsCallback {
        void onTabsLoaded(List<Tab> tabs);
    }

    public interface CompletionCallback {
        void onComplete();
    }

    public interface CountsCallback {
        void onCountsLoaded(int regularCount, int privateCount);
    }

    public TabRepository(Context context) {
        appContext = context.getApplicationContext();
        database = AppDatabase.getInstance(appContext);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public static int getDefaultGroupColor(Context context) {
        int[] colors = getDefaultGroupColors(context);
        return colors.length > 0
                ? colors[0]
                : ContextCompat.getColor(context, R.color.colorPrimary);
    }

    public static int[] getDefaultGroupColors(Context context) {
        TypedArray palette = context.getResources().obtainTypedArray(R.array.tab_group_palette);
        int[] colors = new int[palette.length()];
        int fallback = ContextCompat.getColor(context, R.color.colorPrimary);
        for (int i = 0; i < palette.length(); i++) {
            colors[i] = palette.getColor(i, fallback);
        }
        palette.recycle();
        return colors;
    }

    public void getGroups(GroupsCallback callback) {
        getGroups(false, callback);
    }

    public void getGroups(boolean isPrivate, GroupsCallback callback) {
        executor.execute(() -> {
            cleanupInvalidGroups();
            List<TabGroupWithTabs> rows = database.tabGroupDao().getGroupsWithTabs(isPrivate);
            callback.onGroupsLoaded(toGroups(rows));
        });
    }

    public void getTabsForGroup(String groupId, TabsCallback callback) {
        executor.execute(() -> callback.onTabsLoaded(getTabsForGroupBlocking(groupId)));
    }

    public void getTabCounts(CountsCallback callback) {
        executor.execute(() -> callback.onCountsLoaded(
                database.tabGroupDao().getTabCount(false),
                database.tabGroupDao().getTabCount(true)));
    }

    public void getStandaloneTabs(boolean isPrivate, TabsCallback callback) {
        executor.execute(() -> callback.onTabsLoaded(toTabs(
                getStandaloneTabsAfterCleanup(isPrivate))));
    }

    public void saveGroup(TabGroup group, CompletionCallback callback) {
        executor.execute(() -> {
            if (group != null && !group.isPrivate()) {
                database.tabGroupDao().insertGroup(toEntity(group));
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void deleteGroup(String groupId, CompletionCallback callback) {
        executor.execute(() -> {
            database.tabGroupDao().deleteGroupById(groupId);
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void deleteGroups(List<String> groupIds, CompletionCallback callback) {
        executor.execute(() -> {
            if (groupIds != null) {
                for (String groupId : groupIds) {
                    database.tabGroupDao().deleteGroupById(groupId);
                }
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void saveTab(Tab tab, CompletionCallback callback) {
        executor.execute(() -> {
            if (tab != null && !tab.isPrivate()) {
                ensureGroupExists(tab.getGroupId(), tab.getGroupName(), tab.getGroupColor(), false);
                database.tabGroupDao().insertTab(toEntity(tab));
                touchGroup(tab.getGroupId());
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void deleteTab(String tabId, CompletionCallback callback) {
        executor.execute(() -> {
            String groupId = database.tabGroupDao().getGroupIdForTab(tabId);
            database.tabGroupDao().deleteTabById(tabId);
            enforceMinimumGroupSize(groupId);
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void deleteTabs(List<String> tabIds, CompletionCallback callback) {
        executor.execute(() -> {
            if (tabIds != null) {
                for (String tabId : tabIds) {
                    String groupId = database.tabGroupDao().getGroupIdForTab(tabId);
                    database.tabGroupDao().deleteTabById(tabId);
                    enforceMinimumGroupSize(groupId);
                }
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void moveTabToGroup(String tabId, String groupId, CompletionCallback callback) {
        executor.execute(() -> {
            String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
            int position = database.tabGroupDao().getTabsForGroup(groupId).size();
            database.tabGroupDao().moveTabToGroup(tabId, groupId, position);
            touchGroup(groupId);
            enforceMinimumGroupSize(sourceGroupId);
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void removeTabFromGroup(String tabId, CompletionCallback callback) {
        executor.execute(() -> {
            String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
            database.tabGroupDao().removeTabFromGroup(tabId);
            enforceMinimumGroupSize(sourceGroupId);
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void moveTabsToGroup(List<String> tabIds, String groupId, CompletionCallback callback) {
        executor.execute(() -> {
            List<String> sourceGroupIds = new ArrayList<>();
            if (tabIds != null) {
                int position = database.tabGroupDao().getTabsForGroup(groupId).size();
                for (String tabId : tabIds) {
                    String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
                    if (sourceGroupId != null && !sourceGroupIds.contains(sourceGroupId)) {
                        sourceGroupIds.add(sourceGroupId);
                    }
                    database.tabGroupDao().moveTabToGroup(tabId, groupId, position++);
                }
                touchGroup(groupId);
                for (String sourceGroupId : sourceGroupIds) {
                    enforceMinimumGroupSize(sourceGroupId);
                }
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void createGroupForTabs(List<String> tabIds, String groupName, int groupColor,
                                   boolean isPrivate, CompletionCallback callback) {
        executor.execute(() -> {
            if (!isPrivate && tabIds != null && tabIds.size() >= 2) {
                TabGroup group = new TabGroup(groupName, groupColor);
                group.setPrivate(isPrivate);
                database.tabGroupDao().insertGroup(toEntity(group));
                int position = 0;
                List<String> sourceGroupIds = new ArrayList<>();
                for (String tabId : tabIds) {
                    String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
                    if (sourceGroupId != null && !sourceGroupIds.contains(sourceGroupId)) {
                        sourceGroupIds.add(sourceGroupId);
                    }
                    database.tabGroupDao().moveTabToGroup(tabId, group.getGroupId(), position++);
                }
                touchGroup(group.getGroupId());
                for (String sourceGroupId : sourceGroupIds) {
                    enforceMinimumGroupSize(sourceGroupId);
                }
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void moveGroupsToGroup(List<String> sourceGroupIds, String targetGroupId,
                                  boolean deleteSources, CompletionCallback callback) {
        executor.execute(() -> {
            if (sourceGroupIds != null && targetGroupId != null) {
                int position = database.tabGroupDao().getTabsForGroup(targetGroupId).size();
                for (String sourceGroupId : sourceGroupIds) {
                    if (targetGroupId.equals(sourceGroupId)) {
                        continue;
                    }
                    List<TabEntity> sourceTabs = database.tabGroupDao().getTabsForGroup(sourceGroupId);
                    for (TabEntity tab : sourceTabs) {
                        database.tabGroupDao().moveTabToGroup(tab.getTabId(), targetGroupId, position++);
                    }
                    if (deleteSources) {
                        database.tabGroupDao().deleteGroupById(sourceGroupId);
                    }
                }
                touchGroup(targetGroupId);
                enforceMinimumGroupSize(targetGroupId);
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void updateGroupName(String groupId, String groupName, CompletionCallback callback) {
        executor.execute(() -> {
            database.tabGroupDao().updateGroupName(groupId, groupName, System.currentTimeMillis());
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void updateGroupColor(String groupId, int groupColor, CompletionCallback callback) {
        executor.execute(() -> {
            database.tabGroupDao().updateGroupColor(groupId, groupColor, System.currentTimeMillis());
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void updateTabPositions(List<Tab> tabs, CompletionCallback callback) {
        executor.execute(() -> {
            if (tabs != null) {
                for (int i = 0; i < tabs.size(); i++) {
                    database.tabGroupDao().updateTabPosition(tabs.get(i).getId(), i);
                }
                if (!tabs.isEmpty()) {
                    touchGroup(tabs.get(0).getGroupId());
                }
            }
            if (callback != null) {
                callback.onComplete();
            }
        });
    }

    public void updateThumbnailPath(String tabId, String thumbnailPath) {
        executor.execute(() -> database.tabGroupDao().updateThumbnailPath(tabId, thumbnailPath));
    }

    public List<TabGroup> getGroupsBlocking() {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            return toGroups(database.tabGroupDao().getGroupsWithTabs());
        }, new ArrayList<>());
    }

    public List<TabGroup> getGroupsBlocking(boolean isPrivate) {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            return toGroups(database.tabGroupDao().getGroupsWithTabs(isPrivate));
        }, new ArrayList<>());
    }

    public List<Tab> getAllTabsBlocking() {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            return toTabs(database.tabGroupDao().getAllTabs());
        }, new ArrayList<>());
    }

    public List<Tab> getAllTabsBlocking(boolean isPrivate) {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            return toTabs(database.tabGroupDao().getAllTabs(isPrivate));
        }, new ArrayList<>());
    }

    public List<Tab> getStandaloneTabsBlocking(boolean isPrivate) {
        return runBlocking(() -> toTabs(getStandaloneTabsAfterCleanup(isPrivate)), new ArrayList<>());
    }

    public List<Tab> getTabsForGroupBlocking(String groupId) {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            TabGroupEntity group = database.tabGroupDao().getGroupById(groupId);
            return toTabs(database.tabGroupDao().getTabsForGroup(groupId), group);
        }, new ArrayList<>());
    }

    public TabGroup getGroupBlocking(String groupId) {
        return runBlocking(() -> {
            cleanupInvalidGroups();
            TabGroupEntity entity = database.tabGroupDao().getGroupById(groupId);
            return entity != null ? toModel(entity) : null;
        }, null);
    }

    public int getTabCountBlocking() {
        return runBlocking(() -> database.tabGroupDao().getTabCount(), 0);
    }

    public int getTabCountBlocking(boolean isPrivate) {
        return runBlocking(() -> database.tabGroupDao().getTabCount(isPrivate), 0);
    }

    public void saveGroupBlocking(TabGroup group) {
        runBlocking(() -> {
            if (group != null && !group.isPrivate()) {
                database.tabGroupDao().insertGroup(toEntity(group));
            }
            return null;
        }, null);
    }

    public void ensureGroupBlocking(TabGroup group) {
        runBlocking(() -> {
            if (group != null && !group.isPrivate()
                    && database.tabGroupDao().getGroupById(group.getGroupId()) == null) {
                database.tabGroupDao().insertGroup(toEntity(group));
            }
            return null;
        }, null);
    }

    public void saveTabBlocking(Tab tab) {
        runBlocking(() -> {
            if (tab != null && !tab.isPrivate()) {
                ensureGroupExists(tab.getGroupId(), tab.getGroupName(), tab.getGroupColor(), false);
                database.tabGroupDao().insertTab(toEntity(tab));
                touchGroup(tab.getGroupId());
            }
            return null;
        }, null);
    }

    public void saveTabsBlocking(List<Tab> tabs) {
        runBlocking(() -> {
            List<TabEntity> entities = new ArrayList<>();
            for (Tab tab : tabs) {
                if (tab == null) {
                    continue;
                }
                if (tab.isPrivate()) {
                    continue;
                }
                ensureGroupExists(tab.getGroupId(), tab.getGroupName(), tab.getGroupColor(), false);
                entities.add(toEntity(tab));
            }
            database.tabGroupDao().insertTabs(entities);
            long now = System.currentTimeMillis();
            for (TabEntity entity : entities) {
                if (entity.getGroupId() != null) {
                    database.tabGroupDao().updateGroupTimestamp(entity.getGroupId(), now);
                }
            }
            return null;
        }, null);
    }

    public void deleteTabBlocking(String tabId) {
        runBlocking(() -> {
            String groupId = database.tabGroupDao().getGroupIdForTab(tabId);
            database.tabGroupDao().deleteTabById(tabId);
            enforceMinimumGroupSize(groupId);
            return null;
        }, null);
    }

    public void clearPersistedPrivateStateBlocking() {
        runBlocking(() -> {
            database.tabGroupDao().deletePrivateTabs();
            database.tabGroupDao().deletePrivateGroups();
            return null;
        }, null);
    }

    public void updateGroupBlocking(TabGroup group) {
        runBlocking(() -> {
            database.tabGroupDao().updateGroup(toEntity(group));
            return null;
        }, null);
    }

    public void deleteGroupBlocking(String groupId) {
        runBlocking(() -> {
            database.tabGroupDao().deleteGroupById(groupId);
            return null;
        }, null);
    }

    public void updateThumbnailPathBlocking(String tabId, String thumbnailPath) {
        runBlocking(() -> {
            database.tabGroupDao().updateThumbnailPath(tabId, thumbnailPath);
            return null;
        }, null);
    }

    public void moveTabToGroupBlocking(String tabId, String groupId, int position) {
        runBlocking(() -> {
            String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
            database.tabGroupDao().moveTabToGroup(tabId, groupId, position);
            touchGroup(groupId);
            enforceMinimumGroupSize(sourceGroupId);
            return null;
        }, null);
    }

    public void removeTabFromGroupBlocking(String tabId) {
        runBlocking(() -> {
            String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
            database.tabGroupDao().removeTabFromGroup(tabId);
            enforceMinimumGroupSize(sourceGroupId);
            return null;
        }, null);
    }

    public TabGroup createGroupForTabsBlocking(List<String> tabIds, String groupName,
                                               int groupColor, boolean isPrivate) {
        return runBlocking(() -> {
            if (isPrivate || tabIds == null || tabIds.size() < 2) {
                return null;
            }
            TabGroup group = new TabGroup(groupName, groupColor);
            group.setPrivate(isPrivate);
            database.tabGroupDao().insertGroup(toEntity(group));
            int position = 0;
            List<String> sourceGroupIds = new ArrayList<>();
            for (String tabId : tabIds) {
                String sourceGroupId = database.tabGroupDao().getGroupIdForTab(tabId);
                if (sourceGroupId != null && !sourceGroupIds.contains(sourceGroupId)) {
                    sourceGroupIds.add(sourceGroupId);
                }
                database.tabGroupDao().moveTabToGroup(tabId, group.getGroupId(), position++);
            }
            touchGroup(group.getGroupId());
            for (String sourceGroupId : sourceGroupIds) {
                enforceMinimumGroupSize(sourceGroupId);
            }
            return group;
        }, null);
    }

    public void updateTabPositionsBlocking(List<Tab> tabs) {
        runBlocking(() -> {
            if (tabs != null) {
                for (int i = 0; i < tabs.size(); i++) {
                    database.tabGroupDao().updateTabPosition(tabs.get(i).getId(), i);
                }
                if (!tabs.isEmpty()) {
                    touchGroup(tabs.get(0).getGroupId());
                }
            }
            return null;
        }, null);
    }

    private void ensureGroupExists(String groupId, String groupName, int groupColor, boolean isPrivate) {
        if (groupId == null) {
            return;
        }
        if (database.tabGroupDao().getGroupById(groupId) == null) {
            String fallbackName = isPrivate ? "Private Group" : "Group";
            String name = groupName != null && !groupName.trim().isEmpty() ? groupName : fallbackName;
            int color = groupColor != 0 ? groupColor : getDefaultGroupColor(appContext);
            database.tabGroupDao().insertGroup(
                    new TabGroupEntity(groupId, name, color, isPrivate,
                            System.currentTimeMillis(), System.currentTimeMillis()));
        }
    }

    private TabGroup toModel(TabGroupEntity entity) {
        return new TabGroup(entity.getGroupId(), entity.getGroupName(),
                entity.getGroupColor(), entity.isPrivate(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private TabGroupEntity toEntity(TabGroup group) {
        return new TabGroupEntity(group.getGroupId(), group.getGroupName(),
                group.getGroupColor(), group.isPrivate(),
                group.getCreatedAt(), group.getUpdatedAt() != 0
                ? group.getUpdatedAt()
                : System.currentTimeMillis());
    }

    private List<Tab> toTabs(List<TabEntity> entities) {
        return toTabs(entities, null);
    }

    private List<Tab> toTabs(List<TabEntity> entities, TabGroupEntity group) {
        List<Tab> tabs = new ArrayList<>();
        if (entities == null) {
            return tabs;
        }
        for (TabEntity entity : entities) {
            Tab tab = new Tab(entity.getTabId(), null, entity.getTitle(), entity.getUrl(), entity.isPrivate());
            tab.setGroupId(entity.getGroupId());
            if (group != null) {
                tab.setGroupName(group.getGroupName());
                tab.setGroupColor(group.getGroupColor());
                tab.setPrivate(group.isPrivate());
            }
            tab.setThumbnailPath(entity.getThumbnailPath());
            tab.setSessionState(entity.getSessionState());
            tab.setCreatedAt(entity.getLastAccessed());
            tab.setLastAccessed(entity.getLastAccessed());
            tab.setPosition(entity.getPosition());
            tab.setPinned(entity.isPinned());
            tab.setFaviconUri(entity.getFaviconPath() != null
                    ? entity.getFaviconPath()
                    : entity.getFavicon());
            tabs.add(tab);
        }
        Collections.sort(tabs, new Comparator<Tab>() {
            @Override
            public int compare(Tab first, Tab second) {
                if (first.isPinned() != second.isPinned()) {
                    return first.isPinned() ? -1 : 1;
                }
                int positionCompare = Integer.compare(first.getPosition(), second.getPosition());
                if (positionCompare != 0) {
                    return positionCompare;
                }
                return Long.compare(second.getLastAccessed(), first.getLastAccessed());
            }
        });
        return tabs;
    }

    private List<TabGroup> toGroups(List<TabGroupWithTabs> rows) {
        List<TabGroup> groups = new ArrayList<>();
        if (rows == null) {
            return groups;
        }
        for (TabGroupWithTabs row : rows) {
            TabGroup group = toModel(row.group);
            group.setTabs(toTabs(row.tabs, row.group));
            if (group.getTabCount() >= 2) {
                groups.add(group);
            }
        }
        return groups;
    }

    private TabEntity toEntity(Tab tab) {
        String groupId = tab.getGroupId();
        TabEntity entity = new TabEntity(tab.getId(), groupId,
                tab.getTitle(), tab.getUrl());
        entity.setFavicon(tab.getFaviconUri());
        entity.setFaviconPath(tab.getFaviconUri());
        entity.setThumbnailPath(tab.getThumbnailPath());
        entity.setSessionState(tab.getSessionState());
        entity.setPrivate(tab.isPrivate());
        entity.setLastAccessed(tab.getLastAccessed());
        entity.setPosition(tab.getPosition());
        entity.setPinned(tab.isPinned());
        return entity;
    }

    private void touchGroup(String groupId) {
        if (groupId != null) {
            database.tabGroupDao().updateGroupTimestamp(groupId, System.currentTimeMillis());
        }
    }

    private void enforceMinimumGroupSize(String groupId) {
        if (groupId == null) {
            return;
        }
        int count = database.tabGroupDao().getTabCountForGroup(groupId);
        if (count < 2) {
            database.tabGroupDao().clearGroup(groupId);
            database.tabGroupDao().deleteGroupById(groupId);
        }
    }

    private List<TabEntity> getStandaloneTabsAfterCleanup(boolean isPrivate) {
        cleanupInvalidGroups();
        return database.tabGroupDao().getStandaloneTabs(isPrivate);
    }

    private void cleanupInvalidGroups() {
        List<TabGroupEntity> groups = database.tabGroupDao().getAllGroups();
        if (groups == null) {
            return;
        }
        for (TabGroupEntity group : groups) {
            if (group != null) {
                enforceMinimumGroupSize(group.getGroupId());
            }
        }
    }

    private <T> T runBlocking(Callable<T> callable, T fallback) {
        FutureTask<T> task = new FutureTask<>(callable);
        executor.execute(task);
        try {
            return task.get();
        } catch (Exception e) {
            return fallback;
        }
    }
}
