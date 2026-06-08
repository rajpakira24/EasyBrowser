package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.Calendar;

public final class PrivacyStatsManager {
    public static final String KEY_PAGES_PROTECTED = "privacy_pages_protected";
    public static final String KEY_ITEMS_BLOCKED = "privacy_items_blocked";
    public static final String KEY_TIME_SAVED_SECONDS = "privacy_time_saved_seconds";

    private static final String KEY_SCHEMA_VERSION = "privacy_stats_schema_version";
    private static final String KEY_DAY_BUCKET = "privacy_stats_day_bucket";
    private static final String KEY_WEEK_BUCKET = "privacy_stats_week_bucket";
    private static final String KEY_MONTH_BUCKET = "privacy_stats_month_bucket";
    private static final String KEY_DAY_PAGES_PROTECTED = "privacy_stats_day_pages_protected";
    private static final String KEY_DAY_ITEMS_BLOCKED = "privacy_stats_day_items_blocked";
    private static final String KEY_WEEK_PAGES_PROTECTED = "privacy_stats_week_pages_protected";
    private static final String KEY_WEEK_ITEMS_BLOCKED = "privacy_stats_week_items_blocked";
    private static final String KEY_MONTH_PAGES_PROTECTED = "privacy_stats_month_pages_protected";
    private static final String KEY_MONTH_ITEMS_BLOCKED = "privacy_stats_month_items_blocked";
    private static final int ACTUAL_STATS_SCHEMA_VERSION = 2;
    private static final int SECONDS_SAVED_PER_BLOCKED_ITEM = 1;
    private static final Object LOCK = new Object();

    private PrivacyStatsManager() {
    }

