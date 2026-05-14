package com.webstudio.easybrowser.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.webstudio.easybrowser.database.entity.HistoryEntity;

import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitTime DESC")
    List<HistoryEntity> getAllHistory();

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    HistoryEntity getHistoryByUrl(String url);

    @Query("SELECT * FROM history WHERE visitTime BETWEEN :startTime AND :endTime ORDER BY visitTime DESC")
    List<HistoryEntity> getHistoryBetweenTimes(long startTime, long endTime);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity history);

    @Update
    void update(HistoryEntity history);

    @Delete
    void delete(HistoryEntity history);

    @Query("DELETE FROM history")
    void deleteAll();

    @Query("DELETE FROM history WHERE visitTime < :timestamp")
    void deleteHistoryOlderThan(long timestamp);

    @Query("DELETE FROM history WHERE visitTime BETWEEN :startTime AND :endTime")
    void deleteHistoryBetweenTimes(long startTime, long endTime);
}
