package com.webstudio.easybrowser.managers;

import android.content.SharedPreferences;
import android.util.Log;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.WebExtension;

public final class BuiltInAdBlockerManager {
    private static final String TAG = "BuiltInAdBlocker";
    private static final String EXTENSION_ID = "easy-adblocker@easybrowser.local";
    private static final String EXTENSION_LOCATION =
            "resource://android/assets/extensions/easy_adblocker/";

    private BuiltInAdBlockerManager() {
    }

    public static void apply(GeckoRuntime runtime, SharedPreferences prefs) {
        if (runtime == null || prefs == null) {
            return;
        }
        if ("off".equals(prefs.getString("ad_blocking_level", "balanced"))) {
            uninstall(runtime);
            return;
        }
        runtime.getWebExtensionController()
                .ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID)
                .accept(extension -> { },
                        error -> Log.e(TAG, "Failed to install built-in ad blocker", error));
    }

    private static void uninstall(GeckoRuntime runtime) {
        runtime.getWebExtensionController().list()
                .accept(extensions -> {
                    if (extensions == null) {
                        return;
                    }
                    for (WebExtension extension : extensions) {
                        if (EXTENSION_ID.equals(extension.id)) {
                            runtime.getWebExtensionController().uninstall(extension)
                                    .accept(value -> { },
                                            error -> Log.e(TAG,
                                                    "Failed to uninstall built-in ad blocker",
                                                    error));
                            return;
                        }
                    }
                }, error -> Log.e(TAG, "Failed to list extensions", error));
    }
}
