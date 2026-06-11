package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoSession;

class BrowserProgressDelegate implements GeckoSession.ProgressDelegate {

    private final BrowserActivity activity;

    BrowserProgressDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onProgressChange(@NonNull GeckoSession session, int progress) {
        activity.runOnUiThread(() -> activity.onPageProgressChanged(session, progress));
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
