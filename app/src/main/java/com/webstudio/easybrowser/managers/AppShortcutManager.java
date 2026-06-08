package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

public final class AppShortcutManager {
    public static final String ACTION_NEW_TAB =
            "com.webstudio.easybrowser.action.NEW_TAB";
    public static final String ACTION_NEW_PRIVATE_TAB =
            "com.webstudio.easybrowser.action.NEW_PRIVATE_TAB";
    public static final String ACTION_WIDGETS =
            "com.webstudio.easybrowser.action.WIDGETS";
    public static final String EXTRA_FROM_APP_SHORTCUT = "from_app_shortcut";

    private static final String TAG = "AppShortcutManager";
    private static final String ID_WIDGETS = "widgets";
    private static final String ID_NEW_TAB = "new_tab";
    private static final String ID_PRIVATE_TAB = "private_tab";

    private AppShortcutManager() {
    }

    public static void publish(Context context) {
        if (context == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        List<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        shortcuts.add(buildShortcut(appContext,
                ID_WIDGETS,
                R.string.shortcut_widgets_short,
                R.string.shortcut_widgets_long,
                R.drawable.ic_view_grid,
                ACTION_WIDGETS,
                0));
        shortcuts.add(buildShortcut(appContext,
                ID_NEW_TAB,
                R.string.shortcut_new_tab_short,
                R.string.shortcut_new_tab_long,
                R.drawable.ic_add,
                ACTION_NEW_TAB,
                1));
        shortcuts.add(buildShortcut(appContext,
                ID_PRIVATE_TAB,
                R.string.shortcut_private_tab_short,
                R.string.shortcut_private_tab_long,
                R.drawable.ic_incognito,
                ACTION_NEW_PRIVATE_TAB,
                2));

        try {
            ShortcutManagerCompat.setDynamicShortcuts(appContext, shortcuts);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to publish launcher shortcuts", e);
        }
    }

    private static ShortcutInfoCompat buildShortcut(Context context,
                                                    String id,
                                                    int shortLabelRes,
                                                    int longLabelRes,
                                                    int iconRes,
                                                    String action,
                                                    int rank) {
        Intent intent = new Intent(action)
                .setClass(context, MainActivity.class)
                .putExtra(EXTRA_FROM_APP_SHORTCUT, true);
        return new ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(context.getString(shortLabelRes))
                .setLongLabel(context.getString(longLabelRes))
                .setIcon(IconCompat.createWithResource(context, iconRes))
                .setIntent(intent)
                .setRank(rank)
                .build();
    }
}
