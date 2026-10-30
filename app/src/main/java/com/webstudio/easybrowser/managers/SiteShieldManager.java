package com.webstudio.easybrowser.managers;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebExtension;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Per-site shield: lets the user turn ad and tracker blocking off for one host.
 * The host list lives in preferences; Gecko tracking protection is bypassed through its
 * content blocking exceptions and the built-in cosmetic ad blocker asks us over native
 * messaging whether it should stay quiet on a host.
 */
public final class SiteShieldManager {
    public static final String PREF_DISABLED_HOSTS = "adblock_disabled_hosts";
    public static final String NATIVE_APP = "browser";

    private static final String TAG = "SiteShieldManager";

    private SiteShieldManager() {
    }

    public static String normalizeHost(@Nullable String host) {
        if (host == null) {
            return "";
        }
        String value = host.trim().toLowerCase(Locale.US);
        return value.startsWith("www.") ? value.substring(4) : value;
    }

    public static boolean isDisabled(SharedPreferences prefs, @Nullable String host) {
        String key = normalizeHost(host);
        return !key.isEmpty()
                && prefs.getStringSet(PREF_DISABLED_HOSTS, new HashSet<>()).contains(key);
    }

    public static void setDisabled(SharedPreferences prefs, @Nullable String host,
                                   boolean disabled) {
        String key = normalizeHost(host);
        if (key.isEmpty()) {
            return;
        }
        Set<String> hosts = new HashSet<>(
                prefs.getStringSet(PREF_DISABLED_HOSTS, new HashSet<>()));
        if (disabled) {
            hosts.add(key);
        } else {
            hosts.remove(key);
        }
        prefs.edit().putStringSet(PREF_DISABLED_HOSTS, hosts).apply();
    }

    /** Mirrors the preference onto Gecko's tracking protection exception for the session. */
    public static void applyException(@Nullable GeckoRuntime runtime,
                                      @Nullable GeckoSession session, boolean disabled) {
        if (runtime == null || session == null) {
            return;
        }
        try {
            if (disabled) {
                runtime.getContentBlockingController().addException(session);
            } else {
                runtime.getContentBlockingController().removeException(session);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update content blocking exception", e);
        }
    }

    /** Answers the built-in ad blocker extension's "is this host disabled" question. */
    static WebExtension.MessageDelegate createMessageDelegate(SharedPreferences prefs) {
        return new WebExtension.MessageDelegate() {
            @Nullable
            @Override
            public GeckoResult<Object> onMessage(@NonNull String nativeApp,
                                                 @NonNull Object message,
                                                 @NonNull WebExtension.MessageSender sender) {
                boolean disabled = false;
                if (message instanceof JSONObject) {
                    disabled = isDisabled(prefs, ((JSONObject) message).optString("host"));
                }
                try {
                    return GeckoResult.fromValue(new JSONObject().put("disabled", disabled));
                } catch (JSONException e) {
                    return GeckoResult.fromValue(null);
                }
            }
        };
    }
}
