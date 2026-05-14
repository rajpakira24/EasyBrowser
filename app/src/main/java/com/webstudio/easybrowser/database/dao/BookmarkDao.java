package com.webstudio.easybrowser.database.dao;

import androidx.room.*;
import com.webstudio.easybrowser.database.entity.*;
import java.util.List;

@Dao
public interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    List<BookmarkEntity> getAllBookmarks();

    @Query("SELECT * FROM bookmarks WHERE folder = :folderName ORDER BY createdAt DESC")
    List<BookmarkEntity> getBookmarksByFolder(String folderName);

    @Query("SELECT DISTINCT folder FROM bookmarks WHERE folder IS NOT NULL AND folder != '' ORDER BY folder")
    List<String> getFolderNames();

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    BookmarkEntity getBookmarkByUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookmarkEntity bookmark);

    @Update
    void update(BookmarkEntity bookmark);

    @Delete
    void delete(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks")
    void deleteAll();
}

