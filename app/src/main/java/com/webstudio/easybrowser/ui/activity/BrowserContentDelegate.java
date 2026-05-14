package com.webstudio.easybrowser.ui.activity;

import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.Tab;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebResponse;

class BrowserContentDelegate implements GeckoSession.ContentDelegate {

    private final BrowserActivity activity;

    BrowserContentDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onTitleChange(@NonNull GeckoSession session, String title) {
        activity.currentTitle = title;
        Tab tab = activity.tabManager != null ? activity.tabManager.getCurrentTab() : null;
        if (tab != null && tab.getSession() == session) {
            activity.tabManager.updateTabTitle(tab, title);
        }
        activity.recordHistory();
        activity.runOnUiThread(() -> {
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setSubtitle(title);
            }
        });
    }

    @Override
    public void onContextMenu(@NonNull GeckoSession session, int screenX, int screenY,
                              @NonNull GeckoSession.ContentDelegate.ContextElement element) {
        activity.showPageContextMenu(element);
    }

    @Override
    public void onExternalResponse(@NonNull GeckoSession session,
                                   @NonNull WebResponse response) {
        String contentType = response.headers.get("content-type");
        activity.startDownload(response.uri, getDownloadFileName(response), contentType);
    }

    @Override
    public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
        if (fullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.PictureInPictureParams params =
                    new android.app.PictureInPictureParams.Builder().build();
            try {
                activity.enterPictureInPictureMode(params);
            } catch (Exception ignored) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        R.string.pip_not_supported, Toast.LENGTH_SHORT).show());
            }
        }
    }

    private String getDownloadFileName(WebResponse response) {
        String disposition = response.headers.get("content-disposition");
        if (disposition == null) {
            return null;
        }
        String marker = "filename=";
        int index = disposition.toLowerCase().indexOf(marker);
        if (index < 0) {
            return null;
        }
        String raw = disposition.substring(index + marker.length()).replace("\"", "").trim();
        return sanitizeDownloadFileName(raw);
    }

    /**
     * Reject filenames that try to escape the downloads directory or spoof their
     * extension. AppDownloadManager applies a second layer of sanitization, but the
     * delegate must not pass through obviously-hostile names.
     */
    private String sanitizeDownloadFileName(String name) {
        if (name == null) {
            return null;
        }
        // Strip path traversal and Windows path separators.
        String stripped = name.replace("\\", "/");
        int lastSlash = stripped.lastIndexOf('/');
        if (lastSlash >= 0) {
            stripped = stripped.substring(lastSlash + 1);
        }
        // Strip Unicode RTL override and bidi-control characters used in filename spoofing.
        StringBuilder cleaned = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == 0x202A || c == 0x202B || c == 0x202C || c == 0x202D || c == 0x202E
                    || c == 0x200E || c == 0x200F || c < 0x20) {
                continue;
            }
            cleaned.append(c);
        }
        String result = cleaned.toString().trim();
        if (result.isEmpty() || ".".equals(result) || "..".equals(result)
                || result.startsWith(".") || result.contains("..")) {
            return null;
        }
        // Cap length so a hostile server can't fill the file system path buffer.
        if (result.length() > 200) {
            result = result.substring(0, 200);
        }
        return result;
    }
}
