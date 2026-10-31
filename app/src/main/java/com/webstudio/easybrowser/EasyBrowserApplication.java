package com.webstudio.easybrowser;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;

import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.managers.AppShortcutManager;
import com.webstudio.easybrowser.ui.activity.GroupTabsActivity;
import com.webstudio.easybrowser.ui.activity.MainActivity;
import com.webstudio.easybrowser.ui.activity.PasswordsActivity;
import com.webstudio.easybrowser.ui.activity.WallpaperCropActivity;
import com.webstudio.easybrowser.ui.activity.WeatherActivity;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.managers.AppNotificationChannels;

public class EasyBrowserApplication extends Application {
    private static final long PROCESS_START_ELAPSED_REALTIME = SystemClock.elapsedRealtime();

    public static long getProcessStartElapsedRealtime() {
        return PROCESS_START_ELAPSED_REALTIME;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashRecoveryMarker();
        applySavedThemeMode();
        AppNotificationChannels.ensureCreated(this);
        AppShortcutManager.publish(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ScreenshotProtection.apply(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                ScreenshotProtection.apply(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                applySystemBarInsets(activity);
            }

            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    // Targeting a recent SDK forces edge-to-edge, so content would otherwise draw under the
    // status bar and stay behind the soft keyboard. Pad each screen by the bar + IME insets
    // unless it manages them itself.
    private static void applySystemBarInsets(Activity activity) {
        if (activity instanceof MainActivity
                || activity instanceof PasswordsActivity
                || activity instanceof WallpaperCropActivity
                || activity instanceof GroupTabsActivity
                || activity instanceof WeatherActivity) {
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        if (content == null || content.getTag(R.id.system_bar_insets_applied) != null) {
            return;
        }
        content.setTag(R.id.system_bar_insets_applied, Boolean.TRUE);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets pad = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(pad.left, pad.top, pad.right, pad.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void installCrashRecoveryMarker() {
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(SettingsKeys.PREF_BROWSER_CRASH_RESTORE_PENDING, true)
                    .apply();
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    private void applySavedThemeMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            if (userManager != null && !userManager.isUserUnlocked()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                return;
            }
        }

        String mode = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsKeys.PREF_THEME_MODE, "system");
        if ("light".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
