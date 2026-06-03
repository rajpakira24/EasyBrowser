package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.BuildConfig;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.ContentBlocking;

public class RuntimeManager {
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
                            .trustedRecursiveResolverMode(getDohMode(prefs))
                            .trustedRecursiveResolverUri(getDohUri(prefs))
                            .debugLogging(BuildConfig.DEBUG);

                    try {
                        runtime = GeckoRuntime.create(context, runtimeSettings.build());
                    } catch (Exception e) {
                        Log.e("RuntimeManager", "GeckoRuntime.create failed", e);
                        runtime = null;
                    }
                }
            }
        } else {
            runtime.getSettings()
                    .setJavaScriptEnabled(prefs.getBoolean("javascript_enabled", true))
                    .setRemoteDebuggingEnabled(prefs.getBoolean("remote_debugging_enabled", false))
                    .setGlobalPrivacyControl(prefs.getBoolean("do_not_track", true))
                    .setFontSizeFactor(prefs.getInt("text_size_percent", 100) / 100f)
                    .setTrustedRecursiveResolverMode(getDohMode(prefs))
                    .setTrustedRecursiveResolverUri(getDohUri(prefs));
            applyContentBlocking(runtime.getSettings().getContentBlocking(), prefs);
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
}
