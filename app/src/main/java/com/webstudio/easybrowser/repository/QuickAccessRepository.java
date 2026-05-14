package com.webstudio.easybrowser.repository;

import android.content.Context;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.QuickAccessEntity;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class QuickAccessRepository {
    private AppDatabase database;
    private Executor executor;
    // Application context only — guaranteed-safe lifetime, no Activity leak risk.
    private Context context;

    public QuickAccessRepository(Context context) {
        this.context = context.getApplicationContext();
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public interface QuickAccessCallback {
        void onQuickAccessItemsLoaded(List<QuickAccessItem> items);
        void onQuickAccessItemAdded(QuickAccessItem item);
        void onQuickAccessItemRemoved(QuickAccessItem item);
    }

    public void getMostVisitedItems(int limit, QuickAccessCallback callback) {
        executor.execute(() -> {
            List<QuickAccessEntity> entities = database.quickAccessDao().getAllQuickAccess();
            Map<String, QuickAccessItem> groupedItems = new LinkedHashMap<>();
            for (QuickAccessEntity entity : entities) {
                if (UrlUtils.isSearchResultsUrl(context, entity.getUrl())) {
                    continue;
                }
                String originUrl = UrlUtils.getSiteOrigin(entity.getUrl());
                String displayHost = UrlUtils.getDisplayHost(entity.getUrl());
                String groupedUrl = originUrl != null ? originUrl : entity.getUrl();
                QuickAccessItem existing = groupedItems.get(groupedUrl);
                if (existing == null) {
                    QuickAccessItem item = new QuickAccessItem(
                            displayHost != null ? displayHost : entity.getTitle(),
                            groupedUrl);
                    item.setId(entity.getId());
                    item.setFaviconUrl(entity.getFaviconUrl());
                    item.setVisitCount(entity.getVisitCount());
                    item.setLastVisited(entity.getLastVisited());
                    groupedItems.put(groupedUrl, item);
                } else {
                    existing.setVisitCount(existing.getVisitCount() + entity.getVisitCount());
                    if (entity.getLastVisited() > existing.getLastVisited()) {
                        existing.setLastVisited(entity.getLastVisited());
                    }
                }
            }
            List<QuickAccessItem> items = new ArrayList<>(groupedItems.values());
            Collections.sort(items, (first, second) -> {
                int visitCompare = Integer.compare(second.getVisitCount(), first.getVisitCount());
                if (visitCompare != 0) {
                    return visitCompare;
                }
                return Long.compare(second.getLastVisited(), first.getLastVisited());
            });
            if (items.size() > limit) {
                items = new ArrayList<>(items.subList(0, limit));
            }
            callback.onQuickAccessItemsLoaded(items);
        });
    }

    public void updateQuickAccessItem(QuickAccessItem item) {
        executor.execute(() -> {
            String originUrl = UrlUtils.getSiteOrigin(item.getUrl());
            String displayHost = UrlUtils.getDisplayHost(item.getUrl());
            String quickAccessUrl = originUrl != null ? originUrl : item.getUrl();
            String quickAccessTitle = displayHost != null ? displayHost : item.getTitle();
            QuickAccessEntity existing = database.quickAccessDao().getQuickAccessByUrl(quickAccessUrl);
            if (existing != null) {
                existing.setTitle(quickAccessTitle);
                existing.setUrl(quickAccessUrl);
                existing.setFaviconUrl(item.getFaviconUrl());
                existing.setVisitCount(existing.getVisitCount() + 1);
                existing.setLastVisited(System.currentTimeMillis());
                database.quickAccessDao().update(existing);
            } else {
                QuickAccessEntity entity = new QuickAccessEntity(item.getId(),
                        quickAccessTitle, quickAccessUrl);
                entity.setFaviconUrl(item.getFaviconUrl());
                entity.setVisitCount(1);
                database.quickAccessDao().insert(entity);
            }
        });
    }

    public void saveQuickAccessItem(QuickAccessItem oldItem, QuickAccessItem newItem,
                                    QuickAccessCallback callback) {
        executor.execute(() -> {
            QuickAccessEntity existing = database.quickAccessDao().getQuickAccessByUrl(oldItem.getUrl());
            if (existing != null) {
                existing.setTitle(newItem.getTitle());
                existing.setUrl(newItem.getUrl());
                existing.setFaviconUrl(newItem.getFaviconUrl());
                existing.setLastVisited(System.currentTimeMillis());
                database.quickAccessDao().update(existing);
            } else {
                QuickAccessEntity entity = new QuickAccessEntity(newItem.getId(),
                        newItem.getTitle(), newItem.getUrl());
                entity.setFaviconUrl(newItem.getFaviconUrl());
                entity.setVisitCount(newItem.getVisitCount());
                database.quickAccessDao().insert(entity);
            }
            if (callback != null) {
                callback.onQuickAccessItemAdded(newItem);
            }
        });
    }

    public void removeQuickAccessItem(QuickAccessItem item, QuickAccessCallback callback) {
        executor.execute(() -> {
            QuickAccessEntity entity = database.quickAccessDao().getQuickAccessByUrl(item.getUrl());
            if (entity != null) {
                database.quickAccessDao().delete(entity);
            }
            if (callback != null) {
                callback.onQuickAccessItemRemoved(item);
            }
        });
    }

    public void removeQuickAccessItem(QuickAccessItem item) {
        removeQuickAccessItem(item, null);
    }
}
