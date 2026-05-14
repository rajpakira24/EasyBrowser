package com.webstudio.easybrowser.ui.activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.repository.HistoryRepository;
import com.webstudio.easybrowser.utils.UrlUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private TextView searchEngineValue;
    private TextView homepageValue;
    private TextView textSizeValue;
    private TextView accountValue;
    private TextView adBlockingValue;
    private SwitchMaterial switchDoNotTrack;
    private SwitchMaterial switchJavascript;
    private SwitchMaterial switchRemoteDebugging;
    private SwitchMaterial switchSaveHistory;
    private SwitchMaterial switchBlockPopups;
    private SwitchMaterial switchOpenLinksNewTab;
    private SwitchMaterial switchCookieBanners;
    private SwitchMaterial switchStripTrackingParams;
    private SwitchMaterial switchHttpsOnly;
    private SwitchMaterial switchHomePrivacyStats;
    private SwitchMaterial switchHomeQuickAccess;

    private LinearLayout settingSearchEngine;
    private LinearLayout settingHomepage;
    private LinearLayout settingTextSize;
    private LinearLayout settingAccount;
    private LinearLayout settingAdBlocking;
    private LinearLayout settingClearData;
    private LinearLayout settingDownloadsFolder;
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
        initializeRepositories();
        setupListeners();
        loadSettings();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }
    }

    private void initializeViews() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        clearLegacyProfilePreferences();

        // Find views
        searchEngineValue = findViewById(R.id.search_engine_value);
        homepageValue = findViewById(R.id.homepage_value);
        textSizeValue = findViewById(R.id.text_size_value);
        accountValue = findViewById(R.id.account_value);
        adBlockingValue = findViewById(R.id.ad_blocking_value);
        switchDoNotTrack = findViewById(R.id.switch_do_not_track);
        switchJavascript = findViewById(R.id.switch_javascript);
        switchRemoteDebugging = findViewById(R.id.switch_remote_debugging);
        switchSaveHistory = findViewById(R.id.switch_save_history);
        switchBlockPopups = findViewById(R.id.switch_block_popups);
        switchOpenLinksNewTab = findViewById(R.id.switch_open_links_new_tab);
        switchCookieBanners = findViewById(R.id.switch_cookie_banners);
        switchStripTrackingParams = findViewById(R.id.switch_strip_tracking_params);
        switchHttpsOnly = findViewById(R.id.switch_https_only);
        switchHomePrivacyStats = findViewById(R.id.switch_home_privacy_stats);
        switchHomeQuickAccess = findViewById(R.id.switch_home_quick_access);

        settingSearchEngine = findViewById(R.id.setting_search_engine);
        settingHomepage = findViewById(R.id.setting_homepage);
        settingTextSize = findViewById(R.id.setting_text_size);
        settingAccount = findViewById(R.id.setting_account);
        settingAdBlocking = findViewById(R.id.setting_ad_blocking);
        settingClearData = findViewById(R.id.setting_clear_data);
        settingDownloadsFolder = findViewById(R.id.setting_downloads_folder);
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
        settingAccount.setOnClickListener(v -> showAccountDialog());
        settingAdBlocking.setOnClickListener(v -> showAdBlockingDialog());
        settingUserStyles.setOnClickListener(v -> startActivity(new Intent(this, UserStylesActivity.class)));
        settingClearData.setOnClickListener(v -> showClearDataDialog());
        settingDownloadsFolder.setOnClickListener(v -> showDownloadsFolderDialog());
        settingUserAgent.setOnClickListener(v -> showUserAgentDialog());
        settingDoh.setOnClickListener(v -> showDohDialog());
        findViewById(R.id.setting_cookie_manager).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CookieManagerActivity.class)));
        findViewById(R.id.setting_site_permissions).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SitePermissionsActivity.class)));

        bindSwitchRow(R.id.setting_do_not_track, switchDoNotTrack);
        bindSwitchRow(R.id.setting_javascript, switchJavascript);
        bindSwitchRow(R.id.setting_remote_debugging, switchRemoteDebugging);
        bindSwitchRow(R.id.setting_save_history, switchSaveHistory);
        bindSwitchRow(R.id.setting_block_popups, switchBlockPopups);
        bindSwitchRow(R.id.setting_open_links_new_tab, switchOpenLinksNewTab);
        bindSwitchRow(R.id.setting_cookie_banners, switchCookieBanners);
        bindSwitchRow(R.id.setting_strip_tracking_params, switchStripTrackingParams);
        bindSwitchRow(R.id.setting_https_only, switchHttpsOnly);
        bindSwitchRow(R.id.setting_home_privacy_stats, switchHomePrivacyStats);
        bindSwitchRow(R.id.setting_home_quick_access, switchHomeQuickAccess);

        switchDoNotTrack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("do_not_track", isChecked).apply();
            RuntimeManager.getRuntime(this).getSettings().setGlobalPrivacyControl(isChecked);
        });

        switchJavascript.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("javascript_enabled", isChecked).apply();
            RuntimeManager.getRuntime(this).getSettings().setJavaScriptEnabled(isChecked);
        });

        switchRemoteDebugging.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("remote_debugging_enabled", isChecked).apply();
            RuntimeManager.getRuntime(this).getSettings().setRemoteDebuggingEnabled(isChecked);
        });

        switchSaveHistory.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("save_history", isChecked).apply());

        switchBlockPopups.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("block_popups", isChecked).apply());

        switchOpenLinksNewTab.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("open_links_new_tab", isChecked).apply());

        switchCookieBanners.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("block_cookie_banners", isChecked).apply();
            applyContentBlockingSettings();
        });

        switchStripTrackingParams.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("strip_tracking_params", isChecked).apply();
            applyContentBlockingSettings();
        });

        switchHttpsOnly.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("https_only", isChecked).apply());

        switchHomePrivacyStats.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("show_privacy_stats", isChecked).apply());

        switchHomeQuickAccess.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("show_quick_access", isChecked).apply());

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

    private void loadSettings() {
        // Load search engine
        String searchEngine = prefs.getString("search_engine_url", UrlUtils.DEFAULT_SEARCH_ENGINE);
        if (searchEngine == null || searchEngine.trim().isEmpty()) {
            searchEngine = UrlUtils.DEFAULT_SEARCH_ENGINE;
            prefs.edit().putString("search_engine_url", searchEngine).apply();
        }
        String searchEngineName = getSearchEngineName(searchEngine);
        searchEngineValue.setText(searchEngineName);

        // Load homepage
        String homepage = prefs.getString("homepage", UrlUtils.DEFAULT_HOMEPAGE);
        homepageValue.setText(homepage);

        int textSize = prefs.getInt("text_size_percent", 100);
        textSizeValue.setText(getString(R.string.text_size_percent, textSize));
        updateAccountValue();
        updateAdBlockingValue();

        // Load switches
        switchDoNotTrack.setChecked(prefs.getBoolean("do_not_track", false));
        switchJavascript.setChecked(prefs.getBoolean("javascript_enabled", true));
        switchRemoteDebugging.setChecked(prefs.getBoolean("remote_debugging_enabled", false));
        switchSaveHistory.setChecked(prefs.getBoolean("save_history", true));
        switchBlockPopups.setChecked(prefs.getBoolean("block_popups", true));
        switchOpenLinksNewTab.setChecked(prefs.getBoolean("open_links_new_tab", false));
        switchCookieBanners.setChecked(prefs.getBoolean("block_cookie_banners", true));
        switchStripTrackingParams.setChecked(prefs.getBoolean("strip_tracking_params", true));
        switchHttpsOnly.setChecked(prefs.getBoolean("https_only", false));
        switchHomePrivacyStats.setChecked(prefs.getBoolean("show_privacy_stats", true));
        switchHomeQuickAccess.setChecked(prefs.getBoolean("show_quick_access", true));

        // Load downloads folder
        String folder = prefs.getString("downloads_folder_custom", "");
        textDownloadsFolder.setText(folder.isEmpty()
                ? getString(R.string.downloads_folder_default) : folder);

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

        String currentEngine = prefs.getString("search_engine_url", builtinUrls[0]);
        int checkedItem = 0;
        for (int i = 0; i < allUrls.length; i++) {
            if (allUrls[i].equals(currentEngine)) { checkedItem = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.default_search_engine)
                .setSingleChoiceItems(allNames, checkedItem, (dialog, which) -> {
                    prefs.edit().putString("search_engine_url", allUrls[which]).apply();
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
                        String json = prefs.getString("custom_search_engines", "[]");
                        JSONArray arr = new JSONArray(json);
                        JSONObject obj = new JSONObject();
                        obj.put("name", name);
                        obj.put("url", url);
                        arr.put(obj);
                        prefs.edit().putString("custom_search_engines", arr.toString()).apply();
                        Toast.makeText(this, R.string.custom_engine_saved, Toast.LENGTH_SHORT).show();
                    } catch (JSONException ignored) {}
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadCustomSearchEngines(List<String> names, List<String> urls) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("custom_search_engines", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                names.add(obj.optString("name", "Custom"));
                urls.add(obj.optString("url", ""));
            }
        } catch (JSONException ignored) {}
    }

    private void showDownloadsFolderDialog() {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint(R.string.downloads_folder_hint);
        String current = prefs.getString("downloads_folder_custom", "");
        editText.setText(current);
        editText.setSingleLine(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        editText.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.downloads_folder)
                .setView(editText)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String folder = editText.getText().toString().trim();
                    prefs.edit().putString("downloads_folder_custom", folder).apply();
                    textDownloadsFolder.setText(folder.isEmpty()
                            ? getString(R.string.downloads_folder_default) : folder);
                    Toast.makeText(this, R.string.downloads_folder_updated, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.downloads_folder_default, (dialog, which) -> {
                    prefs.edit().putString("downloads_folder_custom", "").apply();
                    textDownloadsFolder.setText(R.string.downloads_folder_default);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showHomepageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_homepage, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.homepage_input_layout);
        TextInputEditText editText = dialogView.findViewById(R.id.homepage_input);

        String currentHomepage = prefs.getString("homepage", UrlUtils.DEFAULT_HOMEPAGE);
        editText.setText(currentHomepage);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.homepage)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                    String homepage = editText.getText().toString().trim();
                    if (!homepage.isEmpty()) {
                        homepage = UrlUtils.getUrlOrSearchUrl(this, homepage);
                        prefs.edit().putString("homepage", homepage).apply();
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

    private void showAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.browser_profile)
                .setMessage(R.string.profile_unavailable_message)
                .setPositiveButton(android.R.string.ok, null)
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

    private void showAdBlockingDialog() {
        String[] labels = getResources().getStringArray(R.array.ad_blocking_names);
        String[] values = getResources().getStringArray(R.array.ad_blocking_values);
        String current = prefs.getString("ad_blocking_level", "balanced");
        int checkedItem = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ad_blocking)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    prefs.edit().putString("ad_blocking_level", values[which]).apply();
                    updateAdBlockingValue();
                    applyContentBlockingSettings();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateAccountValue() {
        accountValue.setText(R.string.not_signed_in);
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

        ContentBlocking.Settings settings = RuntimeManager.getRuntime(this)
                .getSettings()
                .getContentBlocking();
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
            JSONArray arr = new JSONArray(prefs.getString("custom_search_engines", "[]"));
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
        String current = prefs.getString("user_agent_preset", "mobile");
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
                        prefs.edit().putString("user_agent_preset", values[which]).apply();
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
        input.setText(prefs.getString("user_agent_custom_string", ""));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ua_custom)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String ua = input.getText().toString().trim();
                    prefs.edit()
                            .putString("user_agent_preset", "custom")
                            .putString("user_agent_custom_string", ua)
                            .apply();
                    updateUserAgentValue();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateUserAgentValue() {
        String preset = prefs.getString("user_agent_preset", "mobile");
        switch (preset) {
            case "desktop": userAgentValue.setText(R.string.ua_desktop); break;
            case "iphone":  userAgentValue.setText(R.string.ua_iphone); break;
            case "ipad":    userAgentValue.setText(R.string.ua_ipad); break;
            case "custom":
                String ua = prefs.getString("user_agent_custom_string", "");
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
                        RuntimeManager.getRuntime(this);
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
                        RuntimeManager.getRuntime(this);
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
                    RuntimeManager.getRuntime(this);
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
