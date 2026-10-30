package com.webstudio.easybrowser.managers;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.WebExtension;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Per-site shield: lets the user turn the built-in ad blocker off for one host. The host
 * list lives in preferences and the cosmetic ad blocker extension asks us over native
 * messaging whether it should stay quiet on a host. GeckoView has no per-site exception
 * for its tracking protection, so that part stays global.
 */
public final class SiteShieldManager {
    public static final String PREF_DISABLED_HOSTS = "adblock_disabled_hosts";
    public static final String NATIVE_APP = "browser";

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
