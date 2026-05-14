package com.webstudio.easybrowser.utils;

import android.content.Context;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import android.util.Patterns;
import com.webstudio.easybrowser.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
        String searchEngineUrl = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("search_engine_url", DEFAULT_SEARCH_ENGINE);
        if (searchEngineUrl == null || searchEngineUrl.trim().isEmpty()) {
            searchEngineUrl = DEFAULT_SEARCH_ENGINE;
        }
        if (searchEngineUrl.contains("%s")) {
            return searchEngineUrl.replace("%s", Uri.encode(query));
        }
        return searchEngineUrl + Uri.encode(query);
    }

    public static String getHomepageUrl(Context context) {
        String homepage = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("homepage", DEFAULT_HOMEPAGE);
        if ("https://www.google.com".equals(homepage)) {
            homepage = DEFAULT_HOMEPAGE;
        }
        if (homepage == null || homepage.trim().isEmpty()) {
            return DEFAULT_HOMEPAGE;
        }
        return isSearchQuery(homepage) ? getSearchUrl(context, homepage) : sanitizeUrl(homepage);
    }

    public static String getNewTabPageUrl(Context context) {
        String appName = escapeHtml(context.getString(R.string.app_name));
        String subtitle = escapeHtml(context.getString(R.string.home_chrome_style_subtitle));
        String hint = escapeHtml(context.getString(R.string.search_or_type_url));
        String quickAccess = escapeHtml(context.getString(R.string.quick_access));
        String quickAccessSummary = escapeHtml(context.getString(R.string.quick_access_home_summary));
        String searchEngine = sanitizeSearchEngineForScript(getSearchUrl(context, ""));

        String html = "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + appName + "</title>"
                + "<style>"
                + "body{margin:0;font-family:Roboto,Arial,sans-serif;background:#f8fafd;color:#202124;}"
                + ".wrap{box-sizing:border-box;min-height:100vh;padding:52px 18px 32px;}"
                + ".logo{text-align:center;font-size:34px;font-weight:700;color:#1a73e8;}"
                + ".sub{text-align:center;margin-top:4px;color:#757575;font-size:13px;}"
                + "form{display:flex;align-items:center;height:56px;margin:26px auto 0;max-width:720px;"
                + "background:#fff;border:1px solid #dadce0;border-radius:28px;box-shadow:0 2px 7px #0001;}"
                + ".searchIcon{width:52px;text-align:center;color:#757575;font-size:20px;}"
                + "input{flex:1;border:0;outline:0;background:transparent;font-size:16px;min-width:0;}"
                + "button{border:0;background:transparent;color:#1a73e8;font-size:24px;width:52px;height:52px;}"
                + "#suggestions{display:none;max-width:720px;margin:8px auto 0;background:#fff;border:1px solid #dadce0;"
                + "border-radius:18px;box-shadow:0 2px 7px #0001;overflow:hidden;}"
                + ".suggestion{padding:12px 18px;font-size:15px;border-bottom:1px solid #f1f3f4;}"
                + ".suggestion:last-child{border-bottom:0;}"
                + ".section{max-width:720px;margin:28px auto 0;}"
                + ".title{font-size:17px;font-weight:700;margin-bottom:8px;}"
                + ".summary{color:#757575;font-size:14px;line-height:1.45;}"
                + "</style></head><body><main class='wrap'>"
                + "<div class='logo'>" + appName + "</div>"
                + "<div class='sub'>" + subtitle + "</div>"
                + "<form onsubmit=\"var q=document.getElementById('q').value.trim();"
                + "if(q){location.href='" + escapeJs(searchEngine) + "'+encodeURIComponent(q);}return false;\">"
                + "<div class='searchIcon'>&#128269;</div>"
                + "<input id='q' autocomplete='off' autofocus placeholder='" + hint + "'>"
                + "<button type='submit' aria-label='Search'>&#8250;</button>"
                + "</form>"
                + "<div id='suggestions'></div>"
                + "<script>"
                + "var q=document.getElementById('q'),box=document.getElementById('suggestions'),timer=0;"
                + "function search(v){v=(v||'').trim();if(v){location.href='" + escapeJs(searchEngine) + "'+encodeURIComponent(v);}}"
                + "function fallback(v){return [v,v+' news',v+' images',v+' wikipedia'];}"
                + "function show(items){box.innerHTML='';if(!items||!items.length){box.style.display='none';return;}"
                + "items.slice(0,6).forEach(function(s){var d=document.createElement('div');d.className='suggestion';"
                + "d.textContent=s;d.onclick=function(){search(s);};box.appendChild(d);});box.style.display='block';}"
                + "q.addEventListener('input',function(){clearTimeout(timer);var v=q.value.trim();"
                + "if(!v){show([]);return;}timer=setTimeout(function(){fetch('https://ac.duckduckgo.com/ac/?q='+encodeURIComponent(v)+'&type=list')"
                + ".then(function(r){return r.json();}).then(function(j){var items=j&&j[1]?j[1]:[];show(items.length?items:fallback(v));})"
                + ".catch(function(){show(fallback(v));});},180);});"
                + "</script>"
                + "<section class='section'><div class='title'>" + quickAccess + "</div>"
                + "<div class='summary'>" + quickAccessSummary + "</div></section>"
                + "</main></body></html>";
        if (html.length() > MAX_NEW_TAB_HTML_BYTES) {
            return DEFAULT_HOMEPAGE;
        }
        return "data:text/html;charset=utf-8," + Uri.encode(html);
    }

    /**
     * Validate the search-engine URL so it cannot break out of a JS string literal.
     * Falls back to the safe default if the user-configured value contains anything
     * outside a small whitelist of URL characters.
     */
    private static String sanitizeSearchEngineForScript(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_SEARCH_ENGINE;
        }
        if (!value.startsWith("https://") && !value.startsWith("http://")) {
            return DEFAULT_SEARCH_ENGINE;
        }
        if (value.length() > 512) {
            return DEFAULT_SEARCH_ENGINE;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Allow only RFC3986-ish URL characters; reject controls, quotes,
            // backslashes, angle brackets, backticks, and Unicode line separators
            // (U+2028 / U+2029) that terminate JS string literals.
            if (c < 0x20 || c == 0x7F || c == '"' || c == '\'' || c == '\\'
                    || c == '<' || c == '>' || c == '`' || c == ' '
                    || c == 0x2028 || c == 0x2029) {
                return DEFAULT_SEARCH_ENGINE;
            }
        }
        return value;
    }

    public static String getUrlOrSearchUrl(Context context, String input) {
        if (input == null || input.trim().isEmpty()) {
            return getHomepageUrl(context);
        }
        input = input.trim();
        return isSearchQuery(input) ? getSearchUrl(context, input) : sanitizeUrl(input);
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
                .getString("search_engine_url", DEFAULT_SEARCH_ENGINE);
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

    public static boolean isInternalPageUrl(String url) {
        return url != null && url.startsWith("data:text/html");
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
}
