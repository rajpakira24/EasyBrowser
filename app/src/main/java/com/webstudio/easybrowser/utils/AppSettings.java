package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppSettings {
    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        Context appContext = context.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(
                appContext != null ? appContext : context);
    }

    public String getWallpaperMode() {
        return prefs.getString(SettingsKeys.PREF_WALLPAPER_MODE, "auto");
    }

    public String getWallpaperCollection() {
        return prefs.getString(SettingsKeys.PREF_WALLPAPER_COLLECTION, "nature");
    }

    public String getWallpaperUserUri() {
        return prefs.getString(SettingsKeys.PREF_WALLPAPER_USER_URI, "");
    }

    public int getWallpaperBlur() {
        return getInt(SettingsKeys.PREF_WALLPAPER_BLUR, 0);
    }

    public int getWallpaperOverlay() {
        return getInt(SettingsKeys.PREF_WALLPAPER_OVERLAY, 42);
    }

    public Set<String> getWallpaperFavoriteIds() {
        return getStringSet(SettingsKeys.PREF_WALLPAPER_FAVORITES);
    }

    public void setWallpaperFavoriteIds(Set<String> favoriteIds) {
        putStringSet(SettingsKeys.PREF_WALLPAPER_FAVORITES, favoriteIds);
    }

    public boolean isWallpaperFavoritesOnly() {
        return prefs.getBoolean(SettingsKeys.PREF_WALLPAPER_FAVORITES_ONLY, false);
    }

    public boolean isWallpaperOfflinePackEnabled() {
        return prefs.getBoolean(SettingsKeys.PREF_WALLPAPER_OFFLINE_PACK, false);
    }

    public Set<String> getCollapsedGroupIds() {
        return getStringSet(SettingsKeys.PREF_COLLAPSED_TAB_GROUPS);
    }

    public boolean isGroupCollapsed(String groupId) {
        return groupId != null && getCollapsedGroupIds().contains(groupId);
    }

    public void setGroupCollapsed(String groupId, boolean collapsed) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        Set<String> collapsedIds = getCollapsedGroupIds();
        if (collapsed) {
            collapsedIds.add(groupId);
        } else {
            collapsedIds.remove(groupId);
        }
        putStringSet(SettingsKeys.PREF_COLLAPSED_TAB_GROUPS, collapsedIds);
    }

    private int getInt(String key, int defaultValue) {
        try {
            return prefs.getInt(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    private Set<String> getStringSet(String key) {
        try {
            return new LinkedHashSet<>(prefs.getStringSet(key, Collections.emptySet()));
        } catch (ClassCastException e) {
            return new LinkedHashSet<>();
        }
    }

    private void putStringSet(String key, Set<String> values) {
        prefs.edit()
                .putStringSet(key, new LinkedHashSet<>(values != null
                        ? values
                        : Collections.emptySet()))
                .apply();
    }
}
