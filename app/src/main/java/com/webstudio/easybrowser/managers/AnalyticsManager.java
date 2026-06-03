package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.analytics.FirebaseAnalytics;

public final class AnalyticsManager {
    private static volatile FirebaseAnalytics analytics;

    private AnalyticsManager() {
    }

    public static void logNavigationSubmitted(Context context, String input, boolean isPrivate) {
        if (isPrivate) {
            return;
        }
        Bundle params = new Bundle();
        params.putString("input_type", getInputType(input));
        logEvent(context, "navigation_submitted", params);
    }

    public static void logPageLoadRequested(Context context, String url, boolean isPrivate) {
        if (isPrivate) {
            return;
        }
        Bundle params = new Bundle();
        params.putString("scheme", getSafeScheme(url));
        params.putString("page_type", getPageType(url));
        logEvent(context, "page_load_requested", params);
    }

    public static void logDownloadStarted(Context context, String mimeType) {
        Bundle params = new Bundle();
        params.putString("mime_group", getMimeGroup(mimeType));
        logEvent(context, "download_started", params);
    }

    public static void logDownloadCompleted(Context context, String mimeType) {
        Bundle params = new Bundle();
        params.putString("mime_group", getMimeGroup(mimeType));
        logEvent(context, "download_completed", params);
    }

    public static void logSettingChanged(Context context, String setting, boolean enabled) {
        Bundle params = new Bundle();
        params.putString("setting", setting);
        params.putString("state", enabled ? "enabled" : "disabled");
        logEvent(context, "setting_changed", params);
    }

    public static void logExtensionInstall(Context context, String source, boolean success) {
        Bundle params = new Bundle();
        params.putString("source", sanitizeSource(source));
        params.putString("result", success ? "success" : "failure");
        logEvent(context, "extension_install", params);
    }

    private static void logEvent(Context context, String name, Bundle params) {
        FirebaseAnalytics instance = getAnalytics(context);
        if (instance == null) {
            return;
        }
        try {
            instance.logEvent(name, params);
        } catch (RuntimeException ignored) {
        }
    }

    private static FirebaseAnalytics getAnalytics(Context context) {
        if (context == null) {
            return null;
        }
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

    private static String getInputType(String input) {
        if (TextUtils.isEmpty(input)) {
            return "empty";
        }
        String value = input.trim().toLowerCase(java.util.Locale.US);
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return "url";
        }
        if (value.startsWith("about:") || value.startsWith("data:")) {
            return "internal";
        }
        if (value.contains(" ") || !value.contains(".")) {
            return "search";
        }
        return "url";
    }

    private static String getSafeScheme(String url) {
        if (TextUtils.isEmpty(url)) {
            return "unknown";
        }
        try {
            String scheme = Uri.parse(url).getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                    || "about".equalsIgnoreCase(scheme) || "data".equalsIgnoreCase(scheme)) {
                return scheme.toLowerCase(java.util.Locale.US);
            }
        } catch (RuntimeException ignored) {
        }
        return "other";
    }

    private static String getPageType(String url) {
        String scheme = getSafeScheme(url);
        if ("about".equals(scheme) || "data".equals(scheme)) {
            return "internal";
        }
        if ("http".equals(scheme) || "https".equals(scheme)) {
            return "web";
        }
        return "other";
    }

    private static String getMimeGroup(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return "unknown";
        }
        String value = mimeType.toLowerCase(java.util.Locale.US);
        int separator = value.indexOf('/');
        return separator > 0 ? value.substring(0, separator) : "other";
    }

    private static String sanitizeSource(String source) {
        if (TextUtils.isEmpty(source)) {
            return "unknown";
        }
        String value = source.toLowerCase(java.util.Locale.US);
        if (value.contains("addons.mozilla.org")) {
            return "mozilla_addons";
        }
        if ("recommended".equals(value) || "marketplace".equals(value) || "manual".equals(value)) {
            return value;
        }
        return "other";
    }
}
