package com.webstudio.easybrowser.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.webstudio.easybrowser.database.entity.QuickAccessEntity;

import java.util.List;

@Dao
public interface QuickAccessDao {
    @Query("SELECT * FROM quick_access ORDER BY visitCount DESC, lastVisited DESC")
    List<QuickAccessEntity> getAllQuickAccess();

    @Query("SELECT * FROM quick_access ORDER BY visitCount DESC LIMIT :limit")
    List<QuickAccessEntity> getMostVisited(int limit);

    @Query("SELECT * FROM quick_access WHERE url = :url LIMIT 1")
    QuickAccessEntity getQuickAccessByUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(QuickAccessEntity quickAccess);

    @Update
    void update(QuickAccessEntity quickAccess);

    @Delete
    void delete(QuickAccessEntity quickAccess);

    @Query("DELETE FROM quick_access")
    void deleteAll();
}
