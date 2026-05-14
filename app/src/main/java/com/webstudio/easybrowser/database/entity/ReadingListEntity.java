package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reading_list")
public class ReadingListEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long savedAt;
    private String contentPath;

    public ReadingListEntity(@NonNull String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.savedAt = System.currentTimeMillis();
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }
    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }
    public String getContentPath() { return contentPath; }
    public void setContentPath(String contentPath) { this.contentPath = contentPath; }
}
