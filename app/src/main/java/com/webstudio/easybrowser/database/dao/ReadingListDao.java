package com.webstudio.easybrowser.database.dao;

import androidx.room.*;
import com.webstudio.easybrowser.database.entity.ReadingListEntity;
import java.util.List;

@Dao
public interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY savedAt DESC")
    List<ReadingListEntity> getAll();

    @Query("SELECT * FROM reading_list WHERE id = :id LIMIT 1")
    ReadingListEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReadingListEntity entity);

    @Delete
    void delete(ReadingListEntity entity);

    @Query("DELETE FROM reading_list")
    void deleteAll();
}
