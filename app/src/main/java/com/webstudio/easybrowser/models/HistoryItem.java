package com.webstudio.easybrowser.models;

public class HistoryItem {
    private String id;
    private String title;
    private String url;
    private String favicon;
    private long visitTime;
    private int visitCount;

    public HistoryItem(String title, String url) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.url = url;
        this.visitTime = System.currentTimeMillis();
        this.visitCount = 1;
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

    public long getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(long visitTime) {
        this.visitTime = visitTime;
    }

    public void updateVisitTime() {
        this.visitTime = System.currentTimeMillis();
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public void incrementVisitCount() {
        this.visitCount++;
        updateVisitTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryItem that = (HistoryItem) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
