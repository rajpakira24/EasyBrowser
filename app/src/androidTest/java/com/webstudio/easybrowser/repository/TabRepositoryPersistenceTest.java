package com.webstudio.easybrowser.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;
import com.webstudio.easybrowser.models.Tab;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class TabRepositoryPersistenceTest {
    private AppDatabase database;
    private TabRepository repository;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new TabRepository(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                database,
                Runnable::run);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void saveTabsBlockingReplacesRegularSnapshotAndSkipsPrivateTabs() {
        repository.saveTabsBlocking(Arrays.asList(tab("old", false), tab("private", true)));
        assertEquals(1, database.tabGroupDao().getTabCount(false));
        assertEquals(0, database.tabGroupDao().getTabCount(true));

        Tab replacement = tab("replacement", false);
        replacement.setPosition(0);
        replacement.setPinned(true);

        repository.saveTabsBlocking(Collections.singletonList(replacement));

        assertEquals(1, database.tabGroupDao().getTabCount(false));
        assertEquals(0, database.tabGroupDao().getTabCount(true));
        assertNull(database.tabGroupDao().getGroupIdForTab("old"));
        assertEquals("replacement", repository.getAllTabsBlocking(false).get(0).getId());
        assertTrue(repository.getAllTabsBlocking(false).get(0).isPinned());
    }

    @Test
    public void saveTabsBlockingDeletesStaleGroupsAndClearsOneTabGroups() {
        Tab staleGroupedTab = tab("single", false);
        staleGroupedTab.setGroupId("group");
        staleGroupedTab.setGroupName("Work");
        staleGroupedTab.setGroupColor(0xFF34A853);

        repository.saveTabsBlocking(Collections.singletonList(staleGroupedTab));

        assertTrue(repository.getGroupsBlocking(false).isEmpty());
        assertEquals(1, repository.getAllTabsBlocking(false).size());
        assertNull(repository.getAllTabsBlocking(false).get(0).getGroupId());
    }

    @Test
    public void restoreQueriesCleanPartialPersistedGroupState() {
        database.tabGroupDao().insertGroup(
                new TabGroupEntity("partial-group", "Partial", 0xFF34A853,
                        false, 1000L, 1000L));
        database.tabGroupDao().insertTab(entity("single", "partial-group", false));
        database.tabGroupDao().insertTab(entity("standalone", null, false));

        assertTrue(repository.getGroupsBlocking(false).isEmpty());
        assertNull(database.tabGroupDao().getGroupById("partial-group"));
        assertNull(database.tabGroupDao().getGroupIdForTab("single"));
        assertEquals(2, repository.getAllTabsBlocking(false).size());
    }

    @Test
    public void saveTabsBlockingPersistsGroupedRegularTabs() {
        Tab first = groupedTab("a");
        first.setPosition(0);
        Tab second = groupedTab("b");
        second.setPosition(1);

        repository.saveTabsBlocking(Arrays.asList(first, second));

        assertEquals(2, repository.getAllTabsBlocking(false).size());
        assertEquals(1, repository.getGroupsBlocking(false).size());
        assertEquals(2, repository.getGroupsBlocking(false).get(0).getTabCount());
        assertEquals("group", repository.getGroupsBlocking(false).get(0).getGroupId());
    }

    @Test
    public void clearPersistedPrivateStateDeletesPrivateTabsAndGroupsOnly() {
        database.tabGroupDao().insertGroup(
                new TabGroupEntity("regular-group", "Regular", 0xFF34A853,
                        false, 1000L, 1000L));
        database.tabGroupDao().insertGroup(
                new TabGroupEntity("private-group", "Private", 0xFF1A73E8,
                        true, 1000L, 1000L));
        database.tabGroupDao().insertTab(entity("regular", "regular-group", false));
        database.tabGroupDao().insertTab(entity("regular-2", "regular-group", false));
        database.tabGroupDao().insertTab(entity("private", "private-group", true));

        repository.clearPersistedPrivateStateBlocking();

        assertEquals(2, database.tabGroupDao().getTabCount(false));
        assertEquals(0, database.tabGroupDao().getTabCount(true));
        assertFalse(repository.getGroupsBlocking(false).isEmpty());
        assertTrue(repository.getGroupsBlocking(true).isEmpty());
    }

    private static Tab groupedTab(String id) {
        Tab tab = tab(id, false);
        tab.setGroupId("group");
        tab.setGroupName("Work");
        tab.setGroupColor(0xFF34A853);
        return tab;
    }

    private static Tab tab(String id, boolean isPrivate) {
        Tab tab = new Tab(id, null, id, "https://example.com/" + id, isPrivate);
        tab.setCreatedAt(1000L);
        tab.setLastAccessed(1000L);
        return tab;
    }

    private static TabEntity entity(String id, String groupId, boolean isPrivate) {
        TabEntity entity = new TabEntity(id, groupId, id, "https://example.com/" + id);
        entity.setPrivate(isPrivate);
        entity.setLastAccessed(1000L);
        entity.setPosition(0);
        return entity;
    }
}
