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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QuickAccessRepository {
    private AppDatabase database;
    private ExecutorService executor;
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
            callback.onQuickAccessItemsLoaded(buildMostVisitedItems(limit));
        });
    }

    public List<QuickAccessItem> getMostVisitedItemsSnapshot(int limit, long timeoutMillis) {
        Future<List<QuickAccessItem>> future = executor.submit(() -> buildMostVisitedItems(limit));
        try {
            return future.get(Math.max(1, timeoutMillis), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
        }
        return new ArrayList<>();
    }

    public void updateQuickAccessItem(QuickAccessItem item) {
        executor.execute(() -> {
            if (item == null || shouldSkipQuickAccessUrl(item.getUrl())) {
                return;
            }
            String quickAccessUrl = UrlUtils.getQuickAccessUrl(item.getUrl());
            if (quickAccessUrl == null) {
                return;
            }
            String displayHost = UrlUtils.getQuickAccessTitle(quickAccessUrl);
            String quickAccessTitle = displayHost != null ? displayHost : item.getTitle();
            String faviconUrl = firstNonEmpty(UrlUtils.getFaviconUrl(quickAccessUrl), item.getFaviconUrl());
            QuickAccessEntity existing = findQuickAccessEntity(quickAccessUrl);
            if (existing != null) {
                mergeDuplicateEntities(existing, quickAccessUrl);
                existing.setTitle(quickAccessTitle);
                existing.setUrl(quickAccessUrl);
                existing.setFaviconUrl(faviconUrl);
                existing.setVisitCount(existing.getVisitCount() + 1);
                existing.setLastVisited(System.currentTimeMillis());
                existing.setPinned(existing.isPinned() || item.isPinned());
                database.quickAccessDao().update(existing);
            } else {
                QuickAccessEntity entity = new QuickAccessEntity(item.getId(),
                        quickAccessTitle, quickAccessUrl);
                entity.setFaviconUrl(faviconUrl);
                entity.setVisitCount(1);
                entity.setPinned(item.isPinned());
                database.quickAccessDao().insert(entity);
            }
        });
    }

    public void saveQuickAccessItem(QuickAccessItem oldItem, QuickAccessItem newItem,
                                    QuickAccessCallback callback) {
        executor.execute(() -> {
            if (oldItem == null || newItem == null) {
                return;
            }
            String oldQuickAccessUrl = UrlUtils.getQuickAccessUrl(oldItem.getUrl());
            String newQuickAccessUrl = UrlUtils.getQuickAccessUrl(newItem.getUrl());
            String savedUrl = newQuickAccessUrl != null ? newQuickAccessUrl : newItem.getUrl();
            String displayHost = newQuickAccessUrl != null ? UrlUtils.getQuickAccessTitle(savedUrl) : null;
            String savedTitle = displayHost != null ? displayHost : newItem.getTitle();
            String faviconUrl = firstNonEmpty(UrlUtils.getFaviconUrl(savedUrl), newItem.getFaviconUrl());
            QuickAccessEntity existing = oldQuickAccessUrl != null
                    ? findQuickAccessEntity(oldQuickAccessUrl)
                    : database.quickAccessDao().getQuickAccessByUrl(oldItem.getUrl());
            if (existing != null) {
                existing.setTitle(savedTitle);
                existing.setUrl(savedUrl);
                existing.setFaviconUrl(faviconUrl);
                existing.setLastVisited(System.currentTimeMillis());
                existing.setPinned(existing.isPinned() || oldItem.isPinned() || newItem.isPinned());
                if (newQuickAccessUrl != null) {
                    mergeDuplicateEntities(existing, newQuickAccessUrl);
                }
                database.quickAccessDao().update(existing);
            } else {
                QuickAccessEntity entity = new QuickAccessEntity(newItem.getId(),
                        savedTitle, savedUrl);
                entity.setFaviconUrl(faviconUrl);
                entity.setVisitCount(newItem.getVisitCount());
                entity.setPinned(newItem.isPinned());
                database.quickAccessDao().insert(entity);
            }
            if (callback != null) {
                callback.onQuickAccessItemAdded(newItem);
            }
        });
    }

    public void setPinned(QuickAccessItem item, boolean pinned, QuickAccessCallback callback) {
        executor.execute(() -> {
            if (item == null || shouldSkipQuickAccessUrl(item.getUrl())) {
                return;
            }
            String quickAccessUrl = UrlUtils.getQuickAccessUrl(item.getUrl());
            if (quickAccessUrl == null) {
                return;
            }
            QuickAccessEntity existing = findQuickAccessEntity(quickAccessUrl);
            String displayHost = UrlUtils.getQuickAccessTitle(quickAccessUrl);
            String title = displayHost != null ? displayHost : item.getTitle();
            String faviconUrl = firstNonEmpty(UrlUtils.getFaviconUrl(quickAccessUrl), item.getFaviconUrl());
            if (existing != null) {
                mergeDuplicateEntities(existing, quickAccessUrl);
                existing.setTitle(title);
                existing.setUrl(quickAccessUrl);
                existing.setFaviconUrl(faviconUrl);
                existing.setPinned(pinned);
                existing.setVisitCount(Math.max(1, existing.getVisitCount()));
                existing.setLastVisited(System.currentTimeMillis());
                database.quickAccessDao().update(existing);
            } else {
                QuickAccessEntity entity = new QuickAccessEntity(item.getId(), title, quickAccessUrl);
                entity.setFaviconUrl(faviconUrl);
                entity.setPinned(pinned);
                entity.setVisitCount(Math.max(1, item.getVisitCount()));
                entity.setLastVisited(System.currentTimeMillis());
                database.quickAccessDao().insert(entity);
            }
            QuickAccessItem updated = new QuickAccessItem(title, quickAccessUrl);
            updated.setId(item.getId());
            updated.setFaviconUrl(faviconUrl);
            updated.setVisitCount(Math.max(1, item.getVisitCount()));
            updated.setLastVisited(System.currentTimeMillis());
            updated.setPinned(pinned);
            if (callback != null) {
                callback.onQuickAccessItemAdded(updated);
            }
        });
    }

    public void removeQuickAccessItem(QuickAccessItem item, QuickAccessCallback callback) {
        executor.execute(() -> {
            if (item == null) {
                return;
            }
            String quickAccessUrl = UrlUtils.getQuickAccessUrl(item.getUrl());
            List<QuickAccessEntity> entities = database.quickAccessDao().getAllQuickAccess();
            for (QuickAccessEntity entity : entities) {
                boolean matches = quickAccessUrl != null
                        ? quickAccessUrl.equals(UrlUtils.getQuickAccessUrl(entity.getUrl()))
                        : item.getUrl().equals(entity.getUrl());
                if (matches) {
                    database.quickAccessDao().delete(entity);
                }
            }
            if (callback != null) {
                callback.onQuickAccessItemRemoved(item);
            }
        });
    }

    public void removeQuickAccessItem(QuickAccessItem item) {
        removeQuickAccessItem(item, null);
    }

    private boolean shouldSkipQuickAccessUrl(String url) {
        return isEmpty(url)
                || UrlUtils.isInternalPageUrl(url)
                || UrlUtils.isSearchResultsUrl(context, url);
    }

    private QuickAccessEntity findQuickAccessEntity(String quickAccessUrl) {
        QuickAccessEntity exactMatch = database.quickAccessDao().getQuickAccessByUrl(quickAccessUrl);
        if (exactMatch != null) {
            return exactMatch;
        }
        for (QuickAccessEntity entity : database.quickAccessDao().getAllQuickAccess()) {
            if (quickAccessUrl.equals(UrlUtils.getQuickAccessUrl(entity.getUrl()))) {
                return entity;
            }
        }
        return null;
    }

    private List<QuickAccessItem> buildMostVisitedItems(int limit) {
        List<QuickAccessEntity> entities = database.quickAccessDao().getAllQuickAccess();
        Map<String, QuickAccessItem> groupedItems = new LinkedHashMap<>();
        for (QuickAccessEntity entity : entities) {
            if (shouldSkipQuickAccessUrl(entity.getUrl())) {
                continue;
            }
            String groupedUrl = UrlUtils.getQuickAccessUrl(entity.getUrl());
            if (groupedUrl == null) {
                continue;
            }
            String displayHost = UrlUtils.getQuickAccessTitle(groupedUrl);
            String faviconUrl = firstNonEmpty(UrlUtils.getFaviconUrl(groupedUrl), entity.getFaviconUrl());
            QuickAccessItem existing = groupedItems.get(groupedUrl);
            if (existing == null) {
                QuickAccessItem item = new QuickAccessItem(
                        displayHost != null ? displayHost : entity.getTitle(),
                        groupedUrl);
                item.setId(entity.getId());
                item.setFaviconUrl(faviconUrl);
                item.setVisitCount(entity.getVisitCount());
                item.setLastVisited(entity.getLastVisited());
                item.setPinned(entity.isPinned());
                groupedItems.put(groupedUrl, item);
            } else {
                existing.setVisitCount(existing.getVisitCount() + entity.getVisitCount());
                existing.setPinned(existing.isPinned() || entity.isPinned());
                if (isEmpty(existing.getFaviconUrl()) && !isEmpty(faviconUrl)) {
                    existing.setFaviconUrl(faviconUrl);
                }
                if (entity.getLastVisited() > existing.getLastVisited()) {
                    existing.setLastVisited(entity.getLastVisited());
                }
            }
        }
        List<QuickAccessItem> items = new ArrayList<>(groupedItems.values());
        Collections.sort(items, (first, second) -> {
            if (first.isPinned() != second.isPinned()) {
                return first.isPinned() ? -1 : 1;
            }
            int visitCompare = Integer.compare(second.getVisitCount(), first.getVisitCount());
            if (visitCompare != 0) {
                return visitCompare;
            }
            return Long.compare(second.getLastVisited(), first.getLastVisited());
        });
        if (items.size() > limit) {
            items = new ArrayList<>(items.subList(0, limit));
        }
        return items;
    }

    private void mergeDuplicateEntities(QuickAccessEntity keeper, String quickAccessUrl) {
        for (QuickAccessEntity entity : database.quickAccessDao().getAllQuickAccess()) {
            if (entity.getId().equals(keeper.getId())
                    || !quickAccessUrl.equals(UrlUtils.getQuickAccessUrl(entity.getUrl()))) {
                continue;
            }
            keeper.setVisitCount(keeper.getVisitCount() + entity.getVisitCount());
            if (entity.getLastVisited() > keeper.getLastVisited()) {
                keeper.setLastVisited(entity.getLastVisited());
            }
            keeper.setPinned(keeper.isPinned() || entity.isPinned());
            if (isEmpty(keeper.getFaviconUrl()) && !isEmpty(entity.getFaviconUrl())) {
                keeper.setFaviconUrl(entity.getFaviconUrl());
            }
            database.quickAccessDao().delete(entity);
        }
    }

    private static String firstNonEmpty(String first, String second) {
        return !isEmpty(first) ? first : second;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
