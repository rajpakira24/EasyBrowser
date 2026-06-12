package com.webstudio.easybrowser.ui.activity;

import android.app.Dialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.webstudio.easybrowser.BuildConfig;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.BuiltInAdBlockerManager;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.managers.TabThumbnailCache;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.HistoryRepository;
import com.webstudio.easybrowser.utils.AppUpdateUtils;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_CLEAR_DATA = "open_clear_data";

    private static final String TAG_SETTINGS_SEARCH_BAR = "settings_search_bar";
    private static final String TAG_SETTINGS_NO_RESULTS = "settings_no_results";
    private static final String TAG_DEFAULT_BROWSER_CARD = "default_browser_card";
    private static final String TAG_ADVANCED_SETTINGS_ROW = "advanced_settings_row";
    private static final String TAG_PERFORMANCE_BENCHMARK_ROW = "performance_benchmark_row";

    private static final int[] ADVANCED_ROW_IDS = new int[]{
            R.id.setting_auto_clear_on_exit,
            R.id.layout_auto_clear_items,
            R.id.setting_user_agent,
            R.id.setting_user_styles,
            R.id.setting_doh,
            R.id.setting_remote_debugging
    };

    private static final int[] DUPLICATE_ROW_IDS = new int[]{
            R.id.setting_cookie_banners,
            R.id.setting_strip_tracking_params,
            R.id.setting_https_only,
            R.id.setting_do_not_track,
            R.id.setting_cookie_manager,
            R.id.setting_site_permissions,
            R.id.setting_prevent_screenshots,
            R.id.setting_home_privacy_stats,
            R.id.setting_home_quick_access,
            R.id.setting_private_search_engine,
            R.id.setting_browser_suggestions,
            R.id.setting_search_suggestions,
            R.id.setting_text_size,
            R.id.setting_download_wifi_only,
            R.id.setting_download_bandwidth_limit,
            R.id.setting_javascript,
            R.id.setting_block_popups
    };

    private SharedPreferences prefs;
    private TextView searchEngineValue;
    private TextView privateSearchEngineValue;
    private TextView textSizeValue;
    private TextView adBlockingValue;
    private TextView downloadBandwidthLimitValue;
    private SwitchMaterial switchDoNotTrack;
    private SwitchMaterial switchJavascript;
    private SwitchMaterial switchRemoteDebugging;
    private SwitchMaterial switchSaveHistory;
    private SwitchMaterial switchBlockPopups;
    private SwitchMaterial switchOpenLinksNewTab;
    private SwitchMaterial switchCookieBanners;
    private SwitchMaterial switchStripTrackingParams;
    private SwitchMaterial switchHttpsOnly;
    private SwitchMaterial switchPreventScreenshots;
    private SwitchMaterial switchHomePrivacyStats;
    private SwitchMaterial switchHomeQuickAccess;
    private SwitchMaterial switchBrowserSuggestions;
    private SwitchMaterial switchSearchSuggestions;
    private SwitchMaterial switchDownloadWifiOnly;
    private Toolbar toolbar;
    private EditText settingsSearchInput;
    private TextView settingsNoResultsView;
    private View defaultBrowserCard;
    private ActivityResultLauncher<Intent> defaultBrowserLauncher;
    private ActivityResultLauncher<IntentSenderRequest> appUpdateLauncher;
    private AppUpdateUtils appUpdateUtils;

    private LinearLayout settingSearchEngine;
    private LinearLayout settingPrivateSearchEngine;
    private LinearLayout settingTextSize;
    private LinearLayout settingAdBlocking;
    private LinearLayout settingClearData;
    private LinearLayout settingDownloadsFolder;
    private LinearLayout settingDownloadBandwidthLimit;
    private TextView textDownloadsFolder;

    // F9 — User Agent
    private LinearLayout settingUserAgent;
    private TextView userAgentValue;

    // F14 — User Styles entry point
    private LinearLayout settingUserStyles;

    // F13 — Auto-clear on exit
    private SwitchMaterial switchAutoClearOnExit;
    private LinearLayout layoutAutoClearItems;
    private SwitchMaterial switchAutoClearCookies;
    private SwitchMaterial switchAutoClearCache;
    private SwitchMaterial switchAutoClearHistory;

    // F15 — DNS-over-HTTPS
    private LinearLayout settingDoh;
    private TextView dohValue;

    private BookmarkRepository bookmarkRepository;
    private HistoryRepository historyRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDefaultBrowserLauncher();
        setupAppUpdateLauncher();
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initializeViews();
        appUpdateUtils = new AppUpdateUtils(this);
        applyThemedChrome();
        initializeRepositories();
        setupListeners();
        loadSettings();
        hideDuplicateSettingsRows();
        setupVisualHierarchy();
        if (getIntent().getBooleanExtra(EXTRA_OPEN_CLEAR_DATA, false)) {
            settingClearData.post(this::showClearDataDialog);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemedChrome();
        if (prefs != null) {
            loadSettings();
            applyAdvancedVisibility();
            updateDefaultBrowserCardVisibility();
            applySettingsSearchFilter();
        }
        if (appUpdateUtils != null) {
            appUpdateUtils.resumePendingUpdate(appUpdateLauncher, createAppUpdateCallback(false));
        }
    }

    @Override
    protected void onDestroy() {
        if (appUpdateUtils != null) {
            appUpdateUtils.release();
        }
        super.onDestroy();
    }

    private void setupDefaultBrowserLauncher() {
        defaultBrowserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateDefaultBrowserCardVisibility());
    }

    private void setupAppUpdateLauncher() {
        appUpdateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        showToast(R.string.update_flow_cancelled);
                    }
                });
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            navigationIcon = DrawableCompat.wrap(navigationIcon.mutate());
            DrawableCompat.setTint(navigationIcon,
                    ContextCompat.getColor(this, R.color.app_bar_foreground));
            toolbar.setNavigationIcon(navigationIcon);
        }
    }

    private void applyThemedChrome() {
        int appBarColor = ThemeEngine.settingsChromeColor(this);
        int foreground = ThemeEngine.foregroundFor(appBarColor);
        if (toolbar != null) {
            toolbar.setBackgroundColor(appBarColor);
            toolbar.setTitleTextColor(foreground);
            Drawable navigationIcon = toolbar.getNavigationIcon();
            if (navigationIcon != null) {
                navigationIcon = DrawableCompat.wrap(navigationIcon.mutate());
                DrawableCompat.setTint(navigationIcon, foreground);
                toolbar.setNavigationIcon(navigationIcon);
            }
        }
        SystemBarUtils.apply(this,
                appBarColor,
                appBarColor,
                ThemeEngine.useDarkSystemBarIcons(appBarColor));
        tintSettingsTree(findViewById(android.R.id.content), ThemeEngine.homePalette(this).accent);
        tintSwitches();
    }

    private void tintSettingsTree(View view, int accent) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            Typeface typeface = text.getTypeface();
            int textSizeSp = Math.round(text.getTextSize()
                    / getResources().getDisplayMetrics().scaledDensity);
            if (typeface != null && typeface.isBold() && textSizeSp <= 14) {
                text.setTextColor(accent);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintSettingsTree(group.getChildAt(i), accent);
            }
        }
    }

    private void tintSwitches() {
        SwitchMaterial[] switches = new SwitchMaterial[]{
                switchDoNotTrack,
                switchJavascript,
                switchRemoteDebugging,
                switchSaveHistory,
                switchBlockPopups,
                switchOpenLinksNewTab,
                switchCookieBanners,
                switchStripTrackingParams,
                switchHttpsOnly,
                switchPreventScreenshots,
                switchHomePrivacyStats,
                switchHomeQuickAccess,
                switchBrowserSuggestions,
                switchSearchSuggestions,
                switchDownloadWifiOnly,
                switchAutoClearOnExit,
                switchAutoClearCookies,
                switchAutoClearCache,
                switchAutoClearHistory
        };
        for (SwitchMaterial switchMaterial : switches) {
            if (switchMaterial != null) {
                switchMaterial.setThumbTintList(ThemeEngine.switchThumbTint(this));
                switchMaterial.setTrackTintList(ThemeEngine.switchTrackTint(this));
            }
        }
    }

    private void setupVisualHierarchy() {
        insertSettingsSearchBar();
        insertDefaultBrowserCard();
        insertPerformanceBenchmarkRow();
        insertAdvancedSettingsRow();
        styleSettingsCards(findViewById(android.R.id.content));
        applySectionHeadingIcons();
        applyTopLevelRowIcons();
        applyRowMicroInteractions(findViewById(android.R.id.content));
        updateDefaultBrowserCardVisibility();
        applyAdvancedVisibility();
        applySettingsSearchFilter();
    }

    private void insertSettingsSearchBar() {
        NestedScrollView scrollView = findNestedScrollView(findViewById(android.R.id.content));
        if (scrollView == null || scrollView.getChildCount() == 0
                || !(scrollView.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);
        if (container.findViewWithTag(TAG_SETTINGS_SEARCH_BAR) != null) {
            return;
        }

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setTag(TAG_SETTINGS_SEARCH_BAR);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setMinimumHeight(dp(56));
        searchRow.setPadding(dp(16), 0, dp(6), 0);
        searchRow.setBackground(createRoundedSurface(
                ContextCompat.getColor(this, R.color.settings_card_background),
                ThemeEngine.homePalette(this).accentSoft));

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.colorOnSurface)));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.setMarginEnd(dp(12));
        searchRow.addView(searchIcon, iconParams);

        settingsSearchInput = new EditText(this);
        settingsSearchInput.setSingleLine(true);
        settingsSearchInput.setHint(R.string.settings_search_hint);
        settingsSearchInput.setTextSize(16);
        settingsSearchInput.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        settingsSearchInput.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        settingsSearchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        settingsSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        settingsSearchInput.setBackground(null);
        settingsSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySettingsSearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchRow.addView(settingsSearchInput, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton clearButton = createIconButton(R.drawable.ic_close,
                getString(R.string.clear));
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(v -> settingsSearchInput.setText(""));
        settingsSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchRow.addView(clearButton);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(16), dp(12), dp(16), dp(8));
        container.addView(searchRow, 0, searchParams);

        settingsNoResultsView = new TextView(this);
        settingsNoResultsView.setTag(TAG_SETTINGS_NO_RESULTS);
        settingsNoResultsView.setText(R.string.settings_search_no_results);
        settingsNoResultsView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        settingsNoResultsView.setTextSize(15);
        settingsNoResultsView.setGravity(Gravity.CENTER);
        settingsNoResultsView.setVisibility(View.GONE);
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emptyParams.setMargins(dp(16), dp(20), dp(16), dp(20));
        container.addView(settingsNoResultsView, 1, emptyParams);
    }

    private void insertDefaultBrowserCard() {
        NestedScrollView scrollView = findNestedScrollView(findViewById(android.R.id.content));
        if (scrollView == null || scrollView.getChildCount() == 0
                || !(scrollView.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);
        if (container.findViewWithTag(TAG_DEFAULT_BROWSER_CARD) != null) {
            return;
        }

        MaterialCardView card = createPolishedCard();
        card.setTag(TAG_DEFAULT_BROWSER_CARD);
        defaultBrowserCard = card;

        LinearLayout row = createTopLevelActionRow(
                getString(R.string.set_default_browser),
                getString(R.string.set_default_browser_summary),
                R.drawable.ic_external_link,
                this::requestDefaultBrowserChange);
        ImageButton closeButton = createIconButton(R.drawable.ic_close,
                getString(R.string.dismiss_default_browser_card));
        closeButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(SettingsKeys.PREF_DEFAULT_BROWSER_CARD_DISMISSED, true)
                    .apply();
            updateDefaultBrowserCardVisibility();
        });
        row.addView(closeButton);
        card.addView(row);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(16), dp(0), dp(16), dp(10));
        container.addView(card, Math.min(container.getChildCount(), 2), params);
    }

    private void updateDefaultBrowserCardVisibility() {
        if (defaultBrowserCard == null) {
            return;
        }
        boolean dismissed = prefs != null && prefs.getBoolean(
                SettingsKeys.PREF_DEFAULT_BROWSER_CARD_DISMISSED, false);
        defaultBrowserCard.setVisibility(!dismissed && !isDefaultBrowser()
                && !hasSettingsSearchQuery() ? View.VISIBLE : View.GONE);
    }

    private boolean isDefaultBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        android.content.pm.ResolveInfo info = getPackageManager().resolveActivity(
                intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return info != null
                && info.activityInfo != null
                && getPackageName().equals(info.activityInfo.packageName);
    }

    private void insertAdvancedSettingsRow() {
        View aboutRow = findViewById(R.id.setting_about);
        if (aboutRow == null || !(aboutRow.getParent() instanceof LinearLayout)) {
            return;
        }
        LinearLayout parent = (LinearLayout) aboutRow.getParent();
        if (parent.findViewWithTag(TAG_ADVANCED_SETTINGS_ROW) != null) {
            return;
        }

        View divider = new View(this);
        divider.setBackgroundResource(resolveAttr(android.R.attr.listDivider));
        LinearLayout row = createTopLevelActionRow(
                getString(R.string.advanced_settings),
                getString(R.string.settings_mode_summary),
                R.drawable.ic_settings,
                null);
        row.setTag(TAG_ADVANCED_SETTINGS_ROW);
        SwitchMaterial advancedSwitch = new SwitchMaterial(this);
        advancedSwitch.setChecked(areAdvancedSettingsVisible());
        advancedSwitch.setThumbTintList(ThemeEngine.switchThumbTint(this));
        advancedSwitch.setTrackTintList(ThemeEngine.switchTrackTint(this));
        advancedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit()
                    .putBoolean(SettingsKeys.PREF_SETTINGS_ADVANCED_VISIBLE, isChecked)
                    .apply();
            applyAdvancedVisibility();
            applySettingsSearchFilter();
        });
        row.addView(advancedSwitch);
        row.setOnClickListener(v -> advancedSwitch.toggle());
        attachPressMicroInteraction(row);

        parent.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void insertPerformanceBenchmarkRow() {
        View aboutRow = findViewById(R.id.setting_about);
        if (aboutRow == null || !(aboutRow.getParent() instanceof LinearLayout)) {
            return;
        }
        LinearLayout parent = (LinearLayout) aboutRow.getParent();
        if (parent.findViewWithTag(TAG_PERFORMANCE_BENCHMARK_ROW) != null) {
            return;
        }
        View divider = new View(this);
        divider.setBackgroundResource(resolveAttr(android.R.attr.listDivider));
        LinearLayout row = createTopLevelActionRow(
                getString(R.string.performance_benchmark),
                getString(R.string.performance_benchmark_short_summary),
                R.drawable.ic_speed,
                () -> openSubpage(SettingsSubpageActivity.PAGE_PERFORMANCE));
        row.setTag(TAG_PERFORMANCE_BENCHMARK_ROW);

        int index = parent.indexOfChild(aboutRow);
        parent.addView(divider, index + 1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        parent.addView(row, index + 2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout createTopLevelActionRow(String title, String summary,
                                                 int iconRes, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundResource(resolveAttr(android.R.attr.selectableItemBackground));

        row.addView(createIconBubble(iconRes));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        texts.addView(titleView);
        TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextSize(14);
        summaryView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(4);
        texts.addView(summaryView, summaryParams);
        row.addView(texts);

        if (action != null) {
            row.setOnClickListener(v -> action.run());
            attachPressMicroInteraction(row);
        }
        return row;
    }

    private ImageView createIconBubble(int iconRes) {
        ImageView icon = new ImageView(this);
        ThemeEngine.Palette palette = ThemeEngine.homePalette(this);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(12));
        background.setColor(palette.accentSoft);
        icon.setBackground(background);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(palette.accent));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMarginEnd(dp(14));
        icon.setLayoutParams(params);
        return icon;
    }

    private MaterialCardView createPolishedCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(0);
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.settings_card_background));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ThemeEngine.homePalette(this).accentSoft);
        return card;
    }

    private GradientDrawable createRoundedSurface(int color, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(8));
        background.setColor(color);
        background.setStroke(dp(1), strokeColor);
        return background;
    }

    private ImageButton createIconButton(int iconRes, String contentDescription) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.text_secondary)));
        button.setBackgroundResource(resolveAttr(android.R.attr.selectableItemBackgroundBorderless));
        button.setContentDescription(contentDescription);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return button;
    }

    private void styleSettingsCards(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setRadius(dp(8));
            card.setCardElevation(0);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(ThemeEngine.homePalette(this).accentSoft);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleSettingsCards(group.getChildAt(i));
            }
        }
    }

    private void applyTopLevelRowIcons() {
        setRowIcon(R.id.setting_search_engine, R.drawable.ic_search);
        setRowIcon(R.id.setting_private_search_engine, R.drawable.ic_incognito);
        setRowIcon(R.id.setting_browser_suggestions, R.drawable.ic_search);
        setRowIcon(R.id.setting_search_suggestions, R.drawable.ic_search);
        setRowIcon(R.id.setting_notifications, R.drawable.ic_notifications);
        setRowIcon(R.id.setting_external_links, R.drawable.ic_external_link);
        setRowIcon(R.id.setting_ad_blocking, R.drawable.ic_security);
        setRowIcon(R.id.setting_cookie_banners, R.drawable.ic_security);
        setRowIcon(R.id.setting_strip_tracking_params, R.drawable.ic_security);
        setRowIcon(R.id.setting_https_only, R.drawable.ic_lock);
        setRowIcon(R.id.setting_do_not_track, R.drawable.ic_security);
        setRowIcon(R.id.setting_save_history, R.drawable.ic_history);
        setRowIcon(R.id.setting_clear_data, R.drawable.ic_clear);
        setRowIcon(R.id.setting_cookie_manager, R.drawable.ic_cookie);
        setRowIcon(R.id.setting_site_permissions, R.drawable.ic_permissions);
        setRowIcon(R.id.setting_prevent_screenshots, R.drawable.ic_lock);
        setRowIcon(R.id.setting_terms_of_use, R.drawable.ic_article);
        setRowIcon(R.id.setting_privacy_policy, R.drawable.ic_security);
        setRowIcon(R.id.setting_ip_infringement, R.drawable.ic_copyright);
        setRowIcon(R.id.setting_data_compliance, R.drawable.ic_compliance);
        setRowIcon(R.id.setting_tabs_and_groups, R.drawable.ic_tabs);
        setRowIcon(R.id.setting_media, R.drawable.ic_video);
        setRowIcon(R.id.setting_appearance, R.drawable.ic_palette);
        setRowIcon(R.id.setting_new_tab_page, R.drawable.ic_home);
        setRowIcon(R.id.setting_accessibility_subpage, R.drawable.ic_text);
        setRowIcon(R.id.setting_languages, R.drawable.ic_translate);
        setRowIcon(R.id.setting_home_privacy_stats, R.drawable.ic_security);
        setRowIcon(R.id.setting_home_quick_access, R.drawable.ic_bookmarks);
        setRowIcon(R.id.setting_text_size, R.drawable.ic_text);
        setRowIcon(R.id.setting_downloads_folder, R.drawable.ic_download);
        setRowIcon(R.id.setting_download_wifi_only, R.drawable.ic_download);
        setRowIcon(R.id.setting_download_bandwidth_limit, R.drawable.ic_speed);
        setRowIcon(R.id.setting_javascript, R.drawable.ic_globe);
        setRowIcon(R.id.setting_block_popups, R.drawable.ic_security);
        setRowIcon(R.id.setting_open_links_new_tab, R.drawable.ic_tabs);
        setRowIcon(R.id.setting_user_agent, R.drawable.ic_desktop);
        setRowIcon(R.id.setting_user_styles, R.drawable.ic_text);
        setRowIcon(R.id.setting_doh, R.drawable.ic_security);
        setRowIcon(R.id.setting_remote_debugging, R.drawable.ic_desktop);
        setRowIcon(R.id.setting_help_feedback, R.drawable.ic_help);
        setRowIcon(R.id.setting_check_updates, R.drawable.ic_reload);
        setRowIcon(R.id.setting_about, R.drawable.ic_help);
    }

    private void applySectionHeadingIcons() {
        setSectionHeadingIcon(R.id.settings_section_general, R.drawable.ic_settings);
        setSectionHeadingIcon(R.id.settings_section_privacy, R.drawable.ic_security);
        setSectionHeadingIcon(R.id.settings_section_legal, R.drawable.ic_article);
        setSectionHeadingIcon(R.id.settings_section_downloads, R.drawable.ic_download);
    }

    private void setSectionHeadingIcon(int headingId, int iconRes) {
        TextView heading = findViewById(headingId);
        if (heading == null) {
            return;
        }
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon == null) {
            return;
        }
        icon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(icon, ThemeEngine.homePalette(this).accent);
        icon.setBounds(0, 0, dp(18), dp(18));
        heading.setCompoundDrawablesRelative(icon, null, null, null);
        heading.setCompoundDrawablePadding(dp(8));
        heading.setGravity(Gravity.CENTER_VERTICAL);
    }

    private void setRowIcon(int rowId, int iconRes) {
        TextView title = findFirstTextView(findViewById(rowId));
        if (title == null) {
            return;
        }
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon == null) {
            return;
        }
        icon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(icon, ThemeEngine.homePalette(this).accent);
        icon.setBounds(0, 0, dp(20), dp(20));
        title.setCompoundDrawablesRelative(icon, null, null, null);
        title.setCompoundDrawablePadding(dp(10));
    }

    private TextView findFirstTextView(View view) {
        if (view instanceof TextView) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView match = findFirstTextView(group.getChildAt(i));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private void applyRowMicroInteractions(View view) {
        if (view == null) {
            return;
        }
        if (view.getId() != View.NO_ID) {
            try {
                String name = getResources().getResourceEntryName(view.getId());
                if (name != null && name.startsWith("setting_")) {
                    attachPressMicroInteraction(view);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRowMicroInteractions(group.getChildAt(i));
            }
        }
    }

    private void attachPressMicroInteraction(View row) {
        row.setOnTouchListener((view, event) -> {
            if (!view.isEnabled()) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate()
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .alpha(0.94f)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .setDuration(90)
                        .start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                        .setDuration(160)
                        .start();
            }
            return false;
        });
    }

    private void applyAdvancedVisibility() {
        boolean visible = areAdvancedSettingsVisible();
        for (int rowId : ADVANCED_ROW_IDS) {
            setRowWithDividerVisibility(rowId, visible);
        }
    }

    private boolean areAdvancedSettingsVisible() {
        return prefs != null && prefs.getBoolean(
                SettingsKeys.PREF_SETTINGS_ADVANCED_VISIBLE, false);
    }

    private void setRowWithDividerVisibility(int rowId, boolean visible) {
        View row = findViewById(rowId);
        if (row == null) {
            return;
        }
        boolean rowVisible = visible;
        if (rowId == R.id.layout_auto_clear_items) {
            rowVisible = visible && prefs != null
                    && prefs.getBoolean("auto_clear_on_exit", false);
        }
        int visibility = rowVisible ? View.VISIBLE : View.GONE;
        row.setVisibility(visibility);
        if (!(row.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup parent = (ViewGroup) row.getParent();
        int index = parent.indexOfChild(row);
        if (index > 0) {
            View previous = parent.getChildAt(index - 1);
            if (previous.getId() == View.NO_ID) {
                previous.setVisibility(visibility);
            }
        }
    }

    private void applySettingsSearchFilter() {
        NestedScrollView scrollView = findNestedScrollView(findViewById(android.R.id.content));
        if (scrollView == null || scrollView.getChildCount() == 0
                || !(scrollView.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);
        if (!hasSettingsSearchQuery()) {
            clearSettingsSearchFilter(container);
            return;
        }

        String query = normalizeSearch(settingsSearchInput.getText());
        int visibleCards = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Object tag = child.getTag();
            if (TAG_SETTINGS_SEARCH_BAR.equals(tag)) {
                child.setVisibility(View.VISIBLE);
            } else if (TAG_SETTINGS_NO_RESULTS.equals(tag)
                    || TAG_DEFAULT_BROWSER_CARD.equals(tag)) {
                child.setVisibility(View.GONE);
            } else if (child instanceof MaterialCardView) {
                boolean visible = filterSettingsCard((MaterialCardView) child, query);
                child.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) {
                    visibleCards++;
                }
            } else if (child instanceof TextView) {
                child.setVisibility(View.GONE);
            }
        }
        cleanHiddenSettingDividers(container);
        updateSectionHeadingVisibility(container);
        if (settingsNoResultsView != null) {
            settingsNoResultsView.setVisibility(visibleCards == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void clearSettingsSearchFilter(LinearLayout container) {
        showSettingsTree(container);
        if (settingsNoResultsView != null) {
            settingsNoResultsView.setVisibility(View.GONE);
        }
        hideDuplicateSettingsRows();
        applyAdvancedVisibility();
        updateDefaultBrowserCardVisibility();
        cleanHiddenSettingDividers(container);
        updateSectionHeadingVisibility(container);
    }

    private void showSettingsTree(View view) {
        if (view == null) {
            return;
        }
        Object tag = view.getTag();
        if (TAG_SETTINGS_SEARCH_BAR.equals(tag)) {
            view.setVisibility(View.VISIBLE);
            return;
        }
        if (!TAG_SETTINGS_NO_RESULTS.equals(tag)) {
            view.setVisibility(View.VISIBLE);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                showSettingsTree(group.getChildAt(i));
            }
        }
    }

    private boolean filterSettingsCard(MaterialCardView card, String query) {
        boolean visible = filterSettingsRows(card, query);
        cleanHiddenSettingDividers(card);
        return visible;
    }

    private boolean filterSettingsRows(View view, String query) {
        if (isSearchableSettingsRow(view)) {
            boolean visible = isRowAvailableForSearch(view) && viewTextMatches(view, query);
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            return visible;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        boolean anyVisible = false;
        for (int i = 0; i < group.getChildCount(); i++) {
            anyVisible |= filterSettingsRows(group.getChildAt(i), query);
        }
        if (view.getId() == R.id.layout_auto_clear_items) {
            view.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
        }
        return anyVisible;
    }

    private boolean isSearchableSettingsRow(View view) {
        Object tag = view.getTag();
        if (TAG_ADVANCED_SETTINGS_ROW.equals(tag)
                || TAG_PERFORMANCE_BENCHMARK_ROW.equals(tag)) {
            return true;
        }
        int id = view.getId();
        if (id == View.NO_ID) {
            return false;
        }
        try {
            String name = getResources().getResourceEntryName(id);
            return name != null && name.startsWith("setting_");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isRowAvailableForSearch(View row) {
        int id = row.getId();
        if (isDuplicateRowId(id)) {
            return false;
        }
        if (isAdvancedRowId(id) && !areAdvancedSettingsVisible()) {
            return false;
        }
        if (id == R.id.setting_auto_clear_cookies
                || id == R.id.setting_auto_clear_cache
                || id == R.id.setting_auto_clear_history) {
            return areAdvancedSettingsVisible()
                    && prefs != null
                    && prefs.getBoolean("auto_clear_on_exit", false);
        }
        return true;
    }

    private boolean viewTextMatches(View view, String query) {
        return collectText(view).contains(query);
    }

    private String collectText(View view) {
        StringBuilder builder = new StringBuilder();
        collectText(view, builder);
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private void collectText(View view, StringBuilder builder) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            CharSequence hint = view instanceof EditText ? ((EditText) view).getHint() : null;
            if (!TextUtils.isEmpty(text)) {
                builder.append(text).append(' ');
            }
            if (!TextUtils.isEmpty(hint)) {
                builder.append(hint).append(' ');
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectText(group.getChildAt(i), builder);
            }
        }
    }

    private String normalizeSearch(CharSequence value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasSettingsSearchQuery() {
        return settingsSearchInput != null
                && !TextUtils.isEmpty(settingsSearchInput.getText())
                && !TextUtils.isEmpty(settingsSearchInput.getText().toString().trim());
    }

    private void updateSectionHeadingVisibility(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof TextView) || child.getTag() != null) {
                continue;
            }
            child.setVisibility(hasVisibleCardBeforeNextHeading(container, i)
                    ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasVisibleCardBeforeNextHeading(LinearLayout container, int headingIndex) {
        for (int i = headingIndex + 1; i < container.getChildCount(); i++) {
            View sibling = container.getChildAt(i);
            if (sibling instanceof TextView && sibling.getTag() == null) {
                return false;
            }
            Object tag = sibling.getTag();
            if (TAG_DEFAULT_BROWSER_CARD.equals(tag) || TAG_SETTINGS_SEARCH_BAR.equals(tag)
                    || TAG_SETTINGS_NO_RESULTS.equals(tag)) {
                continue;
            }
            if (sibling instanceof MaterialCardView && sibling.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateRowId(int rowId) {
        for (int duplicateId : DUPLICATE_ROW_IDS) {
            if (duplicateId == rowId) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdvancedRowId(int rowId) {
        for (int advancedId : ADVANCED_ROW_IDS) {
            if (advancedId == rowId) {
                return true;
            }
        }
        return false;
    }

    private NestedScrollView findNestedScrollView(View view) {
        if (view instanceof NestedScrollView) {
            return (NestedScrollView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            NestedScrollView match = findNestedScrollView(group.getChildAt(i));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private int resolveAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void initializeViews() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        clearLegacyProfilePreferences();

        // Find views
        searchEngineValue = findViewById(R.id.search_engine_value);
        privateSearchEngineValue = findViewById(R.id.private_search_engine_value);
        textSizeValue = findViewById(R.id.text_size_value);
        adBlockingValue = findViewById(R.id.ad_blocking_value);
        downloadBandwidthLimitValue = findViewById(R.id.download_bandwidth_limit_value);
        switchDoNotTrack = findViewById(R.id.switch_do_not_track);
        switchJavascript = findViewById(R.id.switch_javascript);
        switchRemoteDebugging = findViewById(R.id.switch_remote_debugging);
        switchSaveHistory = findViewById(R.id.switch_save_history);
        switchBlockPopups = findViewById(R.id.switch_block_popups);
        switchOpenLinksNewTab = findViewById(R.id.switch_open_links_new_tab);
        switchCookieBanners = findViewById(R.id.switch_cookie_banners);
        switchStripTrackingParams = findViewById(R.id.switch_strip_tracking_params);
        switchHttpsOnly = findViewById(R.id.switch_https_only);
        switchPreventScreenshots = findViewById(R.id.switch_prevent_screenshots);
        switchHomePrivacyStats = findViewById(R.id.switch_home_privacy_stats);
        switchHomeQuickAccess = findViewById(R.id.switch_home_quick_access);
        switchBrowserSuggestions = findViewById(R.id.switch_browser_suggestions);
        switchSearchSuggestions = findViewById(R.id.switch_search_suggestions);
        switchDownloadWifiOnly = findViewById(R.id.switch_download_wifi_only);

        settingSearchEngine = findViewById(R.id.setting_search_engine);
        settingPrivateSearchEngine = findViewById(R.id.setting_private_search_engine);
        settingTextSize = findViewById(R.id.setting_text_size);
        settingAdBlocking = findViewById(R.id.setting_ad_blocking);
        settingClearData = findViewById(R.id.setting_clear_data);
        settingDownloadsFolder = findViewById(R.id.setting_downloads_folder);
        settingDownloadBandwidthLimit = findViewById(R.id.setting_download_bandwidth_limit);
        textDownloadsFolder = findViewById(R.id.text_downloads_folder);

        settingUserAgent = findViewById(R.id.setting_user_agent);
        userAgentValue = findViewById(R.id.user_agent_value);
        settingUserStyles = findViewById(R.id.setting_user_styles);

        switchAutoClearOnExit = findViewById(R.id.switch_auto_clear_on_exit);
        layoutAutoClearItems = findViewById(R.id.layout_auto_clear_items);
        switchAutoClearCookies = findViewById(R.id.switch_auto_clear_cookies);
        switchAutoClearCache = findViewById(R.id.switch_auto_clear_cache);
        switchAutoClearHistory = findViewById(R.id.switch_auto_clear_history);

        settingDoh = findViewById(R.id.setting_doh);
        dohValue = findViewById(R.id.doh_value);
    }

    private void initializeRepositories() {
        bookmarkRepository = new BookmarkRepository(this);
        historyRepository = new HistoryRepository(this);
    }

    private void setupListeners() {
        settingSearchEngine.setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_SEARCH_ENGINES));
        settingPrivateSearchEngine.setOnClickListener(v -> showSearchEngineDialog(true));
        settingTextSize.setOnClickListener(v -> showTextSizeDialog());
        settingAdBlocking.setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_SHIELDS));
        settingUserStyles.setOnClickListener(v -> startActivity(new Intent(this, UserStylesActivity.class)));
        settingClearData.setOnClickListener(v -> showClearDataDialog());
        settingDownloadsFolder.setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_DOWNLOADS));
        settingDownloadBandwidthLimit.setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_DOWNLOADS));
        settingUserAgent.setOnClickListener(v -> showUserAgentDialog());
        settingDoh.setOnClickListener(v -> showDohDialog());
        findViewById(R.id.setting_notifications).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_NOTIFICATIONS));
        findViewById(R.id.setting_external_links).setOnClickListener(v ->
                requestDefaultBrowserChange());
        findViewById(R.id.setting_tabs_and_groups).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_TABS));
        findViewById(R.id.setting_media).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_MEDIA));
        findViewById(R.id.setting_appearance).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_APPEARANCE));
        findViewById(R.id.setting_new_tab_page).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_NEW_TAB));
        findViewById(R.id.setting_accessibility_subpage).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_ACCESSIBILITY));
        findViewById(R.id.setting_languages).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_LANGUAGES));
        findViewById(R.id.setting_terms_of_use).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_TERMS_OF_USE));
        findViewById(R.id.setting_privacy_policy).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_PRIVACY_POLICY));
        findViewById(R.id.setting_ip_infringement).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_IP_INFRINGEMENT));
        findViewById(R.id.setting_data_compliance).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_DATA_COMPLIANCE));
        findViewById(R.id.setting_help_feedback).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_HELP_FEEDBACK));
        findViewById(R.id.setting_check_updates).setOnClickListener(v -> checkForUpdates());
        findViewById(R.id.setting_about).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_ABOUT));
        findViewById(R.id.setting_cookie_manager).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CookieManagerActivity.class)));
        findViewById(R.id.setting_site_permissions).setOnClickListener(v ->
                openSubpage(SettingsSubpageActivity.PAGE_SITE_SETTINGS));

        bindSwitchRow(R.id.setting_do_not_track, switchDoNotTrack);
        bindSwitchRow(R.id.setting_javascript, switchJavascript);
        bindSwitchRow(R.id.setting_remote_debugging, switchRemoteDebugging);
        bindSwitchRow(R.id.setting_save_history, switchSaveHistory);
        bindSwitchRow(R.id.setting_block_popups, switchBlockPopups);
        bindSwitchRow(R.id.setting_open_links_new_tab, switchOpenLinksNewTab);
        bindSwitchRow(R.id.setting_cookie_banners, switchCookieBanners);
        bindSwitchRow(R.id.setting_strip_tracking_params, switchStripTrackingParams);
        bindSwitchRow(R.id.setting_https_only, switchHttpsOnly);
        bindSwitchRow(R.id.setting_prevent_screenshots, switchPreventScreenshots);
        bindSwitchRow(R.id.setting_home_privacy_stats, switchHomePrivacyStats);
        bindSwitchRow(R.id.setting_home_quick_access, switchHomeQuickAccess);
        bindSwitchRow(R.id.setting_browser_suggestions, switchBrowserSuggestions);
        bindSwitchRow(R.id.setting_search_suggestions, switchSearchSuggestions);
        bindSwitchRow(R.id.setting_download_wifi_only, switchDownloadWifiOnly);

        switchDoNotTrack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("do_not_track", isChecked).apply();
            GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
            if (runtime != null) {
                runtime.getSettings().setGlobalPrivacyControl(isChecked);
            }
            AnalyticsManager.logSettingChanged(this, "global_privacy_control", isChecked);
        });

        switchJavascript.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("javascript_enabled", isChecked).apply();
            GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
            if (runtime != null) {
                runtime.getSettings().setJavaScriptEnabled(isChecked);
            }
            AnalyticsManager.logSettingChanged(this, "javascript", isChecked);
        });

        switchRemoteDebugging.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("remote_debugging_enabled", isChecked).apply();
            GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
            if (runtime != null) {
                runtime.getSettings().setRemoteDebuggingEnabled(isChecked);
            }
            AnalyticsManager.logSettingChanged(this, "remote_debugging", isChecked);
        });

        switchSaveHistory.setOnCheckedChangeListener((buttonView, isChecked) ->
                {
                    prefs.edit().putBoolean("save_history", isChecked).apply();
                    AnalyticsManager.logSettingChanged(this, "save_history", isChecked);
                });

        switchBlockPopups.setOnCheckedChangeListener((buttonView, isChecked) ->
                {
                    prefs.edit().putBoolean("block_popups", isChecked).apply();
                    AnalyticsManager.logSettingChanged(this, "block_popups", isChecked);
                });

        switchOpenLinksNewTab.setOnCheckedChangeListener((buttonView, isChecked) ->
                {
                    prefs.edit().putBoolean("open_links_new_tab", isChecked).apply();
                    AnalyticsManager.logSettingChanged(this, "open_links_new_tab", isChecked);
                });

        switchCookieBanners.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("block_cookie_banners", isChecked).apply();
            applyContentBlockingSettings();
            AnalyticsManager.logSettingChanged(this, "block_cookie_banners", isChecked);
        });

        switchStripTrackingParams.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("strip_tracking_params", isChecked).apply();
            applyContentBlockingSettings();
            AnalyticsManager.logSettingChanged(this, "strip_tracking_params", isChecked);
        });

        switchHttpsOnly.setOnCheckedChangeListener((buttonView, isChecked) ->
                {
                    prefs.edit().putBoolean("https_only", isChecked).apply();
                    AnalyticsManager.logSettingChanged(this, "https_only", isChecked);
                });

        switchPreventScreenshots.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(ScreenshotProtection.PREF_PREVENT_SCREENSHOTS, isChecked).apply();
            ScreenshotProtection.apply(this, isChecked);
            AnalyticsManager.logSettingChanged(this, "prevent_screenshots", isChecked);
        });

        switchHomePrivacyStats.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(SettingsKeys.PREF_SHOW_PRIVACY_STATS, isChecked).apply());

        switchHomeQuickAccess.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(SettingsKeys.PREF_SHOW_QUICK_ACCESS, isChecked).apply());

        switchDownloadWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(SettingsKeys.PREF_DOWNLOAD_WIFI_ONLY, isChecked).apply();
            AnalyticsManager.logSettingChanged(this, "download_wifi_only", isChecked);
        });

        switchBrowserSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(SettingsKeys.PREF_BROWSER_SUGGESTIONS_ENABLED,
                    isChecked).apply();
            AnalyticsManager.logSettingChanged(this, "browser_suggestions", isChecked);
        });

        switchSearchSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(SettingsKeys.PREF_SEARCH_SUGGESTIONS_ENABLED,
                    isChecked).apply();
            AnalyticsManager.logSettingChanged(this, "search_suggestions", isChecked);
        });

        bindSwitchRow(R.id.setting_auto_clear_on_exit, switchAutoClearOnExit);
        bindSwitchRow(R.id.setting_auto_clear_cookies, switchAutoClearCookies);
        bindSwitchRow(R.id.setting_auto_clear_cache, switchAutoClearCache);
        bindSwitchRow(R.id.setting_auto_clear_history, switchAutoClearHistory);

        switchAutoClearOnExit.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("auto_clear_on_exit", checked).apply();
            layoutAutoClearItems.setVisibility(
                    checked && areAdvancedSettingsVisible() ? View.VISIBLE : View.GONE);
            applySettingsSearchFilter();
        });
        switchAutoClearCookies.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("auto_clear_cookies", checked).apply());
        switchAutoClearCache.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("auto_clear_cache", checked).apply());
        switchAutoClearHistory.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("auto_clear_history", checked).apply());
    }

    private void bindSwitchRow(int rowId, SwitchMaterial switchControl) {
        View row = findViewById(rowId);
        if (row != null && switchControl != null) {
            row.setOnClickListener(v -> switchControl.toggle());
        }
    }

    private void hideDuplicateSettingsRows() {
        for (int rowId : DUPLICATE_ROW_IDS) {
            View row = findViewById(rowId);
            if (row != null) {
                row.setVisibility(View.GONE);
            }
        }
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup) {
            cleanHiddenSettingDividers((ViewGroup) root);
        }
    }

    private void cleanHiddenSettingDividers(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                cleanHiddenSettingDividers((ViewGroup) child);
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (isDivider(child)) {
                child.setVisibility(hasVisibleContentSibling(group, i, -1)
                        && hasVisibleContentSibling(group, i, 1)
                        ? View.VISIBLE : View.GONE);
            }
        }
    }

    private boolean hasVisibleContentSibling(ViewGroup group, int index, int direction) {
        for (int i = index + direction; i >= 0 && i < group.getChildCount(); i += direction) {
            View sibling = group.getChildAt(i);
            if (isDivider(sibling)) {
                continue;
            }
            return sibling.getVisibility() != View.GONE;
        }
        return false;
    }

    private boolean isDivider(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        int dividerMaxHeight = Math.max(1,
                Math.round(getResources().getDisplayMetrics().density));
        return view.getId() == View.NO_ID
                && params != null
                && params.height > 0
                && params.height <= dividerMaxHeight
                && !(view instanceof ViewGroup);
    }

    private void loadSettings() {
        // Load search engines
        String searchEngine = prefs.getString(SettingsKeys.PREF_SEARCH_ENGINE_URL,
                UrlUtils.DEFAULT_SEARCH_ENGINE);
        if (searchEngine == null || searchEngine.trim().isEmpty()) {
            searchEngine = UrlUtils.DEFAULT_SEARCH_ENGINE;
            prefs.edit().putString(SettingsKeys.PREF_SEARCH_ENGINE_URL, searchEngine).apply();
        }
        String privateSearchEngine = prefs.getString(
                SettingsKeys.PREF_PRIVATE_SEARCH_ENGINE_URL, searchEngine);
        if (privateSearchEngine == null || privateSearchEngine.trim().isEmpty()) {
            privateSearchEngine = searchEngine;
            prefs.edit().putString(SettingsKeys.PREF_PRIVATE_SEARCH_ENGINE_URL,
                    privateSearchEngine).apply();
        }
        searchEngineValue.setText(getSearchEngineName(searchEngine));
        privateSearchEngineValue.setText(getSearchEngineName(privateSearchEngine));

        int textSize = prefs.getInt("text_size_percent", 100);
        textSizeValue.setText(getString(R.string.text_size_percent, textSize));
        updateDownloadBandwidthLimitValue();

        // Load switches
        switchDoNotTrack.setChecked(prefs.getBoolean("do_not_track", true));
        switchJavascript.setChecked(prefs.getBoolean("javascript_enabled", true));
        switchRemoteDebugging.setChecked(prefs.getBoolean("remote_debugging_enabled", false));
        switchSaveHistory.setChecked(prefs.getBoolean("save_history", true));
        switchBlockPopups.setChecked(prefs.getBoolean("block_popups", true));
        switchOpenLinksNewTab.setChecked(prefs.getBoolean("open_links_new_tab", false));
        switchCookieBanners.setChecked(prefs.getBoolean("block_cookie_banners", true));
        switchStripTrackingParams.setChecked(prefs.getBoolean("strip_tracking_params", true));
        switchHttpsOnly.setChecked(prefs.getBoolean("https_only", true));
        switchPreventScreenshots.setChecked(ScreenshotProtection.isEnabled(this));
        switchHomePrivacyStats.setChecked(prefs.getBoolean(
                SettingsKeys.PREF_SHOW_PRIVACY_STATS, true));
        switchHomeQuickAccess.setChecked(prefs.getBoolean(
                SettingsKeys.PREF_SHOW_QUICK_ACCESS, true));
        switchBrowserSuggestions.setChecked(prefs.getBoolean(
                SettingsKeys.PREF_BROWSER_SUGGESTIONS_ENABLED, true));
        switchSearchSuggestions.setChecked(prefs.getBoolean(
                SettingsKeys.PREF_SEARCH_SUGGESTIONS_ENABLED, false));
        switchDownloadWifiOnly.setChecked(prefs.getBoolean(
                SettingsKeys.PREF_DOWNLOAD_WIFI_ONLY, false));

        textDownloadsFolder.setText(R.string.downloads_page_summary);

        updateUserAgentValue();

        boolean autoClear = prefs.getBoolean("auto_clear_on_exit", false);
        switchAutoClearOnExit.setChecked(autoClear);
        layoutAutoClearItems.setVisibility(autoClear ? View.VISIBLE : View.GONE);
        switchAutoClearCookies.setChecked(prefs.getBoolean("auto_clear_cookies", true));
        switchAutoClearCache.setChecked(prefs.getBoolean("auto_clear_cache", true));
        switchAutoClearHistory.setChecked(prefs.getBoolean("auto_clear_history", true));

        updateDohValue();
    }

    private void showSearchEngineDialog(boolean privateMode) {
        String[] builtinNames = getResources().getStringArray(R.array.search_engine_names);
        String[] builtinUrls = getResources().getStringArray(R.array.search_engine_values);
        List<String> customNames = new ArrayList<>();
        List<String> customUrls = new ArrayList<>();
        loadCustomSearchEngines(customNames, customUrls);

        int total = builtinNames.length + customNames.size();
        String[] allNames = new String[total];
        String[] allUrls = new String[total];
        System.arraycopy(builtinNames, 0, allNames, 0, builtinNames.length);
        System.arraycopy(builtinUrls, 0, allUrls, 0, builtinUrls.length);
        for (int i = 0; i < customNames.size(); i++) {
            allNames[builtinNames.length + i] = customNames.get(i) + " ★";
            allUrls[builtinNames.length + i] = customUrls.get(i);
        }

        String prefKey = privateMode
                ? SettingsKeys.PREF_PRIVATE_SEARCH_ENGINE_URL
                : SettingsKeys.PREF_SEARCH_ENGINE_URL;
        String fallbackEngine = UrlUtils.getSearchEngineUrl(this, false);
        String currentEngine = prefs.getString(prefKey, fallbackEngine);
        int checkedItem = 0;
        for (int i = 0; i < allUrls.length; i++) {
            if (allUrls[i].equals(currentEngine)) { checkedItem = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(privateMode
                        ? R.string.private_tab_search_engine
                        : R.string.standard_tab_search_engine)
                .setSingleChoiceItems(allNames, checkedItem, (dialog, which) -> {
                    prefs.edit().putString(prefKey, allUrls[which]).apply();
                    if (privateMode) {
                        privateSearchEngineValue.setText(getSearchEngineName(allUrls[which]));
                    } else {
                        searchEngineValue.setText(getSearchEngineName(allUrls[which]));
                    }
                    dialog.dismiss();
                })
                .setNeutralButton(R.string.add_custom_engine, (d, w) -> showAddCustomSearchEngineDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddCustomSearchEngineDialog() {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint(getString(R.string.custom_engine_name_hint));
        android.widget.EditText urlInput = new android.widget.EditText(this);
        urlInput.setHint("https://example.com/search?q=%s");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);
        layout.addView(nameInput);
        layout.addView(urlInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_custom_engine)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.custom_engine_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!url.contains("%s") || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        Toast.makeText(this, R.string.custom_engine_url_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        String json = prefs.getString(SettingsKeys.PREF_CUSTOM_SEARCH_ENGINES,
                                "[]");
                        JSONArray arr = new JSONArray(json);
                        JSONObject obj = new JSONObject();
                        obj.put("name", name);
                        obj.put("url", url);
                        arr.put(obj);
                        prefs.edit().putString(SettingsKeys.PREF_CUSTOM_SEARCH_ENGINES,
                                arr.toString()).apply();
                        Toast.makeText(this, R.string.custom_engine_saved, Toast.LENGTH_SHORT).show();
                    } catch (JSONException ignored) {}
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadCustomSearchEngines(List<String> names, List<String> urls) {
        try {
            JSONArray arr = new JSONArray(prefs.getString(
                    SettingsKeys.PREF_CUSTOM_SEARCH_ENGINES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                names.add(obj.optString("name", "Custom"));
                urls.add(obj.optString("url", ""));
            }
        } catch (JSONException ignored) {}
    }

    private void showTextSizeDialog() {
        String[] labels = getResources().getStringArray(R.array.text_size_names);
        int[] values = new int[]{85, 100, 115, 130};
        int current = prefs.getInt("text_size_percent", 100);
        int checkedItem = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.text_size)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    prefs.edit().putInt("text_size_percent", values[which]).apply();
                    RuntimeManager.getRuntime(this).getSettings().setFontSizeFactor(values[which] / 100f);
                    textSizeValue.setText(getString(R.string.text_size_percent, values[which]));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearLegacyProfilePreferences() {
        if (prefs.contains("profile_signed_in")
                || prefs.contains("profile_name")
                || prefs.contains("profile_email")) {
            prefs.edit()
                    .remove("profile_signed_in")
                    .remove("profile_name")
                    .remove("profile_email")
                    .apply();
        }
    }

    private void updateAdBlockingValue() {
        String level = prefs.getString("ad_blocking_level", "balanced");
        if ("off".equals(level)) {
            adBlockingValue.setText(R.string.ad_blocking_off);
        } else if ("aggressive".equals(level)) {
            adBlockingValue.setText(R.string.ad_blocking_aggressive);
        } else {
            adBlockingValue.setText(R.string.ad_blocking_balanced);
        }
    }

    private void updateDownloadBandwidthLimitValue() {
        String current = prefs.getString(SettingsKeys.PREF_DOWNLOAD_BANDWIDTH_LIMIT, "0");
        String[] labels = getResources().getStringArray(R.array.bandwidth_limit_names);
        String[] values = getResources().getStringArray(R.array.bandwidth_limit_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                downloadBandwidthLimitValue.setText(labels[i]);
                return;
            }
        }
        downloadBandwidthLimitValue.setText(labels[0]);
    }

    private void applyContentBlockingSettings() {
        String level = prefs.getString("ad_blocking_level", "balanced");
        int antiTracking;
        int etpLevel;
        if ("off".equals(level)) {
            antiTracking = ContentBlocking.AntiTracking.NONE;
            etpLevel = ContentBlocking.EtpLevel.NONE;
        } else if ("aggressive".equals(level)) {
            antiTracking = ContentBlocking.AntiTracking.STRICT;
            etpLevel = ContentBlocking.EtpLevel.STRICT;
        } else {
            antiTracking = ContentBlocking.AntiTracking.DEFAULT | ContentBlocking.AntiTracking.AD;
            etpLevel = ContentBlocking.EtpLevel.DEFAULT;
        }

        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime == null) {
            return;
        }
        ContentBlocking.Settings settings = runtime.getSettings().getContentBlocking();
        settings.setAntiTracking(antiTracking)
                .setEnhancedTrackingProtectionLevel(etpLevel)
                .setSafeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .setCookieBannerMode(prefs.getBoolean("block_cookie_banners", true)
                        ? ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT
                        : ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
                .setCookieBannerModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
                .setQueryParameterStrippingEnabled(prefs.getBoolean("strip_tracking_params", true))
                .setQueryParameterStrippingPrivateBrowsingEnabled(true);
        BuiltInAdBlockerManager.apply(runtime, prefs);
    }

    private void showClearDataDialog() {
        String[] options = new String[]{
                getString(R.string.clear_all_data),
                getString(R.string.clear_history_only),
                getString(R.string.clear_cookies_only),
                getString(R.string.clear_cache_only)
        };

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.clear_data)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Clear all data
                            confirmClearAllData();
                            break;
                        case 1: // Clear history only
                            confirmClearHistory();
                            break;
                        case 2: // Clear cookies only
                            confirmClearCookies();
                            break;
                        case 3: // Clear cache only
                            confirmClearCache();
                            break;
                    }
                });
        builder.show();
    }

    private void confirmClearAllData() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_all_data)
                .setMessage(getString(R.string.dialog_message_delete))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    showProgress(R.string.clearing_data);
                    clearAllData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history_only)
                .setMessage(getString(R.string.dialog_message_delete))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    showProgress(R.string.clearing_data);
                    clearHistory();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearCookies() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_cookies_only)
                .setMessage(getString(R.string.dialog_message_delete))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    showProgress(R.string.clearing_data);
                    clearCookies();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearCache() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_cache_only)
                .setMessage(getString(R.string.dialog_message_delete))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    showProgress(R.string.clearing_data);
                    clearCache();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearAllData() {
        AppDatabase.getDatabaseExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(getApplicationContext());
                database.tabGroupDao().deleteAllTabs();
                database.tabGroupDao().deleteAllGroups();
                database.historyDao().deleteAll();
                database.bookmarkDao().deleteAll();
                database.quickAccessDao().deleteAll();
                database.downloadDao().deleteAll();
                database.readingListDao().deleteAll();

                prefs.edit().clear().apply();
                TabThumbnailCache.clear();
                deleteChildren(getFilesDir());
                deleteChildren(getExternalFilesDir(null));

                runOnUiThread(this::clearCookiesAndCache);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast(R.string.error_generic);
                });
            }
        });
    }

    private void clearHistory() {
        historyRepository.clearHistory(new HistoryRepository.HistoryCallback() {
            @Override
            public void onHistoryLoaded(java.util.List<com.webstudio.easybrowser.models.HistoryItem> historyItems) {}

            @Override
            public void onHistoryItemAdded(com.webstudio.easybrowser.models.HistoryItem item) {}

            @Override
            public void onHistoryCleared() {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast(R.string.data_cleared);
                });
            }
        });
    }

    private void clearCookies() {
        clearGeckoData(StorageController.ClearFlags.COOKIES, false);
    }

    private void clearCache() {
        clearGeckoData(StorageController.ClearFlags.ALL_CACHES, true);
    }

    private void clearCookiesAndCache() {
        clearGeckoData(StorageController.ClearFlags.COOKIES
                | StorageController.ClearFlags.ALL_CACHES
                | StorageController.ClearFlags.DOM_STORAGES
                | StorageController.ClearFlags.AUTH_SESSIONS, true);
    }

    private void clearGeckoData(long flags, boolean clearAppCache) {
        RuntimeManager.getRuntime(this)
                .getStorageController()
                .clearData(flags)
                .accept(value -> {
                    if (clearAppCache) {
                        deleteCache(getCacheDir());
                        deleteCache(getExternalCacheDir());
                    }
                    runOnUiThread(() -> {
                        hideProgress();
                        showToast(R.string.data_cleared);
                    });
                }, exception -> runOnUiThread(() -> {
                    hideProgress();
                    showToast(R.string.error_clear_data);
                }));
    }

    private boolean deleteCache(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteCache(new java.io.File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void deleteChildren(java.io.File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        java.io.File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (java.io.File child : children) {
            deleteCache(child);
        }
    }

    private String getSearchEngineName(String url) {
        if (url == null) return "DuckDuckGo";
        if (url.contains("search.brave.com")) return "Brave Search";
        if (url.contains("google.com")) return "Google";
        if (url.contains("bing.com")) return "Bing";
        if (url.contains("duckduckgo.com")) return "DuckDuckGo";
        if (url.contains("yahoo.com")) return "Yahoo";
        if (url.contains("ecosia.org")) return "Ecosia";
        if (url.contains("startpage.com")) return "Startpage";
        // Check custom engines
        try {
            JSONArray arr = new JSONArray(prefs.getString(
                    SettingsKeys.PREF_CUSTOM_SEARCH_ENGINES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (url.equals(obj.optString("url"))) return obj.optString("name", "Custom");
            }
        } catch (JSONException ignored) {}
        return "Custom";
    }

    private void showUserAgentDialog() {
        String[] labels = {
                getString(R.string.ua_mobile),
                getString(R.string.ua_desktop),
                getString(R.string.ua_iphone),
                getString(R.string.ua_ipad),
                getString(R.string.ua_custom)
        };
        String[] values = {"mobile", "desktop", "iphone", "ipad", "custom"};
        String current = prefs.getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) { checked = i; break; }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_agent)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if ("custom".equals(values[which])) {
                        dialog.dismiss();
                        showCustomUaStringDialog();
                    } else {
                        prefs.edit().putString(SettingsKeys.PREF_USER_AGENT_PRESET,
                                values[which]).apply();
                        updateUserAgentValue();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCustomUaStringDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(getString(R.string.ua_custom_hint));
        input.setText(prefs.getString(SettingsKeys.PREF_USER_AGENT_CUSTOM_STRING, ""));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ua_custom)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String ua = input.getText().toString().trim();
                    prefs.edit()
                            .putString(SettingsKeys.PREF_USER_AGENT_PRESET, "custom")
                            .putString(SettingsKeys.PREF_USER_AGENT_CUSTOM_STRING, ua)
                            .apply();
                    updateUserAgentValue();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateUserAgentValue() {
        String preset = prefs.getString(SettingsKeys.PREF_USER_AGENT_PRESET, "mobile");
        switch (preset) {
            case "desktop": userAgentValue.setText(R.string.ua_desktop); break;
            case "iphone":  userAgentValue.setText(R.string.ua_iphone); break;
            case "ipad":    userAgentValue.setText(R.string.ua_ipad); break;
            case "custom":
                String ua = prefs.getString(SettingsKeys.PREF_USER_AGENT_CUSTOM_STRING, "");
                userAgentValue.setText(ua.isEmpty() ? getString(R.string.ua_custom) : ua);
                break;
            default:        userAgentValue.setText(R.string.ua_mobile); break;
        }
    }

    private void showDohDialog() {
        String[] modeLabels = {
                getString(R.string.doh_mode_off),
                getString(R.string.doh_mode_opportunistic),
                getString(R.string.doh_mode_strict)
        };
        String[] modeValues = {"off", "opportunistic", "strict"};
        String currentMode = prefs.getString("doh_mode", "off");
        int checkedMode = 0;
        for (int i = 0; i < modeValues.length; i++) {
            if (modeValues[i].equals(currentMode)) { checkedMode = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.doh_mode)
                .setSingleChoiceItems(modeLabels, checkedMode, (dialog, which) -> {
                    prefs.edit().putString("doh_mode", modeValues[which]).apply();
                    dialog.dismiss();
                    if (!"off".equals(modeValues[which])) {
                        showDohProviderDialog();
                    } else {
                        applyRuntimePreferencesIfRunning();
                        updateDohValue();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDohProviderDialog() {
        String[] providerLabels = {"Cloudflare", "Google", "NextDNS", getString(R.string.doh_custom)};
        String[] providerValues = {"cloudflare", "google", "nextdns", "custom"};
        String currentProvider = prefs.getString("doh_provider", "cloudflare");
        int checked = 0;
        for (int i = 0; i < providerValues.length; i++) {
            if (providerValues[i].equals(currentProvider)) { checked = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.doh_provider)
                .setSingleChoiceItems(providerLabels, checked, (dialog, which) -> {
                    prefs.edit().putString("doh_provider", providerValues[which]).apply();
                    dialog.dismiss();
                    if ("custom".equals(providerValues[which])) {
                        showDohCustomUriDialog();
                    } else {
                        applyRuntimePreferencesIfRunning();
                        updateDohValue();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDohCustomUriDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(getString(R.string.doh_uri_hint));
        input.setText(prefs.getString("doh_uri", ""));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.doh_custom)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    prefs.edit().putString("doh_uri", input.getText().toString().trim()).apply();
                    applyRuntimePreferencesIfRunning();
                    updateDohValue();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateDohValue() {
        String mode = prefs.getString("doh_mode", "off");
        if ("off".equals(mode)) {
            dohValue.setText(R.string.doh_mode_off);
            return;
        }
        String provider = prefs.getString("doh_provider", "cloudflare");
        String modeLabel = "strict".equals(mode) ? getString(R.string.doh_mode_strict) : getString(R.string.doh_mode_opportunistic);
        String providerLabel;
        switch (provider) {
            case "google":  providerLabel = "Google"; break;
            case "nextdns": providerLabel = "NextDNS"; break;
            case "custom":  providerLabel = getString(R.string.doh_custom); break;
            default:        providerLabel = "Cloudflare"; break;
        }
        dohValue.setText(modeLabel + " · " + providerLabel);
    }

    private void applyRuntimePreferencesIfRunning() {
        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime == null) {
            return;
        }
        runtime.getSettings()
                .setTrustedRecursiveResolverMode(getDohMode())
                .setTrustedRecursiveResolverUri(getDohUri());
    }

    private int getDohMode() {
        String mode = prefs.getString("doh_mode", "off");
        if ("opportunistic".equals(mode)) {
            return org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_FIRST;
        }
        if ("strict".equals(mode)) {
            return org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_ONLY;
        }
        return org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_OFF;
    }

    private String getDohUri() {
        if ("off".equals(prefs.getString("doh_mode", "off"))) {
            return "";
        }
        String provider = prefs.getString("doh_provider", "cloudflare");
        switch (provider) {
            case "google":  return "https://dns.google/dns-query";
            case "nextdns": return "https://dns.nextdns.io/dns-query";
            case "custom":  return prefs.getString("doh_uri", "");
            default:        return "https://cloudflare-dns.com/dns-query";
        }
    }

    private void openSubpage(String page) {
        Intent intent = new Intent(this, SettingsSubpageActivity.class);
        intent.putExtra(SettingsSubpageActivity.EXTRA_PAGE, page);
        startActivity(intent);
    }

    private void checkForUpdates() {
        if (appUpdateUtils == null) {
            showToast(R.string.check_for_updates_unavailable);
            return;
        }
        showToast(R.string.checking_for_updates);
        appUpdateUtils.checkForUpdates(appUpdateLauncher, createAppUpdateCallback(true));
    }

    private AppUpdateUtils.Callback createAppUpdateCallback(boolean userInitiated) {
        return new AppUpdateUtils.Callback() {
            @Override
            public void onNoUpdateAvailable() {
                if (userInitiated) {
                    showToast(R.string.no_updates_available);
                }
            }

            @Override
            public void onUpdateCheckFailed() {
                if (userInitiated) {
                    showToast(R.string.check_for_updates_unavailable);
                }
            }

            @Override
            public void onFlexibleUpdateDownloaded(Runnable completeUpdate) {
                showUpdateReadySnackbar(completeUpdate);
            }
        };
    }

    private void showUpdateReadySnackbar(Runnable completeUpdate) {
        View anchor = findViewById(android.R.id.content);
        Snackbar.make(anchor, R.string.update_downloaded, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.update_restart, v -> completeUpdate.run())
                .show();
    }

    private void requestDefaultBrowserChange() {
        if (isDefaultBrowser()) {
            showToast(R.string.default_browser_already_set);
            updateDefaultBrowserCardVisibility();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null
                    && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)
                    && !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                if (launchDefaultBrowserIntent(roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_BROWSER))) {
                    return;
                }
            }
        }
        openDefaultAppsSettings();
    }

    private void openDefaultAppsSettings() {
        Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                : getAppDetailsSettingsIntent();
        if (!launchDefaultBrowserIntent(intent)) {
            showToast(R.string.error_generic);
        }
    }

    private boolean launchDefaultBrowserIntent(Intent intent) {
        try {
            if (defaultBrowserLauncher != null) {
                defaultBrowserLauncher.launch(intent);
            } else {
                startActivity(intent);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Intent getAppDetailsSettingsIntent() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
    }

    private void showHelpAndFeedback() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body));
        startActivitySafely(Intent.createChooser(intent, getString(R.string.help_feedback)));
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_easy_browser)
                .setMessage(getString(R.string.about_easy_browser_message,
                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                        getString(R.string.release_codename)))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void startActivitySafely(Intent intent) {
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            showToast(R.string.error_generic);
        }
    }

    private Dialog progressDialog;

    private void showProgress(int messageResId) {
        progressDialog = new Dialog(this);
        progressDialog.setContentView(R.layout.dialog_progress);
        progressDialog.setCancelable(false);
        TextView messageView = progressDialog.findViewById(R.id.progress_message);
        messageView.setText(messageResId);
        progressDialog.show();
        android.view.Window window = progressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
