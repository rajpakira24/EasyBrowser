package com.webstudio.easybrowser.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.view.MenuItem;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.QuickAccessAdapter;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.AppDownloadManager;
import com.webstudio.easybrowser.managers.AppShortcutManager;
import com.webstudio.easybrowser.managers.PrivacyStatsManager;
import com.webstudio.easybrowser.managers.WeatherAlertManager;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.models.Suggestion;
import com.webstudio.easybrowser.models.WeatherSnapshot;
import com.webstudio.easybrowser.repository.QuickAccessRepository;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.repository.WeatherRepository;
import com.webstudio.easybrowser.utils.AppSettings;
import com.webstudio.easybrowser.utils.BrowserSuggestionProvider;
import com.webstudio.easybrowser.utils.EasyMotion;
import com.webstudio.easybrowser.utils.HomeBackgroundProvider;
import com.webstudio.easybrowser.utils.SearchSuggestionProvider;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.TabActionContract;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.WeatherAnimationMapper;
import com.webstudio.easybrowser.utils.UrlUtils;

import com.webstudio.easybrowser.adapters.SuggestionsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;

public class MainActivity extends AppCompatActivity implements QuickAccessAdapter.OnQuickAccessClickListener {
    private static final int HOME_SEARCH_NAV_GAP_DP = 14;
    private static final int HOME_SEARCH_MIN_BOTTOM_MARGIN_DP = 118;
    private static final int HOME_PHOTO_CREDIT_GAP_DP = 18;
    private static final int HOME_SCROLL_BOTTOM_GAP_DP = 28;
    private static final long HOME_ENTRANCE_DURATION_MS = 360L;
    private static final long HOME_ENTRANCE_STAGGER_MS = 65L;
    private static final int QUICK_ACCESS_MENU_OPEN_NEW_TAB = 1;
    private static final int QUICK_ACCESS_MENU_OPEN_NEW_TAB_IN_GROUP = 2;
    private static final int QUICK_ACCESS_MENU_OPEN_PRIVATE_TAB = 3;
    private static final int QUICK_ACCESS_MENU_DOWNLOAD_LINK = 4;
    private static final int QUICK_ACCESS_MENU_REMOVE = 5;
    private static final int QUICK_ACCESS_MENU_PIN = 6;
    private static final int QUICK_ACCESS_MAX_VISIBLE_ITEMS = 5;
    private static final int QUICK_ACCESS_LOAD_LIMIT = 8;
    private static final int QUICK_ACCESS_MIN_ITEM_WIDTH_DP = 56;

    private EditText urlInput;
    private ImageButton micButton;
    private ImageButton securityButton;
    private ImageButton clearButton;
    // One-time "This time search in:" engine override; consumed by the next search submit.
    private String oneTimeSearchEngineUrl;
    private ImageButton searchButton;
    private RecyclerView quickAccessRecycler;
    private View weatherWidget;
    private LottieAnimationView weatherIcon;
    private TextView weatherLocation;
    private TextView weatherCondition;
    private TextView weatherTemperature;
    private View privacyStatsCard;
    private ImageButton privacyStatsToggle;
    private TextView quickAccessTitle;
    private TextView protectedPagesStat;
    private TextView blockedItemsStat;
    private TextView timeSavedStat;
    private ImageView homeBackgroundImage;
    private View homeBackgroundScrim;
    private TextView photoCredit;
    private View homeContentContainer;
    private View homeSearchBar;
    private BottomNavigationView bottomNav;
    // System nav-bar inset (edge-to-edge). Owned by applyHomeInsets, read by the single
    // bottom-chrome spacing method so search-bar/scroll padding clears the gesture bar.
    private int systemBarsBottomInset;
    private final java.util.concurrent.ExecutorService ioExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private QuickAccessRepository quickAccessRepository;
    private TabRepository tabRepository;
    private WeatherRepository weatherRepository;
    private QuickAccessAdapter quickAccessAdapter;
    private HomeBackgroundProvider.Photo currentHomePhoto;
    private String appliedWallpaperKey;
    private boolean homeWallpaperOfflinePack;
    private boolean homeEntranceAnimated;
    private ActivityResultLauncher<Intent> tabManagerLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private final SharedPreferences.OnSharedPreferenceChangeListener privacyStatsListener =
            (sharedPreferences, key) -> {
                if (PrivacyStatsManager.isStatsKey(key)) {
                    runOnUiThread(this::updatePrivacyStats);
                }
            };

    private ActivityResultLauncher<Intent> speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyHomeSystemBars();
        applyHomeInsets();

