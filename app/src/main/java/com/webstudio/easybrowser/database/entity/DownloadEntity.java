package com.webstudio.easybrowser.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloads")
public class DownloadEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String url;
    private String fileName;
    private String mimeType;
    private String destinationPath;
    private long totalBytes;
    private long downloadedBytes;
    private String status;
    private String errorMessage;
    private long startTime;
    private long lastModified;
    private long speedBytesPerSecond;
    private long remainingSeconds;

    // Constructor
    public DownloadEntity(@NonNull String id, String url, String fileName) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.startTime = System.currentTimeMillis();
        this.lastModified = startTime;
        this.status = "PENDING";
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getDestinationPath() { return destinationPath; }
    public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public long getDownloadedBytes() { return downloadedBytes; }
    public void setDownloadedBytes(long downloadedBytes) { this.downloadedBytes = downloadedBytes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    public long getSpeedBytesPerSecond() { return speedBytesPerSecond; }
    public void setSpeedBytesPerSecond(long speedBytesPerSecond) { this.speedBytesPerSecond = speedBytesPerSecond; }
    public long getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(long remainingSeconds) { this.remainingSeconds = remainingSeconds; }
}
