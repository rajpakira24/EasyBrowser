package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

class BrowserHistoryDelegate implements GeckoSession.HistoryDelegate {

    private final BrowserActivity activity;
    HistoryList lastHistory;

    BrowserHistoryDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onHistoryStateChange(@NonNull GeckoSession session, @NonNull HistoryList state) {
        lastHistory = state;
    }

    @Override
    public GeckoResult<Boolean> onVisited(@NonNull GeckoSession session,
                                          @NonNull String url,
                                          String lastVisitedURL,
                                          int flags) {
        return GeckoResult.fromValue(true);
    }

    @Override
    public GeckoResult<boolean[]> getVisited(@NonNull GeckoSession session,
                                             @NonNull String[] urls) {
        return GeckoResult.fromValue(new boolean[urls.length]);
    }
}
