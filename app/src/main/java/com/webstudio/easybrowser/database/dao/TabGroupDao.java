package com.webstudio.easybrowser.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;
import com.webstudio.easybrowser.database.relation.TabGroupWithTabs;

import java.util.List;

@Dao
public interface TabGroupDao {
    @Transaction
    @Query("SELECT * FROM tab_groups ORDER BY updatedAt DESC, createdAt DESC")
    List<TabGroupWithTabs> getGroupsWithTabs();

    @Transaction
    @Query("SELECT * FROM tab_groups WHERE isPrivate = :isPrivate ORDER BY updatedAt DESC, createdAt DESC")
    List<TabGroupWithTabs> getGroupsWithTabs(boolean isPrivate);

    @Query("SELECT * FROM tab_groups ORDER BY updatedAt DESC, createdAt DESC")
    List<TabGroupEntity> getAllGroups();

    @Query("SELECT * FROM tab_groups WHERE groupId = :groupId LIMIT 1")
    TabGroupEntity getGroupById(String groupId);

    @Query("SELECT * FROM tabs ORDER BY pinned DESC, position ASC, lastAccessed DESC")
    List<TabEntity> getAllTabs();

    @Query("SELECT * FROM tabs WHERE isPrivate = :isPrivate ORDER BY pinned DESC, position ASC, lastAccessed DESC")
    List<TabEntity> getAllTabs(boolean isPrivate);

    @Query("SELECT * FROM tabs WHERE groupId = :groupId ORDER BY pinned DESC, position ASC, lastAccessed DESC")
    List<TabEntity> getTabsForGroup(String groupId);

    @Query("SELECT * FROM tabs WHERE isPrivate = :isPrivate AND groupId IS NULL ORDER BY pinned DESC, position ASC, lastAccessed DESC")
    List<TabEntity> getStandaloneTabs(boolean isPrivate);

    @Query("SELECT groupId FROM tabs WHERE tabId = :tabId LIMIT 1")
    String getGroupIdForTab(String tabId);

    @Query("SELECT COUNT(*) FROM tabs")
    int getTabCount();

    @Query("SELECT COUNT(*) FROM tabs WHERE isPrivate = :isPrivate")
    int getTabCount(boolean isPrivate);

    @Query("SELECT COUNT(*) FROM tabs WHERE groupId = :groupId")
    int getTabCountForGroup(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(TabGroupEntity group);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTab(TabEntity tab);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTabs(List<TabEntity> tabs);

    @Update
    void updateGroup(TabGroupEntity group);

    @Update
    void updateTab(TabEntity tab);

    @Delete
    void deleteGroup(TabGroupEntity group);

    @Delete
    void deleteTab(TabEntity tab);

    @Query("DELETE FROM tabs WHERE tabId = :tabId")
    void deleteTabById(String tabId);

    @Query("DELETE FROM tab_groups WHERE groupId = :groupId")
    void deleteGroupById(String groupId);

    @Query("DELETE FROM tabs WHERE isPrivate = 1")
    void deletePrivateTabs();

    @Query("DELETE FROM tabs WHERE isPrivate = :isPrivate")
    void deleteTabs(boolean isPrivate);

    @Query("DELETE FROM tab_groups WHERE isPrivate = 1")
    void deletePrivateGroups();

    @Query("DELETE FROM tab_groups WHERE isPrivate = :isPrivate")
    void deleteGroups(boolean isPrivate);

    @Query("UPDATE tabs SET groupId = :groupId WHERE tabId = :tabId")
    void moveTabToGroup(String tabId, String groupId);

    @Query("UPDATE tabs SET groupId = :groupId, position = :position WHERE tabId = :tabId")
    void moveTabToGroup(String tabId, String groupId, int position);

    @Query("UPDATE tabs SET groupId = NULL WHERE tabId = :tabId")
    void removeTabFromGroup(String tabId);

    @Query("UPDATE tabs SET groupId = NULL WHERE groupId = :groupId")
    void clearGroup(String groupId);

    @Query("UPDATE tabs SET position = :position WHERE tabId = :tabId")
    void updateTabPosition(String tabId, int position);

    @Query("UPDATE tabs SET thumbnailPath = :thumbnailPath WHERE tabId = :tabId")
    void updateThumbnailPath(String tabId, String thumbnailPath);

    @Query("UPDATE tabs SET faviconPath = :faviconPath WHERE tabId = :tabId")
    void updateFaviconPath(String tabId, String faviconPath);

    @Query("UPDATE tab_groups SET groupName = :groupName, updatedAt = :updatedAt WHERE groupId = :groupId")
    void updateGroupName(String groupId, String groupName, long updatedAt);

    @Query("UPDATE tab_groups SET groupColor = :groupColor, updatedAt = :updatedAt WHERE groupId = :groupId")
    void updateGroupColor(String groupId, int groupColor, long updatedAt);

    @Query("UPDATE tab_groups SET updatedAt = :updatedAt WHERE groupId = :groupId")
    void updateGroupTimestamp(String groupId, long updatedAt);

    @Query("DELETE FROM tabs")
    void deleteAllTabs();

    @Query("DELETE FROM tab_groups")
    void deleteAllGroups();
}
