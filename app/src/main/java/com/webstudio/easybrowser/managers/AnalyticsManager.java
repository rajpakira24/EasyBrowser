package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

/**
 * Centralized helper for Firebase Analytics tracking.
 * Provides methods for tracking browser navigation, feature usage, and performance.
 * Follows best practices for user privacy by sanitizing URLs and respecting private mode.
 */
public final class AnalyticsManager {
    private static volatile FirebaseAnalytics analytics;

    // Custom Parameter Names
    private static final String PARAM_DOMAIN = "domain";
    private static final String PARAM_LOAD_TIME = "load_time_ms";
    private static final String PARAM_TAB_COUNT = "tab_count";
    private static final String PARAM_ENGINE = "search_engine";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_FILE_EXTENSION = "file_extension";
    private static final String PARAM_MIME_TYPE = "mime_type";
    private static final String PARAM_ERROR_CODE = "error_code";

    private AnalyticsManager() {
    }

    // --- 1. Core Navigation Events ---

    /**
     * Tracks when a page load starts. Logs only the domain for privacy.
     */
    public static void logPageLoadStarted(Context context, String url, boolean isPrivate) {
        if (isPrivate || isInternal(url)) return;
        Bundle params = new Bundle();
        params.putString(PARAM_DOMAIN, getDomain(url));
        logEvent(context, "page_load_started", params);
    }

    /**
     * Tracks when a page load finishes with its duration.
     */
    public static void logPageLoadFinished(Context context, String url, long loadTimeMs, boolean isPrivate) {
        if (isPrivate || isInternal(url)) return;
        Bundle params = new Bundle();
        params.putString(PARAM_DOMAIN, getDomain(url));
        params.putLong(PARAM_LOAD_TIME, loadTimeMs);
        logEvent(context, "page_load_finished", params);
    }

    /**
     * Tracks when the user submits a URL or search query from the main screen.
     */
    public static void logNavigationSubmitted(Context context, String input, boolean isPrivate) {
        if (isPrivate) return;
        Bundle params = new Bundle();
        // Determine if it's a URL or search based on spaces (simple heuristic)
        boolean looksLikeUrl = input != null && !input.trim().contains(" ") && input.contains(".");
        params.putString("input_type", looksLikeUrl ? "url" : "search");
        logEvent(context, "navigation_submitted", params);
    }

    /**
     * Tracks search queries and the engine used.
     */
    public static void logSearchQuery(Context context, String query, String engine, boolean isPrivate) {
        if (isPrivate) return;
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SEARCH_TERM, query);
        params.putString(PARAM_ENGINE, engine);
        logEvent(context, FirebaseAnalytics.Event.SEARCH, params);
    }

    // --- 2. Browser Feature Usage ---

    public static void logTabOpened(Context context, int totalTabs) {
        Bundle params = new Bundle();
        params.putInt(PARAM_TAB_COUNT, totalTabs);
        logEvent(context, "tab_opened", params);
    }

    public static void logTabClosed(Context context, int totalTabs) {
        Bundle params = new Bundle();
        params.putInt(PARAM_TAB_COUNT, totalTabs);
        logEvent(context, "tab_closed", params);
    }

    public static void logBookmarkAdded(Context context, String url) {
        Bundle params = new Bundle();
        params.putString(PARAM_DOMAIN, getDomain(url));
        logEvent(context, "bookmark_added", params);
    }

    public static void logBookmarkOpened(Context context, String url) {
        Bundle params = new Bundle();
        params.putString(PARAM_DOMAIN, getDomain(url));
        logEvent(context, "bookmark_opened", params);
    }

    public static void logIncognitoModeToggled(Context context, boolean isEnabled) {
        Bundle params = new Bundle();
        params.putString(PARAM_STATE, isEnabled ? "on" : "off");
        logEvent(context, "incognito_mode_toggled", params);
    }

    public static void logHistoryCleared(Context context) {
        logEvent(context, "history_cleared", null);
    }

    // --- 3. Interactions & Media ---

    public static void logDownloadStarted(Context context, String url, String mimeType) {
        Bundle params = new Bundle();
        params.putString(PARAM_FILE_EXTENSION, getFileExtension(url));
        params.putString(PARAM_MIME_TYPE, mimeType);
        logEvent(context, "download_started", params);
    }

    public static void logDownloadCompleted(Context context, String url, String mimeType) {
        Bundle params = new Bundle();
        params.putString(PARAM_FILE_EXTENSION, getFileExtension(url));
        params.putString(PARAM_MIME_TYPE, mimeType);
        logEvent(context, "download_completed", params);
    }

    public static void logPageShared(Context context, String url, String platform) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "web_page");
        params.putString(FirebaseAnalytics.Param.ITEM_ID, getDomain(url));
        params.putString(FirebaseAnalytics.Param.METHOD, platform);
        logEvent(context, FirebaseAnalytics.Event.SHARE, params);
    }

    // --- 4. Performance & Errors ---

    public static void logWebPageError(Context context, String url, int errorCode) {
        Bundle params = new Bundle();
        params.putString(PARAM_DOMAIN, getDomain(url));
        params.putInt(PARAM_ERROR_CODE, errorCode);
        logEvent(context, "web_page_error", params);
    }

    // --- Compatibility / Internal Helpers ---

    private static void logEvent(Context context, String name, Bundle params) {
        FirebaseAnalytics instance = getAnalytics(context);
        if (instance != null) {
            try {
                instance.logEvent(name, params);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static FirebaseAnalytics getAnalytics(Context context) {
        if (context == null) return null;
        if (analytics == null) {
            synchronized (AnalyticsManager.class) {
                if (analytics == null) {
                    try {
                        analytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
                    } catch (RuntimeException ignored) {
                        return null;
                    }
                }
            }
        }
        return analytics;
    }

    private static String getDomain(String url) {
        if (TextUtils.isEmpty(url)) return "unknown";
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return "internal";
            return host.toLowerCase(Locale.US).startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "invalid";
        }
    }

    private static String getFileExtension(String url) {
        if (TextUtils.isEmpty(url)) return "unknown";
        try {
            String path = Uri.parse(url).getPath();
            if (path != null && path.contains(".")) {
                return path.substring(path.lastIndexOf(".")).toLowerCase(Locale.US);
            }
        } catch (Exception ignored) {
        }
        return "none";
    }

    private static boolean isInternal(String url) {
        if (TextUtils.isEmpty(url)) return true;
        return url.startsWith("about:") || url.startsWith("data:") || url.startsWith("file:");
    }

    // --- Legacy / Required by other classes ---

    public static void logSettingChanged(Context context, String setting, boolean enabled) {
        Bundle params = new Bundle();
        params.putString("setting", setting);
        params.putString("state", enabled ? "enabled" : "disabled");
        logEvent(context, "setting_changed", params);
    }

    public static void logExtensionInstall(Context context, String source, boolean success) {
        Bundle params = new Bundle();
        params.putString("source", source);
        params.putString("result", success ? "success" : "failure");
        logEvent(context, "extension_install", params);
    }
}
