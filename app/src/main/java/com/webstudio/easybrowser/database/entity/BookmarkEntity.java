package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookmarkEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long createdAt;
    private String folder;

    // Constructor
    public BookmarkEntity(@NonNull String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.createdAt = System.currentTimeMillis();
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
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
}

