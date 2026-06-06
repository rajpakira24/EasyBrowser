package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebRequestError;

import java.util.List;

class BrowserNavigationDelegate implements GeckoSession.NavigationDelegate {

    private final BrowserActivity activity;

    BrowserNavigationDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onLocationChange(GeckoSession session, String url,
                                 List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                                 Boolean hasUserGesture) {
        activity.runOnUiThread(() -> {
            if (url != null && !url.equals("about:blank")) {
                activity.updateUrlInputForUrl(url);
                activity.currentUrl = url;
                activity.updateBookmarkStatus();
                applyPerSiteZoom(url);
                Tab tab = activity.tabManager != null ? activity.tabManager.getCurrentTab() : null;
                if (tab != null && tab.getSession() == session) {
                    activity.tabManager.updateTabUrl(tab, url);
                }
                // F14: Inject user CSS style if configured for this host
                activity.injectUserStyleIfNeeded(session, url);
            }
        });
    }

    private void applyPerSiteZoom(String url) {
        String host = UrlUtils.getDisplayHost(url);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        float defaultFactor = prefs.getInt("text_size_percent", 100) / 100f;
        float factor = host != null
                ? prefs.getFloat("zoom_" + host, defaultFactor)
                : defaultFactor;
        try {
            RuntimeManager.getRuntime(activity).getSettings().setFontSizeFactor(factor);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCanGoBack(GeckoSession session, boolean canGoBack) {
        Tab tab = activity.tabManager != null ? activity.tabManager.findTabBySession(session) : null;
        if (tab != null) {
            tab.setCanGoBack(canGoBack);
        }
        if (activity.session != session) {
            return;
        }
        activity.canGoBack = canGoBack;
        activity.invalidateOptionsMenu();
        activity.runOnUiThread(() -> {
            if (activity.backButton != null) {
                activity.backButton.setVisibility(canGoBack ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        });
    }

    @Override
    public void onCanGoForward(GeckoSession session, boolean canGoForward) {
        Tab tab = activity.tabManager != null ? activity.tabManager.findTabBySession(session) : null;
        if (tab != null) {
            tab.setCanGoForward(canGoForward);
        }
        if (activity.session != session) {
            return;
        }
        activity.canGoForward = canGoForward;
        activity.invalidateOptionsMenu();
    }

    @Override
    public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession session,
                                                  GeckoSession.NavigationDelegate.LoadRequest request) {
        if (shouldOpenExternally(request.uri)) {
            openExternalUri(request.uri);
            return GeckoResult.deny();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("https_only", true)
                && request.uri != null
                && request.uri.startsWith("http://")
                && !isLocalUri(request.uri)) {
            String upgraded = "https://" + request.uri.substring("http://".length());
            activity.runOnUiThread(() -> activity.loadUrl(upgraded));
            return GeckoResult.deny();
        }
        // Block top-level navigation to data:/javascript: URIs while HTTPS-only is on —
        // they can escape the policy by carrying their own content. Exempt the in-app
        // new-tab page which is the only data: URI we generate.
        if (prefs.getBoolean("https_only", true)
                && request.uri != null
                && isTopLevelNavigation(request)
                && (request.uri.startsWith("data:") || request.uri.startsWith("javascript:"))
                && !UrlUtils.isInternalPageUrl(request.uri)) {
            return GeckoResult.deny();
        }
        if (UrlUtils.isBlockedByAdBlock(
                prefs.getString("ad_blocking_level", "balanced"), request.uri)) {
            activity.recordBlockedPrivacyItem();
            if (isTopLevelNavigation(request)) {
                loadErrorPage(session, request.uri,
                        new ErrorPage("Website Blocked",
                                "Easy Browser blocked this site because it matched your privacy or blocking settings.",
                                "!", false));
            }
            return GeckoResult.deny();
        }
        boolean isNewWindow = request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW;
        if (isNewWindow && prefs.getBoolean("block_popups", true) && !request.hasUserGesture) {
            activity.recordBlockedPrivacyItem();
            activity.runOnUiThread(() ->
                    Toast.makeText(activity, R.string.popup_blocked, Toast.LENGTH_SHORT).show());
            return GeckoResult.deny();
        }
        if (isNewWindow) {
            activity.openUrlInNewTab(request.uri, false, true);
            return GeckoResult.deny();
        }
        return GeckoResult.allow();
    }

    private boolean isTopLevelNavigation(GeckoSession.NavigationDelegate.LoadRequest request) {
        return request != null
                && (request.isDirectNavigation
                || request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_CURRENT
                || request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW);
    }

    @Override
    public GeckoResult<String> onLoadError(GeckoSession session, String uri, WebRequestError error) {
        ErrorPage errorPage = createErrorPage(error);
        return GeckoResult.fromValue(buildErrorPageHtml(uri, errorPage));
    }

    private void loadErrorPage(GeckoSession session, String uri, ErrorPage errorPage) {
        if (session == null) {
            return;
        }
        String html = buildErrorPageHtml(uri, errorPage);
        String dataUrl = "data:text/html;charset=utf-8," + Uri.encode(html);
        activity.runOnUiThread(() -> session.loadUri(dataUrl));
    }

    private String buildErrorPageHtml(String uri, ErrorPage errorPage) {
        String safeUri = uri != null ? android.text.TextUtils.htmlEncode(uri) : "";
        String background = cssColor(R.color.backgroundColor);
        String surface = cssColor(R.color.colorSurface);
        String primary = cssColor(R.color.colorPrimary);
        String primaryText = cssColor(R.color.colorOnPrimary);
        String text = cssColor(R.color.colorOnBackground);
        String muted = cssColor(R.color.gray);
        String iconBackground = cssColor(R.color.search_bar_background);
        String iconText = cssColor(R.color.error);
        String shadow = cssColorWithAlpha(R.color.colorOnBackground, 0.12f);
        String retryLink = errorPage.showRetry && !safeUri.isEmpty()
                ? "<a href=\"" + safeUri + "\" style=\"display:inline-block;margin-top:20px;"
                  + "padding:10px 28px;background:" + primary + ";color:" + primaryText + ";border-radius:4px;"
                  + "text-decoration:none;font-size:15px;font-weight:500\">Retry</a>"
                : "";
        String safeTitle = android.text.TextUtils.htmlEncode(errorPage.title);
        String safeMessage = android.text.TextUtils.htmlEncode(errorPage.message);
        String safeIconText = android.text.TextUtils.htmlEncode(errorPage.iconText);
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{font-family:sans-serif;margin:0;background:" + background + ";"
                + "display:flex;align-items:center;justify-content:center;min-height:100vh}"
                + ".card{background:" + surface + ";border-radius:12px;padding:36px 28px;"
                + "max-width:440px;width:90%;text-align:center;"
                + "box-shadow:0 1px 6px " + shadow + "}"
                + ".icon{display:inline-flex;align-items:center;justify-content:center;"
                + "width:56px;height:56px;border-radius:50%;background:" + iconBackground + ";"
                + "color:" + iconText + ";font-size:0;font-weight:700;margin-bottom:4px}"
                + ".icon:before{content:'!';font-size:28px}"
                + "h2{margin:12px 0 8px;color:" + text + ";font-size:20px;font-weight:600}"
                + "p{color:" + muted + ";font-size:14px;line-height:1.5;margin:0}"
                + "</style></head>"
                + "<body><div class='card'>"
                + "<div class='icon'>⚠️</div>"
                + "<h2>" + safeTitle + "</h2>"
                + "<p>" + safeMessage + "</p>"
                + retryLink
                + "</div></body></html>";
        return html;
    }

    private String cssColor(int colorRes) {
        int color = ContextCompat.getColor(activity, colorRes);
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private String cssColorWithAlpha(int colorRes, float alpha) {
        int color = ContextCompat.getColor(activity, colorRes);
        float clampedAlpha = Math.max(0f, Math.min(1f, alpha));
        return "rgba(" + android.graphics.Color.red(color)
                + "," + android.graphics.Color.green(color)
                + "," + android.graphics.Color.blue(color)
                + "," + clampedAlpha + ")";
    }

    private ErrorPage createErrorPage(WebRequestError error) {
        switch (error.code) {
            case WebRequestError.ERROR_OFFLINE:
                return new ErrorPage("No Internet Connection",
                        "Check your network connection and tap Retry.", "!", true);
            case WebRequestError.ERROR_NET_TIMEOUT:
                return new ErrorPage("Connection Timed Out",
                        "The server took too long to respond. Try again in a moment.", "!", true);
            case WebRequestError.ERROR_UNKNOWN_HOST:
                return new ErrorPage("Server Not Found",
                        "This link may be broken, typed incorrectly, or no longer available.", "?", true);
            case WebRequestError.ERROR_NET_INTERRUPT:
            case WebRequestError.ERROR_NET_RESET:
                return new ErrorPage("Connection Interrupted",
                        "The connection stopped while the page was loading.", "!", true);
            case WebRequestError.ERROR_CONNECTION_REFUSED:
                return new ErrorPage("Website Refused to Connect",
                        "The site is reachable, but it refused the connection. It may be down, private, or blocked from your network.", "!", true);
            case WebRequestError.ERROR_REDIRECT_LOOP:
                return new ErrorPage("Page Redirected Too Many Times",
                        "This page is stuck in a redirect loop. Clearing cookies for this site may help.", "!", true);
            case WebRequestError.ERROR_PORT_BLOCKED:
                return new ErrorPage("Connection Blocked",
                        "This address uses a port that the browser or device blocks for safety.", "!", false);
            case WebRequestError.ERROR_HTTPS_ONLY:
                return new ErrorPage("Secure Connection Required",
                        "This site could not be opened with HTTPS-only mode enabled.", "!", true);
            case WebRequestError.ERROR_BAD_HSTS_CERT:
                return new ErrorPage("Website Blocked for Your Safety",
                        "This site has a certificate problem and does not allow security exceptions.", "!", false);
            case WebRequestError.ERROR_SECURITY_SSL:
            case WebRequestError.ERROR_SECURITY_BAD_CERT:
                return new ErrorPage("Connection Not Secure",
                        "There was a security issue with this page's certificate.", "!", true);
            case WebRequestError.ERROR_MALFORMED_URI:
                return new ErrorPage("Invalid Link",
                        "This address is not valid. Check the link and try again.", "?", false);
            case WebRequestError.ERROR_UNKNOWN_PROTOCOL:
                return new ErrorPage("Unsupported Link",
                        "Easy Browser cannot open this type of link.", "?", false);
            case WebRequestError.ERROR_FILE_NOT_FOUND:
                return new ErrorPage("File Not Found",
                        "The requested local file does not exist or was moved.", "?", false);
            case WebRequestError.ERROR_FILE_ACCESS_DENIED:
                return new ErrorPage("Access Denied",
                        "Easy Browser does not have permission to open this local file.", "!", false);
            case WebRequestError.ERROR_DATA_URI_TOO_LONG:
                return new ErrorPage("Link Is Too Large",
                        "This data link is too large to open as a page.", "?", false);
            case WebRequestError.ERROR_UNKNOWN_PROXY_HOST:
                return new ErrorPage("Proxy Server Not Found",
                        "The configured proxy server address could not be found.", "!", true);
            case WebRequestError.ERROR_PROXY_CONNECTION_REFUSED:
                return new ErrorPage("Proxy Refused Connection",
                        "The configured proxy server refused the connection.", "!", true);
            case WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI:
                return new ErrorPage("Deceptive Site Blocked",
                        "This page was reported as a phishing site and was blocked for your safety.", "!", false);
            case WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI:
                return new ErrorPage("Malware Site Blocked",
                        "This page was reported for harmful software and was blocked for your safety.", "!", false);
            case WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI:
                return new ErrorPage("Unwanted Software Blocked",
                        "This page may try to install unwanted software and was blocked for your safety.", "!", false);
            case WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI:
                return new ErrorPage("Harmful Site Blocked",
                        "This page may harm your device or data and was blocked for your safety.", "!", false);
            case WebRequestError.ERROR_CONTENT_CRASHED:
                return new ErrorPage("Page Crashed",
                        "The page stopped working while loading. Tap Retry to load it again.", "!", true);
            case WebRequestError.ERROR_UNSAFE_CONTENT_TYPE:
                return new ErrorPage("Unsafe Content Blocked",
                        "This page returned content that the browser blocked for safety.", "!", false);
            case WebRequestError.ERROR_CORRUPTED_CONTENT:
                return new ErrorPage("Broken Page Content",
                        "This page sent damaged or incomplete data.", "!", true);
            case WebRequestError.ERROR_INVALID_CONTENT_ENCODING:
                return new ErrorPage("Page Encoding Error",
                        "This page used an invalid compression or encoding format.", "!", true);
            default:
                return createFallbackErrorPage(error);
        }
    }

    private ErrorPage createFallbackErrorPage(WebRequestError error) {
        switch (error.category) {
            case WebRequestError.ERROR_CATEGORY_NETWORK:
                return new ErrorPage("Connection Error",
                        "A network error occurred. Check your internet connection and try again.", "!", true);
            case WebRequestError.ERROR_CATEGORY_SECURITY:
                return new ErrorPage("Connection Not Secure",
                        "There was a security issue with this page.", "!", true);
            case WebRequestError.ERROR_CATEGORY_URI:
                return new ErrorPage("Invalid Address",
                        "The address is not valid or cannot be found.", "?", false);
            case WebRequestError.ERROR_CATEGORY_PROXY:
                return new ErrorPage("Proxy Error",
                        "The proxy connection failed. Check your network or proxy settings.", "!", true);
            case WebRequestError.ERROR_CATEGORY_SAFEBROWSING:
                return new ErrorPage("Unsafe Site Blocked",
                        "This page was blocked for your safety.", "!", false);
            case WebRequestError.ERROR_CATEGORY_CONTENT:
                return new ErrorPage("Page Content Error",
                        "This page could not be displayed because its content is invalid or damaged.", "!", true);
            default:
                return new ErrorPage("Page Could Not Load",
                        error.getMessage() != null ? error.getMessage() : "An unknown error occurred.",
                        "!", true);
        }
    }

    private static class ErrorPage {
        final String title;
        final String message;
        final String iconText;
        final boolean showRetry;

        ErrorPage(String title, String message, String iconText, boolean showRetry) {
            this.title = title;
            this.message = message;
            this.iconText = iconText;
            this.showRetry = showRetry;
        }
    }

    private boolean isLocalUri(String uri) {
        if (uri == null) return false;
        String host = Uri.parse(uri).getHost();
        if (host == null) return false;
        host = host.toLowerCase();
        return host.equals("localhost")
                || host.equals("[::1]")
                || host.startsWith("127.")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || (host.startsWith("172.")
                        && host.length() >= 6
                        && isInRange172(host));
    }

    private boolean isInRange172(String host) {
        // 172.16.0.0 - 172.31.255.255
        try {
            int secondOctet = Integer.parseInt(host.substring(4, host.indexOf('.', 4)));
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldOpenExternally(String uri) {
        if (uri == null) return false;
        Uri parsed = Uri.parse(uri);
        String scheme = parsed.getScheme();
        return scheme != null
                && !"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)
                && !"about".equalsIgnoreCase(scheme)
                && !"data".equalsIgnoreCase(scheme)
                && !"blob".equalsIgnoreCase(scheme)
                && !"file".equalsIgnoreCase(scheme);
    }

    private void openExternalUri(String uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            // Show the chooser so the URI is never delivered silently to a single
            // intent-resolver that happened to match — the user picks the handler.
            Intent chooser = Intent.createChooser(intent,
                    activity.getString(R.string.share_link));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(chooser);
        } catch (Exception e) {
            activity.runOnUiThread(() ->
                    Toast.makeText(activity, R.string.error_no_app_for_link, Toast.LENGTH_SHORT).show());
        }
    }
}
