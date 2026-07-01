package com.webstudio.easybrowser.repository;

import android.content.Context;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.DownloadEntity;
import com.webstudio.easybrowser.models.DownloadItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class DownloadRepository {
    private AppDatabase database;
    private Executor executor;

    public DownloadRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public interface DownloadCallback {
        void onDownloadsLoaded(List<DownloadItem> downloads);
        void onDownloadUpdated(DownloadItem download);
        void onDownloadRemoved(DownloadItem download);
    }

    public interface DownloadItemCallback {
        void onDownloadItemLoaded(DownloadItem item);
    }

    public void getAllDownloads(DownloadCallback callback) {
        executor.execute(() -> {
            List<DownloadEntity> entities = database.downloadDao().getAllDownloads();
            List<DownloadItem> items = new ArrayList<>();
            for (DownloadEntity entity : entities) {
                items.add(toDownloadItem(entity));
            }
            callback.onDownloadsLoaded(items);
        });
    }

    public void getDownloadById(String downloadId, DownloadItemCallback callback) {
        executor.execute(() -> {
            DownloadEntity entity = database.downloadDao().getDownloadById(downloadId);
            callback.onDownloadItemLoaded(entity != null ? toDownloadItem(entity) : null);
        });
    }

    private DownloadItem toDownloadItem(DownloadEntity entity) {
        DownloadItem item = new DownloadItem(entity.getUrl(),
                entity.getFileName(), entity.getMimeType());
        item.setId(entity.getId());
        item.setDestinationPath(entity.getDestinationPath());
        item.setTotalBytes(entity.getTotalBytes());
        item.setDownloadedBytes(entity.getDownloadedBytes());
        // Defensive: a corrupted DB or future schema change can leave an
        // unknown status string. Map anything we don't recognise to FAILED
        // rather than crashing the downloads screen.
        DownloadItem.Status status;
        try {
            status = DownloadItem.Status.valueOf(entity.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            status = DownloadItem.Status.FAILED;
        }
        item.setStatus(status);
        item.setErrorMessage(entity.getErrorMessage());
        item.setStartTime(entity.getStartTime());
        item.setLastModified(entity.getLastModified());
        item.setSpeedBytesPerSecond(entity.getSpeedBytesPerSecond());
        item.setRemainingSeconds(entity.getRemainingSeconds());
        return item;
    }

    public void updateDownload(DownloadItem download, DownloadCallback callback) {
        executor.execute(() -> {
            DownloadEntity entity = new DownloadEntity(download.getId(),
                    download.getUrl(), download.getFileName());
            entity.setMimeType(download.getMimeType());
            entity.setDestinationPath(download.getDestinationPath());
            entity.setStatus(download.getStatus().name());
            entity.setDownloadedBytes(download.getDownloadedBytes());
            entity.setTotalBytes(download.getTotalBytes());
            entity.setErrorMessage(download.getErrorMessage());
            entity.setStartTime(download.getStartTime());
            entity.setLastModified(download.getLastModified());
            entity.setSpeedBytesPerSecond(download.getSpeedBytesPerSecond());
            entity.setRemainingSeconds(download.getRemainingSeconds());
            database.downloadDao().update(entity);
            if (callback != null) {
                callback.onDownloadUpdated(download);
            }
        });
    }

    public void saveDownload(DownloadItem download, DownloadCallback callback) {
        executor.execute(() -> {
            DownloadEntity entity = new DownloadEntity(download.getId(),
                    download.getUrl(), download.getFileName());
            entity.setMimeType(download.getMimeType());
            entity.setDestinationPath(download.getDestinationPath());
            entity.setStatus(download.getStatus().name());
            entity.setDownloadedBytes(download.getDownloadedBytes());
            entity.setTotalBytes(download.getTotalBytes());
            entity.setErrorMessage(download.getErrorMessage());
            entity.setStartTime(download.getStartTime());
            entity.setLastModified(download.getLastModified());
            entity.setSpeedBytesPerSecond(download.getSpeedBytesPerSecond());
            entity.setRemainingSeconds(download.getRemainingSeconds());
            database.downloadDao().insert(entity);
            if (callback != null) {
                callback.onDownloadUpdated(download);
            }
        });
    }

    public void removeDownload(DownloadItem download, DownloadCallback callback) {
        executor.execute(() -> {
            DownloadEntity entity = database.downloadDao().getDownloadById(download.getId());
            if (entity != null) {
                database.downloadDao().delete(entity);
            }
            if (callback != null) {
                callback.onDownloadRemoved(download);
            }
        });
    }

    public void clearCompletedDownloads() {
        executor.execute(() -> {
            database.downloadDao().deleteCompletedAndFailed();
        });
    }

    public void clearAllDownloads() {
        executor.execute(() -> {
            database.downloadDao().deleteAll();
        });
    }
}
