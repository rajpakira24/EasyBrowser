package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public final class PrivacyStatsManager {
    public static final String KEY_PAGES_PROTECTED = "privacy_pages_protected";
    public static final String KEY_ITEMS_BLOCKED = "privacy_items_blocked";
    public static final String KEY_TIME_SAVED_SECONDS = "privacy_time_saved_seconds";

    private static final String KEY_SCHEMA_VERSION = "privacy_stats_schema_version";
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

    public static boolean isStatsKey(String key) {
        return KEY_PAGES_PROTECTED.equals(key)
                || KEY_ITEMS_BLOCKED.equals(key)
                || KEY_TIME_SAVED_SECONDS.equals(key);
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
            int currentProtectedPages = prefs.getInt(KEY_PAGES_PROTECTED, 0);
            int currentBlockedItems = prefs.getInt(KEY_ITEMS_BLOCKED, 0);
            int currentTimeSaved = prefs.getInt(KEY_TIME_SAVED_SECONDS, 0);
            prefs.edit()
                    .putInt(KEY_PAGES_PROTECTED, currentProtectedPages + protectedPages)
                    .putInt(KEY_ITEMS_BLOCKED, currentBlockedItems + blockedItems)
                    .putInt(KEY_TIME_SAVED_SECONDS,
                            currentTimeSaved + blockedItems * SECONDS_SAVED_PER_BLOCKED_ITEM)
                    .apply();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
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
}
