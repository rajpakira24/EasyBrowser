package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quick_access")
public class QuickAccessEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String url;
    private String faviconUrl;
    private int visitCount;
    private long lastVisited;

    // Constructor
    public QuickAccessEntity(@NonNull String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.lastVisited = System.currentTimeMillis();
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }
    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
    public long getLastVisited() { return lastVisited; }
    public void setLastVisited(long lastVisited) { this.lastVisited = lastVisited; }
}
