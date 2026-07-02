package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.util.Patterns;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.PrivacyStatsManager;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.repository.QuickAccessRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UrlUtils {
    public static final String DEFAULT_HOMEPAGE = "https://duckduckgo.com";
    public static final String DEFAULT_SEARCH_ENGINE = "https://duckduckgo.com/?q=";

    // Fallback lists used before initialize() is called or if assets are missing.
    private static final String[] BALANCED_BLOCKED_HOST_PARTS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "adsystem.com", "adnxs.com", "adsrvr.org",
            "amazon-adsystem.com", "taboola.com", "outbrain.com",
            "scorecardresearch.com", "zedo.com", "moatads.com",
            "facebook.com/tr", "analytics.google.com", "google-analytics.com"
    };
    private static final String[] AGGRESSIVE_BLOCKED_HOST_PARTS = {
            "ads.", ".ads.", "adserver", "adservice", "advertising",
            "tracking", "tracker", "metrics", "telemetry", "analytics",
            "pixel", "beacon", "affiliate"
    };

    private static volatile String[] cachedBalancedList = null;
    private static volatile String[] cachedAggressiveList = null;

    // Data: URIs above this size are dropped to avoid hitting GeckoView's
    // ERROR_DATA_URI_TOO_LONG limit and to keep the new-tab payload bounded.
    private static final int MAX_NEW_TAB_HTML_BYTES = 1_000_000;
    private static final int BROWSER_QUICK_ACCESS_VISIBLE_ITEMS = 5;
    private static final int BROWSER_QUICK_ACCESS_RENDER_LIMIT = 8;
    private static final int BROWSER_QUICK_ACCESS_TILE_GAP_PX = 8;
    private static final String[] COMMON_SITE_PREFIXES = {
            "www", "m", "mobile", "amp", "touch", "lite"
    };
    private static final String[] LANGUAGE_HOST_LABELS = {
            "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "cs",
            "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr",
            "ga", "gl", "gu", "he", "hi", "hr", "hu", "hy", "id", "is",
            "it", "ja", "ka", "kk", "kn", "ko", "lt", "lv", "mk", "ml",
            "mr", "ms", "nl", "no", "pa", "pl", "pt", "ro", "ru", "sk",
            "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tr", "uk",
            "ur", "vi", "zh"
    };

    /**
     * Must be called once at startup (e.g. from RuntimeManager.getRuntime).
     * Loads the blocklists from assets so they can be updated without a code change.
     */
    public static void initialize(Context context) {
        if (cachedBalancedList == null) {
            synchronized (UrlUtils.class) {
                if (cachedBalancedList == null) {
                    Context app = context.getApplicationContext();
                    cachedBalancedList = loadListFromAssets(app, "blocklist_balanced.txt", BALANCED_BLOCKED_HOST_PARTS);
                    cachedAggressiveList = loadListFromAssets(app, "blocklist_aggressive.txt", AGGRESSIVE_BLOCKED_HOST_PARTS);
                }
            }
        }
    }

    private static String[] loadListFromAssets(Context context, String fileName, String[] fallback) {
        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
            return lines.isEmpty() ? fallback : lines.toArray(new String[0]);
        } catch (IOException e) {
            return fallback;
        }
    }
    public static String sanitizeUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        input = input.trim();

        if (isInternalPageUrl(input) || input.startsWith("about:")) {
            return input;
        }

        // If it's already a valid URL with scheme, return it
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }

        // If it looks like a search term, don't process it as URL
        if (isSearchQuery(input)) {
            return input;
        }

        // Remove any existing scheme
        input = input.replaceFirst("^(http://|https://)", "");

        // Add www. if it's not there and looks like a domain
        if (!input.startsWith("www.") && !containsWhitespace(input) && containsDot(input)) {
            input = "www." + input;
        }

        // Add https:// scheme
        return "https://" + input;
    }

    public static String getSearchUrl(Context context, String query) {
        return getSearchUrl(context, query, false);
    }

    public static String getSearchUrl(Context context, String query, boolean privateMode) {
        String searchEngineUrl = getSearchEngineUrl(context, privateMode);
        if (searchEngineUrl == null || searchEngineUrl.trim().isEmpty()) {
            searchEngineUrl = DEFAULT_SEARCH_ENGINE;
        }
        if (searchEngineUrl.contains("%s")) {
            return searchEngineUrl.replace("%s", Uri.encode(query));
        }
        return searchEngineUrl + Uri.encode(query);
    }

    // Build a search URL with a specific engine (for the "This time search in:" one-time
    // override), reusing the same %s / Uri.encode logic as getSearchUrl. Falls back to the
    // configured engine when no override is given.
    public static String getSearchUrlForEngine(Context context, String query, String engineBaseUrl) {
        if (engineBaseUrl == null || engineBaseUrl.trim().isEmpty()) {
            return getSearchUrl(context, query);
        }
        if (engineBaseUrl.contains("%s")) {
            return engineBaseUrl.replace("%s", Uri.encode(query));
        }
        return engineBaseUrl + Uri.encode(query);
    }

    public static String getSearchEngineUrl(Context context, boolean privateMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String standard = prefs.getString(SettingsKeys.PREF_SEARCH_ENGINE_URL, DEFAULT_SEARCH_ENGINE);
        if (standard == null || standard.trim().isEmpty()) {
            standard = DEFAULT_SEARCH_ENGINE;
        }
        if (!privateMode) {
            return standard;
        }
        String privateEngine = prefs.getString(SettingsKeys.PREF_PRIVATE_SEARCH_ENGINE_URL, standard);
        return privateEngine == null || privateEngine.trim().isEmpty() ? standard : privateEngine;
    }

    public static String getHomepageUrl(Context context) {
        String homepage = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsKeys.PREF_HOMEPAGE, DEFAULT_HOMEPAGE);
        if ("https://www.google.com".equals(homepage)) {
            homepage = DEFAULT_HOMEPAGE;
        }
        if (homepage == null || homepage.trim().isEmpty()) {
            return DEFAULT_HOMEPAGE;
        }
        return isSearchQuery(homepage) ? getSearchUrl(context, homepage) : sanitizeUrl(homepage);
    }

    public static String getNewTabPageUrl(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showQuickAccess = prefs.getBoolean(SettingsKeys.PREF_SHOW_QUICK_ACCESS, true);
        // Same switch as the native home screen (Settings → Display → New Tab Page), so toggling
        // Privacy Stats hides/shows the section on both surfaces together.
        boolean showPrivacyStats = prefs.getBoolean(SettingsKeys.PREF_SHOW_PRIVACY_STATS, true);
        boolean searchSuggestionsEnabled = prefs.getBoolean(
                SettingsKeys.PREF_SEARCH_SUGGESTIONS_ENABLED, true);
        String configuredSearchEngine = prefs.getString(
                SettingsKeys.PREF_SEARCH_ENGINE_URL, DEFAULT_SEARCH_ENGINE);
        String appName = escapeHtml(context.getString(R.string.app_name));
        String subtitle = escapeHtml(context.getString(R.string.home_chrome_style_subtitle));
        String hint = escapeHtml(context.getString(R.string.search_or_type_url));
        String quickAccess = escapeHtml(context.getString(R.string.quick_access));
        String quickAccessSummary = escapeHtml(context.getString(R.string.quick_access_home_summary));
        String privacyStatsTitle = escapeHtml(context.getString(R.string.privacy_stats));
        String pagesProtected = escapeHtml(context.getString(R.string.pages_protected));
        String itemsBlocked = escapeHtml(context.getString(R.string.items_blocked));
        String timeSaved = escapeHtml(context.getString(R.string.time_saved));
        String searchEngine = sanitizeSearchEngineForScript(getSearchUrl(context, ""));
        // Decorative search-pill icon: show the configured search engine's real favicon (matching
        // the native search bar), falling back to the original glyph if the icon fails to load.
        String engineIconUrl = getEngineIconUrl(configuredSearchEngine);
        String urlMarkContent = engineIconUrl != null
                ? "<img src='" + escapeHtml(engineIconUrl) + "' alt='' loading='lazy' "
                    + "onerror=\"this.style.display='none';this.nextElementSibling.style.display='block';\">"
                    + "<span class='urlMarkFallback' style='display:none'>&#8981;</span>"
                : "&#8981;";
        String suggestionEndpoint = sanitizeScriptUrl(
                getSuggestionUrl(configuredSearchEngine),
                "https://ac.duckduckgo.com/ac/?q=");
        String suggestionScript = buildSuggestionScript(searchSuggestionsEnabled, suggestionEndpoint);
        PrivacyStatsManager.Stats stats = PrivacyStatsManager.getStats(context);
        HomeBackgroundProvider.Photo backgroundPhoto = HomeBackgroundProvider.nextPhoto();
        List<QuickAccessItem> quickAccessItems = showQuickAccess
                ? new QuickAccessRepository(context).getMostVisitedItemsSnapshot(BROWSER_QUICK_ACCESS_RENDER_LIMIT, 260)
                : new ArrayList<>();
        int quickAccessTotalGapPx = BROWSER_QUICK_ACCESS_TILE_GAP_PX * (BROWSER_QUICK_ACCESS_VISIBLE_ITEMS - 1);
        String quickAccessTileWidth = "calc((100% - " + quickAccessTotalGapPx + "px)/"
                + BROWSER_QUICK_ACCESS_VISIBLE_ITEMS + ")";
        String background = cssColor(context, R.color.home_background);
        String surface = cssColor(context, R.color.home_panel_background);
        String accent = cssColor(context, R.color.home_accent_blue);
        String mint = cssColor(context, R.color.home_accent_mint);
        String warm = cssColor(context, R.color.home_accent_warm);
        String border = cssColor(context, R.color.home_search_stroke);
        String panelBorder = cssColor(context, R.color.home_panel_border);
        String edgeSearchBackground = cssColor(context, R.color.edge_search_background);
        String edgeSearchIconBackground = cssColor(context, R.color.edge_search_icon_background);
        String edgeSearchForeground = cssColor(context, R.color.edge_search_foreground);
        String edgeSearchHint = cssColor(context, R.color.edge_search_hint);
        String onImageText = cssColor(context, R.color.home_on_image_text);
        String onImageMuted = cssColor(context, R.color.home_on_image_muted);
        String glassBackground = cssColorWithAlpha(context, R.color.home_glass_background_strong, 0.68f);
        String statsGlassStart = cssColorWithAlpha(context, R.color.home_glass_background_strong, 0.88f);
        String statsGlassEnd = cssColorWithAlpha(context, R.color.home_glass_background_strong, 0.72f);
        String glassBorder = cssColor(context, R.color.home_glass_border);
        String quickAccessIconBackground = cssColorWithResourceAlpha(context,
                R.color.home_quick_access_glass_background);
        String quickAccessIconBorder = cssColorWithResourceAlpha(context,
                R.color.home_quick_access_glass_border);
        String shadow = cssColorWithAlpha(context, R.color.home_accent_ink, 0.08f);
        String searchShadow = cssColorWithAlpha(context, R.color.home_accent_ink, 0.18f);
        String backgroundImage = backgroundPhoto.getImageUrl();
        String backgroundImageAttribute = escapeHtml(backgroundImage);
        String photoCredit = escapeHtml(backgroundPhoto.getCreditText());
        String photoSource = escapeHtml(backgroundPhoto.getSourceUrl());
        String quickAccessTiles = showQuickAccess
                ? buildQuickAccessTilesHtml(quickAccessItems, quickAccessSummary)
                : "";

        String html = "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + appName + "</title>"
                + "<link rel='preconnect' href='https://images.unsplash.com' crossorigin>"
                + "<link rel='dns-prefetch' href='https://images.unsplash.com'>"
                + "<link rel='preload' as='image' href='" + backgroundImageAttribute + "' fetchpriority='high'>"
                + "<style>"
                + "*{box-sizing:border-box}html,body{min-height:100%;overflow-x:hidden;}body{margin:0;font-family:Roboto,Arial,sans-serif;background:" + background + ";color:" + onImageText + ";}"
                + "body:before{content:'';position:fixed;inset:0;background-image:url('" + backgroundImage + "');background-position:center;background-size:cover;background-repeat:no-repeat;z-index:-2;will-change:transform;transform:translateZ(0);}"
                + "body:after{content:'';position:fixed;inset:0;background:linear-gradient(180deg,rgba(0,0,0,.36) 0%,rgba(0,0,0,.16) 34%,rgba(0,0,0,.36) 64%,rgba(0,0,0,.72) 100%);z-index:-1;}"
                + ".wrap{position:relative;z-index:1;min-height:100vh;padding:34px 18px 228px;}"
                + ".shell{max-width:760px;margin:0 auto;}"
                + ".brand{display:flex;align-items:center;gap:12px;margin-top:8px;}"
                + ".mark{display:flex;align-items:center;justify-content:center;width:42px;height:42px;border-radius:14px;"
                + "background:" + glassBackground + ";color:" + mint + ";font-size:24px;font-weight:700;}"
                + ".logo{font-size:28px;font-weight:700;line-height:1.1;color:" + onImageText + ";opacity:.96;text-shadow:0 1px 2px rgba(0,0,0,.26);}"
                + ".sub{margin-top:3px;color:" + onImageMuted + ";font-size:13px;opacity:.92;text-shadow:0 1px 2px rgba(0,0,0,.22);}"
                + ".searchPanel{position:fixed;left:18px;right:18px;bottom:116px;bottom:calc(116px + env(safe-area-inset-bottom));z-index:10;max-width:760px;margin:0 auto;}"
                + "form{display:flex;align-items:center;height:54px;background:" + edgeSearchBackground + ";border:0;"
                + "border-radius:27px;padding:0 6px 0 7px;box-shadow:0 12px 28px " + searchShadow + ";}"
                + "form:focus-within{background:" + edgeSearchBackground + ";outline:1px solid " + border + ";}"
                + ".urlMark{display:flex;align-items:center;justify-content:center;width:42px;height:42px;flex:0 0 42px;"
                + "border-radius:20px;background:" + edgeSearchIconBackground + ";color:" + edgeSearchForeground + ";font-size:20px;font-weight:700;}"
                + ".urlMark img{width:22px;height:22px;border-radius:50%;object-fit:cover;}"
                + "input{flex:1;border:0;outline:0;background:transparent;color:" + edgeSearchForeground + ";"
                + "caret-color:" + edgeSearchForeground + ";font-size:15px;min-width:0;text-align:center;padding:0 8px;}"
                + "input:focus,input:not(:placeholder-shown){text-align:left;}"
                + "input::placeholder{color:" + edgeSearchHint + ";opacity:.95;}"
                + "button{border:0;background:transparent;color:" + edgeSearchForeground + ";font-size:23px;width:42px;height:42px;border-radius:21px;}"
                + "button:active{background:" + edgeSearchIconBackground + ";}"
                + "#suggestions{display:none;margin:8px 0 0;background:" + surface + ";border:1px solid " + border + ";"
                + "border-radius:18px;box-shadow:0 2px 7px " + shadow + ";overflow:hidden;}"
                + ".suggestion{padding:12px 18px;font-size:15px;border-bottom:1px solid " + panelBorder + ";}"
                + ".suggestion:last-child{border-bottom:0;}"
                + ".stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0;margin-top:18px;"
                + "background:linear-gradient(135deg," + statsGlassStart + "," + statsGlassEnd + ");border:1px solid " + glassBorder + ";border-radius:14px;overflow:hidden;"
                + "-webkit-backdrop-filter:blur(18px) saturate(120%);backdrop-filter:blur(18px) saturate(120%);box-shadow:inset 0 1px 0 rgba(255,255,255,.16),0 12px 30px rgba(0,0,0,.16);}"
                + ".stat{padding:13px 8px;text-align:center;min-width:0;}"
                + ".stat strong{display:block;font-size:22px;line-height:1.2;font-weight:700;}"
                + ".stat span{display:block;margin-top:5px;color:" + onImageMuted + ";font-size:12px;line-height:1.25;opacity:.92;}"
                + ".qa{margin-top:28px;}"
                + ".title{font-size:17px;font-weight:700;color:" + onImageText + ";opacity:.96;text-shadow:0 1px 2px rgba(0,0,0,.28);}"
                + ".summary,.empty{color:" + onImageMuted + ";font-size:14px;line-height:1.45;opacity:.92;}"
                + ".empty{margin-top:12px;padding:14px;background:" + glassBackground + ";border:1px solid " + glassBorder + ";border-radius:14px;}"
                + ".tiles{display:flex;flex-wrap:nowrap;gap:" + BROWSER_QUICK_ACCESS_TILE_GAP_PX + "px;margin:16px -4px 0;padding:0 4px 8px;overflow-x:auto;overflow-y:hidden;"
                + "overscroll-behavior-x:contain;scrollbar-width:none;}"
                + ".tiles::-webkit-scrollbar{display:none;}"
                + ".tile{display:block;flex:0 0 " + quickAccessTileWidth + ";width:" + quickAccessTileWidth + ";min-width:52px;text-align:center;text-decoration:none;color:" + onImageText + ";}"
                + ".tileIcon{display:flex;align-items:center;justify-content:center;width:52px;height:52px;margin:0 auto 8px;"
                + "border-radius:26px;background:" + quickAccessIconBackground + ";border:1px solid " + quickAccessIconBorder + ";overflow:hidden;}"
                + ".tileIcon img{display:block;width:32px;height:32px;min-width:32px;min-height:32px;object-fit:contain;}"
                + ".fallback{align-items:center;justify-content:center;width:100%;height:100%;font-size:20px;font-weight:700;color:" + accent + ";}"
                + ".tileTitle{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:" + onImageText + ";font-size:13px;line-height:1.2;text-shadow:0 1px 2px rgba(0,0,0,.38);}"
                + ".credit{position:fixed;left:18px;bottom:184px;bottom:calc(184px + env(safe-area-inset-bottom));z-index:9;display:inline-flex;max-width:calc(100% - 36px);"
                + "padding:6px 10px;border-radius:14px;background:" + glassBackground + ";border:1px solid " + glassBorder + ";color:" + onImageText + ";font-size:12px;text-decoration:none;text-shadow:0 1px 3px rgba(0,0,0,.55);"
                + "-webkit-backdrop-filter:blur(14px) saturate(115%);backdrop-filter:blur(14px) saturate(115%);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}"
                + "@media(max-width:360px){.wrap{padding-left:14px;padding-right:14px}.searchPanel{left:14px;right:14px;bottom:110px;bottom:calc(110px + env(safe-area-inset-bottom));}.credit{left:14px;bottom:176px;bottom:calc(176px + env(safe-area-inset-bottom));max-width:calc(100% - 28px);}.logo{font-size:26px}.tileTitle{font-size:12px}}"
                + "</style></head><body><main class='wrap'><div class='shell'>"
                + "<section class='brand'><div class='mark' aria-hidden='true'>&#10003;</div><div>"
                + "<div class='logo'>" + appName + "</div>"
                + "<div class='sub'>" + subtitle + "</div></div></section>"
                + "<section class='searchPanel'>"
                + "<form onsubmit=\"var q=document.getElementById('q').value.trim();"
                + "if(q){location.href='" + escapeJs(searchEngine) + "'+encodeURIComponent(q);}return false;\">"
                + "<div class='urlMark' aria-hidden='true'>" + urlMarkContent + "</div>"
                + "<input id='q' autocomplete='off' placeholder='" + hint + "'>"
                + "<button type='submit' aria-label='Search'>&#8594;</button>"
                + "</form>"
                + "<div id='suggestions'></div></section>"
                + (showPrivacyStats
                ? "<section class='stats' aria-label='" + privacyStatsTitle + "'>"
                + "<div class='stat'><strong style='color:" + mint + "'>" + stats.pagesProtected + "</strong><span>" + pagesProtected + "</span></div>"
                + "<div class='stat'><strong style='color:" + warm + "'>" + stats.itemsBlocked + "</strong><span>" + itemsBlocked + "</span></div>"
                + "<div class='stat'><strong style='color:" + accent + "'>" + formatTimeSavedForPage(stats.timeSavedSeconds) + "</strong><span>" + timeSaved + "</span></div>"
                + "</section>"
                : "")
                + (showQuickAccess
                ? "<section class='qa'><div class='title'>" + quickAccess + "</div>" + quickAccessTiles + "</section>"
                : "")
                + "<a class='credit' href='" + photoSource + "'>" + photoCredit + "</a>"
                + "<script>"
                + "var q=document.getElementById('q'),box=document.getElementById('suggestions'),timer=0;"
                + "function search(v){v=(v||'').trim();if(v){location.href='" + escapeJs(searchEngine) + "'+encodeURIComponent(v);}}"
                + "function show(items){box.innerHTML='';if(!items||!items.length){box.style.display='none';return;}"
                + "items.slice(0,6).forEach(function(s){var d=document.createElement('div');d.className='suggestion';"
                + "d.textContent=s;d.onclick=function(){search(s);};box.appendChild(d);});box.style.display='block';}"
                + suggestionScript
                + "</script>"
                + "</div></main></body></html>";
        if (html.length() > MAX_NEW_TAB_HTML_BYTES) {
            return DEFAULT_HOMEPAGE;
        }
        return "data:text/html;charset=utf-8," + Uri.encode(html);
    }

    private static String buildSuggestionScript(boolean enabled, String suggestionEndpoint) {
        if (!enabled) {
            return "q.addEventListener('input',function(){clearTimeout(timer);show([]);});";
        }
        String endpoint = escapeJs(suggestionEndpoint);
        return "var suggestionBase='" + endpoint + "';"
                + "function fallback(v){return [v,v+' news',v+' images',v+' wikipedia'];}"
                + "q.addEventListener('input',function(){clearTimeout(timer);var v=q.value.trim();"
                + "if(!v){show([]);return;}timer=setTimeout(function(){var u=suggestionBase+encodeURIComponent(v)"
                + "+(suggestionBase.indexOf('duckduckgo.com')>=0?'&type=list':'');"
                + "fetch(u).then(function(r){return r.json();}).then(function(j){var items=j&&j[1]?j[1]:[];"
                + "show(items.length?items:fallback(v));}).catch(function(){show(fallback(v));});},180);});";
    }

    private static String buildQuickAccessTilesHtml(List<QuickAccessItem> items, String emptySummary) {
        if (items == null || items.isEmpty()) {
            return "<div class='empty'>" + emptySummary + "</div>";
        }

        StringBuilder html = new StringBuilder("<div class='tiles'>");
        int rendered = 0;
        for (QuickAccessItem item : items) {
            if (item == null || isBlank(item.getUrl())) {
                continue;
            }
            String url = item.getUrl();
            String title = !isBlank(item.getTitle()) ? item.getTitle() : getQuickAccessTitle(url);
            if (isBlank(title)) {
                title = getDisplayHost(url);
            }
            if (isBlank(title)) {
                title = url;
            }
            String faviconUrl = getFaviconUrl(url);
            String initial = getFallbackInitial(title);

            html.append("<a class='tile' href='")
                    .append(escapeHtml(url))
                    .append("'><span class='tileIcon'>");
            if (!isBlank(faviconUrl)) {
                html.append("<img src='")
                        .append(escapeHtml(faviconUrl))
                        .append("' alt='' loading='lazy' onerror=\"this.style.display='none';this.nextElementSibling.style.display='flex';\">")
                        .append("<span class='fallback' style='display:none'>")
                        .append(initial)
                        .append("</span>");
            } else {
                html.append("<span class='fallback' style='display:flex'>")
                        .append(initial)
                        .append("</span>");
            }
            html.append("</span><span class='tileTitle'>")
                    .append(escapeHtml(title))
                    .append("</span></a>");
            rendered++;
            if (rendered >= BROWSER_QUICK_ACCESS_RENDER_LIMIT) {
                break;
            }
        }
        html.append("</div>");

        return rendered > 0 ? html.toString() : "<div class='empty'>" + emptySummary + "</div>";
    }

    private static String getFallbackInitial(String value) {
        if (isBlank(value)) {
            return "?";
        }
        int codePoint = value.trim().codePointAt(0);
        String initial = new String(Character.toChars(codePoint)).toUpperCase(Locale.US);
        return escapeHtml(initial);
    }

    private static String formatTimeSavedForPage(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        return (minutes / 60) + "h";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String cssColor(Context context, int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private static String cssColorWithAlpha(Context context, int colorRes, float alpha) {
        int color = ContextCompat.getColor(context, colorRes);
        float clampedAlpha = Math.max(0f, Math.min(1f, alpha));
        return "rgba(" + android.graphics.Color.red(color)
                + "," + android.graphics.Color.green(color)
                + "," + android.graphics.Color.blue(color)
                + "," + clampedAlpha + ")";
    }

    private static String cssColorWithResourceAlpha(Context context, int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        float alpha = android.graphics.Color.alpha(color) / 255f;
        return "rgba(" + android.graphics.Color.red(color)
                + "," + android.graphics.Color.green(color)
                + "," + android.graphics.Color.blue(color)
                + "," + alpha + ")";
    }

    /**
     * Validate the search-engine URL so it cannot break out of a JS string literal.
     * Falls back to the safe default if the user-configured value contains anything
     * outside a small whitelist of URL characters.
     */
    private static String sanitizeSearchEngineForScript(String value) {
        return sanitizeScriptUrl(value, DEFAULT_SEARCH_ENGINE);
    }

    private static String sanitizeScriptUrl(String value, String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        if (!value.startsWith("https://") && !value.startsWith("http://")) {
            return fallback;
        }
        if (value.length() > 512) {
            return fallback;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Allow only RFC3986-ish URL characters; reject controls, quotes,
            // backslashes, angle brackets, backticks, and Unicode line separators
            // (U+2028 / U+2029) that terminate JS string literals.
            if (c < 0x20 || c == 0x7F || c == '"' || c == '\'' || c == '\\'
                    || c == '<' || c == '>' || c == '`' || c == ' '
                    || c == 0x2028 || c == 0x2029) {
                return fallback;
            }
        }
        return value;
    }

    public static String getSearchEngineName(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            return "unknown";
        }
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) {
            return "unknown";
        }
        host = host.toLowerCase(Locale.US);
        if (host.contains("google.com")) return "Google";
        if (host.contains("bing.com")) return "Bing";
        if (host.contains("duckduckgo.com")) return "DuckDuckGo";
        if (host.contains("brave.com")) return "Brave";
        if (host.contains("yahoo.com")) return "Yahoo";
        if (host.contains("ecosia.org")) return "Ecosia";
        return host;
    }

    public static String getUrlOrSearchUrl(Context context, String input) {
        return getUrlOrSearchUrl(context, input, false);
    }

    public static String getUrlOrSearchUrl(Context context, String input, boolean privateMode) {
        if (input == null || input.trim().isEmpty()) {
            return getHomepageUrl(context);
        }
        input = input.trim();
        return isSearchQuery(input) ? getSearchUrl(context, input, privateMode) : sanitizeUrl(input);
    }

    public static boolean isSearchResultsUrl(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null) {
            return false;
        }
        host = host.toLowerCase();
        path = path != null ? path.toLowerCase() : "";

        String configuredSearchEngine = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsKeys.PREF_SEARCH_ENGINE_URL, DEFAULT_SEARCH_ENGINE);
        Uri searchUri = Uri.parse(configuredSearchEngine);
        String searchHost = searchUri.getHost();
        String searchPath = searchUri.getPath();
        if (searchHost != null && host.equals(searchHost.toLowerCase())) {
            if (searchPath == null || searchPath.isEmpty() || path.startsWith(searchPath.toLowerCase())) {
                return true;
            }
        }

        return (host.endsWith("google.com") && path.startsWith("/search"))
                || (host.endsWith("bing.com") && path.startsWith("/search"))
                || (host.endsWith("search.brave.com") && path.startsWith("/search"))
                || (host.endsWith("duckduckgo.com") && path.startsWith("/"))
                || (host.endsWith("search.yahoo.com") && path.startsWith("/search"));
    }

    public static String getSiteOrigin(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            return null;
        }
        return scheme.toLowerCase() + "://" + host.toLowerCase();
    }

    public static String getDisplayHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String host = Uri.parse(url).getHost();
        if (host == null || host.trim().isEmpty()) {
            return null;
        }
        return host.toLowerCase().startsWith("www.") ? host.substring(4) : host;
    }

    public static String getQuickAccessUrl(String url) {
        ParsedWebUrl parsedUrl = parseWebUrl(url);
        if (parsedUrl == null) {
            return null;
        }
        String host = getCanonicalSiteHost(parsedUrl.host);
        if (host == null) {
            return null;
        }
        return parsedUrl.scheme + "://" + host + parsedUrl.portSuffix;
    }

    public static String getQuickAccessTitle(String url) {
        ParsedWebUrl parsedUrl = parseWebUrl(url);
        if (parsedUrl == null) {
            return null;
        }
        return getCanonicalSiteHost(parsedUrl.host);
    }

    public static String getFaviconUrl(String url) {
        String quickAccessUrl = getQuickAccessUrl(url);
        if (quickAccessUrl == null) {
            return null;
        }
        return "https://www.google.com/s2/favicons?sz=128&domain_url="
                + encodeQueryParam(quickAccessUrl);
    }

    // Favicon for a search-engine URL, built from its host (reliable Google s2 "domain" form).
    public static String getEngineIconUrl(String engineUrl) {
        if (engineUrl == null || engineUrl.trim().isEmpty()) {
            return null;
        }
        String host;
        try {
            host = Uri.parse(engineUrl).getHost();
        } catch (Exception e) {
            return null;
        }
        if (host == null || host.isEmpty()) {
            return null;
        }
        return "https://www.google.com/s2/favicons?sz=64&domain=" + host;
    }

    public static String getDirectFaviconUrl(String url) {
        String quickAccessUrl = getQuickAccessUrl(url);
        return quickAccessUrl != null ? quickAccessUrl + "/favicon.ico" : null;
    }

    public static boolean isBlockedByAdBlock(String level, String url) {
        if (url == null || url.trim().isEmpty() || "off".equals(level)) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        String host = extractHost(url);
        String target = host != null ? host.toLowerCase() : lowerUrl;
        String[] balanced = cachedBalancedList != null ? cachedBalancedList : BALANCED_BLOCKED_HOST_PARTS;
        for (String part : balanced) {
            if (target.contains(part) || lowerUrl.contains(part)) {
                return true;
            }
        }
        if ("aggressive".equals(level)) {
            String[] aggressive = cachedAggressiveList != null ? cachedAggressiveList : AGGRESSIVE_BLOCKED_HOST_PARTS;
            for (String part : aggressive) {
                if (target.contains(part) || lowerUrl.contains(part)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getSuggestionUrl(String searchEngineBaseUrl) {
        if (searchEngineBaseUrl != null) {
            if (searchEngineBaseUrl.contains("google.com"))
                return "https://suggestqueries.google.com/complete/search?client=firefox&q=";
            if (searchEngineBaseUrl.contains("bing.com"))
                return "https://api.bing.com/osjson.aspx?query=";
            if (searchEngineBaseUrl.contains("brave.com"))
                return "https://search.brave.com/api/suggest?q=";
            if (searchEngineBaseUrl.contains("ecosia.org"))
                return "https://ac.ecosia.org/autocomplete?q=";
            if (searchEngineBaseUrl.contains("yahoo.com"))
                return "https://search.yahoo.com/sugg/gossip/gossip-global-os?command=";
        }
        return "https://ac.duckduckgo.com/ac/?q=";
    }

    // Pure-Java host extraction — avoids Android Uri for testability.
    private static String extractHost(String url) {
        String s = url;
        int schemeEnd = s.indexOf("://");
        if (schemeEnd >= 0) s = s.substring(schemeEnd + 3);
        int end = s.length();
        int i;
        if ((i = s.indexOf('/')) >= 0) end = Math.min(end, i);
        if ((i = s.indexOf('?')) >= 0) end = Math.min(end, i);
        if ((i = s.indexOf('#')) >= 0) end = Math.min(end, i);
        s = s.substring(0, end);
        return s.isEmpty() ? null : s;
    }

    private static ParsedWebUrl parseWebUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.US);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            int port = uri.getPort();
            String portSuffix = port >= 0 ? ":" + port : "";
            return new ParsedWebUrl(scheme, host, portSuffix);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String getCanonicalSiteHost(String rawHost) {
        if (rawHost == null || rawHost.trim().isEmpty()) {
            return null;
        }
        String host = rawHost.trim().toLowerCase(Locale.US);
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || host.indexOf(':') >= 0) {
            return host.isEmpty() ? null : host;
        }

        host = decodeTranslateHost(host);
        boolean changed;
        do {
            changed = false;
            String withoutPrefix = stripCommonSitePrefix(host);
            if (!host.equals(withoutPrefix)) {
                host = withoutPrefix;
                changed = true;
            }

            String firstLabel = firstLabel(host);
            if (labelCount(host) >= 3 && isLanguageHostLabel(firstLabel)) {
                host = host.substring(firstLabel.length() + 1);
                changed = true;
            }
        } while (changed);
        return host.isEmpty() ? null : host;
    }

    private static String decodeTranslateHost(String host) {
        final String suffix = ".translate.goog";
        if (!host.endsWith(suffix)) {
            return host;
        }
        String encoded = host.substring(0, host.length() - suffix.length());
        if (encoded.isEmpty()) {
            return host;
        }
        String decoded = encoded
                .replace("--", "{hyphen}")
                .replace('-', '.')
                .replace("{hyphen}", "-");
        return decoded.contains(".") ? decoded : host;
    }

    private static String stripCommonSitePrefix(String host) {
        for (String prefix : COMMON_SITE_PREFIXES) {
            String dottedPrefix = prefix + ".";
            if (host.startsWith(dottedPrefix) && host.length() > dottedPrefix.length()) {
                return host.substring(dottedPrefix.length());
            }
        }
        return host;
    }

    private static String firstLabel(String host) {
        int dot = host.indexOf('.');
        return dot >= 0 ? host.substring(0, dot) : host;
    }

    private static int labelCount(String host) {
        int count = 1;
        for (int i = 0; i < host.length(); i++) {
            if (host.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

    private static boolean isLanguageHostLabel(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        String language = label.toLowerCase(Locale.US);
        int regionSeparator = language.indexOf('-');
        if (regionSeparator > 0) {
            language = language.substring(0, regionSeparator);
        }
        for (String languageLabel : LANGUAGE_HOST_LABELS) {
            if (languageLabel.equals(language)) {
                return true;
            }
        }
        return false;
    }

    private static String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    public static boolean isInternalPageUrl(String url) {
        if (url == null) {
            return false;
        }
        String value = url.trim();
        return value.regionMatches(true, 0, "data:text/html", 0, "data:text/html".length());
    }

    public static boolean isSearchQuery(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        input = input.trim();

        // If it contains spaces, it's definitely a search query
        if (input.contains(" ")) {
            return true;
        }

        // If it's already a valid URL with scheme, it's not a search query
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return false;
        }

        // If it has no dots and no scheme, treat it as a search query
        if (!input.contains(".")) {
            return true;
        }

        // If it looks like a domain (contains dots and no spaces), it's not a search query
        return !Patterns.WEB_URL.matcher("https://" + input).matches();
    }

    private static boolean containsWhitespace(String str) {
        return str.contains(" ");
    }

    private static boolean containsDot(String str) {
        return str.contains(".");
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0x2028) { out.append("\\u2028"); continue; }
            if (c == 0x2029) { out.append("\\u2029"); continue; }
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '\'': out.append("\\'"); break;
                case '"': out.append("\\\""); break;
                case '<': out.append("\\u003c"); break;
                case '>': out.append("\\u003e"); break;
                case '&': out.append("\\u0026"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static final class ParsedWebUrl {
        final String scheme;
        final String host;
        final String portSuffix;

        ParsedWebUrl(String scheme, String host, String portSuffix) {
            this.scheme = scheme;
            this.host = host;
            this.portSuffix = portSuffix;
        }
    }
}
