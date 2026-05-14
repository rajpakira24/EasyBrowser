package com.webstudio.easybrowser.managers;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

public class TabThumbnailCache {
    private static final int MAX_SIZE = 12;

    private static final LinkedHashMap<String, Bitmap> cache =
            new LinkedHashMap<String, Bitmap>(MAX_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                    return size() > MAX_SIZE;
                }
            };

    private TabThumbnailCache() {}

    public static synchronized Bitmap get(String id) {
        if (id == null) return null;
        return cache.get(id);
    }

    public static synchronized void put(String id, Bitmap bitmap) {
        if (id == null || bitmap == null) return;
        cache.put(id, bitmap);
    }

    public static synchronized void remove(String id) {
        if (id == null) return;
        cache.remove(id);
    }

    public static synchronized void clear() {
        cache.clear();
    }
}
