package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long visitTime;
    private int visitCount;

    // Constructor
    public HistoryEntity(@NonNull String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.visitTime = System.currentTimeMillis();
        this.visitCount = 1;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }
    public long getVisitTime() { return visitTime; }
    public void setVisitTime(long visitTime) { this.visitTime = visitTime; }
    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
}
