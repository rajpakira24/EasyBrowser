package com.webstudio.easybrowser.models;

public class QuickAccessItem {
    private String id;
    private String title;
    private String url;
    private String faviconUrl;
    private int visitCount;
    private long lastVisited;

    public QuickAccessItem(String title, String url) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.url = url;
        this.visitCount = 0;
        this.lastVisited = System.currentTimeMillis();
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

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public void incrementVisitCount() {
        this.visitCount++;
        this.lastVisited = System.currentTimeMillis();
    }

    public long getLastVisited() {
        return lastVisited;
    }

    public void setLastVisited(long lastVisited) {
        this.lastVisited = lastVisited;
    }

    public void updateLastVisited() {
        this.lastVisited = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickAccessItem that = (QuickAccessItem) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
