package com.webstudio.easybrowser.repository;

import android.content.Context;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.BookmarkEntity;
import com.webstudio.easybrowser.database.entity.HistoryEntity;
import com.webstudio.easybrowser.database.entity.QuickAccessEntity;
import com.webstudio.easybrowser.database.entity.DownloadEntity;
import com.webstudio.easybrowser.models.Bookmark;
import com.webstudio.easybrowser.models.HistoryItem;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.models.DownloadItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class BookmarkRepository {
    private AppDatabase database;
    private Executor executor;

    public BookmarkRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.getDatabaseExecutor();
    }

    public interface BookmarkCallback {
        void onBookmarksLoaded(List<Bookmark> bookmarks);
        void onBookmarkAdded(Bookmark bookmark);
        void onBookmarkRemoved(Bookmark bookmark);
    }

    public interface FolderNamesCallback {
        void onFolderNamesLoaded(List<String> folderNames);
    }

    public void getFolderNames(FolderNamesCallback callback) {
        executor.execute(() -> {
            List<String> names = database.bookmarkDao().getFolderNames();
            callback.onFolderNamesLoaded(names != null ? names : new ArrayList<>());
        });
    }

    public void getAllBookmarks(BookmarkCallback callback) {
        executor.execute(() -> {
            List<BookmarkEntity> entities = database.bookmarkDao().getAllBookmarks();
            List<Bookmark> bookmarks = new ArrayList<>();
            for (BookmarkEntity entity : entities) {
                Bookmark bookmark = new Bookmark(entity.getTitle(), entity.getUrl());
                bookmark.setId(entity.getId());
                bookmark.setFavicon(entity.getFavicon());
                bookmark.setCreatedAt(entity.getCreatedAt());
                bookmark.setFolder(entity.getFolder());
                bookmarks.add(bookmark);
            }
            callback.onBookmarksLoaded(bookmarks);
        });
    }

    public void addBookmark(Bookmark bookmark, BookmarkCallback callback) {
        executor.execute(() -> {
            BookmarkEntity entity = new BookmarkEntity(bookmark.getId(),
                    bookmark.getTitle(), bookmark.getUrl());
            entity.setFavicon(bookmark.getFavicon());
            entity.setCreatedAt(bookmark.getCreatedAt());
            entity.setFolder(bookmark.getFolder());
            database.bookmarkDao().insert(entity);
            callback.onBookmarkAdded(bookmark);
        });
    }

    public void removeBookmark(Bookmark bookmark, BookmarkCallback callback) {
        executor.execute(() -> {
            BookmarkEntity entity = database.bookmarkDao().getBookmarkByUrl(bookmark.getUrl());
            if (entity != null) {
                database.bookmarkDao().delete(entity);
            }
            callback.onBookmarkRemoved(bookmark);
        });
    }

    public void updateBookmark(Bookmark bookmark, BookmarkCallback callback) {
        executor.execute(() -> {
            BookmarkEntity entity = new BookmarkEntity(bookmark.getId(),
                    bookmark.getTitle(), bookmark.getUrl());
            entity.setFavicon(bookmark.getFavicon());
            entity.setCreatedAt(bookmark.getCreatedAt());
            entity.setFolder(bookmark.getFolder());
            database.bookmarkDao().update(entity);
            if (callback != null) {
                callback.onBookmarkAdded(bookmark);
            }
        });
    }
}
