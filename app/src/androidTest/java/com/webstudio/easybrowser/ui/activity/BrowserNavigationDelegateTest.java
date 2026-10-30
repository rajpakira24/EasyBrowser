package com.webstudio.easybrowser.ui.activity;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.geckoview.GeckoSession;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class BrowserNavigationDelegateTest {
    @Test
    public void backgroundNavigationDoesNotReplaceVisibleUrlOrSecurityInfo() {
        try (ActivityScenario<BrowserActivity> scenario = ActivityScenario.launch(BrowserActivity.class)) {
            scenario.onActivity(activity -> {
                String visibleUrl = activity.currentUrl;
                Object securityInfo = activity.lastSecurityInfo;
                GeckoSession backgroundSession = new GeckoSession();

                new BrowserNavigationDelegate(activity).onLocationChange(backgroundSession,
                        "https://background.example/redirect", Collections.emptyList(), false);

                assertEquals(visibleUrl, activity.currentUrl);
                assertEquals(securityInfo, activity.lastSecurityInfo);
            });
        }
    }
}