        initializeRepositories();
        initializeViews();
        setupToolbar();
        setupUrlInput();
        setupQuickAccess();
        setupTabManagerLauncher();
        setupNotificationPermissionLauncher();
        setupBottomNavigation();
        setupHomeBottomChromeSpacing();
        setupClickListeners();
        setupSpeechRecognition();
        requestNotificationPermissionIfNeeded();
        setupHomeBackground();
        applyHomeTheme();
        setupWeatherWidget();
        applyHomeSectionVisibility();
        updatePrivacyStats();
        runHomeEntranceAnimation();
        handleIncomingIntent(getIntent());
    }

    private void applyHomeSystemBars() {
        // Edge-to-edge: the wallpaper fills behind transparent status + nav bars.
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // Home draws light text over a dark wallpaper → use light (white) system-bar icons.
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    // Pad the foreground (content, bottom nav, search bar) by the system-bar insets so nothing
    // hides behind the status/nav bars while the wallpaper still draws full-bleed behind them.
    private void applyHomeInsets() {
        View root = findViewById(R.id.home_root);
        if (root == null) {
            return;
        }
        View content = findViewById(R.id.home_content_container);
        View weather = findViewById(R.id.weather_widget);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            systemBarsBottomInset = bars.bottom;
            // Own only the top + horizontal padding here. The bottom edge (search-bar margin
            // and scroll clearance) is owned solely by updateHomeBottomChromeSpacing() so the
            // two paths never fight over the same margins and the inset isn't clobbered.
            if (content != null) {
                content.setPadding(dp(16), dp(28) + bars.top, dp(16),
                        content.getPaddingBottom());
            }
            // Weather widget floats at top|end — push it below the status bar.
            if (weather != null
                    && weather.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams wlp =
                        (ViewGroup.MarginLayoutParams) weather.getLayoutParams();
                wlp.topMargin = dp(7) + bars.top;
                weather.setLayoutParams(wlp);
            }
            updateHomeBottomChromeSpacing();
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyHomeSystemBars();
        // Discard any unconsumed "this time search in" override when returning to home so a
        // stale engine can't silently apply to a later search; applyHomeTheme() below repaints
        // the search icon back to the default engine.
        oneTimeSearchEngineUrl = null;
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(privacyStatsListener);
        setupHomeBackground();
        applyHomeTheme();
        applyHomeSectionVisibility();
        updateWeatherWidget(false);
        updatePrivacyStats();
        updateTabCountIcon();
        reloadQuickAccess();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(privacyStatsListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    private void initializeViews() {
        urlInput = findViewById(R.id.url_input);
        micButton = findViewById(R.id.btn_mic);
        securityButton = findViewById(R.id.btn_security);
        clearButton = findViewById(R.id.btn_clear);
        searchButton = findViewById(R.id.btn_search);
        quickAccessRecycler = findViewById(R.id.quick_access_recycler);
        weatherWidget = findViewById(R.id.weather_widget);
        weatherIcon = findViewById(R.id.weather_icon);
        weatherLocation = findViewById(R.id.weather_location);
        weatherCondition = findViewById(R.id.weather_condition);
        weatherTemperature = findViewById(R.id.weather_temperature);
        privacyStatsCard = findViewById(R.id.privacy_stats_card);
        privacyStatsToggle = findViewById(R.id.btn_toggle_privacy_stats);
        quickAccessTitle = findViewById(R.id.quick_access_title);
        protectedPagesStat = findViewById(R.id.stat_protected_pages);
        blockedItemsStat = findViewById(R.id.stat_blocked_items);
        timeSavedStat = findViewById(R.id.stat_time_saved);
        homeBackgroundImage = findViewById(R.id.home_background_image);
        homeBackgroundScrim = findViewById(R.id.home_background_scrim);
        photoCredit = findViewById(R.id.home_photo_credit);
        homeContentContainer = findViewById(R.id.home_content_container);
        homeSearchBar = findViewById(R.id.home_edge_search_bar);
        bottomNav = findViewById(R.id.bottom_navigation);
        updateTabCountIcon();
    }

    private void initializeRepositories() {
        quickAccessRepository = new QuickAccessRepository(this);
        tabRepository = new TabRepository(this);
        weatherRepository = new WeatherRepository(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }
    }

    private void handleUrlInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        AnalyticsManager.logNavigationSubmitted(this, input, false);
        String target;
        if (oneTimeSearchEngineUrl != null && UrlUtils.isSearchQuery(input)) {
            target = UrlUtils.getSearchUrlForEngine(this, input, oneTimeSearchEngineUrl);
        } else {
            target = UrlUtils.getUrlOrSearchUrl(this, input);
        }
        oneTimeSearchEngineUrl = null;
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_URL, target);
        startActivity(intent);
    }

    // Load the current (or one-time override) search engine's favicon onto the search button,
    // clearing the theme tint so the colored favicon shows like Firefox's engine icon.
    private void updateSearchEngineIcon() {
        updateSearchEngineIcon(securityButton);
    }

    private void updateSearchEngineIcon(ImageButton target) {
        if (target == null) {
            return;
        }
        String engineUrl = oneTimeSearchEngineUrl != null
                ? oneTimeSearchEngineUrl : UrlUtils.getSearchEngineUrl(this, false);
        int tint = ThemeEngine.homePalette(this).onSurface;
        Drawable fallback = ContextCompat.getDrawable(this, R.drawable.ic_search);
        if (fallback != null) {
            fallback = fallback.mutate();
            fallback.setTint(tint);
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

    private void searchWithEngine(String input, String engineUrl) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_URL,
                UrlUtils.getSearchUrlForEngine(this, input, engineUrl));
        startActivity(intent);
    }

    private void showSearchEnginePicker() {
        SearchEnginePickerPopup.show(this, securityButton, new SearchEnginePickerPopup.Callback() {
            @Override
            public void onEngineSelected(String name, String engineUrl) {
                String input = urlInput.getText() != null
                        ? urlInput.getText().toString().trim() : "";
                if (!input.isEmpty()) {
                    searchWithEngine(input, engineUrl);
                } else {
                    // No query yet — apply the engine to the next search and focus the field.
                    oneTimeSearchEngineUrl = engineUrl;
                    updateSearchEngineIcon();
                    urlInput.requestFocus();
                    Toast.makeText(MainActivity.this,
                            getString(R.string.search_picker_title) + " " + name,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBookmarks() {
                startActivity(new Intent(MainActivity.this, BookmarksActivity.class));
            }

            @Override
            public void onTabs() {
                launchTabManager();
            }

            @Override
            public void onHistory() {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }

            @Override
            public void onSearchSettings() {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    private void showSearchEnginePickerForPopup(ImageButton anchor, EditText searchInput,
                                                android.app.Dialog dialog) {
        SearchEnginePickerPopup.show(this, anchor, new SearchEnginePickerPopup.Callback() {
            @Override
            public void onEngineSelected(String name, String engineUrl) {
                oneTimeSearchEngineUrl = engineUrl;
                String input = searchInput.getText() != null
                        ? searchInput.getText().toString().trim() : "";
                if (!input.isEmpty()) {
                    dialog.dismiss();
                    handleUrlInput(input);
                } else {
                    updateSearchEngineIcon();
                    updateSearchEngineIcon(anchor);
                    searchInput.requestFocus();
                    Toast.makeText(MainActivity.this,
                            getString(R.string.search_picker_title) + " " + name,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBookmarks() {
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this, BookmarksActivity.class));
            }

            @Override
            public void onTabs() {
                dialog.dismiss();
                launchTabManager();
            }

            @Override
            public void onHistory() {
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }

            @Override
            public void onSearchSettings() {
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (handleAppShortcutIntent(intent)) {
            return;
        }

        String input = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            // Restrict ACTION_VIEW to http(s). Anything else (data:, javascript:, file:,
            // content:, intent:) can either escape into a privileged origin or be used
            // to spoof one — and our intent-filter only declares http/https anyway.
            String dataString = intent.getData().toString();
            String lower = dataString.trim().toLowerCase(java.util.Locale.US);
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                input = dataString;
            }
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            // EXTRA_TEXT may be a plain search query; getUrlOrSearchUrl decides which.
            input = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (input != null && !input.trim().isEmpty()) {
            handleUrlInput(input);
        }
    }

    private boolean handleAppShortcutIntent(Intent intent) {
        String action = intent.getAction();
        String legacyAction = intent.getStringExtra("action");
        if (AppShortcutManager.ACTION_WIDGETS.equals(action)
                || "widgets".equals(legacyAction)) {
            Intent dashboardIntent = new Intent(this, WidgetsDashboardActivity.class)
                    .setAction(AppShortcutManager.ACTION_WIDGETS)
                    .putExtra(AppShortcutManager.EXTRA_FROM_APP_SHORTCUT, true);
            startActivity(dashboardIntent);
            return true;
        }
        if (AppShortcutManager.ACTION_NEW_PRIVATE_TAB.equals(action)
                || "private_tab".equals(legacyAction)) {
            openShortcutBrowser(AppShortcutManager.ACTION_NEW_PRIVATE_TAB, true);
            return true;
        }
        if (AppShortcutManager.ACTION_NEW_TAB.equals(action)
                || "new_tab".equals(legacyAction)) {
            openShortcutBrowser(AppShortcutManager.ACTION_NEW_TAB, false);
            return true;
        }
        return false;
    }

    private void openShortcutBrowser(String action, boolean privateTab) {
        Intent browserIntent = new Intent(this, BrowserActivity.class)
                .setAction(action)
                .putExtra(AppShortcutManager.EXTRA_FROM_APP_SHORTCUT, true)
                .putExtra(privateTab ? BrowserActivity.EXTRA_PRIVATE_TAB : BrowserActivity.EXTRA_NEW_TAB, true);
        startActivity(browserIntent);
    }

    private void setupUrlInput() {
        // Clear button visibility
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear button click handler
        clearButton.setOnClickListener(v -> {
            urlInput.setText("");
            urlInput.requestFocus();
            showKeyboard(urlInput);
        });

        // Search button click handler
        searchButton.setOnClickListener(v -> {
            showSearchPopup();
        });

        urlInput.setFocusable(false);
        urlInput.setOnClickListener(v -> showSearchPopup());
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        }
        urlInput.clearFocus();
    }

    private void setupQuickAccess() {
        quickAccessRecycler.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        quickAccessAdapter = new QuickAccessAdapter(new ArrayList<>(), this);
        quickAccessRecycler.setAdapter(quickAccessAdapter);
        quickAccessRecycler.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                       oldLeft, oldTop, oldRight, oldBottom) ->
                updateQuickAccessItemWidth());
        quickAccessRecycler.post(this::updateQuickAccessItemWidth);

        // Load quick access items
        quickAccessRepository.getMostVisitedItems(QUICK_ACCESS_LOAD_LIMIT, new QuickAccessRepository.QuickAccessCallback() {
            @Override
            public void onQuickAccessItemsLoaded(List<QuickAccessItem> items) {
                runOnUiThread(() -> quickAccessAdapter.updateItems(items));
            }

            @Override
            public void onQuickAccessItemAdded(QuickAccessItem item) {
                // Not needed for this implementation
            }

            @Override
            public void onQuickAccessItemRemoved(QuickAccessItem item) {
                // Not needed for this implementation
            }
        });
    }

    private void updateQuickAccessItemWidth() {
        if (quickAccessRecycler == null || quickAccessAdapter == null) {
            return;
        }

        int rowWidth = quickAccessRecycler.getWidth();
        if (rowWidth <= 0) {
            return;
        }

        int minItemWidth = dp(QUICK_ACCESS_MIN_ITEM_WIDTH_DP);
        int visibleItems = Math.max(1, rowWidth / minItemWidth);
        visibleItems = Math.min(visibleItems, QUICK_ACCESS_MAX_VISIBLE_ITEMS);
        int trailingInset = rowWidth % visibleItems;
        if (quickAccessRecycler.getPaddingEnd() != trailingInset) {
            quickAccessRecycler.setPaddingRelative(0, quickAccessRecycler.getPaddingTop(),
                    trailingInset, quickAccessRecycler.getPaddingBottom());
        }
        quickAccessAdapter.setItemWidth((rowWidth - trailingInset) / visibleItems);
    }

    private void reloadQuickAccess() {
        quickAccessRepository.getMostVisitedItems(QUICK_ACCESS_LOAD_LIMIT, new QuickAccessRepository.QuickAccessCallback() {
            @Override
            public void onQuickAccessItemsLoaded(List<QuickAccessItem> items) {
                runOnUiThread(() -> quickAccessAdapter.updateItems(items));
            }

            @Override
            public void onQuickAccessItemAdded(QuickAccessItem item) {}

            @Override
            public void onQuickAccessItemRemoved(QuickAccessItem item) {}
        });
    }

    private void setupWeatherWidget() {
        if (weatherWidget == null) {
            return;
        }
        weatherWidget.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WeatherActivity.class)
                        .putExtra(WeatherActivity.EXTRA_REQUEST_CURRENT_LOCATION, true)));
        if (!PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsKeys.PREF_SHOW_WEATHER_WIDGET, true)) {
            return;
        }
        WeatherSnapshot cached = weatherRepository.getCachedSnapshot();
        if (cached != null) {
            showWeatherSnapshot(cached);
        }
        updateWeatherWidget(false);
    }

    private void setupHomeBackground() {
        AppSettings settings = new AppSettings(this);
        String wallpaperMode = settings.getWallpaperMode();
        String userWallpaperUri = settings.getWallpaperUserUri();
        String collection = settings.getWallpaperCollection();
        int dayBucket = getWallpaperDayBucket();
        int blur = settings.getWallpaperBlur();
        int overlay = settings.getWallpaperOverlay();
        boolean favoritesOnly = settings.isWallpaperFavoritesOnly();
        boolean offlinePack = settings.isWallpaperOfflinePackEnabled();
        Set<String> favoriteIds = settings.getWallpaperFavoriteIds();
        Set<String> preferredPhotoIds = favoritesOnly ? favoriteIds : null;
        String wallpaperKey = wallpaperMode + ":"
                + ("user".equals(wallpaperMode)
                ? userWallpaperUri
                : collection + ":" + dayBucket)
                + ":b" + blur + ":o" + overlay
                + ":fav" + (favoritesOnly ? favoriteIds.hashCode() : 0)
                + ":offline" + offlinePack;
        if (wallpaperKey.equals(appliedWallpaperKey)) {
            return;
        }
        appliedWallpaperKey = wallpaperKey;
        homeWallpaperOfflinePack = offlinePack;
        applyHomeWallpaperEffects(blur, overlay);
        if ("user".equals(wallpaperMode)
                && userWallpaperUri != null && !userWallpaperUri.trim().isEmpty()) {
            currentHomePhoto = null;
            if (homeBackgroundImage != null) {
                loadHomeBackground(Uri.parse(userWallpaperUri));
            }
            if (photoCredit != null) {
                photoCredit.setText(R.string.wallpaper_user_image);
                photoCredit.setOnClickListener(null);
            }
            return;
        }

        currentHomePhoto = HomeBackgroundProvider.photoForDailyMode(
                wallpaperMode, collection, dayBucket, preferredPhotoIds);
        if (homeBackgroundImage != null) {
            loadHomeBackground(currentHomePhoto.getImageUrl());
        }
        if (offlinePack) {
            prefetchHomeWallpaperPack(wallpaperMode, collection, preferredPhotoIds);
        }
        if (photoCredit != null) {
            photoCredit.setText(currentHomePhoto.getCreditText());
            photoCredit.setOnClickListener(v -> handleUrlInput(currentHomePhoto.getSourceUrl()));
        }
    }

    private void loadHomeBackground(Object source) {
        if (homeBackgroundImage == null) {
            return;
        }
        Glide.with(this)
                .load(source)
                .diskCacheStrategy(homeWallpaperOfflinePack
                        ? DiskCacheStrategy.ALL
                        : DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.bg_home_photo_scrim)
                .error(R.drawable.bg_home_photo_scrim)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(450))
                .into(homeBackgroundImage);
    }

    private void prefetchHomeWallpaperPack(String mode, String collection, Set<String> preferredPhotoIds) {
        for (HomeBackgroundProvider.Photo photo :
                HomeBackgroundProvider.photosForMode(mode, collection, preferredPhotoIds)) {
            Glide.with(this)
                    .load(photo.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload();
        }
    }

    private void runHomeEntranceAnimation() {
        if (homeEntranceAnimated) {
            return;
        }
        homeEntranceAnimated = true;
        View[] stagedViews = new View[]{
                privacyStatsCard,
                quickAccessTitle,
                quickAccessRecycler,
                weatherWidget,
                photoCredit,
                bottomNav
        };
        for (View view : stagedViews) {
            EasyMotion.prepareFadeSlide(view, dp(12));
        }
        if (homeSearchBar != null) {
            homeSearchBar.setAlpha(0f);
            homeSearchBar.setScaleX(0.92f);
            homeSearchBar.setTranslationY(dp(14));
        }
        if (homeContentContainer != null) {
            homeContentContainer.post(() -> {
                for (int i = 0; i < stagedViews.length; i++) {
                    EasyMotion.fadeSlideIn(stagedViews[i],
                            i * EasyMotion.STAGGER_SHORT,
                            EasyMotion.DURATION_LONG);
                }
                if (homeSearchBar != null) {
                    homeSearchBar.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .translationY(0f)
                            .setStartDelay(EasyMotion.STAGGER_SHORT)
                            .setDuration(EasyMotion.DURATION_LONG)
                            .setInterpolator(EasyMotion.EMPHASIZED)
                            .start();
                }
            });
        }
    }

    private void applyHomeWallpaperEffects(int blur, int overlay) {
        if (homeBackgroundScrim != null) {
            homeBackgroundScrim.setBackground(createHomeScrim(overlay));
        }
        if (homeBackgroundImage == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (blur <= 0) {
            homeBackgroundImage.setRenderEffect(null);
            return;
        }
        float radius = Math.min(24, Math.max(0, blur));
        homeBackgroundImage.setRenderEffect(RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP));
    }

    private GradientDrawable createHomeScrim(int overlay) {
        int alpha = Math.round(255f * Math.min(80, Math.max(0, overlay)) / 100f);
        int topAlpha = Math.min(230, alpha + 28);
        int bottomAlpha = Math.min(245, alpha + 52);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.argb(topAlpha, 8, 17, 28),
                        Color.argb(alpha, 8, 17, 28),
                        Color.argb(bottomAlpha, 8, 17, 28)
                });
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int getWallpaperDayBucket() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        return now.get(java.util.Calendar.YEAR) * 1000
                + now.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private int getIntPreference(SharedPreferences prefs, String key, int defaultValue) {
        try {
            return prefs.getInt(key, defaultValue);
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    private void applyHomeTheme() {
        ThemeEngine.Palette palette = ThemeEngine.homePalette(this);
        if (homeSearchBar != null) {
            homeSearchBar.setBackground(createRoundedDrawable(palette.searchBackground, dp(27)));
        }
        if (securityButton != null) {
            securityButton.setBackground(createRoundedDrawable(
                    palette.searchIconBackground, dp(20)));
            securityButton.setColorFilter(palette.onSurface);
        }
        // Show the current search engine's favicon on the icon (overrides the tint above).
        updateSearchEngineIcon();
        tintHomeButton(micButton, palette.onSurface);
        tintHomeButton(clearButton, palette.onSurface);
        tintHomeButton(searchButton, palette.onSurface);
        tintHomeButton(privacyStatsToggle, palette.onSurfaceMuted);
        if (urlInput != null) {
            urlInput.setTextColor(palette.onSurface);
            urlInput.setHintTextColor(palette.onSurfaceMuted);
        }
        if (privacyStatsCard instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) privacyStatsCard;
            card.setCardBackgroundColor(palette.panelBackground);
            card.setStrokeColor(palette.panelBorder);
        }
        if (weatherWidget instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) weatherWidget;
            card.setCardBackgroundColor(palette.panelStrongBackground);
            card.setStrokeColor(palette.panelBorder);
            card.setStrokeWidth(dp(1));
            card.setCardElevation(dp(3));
        }
        if (quickAccessTitle != null) {
            quickAccessTitle.setTextColor(palette.onSurface);
        }
        if (protectedPagesStat != null) {
            protectedPagesStat.setTextColor(palette.accent);
        }
        if (blockedItemsStat != null) {
            blockedItemsStat.setTextColor(palette.accentSoft);
        }
        if (timeSavedStat != null) {
            timeSavedStat.setTextColor(palette.accent);
        }
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(palette.panelStrongBackground);
            ColorStateList navColors = createNavColorStateList(palette);
            bottomNav.setItemIconTintList(navColors);
            bottomNav.setItemTextColor(navColors);
            bottomNav.setItemRippleColor(ColorStateList.valueOf(palette.accentSoft));
        }
    }

    private void tintHomeButton(ImageButton button, int color) {
        if (button != null) {
            button.setColorFilter(color);
        }
    }

    private ColorStateList createNavColorStateList(ThemeEngine.Palette palette) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        palette.accent,
                        palette.onSurfaceMuted
                });
    }

    private void applyHomeSectionVisibility() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showPrivacyStats = prefs.getBoolean(SettingsKeys.PREF_SHOW_PRIVACY_STATS, true);
        boolean showQuickAccess = prefs.getBoolean(SettingsKeys.PREF_SHOW_QUICK_ACCESS, true);
        boolean showWeather = prefs.getBoolean(SettingsKeys.PREF_SHOW_WEATHER_WIDGET, true);
        if (weatherWidget != null) {
            weatherWidget.setVisibility(showWeather ? View.VISIBLE : View.GONE);
        }
        privacyStatsCard.setVisibility(showPrivacyStats ? View.VISIBLE : View.GONE);
        quickAccessTitle.setVisibility(showQuickAccess ? View.VISIBLE : View.GONE);
        quickAccessRecycler.setVisibility(showQuickAccess ? View.VISIBLE : View.GONE);
    }

    private void updateWeatherWidget(boolean forceRefresh) {
        if (weatherWidget == null || weatherWidget.getVisibility() != View.VISIBLE) {
            return;
        }
        weatherRepository.getWeather(forceRefresh, new WeatherRepository.WeatherCallback() {
            @Override
            public void onWeatherLoaded(WeatherSnapshot snapshot, boolean fromCache) {
                runOnUiThread(() -> {
                    showWeatherSnapshot(snapshot);
                    if (!fromCache) {
                        WeatherAlertManager.maybeNotify(MainActivity.this,
                                snapshot, weatherRepository.getUnits());
                    }
                });
            }

            @Override
            public void onWeatherError(Exception error, WeatherSnapshot cachedSnapshot) {
                runOnUiThread(() -> {
                    if (cachedSnapshot != null) {
                        showWeatherSnapshot(cachedSnapshot);
                    } else {
                        weatherLocation.setText(R.string.weather_unavailable);
                        weatherCondition.setText(R.string.weather_summary);
                        weatherTemperature.setText("--");
                        setWeatherWidgetAnimation(null);
                        weatherWidget.setContentDescription(getString(R.string.weather_unavailable));
                    }
                });
            }
        });
    }

    private void showWeatherSnapshot(WeatherSnapshot snapshot) {
        String units = weatherRepository.getUnits();
        weatherLocation.setText(snapshot.getLocationName());
        weatherCondition.setText(snapshot.getCondition());
        weatherTemperature.setText(snapshot.formatTemperature(units));
        setWeatherWidgetAnimation(snapshot);
        if (weatherWidget != null) {
            weatherWidget.setContentDescription(snapshot.getLocationName() + ", "
                    + snapshot.getCondition() + ", " + snapshot.formatTemperature(units));
        }
    }

    private void setWeatherWidgetAnimation(WeatherSnapshot snapshot) {
        if (weatherIcon == null) {
            return;
        }
        weatherIcon.setAnimation(WeatherAnimationMapper.animationFor(snapshot,
                weatherRepository.getUnits()));
        weatherIcon.setRepeatCount(LottieDrawable.INFINITE);
        weatherIcon.setRepeatMode(LottieDrawable.RESTART);
        weatherIcon.playAnimation();
    }

    private void updatePrivacyStats() {
        PrivacyStatsManager.Stats stats = PrivacyStatsManager.getStats(this);

        protectedPagesStat.setText(String.valueOf(stats.pagesProtected));
        blockedItemsStat.setText(String.valueOf(stats.itemsBlocked));
        timeSavedStat.setText(formatTimeSaved(stats.timeSavedSeconds));
    }

    // One-way: hides the whole privacy-stats card via the same pref Settings > Display already
    // exposes. There is no re-show affordance on home by design — re-enabling requires Settings,
    // so this never needs an "eye-off" state.
    private void hidePrivacyStatsCard() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(SettingsKeys.PREF_SHOW_PRIVACY_STATS, false)
                .apply();
        applyHomeSectionVisibility();
        Toast.makeText(this, R.string.privacy_stats_hidden_toast, Toast.LENGTH_LONG).show();
    }

    private String formatTimeSaved(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        return (minutes / 60) + "h";
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            EasyMotion.animateBottomBarSelection(bottomNav, itemId);
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_search) {
                showSearchPopup();
                return true;
            } else if (itemId == R.id.nav_tabs) {
                launchTabManager();
                return true;
            } else if (itemId == R.id.nav_bookmarks) {
                startActivity(new Intent(MainActivity.this, BookmarksActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                showHomeMoreMenu();
                return true;
            }
            return false;
        });
        updateTabCountIcon();
    }

    private void setupTabManagerLauncher() {
        tabManagerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    updateTabCountIcon();
                    if (result.getData() != null) {
                        handleTabManagerResult(result.getData());
                    }
                });
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean(SettingsKeys.PREF_NOTIFICATION_PERMISSION_REQUESTED, true)
                        .apply());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || notificationPermissionLauncher == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(SettingsKeys.PREF_NOTIFICATION_PERMISSION_REQUESTED, false)) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            prefs.edit()
                    .putBoolean(SettingsKeys.PREF_NOTIFICATION_PERMISSION_REQUESTED, true)
                    .apply();
            return;
        }
        prefs.edit()
                .putBoolean(SettingsKeys.PREF_NOTIFICATION_PERMISSION_REQUESTED, true)
                .apply();
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void launchTabManager() {
        Intent intent = new Intent(this, TabManagerActivity.class);
        String currentTabId = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(TabsActivity.EXTRA_CURRENT_TAB_ID, null);
        if (currentTabId != null && !currentTabId.trim().isEmpty()) {
            intent.putExtra(TabsActivity.EXTRA_CURRENT_TAB_ID, currentTabId);
        }
        if (tabManagerLauncher != null) {
            tabManagerLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    private void handleTabManagerResult(Intent data) {
        if (handleTabActionResult(data.getStringExtra(TabActionContract.EXTRA_ACTIONS))) {
            return;
        }
        if (data.hasExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB)) {
            boolean isPrivate = data.getBooleanExtra(TabsActivity.RESULT_CREATE_PRIVATE_TAB, false);
            startActivity(new Intent(this, BrowserActivity.class)
                    .putExtra(isPrivate ? "private_tab" : "new_tab", true));
            return;
        }

        String restoreUrl = data.getStringExtra(TabsActivity.RESULT_RESTORE_URL);
        if (restoreUrl != null && !restoreUrl.trim().isEmpty()) {
            startActivity(new Intent(this, BrowserActivity.class)
                    .putExtra("url", restoreUrl)
                    .putExtra("new_tab", true));
            return;
        }

        String selectedTabId = data.getStringExtra(TabsActivity.RESULT_SELECTED_TAB_ID);
        if (selectedTabId != null && !selectedTabId.trim().isEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(TabsActivity.EXTRA_CURRENT_TAB_ID, selectedTabId)
                    .apply();
            startActivity(new Intent(this, BrowserActivity.class));
        }
    }

    private boolean handleTabActionResult(String payload) {
        List<TabActionContract.Action> actions = TabActionContract.parse(payload);
        if (actions.isEmpty()) {
            return false;
        }
        for (TabActionContract.Action action : actions) {
            if (TabActionContract.TYPE_CREATE_TAB.equals(action.getType())) {
                startActivity(new Intent(this, BrowserActivity.class)
                        .putExtra(action.isPrivate() ? "private_tab" : "new_tab", true));
                return true;
            }
            if (TabActionContract.TYPE_RESTORE_URL.equals(action.getType())) {
                String url = action.getUrl();
                if (!url.trim().isEmpty()) {
                    startActivity(new Intent(this, BrowserActivity.class)
                            .putExtra("url", url)
                            .putExtra("new_tab", true));
                    return true;
                }
            }
            if (TabActionContract.TYPE_SELECT_TAB.equals(action.getType())) {
                String tabId = action.getTabId();
                if (!tabId.trim().isEmpty()) {
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString(TabsActivity.EXTRA_CURRENT_TAB_ID, tabId)
                            .apply();
                    startActivity(new Intent(this, BrowserActivity.class));
                    return true;
                }
            }
        }
        return true;
    }

    private void setupHomeBottomChromeSpacing() {
        if (bottomNav == null || homeSearchBar == null) {
            return;
        }
        View.OnLayoutChangeListener spacingListener =
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateHomeBottomChromeSpacing();
        bottomNav.addOnLayoutChangeListener(spacingListener);
        homeSearchBar.addOnLayoutChangeListener(spacingListener);
        bottomNav.post(this::updateHomeBottomChromeSpacing);
    }

    private void updateHomeBottomChromeSpacing() {
        if (bottomNav == null || homeSearchBar == null) {
            return;
        }
        int bottomNavHeight = getBottomNavClearance();
        int searchBarHeight = homeSearchBar.getHeight() > 0 ? homeSearchBar.getHeight() : dp(48);
        int searchBottomMargin = Math.max(
                bottomNavHeight + dp(HOME_SEARCH_NAV_GAP_DP),
                dp(HOME_SEARCH_MIN_BOTTOM_MARGIN_DP) + systemBarsBottomInset);

        setBottomMargin(homeSearchBar, searchBottomMargin);
        if (photoCredit != null) {
            setBottomMargin(photoCredit,
                    searchBottomMargin + searchBarHeight + dp(HOME_PHOTO_CREDIT_GAP_DP));
        }
        if (homeContentContainer != null) {
            homeContentContainer.setPadding(
                    homeContentContainer.getPaddingLeft(),
                    homeContentContainer.getPaddingTop(),
                    homeContentContainer.getPaddingRight(),
                    searchBottomMargin + searchBarHeight + dp(HOME_SCROLL_BOTTOM_GAP_DP));
        }
    }

    private int getBottomNavClearance() {
        View parent = homeSearchBar.getParent() instanceof View ? (View) homeSearchBar.getParent() : null;
        int parentHeight = parent != null ? parent.getHeight() : 0;
        int bottomNavTop = bottomNav.getTop();
        if (parentHeight > 0 && bottomNavTop > 0 && bottomNavTop < parentHeight) {
            return parentHeight - bottomNavTop;
        }
        return bottomNav.getHeight() > 0 ? bottomNav.getHeight() : dp(72);
    }

    private void setBottomMargin(View view, int bottomMargin) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
        if (marginParams.bottomMargin == bottomMargin) {
            return;
        }
        marginParams.bottomMargin = bottomMargin;
        view.setLayoutParams(marginParams);
    }

    private void updateTabCountIcon() {
        if (bottomNav == null || bottomNav.getMenu().findItem(R.id.nav_tabs) == null) {
            return;
        }
        // Read the tab count off the UI thread (Room + prefs) to avoid main-thread DB blocking.
        ioExecutor.execute(() -> {
            int count = Math.max(1, getSavedPublicTabCount());
            runOnUiThread(() -> {
                if (bottomNav == null) {
                    return;
                }
                MenuItem item = bottomNav.getMenu().findItem(R.id.nav_tabs);
                if (item != null) {
                    item.setIcon(createTabCountIcon(count));
                }
            });
        });
    }

    private int getSavedPublicTabCount() {
        int roomCount = tabRepository != null ? tabRepository.getTabCountBlocking(false) : 0;
        if (roomCount > 0) {
            return roomCount;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedTabs = prefs.getString("saved_tabs", null);
        if (savedTabs == null || savedTabs.trim().isEmpty()) {
            return 0;
        }
        try {
            return new JSONArray(savedTabs).length();
        } catch (Exception e) {
            return 0;
        }
    }

    private Drawable createTabCountIcon(int count) {
        int size = dp(28);
        int strokeWidth = dp(2);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int iconColor = ContextCompat.getColor(this, R.color.home_accent_ink);

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

    private void setupClickListeners() {
        micButton.setOnClickListener(v -> startVoiceRecognition());

        if (privacyStatsToggle != null) {
            privacyStatsToggle.setOnClickListener(v -> hidePrivacyStatsCard());
        }

        securityButton.setOnClickListener(v -> showSearchEnginePicker());
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

    private final SearchSuggestionProvider suggestionProvider = new SearchSuggestionProvider();

    private void showSearchPopup() {
        final android.app.Dialog dialog = new android.app.Dialog(
                this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.dialog_search_popup);
        dialog.setCanceledOnTouchOutside(false);
        if (homeSearchBar != null) {
            homeSearchBar.setVisibility(View.INVISIBLE);
        }
        dialog.setOnDismissListener(d -> {
            if (homeSearchBar != null) {
                homeSearchBar.setVisibility(View.VISIBLE);
            }
            // Popup may have changed the one-time engine override — resync the home icon.
            updateSearchEngineIcon();
        });

        EditText searchInput = dialog.findViewById(R.id.search_popup_input);
        ImageButton popupEngine = dialog.findViewById(R.id.btn_search_popup_engine);
        ImageButton popupMic = dialog.findViewById(R.id.btn_search_popup_mic);
        ImageButton popupGo = dialog.findViewById(R.id.btn_search_popup_go);
        RecyclerView suggestionsRecycler = dialog.findViewById(R.id.suggestions_recycler);
        if (searchInput == null || popupEngine == null || popupMic == null || popupGo == null
                || suggestionsRecycler == null) {
            dialog.dismiss();
            return;
        }
        updateSearchEngineIcon(popupEngine);
        popupEngine.setOnClickListener(v ->
                showSearchEnginePickerForPopup(popupEngine, searchInput, dialog));

        SuggestionsAdapter suggestionsAdapter = new SuggestionsAdapter(suggestion -> {
            dialog.dismiss();
            handleUrlInput(suggestion);
        });
        suggestionsRecycler.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));
        suggestionsRecycler.setAdapter(suggestionsAdapter);

        searchInput.setText(urlInput.getText());
        searchInput.selectAll();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int[] suggestionRequest = {0};
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
                boolean browserSuggestions = prefs.getBoolean(
                        SettingsKeys.PREF_BROWSER_SUGGESTIONS_ENABLED, true);
                boolean searchSuggestions = prefs.getBoolean(
                        SettingsKeys.PREF_SEARCH_SUGGESTIONS_ENABLED, true);
                if (!browserSuggestions && !searchSuggestions) {
                    suggestionsRecycler.setVisibility(View.GONE);
                    return;
                }

                class SuggestionRequest {
                    void show(List<Suggestion> suggestions) {
                        if (requestId != suggestionRequest[0]) {
                            return;
                        }
                        if (!isSearchDialogActive(dialog)) {
                            return;
                        }
                        if (suggestions == null || suggestions.isEmpty()) {
                            suggestionsRecycler.setVisibility(View.GONE);
                        } else {
                            suggestionsAdapter.setSuggestions(suggestions);
                            suggestionsRecycler.setVisibility(View.VISIBLE);
                        }
                    }

                    void fetchSearch(List<Suggestion> browserResults) {
                        String searchEngine = oneTimeSearchEngineUrl != null
                                ? oneTimeSearchEngineUrl
                                : UrlUtils.getSearchEngineUrl(MainActivity.this, false);
                        suggestionProvider.fetchSuggestions(query, searchEngine,
                                searchResults -> runOnUiThread(() -> {
                                    if (isSearchDialogActive(dialog)) {
                                        show(mergeSuggestions(browserResults, searchResults));
                                    }
                                }));
                    }
                }

                SuggestionRequest request = new SuggestionRequest();
                if (browserSuggestions) {
                    BrowserSuggestionProvider.fetchSuggestions(MainActivity.this, query,
                            browserResults -> runOnUiThread(() -> {
                                if (!isSearchDialogActive(dialog)) {
                                    return;
                                }
                                request.show(browserResults);
                                if (searchSuggestions) {
                                    request.fetchSearch(browserResults);
                                }
                            }));
                } else if (searchSuggestions) {
                    request.fetchSearch(new ArrayList<>());
                }
            }
        });

        popupMic.setOnClickListener(v -> startVoiceRecognition());
        popupGo.setOnClickListener(v -> {
            dialog.dismiss();
            handleUrlInput(searchInput.getText().toString());
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                dialog.dismiss();
                handleUrlInput(searchInput.getText().toString());
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    attributes.setBlurBehindRadius(32);
                }
                window.setAttributes(attributes);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                window.setWindowAnimations(R.style.SearchPopupAnimation);
            }
            searchInput.requestFocus();
            showKeyboard(searchInput);
        });
        dialog.show();
    }

    private boolean isSearchDialogActive(android.app.Dialog dialog) {
        return dialog != null
                && dialog.isShowing()
                && !isFinishing()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed());
    }

    // Local (bookmark/history) rows first, then engine suggestions fill the remaining slots,
    // deduped by what tapping the row would submit.
    private List<Suggestion> mergeSuggestions(List<Suggestion> local, List<String> searchResults) {
        List<Suggestion> merged = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        if (local != null) {
            for (Suggestion suggestion : local) {
                if (suggestion != null
                        && seen.add(suggestion.getNavigationText().toLowerCase(Locale.US))) {
                    merged.add(suggestion);
                }
            }
        }
        if (searchResults != null) {
            for (String query : searchResults) {
                if (query == null || query.trim().isEmpty()) {
                    continue;
                }
                if (seen.add(query.trim().toLowerCase(Locale.US))) {
                    merged.add(Suggestion.search(query.trim()));
                }
            }
        }
        return merged.size() > 8 ? new ArrayList<>(merged.subList(0, 8)) : merged;
    }

    private void showHomeMoreMenu() {
        View anchor = bottomNav.findViewById(R.id.nav_settings);
        List<MoreMenuPopup.Action> navigationActions = new ArrayList<>();
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_arrow_back, R.string.back,
                false, () -> {}));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_arrow_forward, R.string.forward,
                false, () -> {}));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_reload, R.string.reload,
                false, () -> {}));
        navigationActions.add(new MoreMenuPopup.Action(R.drawable.ic_share, R.string.share,
                false, () -> {}));

        List<MoreMenuPopup.Action> menuActions = new ArrayList<>();
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_tabs, R.string.new_tab,
                true, () -> startActivity(new Intent(this, BrowserActivity.class)
                        .putExtra("new_tab", true))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_incognito, R.string.new_private_tab,
                true, () -> {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra("private_tab", true);
            startActivity(intent);
        }));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_history, R.string.history,
                true, () -> startActivity(new Intent(this, HistoryActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_download, R.string.downloads,
                true, () -> startActivity(new Intent(this, DownloadsActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_bookmarks, R.string.bookmarks,
                true, () -> startActivity(new Intent(this, BookmarksActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_recent, R.string.recent_tabs,
                true, this::launchTabManager));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_extensions_puzzle, R.string.extensions,
                true, () -> startActivity(new Intent(this, ExtensionsActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_settings, R.string.settings,
                true, () -> startActivity(new Intent(this, SettingsActivity.class))));
        menuActions.add(new MoreMenuPopup.Action(R.drawable.ic_help, R.string.help_feedback,
                true, this::showHelpAndFeedback));

        MoreMenuPopup.show(this, anchor, navigationActions, menuActions);
    }

    private void setupSpeechRecognition() {
        speechRecognizer = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            urlInput.setText(spokenText);
                            handleUrlInput(spokenText);
                        }
                    }
                });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.voice_search_prompt));

        try {
            speechRecognizer.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.voice_search_not_available,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showSecurityInfo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.security_info)
                .setMessage(R.string.security_info_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showHelpAndFeedback() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body));
        startActivity(Intent.createChooser(intent, getString(R.string.help_feedback)));
    }

    @Override
    public void onQuickAccessClick(QuickAccessItem item) {
        handleUrlInput(item.getUrl());
        quickAccessRepository.updateQuickAccessItem(item);
    }

    @Override
    public void onQuickAccessLongClick(QuickAccessItem item, View view) {
        PopupMenu menu = new PopupMenu(this, view);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_OPEN_NEW_TAB,
                Menu.NONE, R.string.context_menu_open_new_tab);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_OPEN_NEW_TAB_IN_GROUP,
                Menu.NONE, R.string.context_menu_open_new_tab_in_group);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_OPEN_PRIVATE_TAB,
                Menu.NONE, R.string.context_menu_open_private_tab);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_DOWNLOAD_LINK,
                Menu.NONE, R.string.context_menu_download_link);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_REMOVE,
                Menu.NONE, R.string.remove);
        menu.getMenu().add(Menu.NONE, QUICK_ACCESS_MENU_PIN,
                Menu.NONE, item.isPinned()
                        ? R.string.quick_access_unpin_shortcut
                        : R.string.quick_access_pin_shortcut);
        menu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case QUICK_ACCESS_MENU_OPEN_NEW_TAB:
                    openQuickAccessInBrowser(item, true, false, false);
                    return true;
                case QUICK_ACCESS_MENU_OPEN_NEW_TAB_IN_GROUP:
                    openQuickAccessInBrowser(item, false, true, false);
                    return true;
                case QUICK_ACCESS_MENU_OPEN_PRIVATE_TAB:
                    openQuickAccessInBrowser(item, false, false, true);
                    return true;
                case QUICK_ACCESS_MENU_DOWNLOAD_LINK:
                    downloadQuickAccessLink(item);
                    return true;
                case QUICK_ACCESS_MENU_REMOVE:
                    removeQuickAccessItem(item);
                    return true;
                case QUICK_ACCESS_MENU_PIN:
                    setQuickAccessPinned(item, !item.isPinned());
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void openQuickAccessInBrowser(QuickAccessItem item, boolean newTab,
                                          boolean newTabInGroup, boolean privateTab) {
        String url = getSafeQuickAccessUrl(item);
        if (url == null) {
            return;
        }
        Intent intent = new Intent(this, BrowserActivity.class)
                .putExtra(BrowserActivity.EXTRA_URL, url);
        if (privateTab) {
            intent.putExtra(BrowserActivity.EXTRA_PRIVATE_TAB, true);
        } else if (newTabInGroup) {
            intent.putExtra(BrowserActivity.EXTRA_NEW_TAB_IN_GROUP, true);
        } else if (newTab) {
            intent.putExtra(BrowserActivity.EXTRA_NEW_TAB, true);
        }
        startActivity(intent);
        if (!privateTab) {
            quickAccessRepository.updateQuickAccessItem(item);
        }
    }

    private void downloadQuickAccessLink(QuickAccessItem item) {
        String url = getSafeQuickAccessUrl(item);
        if (url == null) {
            return;
        }
        AnalyticsManager.logDownloadStarted(this, url, null);
        AppDownloadManager.getInstance().startDownload(this, url, null, null);
    }

    private void setQuickAccessPinned(QuickAccessItem item, boolean pinned) {
        quickAccessRepository.setPinned(item, pinned, new QuickAccessRepository.QuickAccessCallback() {
            @Override
            public void onQuickAccessItemsLoaded(List<QuickAccessItem> items) {}

            @Override
            public void onQuickAccessItemAdded(QuickAccessItem item) {
                runOnUiThread(() -> {
                    reloadQuickAccess();
                    Toast.makeText(MainActivity.this,
                            pinned ? R.string.quick_access_pinned : R.string.quick_access_unpinned,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onQuickAccessItemRemoved(QuickAccessItem item) {}
        });
    }

    private String getSafeQuickAccessUrl(QuickAccessItem item) {
        if (item == null || item.getUrl() == null) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return null;
        }
        String url = UrlUtils.getQuickAccessUrl(item.getUrl());
        if (url == null) {
            url = UrlUtils.getUrlOrSearchUrl(this, item.getUrl());
        }
        String lower = url != null ? url.trim().toLowerCase(Locale.US) : "";
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return null;
        }
        return url.trim();
    }

    private void showEditQuickAccessDialog(QuickAccessItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bookmark, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.bookmark_title_input);
        TextInputEditText urlInput = dialogView.findViewById(R.id.bookmark_url_input);
        titleInput.setText(item.getTitle());
        urlInput.setText(item.getUrl());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_quick_access)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = titleInput.getText() != null
                            ? titleInput.getText().toString().trim() : "";
                    String url = urlInput.getText() != null
                            ? urlInput.getText().toString().trim() : "";
                    if (title.isEmpty() || url.isEmpty()) {
                        Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QuickAccessItem updated = new QuickAccessItem(title, UrlUtils.getUrlOrSearchUrl(this, url));
                    updated.setId(item.getId());
                    updated.setFaviconUrl(item.getFaviconUrl());
                    updated.setVisitCount(item.getVisitCount());
                    updated.setLastVisited(System.currentTimeMillis());
                    quickAccessRepository.saveQuickAccessItem(item, updated,
                            new QuickAccessRepository.QuickAccessCallback() {
                                @Override
                                public void onQuickAccessItemsLoaded(List<QuickAccessItem> items) {}

                                @Override
                                public void onQuickAccessItemAdded(QuickAccessItem item) {
                                    runOnUiThread(() -> {
                                        reloadQuickAccess();
                                        Toast.makeText(MainActivity.this,
                                                R.string.quick_access_updated,
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onQuickAccessItemRemoved(QuickAccessItem item) {}
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeQuickAccessItem(QuickAccessItem item) {
        quickAccessRepository.removeQuickAccessItem(item, new QuickAccessRepository.QuickAccessCallback() {
            @Override
            public void onQuickAccessItemsLoaded(List<QuickAccessItem> items) {}

            @Override
            public void onQuickAccessItemAdded(QuickAccessItem item) {}

            @Override
            public void onQuickAccessItemRemoved(QuickAccessItem item) {
                runOnUiThread(() -> {
                    reloadQuickAccess();
                    Toast.makeText(MainActivity.this, R.string.quick_access_removed,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
