package com.webstudio.easybrowser.ui.activity;

import android.app.Dialog;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.webstudio.easybrowser.BuildConfig;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.HistoryRepository;
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

public class SettingsActivity extends AppCompatActivity {
    private static final int[] ADVANCED_ROW_IDS = new int[]{
            R.id.setting_auto_clear_on_exit,
            R.id.layout_auto_clear_items,
            R.id.setting_user_agent,
            R.id.setting_user_styles,
            R.id.setting_doh,
            R.id.setting_remote_debugging
    };

    private SharedPreferences prefs;
    private TextView searchEngineValue;
    private TextView homepageValue;
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
    private SwitchMaterial switchDownloadWifiOnly;
    private Toolbar toolbar;

    private LinearLayout settingSearchEngine;
    private LinearLayout settingHomepage;
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
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initializeViews();
        applyThemedChrome();
        initializeRepositories();
        setupListeners();
        loadSettings();
        hideDuplicateSettingsRows();
        setupVisualHierarchy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemedChrome();
        if (prefs != null) {
            loadSettings();
            applyAdvancedVisibility();
        }
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
        insertSettingsModeCard();
        insertPerformanceBenchmarkRow();
        styleSettingsCards(findViewById(android.R.id.content));
        applyTopLevelRowIcons();
        applyRowMicroInteractions(findViewById(android.R.id.content));
        applyAdvancedVisibility();
    }

    private void insertSettingsModeCard() {
        NestedScrollView scrollView = findNestedScrollView(findViewById(android.R.id.content));
        if (scrollView == null || scrollView.getChildCount() == 0
                || !(scrollView.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);
        if (container.findViewWithTag("settings_mode_card") != null) {
            return;
        }

        MaterialCardView card = createPolishedCard();
        card.setTag("settings_mode_card");

        LinearLayout row = createTopLevelActionRow(
                getString(R.string.settings_mode),
                getString(R.string.settings_mode_summary),
                R.drawable.ic_settings,
                null);
        SwitchMaterial advancedSwitch = new SwitchMaterial(this);
        advancedSwitch.setChecked(areAdvancedSettingsVisible());
        advancedSwitch.setThumbTintList(ThemeEngine.switchThumbTint(this));
        advancedSwitch.setTrackTintList(ThemeEngine.switchTrackTint(this));
        advancedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit()
                    .putBoolean(SettingsKeys.PREF_SETTINGS_ADVANCED_VISIBLE, isChecked)
                    .apply();
            applyAdvancedVisibility();
        });
        row.addView(advancedSwitch);
        row.setOnClickListener(v -> advancedSwitch.toggle());
        attachPressMicroInteraction(row);
        card.addView(row);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(16), dp(12), dp(16), dp(10));
        container.addView(card, 0, params);
    }

    private void insertPerformanceBenchmarkRow() {
        View aboutRow = findViewById(R.id.setting_about);
        if (aboutRow == null || !(aboutRow.getParent() instanceof LinearLayout)) {
            return;
        }
        LinearLayout parent = (LinearLayout) aboutRow.getParent();
        if (parent.findViewWithTag("performance_benchmark_row") != null) {
            return;
        }
        View divider = new View(this);
        divider.setBackgroundResource(resolveAttr(android.R.attr.listDivider));
        LinearLayout row = createTopLevelActionRow(
                getString(R.string.performance_benchmark),
                getString(R.string.performance_benchmark_short_summary),
                R.drawable.ic_speed,
                () -> openSubpage(SettingsSubpageActivity.PAGE_PERFORMANCE));
        row.setTag("performance_benchmark_row");

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
        card.setCardElevation(dp(1));
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.settings_card_background));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ThemeEngine.homePalette(this).accentSoft);
        return card;
    }

    private void styleSettingsCards(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setRadius(dp(8));
            card.setCardElevation(dp(1));
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
        setRowIcon(R.id.setting_homepage, R.drawable.ic_home);
        setRowIcon(R.id.setting_notifications, R.drawable.ic_settings);
        setRowIcon(R.id.setting_external_links, R.drawable.ic_globe);
        setRowIcon(R.id.setting_ad_blocking, R.drawable.ic_security);
        setRowIcon(R.id.setting_cookie_banners, R.drawable.ic_security);
        setRowIcon(R.id.setting_strip_tracking_params, R.drawable.ic_security);
        setRowIcon(R.id.setting_https_only, R.drawable.ic_lock);
        setRowIcon(R.id.setting_do_not_track, R.drawable.ic_security);
        setRowIcon(R.id.setting_save_history, R.drawable.ic_history);
        setRowIcon(R.id.setting_clear_data, R.drawable.ic_clear);
        setRowIcon(R.id.setting_cookie_manager, R.drawable.ic_globe);
        setRowIcon(R.id.setting_site_permissions, R.drawable.ic_settings);
        setRowIcon(R.id.setting_prevent_screenshots, R.drawable.ic_lock);
        setRowIcon(R.id.setting_terms_of_use, R.drawable.ic_file);
        setRowIcon(R.id.setting_privacy_policy, R.drawable.ic_security);
        setRowIcon(R.id.setting_ip_infringement, R.drawable.ic_file);
        setRowIcon(R.id.setting_data_compliance, R.drawable.ic_file);
        setRowIcon(R.id.setting_tabs_and_groups, R.drawable.ic_tabs);
        setRowIcon(R.id.setting_media, R.drawable.ic_video);
        setRowIcon(R.id.setting_appearance, R.drawable.ic_settings);
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
        setRowIcon(R.id.setting_user_agent, R.drawable.ic_globe);
        setRowIcon(R.id.setting_user_styles, R.drawable.ic_text);
        setRowIcon(R.id.setting_doh, R.drawable.ic_security);
        setRowIcon(R.id.setting_remote_debugging, R.drawable.ic_settings);
        setRowIcon(R.id.setting_help_feedback, R.drawable.ic_help);
        setRowIcon(R.id.setting_about, R.drawable.ic_help);
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
        int visibility = visible ? View.VISIBLE : View.GONE;
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
        homepageValue = findViewById(R.id.homepage_value);
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
        switchDownloadWifiOnly = findViewById(R.id.switch_download_wifi_only);

        settingSearchEngine = findViewById(R.id.setting_search_engine);
        settingHomepage = findViewById(R.id.setting_homepage);
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
        settingSearchEngine.setOnClickListener(v -> showSearchEngineDialog());
        settingHomepage.setOnClickListener(v -> showHomepageDialog());
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
        findViewById(R.id.setting_external_links).setOnClickListener(v -> showExternalLinksDialog());
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
        findViewById(R.id.setting_help_feedback).setOnClickListener(v -> showHelpAndFeedback());
        findViewById(R.id.setting_about).setOnClickListener(v -> showAboutDialog());
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

        bindSwitchRow(R.id.setting_auto_clear_on_exit, switchAutoClearOnExit);
        bindSwitchRow(R.id.setting_auto_clear_cookies, switchAutoClearCookies);
        bindSwitchRow(R.id.setting_auto_clear_cache, switchAutoClearCache);
        bindSwitchRow(R.id.setting_auto_clear_history, switchAutoClearHistory);

        switchAutoClearOnExit.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("auto_clear_on_exit", checked).apply();
            layoutAutoClearItems.setVisibility(checked ? View.VISIBLE : View.GONE);
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
        int[] duplicateRows = new int[]{
                R.id.setting_cookie_banners,
                R.id.setting_strip_tracking_params,
                R.id.setting_https_only,
                R.id.setting_do_not_track,
                R.id.setting_home_privacy_stats,
                R.id.setting_home_quick_access,
                R.id.setting_text_size,
                R.id.setting_download_wifi_only,
                R.id.setting_download_bandwidth_limit,
                R.id.setting_javascript,
                R.id.setting_block_popups
        };
        for (int rowId : duplicateRows) {
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
        // Load search engine
        String searchEngine = prefs.getString(SettingsKeys.PREF_SEARCH_ENGINE_URL,
                UrlUtils.DEFAULT_SEARCH_ENGINE);
        if (searchEngine == null || searchEngine.trim().isEmpty()) {
            searchEngine = UrlUtils.DEFAULT_SEARCH_ENGINE;
            prefs.edit().putString(SettingsKeys.PREF_SEARCH_ENGINE_URL, searchEngine).apply();
        }
        String searchEngineName = getSearchEngineName(searchEngine);
        searchEngineValue.setText(searchEngineName);

        // Load homepage
        String homepage = prefs.getString(SettingsKeys.PREF_HOMEPAGE, UrlUtils.DEFAULT_HOMEPAGE);
        homepageValue.setText(homepage);

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

    private void showSearchEngineDialog() {
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

        String currentEngine = prefs.getString(SettingsKeys.PREF_SEARCH_ENGINE_URL, builtinUrls[0]);
        int checkedItem = 0;
        for (int i = 0; i < allUrls.length; i++) {
            if (allUrls[i].equals(currentEngine)) { checkedItem = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.default_search_engine)
                .setSingleChoiceItems(allNames, checkedItem, (dialog, which) -> {
                    prefs.edit().putString(SettingsKeys.PREF_SEARCH_ENGINE_URL,
                            allUrls[which]).apply();
                    searchEngineValue.setText(getSearchEngineName(allUrls[which]));
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

    private void showHomepageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_homepage, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.homepage_input_layout);
        TextInputEditText editText = dialogView.findViewById(R.id.homepage_input);

        String currentHomepage = prefs.getString(SettingsKeys.PREF_HOMEPAGE,
                UrlUtils.DEFAULT_HOMEPAGE);
        editText.setText(currentHomepage);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.homepage)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                    String homepage = editText.getText().toString().trim();
                    if (!homepage.isEmpty()) {
                        homepage = UrlUtils.getUrlOrSearchUrl(this, homepage);
                        prefs.edit().putString(SettingsKeys.PREF_HOMEPAGE, homepage).apply();
                        homepageValue.setText(homepage);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
        new Thread(() -> {
            try {
                // Clear history
                historyRepository.clearHistory(new HistoryRepository.HistoryCallback() {
                    @Override
                    public void onHistoryLoaded(java.util.List<com.webstudio.easybrowser.models.HistoryItem> historyItems) {}

                    @Override
                    public void onHistoryItemAdded(com.webstudio.easybrowser.models.HistoryItem item) {}

                    @Override
                    public void onHistoryCleared() {
                        runOnUiThread(() -> {
                            // Clear cookies and cache
                            clearCookiesAndCache();

                            // Clear preferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("do_not_track");
                            editor.remove("javascript_enabled");
                            editor.remove("save_history");
                            editor.remove("block_popups");
                            editor.remove("open_links_new_tab");
                            editor.remove("text_size_percent");
                            editor.apply();
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast(R.string.error_generic);
                });
            }
        }).start();
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

    private void showExternalLinksDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.open_external_links)
                .setMessage(R.string.open_external_links_message)
                .setPositiveButton(R.string.open_default_apps_settings,
                        (dialog, which) -> openDefaultAppsSettings())
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    private void openDefaultAppsSettings() {
        Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                : getAppDetailsSettingsIntent();
        startActivitySafely(intent);
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
