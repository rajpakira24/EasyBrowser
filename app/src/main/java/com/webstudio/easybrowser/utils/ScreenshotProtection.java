package com.webstudio.easybrowser.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

public final class ScreenshotProtection {
    public static final String PREF_PREVENT_SCREENSHOTS = "prevent_screenshots";
    public static final boolean DEFAULT_PREVENT_SCREENSHOTS = true;

    private ScreenshotProtection() {
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_PREVENT_SCREENSHOTS, DEFAULT_PREVENT_SCREENSHOTS);
    }

    public static void apply(Activity activity) {
        apply(activity, isEnabled(activity));
    }

    public static void apply(Activity activity, boolean enabled) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
