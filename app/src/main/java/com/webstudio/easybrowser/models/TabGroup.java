package com.webstudio.easybrowser.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TabGroup {
    private String groupId;
    private String groupName;
    private int groupColor;
    private boolean isPrivate;
    private long createdAt;
    private long updatedAt;
    private List<Tab> tabs = new ArrayList<>();

    public TabGroup(String groupName, int groupColor) {
        this(UUID.randomUUID().toString(), groupName, groupColor, System.currentTimeMillis());
    }

    public TabGroup(String groupId, String groupName, int groupColor, long createdAt) {
        this(groupId, groupName, groupColor, false, createdAt, createdAt);
    }

    public TabGroup(String groupId, String groupName, int groupColor,
                    boolean isPrivate, long createdAt, long updatedAt) {
        this.groupId = groupId != null ? groupId : UUID.randomUUID().toString();
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroupColor() {
        return groupColor;
    }

    public void setGroupColor(int groupColor) {
        this.groupColor = groupColor;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Tab> getTabs() {
        return tabs;
    }

    public void setTabs(List<Tab> tabs) {
        this.tabs = tabs != null ? tabs : new ArrayList<>();
    }

    public int getTabCount() {
        return tabs != null ? tabs.size() : 0;
    }
}
