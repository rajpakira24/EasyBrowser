package com.webstudio.easybrowser.ui.activity;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.TabRepository;

import java.util.List;

public class GroupTabsViewModel extends AndroidViewModel {
    private final TabRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface TabsCallback {
        void onTabsLoaded(List<Tab> tabs);
    }

    public interface GroupsCallback {
        void onGroupsLoaded(List<TabGroup> groups);
    }

    public GroupTabsViewModel(@NonNull Application application) {
        super(application);
        repository = new TabRepository(application.getApplicationContext());
    }

    public void loadTabs(String groupId, TabsCallback callback) {
        repository.getTabsForGroup(groupId, tabs -> mainHandler.post(() -> callback.onTabsLoaded(tabs)));
    }

    public void loadGroups(GroupsCallback callback) {
        loadGroups(false, callback);
    }

    public void loadGroups(boolean isPrivate, GroupsCallback callback) {
        repository.getGroups(isPrivate, groups -> mainHandler.post(() -> callback.onGroupsLoaded(groups)));
    }

    public void saveGroup(TabGroup group, Runnable completion) {
        repository.saveGroup(group, () -> postCompletion(completion));
    }

    public void updateGroupName(String groupId, String groupName, Runnable completion) {
        repository.updateGroupName(groupId, groupName, () -> postCompletion(completion));
    }

    public void updateGroupColor(String groupId, int groupColor, Runnable completion) {
        repository.updateGroupColor(groupId, groupColor, () -> postCompletion(completion));
    }

    public void deleteGroup(String groupId, Runnable completion) {
        repository.deleteGroup(groupId, () -> postCompletion(completion));
    }

    public void deleteTab(String tabId, Runnable completion) {
        repository.deleteTab(tabId, () -> postCompletion(completion));
    }

    public void deleteTabs(List<String> tabIds, Runnable completion) {
        repository.deleteTabs(tabIds, () -> postCompletion(completion));
    }

    public void saveTab(Tab tab, Runnable completion) {
        repository.saveTab(tab, () -> postCompletion(completion));
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

    public void updateTabPositions(List<Tab> tabs, Runnable completion) {
        repository.updateTabPositions(tabs, () -> postCompletion(completion));
    }

    private void postCompletion(Runnable completion) {
        if (completion != null) {
            mainHandler.post(completion);
        }
    }
}
