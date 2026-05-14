package com.webstudio.easybrowser.repository;

import android.content.Context;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.HistoryEntity;
import com.webstudio.easybrowser.models.HistoryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class HistoryRepository {
    private AppDatabase database;
    private Executor executor;

    public HistoryRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public interface HistoryCallback {
        void onHistoryLoaded(List<HistoryItem> historyItems);
        void onHistoryItemAdded(HistoryItem item);
        void onHistoryCleared();
    }

    public void getAllHistory(HistoryCallback callback) {
        executor.execute(() -> {
            List<HistoryEntity> entities = database.historyDao().getAllHistory();
            List<HistoryItem> items = new ArrayList<>();
            for (HistoryEntity entity : entities) {
                items.add(toModel(entity));
            }
            callback.onHistoryLoaded(items);
        });
    }

    public void addHistoryItem(HistoryItem item, HistoryCallback callback) {
        executor.execute(() -> {
            HistoryEntity existing = database.historyDao().getHistoryByUrl(item.getUrl());
            if (existing != null) {
                existing.setVisitCount(existing.getVisitCount() + 1);
                existing.setVisitTime(System.currentTimeMillis());
                database.historyDao().update(existing);
            } else {
                HistoryEntity entity = new HistoryEntity(item.getId(),
                        item.getTitle(), item.getUrl());
                database.historyDao().insert(entity);
            }
            callback.onHistoryItemAdded(item);
        });
    }

    public void clearHistory(HistoryCallback callback) {
        executor.execute(() -> {
            database.historyDao().deleteAll();
            callback.onHistoryCleared();
        });
    }

    public void deleteHistoryItem(HistoryItem item) {
        executor.execute(() -> {
            HistoryEntity entity = database.historyDao().getHistoryByUrl(item.getUrl());
            if (entity != null) {
                database.historyDao().delete(entity);
            }
        });
    }

    public void getHistoryBetweenTimes(long startTime, long endTime, HistoryCallback callback) {
        executor.execute(() -> {
            List<HistoryEntity> entities = database.historyDao()
                    .getHistoryBetweenTimes(startTime, endTime);
            List<HistoryItem> items = new ArrayList<>();
            for (HistoryEntity entity : entities) {
                items.add(toModel(entity));
            }
            callback.onHistoryLoaded(items);
        });
    }

    public void clearHistoryBetweenTimes(long startTime, long endTime, HistoryCallback callback) {
        executor.execute(() -> {
            database.historyDao().deleteHistoryBetweenTimes(startTime, endTime);
            if (callback != null) {
                callback.onHistoryCleared();
            }
        });
    }

    private HistoryItem toModel(HistoryEntity entity) {
        HistoryItem item = new HistoryItem(entity.getTitle(), entity.getUrl());
        item.setId(entity.getId());
        item.setFavicon(entity.getFavicon());
        item.setVisitTime(entity.getVisitTime());
        item.setVisitCount(entity.getVisitCount());
        return item;
    }
}
