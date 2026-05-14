package com.webstudio.easybrowser.models;

public class Bookmark {
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long createdAt;
    private String folder;

    public Bookmark(String title, String url) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.url = url;
        this.createdAt = System.currentTimeMillis();
        this.folder = ""; // Default folder
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return id.equals(bookmark.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
