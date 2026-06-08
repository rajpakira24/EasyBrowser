package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "tabs",
        foreignKeys = @ForeignKey(
                entity = TabGroupEntity.class,
                parentColumns = "groupId",
                childColumns = "groupId",
                onDelete = ForeignKey.SET_NULL),
        indices = {
                @Index("groupId"),
                @Index("lastAccessed"),
                @Index(value = {"groupId", "position"})
        })
public class TabEntity {
    @PrimaryKey
    @NonNull
    private String tabId;
    private String groupId;
    private String title;
    private String url;
    private String favicon;
    private String faviconPath;
    private String thumbnailPath;
    private String sessionState;
    private boolean isPrivate;
    private long lastAccessed;
    private int position;
    private boolean pinned;
    private boolean locked;
    private int scrollY;

    public TabEntity(@NonNull String tabId, String groupId, String title, String url) {
        this.tabId = tabId;
        this.groupId = groupId;
        this.title = title;
        this.url = url;
        this.lastAccessed = System.currentTimeMillis();
    }

    @NonNull
    public String getTabId() {
        return tabId;
    }

    public void setTabId(@NonNull String tabId) {
        this.tabId = tabId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getFaviconPath() {
        return faviconPath;
    }

    public void setFaviconPath(String faviconPath) {
        this.faviconPath = faviconPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = Math.max(0, scrollY);
    }
}
