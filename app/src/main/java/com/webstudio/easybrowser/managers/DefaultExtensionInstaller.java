package com.webstudio.easybrowser.managers;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

/**
 * Pre-installs uBlock Origin for every user on first run so privacy protection
 * works out of the box. Runs at most once: the attempt is recorded in
 * SharedPreferences, so a user who later removes uBlock is never nagged with a
 * forced reinstall. A failed attempt (e.g. offline at first launch) is retried
 * on the next launch.
 */
public final class DefaultExtensionInstaller {
    private static final String TAG = "DefaultExtInstaller";
    private static final String UBLOCK_ID = "uBlock0@raymondhill.net";
    private static final String UBLOCK_XPI =
            "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi";
    private static final String PREF_UBLOCK_PREINSTALL_DONE = "ublock_preinstall_done";

    private DefaultExtensionInstaller() {
    }

    public static void preinstallDefaults(GeckoRuntime runtime, SharedPreferences prefs) {
        if (runtime == null || prefs == null) {
            return;
        }
        if (prefs.getBoolean(PREF_UBLOCK_PREINSTALL_DONE, false)) {
            return;
        }
        WebExtensionController controller = runtime.getWebExtensionController();
        // If uBlock is already present (user installed it manually), just record the
        // attempt and stop — never install a second copy.
        controller.list().accept(extensions -> {
            if (extensions != null) {
                for (WebExtension extension : extensions) {
                    if (extension != null && UBLOCK_ID.equals(extension.id)) {
                        markDone(prefs);
                        return;
                    }
                }
            }
            installUblock(controller, prefs);
        }, error -> installUblock(controller, prefs));
    }

    private static void installUblock(WebExtensionController controller, SharedPreferences prefs) {
        // Silent install: auto-grant the requested permissions for the bundled default.
        controller.setPromptDelegate(new WebExtensionController.PromptDelegate() {
            @Override
            public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(
                    @NonNull WebExtension extension,
                    @NonNull String[] permissions,
                    @NonNull String[] origins,
                    @NonNull String[] dataCollectionPermissions) {
                return GeckoResult.fromValue(
                        new WebExtension.PermissionPromptResponse(true, true, true));
            }
        });
        controller.install(UBLOCK_XPI, WebExtensionController.INSTALLATION_METHOD_MANAGER)
                .accept(extension -> {
                    Log.i(TAG, "uBlock Origin pre-installed");
                    markDone(prefs);
                    // Drop the auto-accept delegate so it can never silently approve a
                    // later web-initiated extension install. ExtensionsActivity sets its
                    // own delegate whenever the user opens it.
                    controller.setPromptDelegate(null);
                }, error -> {
                    Log.e(TAG, "uBlock Origin pre-install failed; will retry next launch", error);
                    controller.setPromptDelegate(null);
                });
    }

    private static void markDone(SharedPreferences prefs) {
        prefs.edit().putBoolean(PREF_UBLOCK_PREINSTALL_DONE, true).apply();
    }
}
