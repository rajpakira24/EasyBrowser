package com.webstudio.easybrowser.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;

public final class SystemBarUtils {
    private SystemBarUtils() {
    }

    public static void apply(Activity activity, int statusBarColor, int navigationBarColor) {
        if (activity == null) {
            return;
        }
        apply(activity, statusBarColor, navigationBarColor, isLightTheme(activity));
    }

    public static void apply(Activity activity, int statusBarColor, int navigationBarColor,
                             boolean useDarkSystemBarIcons) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(navigationBarColor);

        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = useDarkSystemBarIcons
                    ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = useDarkSystemBarIcons
                    ? flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decorView.setSystemUiVisibility(flags);
    }

    public static boolean isLightTheme(Activity activity) {
        int nightMode = activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_YES;
    }
}
