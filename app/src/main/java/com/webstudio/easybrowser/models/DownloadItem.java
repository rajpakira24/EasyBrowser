package com.webstudio.easybrowser.models;

public class DownloadItem {
    public enum Status {
        PENDING,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED,
        QUEUED
    }

    private String id;
    private String url;
    private String fileName;
    private String mimeType;
    private String destinationPath;
    private long totalBytes;
    private long downloadedBytes;
    private Status status;
    private String errorMessage;
    private long startTime;
    private long lastModified;
    private long speedBytesPerSecond;
    private long remainingSeconds;

    public DownloadItem(String url, String fileName, String mimeType) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.url = url;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.status = Status.PENDING;
        this.startTime = System.currentTimeMillis();
        this.lastModified = startTime;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
        this.lastModified = System.currentTimeMillis();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.lastModified = System.currentTimeMillis();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getSpeedBytesPerSecond() {
        return speedBytesPerSecond;
    }

    public void setSpeedBytesPerSecond(long speedBytesPerSecond) {
        this.speedBytesPerSecond = speedBytesPerSecond;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public int getProgress() {
        if (totalBytes <= 0) return 0;
        return (int) ((downloadedBytes * 100) / totalBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadItem that = (DownloadItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
