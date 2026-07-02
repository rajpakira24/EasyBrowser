package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.BookmarkEntity;
import com.webstudio.easybrowser.database.entity.HistoryEntity;
import com.webstudio.easybrowser.models.Suggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Local (bookmark + history) suggestions for the URL bar. Returns structured rows with a page
 * title and URL rather than bare URL strings, ranks host-prefix matches ("you" → youtube.com)
 * above incidental substring hits, deduplicates URL variants that differ only by tracking
 * parameters, and caps results per site so one domain can't flood the whole dropdown.
 */
public final class BrowserSuggestionProvider {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_PER_HOST = 2;
    // Query parameters that don't change the page identity — stripped for duplicate detection
    // so e.g. the same video with and without a share-tracking token is one suggestion.
    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "pp", "si", "feature", "ref", "ref_src", "fbclid", "gclid", "igshid",
            "mc_cid", "mc_eid", "spm", "share_id"));

    public interface SuggestionsCallback {
        void onSuggestions(List<Suggestion> suggestions);
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
            List<Candidate> candidates = new ArrayList<>();
            collectBookmarks(database.bookmarkDao().getAllBookmarks(), needle, candidates);
            if (shouldSuggestHistory(appContext)) {
                collectHistory(appContext, database.historyDao().getAllHistory(), needle, candidates);
            }
            callback.onSuggestions(pickBest(candidates, limit));
        });
    }

    private static boolean shouldSuggestHistory(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("save_history", true);
    }

    private static void collectBookmarks(List<BookmarkEntity> bookmarks, String needle,
                                         List<Candidate> out) {
        if (bookmarks == null) {
            return;
        }
        for (BookmarkEntity bookmark : bookmarks) {
            int score = matchScore(bookmark.getTitle(), bookmark.getUrl(), needle);
            if (score >= 0) {
                out.add(new Candidate(
                        Suggestion.bookmark(displayTitle(bookmark.getTitle(), bookmark.getUrl()),
                                bookmark.getUrl().trim()),
                        true, score));
            }
        }
    }

    private static void collectHistory(Context context, List<HistoryEntity> history,
                                       String needle, List<Candidate> out) {
        if (history == null) {
            return;
        }
        for (HistoryEntity item : history) {
            // Skip search-results pages ("flipkart site:... at DuckDuckGo") — they're query
            // history, not a site the user meant to revisit, and clutter suggestions with
            // near-duplicate engine URLs.
            if (UrlUtils.isSearchResultsUrl(context, item.getUrl())) {
                continue;
            }
            int score = matchScore(item.getTitle(), item.getUrl(), needle);
            if (score >= 0) {
                out.add(new Candidate(
                        Suggestion.history(displayTitle(item.getTitle(), item.getUrl()),
                                item.getUrl().trim()),
                        false, score));
            }
        }
    }

    // -1 = no match. Lower scores rank higher: a host that starts with what the user typed is a
    // far stronger signal than the needle appearing somewhere in a title or path.
    private static int matchScore(String title, String url, String needle) {
        if (url == null || url.trim().isEmpty()) {
            return -1;
        }
        String host = UrlUtils.getQuickAccessTitle(url);
        if (host != null && host.toLowerCase(Locale.US).startsWith(needle)) {
            return 0;
        }
        if (contains(title, needle)) {
            return 1;
        }
        if (contains(url, needle)) {
            return 2;
        }
        return -1;
    }

    private static List<Suggestion> pickBest(List<Candidate> candidates, int limit) {
        // Stable sort: bookmarks before history, better match score first; DB order (bookmarks
        // by creation, history newest-first) breaks ties.
        Collections.sort(candidates, (a, b) -> {
            if (a.bookmark != b.bookmark) {
                return a.bookmark ? -1 : 1;
            }
            return Integer.compare(a.score, b.score);
        });
        List<Suggestion> result = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        Map<String, Integer> perHost = new HashMap<>();
        for (Candidate candidate : candidates) {
            if (result.size() >= limit) {
                break;
            }
            String url = candidate.suggestion.getUrl();
            String dedupeKey = normalizeForDedupe(url);
            if (!seenUrls.add(dedupeKey)) {
                continue;
            }
            String host = UrlUtils.getQuickAccessUrl(url);
            String hostKey = host != null ? host : dedupeKey;
            Integer seenForHost = perHost.get(hostKey);
            int hostCount = seenForHost != null ? seenForHost : 0;
            if (hostCount >= MAX_PER_HOST) {
                continue;
            }
            perHost.put(hostKey, hostCount + 1);
            result.add(candidate.suggestion);
        }
        return result;
    }

    // Canonical form for duplicate detection: no fragment, no tracking params, no trailing slash.
    private static String normalizeForDedupe(String url) {
        String s = url.trim();
        int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            String base = s.substring(0, q);
            StringBuilder kept = new StringBuilder();
            for (String param : s.substring(q + 1).split("&")) {
                int eq = param.indexOf('=');
                String name = (eq >= 0 ? param.substring(0, eq) : param).toLowerCase(Locale.US);
                if (TRACKING_PARAMS.contains(name) || name.startsWith("utm_")) {
                    continue;
                }
                kept.append(kept.length() == 0 ? "" : "&").append(param);
            }
            s = kept.length() > 0 ? base + "?" + kept : base;
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.toLowerCase(Locale.US);
    }

    private static String displayTitle(String title, String url) {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        String host = UrlUtils.getDisplayHost(url);
        return host != null ? host : url;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.US).contains(needle);
    }

    private static final class Candidate {
        final Suggestion suggestion;
        final boolean bookmark;
        final int score;

        Candidate(Suggestion suggestion, boolean bookmark, int score) {
            this.suggestion = suggestion;
            this.bookmark = bookmark;
            this.score = score;
        }
    }

    private BrowserSuggestionProvider() {
    }
}
