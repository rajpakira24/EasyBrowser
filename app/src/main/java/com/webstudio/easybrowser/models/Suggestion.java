package com.webstudio.easybrowser.models;

/**
 * One row in the URL-bar suggestion dropdown. Site suggestions (bookmark/history) carry a page
 * title plus the URL shown as a secondary line; search suggestions carry only the query text.
 */
public class Suggestion {
    public enum Type {
        BOOKMARK,
        HISTORY,
        SEARCH
    }

    private final Type type;
    private final String title;
    private final String url;

    private Suggestion(Type type, String title, String url) {
        this.type = type;
        this.title = title;
        this.url = url;
    }

    public static Suggestion bookmark(String title, String url) {
        return new Suggestion(Type.BOOKMARK, title, url);
    }

    public static Suggestion history(String title, String url) {
        return new Suggestion(Type.HISTORY, title, url);
    }

    public static Suggestion search(String query) {
        return new Suggestion(Type.SEARCH, query, null);
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    /** What gets submitted when the row is tapped: the URL for site rows, the query for search. */
    public String getNavigationText() {
        return url != null ? url : title;
    }
}
