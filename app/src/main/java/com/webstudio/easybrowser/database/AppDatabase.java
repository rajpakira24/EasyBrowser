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
import com.webstudio.easybrowser.database.entity.BookmarkEntity;
import com.webstudio.easybrowser.database.entity.DownloadEntity;
import com.webstudio.easybrowser.database.entity.HistoryEntity;
import com.webstudio.easybrowser.database.entity.QuickAccessEntity;
import com.webstudio.easybrowser.database.entity.ReadingListEntity;

@Database(entities = {
        BookmarkEntity.class,
        HistoryEntity.class,
        QuickAccessEntity.class,
        DownloadEntity.class,
        ReadingListEntity.class
}, version = 3)
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

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "browser.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
