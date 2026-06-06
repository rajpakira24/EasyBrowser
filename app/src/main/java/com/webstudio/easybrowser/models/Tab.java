package com.webstudio.easybrowser.models;

import android.graphics.Bitmap;

import org.mozilla.geckoview.GeckoSession;

import java.util.UUID;

public class Tab {
    private String id;
    private String title;
    private String url;
    private GeckoSession session;
    private Bitmap favicon;
    private boolean isPrivate;
    private boolean initialLoadPending;
    private String groupName;
    private String groupId;
    private int groupColor;
    private String faviconUri;
    private String thumbnailPath;
    private long createdAt;
    private long lastAccessed;
    private int position;
    private boolean pinned;
    private boolean canGoBack;
    private boolean canGoForward;
    private boolean closeOnBackToPreviousTab;
    private String sessionState;
    private String parentTabId;

    public Tab(GeckoSession session, String title, String url) {
        this(UUID.randomUUID().toString(), session, title, url, false);
    }

    public Tab(String id, GeckoSession session, String title, String url, boolean isPrivate) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.session = session;
        this.title = title;
        this.url = url;
        this.isPrivate = isPrivate;
        this.initialLoadPending = false;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessed = createdAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
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

    public Bitmap getFavicon() {
        return favicon;
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;
    }

    public GeckoSession getSession() {
        return session;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public boolean isInitialLoadPending() {
        return initialLoadPending;
    }

    public void setInitialLoadPending(boolean initialLoadPending) {
        this.initialLoadPending = initialLoadPending;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getGroupColor() {
        return groupColor;
    }

    public void setGroupColor(int groupColor) {
        this.groupColor = groupColor;
    }

    public String getFaviconUri() {
        return faviconUri;
    }

    public void setFaviconUri(String faviconUri) {
        this.faviconUri = faviconUri;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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

    public boolean canGoBack() {
        return canGoBack;
    }

    public void setCanGoBack(boolean canGoBack) {
        this.canGoBack = canGoBack;
    }

    public boolean canGoForward() {
        return canGoForward;
    }

    public void setCanGoForward(boolean canGoForward) {
        this.canGoForward = canGoForward;
    }

    public boolean shouldCloseOnBackToPreviousTab() {
        return closeOnBackToPreviousTab;
    }

    public void setCloseOnBackToPreviousTab(boolean closeOnBackToPreviousTab) {
        this.closeOnBackToPreviousTab = closeOnBackToPreviousTab;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getParentTabId() {
        return parentTabId;
    }

    public void setParentTabId(String parentTabId) {
        this.parentTabId = parentTabId;
    }
}
