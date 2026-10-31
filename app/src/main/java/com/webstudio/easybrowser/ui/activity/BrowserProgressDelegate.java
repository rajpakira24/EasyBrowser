package com.webstudio.easybrowser.ui.activity;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoSession;

class BrowserProgressDelegate implements GeckoSession.ProgressDelegate {

    // How long a navigation may sit with no server response before we give up on it.
    private static final long CONNECT_TIMEOUT_MS = 30_000L;
    // Progress at or beyond this means the server has started answering.
    private static final int RESPONDING_PROGRESS = 30;

    private final BrowserActivity activity;
    private final BrowserNavigationDelegate navigationDelegate;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingTimeout;

    BrowserProgressDelegate(BrowserActivity activity, BrowserNavigationDelegate navigationDelegate) {
        this.activity = activity;
        this.navigationDelegate = navigationDelegate;
    }

    private void cancelTimeout() {
        if (pendingTimeout != null) {
            handler.removeCallbacks(pendingTimeout);
            pendingTimeout = null;
        }
    }

    private void armTimeout(GeckoSession session, String url) {
        cancelTimeout();
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            return;
        }
        pendingTimeout = () -> {
            pendingTimeout = null;
            // Delegates are replaced when a session is re-attached; only the live one may act.
            if (session.getProgressDelegate() != this) {
                return;
            }
            session.stop();
            navigationDelegate.showLoadTimeoutPage(session, url);
        };
        handler.postDelayed(pendingTimeout, CONNECT_TIMEOUT_MS);
    }

    @Override
    public void onPageStop(@NonNull GeckoSession session, boolean success) {
        cancelTimeout();
    }

    @Override
    public void onProgressChange(@NonNull GeckoSession session, int progress) {
        if (progress >= RESPONDING_PROGRESS) {
            handler.post(this::cancelTimeout);
        }
        activity.runOnUiThread(() -> activity.onPageProgressChanged(session, progress));
    }

    @Override
    public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
        handler.post(() -> armTimeout(session, url));
        activity.runOnUiThread(() -> activity.onReaderPageStart(session));
    }

    @Override
    public void onSecurityChange(@NonNull GeckoSession session,
                                 @NonNull GeckoSession.ProgressDelegate.SecurityInformation securityInfo) {
        if (session != activity.session) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (session == activity.session) {
                activity.onPageSecurityChanged(securityInfo);
            }
        });
    }

    @Override
    public void onSessionStateChange(@NonNull GeckoSession session,
                                     @NonNull GeckoSession.SessionState sessionState) {
        activity.onSessionStateChanged(session, sessionState);
    }
}