    public static void ensureActualStatsInitialized(Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getInt(KEY_SCHEMA_VERSION, 0) >= ACTUAL_STATS_SCHEMA_VERSION) {
            return;
        }
        synchronized (LOCK) {
            if (prefs.getInt(KEY_SCHEMA_VERSION, 0) >= ACTUAL_STATS_SCHEMA_VERSION) {
                return;
            }
            prefs.edit()
                    .putInt(KEY_PAGES_PROTECTED, 0)
                    .putInt(KEY_ITEMS_BLOCKED, 0)
                    .putInt(KEY_TIME_SAVED_SECONDS, 0)
                    .putInt(KEY_SCHEMA_VERSION, ACTUAL_STATS_SCHEMA_VERSION)
                    .apply();
        }
    }

    public static void recordProtectedPage(Context context) {
        if (!isProtectionEnabled(context)) {
            return;
        }
        increment(context, 1, 0);
    }

    public static void recordBlockedItem(Context context) {
        increment(context, 0, 1);
    }

    public static Stats getStats(Context context) {
        ensureActualStatsInitialized(context);
        SharedPreferences prefs = prefs(context);
        return new Stats(
                prefs.getInt(KEY_PAGES_PROTECTED, 0),
                prefs.getInt(KEY_ITEMS_BLOCKED, 0),
                prefs.getInt(KEY_TIME_SAVED_SECONDS, 0));
    }

    public static Report getReport(Context context) {
        ensureActualStatsInitialized(context);
        SharedPreferences prefs = prefs(context);
        synchronized (LOCK) {
            ensurePeriodBuckets(prefs);
            return new Report(
                    new PeriodStats(
                            prefs.getInt(KEY_DAY_PAGES_PROTECTED, 0),
                            prefs.getInt(KEY_DAY_ITEMS_BLOCKED, 0)),
                    new PeriodStats(
                            prefs.getInt(KEY_WEEK_PAGES_PROTECTED, 0),
                            prefs.getInt(KEY_WEEK_ITEMS_BLOCKED, 0)),
                    new PeriodStats(
                            prefs.getInt(KEY_MONTH_PAGES_PROTECTED, 0),
                            prefs.getInt(KEY_MONTH_ITEMS_BLOCKED, 0)));
        }
    }

    public static boolean isStatsKey(String key) {
        return KEY_PAGES_PROTECTED.equals(key)
                || KEY_ITEMS_BLOCKED.equals(key)
                || KEY_TIME_SAVED_SECONDS.equals(key)
                || (key != null && key.startsWith("privacy_stats_"));
    }

    public static boolean isProtectionEnabled(Context context) {
        SharedPreferences prefs = prefs(context);
        String adBlockLevel = prefs.getString("ad_blocking_level", "balanced");
        return !"off".equals(adBlockLevel)
                || prefs.getBoolean("https_only", true)
                || prefs.getBoolean("block_popups", true)
                || prefs.getBoolean("block_cookie_banners", true)
                || prefs.getBoolean("strip_tracking_params", true);
    }

    private static void increment(Context context, int protectedPages, int blockedItems) {
        if (protectedPages == 0 && blockedItems == 0) {
            return;
        }
        ensureActualStatsInitialized(context);
        SharedPreferences prefs = prefs(context);
        synchronized (LOCK) {
            ensurePeriodBuckets(prefs);
            int currentProtectedPages = prefs.getInt(KEY_PAGES_PROTECTED, 0);
            int currentBlockedItems = prefs.getInt(KEY_ITEMS_BLOCKED, 0);
            int currentTimeSaved = prefs.getInt(KEY_TIME_SAVED_SECONDS, 0);
            SharedPreferences.Editor editor = prefs.edit()
                    .putInt(KEY_PAGES_PROTECTED, currentProtectedPages + protectedPages)
                    .putInt(KEY_ITEMS_BLOCKED, currentBlockedItems + blockedItems)
                    .putInt(KEY_TIME_SAVED_SECONDS,
                            currentTimeSaved + blockedItems * SECONDS_SAVED_PER_BLOCKED_ITEM);
            if (protectedPages != 0) {
                editor.putInt(KEY_DAY_PAGES_PROTECTED,
                        prefs.getInt(KEY_DAY_PAGES_PROTECTED, 0) + protectedPages)
                        .putInt(KEY_WEEK_PAGES_PROTECTED,
                                prefs.getInt(KEY_WEEK_PAGES_PROTECTED, 0) + protectedPages)
                        .putInt(KEY_MONTH_PAGES_PROTECTED,
                                prefs.getInt(KEY_MONTH_PAGES_PROTECTED, 0) + protectedPages);
            }
            if (blockedItems != 0) {
                editor.putInt(KEY_DAY_ITEMS_BLOCKED,
                        prefs.getInt(KEY_DAY_ITEMS_BLOCKED, 0) + blockedItems)
                        .putInt(KEY_WEEK_ITEMS_BLOCKED,
                                prefs.getInt(KEY_WEEK_ITEMS_BLOCKED, 0) + blockedItems)
                        .putInt(KEY_MONTH_ITEMS_BLOCKED,
                                prefs.getInt(KEY_MONTH_ITEMS_BLOCKED, 0) + blockedItems);
            }
            editor.apply();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    private static void ensurePeriodBuckets(SharedPreferences prefs) {
        Calendar now = Calendar.getInstance();
        int dayBucket = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR);
        int weekBucket = now.get(Calendar.YEAR) * 100 + now.get(Calendar.WEEK_OF_YEAR);
        int monthBucket = now.get(Calendar.YEAR) * 100 + now.get(Calendar.MONTH);
        SharedPreferences.Editor editor = null;
        if (prefs.getInt(KEY_DAY_BUCKET, 0) != dayBucket) {
            editor = prefs.edit()
                    .putInt(KEY_DAY_BUCKET, dayBucket)
                    .putInt(KEY_DAY_PAGES_PROTECTED, 0)
                    .putInt(KEY_DAY_ITEMS_BLOCKED, 0);
        }
        if (prefs.getInt(KEY_WEEK_BUCKET, 0) != weekBucket) {
            editor = editor != null ? editor : prefs.edit();
            editor.putInt(KEY_WEEK_BUCKET, weekBucket)
                    .putInt(KEY_WEEK_PAGES_PROTECTED, 0)
                    .putInt(KEY_WEEK_ITEMS_BLOCKED, 0);
        }
        if (prefs.getInt(KEY_MONTH_BUCKET, 0) != monthBucket) {
            editor = editor != null ? editor : prefs.edit();
            editor.putInt(KEY_MONTH_BUCKET, monthBucket)
                    .putInt(KEY_MONTH_PAGES_PROTECTED, 0)
                    .putInt(KEY_MONTH_ITEMS_BLOCKED, 0);
        }
        if (editor != null) {
            editor.apply();
        }
    }

    public static final class Stats {
        public final int pagesProtected;
        public final int itemsBlocked;
        public final int timeSavedSeconds;

        private Stats(int pagesProtected, int itemsBlocked, int timeSavedSeconds) {
            this.pagesProtected = pagesProtected;
            this.itemsBlocked = itemsBlocked;
            this.timeSavedSeconds = timeSavedSeconds;
        }
    }

    public static final class Report {
        public final PeriodStats today;
        public final PeriodStats thisWeek;
        public final PeriodStats thisMonth;

        private Report(PeriodStats today, PeriodStats thisWeek, PeriodStats thisMonth) {
            this.today = today;
            this.thisWeek = thisWeek;
            this.thisMonth = thisMonth;
        }
    }

    public static final class PeriodStats {
        public final int pagesProtected;
        public final int itemsBlocked;

        private PeriodStats(int pagesProtected, int itemsBlocked) {
            this.pagesProtected = pagesProtected;
            this.itemsBlocked = itemsBlocked;
        }
    }
}
