package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoSession;

class BrowserContentBlockingDelegate implements ContentBlocking.Delegate {
    private final BrowserActivity activity;

    BrowserContentBlockingDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onContentBlocked(@NonNull GeckoSession session,
                                 @NonNull ContentBlocking.BlockEvent event) {
        if (event.isBlocking()) {
            activity.recordBlockedPrivacyItem();
        }
    }
}
