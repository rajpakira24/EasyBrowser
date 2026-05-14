package com.webstudio.easybrowser.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.webstudio.easybrowser.database.entity.DownloadEntity;

import java.util.List;

@Dao
public interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY startTime DESC")
    List<DownloadEntity> getAllDownloads();

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY startTime DESC")
    List<DownloadEntity> getDownloadsByStatus(String status);

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    DownloadEntity getDownloadById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadEntity download);

    @Update
    void update(DownloadEntity download);

    @Delete
    void delete(DownloadEntity download);

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED' OR status = 'FAILED'")
    void deleteCompletedAndFailed();

    @Query("DELETE FROM downloads")
    void deleteAll();
}
