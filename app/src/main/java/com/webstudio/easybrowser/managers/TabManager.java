package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.models.BrowserState;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabManager {
    private static final String TAG = "TabManager";
    private static final String PREF_SAVED_TABS = "saved_tabs";
    private static final String PREF_CURRENT_TAB_ID = "current_tab_id";
    // If the serialized tab state exceeds this size, session history is dropped to stay
    // within SharedPreferences limits (XML file read entirely into memory on load).
    private static final int MAX_PERSIST_BYTES = 256 * 1024;
    private static final long SCROLL_PERSIST_INTERVAL_MS = 1200L;

    public static class ClosedTab {
        public final String id;
        public final String title;
        public final String url;
        public final String faviconUri;
        public final String thumbnailPath;
        public final boolean isPrivate;
        public final String groupId;
        public final String groupName;
        public final int groupColor;
        public final long createdAt;
        public final long lastAccessed;
        public final int position;
        public final boolean pinned;
        public final int scrollY;
        public final boolean locked;
        public final String parentTabId;
        public final String sessionState;
        public final List<String> groupTabIds;

        public ClosedTab(String title, String url) {
            this(null, title, url, null, null, false, null, null, 0,
                    0, 0, 0, false, 0, false, null, null, new ArrayList<>());
        }

        private ClosedTab(String id, String title, String url, String faviconUri,
                          String thumbnailPath, boolean isPrivate, String groupId,
                          String groupName, int groupColor, long createdAt,
                          long lastAccessed, int position, boolean pinned, int scrollY,
                          boolean locked, String parentTabId, String sessionState,
                          List<String> groupTabIds) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.faviconUri = faviconUri;
            this.thumbnailPath = thumbnailPath;
            this.isPrivate = isPrivate;
            this.groupId = groupId;
            this.groupName = groupName;
            this.groupColor = groupColor;
            this.createdAt = createdAt;
            this.lastAccessed = lastAccessed;
            this.position = position;
            this.pinned = pinned;
            this.scrollY = scrollY;
            this.locked = locked;
            this.parentTabId = parentTabId;
            this.sessionState = sessionState;
            this.groupTabIds = groupTabIds != null ? new ArrayList<>(groupTabIds) : new ArrayList<>();
        }
    }

    private List<Tab> tabs;
    private Tab currentTab;
    private final BrowserStateStore stateStore;
    private final java.util.LinkedList<ClosedTab> recentlyClosed = new java.util.LinkedList<>();
    private final java.util.LinkedList<String> tabBackStack = new java.util.LinkedList<>();
    private GeckoRuntime runtime;
    private Context context;
    private OnTabChangeListener listener;
    private SharedPreferences prefs;
    private TabRepository tabRepository;
    private boolean restoredTabs;
    private long lastScrollPersistAt;

    public interface OnTabChangeListener {
        void onTabChanged(Tab tab);
        void onTabCountChanged(int count);
        default void onTabMetadataChanged(Tab tab) {}
    }

    public TabManager(Context context, GeckoRuntime runtime, OnTabChangeListener listener) {
        this.context = context;
        this.runtime = runtime;
        this.listener = listener;
        this.stateStore = new BrowserStateStore();
        this.tabs = new ArrayList<>();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.tabRepository = new TabRepository(context);

        if (shouldOpenHomepageOnStartup()) {
            restoredTabs = false;
        } else {
            restoredTabs = restoreTabsFromRoom();
            if (!restoredTabs) {
                restoredTabs = restoreLegacyTabs();
                if (restoredTabs) {
                    persistTabs();
                }
            }
        }
        if (!restoredTabs) {
            createDefaultTab();
        } else if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    private boolean shouldOpenHomepageOnStartup() {
        return "homepage".equals(prefs.getString(SettingsKeys.PREF_STARTUP_MODE, "restore_all"));
    }

    private boolean shouldRestoreLastSessionOnly() {
        return "restore_last_session".equals(
                prefs.getString(SettingsKeys.PREF_STARTUP_MODE, "restore_all"));
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        this.listener = listener;
        if (listener != null) {
            syncMirrorFromState();
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public BrowserState getBrowserState() {
        return stateStore.snapshot();
    }

    private void syncMirrorFromState() {
        tabs = stateStore.getAllTabs();
        currentTab = stateStore.getCurrentTab();
    }

    private void notifyTabChanged() {
        syncMirrorFromState();
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
        tab.setPosition(stateStore.getTabCount(false));
        tab.setInitialLoadPending(true);
        stateStore.addTab(tab, true);
        syncMirrorFromState();
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
        if (parentTabId != null && !isPrivate) {
            tab.setParentTabId(parentTabId);
        }
        tab.setPosition(stateStore.getTabCount(isPrivate));
        tab.setInitialLoadPending(loadStartUrl && !isPrivate);
        pushCurrentTabToBackStack();
        stateStore.addTab(tab, true);
        syncMirrorFromState();
        persistTabs();

        if (listener != null) {
            listener.onTabChanged(tab);
            listener.onTabCountChanged(tabs.size());
        }

        return tab;
    }

    private GeckoSession createSession(boolean isPrivate) {
        boolean javascriptEnabled = prefs.getBoolean("javascript_enabled", true);
        String uaPreset = prefs.getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");
        boolean desktopDefault = prefs.getBoolean(SettingsKeys.PREF_DESKTOP_SITE_DEFAULT, false);

        GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder()
                .usePrivateMode(isPrivate)
                .allowJavascript(javascriptEnabled)
                .suspendMediaWhenInactive(
                        !prefs.getBoolean(SettingsKeys.PREF_BACKGROUND_PLAY_ENABLED, false));

        if ("desktop".equals(uaPreset) || desktopDefault) {
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
            String customUa = prefs.getString(SettingsKeys.PREF_USER_AGENT_CUSTOM_STRING, "");
            if (!customUa.isEmpty()) session.getSettings().setUserAgentOverride(customUa);
        }

        return session;
    }

    public void persistCurrentTabGroup() {
        persistTabs();
    }

    public void refreshMetadataFromRepository() {
        List<TabGroup> groups = tabRepository.getGroupsBlocking();
        BrowserState state = stateStore.snapshot();
        Map<String, TabGroup> groupMap = new HashMap<>();
        Map<String, Tab> persistedTabs = new HashMap<>();
        for (TabGroup group : groups) {
            groupMap.put(group.getGroupId(), group);
            for (Tab persistedTab : group.getTabs()) {
                persistedTab.setGroupName(group.getGroupName());
                persistedTab.setGroupColor(group.getGroupColor());
                persistedTabs.put(persistedTab.getId(), persistedTab);
            }
        }
        for (Tab persistedTab : tabRepository.getAllTabsBlocking()) {
            if (!persistedTabs.containsKey(persistedTab.getId())) {
                persistedTabs.put(persistedTab.getId(), persistedTab);
            }
        }

        boolean changed = false;
        List<Tab> regularTabs = stateStore.getRegularTabs();
        for (Tab tab : regularTabs) {
            Tab persisted = persistedTabs.get(tab.getId());
            if (persisted == null) {
                continue;
            }
            tab.setGroupId(persisted.getGroupId());
            TabGroup group = persisted.getGroupId() != null ? groupMap.get(persisted.getGroupId()) : null;
            tab.setGroupName(group != null ? group.getGroupName() : null);
            tab.setGroupColor(group != null ? group.getGroupColor() : 0);
            tab.setThumbnailPath(persisted.getThumbnailPath());
            tab.setPosition(persisted.getPosition());
            tab.setPinned(persisted.isPinned());
            tab.setLocked(persisted.isLocked());
            changed = true;
        }
        stateStore.loadRegularState(regularTabs, groups, state.getActiveRegularTabId());
        syncMirrorFromState();
        if (changed && listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public void updateTabThumbnail(String tabId, String thumbnailPath) {
        Tab tab = findTabById(tabId);
        if (tab != null) {
            tab.setThumbnailPath(thumbnailPath);
            if (!tab.isPrivate()) {
                tabRepository.updateThumbnailPathBlocking(tabId, thumbnailPath);
            }
        }
    }

    public void moveTabToGroup(String tabId, String groupId, String groupName, int groupColor) {
        Tab tab = findTabById(tabId);
        if (tab == null || groupId == null) {
            return;
        }
        stateStore.addTabToGroup(tab, groupId, groupName, groupColor);
        syncMirrorFromState();
        persistTabs();
    }

    public TabGroup createGroupForTabs(List<String> tabIds, String groupName, int groupColor, boolean isPrivate) {
        return createGroupForTabs(tabIds, groupName, groupColor, isPrivate, null);
    }

    public TabGroup createGroupForTabs(List<String> tabIds, String groupName, int groupColor,
                                       boolean isPrivate, String groupId) {
        if (tabIds == null || tabIds.size() < 2) {
            return null;
        }
        TabGroup group = stateStore.createGroupForTabs(tabIds, groupName, groupColor, isPrivate, groupId);
        if (group == null) {
            return null;
        }
        syncMirrorFromState();
        persistTabs();
        return group;
    }

    public void addTabToGroup(Tab tab, String groupId, String groupName, int groupColor) {
        if (tab == null || groupId == null) {
            return;
        }
        stateStore.addTabToGroup(tab, groupId, groupName, groupColor);
        syncMirrorFromState();
        persistTabs();
    }

    public void removeTabFromGroup(Tab tab) {
        if (tab == null || tab.getGroupId() == null) {
            return;
        }
        stateStore.removeTabFromGroup(tab.getId());
        syncMirrorFromState();
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public boolean reorderTabs(List<Tab> orderedTabs) {
        if (orderedTabs == null || orderedTabs.isEmpty()) {
            return false;
        }
        boolean isPrivate = orderedTabs.get(0).isPrivate();
        List<String> orderedIds = new ArrayList<>();
        for (Tab tab : orderedTabs) {
            if (tab == null || tab.isPrivate() != isPrivate) {
                return false;
            }
            orderedIds.add(tab.getId());
        }
        boolean changed = stateStore.reorderTabs(orderedIds, isPrivate);
        if (!changed) {
            return false;
        }
        syncMirrorFromState();
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
        return true;
    }

    public List<ClosedTab> getRecentlyClosed() {
        return new ArrayList<>(recentlyClosed);
    }

    public synchronized ClosedTab closeTab(Tab tab) {
        if (tab != null) {
            if (tab.isLocked()) {
                return null;
            }
            ClosedTab closedTab = snapshotClosedTab(tab);
            if (!tab.isPrivate() && tab.getUrl() != null && !tab.getUrl().trim().isEmpty()
                    && !"about:blank".equals(tab.getUrl())) {
                recentlyClosed.addFirst(closedTab);
                if (recentlyClosed.size() > 10) recentlyClosed.removeLast();
            }
            GeckoSession session = tab.getSession();
            if (session != null && session.isOpen()) {
                session.close();
            }
            TabThumbnailCache.remove(tab.getId());
            boolean wasCurrent = tab == currentTab;
            if (!tab.isPrivate()) {
                tabRepository.deleteTabBlocking(tab.getId());
            }
            stateStore.closeTab(tab.getId());
            syncMirrorFromState();
            removeTabFromBackStack(tab.getId());

            if (wasCurrent) {
                if (!tabs.isEmpty()) {
                    Tab previousTab = popPreviousTab();
                    if (previousTab != null) {
                        stateStore.switchToTab(previousTab.getId());
                    } else if (currentTab == null && !tabs.isEmpty()) {
                        stateStore.switchToTab(tabs.get(tabs.size() - 1).getId());
                    }
                    syncMirrorFromState();
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
            return closedTab;
        }
        return null;
    }

    public void switchToTab(Tab tab) {
        if (tab != null && findTabById(tab.getId()) != null && tab != currentTab) {
            pushCurrentTabToBackStack();
            tab.setLastAccessed(System.currentTimeMillis());
            stateStore.switchToTab(tab.getId());
            syncMirrorFromState();
            persistTabs();
            if (listener != null) {
                listener.onTabChanged(tab);
            }
        }
    }

    public synchronized void touchTabs(List<String> tabIds) {
        if (tabIds == null || tabIds.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (String tabId : tabIds) {
            Tab tab = findTabById(tabId);
            if (tab == null) {
                continue;
            }
            tab.setLastAccessed(now);
            stateStore.updateTabAccess(tabId, now);
            changed = true;
        }
        if (changed) {
            syncMirrorFromState();
            persistTabs();
        }
    }

    public synchronized void setTabPinned(String tabId, boolean pinned) {
        if (!stateStore.setTabPinned(tabId, pinned)) {
            return;
        }
        syncMirrorFromState();
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(currentTab);
            listener.onTabCountChanged(tabs.size());
        }
    }

    public boolean switchToPreviousTab() {
        Tab previousTab = popPreviousTab();
        if (previousTab == null) {
            return false;
        }
        stateStore.switchToTab(previousTab.getId());
        syncMirrorFromState();
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(previousTab);
        }
        return true;
    }

    public synchronized boolean closeCurrentTabAndSwitchToPrevious() {
        return closeCurrentTabAndSwitchToPreviousForUndo() != null;
    }

    public synchronized ClosedTab closeCurrentTabAndSwitchToPreviousForUndo() {
        if (currentTab == null) {
            return null;
        }
        if (currentTab.isLocked()) {
            return null;
        }
        Tab previousTab = popPreviousTab();
        if (previousTab == null) {
            return null;
        }

        Tab tabToClose = currentTab;
        ClosedTab closedTab = snapshotClosedTab(tabToClose);
        if (!tabToClose.isPrivate() && tabToClose.getUrl() != null
                && !tabToClose.getUrl().trim().isEmpty()
                && !"about:blank".equals(tabToClose.getUrl())) {
            recentlyClosed.addFirst(closedTab);
            if (recentlyClosed.size() > 10) recentlyClosed.removeLast();
        }
        GeckoSession closingSession = tabToClose.getSession();
        if (closingSession != null && closingSession.isOpen()) {
            closingSession.close();
        }
        TabThumbnailCache.remove(tabToClose.getId());
        if (!tabToClose.isPrivate()) {
            tabRepository.deleteTabBlocking(tabToClose.getId());
        }
        stateStore.closeTab(tabToClose.getId());
        removeTabFromBackStack(tabToClose.getId());
        previousTab.setLastAccessed(System.currentTimeMillis());
        stateStore.switchToTab(previousTab.getId());
        syncMirrorFromState();
        persistTabs();

        if (listener != null) {
            listener.onTabChanged(previousTab);
            listener.onTabCountChanged(tabs.size());
        }
        return closedTab;
    }

    public synchronized Tab restoreClosedTab(ClosedTab closedTab) {
        if (closedTab == null || closedTab.url == null || closedTab.url.trim().isEmpty()) {
            return null;
        }
        Tab existing = findTabById(closedTab.id);
        if (existing != null) {
            switchToTab(existing);
            return existing;
        }
        GeckoSession session = createSession(closedTab.isPrivate);
        session.open(runtime);
        Tab restored = new Tab(closedTab.id, session,
                closedTab.title != null && !closedTab.title.trim().isEmpty()
                        ? closedTab.title
                        : "New Tab",
                closedTab.url,
                closedTab.isPrivate);
        restored.setFaviconUri(closedTab.faviconUri);
        restored.setThumbnailPath(closedTab.thumbnailPath);
        restored.setGroupId(closedTab.groupId);
        restored.setGroupName(closedTab.groupName);
        restored.setGroupColor(closedTab.groupColor);
        restored.setCreatedAt(closedTab.createdAt != 0
                ? closedTab.createdAt
                : System.currentTimeMillis());
        restored.setLastAccessed(System.currentTimeMillis());
        restored.setPosition(closedTab.position);
        restored.setPinned(closedTab.pinned);
        restored.setScrollY(closedTab.scrollY);
        restored.setLocked(closedTab.locked);
        restored.setParentTabId(closedTab.parentTabId);
        restored.setSessionState(closedTab.sessionState);
        restored.setInitialLoadPending(true);

        pushCurrentTabToBackStack();
        stateStore.addTab(restored, true);
        if (closedTab.groupId != null && findTabById(restored.getId()).getGroupId() == null) {
            List<String> groupTabIds = new ArrayList<>();
            for (String tabId : closedTab.groupTabIds) {
                if (findTabById(tabId) != null && !groupTabIds.contains(tabId)) {
                    groupTabIds.add(tabId);
                }
            }
            if (!groupTabIds.contains(restored.getId())) {
                groupTabIds.add(restored.getId());
            }
            if (groupTabIds.size() >= 2) {
                stateStore.createGroupForTabs(groupTabIds, closedTab.groupName,
                        closedTab.groupColor, closedTab.isPrivate, closedTab.groupId);
            }
        }
        syncMirrorFromState();
        removeRecentlyClosed(closedTab);
        persistTabs();
        if (listener != null) {
            listener.onTabChanged(restored);
            listener.onTabCountChanged(tabs.size());
        }
        return restored;
    }

    private ClosedTab snapshotClosedTab(Tab tab) {
        List<String> groupTabIds = new ArrayList<>();
        if (tab != null && tab.getGroupId() != null) {
            for (Tab groupTab : stateStore.getTabs(tab.isPrivate())) {
                if (tab.getGroupId().equals(groupTab.getGroupId())) {
                    groupTabIds.add(groupTab.getId());
                }
            }
        }
        return new ClosedTab(
                tab.getId(),
                tab.getTitle(),
                tab.getUrl(),
                tab.getFaviconUri(),
                tab.getThumbnailPath(),
                tab.isPrivate(),
                tab.getGroupId(),
                tab.getGroupName(),
                tab.getGroupColor(),
                tab.getCreatedAt(),
                tab.getLastAccessed(),
                tab.getPosition(),
                tab.isPinned(),
                tab.getScrollY(),
                tab.isLocked(),
                tab.getParentTabId(),
                tab.getSessionState(),
                groupTabIds);
    }

    private void removeRecentlyClosed(ClosedTab closedTab) {
        if (closedTab == null) {
            return;
        }
        for (int i = recentlyClosed.size() - 1; i >= 0; i--) {
            ClosedTab item = recentlyClosed.get(i);
            if ((closedTab.id != null && closedTab.id.equals(item.id))
                    || (closedTab.url != null && closedTab.url.equals(item.url))) {
                recentlyClosed.remove(i);
            }
        }
    }

    private void pushCurrentTabToBackStack() {
        Tab active = stateStore.getCurrentTab();
        if (active == null) {
            return;
        }
        String currentId = active.getId();
        tabBackStack.remove(currentId);
        tabBackStack.addFirst(currentId);
    }

    private Tab popPreviousTab() {
        while (!tabBackStack.isEmpty()) {
            String tabId = tabBackStack.removeFirst();
            Tab tab = findTabById(tabId);
            if (tab != null && tab != stateStore.getCurrentTab()) {
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
                updateTabFaviconFallback(tab, url);
                tab.setUrl(url);
                tab.setLastAccessed(System.currentTimeMillis());
                persistTabs();
                notifyTabMetadataChanged(tab);
                break;
            }
        }
    }

    private void updateTabTitle(GeckoSession session, String title) {
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setTitle(title);
                tab.setLastAccessed(System.currentTimeMillis());
                persistTabs();
                notifyTabMetadataChanged(tab);
                break;
            }
        }
    }

    public void updateTabUrl(Tab tab, String url) {
        if (tab != null && tabs.contains(tab)) {
            updateTabFaviconFallback(tab, url);
            tab.setUrl(url);
            tab.setLastAccessed(System.currentTimeMillis());
            persistTabs();
            notifyTabMetadataChanged(tab);
        }
    }

    public synchronized void setTabLocked(String tabId, boolean locked) {
        Tab tab = findTabById(tabId);
        if (tab == null || tab.isLocked() == locked) {
            return;
        }
        tab.setLocked(locked);
        persistTabs();
        notifyTabMetadataChanged(tab);
    }

    public void updateTabTitle(Tab tab, String title) {
        if (tab != null && tabs.contains(tab)) {
            tab.setTitle(title);
            tab.setLastAccessed(System.currentTimeMillis());
            persistTabs();
            notifyTabMetadataChanged(tab);
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

    public void updateTabScrollPosition(GeckoSession session, int scrollY, boolean forcePersist) {
        if (session == null) {
            return;
        }
        int sanitizedScrollY = Math.max(0, scrollY);
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                if (tab.getScrollY() == sanitizedScrollY && !forcePersist) {
                    return;
                }
                tab.setScrollY(sanitizedScrollY);
                long now = System.currentTimeMillis();
                if (forcePersist || now - lastScrollPersistAt >= SCROLL_PERSIST_INTERVAL_MS) {
                    lastScrollPersistAt = now;
                    persistTabs();
                }
                break;
            }
        }
    }

    private void updateTabFavicon(GeckoSession session, String url, Bitmap favicon) {
        for (Tab tab : tabs) {
            if (tab.getSession() == session) {
                tab.setFavicon(favicon);
                notifyTabMetadataChanged(tab);
                break;
            }
        }
    }

    private void updateTabFaviconFallback(Tab tab, String newUrl) {
        if (tab == null || newUrl == null) {
            return;
        }
        String oldHost = hostOf(tab.getUrl());
        String newHost = hostOf(newUrl);
        if (newHost == null) {
            tab.setFavicon(null);
            tab.setFaviconUri(null);
            return;
        }
        if (oldHost == null || !oldHost.equalsIgnoreCase(newHost)) {
            tab.setFavicon(null);
        }
        tab.setFaviconUri(faviconUrlFor(newUrl));
    }

    private void notifyTabMetadataChanged(Tab tab) {
        if (listener != null) {
            listener.onTabMetadataChanged(tab);
        }
    }

    private static String faviconUrlFor(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }
        Uri uri = Uri.parse(rawUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return null;
        }
        return scheme + "://" + host + "/favicon.ico";
    }

    private static String hostOf(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }
        return Uri.parse(rawUrl).getHost();
    }

    public List<Tab> getTabs() {
        return stateStore.getAllTabs();
    }

    public Tab findTabBySession(GeckoSession session) {
        if (session == null) {
            return null;
        }
        for (Tab tab : stateStore.getAllTabs()) {
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
        return stateStore.findTabById(tabId);
    }

    public Tab getCurrentTab() {
        return stateStore.getCurrentTab();
    }

    public int getTabCount() {
        return stateStore.getTabCount();
    }

    public int getTabCount(boolean privateTabsMode) {
        return stateStore.getTabCount(privateTabsMode);
    }

    public int getRegularTabCount() {
        return stateStore.getTabCount(false);
    }

    public int getPrivateTabCount() {
        return stateStore.getTabCount(true);
    }

    public boolean hasRestoredTabs() {
        return restoredTabs;
    }

    public void closeAllTabs() {
        for (Tab tab : stateStore.getAllTabs()) {
            if (tab.getSession() != null && tab.getSession().isOpen()) {
                tab.getSession().close();
            }
            TabThumbnailCache.remove(tab.getId());
            if (!tab.isPrivate()) {
                tabRepository.deleteTabBlocking(tab.getId());
            }
        }
        stateStore.closeAll(false);
        stateStore.closeAll(true);
        syncMirrorFromState();
        createDefaultTab();
        if (listener != null) {
            listener.onTabCountChanged(tabs.size());
            listener.onTabChanged(currentTab);
        }
    }

    public void releaseAllSessions() {
        for (Tab tab : stateStore.getAllTabs()) {
            GeckoSession session = tab.getSession();
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public synchronized GeckoSession replaceSessionForTab(Tab tab) {
        if (tab == null || !tabs.contains(tab)) {
            return null;
        }
        GeckoSession oldSession = tab.getSession();
        GeckoSession replacement = createSession(tab.isPrivate());
        replacement.open(runtime);
        tab.setSession(replacement);
        tab.setSessionState(null);
        tab.setInitialLoadPending(false);
        tab.setCanGoBack(false);
        tab.setCanGoForward(false);
        syncMirrorFromState();
        if (!tab.isPrivate()) {
            persistTabs();
        }
        if (oldSession != null && oldSession != replacement && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (Exception e) {
                Log.w(TAG, "Failed to close replaced GeckoSession for tab " + tab.getId(), e);
            }
        }
        if (listener != null) {
            listener.onTabChanged(tab);
            listener.onTabCountChanged(tabs.size());
        }
        return replacement;
    }

    private boolean restoreTabsFromRoom() {
        tabRepository.clearPersistedPrivateStateBlocking();
        List<TabGroup> groups = tabRepository.getGroupsBlocking(false);
        Map<String, TabGroup> groupMap = new HashMap<>();
        for (TabGroup group : groups) {
            groupMap.put(group.getGroupId(), group);
        }
        List<Tab> storedTabs = tabRepository.getAllTabsBlocking(false);
        String currentTabId = prefs.getString(PREF_CURRENT_TAB_ID, null);
        List<Tab> restoredRegularTabs = new ArrayList<>();
        for (Tab storedTab : storedTabs) {
                if (storedTab.isPrivate()) {
                    continue;
                }
                String url = storedTab.getUrl();
                if (url == null || url.trim().isEmpty() || "about:blank".equals(url)) {
                    continue;
                }
                GeckoSession session = createSession(false);
                session.open(runtime);
                Tab runtimeTab = new Tab(storedTab.getId(), session,
                        storedTab.getTitle(), url, false);
                runtimeTab.setGroupId(storedTab.getGroupId());
                TabGroup group = storedTab.getGroupId() != null ? groupMap.get(storedTab.getGroupId()) : null;
                if (group != null) {
                    runtimeTab.setGroupName(group.getGroupName());
                    runtimeTab.setGroupColor(group.getGroupColor());
                }
                runtimeTab.setThumbnailPath(storedTab.getThumbnailPath());
                runtimeTab.setSessionState(storedTab.getSessionState());
                runtimeTab.setCreatedAt(storedTab.getCreatedAt() != 0
                        ? storedTab.getCreatedAt()
                        : storedTab.getLastAccessed());
                runtimeTab.setLastAccessed(storedTab.getLastAccessed());
                runtimeTab.setPosition(storedTab.getPosition());
                runtimeTab.setPinned(storedTab.isPinned());
                runtimeTab.setScrollY(storedTab.getScrollY());
                runtimeTab.setLocked(storedTab.isLocked());
                runtimeTab.setFaviconUri(storedTab.getFaviconUri());
                restoreSessionState(runtimeTab, session);
                restoredRegularTabs.add(runtimeTab);
        }
        if (restoredRegularTabs.isEmpty()) {
            return false;
        }
        if (shouldRestoreLastSessionOnly()) {
            restoredRegularTabs = keepOnlyLastSessionTab(restoredRegularTabs, currentTabId);
            groups = Collections.emptyList();
            currentTabId = restoredRegularTabs.isEmpty() ? null : restoredRegularTabs.get(0).getId();
        }
        stateStore.loadRegularState(restoredRegularTabs, groups, currentTabId);
        syncMirrorFromState();
        return true;
    }

    private boolean restoreLegacyTabs() {
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
            List<Tab> restoredRegularTabs = new ArrayList<>();
            Map<String, TabGroup> restoredGroups = new HashMap<>();
            for (int i = 0; i < saved.length(); i++) {
                JSONObject item = saved.getJSONObject(i);
                String url = item.optString("url", UrlUtils.getNewTabPageUrl(context));
                if (UrlUtils.isInternalPageUrl(url)) {
                    url = UrlUtils.getNewTabPageUrl(context);
                }
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
                tab.setSessionState(sessionState);
                restoreSessionState(tab, session);
                String group = item.optString("group", null);
                if (group != null && !group.isEmpty()) {
                    tab.setGroupName(group);
                    tab.setGroupId(createStableGroupId(group));
                    tab.setGroupColor(colorForGroup(group));
                    if (!restoredGroups.containsKey(tab.getGroupId())) {
                        restoredGroups.put(tab.getGroupId(), new TabGroup(
                                tab.getGroupId(), group, tab.getGroupColor(), System.currentTimeMillis()));
                    }
                }
                String parentId = item.optString("parent_id", null);
                if (parentId != null && !parentId.isEmpty()) tab.setParentTabId(parentId);
                tab.setPinned(item.optBoolean("pinned", false));
                tab.setScrollY(item.optInt("scroll_y", 0));
                tab.setLocked(item.optBoolean("locked", false));
                tab.setPosition(restoredRegularTabs.size());
                restoredRegularTabs.add(tab);
            }
            if (restoredRegularTabs.isEmpty()) {
                return false;
            }
            List<TabGroup> groups = new ArrayList<>(restoredGroups.values());
            if (shouldRestoreLastSessionOnly()) {
                restoredRegularTabs = keepOnlyLastSessionTab(restoredRegularTabs, currentTabId);
                groups = Collections.emptyList();
                currentTabId = restoredRegularTabs.isEmpty() ? null : restoredRegularTabs.get(0).getId();
            }
            stateStore.loadRegularState(restoredRegularTabs,
                    groups, currentTabId);
            syncMirrorFromState();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore tabs, starting fresh", e);
            stateStore.closeAll(false);
            stateStore.closeAll(true);
            syncMirrorFromState();
            return false;
        }
    }

    private void persistTabs() {
        syncMirrorFromState();
        List<Tab> persistableTabs = stateStore.getPersistableTabs();
        tabRepository.saveTabsBlocking(persistableTabs);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_SAVED_TABS);
        BrowserState state = stateStore.snapshot();
        if (state.getActiveRegularTabId() != null) {
            editor.putString(PREF_CURRENT_TAB_ID, state.getActiveRegularTabId());
        } else {
            editor.remove(PREF_CURRENT_TAB_ID);
        }
        editor.apply();
    }

    private void restoreSessionState(Tab tab, GeckoSession session) {
        String sessionState = tab.getSessionState();
        if (sessionState != null && !sessionState.trim().isEmpty()) {
            GeckoSession.SessionState state = null;
            try {
                state = GeckoSession.SessionState.fromString(sessionState);
            } catch (Exception parseError) {
                Log.w(TAG, "Skipping unparseable session state for tab " + tab.getId(), parseError);
            }
            if (state != null) {
                session.restoreState(state);
                tab.setInitialLoadPending(false);
                return;
            }
        }
        tab.setInitialLoadPending(true);
    }

    private List<Tab> keepOnlyLastSessionTab(List<Tab> restoredTabs, String currentTabId) {
        if (restoredTabs == null || restoredTabs.isEmpty()) {
            return Collections.emptyList();
        }
        Tab selected = null;
        if (currentTabId != null) {
            for (Tab tab : restoredTabs) {
                if (currentTabId.equals(tab.getId())) {
                    selected = tab;
                    break;
                }
            }
        }
        if (selected == null) {
            for (Tab tab : restoredTabs) {
                if (selected == null || tab.getLastAccessed() > selected.getLastAccessed()) {
                    selected = tab;
                }
            }
        }
        List<Tab> result = new ArrayList<>();
        if (selected != null) {
            selected.setPosition(0);
            selected.setGroupId(null);
            selected.setGroupName(null);
            selected.setGroupColor(0);
            result.add(selected);
        }
        for (Tab tab : restoredTabs) {
            if (tab != selected && tab.getSession() != null && tab.getSession().isOpen()) {
                tab.getSession().close();
            }
        }
        return result;
    }

    private String createStableGroupId(String groupName) {
        return "group_" + Integer.toHexString(groupName.hashCode());
    }

    private int colorForGroup(String groupName) {
        int[] groupColors = TabRepository.getDefaultGroupColors(context);
        int index = Math.floorMod(groupName.hashCode(), groupColors.length);
        return groupColors[index];
    }

}
