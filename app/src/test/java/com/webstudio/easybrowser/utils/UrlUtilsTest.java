package com.webstudio.easybrowser.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class UrlUtilsTest {

    // ── sanitizeUrl ───────────────────────────────────────────────────────────

    @Test
    public void sanitizeUrl_null_returnsEmpty() {
        assertEquals("", UrlUtils.sanitizeUrl(null));
    }

    @Test
    public void sanitizeUrl_blank_returnsEmpty() {
        assertEquals("", UrlUtils.sanitizeUrl("   "));
    }

    @Test
    public void sanitizeUrl_aboutBlank_unchanged() {
        assertEquals("about:blank", UrlUtils.sanitizeUrl("about:blank"));
    }

    @Test
    public void sanitizeUrl_httpsUrl_unchanged() {
        assertEquals("https://example.com", UrlUtils.sanitizeUrl("https://example.com"));
    }

    @Test
    public void sanitizeUrl_httpUrl_unchanged() {
        assertEquals("http://example.com/path", UrlUtils.sanitizeUrl("http://example.com/path"));
    }

    @Test
    public void sanitizeUrl_spaceQuery_returnedAsIs() {
        // Spaces → isSearchQuery short-circuits to true; sanitizeUrl returns input unchanged.
        assertEquals("hello world", UrlUtils.sanitizeUrl("hello world"));
    }

    @Test
    public void sanitizeUrl_noDots_returnedAsIs() {
        // No dots → isSearchQuery short-circuits to true (treated as keyword).
        assertEquals("localhost", UrlUtils.sanitizeUrl("localhost"));
    }

    // ── isSearchQuery ─────────────────────────────────────────────────────────

    @Test
    public void isSearchQuery_withSpace_true() {
        assertTrue(UrlUtils.isSearchQuery("hello world"));
    }

    @Test
    public void isSearchQuery_multiWord_true() {
        assertTrue(UrlUtils.isSearchQuery("android studio tips"));
    }

    @Test
    public void isSearchQuery_httpsUrl_false() {
        assertFalse(UrlUtils.isSearchQuery("https://example.com"));
    }

    @Test
    public void isSearchQuery_httpUrl_false() {
        assertFalse(UrlUtils.isSearchQuery("http://example.com/path?q=1"));
    }

    @Test
    public void isSearchQuery_noDots_true() {
        assertTrue(UrlUtils.isSearchQuery("googl"));
    }

    @Test
    public void isSearchQuery_null_false() {
        assertFalse(UrlUtils.isSearchQuery(null));
    }

    @Test
    public void isSearchQuery_empty_false() {
        assertFalse(UrlUtils.isSearchQuery(""));
    }

    // ── isBlockedByAdBlock ────────────────────────────────────────────────────

    @Test
    public void isBlockedByAdBlock_offLevel_neverBlocks() {
        assertFalse(UrlUtils.isBlockedByAdBlock("off", "https://doubleclick.net/ad"));
        assertFalse(UrlUtils.isBlockedByAdBlock("off", "https://ads.example.com"));
    }

    @Test
    public void isBlockedByAdBlock_nullUrl_false() {
        assertFalse(UrlUtils.isBlockedByAdBlock("balanced", null));
    }

    @Test
    public void isBlockedByAdBlock_emptyUrl_false() {
        assertFalse(UrlUtils.isBlockedByAdBlock("balanced", ""));
    }

    @Test
    public void isBlockedByAdBlock_balanced_blocksDoubleclick() {
        assertTrue(UrlUtils.isBlockedByAdBlock("balanced", "https://doubleclick.net/ad"));
    }

    @Test
    public void isBlockedByAdBlock_balanced_blocksGoogleSyndication() {
        assertTrue(UrlUtils.isBlockedByAdBlock("balanced",
                "https://www.googlesyndication.com/tag/js/gpt.js"));
    }

    @Test
    public void isBlockedByAdBlock_balanced_doesNotBlockAdsSubdomain() {
        // "ads." is an aggressive-only pattern, not in the balanced list.
        assertFalse(UrlUtils.isBlockedByAdBlock("balanced", "https://ads.example.com/banner"));
    }

    @Test
    public void isBlockedByAdBlock_aggressive_blocksAdsSubdomain() {
        assertTrue(UrlUtils.isBlockedByAdBlock("aggressive", "https://ads.example.com/banner"));
    }

    @Test
    public void isBlockedByAdBlock_aggressive_blocksTrackerSubdomain() {
        assertTrue(UrlUtils.isBlockedByAdBlock("aggressive",
                "https://tracker.example.com/pixel.gif"));
    }

    @Test
    public void isBlockedByAdBlock_aggressive_alsoPassesBalancedPatterns() {
        assertTrue(UrlUtils.isBlockedByAdBlock("aggressive", "https://doubleclick.net/ad"));
    }

    // ── getSuggestionUrl ──────────────────────────────────────────────────────

    @Test
    public void getSuggestionUrl_google_returnsGoogleEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://www.google.com/search?q=");
        assertTrue(url.contains("suggestqueries.google.com"));
    }

    @Test
    public void getSuggestionUrl_bing_returnsBingEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://www.bing.com/search?q=");
        assertTrue(url.contains("bing.com"));
    }

    @Test
    public void getSuggestionUrl_brave_returnsBraveEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://search.brave.com/search?q=");
        assertTrue(url.contains("brave.com"));
    }

    @Test
    public void getSuggestionUrl_ecosia_returnsEcosiaEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://www.ecosia.org/search?q=");
        assertTrue(url.contains("ecosia.org"));
    }

    @Test
    public void getSuggestionUrl_yahoo_returnsYahooEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://search.yahoo.com/search?p=");
        assertTrue(url.contains("yahoo.com"));
    }

    @Test
    public void getSuggestionUrl_startpage_fallsBackToDdg() {
        // Startpage has no public autocomplete API; DDG is the fallback.
        String url = UrlUtils.getSuggestionUrl("https://www.startpage.com/sp/search?q=");
        assertTrue(url.contains("duckduckgo.com"));
    }

    @Test
    public void getSuggestionUrl_ddg_returnsDdgEndpoint() {
        String url = UrlUtils.getSuggestionUrl("https://duckduckgo.com/?q=");
        assertTrue(url.contains("duckduckgo.com"));
    }

    @Test
    public void getSuggestionUrl_null_returnsDdg() {
        String url = UrlUtils.getSuggestionUrl(null);
        assertTrue(url.contains("duckduckgo.com"));
    }

    @Test
    public void getSuggestionUrl_unknown_returnsDdg() {
        String url = UrlUtils.getSuggestionUrl("https://some-unknown-engine.com/search?q=");
        assertTrue(url.contains("duckduckgo.com"));
    }
}
