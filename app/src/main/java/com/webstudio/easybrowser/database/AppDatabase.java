package com.webstudio.easybrowser.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.webstudio.easybrowser.database.dao.BookmarkDao;
import com.webstudio.easybrowser.database.dao.DownloadDao;
import com.webstudio.easybrowser.database.dao.HistoryDao;
import com.webstudio.easybrowser.database.dao.QuickAccessDao;
import com.webstudio.easybrowser.database.dao.ReadingListDao;
import com.webstudio.easybrowser.database.dao.TabGroupDao;
import com.webstudio.easybrowser.database.entity.BookmarkEntity;
import com.webstudio.easybrowser.database.entity.DownloadEntity;
import com.webstudio.easybrowser.database.entity.HistoryEntity;
import com.webstudio.easybrowser.database.entity.QuickAccessEntity;
import com.webstudio.easybrowser.database.entity.ReadingListEntity;
import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;

@Database(entities = {
        BookmarkEntity.class,
        HistoryEntity.class,
        QuickAccessEntity.class,
        DownloadEntity.class,
        ReadingListEntity.class,
        TabGroupEntity.class,
        TabEntity.class
}, version = 7)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    // Single shared executor for all repository background work. Each repository used
    // to spin up its own newSingleThreadExecutor() that was never shutdown, leaking a
    // thread per Activity that constructed a repo.
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newFixedThreadPool(2);

    public static ExecutorService getDatabaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `reading_list` ("
                    + "`id` TEXT NOT NULL, "
                    + "`title` TEXT, "
                    + "`url` TEXT, "
                    + "`favicon` TEXT, "
                    + "`savedAt` INTEGER NOT NULL, "
                    + "`contentPath` TEXT, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `tab_groups` ("
                    + "`groupId` TEXT NOT NULL, "
                    + "`groupName` TEXT, "
                    + "`groupColor` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`groupId`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `tabs` ("
                    + "`tabId` TEXT NOT NULL, "
                    + "`groupId` TEXT NOT NULL, "
                    + "`title` TEXT, "
                    + "`url` TEXT, "
                    + "`favicon` TEXT, "
                    + "`thumbnailPath` TEXT, "
                    + "`sessionState` TEXT, "
                    + "`lastAccessed` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`tabId`), "
                    + "FOREIGN KEY(`groupId`) REFERENCES `tab_groups`(`groupId`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_groupId` ON `tabs` (`groupId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_lastAccessed` ON `tabs` (`lastAccessed`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_groupId_position` ON `tabs` (`groupId`, `position`)");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `tab_groups` ADD COLUMN `isPrivate` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `tab_groups` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE `tab_groups` SET `updatedAt` = `createdAt` WHERE `updatedAt` = 0");
            database.execSQL("ALTER TABLE `tabs` ADD COLUMN `faviconPath` TEXT");
            database.execSQL("UPDATE `tabs` SET `faviconPath` = `favicon` WHERE `favicon` IS NOT NULL");
            database.execSQL("ALTER TABLE `tabs` ADD COLUMN `isPrivate` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `tabs_new` ("
                    + "`tabId` TEXT NOT NULL, "
                    + "`groupId` TEXT, "
                    + "`title` TEXT, "
                    + "`url` TEXT, "
                    + "`favicon` TEXT, "
                    + "`faviconPath` TEXT, "
                    + "`thumbnailPath` TEXT, "
                    + "`sessionState` TEXT, "
                    + "`isPrivate` INTEGER NOT NULL, "
                    + "`lastAccessed` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`tabId`), "
                    + "FOREIGN KEY(`groupId`) REFERENCES `tab_groups`(`groupId`) "
                    + "ON UPDATE NO ACTION ON DELETE SET NULL)");
            database.execSQL("INSERT INTO `tabs_new` (`tabId`, `groupId`, `title`, `url`, "
                    + "`favicon`, `faviconPath`, `thumbnailPath`, `sessionState`, `isPrivate`, "
                    + "`lastAccessed`, `position`) "
                    + "SELECT `tabId`, "
                    + "CASE WHEN `groupId` IN ('default', 'private_default') THEN NULL ELSE `groupId` END, "
                    + "`title`, `url`, `favicon`, `faviconPath`, `thumbnailPath`, `sessionState`, "
                    + "`isPrivate`, `lastAccessed`, `position` FROM `tabs`");
            database.execSQL("DROP TABLE `tabs`");
            database.execSQL("ALTER TABLE `tabs_new` RENAME TO `tabs`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_groupId` ON `tabs` (`groupId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_lastAccessed` ON `tabs` (`lastAccessed`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_groupId_position` ON `tabs` (`groupId`, `position`)");
            database.execSQL("DELETE FROM `tab_groups` WHERE `groupId` IN ('default', 'private_default')");
            database.execSQL("UPDATE `tabs` SET `groupId` = NULL WHERE `groupId` IN ("
                    + "SELECT `groupId` FROM `tabs` WHERE `groupId` IS NOT NULL "
                    + "GROUP BY `groupId` HAVING COUNT(*) < 2)");
            database.execSQL("DELETE FROM `tab_groups` WHERE `groupId` NOT IN ("
                    + "SELECT DISTINCT `groupId` FROM `tabs` WHERE `groupId` IS NOT NULL)");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `tabs` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `downloads` ("
                    + "`id` TEXT NOT NULL, "
                    + "`url` TEXT, "
                    + "`fileName` TEXT, "
                    + "`mimeType` TEXT, "
                    + "`destinationPath` TEXT, "
                    + "`totalBytes` INTEGER NOT NULL, "
                    + "`downloadedBytes` INTEGER NOT NULL, "
                    + "`status` TEXT, "
                    + "`errorMessage` TEXT, "
                    + "`startTime` INTEGER NOT NULL, "
                    + "`lastModified` INTEGER NOT NULL, "
                    + "`speedBytesPerSecond` INTEGER NOT NULL, "
                    + "`remainingSeconds` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    public abstract BookmarkDao bookmarkDao();
    public abstract HistoryDao historyDao();
    public abstract QuickAccessDao quickAccessDao();
    public abstract DownloadDao downloadDao();
    public abstract ReadingListDao readingListDao();
    public abstract TabGroupDao tabGroupDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "browser.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            // Prevents a crash if the user downgrades the app.
                            // To add a new migration: define MIGRATION_X_Y, pass it here,
                            // and bump the @Database version above.
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
