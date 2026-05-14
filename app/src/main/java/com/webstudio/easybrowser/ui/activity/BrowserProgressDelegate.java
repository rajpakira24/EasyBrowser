package com.webstudio.easybrowser.ui.activity;

import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.webstudio.easybrowser.R;

import org.mozilla.geckoview.GeckoSession;

class BrowserProgressDelegate implements GeckoSession.ProgressDelegate {

    private final BrowserActivity activity;

    BrowserProgressDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onProgressChange(@NonNull GeckoSession session, int progress) {
        activity.runOnUiThread(() -> {
            if (progress == 100) {
                activity.progressBar.setVisibility(View.GONE);
                if (activity.swipeRefresh != null) {
                    activity.swipeRefresh.setRefreshing(false);
                }
            } else {
                activity.progressBar.setVisibility(View.VISIBLE);
                activity.progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void onSecurityChange(@NonNull GeckoSession session,
                                 @NonNull GeckoSession.ProgressDelegate.SecurityInformation securityInfo) {
        activity.lastSecurityInfo = securityInfo;
        activity.runOnUiThread(() -> {
            ImageButton securityButton = activity.findViewById(R.id.btn_security);
            if (securityInfo.isSecure) {
                securityButton.setImageResource(R.drawable.ic_security);
            } else {
                securityButton.setImageResource(R.drawable.ic_security_warning);
            }
        });
    }

    @Override
    public void onSessionStateChange(@NonNull GeckoSession session,
                                     @NonNull GeckoSession.SessionState sessionState) {
        activity.onSessionStateChanged(session, sessionState);
    }
}
