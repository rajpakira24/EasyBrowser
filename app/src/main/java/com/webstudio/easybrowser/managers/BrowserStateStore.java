package com.webstudio.easybrowser.managers;

import com.webstudio.easybrowser.models.BrowserState;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrowserStateStore {
    private final LinkedHashMap<String, Tab> regularTabs = new LinkedHashMap<>();
    private final LinkedHashMap<String, Tab> privateTabs = new LinkedHashMap<>();
    private final LinkedHashMap<String, TabGroup> groups = new LinkedHashMap<>();
    private String activeRegularTabId;
    private String activePrivateTabId;
    private boolean privateMode;

    public synchronized void loadRegularState(List<Tab> tabs, List<TabGroup> restoredGroups,
                                              String activeTabId) {
        regularTabs.clear();
        groups.entrySet().removeIf(entry -> !entry.getValue().isPrivate());
        if (tabs != null) {
            for (Tab tab : tabs) {
                if (tab == null || tab.isPrivate()) {
                    continue;
                }
                regularTabs.put(tab.getId(), tab);
            }
        }
        if (restoredGroups != null) {
            for (TabGroup group : restoredGroups) {
                if (group != null && !group.isPrivate()) {
                    groups.put(group.getGroupId(), group);
                }
            }
        }
        normalizeGroups(false);
        activeRegularTabId = regularTabs.containsKey(activeTabId)
                ? activeTabId
                : lastKey(regularTabs);
        if (activePrivateTabId == null) {
            privateMode = false;
        }
    }

    public synchronized void addTab(Tab tab, boolean activate) {
        if (tab == null) {
            return;
        }
        if (tab.isPrivate()) {
            makeStandalone(tab);
            privateTabs.put(tab.getId(), tab);
            if (activate || activePrivateTabId == null) {
                activePrivateTabId = tab.getId();
                privateMode = true;
            }
        } else {
            regularTabs.put(tab.getId(), tab);
            if (activate || activeRegularTabId == null) {
                activeRegularTabId = tab.getId();
                privateMode = false;
            }
        }
        normalizeGroups(tab.isPrivate());
    }

    public synchronized boolean switchToTab(String tabId) {
        Tab tab = findTabById(tabId);
        if (tab == null) {
            return false;
        }
        tab.setLastAccessed(System.currentTimeMillis());
        if (tab.isPrivate()) {
            activePrivateTabId = tab.getId();
            privateMode = true;
        } else {
            activeRegularTabId = tab.getId();
            privateMode = false;
        }
        return true;
    }

    public synchronized Tab closeTab(String tabId) {
        Tab removed = regularTabs.remove(tabId);
        boolean wasPrivate = false;
        if (removed == null) {
            removed = privateTabs.remove(tabId);
            wasPrivate = removed != null;
        }
        if (removed == null) {
            return null;
        }
        if (removed.getGroupId() != null) {
            normalizeGroups(wasPrivate);
        }
        if (tabId.equals(activeRegularTabId)) {
            activeRegularTabId = lastKey(regularTabs);
        }
        if (tabId.equals(activePrivateTabId)) {
            activePrivateTabId = lastKey(privateTabs);
        }
        if (privateMode && activePrivateTabId == null) {
            privateMode = false;
        }
        return removed;
    }

    public synchronized List<Tab> closeAll(boolean privateTabsMode) {
        List<Tab> closed = new ArrayList<>(privateTabsMode
                ? privateTabs.values()
                : regularTabs.values());
        if (privateTabsMode) {
            privateTabs.clear();
            groups.entrySet().removeIf(entry -> entry.getValue().isPrivate());
            activePrivateTabId = null;
            privateMode = false;
        } else {
            regularTabs.clear();
            groups.entrySet().removeIf(entry -> !entry.getValue().isPrivate());
            activeRegularTabId = null;
            privateMode = activePrivateTabId != null;
        }
        return closed;
    }

    public synchronized TabGroup createGroupForTabs(List<String> tabIds, String groupName,
                                                    int groupColor, boolean privateGroup) {
        return createGroupForTabs(tabIds, groupName, groupColor, privateGroup, null);
    }

    public synchronized TabGroup createGroupForTabs(List<String> tabIds, String groupName,
                                                    int groupColor, boolean privateGroup,
                                                    String requestedGroupId) {
        List<Tab> selected = resolveTabs(tabIds, privateGroup);
        if (selected.size() < 2) {
            return null;
        }
        Set<String> affectedGroupIds = new LinkedHashSet<>();
        for (Tab tab : selected) {
            if (tab.getGroupId() != null) {
                affectedGroupIds.add(tab.getGroupId());
            }
        }
        long now = System.currentTimeMillis();
        TabGroup group = new TabGroup(requestedGroupId, groupName, groupColor,
                privateGroup, now, now);
        for (Tab tab : selected) {
            tab.setGroupId(group.getGroupId());
            tab.setGroupName(group.getGroupName());
            tab.setGroupColor(group.getGroupColor());
        }
        groups.put(group.getGroupId(), group);
        for (String affectedGroupId : affectedGroupIds) {
            normalizeGroup(affectedGroupId);
        }
        normalizeGroup(group.getGroupId());
        return groups.get(group.getGroupId());
    }

    public synchronized boolean addTabToGroup(String tabId, String groupId) {
        return moveTabToGroup(tabId, groupId);
    }

    public synchronized boolean addTabToGroup(Tab tab, String groupId,
                                             String groupName, int groupColor) {
        if (tab == null || groupId == null) {
            return false;
        }
        return moveTabToGroup(tab.getId(), groupId);
    }

    public synchronized boolean moveTabToGroup(String tabId, String groupId) {
        Tab tab = findTabById(tabId);
        TabGroup target = groups.get(groupId);
        if (tab == null || target == null || tab.isPrivate() != target.isPrivate()) {
            return false;
        }
        String sourceGroupId = tab.getGroupId();
        tab.setGroupId(target.getGroupId());
        tab.setGroupName(target.getGroupName());
        tab.setGroupColor(target.getGroupColor());
        if (sourceGroupId != null && !sourceGroupId.equals(target.getGroupId())) {
            normalizeGroup(sourceGroupId);
        }
        normalizeGroup(target.getGroupId());
        return true;
    }

    public synchronized boolean removeTabFromGroup(String tabId) {
        Tab tab = findTabById(tabId);
        if (tab == null || tab.getGroupId() == null) {
            return false;
        }
        String sourceGroupId = tab.getGroupId();
        makeStandalone(tab);
        normalizeGroup(sourceGroupId);
        return true;
    }

    public synchronized boolean ungroup(String groupId) {
        TabGroup group = groups.remove(groupId);
        if (group == null) {
            return false;
        }
        for (Tab tab : getTabsForGroup(groupId, group.isPrivate())) {
            makeStandalone(tab);
        }
        return true;
    }

    public synchronized List<Tab> closeGroup(String groupId) {
        TabGroup group = groups.remove(groupId);
        if (group == null) {
            return new ArrayList<>();
        }
        List<Tab> closed = new ArrayList<>(getTabsForGroup(groupId, group.isPrivate()));
        for (Tab tab : closed) {
            if (group.isPrivate()) {
                privateTabs.remove(tab.getId());
                if (tab.getId().equals(activePrivateTabId)) {
                    activePrivateTabId = lastKey(privateTabs);
                }
            } else {
                regularTabs.remove(tab.getId());
                if (tab.getId().equals(activeRegularTabId)) {
                    activeRegularTabId = lastKey(regularTabs);
                }
            }
        }
        if (privateMode && activePrivateTabId == null) {
            privateMode = false;
        }
        return closed;
    }

    public synchronized boolean renameGroup(String groupId, String groupName) {
        TabGroup group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        group.setGroupName(groupName);
        for (Tab tab : getTabsForGroup(groupId, group.isPrivate())) {
            tab.setGroupName(groupName);
        }
        return true;
    }

    public synchronized boolean changeGroupColor(String groupId, int groupColor) {
        TabGroup group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        group.setGroupColor(groupColor);
        for (Tab tab : getTabsForGroup(groupId, group.isPrivate())) {
            tab.setGroupColor(groupColor);
        }
        return true;
    }

    public synchronized boolean reorderTabs(List<String> orderedTabIds, boolean privateTabsMode) {
        if (orderedTabIds == null || orderedTabIds.isEmpty()) {
            return false;
        }
        LinkedHashMap<String, Tab> source = privateTabsMode ? privateTabs : regularTabs;
        List<Tab> orderedTabs = resolveTabs(orderedTabIds, privateTabsMode);
        if (orderedTabs.size() != orderedTabIds.size()) {
            return false;
        }
        Set<String> orderedIdSet = new LinkedHashSet<>(orderedTabIds);
        LinkedHashMap<String, Tab> reordered = new LinkedHashMap<>();
        boolean insertedOrderedBlock = false;
        for (Map.Entry<String, Tab> entry : source.entrySet()) {
            if (orderedIdSet.contains(entry.getKey())) {
                if (!insertedOrderedBlock) {
                    for (int i = 0; i < orderedTabs.size(); i++) {
                        Tab tab = orderedTabs.get(i);
                        tab.setPosition(i);
                        reordered.put(tab.getId(), tab);
                    }
                    insertedOrderedBlock = true;
                }
                continue;
            }
            reordered.put(entry.getKey(), entry.getValue());
        }
        if (privateTabsMode) {
            privateTabs.clear();
            privateTabs.putAll(reordered);
        } else {
            regularTabs.clear();
            regularTabs.putAll(reordered);
        }
        normalizeGroups(privateTabsMode);
        return true;
    }

    public synchronized void updateTabAccess(String tabId, long lastAccessed) {
        Tab tab = findTabById(tabId);
        if (tab != null) {
            tab.setLastAccessed(lastAccessed);
        }
    }

    public synchronized Tab findTabById(String tabId) {
        if (tabId == null) {
            return null;
        }
        Tab tab = regularTabs.get(tabId);
        return tab != null ? tab : privateTabs.get(tabId);
    }

    public synchronized Tab getCurrentTab() {
        Tab tab = privateMode ? privateTabs.get(activePrivateTabId) : regularTabs.get(activeRegularTabId);
        if (tab != null) {
            return tab;
        }
        return activeRegularTabId != null ? regularTabs.get(activeRegularTabId) : null;
    }

    public synchronized List<Tab> getAllTabs() {
        List<Tab> allTabs = new ArrayList<>(regularTabs.size() + privateTabs.size());
        allTabs.addAll(regularTabs.values());
        allTabs.addAll(privateTabs.values());
        return allTabs;
    }

    public synchronized List<Tab> getRegularTabs() {
        return new ArrayList<>(regularTabs.values());
    }

    public synchronized List<Tab> getPrivateTabs() {
        return new ArrayList<>(privateTabs.values());
    }

    public synchronized List<Tab> getTabs(boolean privateTabsMode) {
        return privateTabsMode ? getPrivateTabs() : getRegularTabs();
    }

    public synchronized List<TabGroup> getGroups(boolean privateGroupsMode) {
        List<TabGroup> result = new ArrayList<>();
        for (TabGroup group : groups.values()) {
            if (group.isPrivate() == privateGroupsMode) {
                result.add(group);
            }
        }
        return result;
    }

    public synchronized List<TabGroup> getPersistableGroups() {
        return getGroups(false);
    }

    public synchronized int getTabCount() {
        return regularTabs.size() + privateTabs.size();
    }

    public synchronized int getTabCount(boolean privateTabsMode) {
        return privateTabsMode ? privateTabs.size() : regularTabs.size();
    }

    public synchronized BrowserState snapshot() {
        return new BrowserState(
                getRegularTabs(),
                getPrivateTabs(),
                getGroups(false),
                getGroups(true),
                activeRegularTabId,
                activePrivateTabId,
                getActiveGroupId(),
                privateMode);
    }

    private String getActiveGroupId() {
        Tab active = getCurrentTab();
        return active != null ? active.getGroupId() : null;
    }

    private List<Tab> resolveTabs(List<String> tabIds, boolean privateTabsMode) {
        List<Tab> resolved = new ArrayList<>();
        if (tabIds == null) {
            return resolved;
        }
        Set<String> seenIds = new LinkedHashSet<>();
        for (String tabId : tabIds) {
            if (tabId == null || !seenIds.add(tabId)) {
                continue;
            }
            Tab tab = privateTabsMode ? privateTabs.get(tabId) : regularTabs.get(tabId);
            if (tab != null) {
                resolved.add(tab);
            }
        }
        return resolved;
    }

    private void normalizeGroups(boolean privateGroupsMode) {
        normalizeTabMemberships(privateGroupsMode);
        List<String> ids = new ArrayList<>();
        for (TabGroup group : groups.values()) {
            if (group.isPrivate() == privateGroupsMode) {
                ids.add(group.getGroupId());
            }
        }
        for (String groupId : ids) {
            normalizeGroup(groupId);
        }
    }

    private void normalizeTabMemberships(boolean privateTabsMode) {
        Map<String, Tab> source = privateTabsMode ? privateTabs : regularTabs;
        for (Tab tab : source.values()) {
            String groupId = tab.getGroupId();
            if (groupId == null) {
                makeStandalone(tab);
                continue;
            }
            TabGroup group = groups.get(groupId);
            if (group == null || group.isPrivate() != tab.isPrivate()) {
                makeStandalone(tab);
            }
        }
    }

    private void normalizeGroup(String groupId) {
        TabGroup group = groups.get(groupId);
        if (group == null) {
            return;
        }
        List<Tab> groupTabs = getTabsForGroup(groupId, group.isPrivate());
        if (groupTabs.size() < 2) {
            for (Tab tab : groupTabs) {
                makeStandalone(tab);
            }
            groups.remove(groupId);
            return;
        }
        for (Tab tab : groupTabs) {
            tab.setGroupName(group.getGroupName());
            tab.setGroupColor(group.getGroupColor());
        }
        group.setTabs(groupTabs);
    }

    private List<Tab> getTabsForGroup(String groupId, boolean privateTabsMode) {
        List<Tab> result = new ArrayList<>();
        Map<String, Tab> source = privateTabsMode ? privateTabs : regularTabs;
        for (Tab tab : source.values()) {
            if (groupId.equals(tab.getGroupId())) {
                result.add(tab);
            }
        }
        return result;
    }

    private void makeStandalone(Tab tab) {
        tab.setGroupId(null);
        tab.setGroupName(null);
        tab.setGroupColor(0);
    }

    private static String lastKey(LinkedHashMap<String, Tab> tabs) {
        String last = null;
        for (String key : tabs.keySet()) {
            last = key;
        }
        return last;
    }
}
