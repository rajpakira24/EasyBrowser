package com.webstudio.easybrowser.ui.activity;

import android.Manifest;
import android.util.Log;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.bumptech.glide.Glide;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.QuickTabStripAdapter;
import com.webstudio.easybrowser.adapters.SuggestionsAdapter;
import com.webstudio.easybrowser.adapters.TabItemTouchHelperCallback;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.AppShortcutManager;
import com.webstudio.easybrowser.managers.AppDownloadManager;
import com.webstudio.easybrowser.managers.BackgroundMediaService;
import com.webstudio.easybrowser.managers.PrivacyStatsManager;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.managers.TabManager;
import com.webstudio.easybrowser.managers.TabThumbnailCache;
import com.webstudio.easybrowser.managers.TabThumbnailManager;
import com.webstudio.easybrowser.models.Bookmark;
import com.webstudio.easybrowser.models.HistoryItem;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.HistoryRepository;
import com.webstudio.easybrowser.repository.QuickAccessRepository;
import com.webstudio.easybrowser.repository.ReadingListRepository;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.models.ReadingListItem;
import com.webstudio.easybrowser.utils.BrowserSuggestionProvider;
import com.webstudio.easybrowser.utils.EasyMotion;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SearchSuggestionProvider;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.TabActionContract;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.MediaSession;
import org.mozilla.geckoview.StorageController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BrowserActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_NEW_TAB = "new_tab";
    public static final String EXTRA_PRIVATE_TAB = "private_tab";
    public static final String EXTRA_NEW_TAB_IN_GROUP = "new_tab_in_group";
    static final int REQUEST_GECKO_PERMISSIONS = 1001;
    private static final long MAX_UPLOAD_CACHE_BYTES = 100L * 1024L * 1024L;
    private static final long TRUSTED_JAVASCRIPT_URI_TTL_MS = 5_000L;
    private static final long FULLSCREEN_EXIT_SEARCH_SUPPRESS_MS = 2_000L;
    private GeckoView geckoView;
    GeckoSession session;
    private BrowserViewModel browserViewModel;
    TabManager tabManager;
    EditText urlInput;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeRefresh;
    private ImageButton securityButton;
    private final Set<String> trustedJavascriptUris =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<GeckoSession> playingMediaSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<GeckoSession, MediaSession> geckoMediaSessions =
            new ConcurrentHashMap<>();
    private final Map<GeckoSession, MediaSession.Metadata> geckoMediaMetadata =
            new ConcurrentHashMap<>();
    private GeckoSession notificationMediaSession;
    private final MediaSession.Delegate mediaSessionDelegate = new MediaSession.Delegate() {
        @Override
        public void onActivated(@NonNull GeckoSession mediaSession,
                                @NonNull MediaSession geckoMediaSession) {
            geckoMediaSessions.put(mediaSession, geckoMediaSession);
            if (notificationMediaSession == null) {
                notificationMediaSession = mediaSession;
            }
        }

        @Override
        public void onMetadata(@NonNull GeckoSession mediaSession,
                               @NonNull MediaSession geckoMediaSession,
                               @NonNull MediaSession.Metadata metadata) {
            geckoMediaSessions.put(mediaSession, geckoMediaSession);
            geckoMediaMetadata.put(mediaSession, metadata);
            if (mediaSession == notificationMediaSession) {
                updateBackgroundMediaNotification(mediaSession,
                        playingMediaSessions.contains(mediaSession));
            }
        }

        @Override
        public void onPlay(@NonNull GeckoSession mediaSession,
                           @NonNull MediaSession geckoMediaSession) {
            updateMediaSessionPlaybackState(mediaSession, geckoMediaSession, true);
        }

        @Override
        public void onPause(@NonNull GeckoSession mediaSession,
                            @NonNull MediaSession geckoMediaSession) {
            handleMediaSessionPaused(mediaSession, geckoMediaSession);
        }

        @Override
        public void onStop(@NonNull GeckoSession mediaSession,
                           @NonNull MediaSession geckoMediaSession) {
            handleMediaSessionEnded(mediaSession);
        }

        @Override
        public void onDeactivated(@NonNull GeckoSession mediaSession,
                                  @NonNull MediaSession geckoMediaSession) {
            handleMediaSessionEnded(mediaSession);
        }
    };
    private ImageButton bookmarkButton;
    private ImageButton toolbarShortcutButton;
    ImageButton backButton;
    private BookmarkRepository bookmarkRepository;
    private HistoryRepository historyRepository;
    private QuickAccessRepository quickAccessRepository;
    String currentUrl;
    String currentTitle;
    private String lastRecordedUrl;
    private boolean isCurrentPageBookmarked = false;
    private BottomNavigationView bottomNav;
    private Toolbar browserToolbar;
    private View quickTabStripContainer;
    private View quickTabStripShadow;
    private RecyclerView quickTabStrip;
    private ImageButton quickTabAdd;
    private QuickTabStripAdapter quickTabStripAdapter;
    private boolean hadExistingTabsOnCreate;
    private int contentScrollY = 0;
    private AppBarLayout appBar;
    private boolean browserChromeVisible = true;
    private boolean inWebFullscreen = false;
    private long suppressSearchPopupUntilMs = 0L;
    private int preFullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean preFullscreenKeepScreenOn = false;
    private int preFullscreenCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
    private Drawable preFullscreenWindowBackground;
    private Drawable preFullscreenGeckoBackground;
    private ViewGroup preFullscreenGeckoParent;
    private ViewGroup.LayoutParams preFullscreenGeckoLayoutParams;
    private int preFullscreenGeckoIndex = -1;
    private boolean addressBarAtBottom = false;
    private boolean bottomNavigationEnabled = true;
    private boolean animateNextTabAttach;
    private int lastChromeScrollY = 0;
    private int accumulatedChromeScrollDelta = 0;
    private static final int CHROME_SCROLL_THRESHOLD_PX = 48;
    private static final int BOTTOM_NAV_FALLBACK_HEIGHT_DP = 58;
    private static final int BOTTOM_ADDRESS_BAR_FALLBACK_HEIGHT_DP = 60;
    private static final int QUICK_TAB_STRIP_FALLBACK_HEIGHT_DP = 56;
    private static final int QUICK_TAB_SHADOW_FALLBACK_HEIGHT_DP = 8;
    private static final long RESUME_REATTACH_DELAY_MS = 220L;
    private static final long RECENT_RESUME_WINDOW_MS = 120_000L;
    private static final long RELOAD_STUCK_TIMEOUT_MS = 8_000L;
    private static final long BACKGROUND_MEDIA_RESUME_DELAY_MS = 450L;
    private static final long USER_MEDIA_PAUSE_SUPPRESSION_MS = 3_000L;
    private View browserRoot;
    private View downloadChipView;
    // One-time "This time search in:" engine override; consumed by the next search submit.
    private String oneTimeSearchEngineUrl;
    // Guards against running the auto-clear-on-exit more than once (Exit menu + onDestroy).
    private boolean autoClearTriggered = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback connectivityCallback;
    private BroadcastReceiver connectivityReceiver;
    private BroadcastReceiver backgroundPlayReceiver;
    private final Handler connectivityHandler = new Handler(Looper.getMainLooper());
    private final Handler browserHandler = new Handler(Looper.getMainLooper());
    private final Handler mediaHandler = new Handler(Looper.getMainLooper());
    private Boolean lastOnlineState;
    private long lastStoppedAtMs;
    private long lastResumedAtMs;
    private long lastProgressChangedAtMs;
    private int lastPageProgress = 100;
    private String pendingReloadRecoveryTabId;
    private boolean browserPaused;
    private boolean browserStopped;
    private long suppressBackgroundAutoResumeUntilMs;
    private final Set<GeckoSession> backgroundResumeAttemptedSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    boolean canGoBack = false;
    boolean canGoForward = false;
    GeckoSession.PermissionDelegate.Callback pendingPermissionCallback;
    GeckoSession.PromptDelegate.FilePrompt pendingFilePrompt;
    GeckoResult<GeckoSession.PromptDelegate.PromptResponse> pendingFileResult;
    BrowserHistoryDelegate historyDelegate;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> tabsActivityLauncher;
    private ActivityResultLauncher<String> downloadPermissionLauncher;
    // url, filename, mimeType awaiting a legacy storage-permission grant (API <= 28).
    private String[] pendingDownloadParams;
    private boolean tabManagerLaunchPending;
    private final SearchSuggestionProvider suggestionProvider = new SearchSuggestionProvider();

    private boolean isDesktopMode = false;

    // F12 — Site info bottom sheet
    GeckoSession.ProgressDelegate.SecurityInformation lastSecurityInfo;
    private SmartShieldLevel smartShieldLevel = SmartShieldLevel.MODERATE;
    private String lastSecurityAlertKey;

    // F9 — User Agent live re-apply
    private String lastAppliedUaPreset = "";

    // F11 — Reading list
    private ReadingListRepository readingListRepository;

    enum SmartShieldLevel {
        SAFE,
        MODERATE,
        WARNING
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        applySystemBars();

        initializeRepositories();
        initializeViewModel();
        initializeViews();
        setupFilePicker();
        setupTabsActivityLauncher();
        setupExtensionInstallPrompt();
        setupUrlInput();
        hadExistingTabsOnCreate = tabManager != null && tabManager.getTabCount() > 0;
        setupTabManager();
        registerBackgroundPlayReceiver();
        attachTabToView(tabManager.getCurrentTab());
        setupBackHandling();
        // Seed lastAppliedUaPreset so the first onResume() doesn't trigger a spurious reload
        lastAppliedUaPreset = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");

        // Handle the incoming URL
        handleIncomingIntent(getIntent(), hadExistingTabsOnCreate);
        showCrashRecoveryMessageIfNeeded();
    }

    private void applySystemBars() {
        int chrome = ThemeEngine.homeChromeColor(this);
        SystemBarUtils.apply(this, chrome, chrome, ThemeEngine.useDarkSystemBarIcons(chrome));
    }

    @Override
    protected void onResume() {
        super.onResume();
        browserPaused = false;
        browserStopped = false;
        backgroundResumeAttemptedSessions.clear();
        lastResumedAtMs = System.currentTimeMillis();
        reattachCurrentSessionAfterResume();
        browserHandler.postDelayed(this::reattachCurrentSessionAfterResume, RESUME_REATTACH_DELAY_MS);
        if (session != null) {
            applyMediaPlaybackPreference(session);
            session.setActive(true);
        }
        startConnectivityMonitoring();
        applyBrowserUiPreferences();
        // F9: Re-apply UA preset if it changed in settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        applyMediaPlaybackPreferenceToTabs();
        String currentPreset = prefs.getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");
        if (!currentPreset.equals(lastAppliedUaPreset) && session != null) {
            lastAppliedUaPreset = currentPreset;
            applyUaPresetToSession(session, prefs);
            reloadCurrentPage();
        }
        if (session != null && currentUrl != null) {
            injectPageUrlSyncIfNeeded(session, currentUrl);
        }
    }

    @Override
    protected void onPause() {
        browserPaused = true;
        applyMediaPlaybackPreferenceToTabs();
        super.onPause();
    }

    @Override
    protected void onStop() {
        browserStopped = true;
        lastStoppedAtMs = System.currentTimeMillis();
        browserHandler.removeCallbacksAndMessages(null);
        stopConnectivityMonitoring();
        if (tabManager != null && session != null) {
            tabManager.updateTabScrollPosition(session, contentScrollY, true);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
        if (session != null) {
            applyMediaPlaybackPreferenceToTabs();
        }
        super.onStop();
    }

    private void reattachCurrentSessionAfterResume() {
        if (isFinishing() || tabManager == null || geckoView == null) {
            return;
        }
        Tab current = tabManager.getCurrentTab();
        if (current == null) {
            current = tabManager.createNewTab(false);
        }
        if (current.getSession() == null || !current.getSession().isOpen()) {
            tabManager.replaceSessionForTab(current);
        }
        attachTabToView(current);
        if (session != null) {
            applyMediaPlaybackPreference(session);
            session.setActive(true);
            applyMediaPlaybackPreferenceToTabs();
        }
        try {
            geckoView.requestNewSurface();
        } catch (Exception e) {
            Log.w("BrowserActivity", "Failed to request GeckoView surface after resume", e);
        }
        if (lastPageProgress >= 100 && swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void reloadCurrentPage() {
        if (tabManager == null) {
            return;
        }
        Tab current = tabManager.getCurrentTab();
        if (current == null) {
            current = tabManager.createNewTab(false);
            attachTabToView(current);
        }
        if (session == null || current.getSession() != session) {
            attachTabToView(current);
        }
        if (session == null) {
            return;
        }
        lastPageProgress = 0;
        lastProgressChangedAtMs = System.currentTimeMillis();
        pendingReloadRecoveryTabId = current.getId();
        session.reload();
        scheduleReloadRecovery(current.getId());
    }

    private void scheduleReloadRecovery(String tabId) {
        browserHandler.removeCallbacksAndMessages(null);
        browserHandler.postDelayed(() -> recoverStuckReloadIfNeeded(tabId), RELOAD_STUCK_TIMEOUT_MS);
    }

    private void recoverStuckReloadIfNeeded(String tabId) {
        if (isFinishing() || tabManager == null || tabId == null
                || !tabId.equals(pendingReloadRecoveryTabId)) {
            return;
        }
        boolean recentlyResumed = lastStoppedAtMs > 0
                && lastResumedAtMs > lastStoppedAtMs
                && System.currentTimeMillis() - lastResumedAtMs <= RECENT_RESUME_WINDOW_MS;
        if (!recentlyResumed || lastPageProgress >= 100) {
            return;
        }
        long idleMs = System.currentTimeMillis() - lastProgressChangedAtMs;
        if (idleMs < RELOAD_STUCK_TIMEOUT_MS - 500L) {
            return;
        }
        recoverCurrentTabSession("reload stuck after resume");
    }

    private void recoverCurrentTabSession(String reason) {
        Tab current = tabManager != null ? tabManager.getCurrentTab() : null;
        if (current == null || geckoView == null) {
            return;
        }
        String url = current.getUrl();
        if (url == null || url.trim().isEmpty() || "about:blank".equals(url)) {
            url = UrlUtils.getNewTabPageUrl(this);
        }
        GeckoSession oldSession = current.getSession();
        try {
            if (geckoView.getSession() == oldSession) {
                geckoView.releaseSession();
            }
        } catch (Exception e) {
            Log.w("BrowserActivity", "Failed to release stale GeckoSession before recovery", e);
        }
        GeckoSession replacement = tabManager.replaceSessionForTab(current);
        if (replacement == null) {
            return;
        }
        Log.w("BrowserActivity", "Recovered GeckoSession for current tab: " + reason);
        session = replacement;
        configureSession(session);
        geckoView.setSession(session);
        session.setActive(true);
        updateUIForTab(current);
        lastPageProgress = 0;
        lastProgressChangedAtMs = System.currentTimeMillis();
        pendingReloadRecoveryTabId = current.getId();
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }
        session.loadUri(url);
    }

    void onPageProgressChanged(@NonNull GeckoSession progressSession, int progress) {
        if (progressSession != session) {
            return;
        }
        lastPageProgress = progress;
        lastProgressChangedAtMs = System.currentTimeMillis();
        if (progress >= 100) {
            if (session == this.session && tabManager != null) {
                Tab currentTab = tabManager.getCurrentTab();
                if (currentTab != null && currentTab.getSession() == session && currentTab.getLoadStartTime() > 0) {
                    long duration = System.currentTimeMillis() - currentTab.getLoadStartTime();
                    AnalyticsManager.logPageLoadFinished(this, currentTab.getUrl(), duration, currentTab.isPrivate());
                    currentTab.setLoadStartTime(0);
                }
            }
            pendingReloadRecoveryTabId = null;
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
        }
    }

    private void showCrashRecoveryMessageIfNeeded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(SettingsKeys.PREF_BROWSER_CRASH_RESTORE_PENDING, false)) {
            return;
        }
        prefs.edit()
                .putBoolean(SettingsKeys.PREF_BROWSER_CRASH_RESTORE_PENDING, false)
                .apply();
        if (tabManager == null || !tabManager.hasRestoredTabs()) {
            return;
        }
        View root = browserRoot != null ? browserRoot : geckoView;
        if (root != null) {
            Snackbar.make(root, R.string.crash_recovery_restored, Snackbar.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.crash_recovery_restored, Toast.LENGTH_LONG).show();
        }
    }

    private void startConnectivityMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        if (lastOnlineState == null) {
            lastOnlineState = isInternetAvailable();
            if (!lastOnlineState) {
                showConnectivityMessage(R.string.network_status_offline);
            }
        }

        if (connectivityCallback == null) {
            connectivityCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    scheduleConnectivityChecks();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    scheduleConnectivityChecks();
                }

                @Override
                public void onUnavailable() {
                    scheduleConnectivityChecks();
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                                                  @NonNull NetworkCapabilities networkCapabilities) {
                    scheduleConnectivityChecks();
                }
            };

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(connectivityCallback);
                } else {
                    android.net.NetworkRequest request = new android.net.NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();
                    connectivityManager.registerNetworkCallback(request, connectivityCallback);
                }
            } catch (Exception ignored) {
                connectivityCallback = null;
            }
        }

        if (connectivityReceiver == null) {
            connectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    scheduleConnectivityChecks();
                }
            };
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(connectivityReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(connectivityReceiver, filter);
            }
        }
    }

    private void registerBackgroundPlayReceiver() {
        if (backgroundPlayReceiver != null) {
            return;
        }
        backgroundPlayReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if (SettingsKeys.ACTION_BACKGROUND_PLAY_CHANGED.equals(action)) {
                    applyMediaPlaybackPreferenceToTabs();
                    if (isBackgroundPlayEnabled() && notificationMediaSession != null) {
                        updateBackgroundMediaNotification(notificationMediaSession,
                                playingMediaSessions.contains(notificationMediaSession));
                    } else {
                        stopBackgroundMediaService();
                    }
                } else if (BackgroundMediaService.ACTION_MEDIA_CONTROL.equals(action)) {
                    handleBackgroundMediaControl(intent.getStringExtra(
                            BackgroundMediaService.EXTRA_COMMAND));
                }
            }
        };
        IntentFilter filter = new IntentFilter(SettingsKeys.ACTION_BACKGROUND_PLAY_CHANGED);
        filter.addAction(BackgroundMediaService.ACTION_MEDIA_CONTROL);
        ContextCompat.registerReceiver(this, backgroundPlayReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void unregisterBackgroundPlayReceiver() {
        if (backgroundPlayReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(backgroundPlayReceiver);
        } catch (Exception ignored) {
        } finally {
            backgroundPlayReceiver = null;
        }
    }

    private void stopConnectivityMonitoring() {
        connectivityHandler.removeCallbacksAndMessages(null);
        if (connectivityManager != null && connectivityCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(connectivityCallback);
            } catch (Exception ignored) {
            } finally {
                connectivityCallback = null;
            }
        }
        if (connectivityReceiver != null) {
            try {
                unregisterReceiver(connectivityReceiver);
            } catch (Exception ignored) {
            } finally {
                connectivityReceiver = null;
            }
        }
    }

    private void scheduleConnectivityChecks() {
        runOnUiThread(() -> {
            checkConnectivityNow();
            connectivityHandler.postDelayed(this::checkConnectivityNow, 300);
            connectivityHandler.postDelayed(this::checkConnectivityNow, 1200);
        });
    }

    private void checkConnectivityNow() {
        handleConnectivityChanged(isInternetAvailable());
    }

    private boolean isInternetAvailable() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (connectivityManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void handleConnectivityChanged(boolean isOnline) {
        if (lastOnlineState != null && lastOnlineState == isOnline) {
            return;
        }
        lastOnlineState = isOnline;
        showConnectivityMessage(isOnline
                ? R.string.network_status_online
                : R.string.network_status_offline);
    }

    private void showConnectivityMessage(int messageResId) {
        View anchorView = browserRoot != null ? browserRoot : geckoView;
        if (anchorView == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(anchorView, messageResId, Snackbar.LENGTH_LONG);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE
                && browserChromeVisible && bottomNav.getTranslationY() == 0f) {
            snackbar.setAnchorView(bottomNav);
        }
        snackbar.show();
    }

    private void showClosedTabUndoSnackbar(TabManager.ClosedTab closedTab) {
        if (!PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsKeys.PREF_SHOW_TAB_UNDO, true)) {
            return;
        }
        View anchorView = browserRoot != null ? browserRoot : geckoView;
        if (anchorView == null || closedTab == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(anchorView,
                getString(R.string.closed_tab_snackbar, getClosedTabLabel(closedTab)),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, v -> {
            Tab restored = tabManager != null ? tabManager.restoreClosedTab(closedTab) : null;
            if (restored != null) {
                attachTabToView(restored);
                updateTabCount();
                updateQuickTabStrip();
            }
        });
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE
                && browserChromeVisible && bottomNav.getTranslationY() == 0f) {
            snackbar.setAnchorView(bottomNav);
        }
        snackbar.show();
    }

    private String getClosedTabLabel(TabManager.ClosedTab closedTab) {
        if (closedTab.title != null && !closedTab.title.trim().isEmpty()) {
            return closedTab.title;
        }
        return getString(R.string.tab_title_default);
    }

    private void initializeViewModel() {
        browserViewModel = new ViewModelProvider(this).get(BrowserViewModel.class);
        tabManager = browserViewModel.getTabManager();
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingFilePrompt == null) {
                        return;
                    }
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        completeFilePrompt(pendingFilePrompt.dismiss());
                        pendingFilePrompt = null;
                        return;
                    }

                    Uri[] pickedUris = extractPickedUris(result.getData());
                    Uri[] fileUris = copyPickedUrisToCache(pickedUris);
                    if (fileUris.length == 0) {
                        completeFilePrompt(pendingFilePrompt.dismiss());
                    } else {
                        completeFilePrompt(pendingFilePrompt.confirm(getApplicationContext(), fileUris));
                    }
                    pendingFilePrompt = null;
                });
        downloadPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    String[] params = pendingDownloadParams;
                    pendingDownloadParams = null;
                    if (params == null) {
                        return;
                    }
                    if (granted) {
                        performStartDownload(params[0], params[1], params[2]);
                    } else {
                        Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupTabsActivityLauncher() {
        tabsActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        handleTabsResult(result.getData());
                    }
                });
    }

    private void initializeRepositories() {
        bookmarkRepository = new BookmarkRepository(this);
        historyRepository = new HistoryRepository(this);
        quickAccessRepository = new QuickAccessRepository(this);
        readingListRepository = new ReadingListRepository(this);
    }

    private void initializeViews() {
        browserToolbar = findViewById(R.id.toolbar);
        appBar = findViewById(R.id.app_bar);
        setSupportActionBar(browserToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        geckoView = findViewById(R.id.gecko_view);
        urlInput = findViewById(R.id.url_input);
        progressBar = findViewById(R.id.progress_bar);
        securityButton = findViewById(R.id.btn_security);
        bookmarkButton = findViewById(R.id.btn_bookmark);
        toolbarShortcutButton = findViewById(R.id.btn_toolbar_shortcut);
        backButton = null;
        swipeRefresh = findViewById(R.id.swipe_refresh);
        browserRoot = findViewById(R.id.browser_root);

        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                contentScrollY > 0 || (geckoView != null && geckoView.canScrollVertically(-1)));
        swipeRefresh.setOnRefreshListener(() -> {
            if (session != null && contentScrollY <= 0) {
                reloadCurrentPage();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        bookmarkButton.setOnClickListener(v -> handleBookmarkButtonClick());
        // F12: Site info bottom sheet
        if (securityButton != null) {
            securityButton.setOnClickListener(v -> handleSecurityButtonClick());
        }
        bottomNav = findViewById(R.id.bottom_navigation);
        quickTabStripContainer = findViewById(R.id.quick_tab_strip_container);
        quickTabStripShadow = findViewById(R.id.quick_tab_strip_shadow);
        quickTabStrip = findViewById(R.id.quick_tab_strip);
        quickTabAdd = findViewById(R.id.quick_tab_add);
        setupBottomNavigation();
        setupQuickTabStrip();
        setupWebContentInsetUpdates();
        applyBrowserUiPreferences();
    }

    private void applyBrowserThemeChrome() {
        ThemeEngine.Palette palette = ThemeEngine.homePalette(this);
        int chrome = ThemeEngine.homeChromeColor(this);
        int foreground = ThemeEngine.foregroundFor(chrome);
        SystemBarUtils.apply(this, chrome, chrome, ThemeEngine.useDarkSystemBarIcons(chrome));
        if (browserToolbar != null) {
            browserToolbar.setBackgroundColor(chrome);
            browserToolbar.setTitleTextColor(foreground);
        }
        if (appBar != null) {
            appBar.setBackgroundColor(chrome);
        }

        tintChromeButton(backButton, foreground);
        tintChromeButton(quickTabAdd, foreground);
        updateSmartShieldIndicator();

        int urlForeground = ContextCompat.getColor(this, R.color.browser_url_foreground);
        tintChromeButton(bookmarkButton, urlForeground);
        tintChromeButton(toolbarShortcutButton, urlForeground);

        if (securityButton != null && securityButton.getParent() instanceof View) {
            View searchContainer = (View) securityButton.getParent();
            searchContainer.setBackground(createChromeRoundedDrawable(
                    ContextCompat.getColor(this, R.color.browser_url_background),
                    ContextCompat.getColor(this, R.color.browser_url_stroke),
                    dp(12)));
        }
        if (urlInput != null) {
            urlInput.setTextColor(urlForeground);
            urlInput.setHintTextColor(ContextCompat.getColor(this, R.color.browser_url_hint));
        }
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(chrome);
            ColorStateList navColors = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{
                            palette.accent,
                            foreground
                    });
            bottomNav.setItemIconTintList(navColors);
            bottomNav.setItemTextColor(navColors);
            bottomNav.setItemRippleColor(ColorStateList.valueOf(palette.accentSoft));
        }
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(palette.accent);
        }
    }

    private void tintChromeButton(ImageButton button, int color) {
        if (button != null) {
            button.setColorFilter(color);
        }
    }

    private GradientDrawable createChromeRoundedDrawable(int color, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private void handleIncomingIntent(Intent intent, boolean preserveExistingTabWithoutUrl) {
        if (intent != null) {
            String action = intent.getAction();
            String legacyAction = intent.getStringExtra("action");
            String url = sanitizeIncomingIntentUrl(intent.getStringExtra(EXTRA_URL));
            if ((AppShortcutManager.ACTION_NEW_PRIVATE_TAB.equals(action)
                    || "private_tab".equals(legacyAction)
                    || intent.getBooleanExtra(EXTRA_PRIVATE_TAB, false))
                    && tabManager != null) {
                if (url != null && !url.isEmpty()) {
                    openUrlInNewTab(url, true);
                } else {
                    animateNextTabAttach = true;
                    attachTabToView(tabManager.createNewTab(true));
                }
                return;
            }
            if ((AppShortcutManager.ACTION_NEW_TAB.equals(action)
                    || "new_tab".equals(legacyAction))
                    && tabManager != null) {
                openShortcutHomepageTab();
                return;
            }
            if (intent.getBooleanExtra(EXTRA_NEW_TAB_IN_GROUP, false) && tabManager != null) {
                if (url != null && !url.isEmpty()) {
                    openUrlInNewTabInGroup(url);
                } else {
                    attachTabToView(tabManager.getCurrentTab());
                }
                return;
            }
            if (intent.getBooleanExtra(EXTRA_NEW_TAB, false) && tabManager != null) {
                animateNextTabAttach = true;
                attachTabToView(tabManager.createNewTab(false, url == null || url.isEmpty()));
                if (url == null || url.isEmpty()) {
                    return;
                }
            }
            if (url != null && !url.isEmpty()) {
                loadUrl(url);
            } else if (preserveExistingTabWithoutUrl) {
                attachTabToView(tabManager.getCurrentTab());
            } else if (tabManager != null && tabManager.hasRestoredTabs()) {
                attachTabToView(tabManager.getCurrentTab());
            } else {
                loadUrl(UrlUtils.getNewTabPageUrl(this));
            }
            if (intent.getBooleanExtra("show_tabs", false)) {
                launchTabsActivity();
            }
        }
    }

    private void openShortcutHomepageTab() {
        if (tabManager == null) {
            return;
        }
        animateNextTabAttach = true;
        attachTabToView(tabManager.createNewTab(false, false));
        loadUrl(UrlUtils.getHomepageUrl(this));
    }

    /**
     * Drop intent-extra URLs whose scheme isn't http(s). Other apps can hand us a
     * "url" extra via ACTION_VIEW; allowing javascript:/file:/data: there would let
     * a third-party app execute script or read local files in the browser context.
     */
    private String sanitizeIncomingIntentUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }

    private void setupUrlInput() {
        urlInput.setShowSoftInputOnFocus(false);
        urlInput.setCursorVisible(false);
        urlInput.setOnClickListener(v -> showSearchPopup());
        urlInput.setOnFocusChangeListener(null);
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleUrlInput(urlInput.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void showSearchPopup() {
        if (inWebFullscreen || isSearchPopupSuppressed()) {
            hideKeyboard();
            return;
        }
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.dialog_search_popup);
        dialog.setCanceledOnTouchOutside(true);

        EditText searchInput = dialog.findViewById(R.id.search_popup_input);
        ImageButton popupMic = dialog.findViewById(R.id.btn_search_popup_mic);
        ImageButton popupGo = dialog.findViewById(R.id.btn_search_popup_go);
        ImageButton popupEngine = dialog.findViewById(R.id.btn_search_popup_engine);
        RecyclerView suggestionsRecycler = dialog.findViewById(R.id.suggestions_recycler);
        popupMic.setVisibility(View.GONE);
        if (popupEngine != null) {
            updatePopupSearchEngineIcon(popupEngine);
            popupEngine.setOnClickListener(v ->
                    showSearchEnginePickerForPopup(popupEngine, searchInput, dialog));
        }
        bindCurrentPageSearchCard(dialog, searchInput);

        SuggestionsAdapter suggestionsAdapter = new SuggestionsAdapter(suggestion -> {
            handleUrlInput(suggestion);
            dialog.dismiss();
        });
        suggestionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecycler.setAdapter(suggestionsAdapter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int[] suggestionRequest = {0};
        searchInput.setText("");
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                int requestId = ++suggestionRequest[0];
                if (query.isEmpty()) {
                    suggestionsRecycler.setVisibility(View.GONE);
                    return;
                }
                boolean privateMode = isCurrentTabPrivate();
                boolean browserSuggestions = prefs.getBoolean(
                        SettingsKeys.PREF_BROWSER_SUGGESTIONS_ENABLED, true);
                boolean searchSuggestions = !privateMode && prefs.getBoolean(
                        SettingsKeys.PREF_SEARCH_SUGGESTIONS_ENABLED, false);
                if (!browserSuggestions && !searchSuggestions) {
                    suggestionsRecycler.setVisibility(View.GONE);
                    return;
                }

                class SuggestionRequest {
                    void show(List<String> suggestions) {
                        if (requestId != suggestionRequest[0]) {
                            return;
                        }
                        if (suggestions == null || suggestions.isEmpty()) {
                            suggestionsRecycler.setVisibility(View.GONE);
                        } else {
                            suggestionsAdapter.setSuggestions(suggestions);
                            suggestionsRecycler.setVisibility(View.VISIBLE);
                        }
                    }

                    void fetchSearch(List<String> browserResults) {
                        String searchEngine = oneTimeSearchEngineUrl != null
                                ? oneTimeSearchEngineUrl
                                : UrlUtils.getSearchEngineUrl(BrowserActivity.this, privateMode);
                        suggestionProvider.fetchSuggestions(query, searchEngine,
                                searchResults -> runOnUiThread(() -> show(
                                        mergeSuggestions(browserResults, searchResults))));
                    }
                }

                SuggestionRequest request = new SuggestionRequest();
                if (browserSuggestions) {
                    BrowserSuggestionProvider.fetchSuggestions(BrowserActivity.this, query,
                            browserResults -> runOnUiThread(() -> {
                                request.show(browserResults);
                                if (searchSuggestions) {
                                    request.fetchSearch(browserResults);
                                }
                            }));
                } else if (searchSuggestions) {
                    request.fetchSearch(Collections.emptyList());
                }
            }
        });

        popupGo.setOnClickListener(v -> {
            handleUrlInput(searchInput.getText().toString());
            dialog.dismiss();
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleUrlInput(searchInput.getText().toString());
                dialog.dismiss();
                return true;
            }
            return false;
        });

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.TRANSPARENT));
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.TOP);
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.dimAmount = 0.28f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    attributes.setBlurBehindRadius(32);
                }
                window.setAttributes(attributes);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                window.setWindowAnimations(R.style.SearchPopupAnimation);
            }
            searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.setOnDismissListener(d -> urlInput.clearFocus());
        dialog.show();
    }

    private void updatePopupSearchEngineIcon(ImageButton target) {
        if (target == null) {
            return;
        }
        String engineUrl = oneTimeSearchEngineUrl != null
                ? oneTimeSearchEngineUrl
                : UrlUtils.getSearchEngineUrl(this, isCurrentTabPrivate());
        Drawable fallback = ContextCompat.getDrawable(this, R.drawable.ic_search);
        if (fallback != null) {
            fallback = fallback.mutate();
            fallback.setTint(ContextCompat.getColor(this, R.color.edge_search_foreground));
        }
        target.setImageTintList(null);
        target.clearColorFilter();
        target.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(target)
                .load(UrlUtils.getEngineIconUrl(engineUrl))
                .circleCrop()
                .placeholder(fallback)
                .error(fallback)
                .into(target);
    }

    private void bindCurrentPageSearchCard(Dialog dialog, EditText searchInput) {
        View card = dialog.findViewById(R.id.current_page_card);
        if (card == null) {
            return;
        }
        if (!hasCurrentWebPage()) {
            card.setVisibility(View.GONE);
            return;
        }

        ImageView icon = dialog.findViewById(R.id.current_page_icon);
        TextView title = dialog.findViewById(R.id.current_page_title);
        TextView host = dialog.findViewById(R.id.current_page_host);
        ImageButton share = dialog.findViewById(R.id.current_page_share);
        ImageButton copy = dialog.findViewById(R.id.current_page_copy);
        ImageButton edit = dialog.findViewById(R.id.current_page_edit);

        String displayHost = UrlUtils.getDisplayHost(currentUrl);
        String displayTitle = !TextUtils.isEmpty(currentTitle)
                ? currentTitle
                : (!TextUtils.isEmpty(displayHost) ? displayHost : currentUrl);
        title.setText(displayTitle);
        host.setText(!TextUtils.isEmpty(displayHost) ? displayHost : currentUrl);
        bindCurrentPageIcon(icon);

        share.setOnClickListener(v -> {
            dialog.dismiss();
            shareCurrentPage();
        });
        copy.setOnClickListener(v -> {
            copyToClipboard(currentUrl);
            dialog.dismiss();
        });
        edit.setOnClickListener(v -> {
            searchInput.setText(getEditableUrlText());
            searchInput.selectAll();
            card.setVisibility(View.GONE);
        });
        card.setVisibility(View.VISIBLE);
    }

    private List<String> mergeSuggestions(List<String> primary, List<String> secondary) {
        List<String> merged = new ArrayList<>();
        addUniqueSuggestions(merged, primary);
        addUniqueSuggestions(merged, secondary);
        return merged.size() > 8 ? new ArrayList<>(merged.subList(0, 8)) : merged;
    }

    private void addUniqueSuggestions(List<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String suggestion : source) {
            if (suggestion != null && !suggestion.trim().isEmpty()
                    && !target.contains(suggestion)) {
                target.add(suggestion);
            }
        }
    }

    private boolean hasCurrentWebPage() {
        return currentUrl != null
                && !currentUrl.trim().isEmpty()
                && !"about:blank".equals(currentUrl)
                && !UrlUtils.isInternalPageUrl(currentUrl);
    }

    private void bindCurrentPageIcon(ImageView icon) {
        if (icon == null) {
            return;
        }
        Tab tab = tabManager != null ? tabManager.getCurrentTab() : null;
        if (tab != null && tab.getFavicon() != null) {
            Glide.with(icon).clear(icon);
            icon.setImageTintList(null);
            icon.setImageBitmap(tab.getFavicon());
            return;
        }

        String faviconUrl = tab != null && !TextUtils.isEmpty(tab.getFaviconUri())
                ? tab.getFaviconUri()
                : UrlUtils.getFaviconUrl(currentUrl);
        if (!TextUtils.isEmpty(faviconUrl)) {
            icon.setImageTintList(null);
            Glide.with(icon)
                    .load(faviconUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(icon);
            return;
        }

        Glide.with(icon).clear(icon);
        icon.setImageTintList(null);
        icon.setImageResource(R.mipmap.ic_launcher);
    }

    private void hideKeyboard() {
        if (urlInput == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        }
        urlInput.clearFocus();
    }

    private void suppressSearchPopupAfterFullscreenExit() {
        suppressSearchPopupUntilMs =
                SystemClock.uptimeMillis() + FULLSCREEN_EXIT_SEARCH_SUPPRESS_MS;
    }

    private boolean isSearchPopupSuppressed() {
        return SystemClock.uptimeMillis() < suppressSearchPopupUntilMs;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent, true);
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (inWebFullscreen && session != null) {
                    suppressSearchPopupAfterFullscreenExit();
                    session.exitFullScreen();
                } else if (session != null && canGoBack) {
                    session.goBack();
                } else if (shouldCloseCurrentTabOnBack()) {
                    TabManager.ClosedTab closedTab =
                            tabManager.closeCurrentTabAndSwitchToPreviousForUndo();
                    if (closedTab != null) {
                        updateTabCount();
                        updateQuickTabStrip();
                        showClosedTabUndoSnackbar(closedTab);
                    } else if (tabManager != null && tabManager.getCurrentTab() != null
                            && tabManager.getCurrentTab().isLocked()) {
                        Toast.makeText(BrowserActivity.this,
                                R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
                    } else if (tabManager != null && tabManager.switchToPreviousTab()) {
                        updateTabCount();
                    } else {
                        moveTaskToBack(true);
                    }
                } else if (tabManager != null && tabManager.switchToPreviousTab()) {
                    updateTabCount();
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private boolean shouldCloseCurrentTabOnBack() {
        if (tabManager == null) {
            return false;
        }
        Tab currentTab = tabManager.getCurrentTab();
        return currentTab != null && currentTab.shouldCloseOnBackToPreviousTab();
    }

    private void setupTabManager() {
        TabManager.OnTabChangeListener listener = new TabManager.OnTabChangeListener() {
            @Override
            public void onTabChanged(Tab tab) {
                attachTabToView(tab);
                updateQuickTabStrip();
            }

            @Override
            public void onTabCountChanged(int count) {
                updateTabCount();
                updateQuickTabStrip();
            }

            @Override
            public void onTabMetadataChanged(Tab tab) {
                updateQuickTabStrip();
            }
        };

        tabManager.setOnTabChangeListener(listener);
        updateTabCount();
    }

    private void attachTabToView(Tab tab) {
        if (tab == null || geckoView == null) {
            return;
        }
        // A tab discarded under memory pressure has no session — recreate it lazily.
        if (tab.getSession() == null && tabManager != null) {
            tabManager.ensureSessionForTab(tab);
        }
        if (tab.getSession() == null) {
            return;
        }
        showBrowserChrome(false);
        contentScrollY = tab.getScrollY();
        lastChromeScrollY = contentScrollY;
        accumulatedChromeScrollDelta = 0;
        session = tab.getSession();
        ScreenshotProtection.apply(this);
        session.getSettings().setAllowJavascript(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("javascript_enabled", true));
        applyMediaPlaybackPreference(session);
        configureSession(session);
        geckoView.setSession(session);
        applyMediaPlaybackPreferenceToTabs();
        updateUIForTab(tab);
        updateQuickTabStrip();
        if (tab.isInitialLoadPending()) {
            String tabUrl = tab.getUrl();
            tab.setInitialLoadPending(false);
            if (tabUrl != null && !tabUrl.trim().isEmpty() && !"about:blank".equals(tabUrl)) {
                session.loadUri(tabUrl);
            }
        }
        if (animateNextTabAttach) {
            animateNextTabAttach = false;
            EasyMotion.animateTabOpen(swipeRefresh);
            EasyMotion.pulse(quickTabAdd);
        }
    }

    private void configureSession(GeckoSession targetSession) {
        targetSession.setNavigationDelegate(new BrowserNavigationDelegate(this));
        targetSession.setContentDelegate(new BrowserContentDelegate(this));
        targetSession.setContentBlockingDelegate(new BrowserContentBlockingDelegate(this));
        targetSession.setProgressDelegate(new BrowserProgressDelegate(this));
        targetSession.setScrollDelegate(new GeckoSession.ScrollDelegate() {
            @Override
            public void onScrollChanged(@NonNull GeckoSession scrollSession,
                                        int scrollX, int scrollY) {
                if (scrollSession == session) {
                    contentScrollY = Math.max(0, scrollY);
                    if (tabManager != null) {
                        tabManager.updateTabScrollPosition(scrollSession, contentScrollY, false);
                    }
                    handleBrowserChromeForScroll(contentScrollY);
                }
            }
        });
        targetSession.setPromptDelegate(new BrowserPromptDelegate(this));
        targetSession.setPermissionDelegate(new BrowserPermissionDelegate(this));
        targetSession.setMediaSessionDelegate(mediaSessionDelegate);
        historyDelegate = new BrowserHistoryDelegate(this);
        targetSession.setHistoryDelegate(historyDelegate);
    }

    // Drop every delegate set in configureSession so a session can't pin this Activity.
    // Critical on rotation: BrowserViewModel keeps background-tab sessions alive across
    // Activity recreation, and only the current tab is re-configured by attachTabToView —
    // background sessions would otherwise hold delegates referencing the destroyed Activity.
    private void clearSessionDelegates(GeckoSession targetSession) {
        if (targetSession == null) {
            return;
        }
        try {
            targetSession.setNavigationDelegate(null);
            targetSession.setContentDelegate(null);
            targetSession.setContentBlockingDelegate(null);
            targetSession.setProgressDelegate(null);
            targetSession.setScrollDelegate(null);
            targetSession.setPromptDelegate(null);
            targetSession.setPermissionDelegate(null);
            targetSession.setMediaSessionDelegate(null);
            targetSession.setHistoryDelegate(null);
        } catch (RuntimeException ignored) {
        }
    }

    // GeckoView asks us to host a brand-new session (window.open() with an opener handle,
    // OAuth/payment popups). Must return an UNOPENED session; GeckoView opens it.
    GeckoResult<GeckoSession> onContentNewSession(GeckoSession opener, String uri) {
        if (tabManager == null) {
            return GeckoResult.fromValue(null);
        }
        boolean blockPopups = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("block_popups", true);
        if (blockPopups) {
            recordBlockedPrivacyItem();
            runOnUiThread(() ->
                    Toast.makeText(this, R.string.popup_blocked, Toast.LENGTH_SHORT).show());
            return GeckoResult.fromValue(null);
        }
        Tab tab = tabManager.adoptNewSession(opener);
        if (tab == null || tab.getSession() == null) {
            return GeckoResult.fromValue(null);
        }
        // Gate the popup's load (ad-block, HTTPS-only, external-scheme handling) from the
        // start, even though it opens in the background and isn't attached to the GeckoView.
        configureSession(tab.getSession());
        runOnUiThread(this::updateTabCount);
        return GeckoResult.fromValue(tab.getSession());
    }

    void onSessionStateChanged(@NonNull GeckoSession stateSession,
                               @NonNull GeckoSession.SessionState state) {
        if (tabManager != null) {
            tabManager.updateTabSessionState(stateSession, state.toString());
        }
    }

    private void toggleBookmark() {
        if (currentUrl == null || currentUrl.trim().isEmpty()
                || "about:blank".equals(currentUrl)
                || UrlUtils.isInternalPageUrl(currentUrl)) {
            return;
        }
        if (isCurrentPageBookmarked) {
            // Remove bookmark
            bookmarkRepository.getAllBookmarks(new BookmarkRepository.BookmarkCallback() {
                @Override
                public void onBookmarksLoaded(List<Bookmark> bookmarks) {
                    for (Bookmark bookmark : bookmarks) {
                        if (bookmark.getUrl().equals(currentUrl)) {
                            bookmarkRepository.removeBookmark(bookmark, this);
                            break;
                        }
                    }
                }

                @Override
                public void onBookmarkAdded(Bookmark bookmark) {
                }

                @Override
                public void onBookmarkRemoved(Bookmark bookmark) {
                    runOnUiThread(() -> {
                        isCurrentPageBookmarked = false;
                        updateBookmarkIcon();
                        Toast.makeText(BrowserActivity.this,
                                R.string.bookmark_removed_message,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // Add bookmark
            Bookmark newBookmark = new Bookmark(currentTitle, currentUrl);
            bookmarkRepository.addBookmark(newBookmark, new BookmarkRepository.BookmarkCallback() {
                @Override
                public void onBookmarksLoaded(List<Bookmark> bookmarks) {
                }

                @Override
                public void onBookmarkAdded(Bookmark bookmark) {
                    runOnUiThread(() -> {
                        AnalyticsManager.logBookmarkAdded(BrowserActivity.this, currentUrl);
                        isCurrentPageBookmarked = true;
                        updateBookmarkIcon();
                        Toast.makeText(BrowserActivity.this,
                                R.string.bookmark_added_message,
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onBookmarkRemoved(Bookmark bookmark) {
                }
            });
        }
    }

    void updateBookmarkStatus() {
        if (currentUrl == null || currentUrl.trim().isEmpty()
                || "about:blank".equals(currentUrl)
                || UrlUtils.isInternalPageUrl(currentUrl)) {
            isCurrentPageBookmarked = false;
            updateBookmarkIcon();
            return;
        }

        bookmarkRepository.getAllBookmarks(new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {
                isCurrentPageBookmarked = false;
                for (Bookmark bookmark : bookmarks) {
                    if (bookmark.getUrl() != null && bookmark.getUrl().equals(currentUrl)) {
                        isCurrentPageBookmarked = true;
                        break;
                    }
                }
                runOnUiThread(() -> updateBookmarkIcon());
            }

            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
            }

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {
            }
        });
    }

    private void handleUrlInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        input = input.trim();

        if (tabManager.getCurrentTab() == null) {
            tabManager.createNewTab(false);
        }

        Tab tab = tabManager.getCurrentTab();
        if (UrlUtils.isSearchQuery(input)) {
            boolean isPrivate = tab != null && tab.isPrivate();
            AnalyticsManager.logSearchQuery(this, input,
                    UrlUtils.getSearchEngineName(this, UrlUtils.getSearchUrl(this, input)),
                    isPrivate);
        }
        String target;
        if (oneTimeSearchEngineUrl != null && UrlUtils.isSearchQuery(input)) {
            target = UrlUtils.getSearchUrlForEngine(this, input, oneTimeSearchEngineUrl);
        } else {
            target = UrlUtils.getUrlOrSearchUrl(this, input, tab != null && tab.isPrivate());
        }
        oneTimeSearchEngineUrl = null;
        loadUrl(target);
    }

    private void showSearchEnginePickerForPopup(View anchor, EditText searchInput, Dialog dialog) {
        SearchEnginePickerPopup.show(this, anchor, new SearchEnginePickerPopup.Callback() {
            @Override
            public void onEngineSelected(String name, String engineUrl) {
                oneTimeSearchEngineUrl = engineUrl;
                String input = searchInput.getText() != null
                        ? searchInput.getText().toString().trim() : "";
                if (!input.isEmpty()) {
                    handleUrlInput(input);
                    dialog.dismiss();
                } else {
                    if (anchor instanceof ImageButton) {
                        updatePopupSearchEngineIcon((ImageButton) anchor);
                    }
                    searchInput.requestFocus();
                    Toast.makeText(BrowserActivity.this,
                            getString(R.string.search_picker_title) + " " + name,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBookmarks() {
                dialog.dismiss();
                startActivity(new Intent(BrowserActivity.this, BookmarksActivity.class));
            }

            @Override
            public void onTabs() {
                dialog.dismiss();
                launchTabsActivity();
            }

            @Override
            public void onHistory() {
                dialog.dismiss();
                startActivity(new Intent(BrowserActivity.this, HistoryActivity.class));
            }

            @Override
            public void onSearchSettings() {
                dialog.dismiss();
                startActivity(new Intent(BrowserActivity.this, SettingsActivity.class));
            }
        });
    }

    void loadUrl(String url) {
        if (tabManager == null) {
            return;
        }
        if (tabManager.getCurrentTab() == null) {
            attachTabToView(tabManager.createNewTab(false));
        }
        Tab currentTab = tabManager.getCurrentTab();
        if (currentTab != null && currentTab.getSession() != null) {
            url = UrlUtils.sanitizeUrl(url);
            lastSecurityInfo = null;
            resetSecurityAlertState();
            updateUrlInputForUrl(url);
            currentUrl = url;
            updateSmartShieldIndicator();
            currentTab.setInitialLoadPending(false);
            tabManager.updateTabUrl(currentTab, url);
            attachTabToView(currentTab);
            currentTab.setLoadStartTime(System.currentTimeMillis());
            AnalyticsManager.logPageLoadStarted(this, url, currentTab.isPrivate());
            currentTab.getSession().loadUri(url);
        }
    }

    private void updateUIForTab(Tab tab) {
        if (tab != null) {
            updateUrlInputForUrl(tab.getUrl());
            currentUrl = tab.getUrl();
            currentTitle = tab.getTitle();
            canGoBack = tab.canGoBack();
            canGoForward = tab.canGoForward();
            invalidateOptionsMenu();

            updateSmartShieldIndicator();
            updateBookmarkStatus();
        }
    }

    void updateUrlInputForUrl(String url) {
        if (urlInput != null) {
            urlInput.setText(getStyledToolbarDisplayUrl(url));
        }
    }

    void syncUrlFromPageScript(GeckoSession sourceSession, String url) {
        if (sourceSession == null || TextUtils.isEmpty(url) || !isHttpUrl(url)) {
            return;
        }
        String sanitizedUrl = UrlUtils.sanitizeUrl(url);
        runOnUiThread(() -> {
            if (tabManager == null) {
                return;
            }
            Tab tab = tabManager.findTabBySession(sourceSession);
            if (tab == null) {
                return;
            }
            // Anti-spoof: the URL arrives through a document.title channel, which a
            // hostile page could set to our sentinel with a forged address. But
            // history.pushState/replaceState are same-origin only, so a legitimate
            // in-page URL change can never cross host. Reject any reported URL whose
            // host differs from the tab's committed host (set by onLocationChange).
            String reportedHost = UrlUtils.getDisplayHost(sanitizedUrl);
            String committedHost = UrlUtils.getDisplayHost(tab.getUrl());
            if (reportedHost == null || committedHost == null
                    || !reportedHost.equals(committedHost)) {
                return;
            }
            tabManager.updateTabUrl(tab, sanitizedUrl);
            if (tab == tabManager.getCurrentTab()) {
                currentUrl = sanitizedUrl;
                updateUrlInputForUrl(sanitizedUrl);
                updateSmartShieldIndicator();
                updateBookmarkStatus();
            }
        });
    }

    /** True for top-level web pages where in-page (SPA) URL sync is safe to run. */
    private boolean isHttpUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            String scheme = Uri.parse(url).getScheme();
            if (scheme == null) {
                return false;
            }
            scheme = scheme.toLowerCase(Locale.US);
            return scheme.equals("http") || scheme.equals("https");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isBackgroundPlayEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsKeys.PREF_BACKGROUND_PLAY_ENABLED, false);
    }

    private void applyMediaPlaybackPreference(GeckoSession targetSession) {
        applyMediaPlaybackPreference(targetSession, isBackgroundPlayEnabled());
    }

    private void applyMediaPlaybackPreference(GeckoSession targetSession,
                                              boolean backgroundPlayEnabled) {
        if (targetSession == null) {
            return;
        }
        try {
            targetSession.getSettings().setSuspendMediaWhenInactive(!backgroundPlayEnabled);
        } catch (RuntimeException ignored) {
        }
    }

    private void applyMediaPlaybackPreferenceToTabs() {
        if (tabManager == null) {
            return;
        }
        boolean backgroundPlayEnabled = isBackgroundPlayEnabled();
        List<GeckoSession> openSessions = new ArrayList<>();
        for (Tab tab : tabManager.getTabs()) {
            if (tab != null && tab.getSession() != null) {
                GeckoSession tabSession = tab.getSession();
                openSessions.add(tabSession);
                applyMediaPlaybackPreference(tabSession, backgroundPlayEnabled);
                try {
                    tabSession.setActive(shouldKeepSessionActive(tabSession,
                            backgroundPlayEnabled));
                } catch (RuntimeException ignored) {
                }
            }
        }
        playingMediaSessions.retainAll(openSessions);
        geckoMediaSessions.keySet().retainAll(openSessions);
        geckoMediaMetadata.keySet().retainAll(openSessions);
        if (notificationMediaSession != null && !openSessions.contains(notificationMediaSession)) {
            notificationMediaSession = null;
        }
        if (!backgroundPlayEnabled) {
            stopBackgroundMediaService();
        }
    }

    private boolean shouldKeepSessionActive(GeckoSession targetSession,
                                            boolean backgroundPlayEnabled) {
        if (targetSession == null) {
            return false;
        }
        if (!backgroundPlayEnabled) {
            return !browserStopped && targetSession == session;
        }
        return targetSession == session || playingMediaSessions.contains(targetSession);
    }

    private void handleMediaSessionPaused(@NonNull GeckoSession mediaSession,
                                          @NonNull MediaSession geckoMediaSession) {
        boolean shouldAutoResume = shouldAutoResumeBackgroundPause(mediaSession);
        updateMediaSessionPlaybackState(mediaSession, geckoMediaSession, false);
        if (shouldAutoResume) {
            scheduleBackgroundMediaResume(mediaSession, geckoMediaSession);
        }
    }

    private boolean shouldAutoResumeBackgroundPause(@NonNull GeckoSession mediaSession) {
        return isBackgroundPlayEnabled()
                && (browserPaused || browserStopped)
                && !isFinishing()
                && !isDestroyed()
                && System.currentTimeMillis() > suppressBackgroundAutoResumeUntilMs
                && playingMediaSessions.contains(mediaSession)
                && backgroundResumeAttemptedSessions.add(mediaSession);
    }

    private void scheduleBackgroundMediaResume(@NonNull GeckoSession mediaSession,
                                               @NonNull MediaSession geckoMediaSession) {
        runOnUiThread(() -> mediaHandler.postDelayed(() ->
                        resumeBackgroundMediaIfNeeded(mediaSession, geckoMediaSession),
                BACKGROUND_MEDIA_RESUME_DELAY_MS));
    }

    private void resumeBackgroundMediaIfNeeded(@NonNull GeckoSession mediaSession,
                                               @NonNull MediaSession geckoMediaSession) {
        if (!isBackgroundPlayEnabled()
                || !(browserPaused || browserStopped)
                || isFinishing()
                || isDestroyed()
                || !geckoMediaSessions.containsKey(mediaSession)) {
            return;
        }
        try {
            applyMediaPlaybackPreference(mediaSession, true);
            mediaSession.setActive(true);
            geckoMediaSession.play();
        } catch (RuntimeException e) {
            Log.w("BrowserActivity", "Failed to resume background media", e);
        }
    }

    private void updateMediaSessionPlaybackState(@NonNull GeckoSession mediaSession,
                                                 @NonNull MediaSession geckoMediaSession,
                                                 boolean playing) {
        geckoMediaSessions.put(mediaSession, geckoMediaSession);
        if (playing) {
            playingMediaSessions.add(mediaSession);
            notificationMediaSession = mediaSession;
        } else {
            playingMediaSessions.remove(mediaSession);
            if (notificationMediaSession == null) {
                notificationMediaSession = mediaSession;
            }
        }
        runOnUiThread(() -> {
            applyMediaPlaybackPreferenceToTabs();
            if (notificationMediaSession == mediaSession) {
                updateBackgroundMediaNotification(mediaSession, playing);
            }
        });
    }

    private void handleMediaSessionEnded(@NonNull GeckoSession mediaSession) {
        playingMediaSessions.remove(mediaSession);
        geckoMediaSessions.remove(mediaSession);
        geckoMediaMetadata.remove(mediaSession);
        runOnUiThread(() -> {
            applyMediaPlaybackPreferenceToTabs();
            if (mediaSession != notificationMediaSession) {
                return;
            }
            notificationMediaSession = findPlayingMediaSession();
            if (notificationMediaSession != null) {
                updateBackgroundMediaNotification(notificationMediaSession, true);
            } else {
                stopBackgroundMediaService();
            }
        });
    }

    private GeckoSession findPlayingMediaSession() {
        for (GeckoSession playingSession : playingMediaSessions) {
            if (geckoMediaSessions.containsKey(playingSession)) {
                return playingSession;
            }
        }
        return null;
    }

    private void updateBackgroundMediaNotification(GeckoSession mediaSession,
                                                   boolean playing) {
        if (!isBackgroundPlayEnabled() || mediaSession == null) {
            stopBackgroundMediaService();
            return;
        }
        notificationMediaSession = mediaSession;
        Intent intent = BackgroundMediaService.createUpdateIntent(
                this,
                resolveBackgroundMediaTitle(mediaSession),
                resolveBackgroundMediaArtist(mediaSession),
                playing);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent);
            } else {
                startService(intent);
            }
        } catch (RuntimeException e) {
            Log.w("BrowserActivity", "Failed to start background media service", e);
        }
    }

    private void stopBackgroundMediaService() {
        try {
            stopService(new Intent(this, BackgroundMediaService.class));
        } catch (RuntimeException ignored) {
        }
    }

    private String resolveBackgroundMediaTitle(GeckoSession mediaSession) {
        MediaSession.Metadata metadata = geckoMediaMetadata.get(mediaSession);
        if (metadata != null && !TextUtils.isEmpty(metadata.title)) {
            return metadata.title;
        }
        Tab tab = tabManager != null ? tabManager.findTabBySession(mediaSession) : null;
        if (tab != null && !TextUtils.isEmpty(tab.getTitle())) {
            return tab.getTitle();
        }
        if (!TextUtils.isEmpty(currentTitle)) {
            return currentTitle;
        }
        return getString(R.string.app_name);
    }

    private String resolveBackgroundMediaArtist(GeckoSession mediaSession) {
        MediaSession.Metadata metadata = geckoMediaMetadata.get(mediaSession);
        if (metadata != null) {
            if (!TextUtils.isEmpty(metadata.artist)) {
                return metadata.artist;
            }
            if (!TextUtils.isEmpty(metadata.album)) {
                return metadata.album;
            }
        }
        Tab tab = tabManager != null ? tabManager.findTabBySession(mediaSession) : null;
        String host = tab != null ? UrlUtils.getDisplayHost(tab.getUrl()) : null;
        return !TextUtils.isEmpty(host) ? host : getString(R.string.app_name);
    }

    private void handleBackgroundMediaControl(String command) {
        MediaSession mediaController = getBackgroundMediaController();
        if (mediaController == null || TextUtils.isEmpty(command)) {
            return;
        }
        if (BackgroundMediaService.COMMAND_PLAY.equals(command)) {
            suppressBackgroundAutoResumeUntilMs = 0L;
            backgroundResumeAttemptedSessions.clear();
            mediaController.play();
        } else if (BackgroundMediaService.COMMAND_PAUSE.equals(command)) {
            suppressBackgroundAutoResumeUntilMs =
                    System.currentTimeMillis() + USER_MEDIA_PAUSE_SUPPRESSION_MS;
            mediaController.pause();
        } else if (BackgroundMediaService.COMMAND_STOP.equals(command)) {
            suppressBackgroundAutoResumeUntilMs =
                    System.currentTimeMillis() + USER_MEDIA_PAUSE_SUPPRESSION_MS;
            mediaController.stop();
            stopBackgroundMediaService();
        }
    }

    private MediaSession getBackgroundMediaController() {
        if (notificationMediaSession != null) {
            MediaSession mediaController = geckoMediaSessions.get(notificationMediaSession);
            if (mediaController != null) {
                return mediaController;
            }
        }
        if (session != null) {
            MediaSession mediaController = geckoMediaSessions.get(session);
            if (mediaController != null) {
                return mediaController;
            }
        }
        for (MediaSession mediaController : geckoMediaSessions.values()) {
            return mediaController;
        }
        return null;
    }

    void onPageSecurityChanged(GeckoSession.ProgressDelegate.SecurityInformation securityInfo) {
        lastSecurityInfo = securityInfo;
        updateSmartShieldIndicator();
    }

    void resetSecurityAlertState() {
        lastSecurityAlertKey = null;
    }

    void updateSmartShieldIndicator() {
        if (securityButton == null) {
            return;
        }
        smartShieldLevel = resolveSmartShieldLevel(currentUrl);
        int colorRes;
        int descriptionRes;
        if (smartShieldLevel == SmartShieldLevel.SAFE) {
            colorRes = R.color.smart_shield_safe;
            descriptionRes = R.string.smart_shield_safe;
            securityButton.setImageResource(R.drawable.ic_security);
        } else if (smartShieldLevel == SmartShieldLevel.MODERATE) {
            colorRes = R.color.smart_shield_moderate;
            descriptionRes = R.string.smart_shield_moderate;
            securityButton.setImageResource(R.drawable.ic_security);
        } else {
            colorRes = R.color.smart_shield_warning;
            descriptionRes = R.string.smart_shield_warning;
            securityButton.setImageResource(R.drawable.ic_security_warning);
        }
        securityButton.setColorFilter(ContextCompat.getColor(this, colorRes));
        securityButton.setContentDescription(getString(descriptionRes));
        maybeShowSecurityAlert();
    }

    private void maybeShowSecurityAlert() {
        if (smartShieldLevel != SmartShieldLevel.WARNING
                || currentUrl == null
                || currentUrl.trim().isEmpty()
                || UrlUtils.isInternalPageUrl(currentUrl)) {
            return;
        }
        String alertKey = currentUrl + ":"
                + (lastSecurityInfo != null && !lastSecurityInfo.isSecure
                ? "certificate" : "connection");
        if (alertKey.equals(lastSecurityAlertKey)) {
            return;
        }
        lastSecurityAlertKey = alertKey;
        View anchor = browserRoot != null ? browserRoot : findViewById(android.R.id.content);
        if (anchor == null) {
            return;
        }
        Snackbar.make(anchor, getSecurityAlertMessageRes(), Snackbar.LENGTH_LONG)
                .setAction(R.string.site_info, v -> showSiteInfoBottomSheet())
                .show();
    }

    private int getSecurityAlertMessageRes() {
        String lower = currentUrl != null ? currentUrl.toLowerCase(Locale.US) : "";
        if (lower.startsWith("https://") && lastSecurityInfo != null && !lastSecurityInfo.isSecure) {
            return R.string.security_alert_certificate;
        }
        return R.string.security_alert_not_secure;
    }

    private SmartShieldLevel resolveSmartShieldLevel(String url) {
        if (url == null || url.trim().isEmpty()
                || "about:blank".equals(url)
                || UrlUtils.isInternalPageUrl(url)) {
            return SmartShieldLevel.MODERATE;
        }
        String lower = url.trim().toLowerCase(Locale.US);
        if (lower.startsWith("http://")) {
            return SmartShieldLevel.WARNING;
        }
        if (lower.startsWith("https://")) {
            if (lastSecurityInfo != null && !lastSecurityInfo.isSecure) {
                return SmartShieldLevel.WARNING;
            }
            return PrivacyStatsManager.isProtectionEnabled(this)
                    ? SmartShieldLevel.SAFE
                    : SmartShieldLevel.MODERATE;
        }
        return SmartShieldLevel.WARNING;
    }

    private int calculateSmartShieldScore() {
        SmartShieldLevel level = resolveSmartShieldLevel(currentUrl);
        int score;
        if (level == SmartShieldLevel.SAFE) {
            score = 92;
        } else if (level == SmartShieldLevel.MODERATE) {
            score = 68;
        } else {
            score = 35;
        }
        if (!PrivacyStatsManager.isProtectionEnabled(this)) {
            score -= 12;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String getToolbarDisplayUrl(String url) {
        if (url == null || url.trim().isEmpty()
                || "about:blank".equals(url)
                || UrlUtils.isInternalPageUrl(url)) {
            return "";
        }
        if (shouldShowDetailedToolbarUrl(url)) {
            return stripSchemeForToolbar(url);
        }
        String host = UrlUtils.getDisplayHost(url);
        return host != null ? host : url;
    }

    private CharSequence getStyledToolbarDisplayUrl(String url) {
        String displayUrl = getToolbarDisplayUrl(url);
        if (TextUtils.isEmpty(displayUrl) || !shouldShowDetailedToolbarUrl(url)) {
            return displayUrl;
        }
        int fadeStart = getToolbarUrlDetailStart(displayUrl);
        if (fadeStart <= 0 || fadeStart >= displayUrl.length()) {
            return displayUrl;
        }
        SpannableString styledUrl = new SpannableString(displayUrl);
        int fadedColor = fadedToolbarUrlColor();
        styledUrl.setSpan(new ForegroundColorSpan(fadedColor),
                fadeStart, displayUrl.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styledUrl;
    }

    private int getToolbarUrlDetailStart(String displayUrl) {
        if (TextUtils.isEmpty(displayUrl)) {
            return -1;
        }
        int slash = displayUrl.indexOf('/');
        int query = displayUrl.indexOf('?');
        int fragment = displayUrl.indexOf('#');
        int start = -1;
        if (slash >= 0) {
            start = slash;
        }
        if (query >= 0 && (start < 0 || query < start)) {
            start = query;
        }
        if (fragment >= 0 && (start < 0 || fragment < start)) {
            start = fragment;
        }
        return start;
    }

    private int fadedToolbarUrlColor() {
        int foreground = ContextCompat.getColor(this, R.color.browser_url_foreground);
        return Color.argb(Math.round(Color.alpha(foreground) * 0.54f),
                Color.red(foreground),
                Color.green(foreground),
                Color.blue(foreground));
    }

    private boolean shouldShowDetailedToolbarUrl(String url) {
        if (!isHttpUrl(url)) {
            return false;
        }
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            String query = uri.getQuery();
            String fragment = uri.getFragment();
            return (path != null && !path.isEmpty() && !"/".equals(path))
                    || (query != null && !query.isEmpty())
                    || (fragment != null && !fragment.isEmpty());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String stripSchemeForToolbar(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (TextUtils.isEmpty(host)) {
                return url;
            }
            StringBuilder builder = new StringBuilder(host);
            String path = uri.getEncodedPath();
            if (path != null) {
                builder.append(path);
            }
            String query = uri.getEncodedQuery();
            if (!TextUtils.isEmpty(query)) {
                builder.append('?').append(query);
            }
            String fragment = uri.getEncodedFragment();
            if (!TextUtils.isEmpty(fragment)) {
                builder.append('#').append(fragment);
            }
            return builder.toString();
        } catch (RuntimeException ignored) {
            return url;
        }
    }

    private boolean isYouTubeUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            String host = Uri.parse(url).getHost();
            if (host == null) {
                return false;
            }
            host = host.toLowerCase(Locale.US);
            return "youtube.com".equals(host)
                    || host.endsWith(".youtube.com")
                    || "youtu.be".equals(host)
                    || host.endsWith(".youtu.be");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String getEditableUrlText() {
        if (currentUrl == null || currentUrl.trim().isEmpty()
                || "about:blank".equals(currentUrl)
                || UrlUtils.isInternalPageUrl(currentUrl)) {
            return urlInput != null ? urlInput.getText().toString() : "";
        }
        return currentUrl;
    }

    private void updateTabCount() {
        if (bottomNav == null) {
            return;
        }
        MenuItem tabsItem = bottomNav.getMenu().findItem(R.id.nav_tabs);
        if (tabsItem != null) {
            if (tabManager != null) {
                Tab active = tabManager.getCurrentTab();
                boolean privateMode = active != null && active.isPrivate();
                int count = tabManager.getTabCount(privateMode);
                bottomNav.removeBadge(R.id.nav_tabs);
                tabsItem.setIcon(createTabCountIcon(count));
                bottomNav.post(() -> {
                    Tab latestActive = tabManager.getCurrentTab();
                    boolean latestPrivateMode = latestActive != null && latestActive.isPrivate();
                    tabsItem.setIcon(createTabCountIcon(tabManager.getTabCount(latestPrivateMode)));
                });
                updateQuickTabStrip();
            }
        }
    }

    private void launchTabsActivity() {
        if (tabManager == null || tabsActivityLauncher == null) {
            return;
        }
        if (tabManagerLaunchPending) {
            return;
        }
        tabManagerLaunchPending = true;
        captureCurrentTabThumbnail(() -> {
            tabManagerLaunchPending = false;
            launchTabsActivityWithCurrentState();
        });
    }

    private void launchTabsActivityWithCurrentState() {
        if (tabManager == null || tabsActivityLauncher == null) {
            return;
        }
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> groups = new ArrayList<>();
        ArrayList<String> groupIds = new ArrayList<>();
        ArrayList<String> thumbnailPaths = new ArrayList<>();
        ArrayList<String> faviconUris = new ArrayList<>();
        ArrayList<String> parentIds = new ArrayList<>();
        boolean[] privateStates = new boolean[tabManager.getTabCount()];
        boolean[] pinnedStates = new boolean[tabManager.getTabCount()];
        boolean[] lockedStates = new boolean[tabManager.getTabCount()];
        int[] groupColors = new int[tabManager.getTabCount()];
        int[] positions = new int[tabManager.getTabCount()];
        long[] createdAt = new long[tabManager.getTabCount()];
        long[] lastAccessed = new long[tabManager.getTabCount()];
        List<Tab> tabs = tabManager.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            ids.add(tab.getId());
            titles.add(tab.getTitle());
            urls.add(tab.getUrl());
            groups.add(tab.getGroupName() != null ? tab.getGroupName() : "");
            groupIds.add(tab.getGroupId() != null ? tab.getGroupId() : "");
            thumbnailPaths.add(tab.getThumbnailPath() != null ? tab.getThumbnailPath() : "");
            faviconUris.add(tab.getFaviconUri() != null ? tab.getFaviconUri() : "");
            parentIds.add(tab.getParentTabId() != null ? tab.getParentTabId() : "");
            privateStates[i] = tab.isPrivate();
            pinnedStates[i] = tab.isPinned();
            lockedStates[i] = tab.isLocked();
            groupColors[i] = tab.getGroupColor();
            positions[i] = tab.getPosition();
            createdAt[i] = tab.getCreatedAt();
            lastAccessed[i] = tab.getLastAccessed();
        }
        String currentTabId = tabManager.getCurrentTab() != null
                ? tabManager.getCurrentTab().getId()
                : null;
        ArrayList<String> closedTitles = new ArrayList<>();
        ArrayList<String> closedUrls = new ArrayList<>();
        for (TabManager.ClosedTab ct : tabManager.getRecentlyClosed()) {
            closedTitles.add(ct.title != null ? ct.title : "");
            closedUrls.add(ct.url);
        }
        Intent intent = new Intent(this, TabManagerActivity.class)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_IDS, ids)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_TITLES, titles)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_URLS, urls)
                .putExtra(TabsActivity.EXTRA_TAB_PRIVATE_STATES, privateStates)
                .putExtra(TabsActivity.EXTRA_TAB_PINNED_STATES, pinnedStates)
                .putExtra(TabsActivity.EXTRA_TAB_LOCKED_STATES, lockedStates)
                .putExtra(TabsActivity.EXTRA_CURRENT_TAB_ID, currentTabId)
                .putStringArrayListExtra(TabsActivity.EXTRA_CLOSED_TAB_TITLES, closedTitles)
                .putStringArrayListExtra(TabsActivity.EXTRA_CLOSED_TAB_URLS, closedUrls)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUPS, groups)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_GROUP_IDS, groupIds)
                .putExtra(TabsActivity.EXTRA_TAB_GROUP_COLORS, groupColors)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_THUMBNAILS, thumbnailPaths)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_FAVICONS, faviconUris)
                .putExtra(TabsActivity.EXTRA_TAB_POSITIONS, positions)
                .putExtra(TabsActivity.EXTRA_TAB_CREATED_AT, createdAt)
                .putExtra(TabsActivity.EXTRA_TAB_LAST_ACCESSED, lastAccessed)
                .putStringArrayListExtra(TabsActivity.EXTRA_TAB_PARENT_IDS, parentIds);
        tabsActivityLauncher.launch(intent);
    }

    private void captureCurrentTabThumbnail() {
        captureCurrentTabThumbnail(null);
    }

    private void captureCurrentTabThumbnail(Runnable onComplete) {
        if (tabManager == null || geckoView == null) {
            finishThumbnailCapture(onComplete);
            return;
        }
        Tab cur = tabManager.getCurrentTab();
        if (cur == null || cur.getSession() == null || cur.isPrivate()) {
            finishThumbnailCapture(onComplete);
            return;
        }
        String tabId = cur.getId();
        try {
            geckoView.capturePixels().then(bitmap -> {
                String thumbnailPath = null;
                if (bitmap != null) {
                    TabThumbnailCache.put(tabId, bitmap);
                    thumbnailPath = TabThumbnailManager.saveThumbnail(
                            getApplicationContext(), tabId, bitmap);
                }
                final String savedPath = thumbnailPath;
                runOnUiThread(() -> {
                    if (savedPath != null && tabManager != null) {
                        tabManager.updateTabThumbnail(tabId, savedPath);
                    }
                    finishThumbnailCapture(onComplete);
                });
                return GeckoResult.fromValue(null);
            }, error -> {
                runOnUiThread(() -> finishThumbnailCapture(onComplete));
                return GeckoResult.fromValue(null);
            });
        } catch (Exception ignored) {
            finishThumbnailCapture(onComplete);
        }
    }

    private void finishThumbnailCapture(Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void handleTabsResult(Intent data) {
        if (tabManager == null) {
            return;
        }
        if (handleTabActionResult(data.getStringExtra(TabActionContract.EXTRA_ACTIONS))) {
            return;
        }
        ArrayList<String> closedIds = data.getStringArrayListExtra(TabsActivity.RESULT_CLOSED_TAB_IDS);
        if (closedIds != null) {
            for (String closedId : closedIds) {
                closeTabById(closedId);
            }
        }
        ArrayList<String> restoredInactiveIds =
                data.getStringArrayListExtra(InactiveTabsActivity.RESULT_RESTORED_TAB_IDS);
        if (restoredInactiveIds != null && !restoredInactiveIds.isEmpty()) {
            tabManager.touchTabs(restoredInactiveIds);
        }
        if (data.hasExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB)) {
            String resultGroupId = data.getStringExtra(GroupTabsActivity.EXTRA_GROUP_ID);
            if (resultGroupId != null) {
                createNewTabInGroup(
                        data.getBooleanExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, false),
                        resultGroupId,
                        data.getStringExtra(GroupTabsActivity.EXTRA_GROUP_NAME),
                        data.getIntExtra(GroupTabsActivity.EXTRA_GROUP_COLOR,
                                TabRepository.getDefaultGroupColor(this)));
            } else {
                createNewTab(data.getBooleanExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, false));
            }
            return;
        }
        String restoreUrl = data.getStringExtra(TabsActivity.RESULT_RESTORE_URL);
        if (restoreUrl != null) {
            openUrlInNewTab(restoreUrl, data.getBooleanExtra(TabsActivity.RESULT_RESTORE_PRIVATE, false));
            updateTabCount();
            return;
        }
        applyCreatedPrivateGroups(data);
        applyTabGroupMutations(data);
        applyPrivateTabReorder(data);
        if (data.getBooleanExtra(TabManagerActivity.RESULT_GROUPS_CHANGED, false)) {
            tabManager.refreshMetadataFromRepository();
        }
        applyTabPinMutations(data);
        applyTabLockMutations(data);
        String selectedTabId = data.getStringExtra(TabsActivity.RESULT_SELECTED_TAB_ID);
        if (selectedTabId != null) {
            switchToTabById(selectedTabId);
        } else {
            attachTabToView(tabManager.getCurrentTab());
        }
        updateTabCount();
    }

    private boolean handleTabActionResult(String payload) {
        List<TabActionContract.Action> actions = TabActionContract.parse(payload);
        if (actions.isEmpty()) {
            return false;
        }
        String selectedTabId = null;
        boolean attachCurrentTab = true;
        boolean updateQuickTabs = false;
        for (TabActionContract.Action action : actions) {
            String type = action.getType();
            if (TabActionContract.TYPE_CLOSE_TABS.equals(type)) {
                for (String tabId : action.getTabIds()) {
                    closeTabById(tabId);
                }
            } else if (TabActionContract.TYPE_RESTORE_INACTIVE_TABS.equals(type)) {
                List<String> tabIds = action.getTabIds();
                if (!tabIds.isEmpty()) {
                    tabManager.touchTabs(tabIds);
                }
            } else if (TabActionContract.TYPE_CREATE_TAB.equals(type)) {
                String groupId = action.getGroupId();
                if (!groupId.trim().isEmpty()) {
                    createNewTabInGroup(action.isPrivate(),
                            groupId,
                            action.getGroupName(),
                            action.getGroupColor(TabRepository.getDefaultGroupColor(this)));
                } else {
                    createNewTab(action.isPrivate());
                }
                attachCurrentTab = false;
            } else if (TabActionContract.TYPE_RESTORE_URL.equals(type)) {
                String url = action.getUrl();
                if (!url.trim().isEmpty()) {
                    openUrlInNewTab(url, action.isPrivate());
                    attachCurrentTab = false;
                }
            } else if (TabActionContract.TYPE_CREATE_PRIVATE_GROUP.equals(type)) {
                List<String> tabIds = action.getTabIds();
                if (tabIds.size() >= 2) {
                    tabManager.createGroupForTabs(tabIds,
                            action.getGroupName(),
                            action.getGroupColor(TabRepository.getDefaultGroupColor(this)),
                            true,
                            action.getGroupId());
                    updateQuickTabs = true;
                }
            } else if (TabActionContract.TYPE_SET_GROUP.equals(type)) {
                Tab tab = findTabById(action.getTabId());
                if (tab != null) {
                    String groupId = action.getGroupId();
                    if (groupId.trim().isEmpty()) {
                        tabManager.removeTabFromGroup(tab);
                    } else {
                        tabManager.addTabToGroup(tab, groupId,
                                action.getGroupName(),
                                action.getGroupColor(TabRepository.getDefaultGroupColor(this)));
                    }
                    updateQuickTabs = true;
                }
            } else if (TabActionContract.TYPE_REORDER_PRIVATE_TABS.equals(type)) {
                List<Tab> orderedTabs = new ArrayList<>();
                for (String tabId : action.getTabIds()) {
                    Tab tab = findTabById(tabId);
                    if (tab != null && tab.isPrivate()) {
                        orderedTabs.add(tab);
                    }
                }
                if (orderedTabs.size() >= 2) {
                    tabManager.reorderTabs(orderedTabs);
                    updateQuickTabs = true;
                }
            } else if (TabActionContract.TYPE_SET_PINNED.equals(type)) {
                for (String tabId : action.getTabIds()) {
                    tabManager.setTabPinned(tabId, action.isPinned());
                }
                updateQuickTabs = updateQuickTabs || !action.getTabIds().isEmpty();
            } else if (TabActionContract.TYPE_SET_LOCKED.equals(type)) {
                for (String tabId : action.getTabIds()) {
                    tabManager.setTabLocked(tabId, action.isLocked());
                }
                updateQuickTabs = updateQuickTabs || !action.getTabIds().isEmpty();
            } else if (TabActionContract.TYPE_GROUPS_CHANGED.equals(type)) {
                tabManager.refreshMetadataFromRepository();
            } else if (TabActionContract.TYPE_SELECT_TAB.equals(type)) {
                selectedTabId = action.getTabId();
            }
        }
        if (updateQuickTabs) {
            updateQuickTabStrip();
        }
        if (selectedTabId != null && !selectedTabId.trim().isEmpty()) {
            switchToTabById(selectedTabId);
        } else if (attachCurrentTab) {
            attachTabToView(tabManager.getCurrentTab());
        }
        updateTabCount();
        return true;
    }

    private void applyTabPinMutations(Intent data) {
        ArrayList<String> pinnedIds =
                data.getStringArrayListExtra(TabManagerActivity.RESULT_PINNED_TAB_IDS);
        if (pinnedIds != null) {
            for (String tabId : pinnedIds) {
                tabManager.setTabPinned(tabId, true);
            }
        }
        ArrayList<String> unpinnedIds =
                data.getStringArrayListExtra(TabManagerActivity.RESULT_UNPINNED_TAB_IDS);
        if (unpinnedIds != null) {
            for (String tabId : unpinnedIds) {
                tabManager.setTabPinned(tabId, false);
            }
        }
        if ((pinnedIds != null && !pinnedIds.isEmpty())
                || (unpinnedIds != null && !unpinnedIds.isEmpty())) {
            updateQuickTabStrip();
        }
    }

    private void applyTabLockMutations(Intent data) {
        ArrayList<String> lockedIds =
                data.getStringArrayListExtra(TabManagerActivity.RESULT_LOCKED_TAB_IDS);
        if (lockedIds != null) {
            for (String tabId : lockedIds) {
                tabManager.setTabLocked(tabId, true);
            }
        }
        ArrayList<String> unlockedIds =
                data.getStringArrayListExtra(TabManagerActivity.RESULT_UNLOCKED_TAB_IDS);
        if (unlockedIds != null) {
            for (String tabId : unlockedIds) {
                tabManager.setTabLocked(tabId, false);
            }
        }
        if ((lockedIds != null && !lockedIds.isEmpty())
                || (unlockedIds != null && !unlockedIds.isEmpty())) {
            updateQuickTabStrip();
        }
    }

    private void applyCreatedPrivateGroups(Intent data) {
        ArrayList<String> groupIds = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_IDS);
        ArrayList<String> tabIds = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_TAB_IDS);
        ArrayList<Integer> tabCounts = data.getIntegerArrayListExtra(
                TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_TAB_COUNTS);
        ArrayList<String> groupNames = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_NAMES);
        ArrayList<Integer> groupColors = data.getIntegerArrayListExtra(
                TabManagerActivity.RESULT_CREATED_PRIVATE_GROUP_COLORS);
        if (groupIds == null || tabIds == null || tabCounts == null
                || groupNames == null || groupColors == null) {
            return;
        }
        int cursor = 0;
        int count = Math.min(Math.min(groupIds.size(), tabCounts.size()),
                Math.min(groupNames.size(), groupColors.size()));
        for (int i = 0; i < count; i++) {
            int tabCount = Math.max(0, tabCounts.get(i));
            if (cursor + tabCount > tabIds.size()) {
                break;
            }
            ArrayList<String> groupTabIds = new ArrayList<>();
            for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                groupTabIds.add(tabIds.get(cursor + tabIndex));
            }
            cursor += tabCount;
            tabManager.createGroupForTabs(groupTabIds,
                    groupNames.get(i),
                    groupColors.get(i),
                    true,
                    groupIds.get(i));
        }
        updateQuickTabStrip();
    }

    private void applyPrivateTabReorder(Intent data) {
        ArrayList<String> tabIds = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_REORDERED_PRIVATE_TAB_IDS);
        if (tabIds == null || tabIds.size() < 2) {
            return;
        }
        List<Tab> orderedTabs = new ArrayList<>();
        for (String tabId : tabIds) {
            Tab tab = findTabById(tabId);
            if (tab != null && tab.isPrivate()) {
                orderedTabs.add(tab);
            }
        }
        if (orderedTabs.size() >= 2) {
            tabManager.reorderTabs(orderedTabs);
            updateQuickTabStrip();
        }
    }

    private void applyTabGroupMutations(Intent data) {
        ArrayList<String> tabIds = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_GROUP_MUTATION_TAB_IDS);
        ArrayList<String> groupIds = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_IDS);
        ArrayList<String> groupNames = data.getStringArrayListExtra(
                TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_NAMES);
        ArrayList<Integer> groupColors = data.getIntegerArrayListExtra(
                TabManagerActivity.RESULT_GROUP_MUTATION_GROUP_COLORS);
        if (tabIds == null || groupIds == null || groupNames == null || groupColors == null) {
            return;
        }
        int count = Math.min(Math.min(tabIds.size(), groupIds.size()),
                Math.min(groupNames.size(), groupColors.size()));
        for (int i = 0; i < count; i++) {
            Tab tab = findTabById(tabIds.get(i));
            if (tab == null) {
                continue;
            }
            String groupId = groupIds.get(i);
            if (groupId == null || groupId.trim().isEmpty()) {
                tabManager.removeTabFromGroup(tab);
            } else {
                tabManager.addTabToGroup(tab, groupId, groupNames.get(i), groupColors.get(i));
            }
        }
        updateQuickTabStrip();
    }

    private List<Tab> getActiveGroupTabs() {
        List<Tab> groupTabs = new ArrayList<>();
        if (tabManager == null || tabManager.getCurrentTab() == null) {
            return groupTabs;
        }
        String groupId = tabManager.getCurrentTab().getGroupId();
        if (groupId == null) {
            return groupTabs;
        }
        for (Tab tab : tabManager.getTabs()) {
            if (groupId.equals(tab.getGroupId())) {
                groupTabs.add(tab);
            }
        }
        Collections.sort(groupTabs, (first, second) -> {
            if (first.isPinned() != second.isPinned()) {
                return first.isPinned() ? -1 : 1;
            }
            return Integer.compare(first.getPosition(), second.getPosition());
        });
        return groupTabs;
    }

    private void closeTabById(String tabId) {
        for (Tab tab : tabManager.getTabs()) {
            if (tab.getId().equals(tabId)) {
                tabManager.closeTab(tab);
                return;
            }
        }
    }

    private Tab findTabById(String tabId) {
        if (tabId == null || tabManager == null) {
            return null;
        }
        for (Tab tab : tabManager.getTabs()) {
            if (tabId.equals(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    private void closeTabWithUndo(Tab tab) {
        if (tab == null || tabManager == null) {
            return;
        }
        if (tab.isLocked()) {
            Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        EasyMotion.animateTabCloseThenOpen(swipeRefresh, () -> {
            TabManager.ClosedTab closedTab = tabManager.closeTab(tab);
            if (closedTab == null) {
                Toast.makeText(this, R.string.locked_tab_close_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            attachTabToView(tabManager.getCurrentTab());
            updateTabCount();
            updateQuickTabStrip();
            showClosedTabUndoSnackbar(closedTab);
        });
    }

    private void switchToTabById(String tabId) {
        for (Tab tab : tabManager.getTabs()) {
            if (tab.getId().equals(tabId)) {
                captureCurrentTabThumbnail();
                tabManager.switchToTab(tab);
                return;
            }
        }
    }

    private Drawable createTabCountIcon(int count) {
        int size = dp(28);
        int strokeWidth = dp(2);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int iconColor = ThemeEngine.foregroundFor(ThemeEngine.homeChromeColor(this));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(iconColor);
        float inset = strokeWidth / 2f + dp(3);
        canvas.drawRoundRect(inset, inset, size - inset, size - inset, dp(4), dp(4), paint);

        String label = count > 99 ? "99+" : String.valueOf(count);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(count > 99 ? dp(8) : dp(11));
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float y = size / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, size / 2f, y, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateBookmarkIcon() {
        if (isInternalOrBlankPage(currentUrl)) {
            bookmarkButton.setImageResource(R.drawable.ic_bookmarks);
            bookmarkButton.setContentDescription(getString(R.string.bookmarks));
        } else if (isCurrentPageBookmarked) {
            bookmarkButton.setImageResource(R.drawable.ic_bookmarks);
            bookmarkButton.setContentDescription(getString(R.string.bookmark));
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_border);
            bookmarkButton.setContentDescription(getString(R.string.bookmark));
        }
    }

    private void handleBookmarkButtonClick() {
        if (isInternalOrBlankPage(currentUrl)) {
            startActivity(new Intent(this, BookmarksActivity.class));
            return;
        }
        toggleBookmark();
    }

    private boolean isInternalOrBlankPage(String url) {
        return url == null || url.trim().isEmpty()
                || "about:blank".equals(url)
                || UrlUtils.isInternalPageUrl(url);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            EasyMotion.animateBottomBarSelection(bottomNav, itemId);
            if (itemId == R.id.nav_home) {
                loadUrl(UrlUtils.getNewTabPageUrl(this));
                return true;
            } else if (itemId == R.id.nav_bookmarks) {
                startActivity(new Intent(this, BookmarksActivity.class));
                return true;
            } else if (itemId == R.id.nav_search) {
                showSearchPopup();
                return true;
            } else if (itemId == R.id.nav_tabs) {
                launchTabsActivity();
                return true;
            } else if (itemId == R.id.nav_settings) {
                showMoreMenu();
                return true;
            }
            return false;
        });

        // Update initial tab count
        updateTabCount();
    }

    private void applyBrowserUiPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        bottomNavigationEnabled = prefs.getBoolean(
                SettingsKeys.PREF_BOTTOM_NAVIGATION_ENABLED, true);
        addressBarAtBottom = "bottom".equals(
                prefs.getString(SettingsKeys.PREF_ADDRESS_BAR_POSITION, "top"));

        if (bottomNav != null) {
            bottomNav.setVisibility(bottomNavigationEnabled ? View.VISIBLE : View.GONE);
        }
        applyAddressBarPosition(addressBarAtBottom, bottomNavigationEnabled);
        applyQuickTabStripMargins(addressBarAtBottom, bottomNavigationEnabled);
        updateToolbarShortcutButton(prefs, bottomNavigationEnabled);
        applyBrowserThemeChrome();
    }

    private void applyAddressBarPosition(boolean bottom, boolean bottomNavEnabled) {
        if (appBar != null && appBar.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams appBarParams =
                    (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
            appBarParams.gravity = bottom ? Gravity.BOTTOM : Gravity.TOP;
            appBarParams.bottomMargin = bottom && bottomNavEnabled ? getVisibleBottomNavHeight() : 0;
            appBar.setLayoutParams(appBarParams);
            appBar.setElevation(dp(6));
            appBar.bringToFront();
            bringBottomChromeToFront();
        }

        if (swipeRefresh != null
                && swipeRefresh.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams contentParams =
                    (CoordinatorLayout.LayoutParams) swipeRefresh.getLayoutParams();
            if (bottom) {
                contentParams.setBehavior(null);
                contentParams.topMargin = 0;
                contentParams.bottomMargin = 0;
            } else {
                contentParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
                contentParams.topMargin = 0;
            }
            swipeRefresh.setLayoutParams(contentParams);
        }
        requestWebContentBottomInsetUpdate();
    }

    private void applyQuickTabStripMargins(boolean addressBarBottom, boolean bottomNavEnabled) {
        int bottomBase = bottomNavEnabled ? getVisibleBottomNavHeight() : 0;
        int addressBarSpace = addressBarBottom ? getBottomAddressBarHeight() : 0;
        int stripHeight = getQuickTabStripHeight();
        setBottomMargin(quickTabStripContainer, bottomBase + addressBarSpace);
        setBottomMargin(quickTabStripShadow, bottomBase + addressBarSpace + stripHeight);
        requestWebContentBottomInsetUpdate();
    }

    private int getQuickTabStripHeight() {
        if (quickTabStripContainer != null && quickTabStripContainer.getHeight() > 0) {
            return quickTabStripContainer.getHeight();
        }
        return dp(QUICK_TAB_STRIP_FALLBACK_HEIGHT_DP);
    }

    private void setupWebContentInsetUpdates() {
        View.OnLayoutChangeListener insetListener = (v, left, top, right, bottom,
                                                     oldLeft, oldTop, oldRight, oldBottom) ->
                requestWebContentBottomInsetUpdate();
        if (bottomNav != null) {
            bottomNav.addOnLayoutChangeListener(insetListener);
        }
        if (appBar != null) {
            appBar.addOnLayoutChangeListener(insetListener);
        }
        if (quickTabStripContainer != null) {
            quickTabStripContainer.addOnLayoutChangeListener(insetListener);
        }
        if (quickTabStripShadow != null) {
            quickTabStripShadow.addOnLayoutChangeListener(insetListener);
        }
        requestWebContentBottomInsetUpdate();
    }

    private void requestWebContentBottomInsetUpdate() {
        if (swipeRefresh == null) {
            return;
        }
        swipeRefresh.post(this::updateWebContentBottomInset);
    }

    private void requestWebContentBottomInsetUpdateAfter(long delayMs) {
        if (swipeRefresh == null) {
            return;
        }
        swipeRefresh.postDelayed(this::updateWebContentBottomInset, delayMs);
    }

    private void updateWebContentBottomInset() {
        if (swipeRefresh == null
                || !(swipeRefresh.getLayoutParams() instanceof CoordinatorLayout.LayoutParams)) {
            return;
        }

        int bottomInset = getVisibleBottomChromeHeight();
        if (geckoView != null) {
            geckoView.setVerticalClipping(bottomInset);
        }

        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) swipeRefresh.getLayoutParams();
        if (params.bottomMargin != bottomInset) {
            params.bottomMargin = bottomInset;
            swipeRefresh.setLayoutParams(params);
        }
    }

    private int getVisibleBottomChromeHeight() {
        int bottomInset = getBottomOverlapHeight(bottomNav, BOTTOM_NAV_FALLBACK_HEIGHT_DP);
        if (addressBarAtBottom) {
            bottomInset = Math.max(bottomInset,
                    getBottomOverlapHeight(appBar, BOTTOM_ADDRESS_BAR_FALLBACK_HEIGHT_DP));
        }
        bottomInset = Math.max(bottomInset,
                getBottomOverlapHeight(quickTabStripContainer, QUICK_TAB_STRIP_FALLBACK_HEIGHT_DP));
        bottomInset = Math.max(bottomInset,
                getBottomOverlapHeight(quickTabStripShadow, QUICK_TAB_SHADOW_FALLBACK_HEIGHT_DP));
        return Math.max(0, bottomInset);
    }

    private int getVisibleBottomNavHeight() {
        if (bottomNav == null || bottomNav.getVisibility() != View.VISIBLE) {
            return 0;
        }
        return getViewHeightOrDefault(bottomNav, BOTTOM_NAV_FALLBACK_HEIGHT_DP);
    }

    private int getBottomAddressBarHeight() {
        if (appBar == null || appBar.getVisibility() != View.VISIBLE) {
            return 0;
        }
        return getViewHeightOrDefault(appBar, BOTTOM_ADDRESS_BAR_FALLBACK_HEIGHT_DP);
    }

    private int getViewHeightOrDefault(View view, int fallbackDp) {
        int height = view != null ? view.getHeight() : 0;
        return height > 0 ? height : dp(fallbackDp);
    }

    private int getBottomOverlapHeight(View view, int fallbackDp) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return 0;
        }
        int rootHeight = browserRoot != null ? browserRoot.getHeight() : 0;
        int top = view.getTop();
        int height = view.getHeight();
        if (rootHeight <= 0 || top <= 0 || height <= 0) {
            return getViewHeightOrDefault(view, fallbackDp);
        }
        float translatedTop = top + view.getTranslationY();
        float overlap = rootHeight - translatedTop;
        if (overlap <= 0f) {
            return 0;
        }
        return Math.min(rootHeight, Math.round(overlap));
    }

    private void setBottomMargin(View view, int margin) {
        if (view == null || !(view.getLayoutParams() instanceof CoordinatorLayout.LayoutParams)) {
            return;
        }
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.bottomMargin = margin;
        view.setLayoutParams(params);
    }

    private void bringBottomChromeToFront() {
        if (appBar != null) {
            appBar.setTranslationZ(dp(6));
        }
        if (quickTabStripShadow != null) {
            quickTabStripShadow.setTranslationZ(dp(7));
            quickTabStripShadow.bringToFront();
        }
        if (quickTabStripContainer != null) {
            quickTabStripContainer.setTranslationZ(dp(8));
            quickTabStripContainer.bringToFront();
        }
        if (bottomNav != null) {
            bottomNav.setTranslationZ(dp(9));
            bottomNav.bringToFront();
        }
    }

    private void updateToolbarShortcutButton(SharedPreferences prefs, boolean bottomNavEnabled) {
        if (toolbarShortcutButton == null) {
            return;
        }
        String shortcut = prefs.getString(SettingsKeys.PREF_TOOLBAR_SHORTCUT,
                SettingsKeys.VALUE_OFF);
        boolean showFallbackMenu = !bottomNavEnabled && SettingsKeys.VALUE_OFF.equals(shortcut);
        if (SettingsKeys.VALUE_OFF.equals(shortcut) && !showFallbackMenu) {
            toolbarShortcutButton.setVisibility(View.GONE);
            return;
        }

        toolbarShortcutButton.setVisibility(View.VISIBLE);
        if (showFallbackMenu) {
            toolbarShortcutButton.setImageResource(R.drawable.ic_more_vert);
            toolbarShortcutButton.setContentDescription(getString(R.string.more));
            toolbarShortcutButton.setOnClickListener(v -> showMoreMenu());
            return;
        }

        toolbarShortcutButton.setImageResource(getToolbarShortcutIcon(shortcut));
        toolbarShortcutButton.setContentDescription(getToolbarShortcutLabel(shortcut));
        toolbarShortcutButton.setOnClickListener(v -> performToolbarShortcut(shortcut));
    }

    private int getToolbarShortcutIcon(String shortcut) {
        switch (shortcut) {
            case "new_tab":
                return R.drawable.ic_add;
            case "bookmarks":
                return R.drawable.ic_bookmarks;
            case "history":
                return R.drawable.ic_history;
            case "downloads":
                return R.drawable.ic_download;
            case "share":
                return R.drawable.ic_share;
            case "translate":
                return R.drawable.ic_translate;
            case "find":
                return R.drawable.ic_search;
            default:
                return R.drawable.ic_more_vert;
        }
    }

    private String getToolbarShortcutLabel(String shortcut) {
        switch (shortcut) {
            case "new_tab":
                return getString(R.string.new_tab);
            case "bookmarks":
                return getString(R.string.bookmarks);
            case "history":
                return getString(R.string.history);
            case "downloads":
                return getString(R.string.downloads);
            case "share":
                return getString(R.string.share);
            case "translate":
                return getString(R.string.translate_page);
            case "find":
                return getString(R.string.find_in_page);
            default:
                return getString(R.string.toolbar_shortcut);
        }
    }

    private void performToolbarShortcut(String shortcut) {
        switch (shortcut) {
            case "new_tab":
                createNewTab(false);
                break;
            case "bookmarks":
                startActivity(new Intent(this, BookmarksActivity.class));
                break;
            case "history":
                startActivity(new Intent(this, HistoryActivity.class));
                break;
            case "downloads":
                startActivity(new Intent(this, DownloadsActivity.class));
                break;
            case "share":
                shareCurrentPage();
                break;
            case "translate":
                translatePage();
                break;
            case "find":
                showFindInPageDialog();
                break;
            default:
                showMoreMenu();
                break;
        }
    }

    private void setupQuickTabStrip() {
        if (quickTabStrip == null || quickTabAdd == null) {
            return;
        }
        quickTabStripAdapter = new QuickTabStripAdapter(new QuickTabStripAdapter.Listener() {
            @Override
            public void onTabClick(Tab tab) {
                captureCurrentTabThumbnail();
                tabManager.switchToTab(tab);
                updateQuickTabStrip();
            }

            @Override
            public void onTabClose(Tab tab) {
                closeTabWithUndo(tab);
            }

            @Override
            public void onTabLongClick(Tab tab, View anchor) {
                showQuickTabMenu(tab, anchor);
            }

            @Override
            public void onTabsReordered(List<Tab> tabs) {
                if (tabManager != null && tabManager.reorderTabs(tabs)) {
                    updateQuickTabStrip();
                }
            }
        });
        quickTabStrip.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        quickTabStrip.setAdapter(quickTabStripAdapter);
        EasyMotion.configurePremiumItemAnimator(quickTabStrip);
        new ItemTouchHelper(new TabItemTouchHelperCallback(quickTabStripAdapter))
                .attachToRecyclerView(quickTabStrip);
        quickTabStrip.setContentDescription(getString(R.string.quick_tab_strip));
        quickTabAdd.setOnClickListener(this::showNewTabButtonMenu);
        quickTabAdd.setOnLongClickListener(v -> {
            showNewTabLongPressMenu(v);
            return true;
        });

        updateQuickTabStrip();
    }

    private void showQuickTabMenu(Tab tab, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.close_tab);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.new_tab);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.tab_groups_title);
        menu.getMenu().add(Menu.NONE, 4, Menu.NONE,
                tab.isPinned() ? R.string.unpin_tab : R.string.pin_tab);
        menu.getMenu().add(Menu.NONE, 5, Menu.NONE,
                tab.isLocked() ? R.string.unlock_tab : R.string.lock_tab);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                closeTabWithUndo(tab);
                return true;
            } else if (item.getItemId() == 2) {
                createNewTabInGroup(tab.isPrivate(), tab.getGroupId(),
                        tab.getGroupName(), tab.getGroupColor());
                return true;
            } else if (item.getItemId() == 3) {
                launchTabsActivity();
                return true;
            } else if (item.getItemId() == 4) {
                tabManager.setTabPinned(tab.getId(), !tab.isPinned());
                updateQuickTabStrip();
                Toast.makeText(this,
                        tab.isPinned() ? R.string.tab_pinned : R.string.tab_unpinned,
                        Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == 5) {
                tabManager.setTabLocked(tab.getId(), !tab.isLocked());
                updateQuickTabStrip();
                Toast.makeText(this,
                        tab.isLocked() ? R.string.tab_locked : R.string.tab_unlocked,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showNewTabButtonMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        Tab current = tabManager != null ? tabManager.getCurrentTab() : null;
        boolean insideGroup = current != null && current.getGroupId() != null;
        if (insideGroup) {
            menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.new_tab_in_group);
        }
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.new_tab);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.new_private_tab);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1 && current != null) {
                createNewTabInGroup(current.isPrivate(), current.getGroupId(),
                        current.getGroupName(), current.getGroupColor());
                return true;
            } else if (item.getItemId() == 2) {
                createNewTab(false);
                return true;
            } else if (item.getItemId() == 3) {
                createNewTab(true);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showNewTabLongPressMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.select_tabs);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.close_all_tabs);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                launchTabsActivity();
                return true;
            } else if (item.getItemId() == 2) {
                confirmCloseAllTabs();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmCloseAllTabs() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.close_all_tabs)
                .setMessage(R.string.close_all_tabs_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.close_all_tabs, (dialog, which) -> {
                    if (tabManager == null) {
                        return;
                    }
                    captureCurrentTabThumbnail();
                    tabManager.closeAllTabs();
                    attachTabToView(tabManager.getCurrentTab());
                    updateTabCount();
                    updateQuickTabStrip();
                })
                .show();
    }

    private void updateQuickTabStrip() {
        if (quickTabStripAdapter == null || tabManager == null) {
            return;
        }
        Tab current = tabManager.getCurrentTab();
        String currentId = current != null ? current.getId() : null;
        List<Tab> tabs = getActiveGroupTabs();
        boolean showStrip = current != null && current.getGroupId() != null && tabs.size() >= 2;
        if (quickTabStripContainer != null) {
            quickTabStripContainer.setVisibility(showStrip ? View.VISIBLE : View.GONE);
            if (showStrip && browserChromeVisible) {
                quickTabStripContainer.setTranslationY(0f);
            }
        }
        if (quickTabStripShadow != null) {
            quickTabStripShadow.setVisibility(showStrip ? View.VISIBLE : View.GONE);
            if (showStrip && browserChromeVisible) {
                quickTabStripShadow.setTranslationY(0f);
            }
        }
        quickTabStripAdapter.submitTabs(showStrip ? tabs : new ArrayList<>(), currentId);
        requestWebContentBottomInsetUpdate();
        if (!showStrip) {
            return;
        }
        if (quickTabStrip != null && currentId != null) {
            for (int i = 0; i < tabs.size(); i++) {
                if (currentId.equals(tabs.get(i).getId())) {
                    final int position = i;
                    quickTabStrip.post(() -> quickTabStrip.smoothScrollToPosition(position));
                    break;
                }
            }
        }
    }

    private void handleBrowserChromeForScroll(int scrollY) {
        if (appBar == null || bottomNav == null) {
            return;
        }
        if (scrollY <= dp(12)) {
            showBrowserChrome(true);
            lastChromeScrollY = scrollY;
            accumulatedChromeScrollDelta = 0;
            return;
        }

        int delta = scrollY - lastChromeScrollY;
        lastChromeScrollY = scrollY;
        if (Math.abs(delta) < 2) {
            return;
        }

        if (delta > 0) {
            accumulatedChromeScrollDelta = Math.max(0, accumulatedChromeScrollDelta) + delta;
            if (accumulatedChromeScrollDelta > dp(CHROME_SCROLL_THRESHOLD_PX)) {
                hideBrowserChrome();
                accumulatedChromeScrollDelta = 0;
            }
        } else {
            accumulatedChromeScrollDelta = Math.min(0, accumulatedChromeScrollDelta) + delta;
            if (Math.abs(accumulatedChromeScrollDelta) > dp(CHROME_SCROLL_THRESHOLD_PX / 2)) {
                showBrowserChrome(true);
                accumulatedChromeScrollDelta = 0;
            }
        }
    }

    private void hideBrowserChrome() {
        if (!browserChromeVisible || appBar == null || bottomNav == null) {
            return;
        }
        browserChromeVisible = false;
        requestWebContentBottomInsetUpdate();
        requestWebContentBottomInsetUpdateAfter(230L);
        int bottomNavOffset = getBottomNavHideOffset();
        if (addressBarAtBottom) {
            appBar.animate()
                    .translationY(bottomNavOffset + appBar.getHeight())
                    .setDuration(180)
                    .setInterpolator(EasyMotion.STANDARD_ACCELERATE)
                    .start();
        } else {
            appBar.setExpanded(false, true);
        }
        EasyMotion.animateBottomBarVisibility(bottomNav, bottomNavOffset, false, true);
        int stripOffset = bottomNavOffset + dp(12);
        if (addressBarAtBottom) {
            stripOffset += appBar.getHeight();
        }
        if (quickTabStripContainer != null) {
            quickTabStripContainer.animate()
                    .translationY(stripOffset + quickTabStripContainer.getHeight())
                    .setDuration(180)
                    .setInterpolator(EasyMotion.STANDARD_ACCELERATE)
                    .start();
        }
        if (quickTabStripShadow != null && quickTabStripContainer != null) {
            quickTabStripShadow.animate()
                    .translationY(stripOffset + quickTabStripContainer.getHeight())
                    .setDuration(180)
                    .setInterpolator(EasyMotion.STANDARD_ACCELERATE)
                    .start();
        }
    }

    private void showBrowserChrome(boolean animated) {
        if (browserChromeVisible && appBar != null && bottomNav != null) {
            return;
        }
        browserChromeVisible = true;
        requestWebContentBottomInsetUpdate();
        requestWebContentBottomInsetUpdateAfter(animated ? EasyMotion.DURATION_MEDIUM + 40L : 40L);
        if (appBar != null) {
            appBar.setExpanded(true, animated);
            appBar.animate()
                    .translationY(0f)
                    .setDuration(animated ? EasyMotion.DURATION_MEDIUM : 0)
                    .setInterpolator(EasyMotion.EMPHASIZED)
                    .start();
        }
        EasyMotion.animateBottomBarVisibility(bottomNav, 0f, true, animated);
        if (quickTabStripContainer != null) {
            quickTabStripContainer.animate()
                    .translationY(0f)
                    .setDuration(animated ? EasyMotion.DURATION_MEDIUM : 0)
                    .setInterpolator(EasyMotion.EMPHASIZED)
                    .start();
        }
        if (quickTabStripShadow != null) {
            quickTabStripShadow.animate()
                    .translationY(0f)
                    .setDuration(animated ? EasyMotion.DURATION_MEDIUM : 0)
                    .setInterpolator(EasyMotion.EMPHASIZED)
                    .start();
        }
    }

    private int getBottomNavHideOffset() {
        if (bottomNav == null || bottomNav.getVisibility() != View.VISIBLE) {
            return 0;
        }
        return bottomNav.getHeight();
    }

    private void showMoreMenu() {
        View anchor = null;
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            anchor = bottomNav.findViewById(R.id.nav_settings);
        }
        if (anchor == null && toolbarShortcutButton != null
                && toolbarShortcutButton.getVisibility() == View.VISIBLE) {
            anchor = toolbarShortcutButton;
        }
        if (anchor == null) {
            anchor = browserRoot != null ? browserRoot : geckoView;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        List<MoreMenuPopup.Action> navigationActions = new ArrayList<>();
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_arrow_back, R.string.back,
                canGoBack, () -> {
            if (session != null && canGoBack) {
                session.goBack();
            }
        }));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_arrow_forward, R.string.forward,
                canGoForward, () -> {
            if (session != null && canGoForward) {
                session.goForward();
            }
        }));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_reload, R.string.reload,
                session != null, () -> {
            if (session != null) {
                reloadCurrentPage();
            }
        }));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_share, R.string.share,
                currentUrl != null && !currentUrl.trim().isEmpty(), this::shareCurrentPage));

        List<MoreMenuPopup.Action> menuActions = new ArrayList<>();
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_NEW_TAB,
                new MoreMenuPopup.Action(R.drawable.ic_tabs, R.string.new_tab,
                        true, () -> createNewTab(false)));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_NEW_PRIVATE_TAB,
                new MoreMenuPopup.Action(R.drawable.ic_incognito, R.string.new_private_tab,
                        true, () -> createNewTab(true)));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_HISTORY,
                new MoreMenuPopup.Action(R.drawable.ic_history, R.string.history,
                        true, () -> startActivity(new Intent(this, HistoryActivity.class))));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_DOWNLOADS,
                new MoreMenuPopup.Action(R.drawable.ic_download, R.string.downloads,
                        true, () -> startActivity(new Intent(this, DownloadsActivity.class))));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_BOOKMARKS,
                new MoreMenuPopup.Action(R.drawable.ic_bookmarks, R.string.bookmarks,
                        true, () -> startActivity(new Intent(this, BookmarksActivity.class))));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_FIND_IN_PAGE,
                new MoreMenuPopup.Action(R.drawable.ic_search, R.string.find_in_page,
                        session != null, this::showFindInPageDialog));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_DESKTOP_SITE,
                new MoreMenuPopup.Action(R.drawable.ic_desktop, R.string.desktop_site,
                        session != null, this::toggleDesktopSite));
        boolean isReaderMode = currentUrl != null && currentUrl.startsWith("about:reader");
        if (prefs.getBoolean(SettingsKeys.PREF_READER_MODE_MENU_ENABLED, true)) {
            addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_READER_MODE,
                    new MoreMenuPopup.Action(R.drawable.ic_text,
                            isReaderMode ? R.string.reader_mode_on : R.string.reader_mode,
                            session != null && currentUrl != null,
                            this::toggleReaderMode));
        }
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_ADD_TO_QUICK_ACCESS,
                new MoreMenuPopup.Action(R.drawable.ic_view_grid, R.string.add_to_quick_access,
                        currentUrl != null && !currentUrl.trim().isEmpty(),
                        this::addCurrentPageToQuickAccess));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_ZOOM,
                new MoreMenuPopup.Action(R.drawable.ic_search, R.string.zoom_for_site,
                        currentUrl != null && !currentUrl.trim().isEmpty()
                                && !"about:blank".equals(currentUrl),
                        this::showSiteZoomDialog));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_SAVE_PAGE,
                new MoreMenuPopup.Action(R.drawable.ic_pdf, R.string.save_page,
                        session != null, this::savePageAsPdf));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_ADD_TO_HOME_SCREEN,
                new MoreMenuPopup.Action(R.drawable.ic_home, R.string.add_to_home_screen,
                        currentUrl != null && !currentUrl.trim().isEmpty()
                                && !"about:blank".equals(currentUrl),
                        this::addToHomeScreen));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_READING_LIST,
                new MoreMenuPopup.Action(R.drawable.ic_history, R.string.reading_list,
                        true, () -> startActivity(new Intent(this, ReadingListActivity.class))));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_SAVE_TO_READING_LIST,
                new MoreMenuPopup.Action(R.drawable.ic_download, R.string.save_to_reading_list,
                        session != null && currentUrl != null && !currentUrl.trim().isEmpty(),
                        this::saveToReadingList));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_TRANSLATE,
                new MoreMenuPopup.Action(R.drawable.ic_translate, R.string.translate_page,
                        currentUrl != null && !currentUrl.trim().isEmpty(),
                        this::translatePage));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_EXTENSIONS,
                new MoreMenuPopup.Action(R.drawable.ic_extensions_puzzle, R.string.extensions,
                        true, () -> startActivity(new Intent(this, ExtensionsActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_settings, R.string.settings,
                true, () -> startActivity(new Intent(this, SettingsActivity.class))));
        addMenuAction(menuActions, prefs, SettingsKeys.PREF_MENU_EXIT,
                new MoreMenuPopup.Action(R.drawable.ic_exit, R.string.exit,
                        true, this::exitApp));

        MoreMenuPopup.show(this, anchor, navigationActions, menuActions);
    }

    private void addMenuAction(List<MoreMenuPopup.Action> actions, SharedPreferences prefs,
                               String prefKey, MoreMenuPopup.Action action) {
        if (prefs.getBoolean(prefKey, true)) {
            actions.add(action);
        }
    }

    private void createNewTab(boolean isPrivate) {
        if (tabManager != null) {
            captureCurrentTabThumbnail();
            Tab newTab = tabManager.createNewTab(isPrivate);
            animateNextTabAttach = true;
            attachTabToView(newTab);
            if (!isPrivate) {
                updateUrlInputForUrl(newTab.getUrl());
            } else {
                urlInput.setText("");
                Toast.makeText(this, R.string.private_tab_started, Toast.LENGTH_SHORT).show();
            }
            updateTabCount();
        }
    }

    private void createNewTabInGroup(boolean isPrivate, String groupId,
                                     String groupName, int groupColor) {
        if (tabManager == null || groupId == null) {
            createNewTab(isPrivate);
            return;
        }
        captureCurrentTabThumbnail();
        Tab newTab = tabManager.createNewTab(isPrivate);
        tabManager.addTabToGroup(newTab, groupId, groupName,
                groupColor != 0 ? groupColor : TabRepository.getDefaultGroupColor(this));
        animateNextTabAttach = true;
        attachTabToView(newTab);
        if (!isPrivate) {
            updateUrlInputForUrl(newTab.getUrl());
        } else {
            urlInput.setText("");
            Toast.makeText(this, R.string.private_tab_started, Toast.LENGTH_SHORT).show();
        }
        updateTabCount();
        updateQuickTabStrip();
    }

    void recordHistory() {
        if (currentUrl == null || currentUrl.isEmpty() || "about:blank".equals(currentUrl)) {
            return;
        }
        if (UrlUtils.isInternalPageUrl(currentUrl)) {
            return;
        }
        if (currentUrl.equals(lastRecordedUrl)) {
            return;
        }
        Tab currentTab = tabManager != null ? tabManager.getCurrentTab() : null;
        if (currentTab != null && currentTab.isPrivate()) {
            return;
        }
        String title = currentTitle != null && !currentTitle.isEmpty() ? currentTitle : currentUrl;
        lastRecordedUrl = currentUrl;
        PrivacyStatsManager.recordProtectedPage(this);
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("save_history", true)) {
            return;
        }
        if (!UrlUtils.isSearchResultsUrl(this, currentUrl)) {
            QuickAccessItem quickAccessItem = createQuickAccessItem(title, currentUrl, currentTab);
            if (quickAccessItem != null) {
                quickAccessRepository.updateQuickAccessItem(quickAccessItem);
            }
        }
        historyRepository.addHistoryItem(new HistoryItem(title, currentUrl), new HistoryRepository.HistoryCallback() {
            @Override
            public void onHistoryLoaded(List<HistoryItem> historyItems) {}

            @Override
            public void onHistoryItemAdded(HistoryItem item) {}

            @Override
            public void onHistoryCleared() {}
        });
    }

    void recordBlockedPrivacyItem() {
        PrivacyStatsManager.recordBlockedItem(this);
    }

    /**
     * Called from {@link BrowserContentDelegate#onFullScreen} when web content
     * enters or leaves DOM fullscreen (e.g. tapping the fullscreen button on a
     * YouTube video). Instead of Picture-in-Picture, the browser goes immersive
     * fullscreen: chrome hidden, system bars hidden, locked landscape.
     */
    void setWebFullscreen(boolean fullscreen) {
        if (fullscreen == inWebFullscreen) {
            return;
        }
        if (!fullscreen) {
            suppressSearchPopupAfterFullscreenExit();
        }
        inWebFullscreen = fullscreen;
        runOnUiThread(() -> applyWebFullscreen(fullscreen));
    }

    boolean isInWebFullscreen() {
        return inWebFullscreen;
    }

    private void applyWebFullscreen(boolean fullscreen) {
        WindowInsetsControllerCompat insets =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (fullscreen) {
            preFullscreenOrientation = getRequestedOrientation();
            preFullscreenKeepScreenOn = (getWindow().getAttributes().flags
                    & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Window/Gecko backgrounds default to the light theme color. When the
            // video letterboxes (e.g. 16:9 content on a wider device), that light
            // color shows around the video — replace with black for both surfaces.
            preFullscreenWindowBackground = getWindow().getDecorView().getBackground();
            getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.BLACK));
            if (geckoView != null) {
                preFullscreenGeckoBackground = geckoView.getBackground();
                geckoView.setBackgroundColor(Color.BLACK);
            }
            hideKeyboard();
            // Draw edge-to-edge so the hidden status/nav bar regions are filled by
            // the video instead of the (light) window background.
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                preFullscreenCutoutMode = lp.layoutInDisplayCutoutMode;
                // ALWAYS (R+) lets the window extend into the cutout on any edge —
                // SHORT_EDGES leaves a side strip in landscape because the cutout
                // is then on a long edge.
                lp.layoutInDisplayCutoutMode =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                                : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(lp);
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            if (appBar != null) {
                // AppBarLayout can carry a stale scroll offset; reset before hiding so
                // the post-exit restore doesn't show a half-collapsed bar.
                appBar.setExpanded(true, false);
                appBar.setVisibility(View.GONE);
            }
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            if (quickTabStripContainer != null) {
                quickTabStripContainer.setVisibility(View.GONE);
            }
            if (quickTabStripShadow != null) {
                quickTabStripShadow.setVisibility(View.GONE);
            }
            hideSystemBarsForWebFullscreen();
            // CoordinatorLayout/AppBarLayout otherwise pad the content by the status-bar
            // inset, leaving a band of window background at the top. Swallow all insets
            // while fullscreen so the video reaches every edge.
            if (browserRoot != null) {
                browserRoot.setPadding(0, 0, 0, 0);
                ViewCompat.setOnApplyWindowInsetsListener(browserRoot,
                        (v, ins) -> WindowInsetsCompat.CONSUMED);
                ViewCompat.requestApplyInsets(browserRoot);
            }
            // Drop the bottom margin / vertical clipping that tracked the now-hidden
            // bottom chrome — otherwise the GeckoView surface is short by a strip
            // that shows the (light) window background.
            if (swipeRefresh != null
                    && swipeRefresh.getLayoutParams()
                            instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams params =
                        (CoordinatorLayout.LayoutParams) swipeRefresh.getLayoutParams();
                if (params.bottomMargin != 0) {
                    params.bottomMargin = 0;
                    swipeRefresh.setLayoutParams(params);
                }
            }
            if (geckoView != null) {
                geckoView.setVerticalClipping(0);
            }
            // Re-parent GeckoView into DecorView. CoordinatorLayout/AppBarLayout/
            // SwipeRefreshLayout each impose layout constraints (top/bottom insets,
            // scroll offset, behavior margins) that otherwise leave residual strips
            // around the video. Hoisting the surface above the entire chrome tree
            // mirrors the WebView onShowCustomView pattern.
            if (geckoView != null && geckoView.getParent() instanceof ViewGroup) {
                preFullscreenGeckoParent = (ViewGroup) geckoView.getParent();
                preFullscreenGeckoIndex = preFullscreenGeckoParent.indexOfChild(geckoView);
                preFullscreenGeckoLayoutParams = geckoView.getLayoutParams();
                preFullscreenGeckoParent.removeView(geckoView);
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(geckoView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            }
        } else {
            suppressSearchPopupAfterFullscreenExit();
            if (!preFullscreenKeepScreenOn) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            if (browserRoot != null) {
                ViewCompat.setOnApplyWindowInsetsListener(browserRoot, null);
                ViewCompat.requestApplyInsets(browserRoot);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.layoutInDisplayCutoutMode = preFullscreenCutoutMode;
                getWindow().setAttributes(lp);
            }
            setRequestedOrientation(preFullscreenOrientation);
            if (insets != null) {
                insets.show(WindowInsetsCompat.Type.systemBars());
            }
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
            if (bottomNav != null) {
                bottomNav.setVisibility(bottomNavigationEnabled ? View.VISIBLE : View.GONE);
            }
            getWindow().setBackgroundDrawable(preFullscreenWindowBackground != null
                    ? preFullscreenWindowBackground
                    : new android.graphics.drawable.ColorDrawable(
                            ContextCompat.getColor(this, R.color.backgroundColor)));
            preFullscreenWindowBackground = null;
            // Re-parent GeckoView back into its original container so the SurfaceView
            // reattaches and SwipeRefresh again has its child.
            if (geckoView != null && preFullscreenGeckoParent != null
                    && geckoView.getParent() instanceof ViewGroup) {
                ((ViewGroup) geckoView.getParent()).removeView(geckoView);
                int index = preFullscreenGeckoIndex >= 0
                        && preFullscreenGeckoIndex <= preFullscreenGeckoParent.getChildCount()
                        ? preFullscreenGeckoIndex
                        : -1;
                if (preFullscreenGeckoLayoutParams != null) {
                    preFullscreenGeckoParent.addView(geckoView, index,
                            preFullscreenGeckoLayoutParams);
                } else {
                    preFullscreenGeckoParent.addView(geckoView, index);
                }
            }
            preFullscreenGeckoParent = null;
            preFullscreenGeckoLayoutParams = null;
            preFullscreenGeckoIndex = -1;
            if (geckoView != null) {
                geckoView.setBackground(preFullscreenGeckoBackground);
                preFullscreenGeckoBackground = null;
            }
            // Quick tab strip + shadow visibility is owned by updateQuickTabStrip().
            updateQuickTabStrip();
            // Restore the bottom margin / vertical clipping for the (now visible)
            // chrome.
            requestWebContentBottomInsetUpdate();
            hideKeyboard();
            if (geckoView != null) {
                geckoView.requestFocus();
            }
        }
    }

    /**
     * Immersive hide of status + nav bars. Used both on initial fullscreen entry
     * and re-applied after configuration changes / focus regain, since the system
     * tends to restore bars after orientation flips.
     */
    private void hideSystemBarsForWebFullscreen() {
        WindowInsetsControllerCompat insets =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insets != null) {
            insets.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            insets.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && inWebFullscreen) {
            hideSystemBarsForWebFullscreen();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (inWebFullscreen) {
            // Re-apply after the system finishes the orientation flip.
            getWindow().getDecorView().post(this::hideSystemBarsForWebFullscreen);
        }
    }

    private void toggleDesktopSite() {
        if (session == null) {
            return;
        }
        isDesktopMode = !isDesktopMode;
        session.getSettings().setUserAgentMode(isDesktopMode
                ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        session.getSettings().setViewportMode(isDesktopMode
                ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        reloadCurrentPage();
    }

    private void showFindInPageDialog() {
        if (session == null) {
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_find_in_page, null);
        EditText input = dialogView.findViewById(R.id.find_input);
        ImageButton prev = dialogView.findViewById(R.id.find_prev);
        ImageButton next = dialogView.findViewById(R.id.find_next);
        ImageButton close = dialogView.findViewById(R.id.find_close);

        session.getFinder().setDisplayFlags(
                GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL |
                        GeckoSession.FINDER_DISPLAY_DIM_PAGE);

        Dialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.find_in_page)
                .setView(dialogView)
                .setOnDismissListener(d -> {
                    if (session != null) {
                        session.getFinder().clear();
                    }
                })
                .create();

        Runnable findForward = () -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                session.getFinder().find(query, GeckoSession.FINDER_FIND_FORWARD);
            }
        };
        Runnable findBackward = () -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                session.getFinder().find(query, GeckoSession.FINDER_FIND_BACKWARDS);
            }
        };

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    session.getFinder().clear();
                } else {
                    session.getFinder().find(query, GeckoSession.FINDER_FIND_FORWARD);
                }
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                findForward.run();
                return true;
            }
            return false;
        });
        next.setOnClickListener(v -> findForward.run());
        prev.setOnClickListener(v -> findBackward.run());
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        input.requestFocus();
    }

    private void showHelpAndFeedback() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body));
        startActivity(Intent.createChooser(intent, getString(R.string.help_feedback)));
    }

    void showPageContextMenu(GeckoSession.ContentDelegate.ContextElement element) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        String link = element.linkUri;
        String media = element.srcUri;

        if (!TextUtils.isEmpty(link)) {
            labels.add(getString(R.string.context_menu_open_new_tab));
            actions.add(() -> openUrlInNewTab(link, false));
            labels.add(getString(R.string.context_menu_open_new_tab_in_group));
            actions.add(() -> openUrlInNewTabInGroup(link));
            labels.add(getString(R.string.context_menu_open_private_tab));
            actions.add(() -> openUrlInNewTab(link, true));
            labels.add(getString(R.string.context_menu_copy_link));
            actions.add(() -> copyToClipboard(link));
            labels.add(getString(R.string.context_menu_share_link));
            actions.add(() -> shareText(link));
            labels.add(getString(R.string.context_menu_download_link));
            actions.add(() -> startDownload(link, null, null));
        }

        if (!TextUtils.isEmpty(media)
                && element.type != GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE) {
            labels.add(getString(R.string.download));
            actions.add(() -> startDownload(media, null, null));
        }

        if (labels.isEmpty() && !TextUtils.isEmpty(element.textContent)) {
            labels.add(getString(R.string.context_menu_copy_text));
            actions.add(() -> copyToClipboard(element.textContent));
        }

        if (labels.isEmpty()) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    void openUrlInNewTab(String url, boolean isPrivate) {
        openUrlInNewTab(url, isPrivate, false);
    }

    void openUrlInNewTab(String url, boolean isPrivate, boolean closeOnBackToPreviousTab) {
        if (tabManager == null) {
            return;
        }
        captureCurrentTabThumbnail();
        Tab current = tabManager.getCurrentTab();
        String parentId = (!isPrivate && current != null) ? current.getId() : null;
        Tab tab = tabManager.createNewTab(isPrivate, false, parentId);
        if (shouldOpenNewTabInCurrentGroup(current, isPrivate)) {
            tabManager.addTabToGroup(tab, current.getGroupId(),
                    current.getGroupName(), current.getGroupColor());
        }
        tab.setCloseOnBackToPreviousTab(closeOnBackToPreviousTab);
        animateNextTabAttach = true;
        attachTabToView(tab);
        loadUrl(url);
        updateTabCount();
        updateQuickTabStrip();
    }

    private boolean shouldOpenNewTabInCurrentGroup(Tab current, boolean isPrivate) {
        return current != null
                && current.getGroupId() != null
                && current.isPrivate() == isPrivate
                && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                SettingsKeys.PREF_ONLY_OPEN_LINKS_IN_CURRENT_GROUP, false);
    }

    private void openUrlInNewTabInGroup(String url) {
        if (tabManager == null) {
            return;
        }
        Tab current = tabManager.getCurrentTab();
        if (current == null) {
            openUrlInNewTab(url, false);
            return;
        }
        captureCurrentTabThumbnail();
        boolean isPrivate = current.isPrivate();
        Tab tab = tabManager.createNewTab(isPrivate, false, current.getId());
        if (current.getGroupId() != null) {
            tabManager.addTabToGroup(tab, current.getGroupId(), current.getGroupName(), current.getGroupColor());
        } else {
            String groupName = current.getTitle() != null && !current.getTitle().trim().isEmpty()
                    ? current.getTitle()
                    : getString(R.string.tab_group);
            tabManager.createGroupForTabs(Arrays.asList(current.getId(), tab.getId()),
                    groupName,
                    TabRepository.getDefaultGroupColor(this),
                    isPrivate);
        }
        animateNextTabAttach = true;
        attachTabToView(tab);
        loadUrl(url);
        updateTabCount();
        updateQuickTabStrip();
    }

    private void copyToClipboard(String value) {
        if (value == null) {
            return;
        }
        // Refuse to copy from a private tab — the clipboard is readable by every
        // foreground app on API < 31 and the URL would defeat the user's reason
        // for using private mode in the first place.
        if (isCurrentTabPrivate()) {
            Toast.makeText(this, R.string.private_copy_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(getString(R.string.copy_link), value);
            // Mark the clip as sensitive so API 33+ launchers don't show a preview
            // toast/snackbar with the contents.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.os.PersistableBundle extras = new android.os.PersistableBundle();
                extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(extras);
            }
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareText(String value) {
        if (value == null || value.trim().isEmpty() || shouldBlockPrivateShare()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, value);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private boolean shouldBlockPrivateShare() {
        if (!isCurrentTabPrivate()) {
            return false;
        }
        Toast.makeText(this, R.string.private_share_blocked, Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean isCurrentTabPrivate() {
        Tab current = tabManager != null ? tabManager.getCurrentTab() : null;
        return current != null && current.isPrivate();
    }

    // Register the add-on install prompt so AMO's "Add to Firefox" web-install flow (and any
    // .xpi we hand to the controller) asks the user to approve the requested permissions.
    private void setupExtensionInstallPrompt() {
        try {
            RuntimeManager.getRuntime(this).getWebExtensionController().setPromptDelegate(
                    new WebExtensionController.PromptDelegate() {
                        @Override
                        public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(
                                @NonNull WebExtension extension, @NonNull String[] permissions,
                                @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
                            return promptInstallPermission(extension, permissions, origins);
                        }
                    });
        } catch (Exception ignored) {
            // Runtime not ready — installExtensionFromUrl sets the delegate again on demand.
        }
    }

    // Install an add-on the browser encountered (an .xpi link, or AMO's "Add to Firefox"),
    // instead of downloading the file. Prompts the user with the requested permissions first.
    void installExtensionFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        WebExtensionController controller = RuntimeManager.getRuntime(this).getWebExtensionController();
        controller.setPromptDelegate(new WebExtensionController.PromptDelegate() {
            @Override
            public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(
                    @NonNull WebExtension extension, @NonNull String[] permissions,
                    @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
                return promptInstallPermission(extension, permissions, origins);
            }
        });
        Toast.makeText(this, R.string.extension_installing, Toast.LENGTH_SHORT).show();
        controller.install(url, WebExtensionController.INSTALLATION_METHOD_MANAGER).accept(
                extension -> runOnUiThread(() ->
                        Toast.makeText(this, R.string.extension_installed, Toast.LENGTH_SHORT).show()),
                exception -> runOnUiThread(() ->
                        Toast.makeText(this, R.string.extension_install_failed, Toast.LENGTH_LONG).show()));
    }

    private GeckoResult<WebExtension.PermissionPromptResponse> promptInstallPermission(
            WebExtension extension, String[] permissions, String[] origins) {
        GeckoResult<WebExtension.PermissionPromptResponse> result = new GeckoResult<>();
        runOnUiThread(() -> {
            String name = extension.metaData != null && extension.metaData.name != null
                    ? extension.metaData.name : extension.id;
            StringBuilder message = new StringBuilder(
                    getString(R.string.extension_install_permissions_intro, name));
            for (String permission : permissions) {
                message.append("\n• ").append(permission);
            }
            for (String origin : origins) {
                message.append("\n• ").append(origin);
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_extension)
                    .setMessage(message.toString())
                    .setPositiveButton(R.string.install_extension, (dialog, which) ->
                            result.complete(new WebExtension.PermissionPromptResponse(true, true, true)))
                    .setNegativeButton(R.string.cancel, (dialog, which) ->
                            result.complete(new WebExtension.PermissionPromptResponse(false, false, false)))
                    .setOnCancelListener(dialog ->
                            result.complete(new WebExtension.PermissionPromptResponse(false, false, false)))
                    .show();
        });
        return result;
    }

    void startDownload(String url, String filename, String mimeType) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        // On API <= 28 the download is published to public Downloads via the filesystem,
        // which needs WRITE_EXTERNAL_STORAGE granted at runtime. Request it up front so the
        // download doesn't transfer fully and then fail at the publish step.
        if (needsLegacyStoragePermission()) {
            pendingDownloadParams = new String[]{url, filename, mimeType};
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        performStartDownload(url, filename, mimeType);
    }

    private boolean needsLegacyStoragePermission() {
        return Build.VERSION.SDK_INT <= 28
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED;
    }

    private void performStartDownload(String url, String filename, String mimeType) {
        try {
            AnalyticsManager.logDownloadStarted(this, url, mimeType);
            AppDownloadManager.getInstance().startDownload(this, url, filename, mimeType,
                    new AppDownloadManager.DownloadStartListener() {
                        @Override
                        public void onDownloadStarted(String fileName) {
                            runOnUiThread(() -> showDownloadStartAnimation(fileName));
                        }

                        @Override
                        public void onDownloadQueuedForWifi(String fileName) {
                            runOnUiThread(() -> Toast.makeText(BrowserActivity.this,
                                    R.string.download_queued_wifi, Toast.LENGTH_SHORT).show());
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opera/Brave-style download-start feedback: a chip slides up with the filename, then
     * collapses into the toolbar's downloads button (which bounces). Tapping it opens Downloads.
     */
    private void showDownloadStartAnimation(String fileName) {
        if (browserRoot == null || isFinishing() || isDestroyed()) {
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
            return;
        }
        dismissDownloadChip();
        if (!(browserRoot instanceof ViewGroup)) {
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
            return;
        }
        ViewGroup rootGroup = (ViewGroup) browserRoot;
        View chip = getLayoutInflater().inflate(R.layout.view_download_chip, rootGroup, false);
        TextView title = chip.findViewById(R.id.download_chip_title);
        title.setText(TextUtils.isEmpty(fileName) ? getString(R.string.download_started) : fileName);

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.leftMargin = EasyMotion.dp(this, 16);
        lp.rightMargin = EasyMotion.dp(this, 16);
        lp.bottomMargin = EasyMotion.dp(this, 16)
                + (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE
                        ? bottomNav.getHeight() : 0);
        rootGroup.addView(chip, lp);
        downloadChipView = chip;

        chip.setOnClickListener(v -> {
            startActivity(new Intent(this, DownloadsActivity.class));
            dismissDownloadChip();
        });

        EasyMotion.animateTabChipOpen(chip);

        browserHandler.postDelayed(() -> {
            if (downloadChipView != chip) {
                return;
            }
            View target = (toolbarShortcutButton != null
                    && toolbarShortcutButton.getVisibility() == View.VISIBLE)
                    ? toolbarShortcutButton : null;
            if (target == null) {
                EasyMotion.animateDismiss(chip, () -> removeDownloadChip(chip));
            } else {
                EasyMotion.animateViewIntoTarget(chip, target, () -> {
                    removeDownloadChip(chip);
                    EasyMotion.pulse(target);
                });
            }
        }, 1600L);
    }

    private void dismissDownloadChip() {
        if (downloadChipView != null) {
            removeDownloadChip(downloadChipView);
        }
    }

    private void removeDownloadChip(View chip) {
        if (chip != null && chip.getParent() instanceof ViewGroup) {
            chip.animate().cancel();
            ((ViewGroup) chip.getParent()).removeView(chip);
        }
        if (downloadChipView == chip) {
            downloadChipView = null;
        }
    }

    void launchFilePicker(GeckoSession.PromptDelegate.FilePrompt prompt) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(getFilePickerMimeType(prompt.mimeTypes));
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE);
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_file)));
        } catch (Exception e) {
            completeFilePrompt(prompt.dismiss());
            pendingFilePrompt = null;
        }
    }

    private String getFilePickerMimeType(String[] mimeTypes) {
        if (mimeTypes == null || mimeTypes.length == 0) {
            return "*/*";
        }
        for (String mimeType : mimeTypes) {
            if (!TextUtils.isEmpty(mimeType) && mimeType.contains("/")) {
                return mimeType;
            }
        }
        return "*/*";
    }

    private Uri[] extractPickedUris(Intent data) {
        List<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) {
                    uris.add(uri);
                }
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        return uris.toArray(new Uri[0]);
    }

    private Uri[] copyPickedUrisToCache(Uri[] pickedUris) {
        List<Uri> fileUris = new ArrayList<>();
        boolean skippedTooLarge = false;
        File uploadDir = new File(getCacheDir(), "uploads");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            return new Uri[0];
        }

        // Clean stale upload files older than 1 hour
        File[] existing = uploadDir.listFiles();
        if (existing != null) {
            long cutoff = System.currentTimeMillis() - 60 * 60 * 1000L;
            for (File f : existing) {
                if (f.lastModified() < cutoff) f.delete();
            }
        }

        for (Uri uri : pickedUris) {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    continue;
                }
                String name = "upload_" + System.currentTimeMillis();
                File outFile = new File(uploadDir, name);
                boolean tooLarge = false;
                try (FileOutputStream output = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long copied = 0;
                    while ((read = input.read(buffer)) != -1) {
                        if (copied + read > MAX_UPLOAD_CACHE_BYTES) {
                            tooLarge = true;
                            break;
                        }
                        output.write(buffer, 0, read);
                        copied += read;
                    }
                }
                if (tooLarge) {
                    skippedTooLarge = true;
                    outFile.delete();
                } else {
                    fileUris.add(Uri.fromFile(outFile));
                }
            } catch (Exception ignored) {
            }
        }
        if (skippedTooLarge) {
            Toast.makeText(this, R.string.upload_file_too_large, Toast.LENGTH_SHORT).show();
        }
        return fileUris.toArray(new Uri[0]);
    }

    private void completeFilePrompt(GeckoSession.PromptDelegate.PromptResponse response) {
        if (pendingFileResult != null) {
            pendingFileResult.complete(response);
            pendingFileResult = null;
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // Under heavy memory pressure, free native resources for background tabs. They are
        // reloaded from persisted session state when the user switches back to them.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
                && tabManager != null) {
            tabManager.discardBackgroundTabs(session);
            updateTabCount();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBackgroundPlayReceiver();
        playingMediaSessions.clear();
        geckoMediaSessions.clear();
        geckoMediaMetadata.clear();
        notificationMediaSession = null;
        stopBackgroundMediaService();
        browserHandler.removeCallbacksAndMessages(null);
        mediaHandler.removeCallbacksAndMessages(null);
        dismissDownloadChip();
        // Best-effort auto-clear on a real exit (back press / Exit menu) when the user enabled
        // it — the GeckoRuntime + history executor outlive this Activity, so the async clears
        // still complete. (Swipe-away that kills the process can't be guaranteed.)
        if (isFinishing() && !isChangingConfigurations()
                && PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean("auto_clear_on_exit", false)) {
            performAutoClear(null);
        }
        // Detach delegates from every session so none keep a reference to this (destroyed)
        // Activity. BrowserViewModel keeps the sessions alive across config changes; the new
        // Activity re-configures the current tab via attachTabToView and each background tab
        // the next time it is switched to.
        if (tabManager != null) {
            for (Tab tab : tabManager.getTabs()) {
                if (tab != null) {
                    clearSessionDelegates(tab.getSession());
                }
            }
        }
        if (geckoView != null && session != null) {
            try {
                geckoView.releaseSession();
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GECKO_PERMISSIONS || pendingPermissionCallback == null) {
            return;
        }
        GeckoSession.PermissionDelegate.Callback callback = pendingPermissionCallback;
        pendingPermissionCallback = null;
        // Empty grantResults indicates the request was cancelled (e.g. activity recreated
        // or user dismissed the system dialog). Treat as denial — never grant by default.
        boolean granted = isGeckoPermissionRequestGranted(permissions, grantResults);
        if (granted) {
            callback.grant();
        } else {
            callback.reject();
        }
    }

    private boolean isGeckoPermissionRequestGranted(@NonNull String[] permissions,
                                                    @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        boolean requestedLocation = false;
        boolean grantedLocation = false;
        boolean grantedAllNonLocation = true;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = i < grantResults.length
                    ? grantResults[i]
                    : PackageManager.PERMISSION_DENIED;
            if (isLocationPermission(permission)) {
                requestedLocation = true;
                if (result == PackageManager.PERMISSION_GRANTED) {
                    grantedLocation = true;
                }
            } else if (result != PackageManager.PERMISSION_GRANTED) {
                grantedAllNonLocation = false;
            }
        }
        return grantedAllNonLocation && (!requestedLocation || grantedLocation);
    }

    private boolean isLocationPermission(String permission) {
        return Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)
                || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission);
    }

    private void shareCurrentPage() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            return;
        }
        if (shouldBlockPrivateShare()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, currentUrl);
        AnalyticsManager.logPageShared(this, currentUrl, "Android Chooser");
        startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
    }

    private void toggleReaderMode() {
        if (session == null || currentUrl == null) return;
        if (currentUrl.startsWith("about:reader")) {
            String orig = Uri.parse(currentUrl).getQueryParameter("url");
            if (orig != null) loadUrl(orig);
        } else {
            loadUrl("about:reader?url=" + Uri.encode(currentUrl));
        }
    }

    private void translatePage() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        loadUrl("https://translate.google.com/translate?u=" + Uri.encode(currentUrl));
    }

    private void showNavigationHistoryDialog() {
        if (session == null) return;
        GeckoSession.HistoryDelegate.HistoryList history =
                historyDelegate != null ? historyDelegate.lastHistory : null;
        if (history == null || history.size() == 0) {
            Toast.makeText(this, R.string.no_tab_history, Toast.LENGTH_SHORT).show();
            return;
        }
        int currentIndex = history.getCurrentIndex();
        int size = history.size();
        String[] entries = new String[size];
        for (int i = 0; i < size; i++) {
            GeckoSession.HistoryDelegate.HistoryItem item = history.get(i);
            String title = item != null ? item.getTitle() : null;
            String uri = item != null ? item.getUri() : "";
            String label = (title != null && !title.isEmpty()) ? title : uri;
            entries[i] = (i == currentIndex ? "▸  " : "    ") + label;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tab_history)
                .setSingleChoiceItems(entries, currentIndex, (dialog, which) -> {
                    if (which != currentIndex && session != null) {
                        session.gotoHistoryIndex(which);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showSiteZoomDialog() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        String host = UrlUtils.getDisplayHost(currentUrl);
        if (host == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float defaultFactor = prefs.getInt("text_size_percent", 100) / 100f;
        float currentFactor = prefs.getFloat("zoom_" + host, defaultFactor);
        int currentPercent = Math.round(currentFactor * 100f);

        int padding = dp(16);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, 0);

        TextView label = new TextView(this);
        label.setText(getString(R.string.zoom_for_site_title,
                getString(R.string.zoom_percent, currentPercent)));
        label.setTextSize(16);
        container.addView(label);

        Slider slider = new Slider(this);
        slider.setValueFrom(50f);
        slider.setValueTo(200f);
        slider.setStepSize(25f);
        slider.setValue(clampZoomPercent(currentPercent));
        slider.addOnChangeListener((s, value, fromUser) -> label.setText(
                getString(R.string.zoom_for_site_title,
                        getString(R.string.zoom_percent, Math.round(value)))));
        android.widget.LinearLayout.LayoutParams sliderParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        sliderParams.topMargin = dp(8);
        container.addView(slider, sliderParams);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.zoom_for_site)
                .setView(container)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    float factor = slider.getValue() / 100f;
                    prefs.edit().putFloat("zoom_" + host, factor).apply();
                    RuntimeManager.getRuntime(this).getSettings().setFontSizeFactor(factor);
                })
                .setNeutralButton(R.string.zoom_reset, (dialog, which) -> {
                    prefs.edit().remove("zoom_" + host).apply();
                    RuntimeManager.getRuntime(this).getSettings().setFontSizeFactor(defaultFactor);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private float clampZoomPercent(int percent) {
        if (percent < 50) return 50f;
        if (percent > 200) return 200f;
        return percent;
    }

    private void addCurrentPageToQuickAccess() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        String title = currentTitle != null && !currentTitle.isEmpty() ? currentTitle : currentUrl;
        Tab currentTab = tabManager != null ? tabManager.getCurrentTab() : null;
        QuickAccessItem item = createQuickAccessItem(title, currentUrl, currentTab);
        if (item == null) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }
        quickAccessRepository.updateQuickAccessItem(item);
        Toast.makeText(this, R.string.added_to_quick_access, Toast.LENGTH_SHORT).show();
    }

    private QuickAccessItem createQuickAccessItem(String fallbackTitle, String url, Tab tab) {
        String quickAccessUrl = UrlUtils.getQuickAccessUrl(url);
        if (quickAccessUrl == null) {
            return null;
        }
        String quickAccessTitle = UrlUtils.getQuickAccessTitle(url);
        QuickAccessItem item = new QuickAccessItem(
                !TextUtils.isEmpty(quickAccessTitle) ? quickAccessTitle : fallbackTitle,
                quickAccessUrl);
        String faviconUrl = UrlUtils.getFaviconUrl(quickAccessUrl);
        if (TextUtils.isEmpty(faviconUrl) && tab != null) {
            faviconUrl = tab.getFaviconUri();
        }
        item.setFaviconUrl(faviconUrl);
        return item;
    }

    private void savePageAsPdf() {
        if (session == null) return;
        // MediaStore.Downloads-based save is only available on API 29+.
        // Below that we'd need legacy WRITE_EXTERNAL_STORAGE which we've stopped
        // requesting on modern devices, so tell the user instead of silently failing.
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this, R.string.save_page_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.preparing_print, Toast.LENGTH_SHORT).show();
        session.saveAsPdf().then(inputStream -> {
            if (inputStream == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.save_page_failed, Toast.LENGTH_SHORT).show());
                return GeckoResult.fromValue(null);
            }
            String filename = (currentTitle != null && !currentTitle.isEmpty()
                    ? currentTitle : "page") + ".pdf";
            filename = filename.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
            writePdfToDownloads(inputStream, filename);
            return GeckoResult.fromValue(null);
        }, e -> {
            runOnUiThread(() -> Toast.makeText(this,
                    R.string.save_page_failed, Toast.LENGTH_SHORT).show());
            return GeckoResult.fromValue(null);
        });
    }

    @androidx.annotation.RequiresApi(29)
    private void writePdfToDownloads(java.io.InputStream inputStream, String filename) {
        new Thread(() -> {
            try {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS);
                android.net.Uri uri = getContentResolver()
                        .insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("MediaStore insert failed");
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buf)) != -1) out.write(buf, 0, read);
                } finally {
                    inputStream.close();
                }
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.page_saved, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.save_page_failed, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void printPage() {
        if (session == null) return;
        Toast.makeText(this, R.string.preparing_print, Toast.LENGTH_SHORT).show();
        session.saveAsPdf().then(inputStream -> {
            if (inputStream == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.print_failed, Toast.LENGTH_SHORT).show());
                return GeckoResult.fromValue(null);
            }
            try {
                File pdfFile = File.createTempFile("print_", ".pdf", getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buf)) != -1) fos.write(buf, 0, read);
                } finally {
                    inputStream.close();
                }
                runOnUiThread(() -> startPrintJob(pdfFile));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.print_failed, Toast.LENGTH_SHORT).show());
            }
            return GeckoResult.fromValue(null);
        }, e -> {
            runOnUiThread(() -> Toast.makeText(this,
                    R.string.print_failed, Toast.LENGTH_SHORT).show());
            return GeckoResult.fromValue(null);
        });
    }

    private void startPrintJob(File pdfFile) {
        android.print.PrintManager pm =
                (android.print.PrintManager) getSystemService(PRINT_SERVICE);
        if (pm == null) return;
        String jobName = currentTitle != null && !currentTitle.isEmpty() ? currentTitle : "Page";
        pm.print(jobName, new android.print.PrintDocumentAdapter() {
            @Override
            public void onLayout(android.print.PrintAttributes oldAttr,
                    android.print.PrintAttributes newAttr,
                    android.os.CancellationSignal cancel,
                    LayoutResultCallback callback,
                    android.os.Bundle extras) {
                android.print.PrintDocumentInfo info =
                        new android.print.PrintDocumentInfo.Builder(jobName + ".pdf")
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build();
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(android.print.PageRange[] pages,
                    android.os.ParcelFileDescriptor dest,
                    android.os.CancellationSignal cancel,
                    WriteResultCallback callback) {
                try (java.io.InputStream in = new java.io.FileInputStream(pdfFile);
                     java.io.OutputStream out =
                             new java.io.FileOutputStream(dest.getFileDescriptor())) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                    callback.onWriteFinished(new android.print.PageRange[]{
                            android.print.PageRange.ALL_PAGES});
                } catch (Exception e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }
        }, null);
    }

    // F9 — Apply UA preset to a GeckoSession
    private void applyUaPresetToSession(GeckoSession targetSession, SharedPreferences prefs) {
        String preset = prefs.getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");
        GeckoSessionSettings settings = targetSession.getSettings();
        switch (preset) {
            case "desktop":
                settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
                settings.setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
                break;
            case "iphone":
                settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                settings.setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
                settings.setUserAgentOverride("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
                break;
            case "ipad":
                settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                settings.setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
                settings.setUserAgentOverride("Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
                break;
            case "custom":
                String customUa = prefs.getString(SettingsKeys.PREF_USER_AGENT_CUSTOM_STRING, "");
                if (!customUa.isEmpty()) {
                    settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                    settings.setUserAgentOverride(customUa);
                }
                break;
            default: // "mobile"
                settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                settings.setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
                settings.setUserAgentOverride("");
                break;
        }
    }

    // F10 — Add to Home Screen
    private void addToHomeScreen() {
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;
        String id = "easybrowser_" + currentUrl.hashCode();
        String label = (currentTitle != null && !currentTitle.isEmpty()) ? currentTitle : currentUrl;
        if (label.length() > 25) label = label.substring(0, 25);

        Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
        launchIntent.setClass(this, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        IconCompat icon;
        Tab tab = tabManager != null ? tabManager.getCurrentTab() : null;
        Bitmap favicon = (tab != null) ? tab.getFavicon() : null;
        if (favicon != null) {
            icon = IconCompat.createWithBitmap(favicon);
        } else {
            icon = IconCompat.createWithResource(this, R.mipmap.ic_launcher);
        }

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(label)
                .setIcon(icon)
                .setIntent(launchIntent)
                .build();
        ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
        Toast.makeText(this, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
    }

    // F11 — Save to Reading List
    private void saveToReadingList() {
        if (session == null || currentUrl == null) return;
        String title = currentTitle != null ? currentTitle : currentUrl;
        String itemId = UUID.randomUUID().toString();
        Toast.makeText(this, R.string.saving_to_reading_list, Toast.LENGTH_SHORT).show();
        session.saveAsPdf().then(inputStream -> {
            if (inputStream == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.save_page_failed, Toast.LENGTH_SHORT).show());
                return GeckoResult.fromValue(null);
            }
            File dir = new File(getFilesDir(), "reading_list");
            dir.mkdirs();
            File pdfFile = new File(dir, itemId + ".pdf");
            boolean written = false;
            try (java.io.OutputStream out = new FileOutputStream(pdfFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = inputStream.read(buf)) != -1) out.write(buf, 0, r);
                written = true;
            } catch (Exception e) {
                pdfFile.delete();
            }
            final String contentPath = written ? pdfFile.getAbsolutePath() : null;
            ReadingListItem item = new ReadingListItem(itemId, title, currentUrl,
                    System.currentTimeMillis(), contentPath);
            readingListRepository.save(item, new ReadingListRepository.ReadingListCallback() {
                @Override public void onItemsLoaded(java.util.List<ReadingListItem> items) {}
                @Override public void onItemSaved() {
                    runOnUiThread(() -> Toast.makeText(BrowserActivity.this,
                            R.string.saved_to_reading_list, Toast.LENGTH_SHORT).show());
                }
                @Override public void onItemDeleted() {}
            });
            return GeckoResult.fromValue(null);
        }, e -> {
            runOnUiThread(() -> Toast.makeText(this,
                    R.string.save_page_failed, Toast.LENGTH_SHORT).show());
            return GeckoResult.fromValue(null);
        });
    }

    // F12 — Site info bottom sheet
    private void handleSecurityButtonClick() {
        if (UrlUtils.isInternalPageUrl(currentUrl)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.new_tab_info_title)
                    .setMessage(R.string.new_tab_info_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        showSiteInfoBottomSheet();
    }

    private void showSiteInfoBottomSheet() {
        if (currentUrl == null) return;
        boolean isSecure = currentUrl != null
                && currentUrl.toLowerCase(Locale.US).startsWith("https://")
                && (lastSecurityInfo == null || lastSecurityInfo.isSecure);
        String host = UrlUtils.getDisplayHost(currentUrl);
        PrivacyStatsManager.Report report = PrivacyStatsManager.getReport(this);
        SiteInfoBottomSheet sheet = SiteInfoBottomSheet.newInstance(
                currentUrl,
                isSecure,
                host != null ? host : currentUrl,
                smartShieldLevel.name(),
                calculateSmartShieldScore(),
                report.today.itemsBlocked);
        sheet.show(getSupportFragmentManager(), "site_info");
    }

    // F13 — Exit with optional auto-clear
    private void exitApp() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("auto_clear_on_exit", false)) {
            finish();
            return;
        }
        performAutoClear(this::finish);
    }

    // Clear cookies/cache/history per the auto-clear-on-exit prefs. Runs at most once.
    // Called from the Exit menu (with finish) and from onDestroy on a normal finish
    // (back press / Exit) so the data is cleared on any real exit, not only the menu.
    private void performAutoClear(Runnable onComplete) {
        if (autoClearTriggered) {
            if (onComplete != null) onComplete.run();
            return;
        }
        autoClearTriggered = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean clearCookies = prefs.getBoolean("auto_clear_cookies", true);
        boolean clearCache = prefs.getBoolean("auto_clear_cache", true);
        boolean clearHistory = prefs.getBoolean("auto_clear_history", true);

        long flags = 0;
        if (clearCookies) flags |= StorageController.ClearFlags.COOKIES
                | StorageController.ClearFlags.AUTH_SESSIONS
                | StorageController.ClearFlags.DOM_STORAGES;
        if (clearCache) flags |= StorageController.ClearFlags.ALL_CACHES;

        AtomicInteger pending = new AtomicInteger(0);
        if (flags != 0) pending.incrementAndGet();
        if (clearHistory) pending.incrementAndGet();
        if (pending.get() == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Runnable checkDone = () -> {
            if (pending.decrementAndGet() == 0 && onComplete != null) {
                runOnUiThread(onComplete);
            }
        };

        if (flags != 0) {
            RuntimeManager.getRuntime(this)
                    .getStorageController()
                    .clearData(flags)
                    .accept(v -> checkDone.run(), e -> checkDone.run());
        }
        if (clearHistory) {
            historyRepository.clearHistory(new HistoryRepository.HistoryCallback() {
                @Override public void onHistoryLoaded(java.util.List<HistoryItem> items) {}
                @Override public void onHistoryItemAdded(HistoryItem item) {}
                @Override public void onHistoryCleared() { checkDone.run(); }
            });
        }
    }

    // F14 — Inject user CSS style for current host
    void injectUserStyleIfNeeded(GeckoSession targetSession, String url) {
        if (url == null || targetSession == null) return;
        String host = UrlUtils.getDisplayHost(url);
        if (host == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String json = prefs.getString("userstyle_" + host, null);
        if (json == null) return;
        try {
            JSONObject obj = new JSONObject(json);
            if (!obj.optBoolean("enabled", true)) return;
            String css = obj.optString("css", "");
            if (css.isEmpty()) return;
            // Carry the CSS as base64 and decode at runtime. Hand-escaping CSS into a
            // JS single-quoted string left at least four breakouts (U+2028, U+2029,
            // </script> in CSS comments, lone CR). Base64 is alphanumeric + '/+='
            // which cannot break out of a JS string literal.
            String encoded = android.util.Base64.encodeToString(
                    css.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    android.util.Base64.NO_WRAP);
            String js = "(function(){try{var s=document.createElement('style');"
                    + "s.textContent=decodeURIComponent(escape(atob('" + encoded + "')));"
                    + "(document.head||document.documentElement).appendChild(s);}catch(e){}})();";
            runOnUiThread(() -> {
                try {
                    loadTrustedJavascript(targetSession, js);
                } catch (Exception e) {
                    Log.e("BrowserActivity", "Failed to inject user style for " + host, e);
                }
            });
        } catch (Exception ignored) {}
    }

    void injectPageUrlSyncIfNeeded(GeckoSession targetSession, String url) {
        if (targetSession == null || !isHttpUrl(url)) {
            return;
        }
        String js = "(function(){try{"
                + "var PREFIX='__EASY_BROWSER_PAGE_URL__';"
                + "if(window.__easyBrowserPageUrlSyncInstalled){"
                + "if(window.__easyBrowserReportPageUrl)window.__easyBrowserReportPageUrl();return;}"
                + "window.__easyBrowserPageUrlSyncInstalled=true;"
                + "var last='';"
                + "function report(){var h=location.href;if(!h||h===last)return;last=h;"
                + "var old=document.title;document.title=PREFIX+h;"
                + "setTimeout(function(){if(document.title===PREFIX+h)document.title=old;},80);}"
                + "window.__easyBrowserReportPageUrl=report;"
                + "['pushState','replaceState'].forEach(function(k){var o=history[k];"
                + "if(!o)return;history[k]=function(){var r=o.apply(this,arguments);"
                + "setTimeout(report,0);setTimeout(report,250);return r;};});"
                + "window.addEventListener('popstate',function(){setTimeout(report,0);});"
                + "window.addEventListener('hashchange',function(){setTimeout(report,0);});"
                + "document.addEventListener('yt-navigate-finish',report,true);"
                + "document.addEventListener('yt-page-data-updated',report,true);"
                + "setTimeout(report,0);setTimeout(report,500);setTimeout(report,1500);"
                + "}catch(e){}})();";
        runOnUiThread(() -> {
            try {
                loadTrustedJavascript(targetSession, js);
            } catch (Exception e) {
                Log.e("BrowserActivity", "Failed to inject page URL sync", e);
            }
        });
    }

    boolean isTrustedJavascriptUri(String uri) {
        return uri != null && trustedJavascriptUris.contains(uri);
    }

    private void loadTrustedJavascript(GeckoSession targetSession, String js) {
        if (targetSession == null || js == null || js.trim().isEmpty()) {
            return;
        }
        String scriptUri = "javascript:" + js;
        trustedJavascriptUris.add(scriptUri);
        try {
            targetSession.loadUri(scriptUri);
        } catch (RuntimeException e) {
            trustedJavascriptUris.remove(scriptUri);
            throw e;
        }
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> trustedJavascriptUris.remove(scriptUri),
                TRUSTED_JAVASCRIPT_URI_TTL_MS);
    }

}
