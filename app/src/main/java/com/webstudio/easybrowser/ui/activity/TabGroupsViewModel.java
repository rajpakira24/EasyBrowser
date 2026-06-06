package com.webstudio.easybrowser.ui.activity;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.TabRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TabGroupsViewModel extends AndroidViewModel {
    private final TabRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GroupsCallback {
        void onGroupsLoaded(List<TabGroup> groups);
    }

    public interface OverviewCallback {
        void onOverviewLoaded(List<TabGroup> groups, List<Tab> standaloneTabs);
    }

    public interface CountsCallback {
        void onCountsLoaded(int regularCount, int privateCount);
    }

    public TabGroupsViewModel(@NonNull Application application) {
        super(application);
        repository = new TabRepository(application.getApplicationContext());
    }

    public void loadGroups(String query, GroupsCallback callback) {
        loadGroups(false, query, callback);
    }

    public void loadGroups(boolean isPrivate, String query, GroupsCallback callback) {
        repository.getGroups(isPrivate, groups -> {
            List<TabGroup> filtered = filterGroups(groups, query);
            mainHandler.post(() -> callback.onGroupsLoaded(filtered));
        });
    }

    public void loadOverview(boolean isPrivate, String query, OverviewCallback callback) {
        repository.getGroups(isPrivate, groups ->
                repository.getStandaloneTabs(isPrivate, tabs -> {
                    List<TabGroup> filteredGroups = filterGroups(groups, query);
                    List<Tab> filteredTabs = filterTabs(tabs, query);
                    mainHandler.post(() -> callback.onOverviewLoaded(filteredGroups, filteredTabs));
                }));
    }

    public void loadCounts(CountsCallback callback) {
        repository.getTabCounts((regularCount, privateCount) ->
                mainHandler.post(() -> callback.onCountsLoaded(regularCount, privateCount)));
    }

    public void saveGroup(TabGroup group, Runnable completion) {
        repository.saveGroup(group, () -> postCompletion(completion));
    }

    public void renameGroup(String groupId, String groupName, Runnable completion) {
        repository.updateGroupName(groupId, groupName, () -> postCompletion(completion));
    }

    public void updateGroupColor(String groupId, int groupColor, Runnable completion) {
        repository.updateGroupColor(groupId, groupColor, () -> postCompletion(completion));
    }

    public void moveTabToGroup(String tabId, String groupId, Runnable completion) {
        repository.moveTabToGroup(tabId, groupId, () -> postCompletion(completion));
    }

    public void removeTabFromGroup(String tabId, Runnable completion) {
        repository.removeTabFromGroup(tabId, () -> postCompletion(completion));
    }

    public void moveTabsToGroup(List<String> tabIds, String groupId, Runnable completion) {
        repository.moveTabsToGroup(tabIds, groupId, () -> postCompletion(completion));
    }

    public void createGroupForTabs(List<String> tabIds, String groupName, int groupColor,
                                   boolean isPrivate, Runnable completion) {
        repository.createGroupForTabs(tabIds, groupName, groupColor, isPrivate,
                () -> postCompletion(completion));
    }

    public void deleteTabs(List<String> tabIds, Runnable completion) {
        repository.deleteTabs(tabIds, () -> postCompletion(completion));
    }

    public void deleteTab(String tabId, Runnable completion) {
        repository.deleteTab(tabId, () -> postCompletion(completion));
    }

    public void saveTab(Tab tab, Runnable completion) {
        repository.saveTab(tab, () -> postCompletion(completion));
    }

    public void deleteGroup(String groupId, Runnable completion) {
        repository.deleteGroup(groupId, () -> postCompletion(completion));
    }

    public void deleteGroups(List<String> groupIds, Runnable completion) {
        repository.deleteGroups(groupIds, () -> postCompletion(completion));
    }

    public void moveGroupsToGroup(List<String> sourceGroupIds, String targetGroupId,
                                  boolean deleteSources, Runnable completion) {
        repository.moveGroupsToGroup(sourceGroupIds, targetGroupId, deleteSources,
                () -> postCompletion(completion));
    }

    public void updateTabPositions(List<Tab> tabs, Runnable completion) {
        repository.updateTabPositions(tabs, () -> postCompletion(completion));
    }

    private List<TabGroup> filterGroups(List<TabGroup> groups, String query) {
        if (query == null || query.trim().isEmpty()) {
            return groups;
        }
        String needle = query.trim().toLowerCase(Locale.US);
        List<TabGroup> filtered = new ArrayList<>();
        for (TabGroup group : groups) {
            if (contains(group.getGroupName(), needle)) {
                filtered.add(group);
                continue;
            }
            for (Tab tab : group.getTabs()) {
                if (contains(tab.getTitle(), needle) || contains(tab.getUrl(), needle)) {
                    filtered.add(group);
                    break;
                }
            }
        }
        return filtered;
    }

    private List<Tab> filterTabs(List<Tab> tabs, String query) {
        if (query == null || query.trim().isEmpty()) {
            return tabs;
        }
        String needle = query.trim().toLowerCase(Locale.US);
        List<Tab> filtered = new ArrayList<>();
        for (Tab tab : tabs) {
            if (contains(tab.getTitle(), needle) || contains(tab.getUrl(), needle)) {
                filtered.add(tab);
            }
        }
        return filtered;
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.US).contains(needle);
    }

    private void postCompletion(Runnable completion) {
        if (completion != null) {
            mainHandler.post(completion);
        }
    }
}
