package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "tab_groups")
public class TabGroupEntity {
    @PrimaryKey
    @NonNull
    private String groupId;
    private String groupName;
    private int groupColor;
    private boolean isPrivate;
    private long createdAt;
    private long updatedAt;

    @Ignore
    public TabGroupEntity(@NonNull String groupId, String groupName, int groupColor, long createdAt) {
        this(groupId, groupName, groupColor, false, createdAt, createdAt);
    }

    public TabGroupEntity(@NonNull String groupId, String groupName, int groupColor,
                          boolean isPrivate, long createdAt, long updatedAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(@NonNull String groupId) {
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
}
