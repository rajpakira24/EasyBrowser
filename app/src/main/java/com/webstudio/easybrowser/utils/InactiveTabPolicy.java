package com.webstudio.easybrowser.utils;

import com.webstudio.easybrowser.models.Tab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for deciding which tabs are "inactive" — aged out or a
 * surplus duplicate. Shared by the tab switcher (active/inactive split) and the
 * inactive-tabs screen so the two views can never disagree about a tab.
 */
public final class InactiveTabPolicy {
    public static final int DEFAULT_INACTIVE_DAYS = 21;

    private InactiveTabPolicy() {
    }

    /** Epoch-millis threshold; tabs last accessed at or before it are aged out. {@code -1} disables. */
    public static long cutoffMillis(int days) {
        if (days <= 0) {
            return -1L;
        }
        return System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L;
    }

    /** A tab is inactive if it aged out OR it is a surplus duplicate copy. */
    public static boolean isInactive(Tab tab, long cutoff, Set<String> surplusDuplicateTabIds) {
        if (tab == null || skipEvaluation(tab.getUrl())) {
            return false;
        }
        if (cutoff > 0 && tab.getLastAccessed() > 0 && tab.getLastAccessed() <= cutoff) {
            return true;
        }
        return surplusDuplicateTabIds != null && surplusDuplicateTabIds.contains(tab.getId());
    }

    /**
     * IDs of tabs that duplicate another tab's URL, EXCEPT the most-recently-used
     * copy of each URL (that one stays active). Pass the already-filtered
     * population (e.g. same privacy mode, not closed). Internal/non-navigable
     * pages are never counted, so multiple new-tab pages are not "duplicates".
     */
    public static Set<String> surplusDuplicateTabIds(List<Tab> tabs) {
        Set<String> result = new LinkedHashSet<>();
        if (tabs == null) {
            return result;
        }
        Map<String, List<Tab>> byUrl = new LinkedHashMap<>();
        for (Tab tab : tabs) {
            if (tab == null || skipEvaluation(tab.getUrl())) {
                continue;
            }
            String normalized = normalizeUrl(tab.getUrl());
            if (normalized.isEmpty()) {
                continue;
            }
            List<Tab> bucket = byUrl.get(normalized);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byUrl.put(normalized, bucket);
            }
            bucket.add(tab);
        }
        for (List<Tab> bucket : byUrl.values()) {
            if (bucket.size() < 2) {
                continue;
            }
            Tab keep = bucket.get(0);
            for (Tab tab : bucket) {
                if (tab.getLastAccessed() > keep.getLastAccessed()) {
                    keep = tab;
                }
            }
            for (Tab tab : bucket) {
                if (tab != keep) {
                    result.add(tab.getId());
                }
            }
        }
        return result;
    }

    /** Lower-cased http(s) URL with fragment and trailing slashes removed; "" if not comparable. */
    public static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim().toLowerCase(Locale.US);
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return "";
        }
        int hashIndex = value.indexOf('#');
        if (hashIndex >= 0) {
            value = value.substring(0, hashIndex);
        }
        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /** Internal/non-navigable pages (new tab, about:, data:, javascript:) are never inactive. */
    public static boolean skipEvaluation(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        String value = url.trim();
        String lower = value.toLowerCase(Locale.US);
        return "about:blank".equals(lower)
                || lower.startsWith("about:")
                || lower.startsWith("data:")
                || lower.startsWith("javascript:")
                || UrlUtils.isInternalPageUrl(value);
    }
}
