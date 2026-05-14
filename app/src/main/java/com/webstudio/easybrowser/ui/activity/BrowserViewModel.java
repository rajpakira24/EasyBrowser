package com.webstudio.easybrowser.ui.activity;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.managers.TabManager;
import org.mozilla.geckoview.GeckoRuntime;

public class BrowserViewModel extends AndroidViewModel {
    private final TabManager tabManager;

    public BrowserViewModel(@NonNull Application application) {
        super(application);
        GeckoRuntime runtime = RuntimeManager.getRuntime(application);
        if (runtime == null) {
            throw new IllegalStateException(
                    "GeckoRuntime failed to initialize; see logcat for details.");
        }
        tabManager = new TabManager(
                application.getApplicationContext(),
                runtime,
                null);
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    @Override
    protected void onCleared() {
        tabManager.setOnTabChangeListener(null);
        tabManager.releaseAllSessions();
    }
}
