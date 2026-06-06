package com.webstudio.easybrowser.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {
    private static final String MIGRATION_3_7_DB = "migration-3-7";
    private static final String MIGRATION_6_7_DB = "migration-6-7";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class);

    @Test
    public void migrate3To7CreatesTabGroupSchema() throws Exception {
        String databaseName = deviceProtectedDatabaseName(MIGRATION_3_7_DB);
        SupportSQLiteDatabase database = helper.createDatabase(databaseName, 3);
        database.close();

        database = helper.runMigrationsAndValidate(
                databaseName,
                7,
                true,
                AppDatabase.ALL_MIGRATIONS);

        assertTrue(hasColumn(database, "tab_groups", "isPrivate"));
        assertTrue(hasColumn(database, "tab_groups", "updatedAt"));
        assertTrue(hasColumn(database, "tabs", "groupId"));
        assertTrue(hasColumn(database, "tabs", "faviconPath"));
        assertTrue(hasColumn(database, "tabs", "isPrivate"));
        assertTrue(hasColumn(database, "tabs", "pinned"));
        assertFalse(isColumnNotNull(database, "tabs", "groupId"));
        database.close();
    }

    @Test
    public void migrate6To7AddsPinnedColumnWithDefaultFalse() throws Exception {
        String databaseName = deviceProtectedDatabaseName(MIGRATION_6_7_DB);
        SupportSQLiteDatabase database = helper.createDatabase(databaseName, 6);
        database.execSQL("INSERT INTO tab_groups "
                + "(groupId, groupName, groupColor, isPrivate, createdAt, updatedAt) "
                + "VALUES ('group', 'Work', 4281558732, 0, 1000, 1000)");
        database.execSQL("INSERT INTO tabs "
                + "(tabId, groupId, title, url, favicon, faviconPath, thumbnailPath, "
                + "sessionState, isPrivate, lastAccessed, position) "
                + "VALUES ('tab', 'group', 'Example', 'https://example.com', NULL, NULL, NULL, "
                + "NULL, 0, 2000, 0)");
        database.close();

        database = helper.runMigrationsAndValidate(
                databaseName,
                7,
                true,
                AppDatabase.MIGRATION_6_7);

        try (Cursor cursor = database.query("SELECT pinned FROM tabs WHERE tabId = 'tab'")) {
            assertTrue(cursor.moveToFirst());
            assertEquals(0, cursor.getInt(0));
        }
        database.close();
    }

    @Test
    public void deletingGroupSetsTabGroupIdToNull() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        try {
            database.tabGroupDao().insertGroup(
                    new TabGroupEntity("group", "Work", 0xFF34A853, false, 1000L, 1000L));
            database.tabGroupDao().insertTab(tabEntity("a", "group"));
            database.tabGroupDao().insertTab(tabEntity("b", "group"));

            database.tabGroupDao().deleteGroupById("group");

            assertEquals(null, database.tabGroupDao().getGroupIdForTab("a"));
            assertEquals(null, database.tabGroupDao().getGroupIdForTab("b"));
        } finally {
            database.close();
        }
    }

    private static TabEntity tabEntity(String id, String groupId) {
        TabEntity entity = new TabEntity(id, groupId, id, "https://example.com/" + id);
        entity.setPrivate(false);
        entity.setLastAccessed(1000L);
        entity.setPosition(0);
        entity.setPinned(false);
        return entity;
    }

    private static boolean hasColumn(SupportSQLiteDatabase database, String tableName,
                                     String columnName) {
        try (Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)")) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isColumnNotNull(SupportSQLiteDatabase database, String tableName,
                                           String columnName) {
        try (Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)")) {
            int nameIndex = cursor.getColumnIndex("name");
            int notNullIndex = cursor.getColumnIndex("notnull");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return cursor.getInt(notNullIndex) == 1;
                }
            }
        }
        return false;
    }

    private static String deviceProtectedDatabaseName(String fileName) {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .createDeviceProtectedStorageContext();
        File databaseFile = context.getDatabasePath(fileName);
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        deleteDatabaseFiles(databaseFile);
        return databaseFile.getAbsolutePath();
    }

    private static void deleteDatabaseFiles(File databaseFile) {
        deleteIfExists(databaseFile);
        deleteIfExists(new File(databaseFile.getPath() + "-journal"));
        deleteIfExists(new File(databaseFile.getPath() + "-shm"));
        deleteIfExists(new File(databaseFile.getPath() + "-wal"));
    }

    private static void deleteIfExists(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
