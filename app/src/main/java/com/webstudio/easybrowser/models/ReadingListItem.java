package com.webstudio.easybrowser.models;

public class ReadingListItem {
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long savedAt;
    private String contentPath;

    public ReadingListItem(String id, String title, String url, long savedAt, String contentPath) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.savedAt = savedAt;
        this.contentPath = contentPath;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }
    public long getSavedAt() { return savedAt; }
    public String getContentPath() { return contentPath; }
    public void setContentPath(String contentPath) { this.contentPath = contentPath; }
}
