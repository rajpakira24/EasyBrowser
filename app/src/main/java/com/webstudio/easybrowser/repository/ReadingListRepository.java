package com.webstudio.easybrowser.repository;

import android.content.Context;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.ReadingListEntity;
import com.webstudio.easybrowser.models.ReadingListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ReadingListRepository {
    private final AppDatabase database;
    private final Executor executor;

    public ReadingListRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public interface ReadingListCallback {
        void onItemsLoaded(List<ReadingListItem> items);
        void onItemSaved();
        void onItemDeleted();
    }

    public void getAll(ReadingListCallback callback) {
        executor.execute(() -> {
            List<ReadingListEntity> entities = database.readingListDao().getAll();
            List<ReadingListItem> items = new ArrayList<>();
            for (ReadingListEntity e : entities) {
                items.add(toModel(e));
            }
            callback.onItemsLoaded(items);
        });
    }

    public void save(ReadingListItem item, ReadingListCallback callback) {
        executor.execute(() -> {
            ReadingListEntity entity = new ReadingListEntity(item.getId(), item.getTitle(), item.getUrl());
            entity.setSavedAt(item.getSavedAt());
            entity.setContentPath(item.getContentPath());
            entity.setFavicon(item.getFavicon());
            database.readingListDao().insert(entity);
            callback.onItemSaved();
        });
    }

    public void delete(ReadingListItem item, ReadingListCallback callback) {
        executor.execute(() -> {
            ReadingListEntity entity = database.readingListDao().getById(item.getId());
            if (entity != null) database.readingListDao().delete(entity);
            callback.onItemDeleted();
        });
    }

    private ReadingListItem toModel(ReadingListEntity e) {
        ReadingListItem item = new ReadingListItem(e.getId(), e.getTitle(), e.getUrl(), e.getSavedAt(), e.getContentPath());
        item.setFavicon(e.getFavicon());
        return item;
    }
}
