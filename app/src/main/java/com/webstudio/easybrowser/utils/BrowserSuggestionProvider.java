package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.BookmarkEntity;
import com.webstudio.easybrowser.database.entity.HistoryEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BrowserSuggestionProvider {
    private static final int DEFAULT_LIMIT = 6;

    public interface SuggestionsCallback {
        void onSuggestions(List<String> suggestions);
    }

    public static void fetchSuggestions(Context context, String query,
                                        SuggestionsCallback callback) {
        fetchSuggestions(context, query, DEFAULT_LIMIT, callback);
    }

    public static void fetchSuggestions(Context context, String query, int limit,
                                        SuggestionsCallback callback) {
        if (context == null || query == null || query.trim().isEmpty()) {
            callback.onSuggestions(new ArrayList<>());
            return;
        }
        Context appContext = context.getApplicationContext();
        String needle = query.trim().toLowerCase(Locale.US);
        AppDatabase.getDatabaseExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(appContext);
            Set<String> matches = new LinkedHashSet<>();
            addBookmarkMatches(database.bookmarkDao().getAllBookmarks(), needle, matches, limit);
            if (matches.size() < limit && shouldSuggestHistory(appContext)) {
                addHistoryMatches(database.historyDao().getAllHistory(), needle, matches, limit);
            }
            callback.onSuggestions(new ArrayList<>(matches));
        });
    }

    private static boolean shouldSuggestHistory(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("save_history", true);
    }

    private static void addBookmarkMatches(List<BookmarkEntity> bookmarks, String needle,
                                           Set<String> matches, int limit) {
        if (bookmarks == null) {
            return;
        }
        for (BookmarkEntity bookmark : bookmarks) {
            if (matches.size() >= limit) {
                return;
            }
            if (matches(bookmark.getTitle(), bookmark.getUrl(), needle)) {
                addUrl(matches, bookmark.getUrl());
            }
        }
    }

    private static void addHistoryMatches(List<HistoryEntity> history, String needle,
                                          Set<String> matches, int limit) {
        if (history == null) {
            return;
        }
        for (HistoryEntity item : history) {
            if (matches.size() >= limit) {
                return;
            }
            if (matches(item.getTitle(), item.getUrl(), needle)) {
                addUrl(matches, item.getUrl());
            }
        }
    }

    private static boolean matches(String title, String url, String needle) {
        return contains(title, needle) || contains(url, needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.US).contains(needle);
    }

    private static void addUrl(Set<String> matches, String url) {
        if (url != null && !url.trim().isEmpty()) {
            matches.add(url.trim());
        }
    }

    private BrowserSuggestionProvider() {
    }
}
