package com.webstudio.easybrowser.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowserState {
    private final List<Tab> regularTabs;
    private final List<Tab> privateTabs;
    private final List<TabGroup> regularGroups;
    private final List<TabGroup> privateGroups;
    private final String activeRegularTabId;
    private final String activePrivateTabId;
    private final String activeGroupId;
    private final boolean privateMode;

    public BrowserState(List<Tab> regularTabs, List<Tab> privateTabs,
                        List<TabGroup> regularGroups, List<TabGroup> privateGroups,
                        String activeRegularTabId, String activePrivateTabId,
                        String activeGroupId, boolean privateMode) {
        this.regularTabs = copyTabs(regularTabs);
        this.privateTabs = copyTabs(privateTabs);
        this.regularGroups = copyGroups(regularGroups);
        this.privateGroups = copyGroups(privateGroups);
        this.activeRegularTabId = activeRegularTabId;
        this.activePrivateTabId = activePrivateTabId;
        this.activeGroupId = activeGroupId;
        this.privateMode = privateMode;
    }

    public List<Tab> getRegularTabs() {
        return regularTabs;
    }

    public List<Tab> getPrivateTabs() {
        return privateTabs;
    }

    public List<Tab> getAllTabs() {
        List<Tab> allTabs = new ArrayList<>(regularTabs.size() + privateTabs.size());
        allTabs.addAll(regularTabs);
        allTabs.addAll(privateTabs);
        return Collections.unmodifiableList(allTabs);
    }

    public List<TabGroup> getRegularGroups() {
        return regularGroups;
    }

    public List<TabGroup> getPrivateGroups() {
        return privateGroups;
    }

    public List<TabGroup> getGroups(boolean privateMode) {
        return privateMode ? privateGroups : regularGroups;
    }

    public String getActiveRegularTabId() {
        return activeRegularTabId;
    }

    public String getActivePrivateTabId() {
        return activePrivateTabId;
    }

    public String getActiveTabId() {
        return privateMode ? activePrivateTabId : activeRegularTabId;
    }

    public String getActiveGroupId() {
        return activeGroupId;
    }

    public boolean isPrivateMode() {
        return privateMode;
    }

    public int getRegularTabCount() {
        return regularTabs.size();
    }

    public int getPrivateTabCount() {
        return privateTabs.size();
    }

    private static List<Tab> copyTabs(List<Tab> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static List<TabGroup> copyGroups(List<TabGroup> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
