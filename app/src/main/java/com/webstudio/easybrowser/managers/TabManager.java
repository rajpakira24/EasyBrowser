package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TabManager {
    private static final String TAG = "TabManager";
    private static final String PREF_SAVED_TABS = "saved_tabs";
    private static final String PREF_CURRENT_TAB_ID = "current_tab_id";
    // If the serialized tab state exceeds this size, session history is dropped to stay
    // within SharedPreferences limits (XML file read entirely into memory on load).
    private static final int MAX_PERSIST_BYTES = 256 * 1024;

    public static class ClosedTab {
        public final String title;
        public final String url;
        public ClosedTab(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    private List<Tab> tabs;
    private Tab currentTab;
    private final java.util.LinkedList<ClosedTab> recentlyClosed = new java.util.LinkedList<>();
    private final java.util.LinkedList<String> tabBackStack = new java.util.LinkedList<>();
    private GeckoRuntime runtime;
    private Context context;
    private OnTabChangeListener listener;
    private SharedPreferences prefs;
    private boolean restoredTabs;

    public interface OnTabChangeListener {
        void onTabChanged(Tab tab);
        void onTabCountChanged(int count);
    }

    public TabManager(Context context, GeckoRuntime runtime, OnTabChangeListener listener) {
        this.context = context;
        this.runtime = runtime;
        this.listener = listener;
        this.tabs = new ArrayList<>();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        restoredTabs = restoreTabs();
        if (!restoredTabs) {
            createDefaultTab();
        } else if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    private void createDefaultTab() {
        restoredTabs = false;
        GeckoSession session = createSession(false);
        session.open(runtime);

        String homepage = UrlUtils.getNewTabPageUrl(context);
        Tab tab = new Tab(session, "Home", homepage);
        tab.setInitialLoadPending(true);
        tabs.add(tab);
        currentTab = tab;
        persistTabs();

        if (listener != null) {
            listener.onTabChanged(tab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public Tab createNewTab(boolean isPrivate) {
        return createNewTab(isPrivate, !isPrivate, null);
    }

    public Tab createNewTab(boolean isPrivate, boolean loadStartUrl) {
        return createNewTab(isPrivate, loadStartUrl, null);
    }

    public Tab createNewTab(boolean isPrivate, boolean loadStartUrl, String parentTabId) {
        GeckoSession session = createSession(isPrivate);
        session.open(runtime);

        String startUrl = isPrivate ? "about:blank" : UrlUtils.getNewTabPageUrl(context);
        Tab tab = new Tab(null, session, isPrivate ? "Private Tab" : "New Tab", startUrl, isPrivate);
        if (!isPrivate && currentTab != null && !currentTab.isPrivate()) {
            tab.setGroupName(currentTab.getGroupName());
        }
        if (parentTabId != null && !isPrivate) {
            tab.setParentTabId(parentTabId);
        }
        tab.setInitialLoadPending(loadStartUrl && !isPrivate);
        tabs.add(tab);
        pushCurrentTabToBackStack();
        currentTab = tab;
        persistTabs();

        if (listener != null) {
            listener.onTabChanged(tab);
            listener.onTabCountChanged(tabs.size());
        }

        return tab;
    }

    private GeckoSession createSession(boolean isPrivate) {
        boolean javascriptEnabled = prefs.getBoolean("javascript_enabled", true);
        String uaPreset = prefs.getString("user_agent_preset", "mobile");

        GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder()
                .usePrivateMode(isPrivate)
                .allowJavascript(javascriptEnabled);

        if ("desktop".equals(uaPreset)) {
            builder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                    .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
        } else {
            builder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                    .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        }

        GeckoSession session = new GeckoSession(builder.build());

        // Apply custom UA string for presets that need it
        if ("iphone".equals(uaPreset)) {
            session.getSettings().setUserAgentOverride(
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        } else if ("ipad".equals(uaPreset)) {
            session.getSettings().setUserAgentOverride(
                    "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        } else if ("custom".equals(uaPreset)) {
            String customUa = prefs.getString("user_agent_custom_string", "");
            if (!customUa.isEmpty()) session.getSettings().setUserAgentOverride(customUa);
        }

        return session;
    }

    public void persistCurrentTabGroup() {
        persistTabs();
    }

    public List<ClosedTab> getRecentlyClosed() {
        return new ArrayList<>(recentlyClosed);
    }

    public synchronized void closeTab(Tab tab) {
        if (tab != null) {
            if (!tab.isPrivate() && tab.getUrl() != null && !tab.getUrl().trim().isEmpty()
                    && !"about:blank".equals(tab.getUrl())) {
                recentlyClosed.addFirst(new ClosedTab(tab.getTitle(), tab.getUrl()));
                if (recentlyClosed.size() > 10) recentlyClosed.removeLast();
            }
            GeckoSession session = tab.getSession();
            if (session != null && session.isOpen()) {
                session.close();
            }
            TabThumbnailCache.remove(tab.getId());
            tabs.remove(tab);
            removeTabFromBackStack(tab.getId());

            if (tab == currentTab) {
                if (!tabs.isEmpty()) {
                    Tab previousTab = popPreviousTab();
                    currentTab = previousTab != null ? previousTab : tabs.get(tabs.size() - 1);
                    if (listener != null) {
                        listener.onTabChanged(currentTab);
                    }
                } else {
                    createDefaultTab();
                }
            }
            persistTabs();

            if (listener != null) {
                listener.onTabCountChanged(tabs.size());
            }
        }
    }

    public void switchToTab(Tab tab) {
        if (tabs.contains(tab) && tab != currentTab) {
            pushCurrentTabToBackStack();
            currentTab = tab;
            persistTabs();
            if (listener != null) {
                listener.onTabChanged(tab);
            }
        }
    }

    public boolean switchToPreviousTab() {
        Tab previousTab = popPreviousTab();
        if (previousTab == null) {
            return false;
        }
        currentTab = previousTab;
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(previousTab);
        }
        return true;
    }

    public synchronized boolean closeCurrentTabAndSwitchToPrevious() {
        if (currentTab == null) {
            return false;
        }
        Tab previousTab = popPreviousTab();
        if (previousTab == null) {
            return false;
        }

        Tab tabToClose = currentTab;
        GeckoSession closingSession = tabToClose.getSession();
        if (closingSession != null && closingSession.isOpen()) {
            closingSession.close();
        }
        TabThumbnailCache.remove(tabToClose.getId());
        tabs.remove(tabToClose);
        removeTabFromBackStack(tabToClose.getId());
        currentTab = previousTab;
        persistTabs();

        if (listener != null) {
            listener.onTabChanged(previousTab);
            listener.onTabCountChanged(tabs.size());
        }
        return true;
    }

    private void pushCurrentTabToBackStack() {
        if (currentTab == null) {
            return;
        }
        String currentId = currentTab.getId();
        tabBackStack.remove(currentId);
        tabBackStack.addFirst(currentId);
    }

    private Tab popPreviousTab() {
        while (!tabBackStack.isEmpty()) {
            String tabId = tabBackStack.removeFirst();
            Tab tab = findTabById(tabId);
            if (tab != null && tab != currentTab) {
                return tab;
            }
        }
        return null;
    }

    private void removeTabFromBackStack(String tabId) {
        tabBackStack.remove(tabId);
    }

    private void updateTabUrl(GeckoSession session, String url) {
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setUrl(url);
                persistTabs();
                break;
            }
        }
    }

    private void updateTabTitle(GeckoSession session, String title) {
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setTitle(title);
                persistTabs();
                break;
            }
        }
    }

    public void updateTabUrl(Tab tab, String url) {
        if (tab != null && tabs.contains(tab)) {
            tab.setUrl(url);
            persistTabs();
        }
    }

    public void updateTabTitle(Tab tab, String title) {
        if (tab != null && tabs.contains(tab)) {
            tab.setTitle(title);
            persistTabs();
        }
    }

    public void updateTabSessionState(GeckoSession session, String sessionState) {
        if (sessionState == null || sessionState.trim().isEmpty()) {
            return;
        }
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setSessionState(sessionState);
                persistTabs();
                break;
            }
        }
    }

    private void updateTabFavicon(GeckoSession session, String url, Bitmap favicon) {
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setFavicon(favicon);
                break;
            }
        }
    }

    public List<Tab> getTabs() {
        return new ArrayList<>(tabs);
    }

    public Tab findTabBySession(GeckoSession session) {
        if (session == null) {
            return null;
        }
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                return tab;
            }
        }
        return null;
    }

    private Tab findTabById(String tabId) {
        if (tabId == null) {
            return null;
        }
        for (Tab tab : tabs) {
            if (tabId.equals(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    public Tab getCurrentTab() {
        return currentTab;
    }

    public int getTabCount() {
        return tabs.size();
    }

    public boolean hasRestoredTabs() {
        return restoredTabs;
    }

    public void closeAllTabs() {
        for (Tab tab : tabs) {
            tab.getSession().close();
            TabThumbnailCache.remove(tab.getId());
        }
        tabs.clear();
        createDefaultTab();
        if (listener != null) {
            listener.onTabCountChanged(tabs.size());
            listener.onTabChanged(currentTab);
        }
    }

    public void releaseAllSessions() {
        for (Tab tab : tabs) {
            GeckoSession session = tab.getSession();
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private boolean restoreTabs() {
        String savedTabs = prefs.getString(PREF_SAVED_TABS, null);
        if (savedTabs == null || savedTabs.trim().isEmpty()) {
            return false;
        }
        // Guard against tampered/oversized preferences. Same cap as persistTabs() —
        // anything bigger than this can't have come from us, so refuse to parse.
        if (savedTabs.length() > MAX_PERSIST_BYTES * 2) {
            Log.w(TAG, "Saved tabs blob too large (" + savedTabs.length() + " bytes); discarding");
            return false;
        }

        try {
            JSONArray saved = new JSONArray(savedTabs);
            String currentTabId = prefs.getString(PREF_CURRENT_TAB_ID, null);
            for (int i = 0; i < saved.length(); i++) {
                JSONObject item = saved.getJSONObject(i);
                String url = item.optString("url", UrlUtils.getNewTabPageUrl(context));
                if (url == null || url.trim().isEmpty() || "about:blank".equals(url)) {
                    continue;
                }
                // Never restore tabs that claim to be private — saved_tabs is supposed
                // to contain only non-private tabs, so an isPrivate=true entry indicates
                // tampering. Skip rather than honor the flag and create an unexpected
                // non-private session for what was originally a private tab.
                if (item.optBoolean("is_private", false)) {
                    continue;
                }
                GeckoSession session = createSession(false);
                session.open(runtime);

                Tab tab = new Tab(
                        item.optString("id", null),
                        session,
                        item.optString("title", "New Tab"),
                        url,
                        false);
                String sessionState = item.optString("session_state", null);
                if (sessionState != null && !sessionState.trim().isEmpty()) {
                    GeckoSession.SessionState state = null;
                    try {
                        state = GeckoSession.SessionState.fromString(sessionState);
                    } catch (Exception parseError) {
                        Log.w(TAG, "Skipping unparseable session state for tab " + tab.getId(), parseError);
                    }
                    if (state != null) {
                        session.restoreState(state);
                        tab.setSessionState(sessionState);
                        tab.setInitialLoadPending(false);
                    } else {
                        tab.setInitialLoadPending(true);
                    }
                } else {
                    tab.setInitialLoadPending(true);
                }
                String group = item.optString("group", null);
                if (group != null && !group.isEmpty()) tab.setGroupName(group);
                String parentId = item.optString("parent_id", null);
                if (parentId != null && !parentId.isEmpty()) tab.setParentTabId(parentId);
                tabs.add(tab);
                if (tab.getId().equals(currentTabId)) {
                    currentTab = tab;
                }
            }
            if (tabs.isEmpty()) {
                return false;
            }
            if (currentTab == null) {
                currentTab = tabs.get(tabs.size() - 1);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore tabs, starting fresh", e);
            tabs.clear();
            currentTab = null;
            return false;
        }
    }

    private void persistTabs() {
        String json = buildTabsJson(true).toString();
        if (json.length() > MAX_PERSIST_BYTES) {
            Log.w(TAG, "Tab state too large (" + json.length() + " bytes); dropping session history");
            json = buildTabsJson(false).toString();
        }
        SharedPreferences.Editor editor = prefs.edit().putString(PREF_SAVED_TABS, json);
        if (currentTab != null && !currentTab.isPrivate()) {
            editor.putString(PREF_CURRENT_TAB_ID, currentTab.getId());
        } else {
            editor.remove(PREF_CURRENT_TAB_ID);
        }
        editor.apply();
    }

    private JSONArray buildTabsJson(boolean includeSessionState) {
        JSONArray saved = new JSONArray();
        for (Tab tab : tabs) {
            if (tab == null || tab.isPrivate()) continue;
            String url = tab.getUrl();
            if (url == null || url.trim().isEmpty() || "about:blank".equals(url)) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("id", tab.getId());
                item.put("title", tab.getTitle());
                item.put("url", url);
                if (includeSessionState && tab.getSessionState() != null) {
                    item.put("session_state", tab.getSessionState());
                }
                if (tab.getGroupName() != null) item.put("group", tab.getGroupName());
                if (tab.getParentTabId() != null) item.put("parent_id", tab.getParentTabId());
                saved.put(item);
            } catch (Exception e) {
                Log.e(TAG, "Failed to serialize tab " + tab.getId(), e);
            }
        }
        return saved;
    }
}
