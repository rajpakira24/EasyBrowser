package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.ContentBlocking;

import java.util.Locale;

public class RuntimeManager {
    private static final String PREF_GECKO_DEBUG_LOGGING = "gecko_debug_logging_enabled";
    private static volatile GeckoRuntime runtime;

    public static GeckoRuntime getExistingRuntime() {
        return runtime;
    }

    public static GeckoRuntime getRuntime(Context context) {
        UrlUtils.initialize(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ContentBlocking.Settings contentBlockingSettings = createContentBlockingSettings(prefs);
        if (runtime == null) {
            synchronized (RuntimeManager.class) {
                if (runtime == null) {
                    GeckoRuntimeSettings.Builder runtimeSettings = new GeckoRuntimeSettings.Builder()
                            .contentBlocking(contentBlockingSettings)
                            .javaScriptEnabled(prefs.getBoolean("javascript_enabled", true))
                            .remoteDebuggingEnabled(prefs.getBoolean("remote_debugging_enabled", false))
                            .globalPrivacyControlEnabled(prefs.getBoolean("do_not_track", true))
                            .fontSizeFactor(prefs.getInt("text_size_percent", 100) / 100f)
                            .forceUserScalableEnabled(
                                    prefs.getBoolean(SettingsKeys.PREF_FORCE_ENABLE_ZOOM, false))
                            .translationsOfferPopup(
                                    prefs.getBoolean(SettingsKeys.PREF_TRANSLATIONS_OFFER_POPUP, true))
                            .locales(getPreferredLocales(prefs))
                            .preferredColorScheme(getPreferredColorScheme(prefs))
                            .trustedRecursiveResolverMode(getDohMode(prefs))
                            .trustedRecursiveResolverUri(getDohUri(prefs))
                            .debugLogging(prefs.getBoolean(PREF_GECKO_DEBUG_LOGGING, false));
                    applyLocalNetworkBlocking(runtimeSettings, shouldBlockLocalNetwork(prefs));

                    try {
                        runtime = GeckoRuntime.create(context, runtimeSettings.build());
                        BuiltInAdBlockerManager.apply(runtime, prefs);
                    } catch (Exception e) {
                        Log.e("RuntimeManager", "GeckoRuntime.create failed", e);
                        runtime = null;
                    }
                }
            }
        } else {
            GeckoRuntimeSettings settings = runtime.getSettings();
            settings.setJavaScriptEnabled(prefs.getBoolean("javascript_enabled", true))
                    .setRemoteDebuggingEnabled(prefs.getBoolean("remote_debugging_enabled", false))
                    .setGlobalPrivacyControl(prefs.getBoolean("do_not_track", true))
                    .setFontSizeFactor(prefs.getInt("text_size_percent", 100) / 100f)
                    .setTrustedRecursiveResolverMode(getDohMode(prefs))
                    .setTrustedRecursiveResolverUri(getDohUri(prefs))
                    .setForceUserScalableEnabled(
                            prefs.getBoolean(SettingsKeys.PREF_FORCE_ENABLE_ZOOM, false))
                    .setTranslationsOfferPopup(
                            prefs.getBoolean(SettingsKeys.PREF_TRANSLATIONS_OFFER_POPUP, true))
                    .setPreferredColorScheme(getPreferredColorScheme(prefs));
            applyLocalNetworkBlocking(settings, shouldBlockLocalNetwork(prefs));
            settings.setLocales(getPreferredLocales(prefs));
            applyContentBlocking(runtime.getSettings().getContentBlocking(), prefs);
            BuiltInAdBlockerManager.apply(runtime, prefs);
        }
        return runtime;
    }

    private static ContentBlocking.Settings createContentBlockingSettings(SharedPreferences prefs) {
        String level = prefs.getString("ad_blocking_level", "balanced");
        int antiTracking = getAntiTracking(level);
        int etpLevel = getEtpLevel(level);
        return new ContentBlocking.Settings.Builder()
                .antiTracking(antiTracking)
                .enhancedTrackingProtectionLevel(etpLevel)
                .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .strictSocialTrackingProtection(true)
                .cookieBannerHandlingMode(prefs.getBoolean("block_cookie_banners", true)
                        ? ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT
                        : ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
                .cookieBannerHandlingModePrivateBrowsing(
                        ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
                .queryParameterStrippingEnabled(prefs.getBoolean("strip_tracking_params", true))
                .queryParameterStrippingPrivateBrowsingEnabled(true)
                .build();
    }

    private static void applyContentBlocking(ContentBlocking.Settings settings, SharedPreferences prefs) {
        String level = prefs.getString("ad_blocking_level", "balanced");
        settings.setAntiTracking(getAntiTracking(level))
                .setEnhancedTrackingProtectionLevel(getEtpLevel(level))
                .setSafeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .setCookieBannerMode(prefs.getBoolean("block_cookie_banners", true)
                        ? ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT
                        : ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
                .setCookieBannerModePrivateBrowsing(
                        ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
                .setQueryParameterStrippingEnabled(prefs.getBoolean("strip_tracking_params", true))
                .setQueryParameterStrippingPrivateBrowsingEnabled(true);
    }

    private static int getAntiTracking(String level) {
        if ("off".equals(level)) {
            return ContentBlocking.AntiTracking.NONE;
        }
        if ("aggressive".equals(level)) {
            return ContentBlocking.AntiTracking.STRICT;
        }
        return ContentBlocking.AntiTracking.DEFAULT | ContentBlocking.AntiTracking.AD;
    }

    private static int getEtpLevel(String level) {
        if ("off".equals(level)) {
            return ContentBlocking.EtpLevel.NONE;
        }
        if ("aggressive".equals(level)) {
            return ContentBlocking.EtpLevel.STRICT;
        }
        return ContentBlocking.EtpLevel.DEFAULT;
    }

    private static int getDohMode(SharedPreferences prefs) {
        String mode = prefs.getString("doh_mode", "off");
        if ("opportunistic".equals(mode)) return GeckoRuntimeSettings.TRR_MODE_FIRST;
        if ("strict".equals(mode)) return GeckoRuntimeSettings.TRR_MODE_ONLY;
        return GeckoRuntimeSettings.TRR_MODE_OFF;
    }

    private static String getDohUri(SharedPreferences prefs) {
        if ("off".equals(prefs.getString("doh_mode", "off"))) return "";
        String provider = prefs.getString("doh_provider", "cloudflare");
        switch (provider) {
            case "google":  return "https://dns.google/dns-query";
            case "nextdns": return "https://dns.nextdns.io/dns-query";
            case "custom":  return prefs.getString("doh_uri", "");
            default:        return "https://cloudflare-dns.com/dns-query";
        }
    }

    private static int getPreferredColorScheme(SharedPreferences prefs) {
        String mode = prefs.getString(SettingsKeys.PREF_THEME_MODE, "system");
        if ("light".equals(mode)) {
            return GeckoRuntimeSettings.COLOR_SCHEME_LIGHT;
        }
        if ("dark".equals(mode)) {
            return GeckoRuntimeSettings.COLOR_SCHEME_DARK;
        }
        return GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM;
    }

    private static String[] getPreferredLocales(SharedPreferences prefs) {
        String raw = prefs.getString(SettingsKeys.PREF_PREFERRED_LANGUAGES, "");
        if (raw == null || raw.trim().isEmpty()) {
            return new String[]{Locale.getDefault().toLanguageTag()};
        }
        String[] parts = raw.split(",");
        java.util.ArrayList<String> locales = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                locales.add(trimmed);
            }
        }
        if (locales.isEmpty()) {
            locales.add(Locale.getDefault().toLanguageTag());
        }
        return locales.toArray(new String[0]);
    }

    private static boolean shouldBlockLocalNetwork(SharedPreferences prefs) {
        return SettingsKeys.VALUE_DENY.equals(
                prefs.getString(SettingsKeys.PREF_SITE_LOCAL_NETWORK, SettingsKeys.VALUE_ASK));
    }

    private static void applyLocalNetworkBlocking(Object target, boolean enabled) {
        if (target == null) {
            return;
        }
        try {
            target.getClass()
                    .getMethod("setLnaBlockingEnabled", boolean.class)
                    .invoke(target, enabled);
        } catch (ReflectiveOperationException ignored) {
            // GeckoView 150 no longer exposes this setting on the public API.
        }
    }
}
