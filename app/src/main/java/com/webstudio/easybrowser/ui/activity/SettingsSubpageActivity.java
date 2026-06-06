package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.webstudio.easybrowser.BuildConfig;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.ScreenshotProtection;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.utils.SettingsKeys;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.ContentBlocking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsSubpageActivity extends AppCompatActivity {
    public static final String EXTRA_PAGE = "page";
    public static final String PAGE_SITE_SETTINGS = "site_settings";
    public static final String PAGE_TABS = "tabs";
    public static final String PAGE_MEDIA = "media";
    public static final String PAGE_APPEARANCE = "appearance";
    public static final String PAGE_NEW_TAB = "new_tab";
    public static final String PAGE_ACCESSIBILITY = "accessibility";
    public static final String PAGE_LANGUAGES = "languages";
    public static final String PAGE_CUSTOMIZE_MENU = "customize_menu";
    public static final String PAGE_THEME = "theme";
    public static final String PAGE_TOOLBAR_SHORTCUT = "toolbar_shortcut";
    public static final String PAGE_ADDRESS_BAR = "address_bar";
    public static final String PAGE_INACTIVE_TABS = "inactive_tabs";
    public static final String PAGE_NOTIFICATIONS = "notifications";
    public static final String PAGE_DOWNLOADS = "downloads";
    public static final String PAGE_SHIELDS = "shields";
    public static final String PAGE_TERMS_OF_USE = "terms_of_use";
    public static final String PAGE_PRIVACY_POLICY = "privacy_policy";
    public static final String PAGE_IP_INFRINGEMENT = "ip_infringement";
    public static final String PAGE_DATA_COMPLIANCE = "data_compliance";

    private SharedPreferences prefs;
    private LinearLayout content;
    private WebView legalWebView;
    private Toolbar toolbar;
    private String page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SystemBarUtils.apply(this,
                ContextCompat.getColor(this, R.color.app_bar_background),
                ContextCompat.getColor(this, R.color.browser_chrome_background));
        page = getIntent().getStringExtra(EXTRA_PAGE);
        if (page == null || page.trim().isEmpty()) {
            page = PAGE_SITE_SETTINGS;
        }
        buildShell();
        buildPage();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor));
        setContentView(root);

        toolbar = new Toolbar(this);
        int appBarForeground = ContextCompat.getColor(this, R.color.app_bar_foreground);
        Drawable backIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back);
        if (backIcon != null) {
            backIcon = DrawableCompat.wrap(backIcon.mutate());
            DrawableCompat.setTint(backIcon, appBarForeground);
            toolbar.setNavigationIcon(backIcon);
        }
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setTitleTextColor(appBarForeground);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.app_bar_background));
        toolbar.setNavigationOnClickListener(v -> finish());
        MenuItem closeItem = toolbar.getMenu().add(R.string.dialog_close);
        Drawable closeIcon = ContextCompat.getDrawable(this, R.drawable.ic_close);
        if (closeIcon != null) {
            closeIcon = DrawableCompat.wrap(closeIcon.mutate());
            DrawableCompat.setTint(closeIcon, appBarForeground);
            closeItem.setIcon(closeIcon);
        }
        closeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.setOnMenuItemClickListener(item -> {
            finish();
            return true;
        });
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getActionBarSize()));

        if (isLegalPage(page)) {
            legalWebView = new WebView(this);
            configureLegalWebView();
            root.addView(legalWebView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            NestedScrollView scrollView = new NestedScrollView(this);
            scrollView.setFillViewport(false);
            content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(8), dp(16), dp(24));
            scrollView.addView(content, new NestedScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(scrollView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
    }

    private void buildPage() {
        if (content != null) {
            content.removeAllViews();
        }
        toolbar.setTitle(getPageTitle(page));
        switch (page) {
            case PAGE_TABS:
                buildTabsPage();
                break;
            case PAGE_MEDIA:
                buildMediaPage();
                break;
            case PAGE_APPEARANCE:
                buildAppearancePage();
                break;
            case PAGE_NEW_TAB:
                buildNewTabPage();
                break;
            case PAGE_ACCESSIBILITY:
                buildAccessibilityPage();
                break;
            case PAGE_LANGUAGES:
                buildLanguagesPage();
                break;
            case PAGE_CUSTOMIZE_MENU:
                buildCustomizeMenuPage();
                break;
            case PAGE_THEME:
                buildThemePage();
                break;
            case PAGE_TOOLBAR_SHORTCUT:
                buildToolbarShortcutPage();
                break;
            case PAGE_ADDRESS_BAR:
                buildAddressBarPage();
                break;
            case PAGE_INACTIVE_TABS:
                buildInactiveTabsPage();
                break;
            case PAGE_NOTIFICATIONS:
                buildNotificationsPage();
                break;
            case PAGE_DOWNLOADS:
                buildDownloadsPage();
                break;
            case PAGE_SHIELDS:
                buildShieldsPage();
                break;
            case PAGE_TERMS_OF_USE:
                buildTermsOfUsePage();
                break;
            case PAGE_PRIVACY_POLICY:
                buildPrivacyPolicyPage();
                break;
            case PAGE_IP_INFRINGEMENT:
                buildIpInfringementPage();
                break;
            case PAGE_DATA_COMPLIANCE:
                buildDataCompliancePage();
                break;
            case PAGE_SITE_SETTINGS:
            default:
                buildSiteSettingsPage();
                break;
        }
    }

    private int getPageTitle(String pageId) {
        switch (pageId) {
            case PAGE_TABS:
                return R.string.tabs_and_groups;
            case PAGE_MEDIA:
                return R.string.media_settings;
            case PAGE_APPEARANCE:
                return R.string.appearance;
            case PAGE_NEW_TAB:
                return R.string.new_tab_page;
            case PAGE_ACCESSIBILITY:
                return R.string.accessibility_settings;
            case PAGE_LANGUAGES:
                return R.string.languages;
            case PAGE_CUSTOMIZE_MENU:
                return R.string.customise_menu;
            case PAGE_THEME:
                return R.string.theme;
            case PAGE_TOOLBAR_SHORTCUT:
                return R.string.toolbar_shortcut;
            case PAGE_ADDRESS_BAR:
                return R.string.address_bar;
            case PAGE_INACTIVE_TABS:
                return R.string.inactive;
            case PAGE_NOTIFICATIONS:
                return R.string.notifications;
            case PAGE_DOWNLOADS:
                return R.string.downloads;
            case PAGE_SHIELDS:
                return R.string.shields;
            case PAGE_TERMS_OF_USE:
                return R.string.terms_of_use;
            case PAGE_PRIVACY_POLICY:
                return R.string.privacy_policy;
            case PAGE_IP_INFRINGEMENT:
                return R.string.ip_infringement;
            case PAGE_DATA_COMPLIANCE:
                return R.string.data_compliance;
            case PAGE_SITE_SETTINGS:
            default:
                return R.string.site_settings;
        }
    }

    private void buildSiteSettingsPage() {
        LinearLayout allSites = addCard();
        addNavigationRow(allSites, R.string.all_sites, R.string.site_permissions_summary,
                () -> startActivity(new Intent(this, SitePermissionsActivity.class)));

        addSection(R.string.permissions);
        LinearLayout permissions = addCard();
        addPermissionChoiceRow(permissions, R.string.location, SettingsKeys.PREF_SITE_LOCATION);
        addPermissionChoiceRow(permissions, R.string.camera, SettingsKeys.PREF_SITE_CAMERA);
        addPermissionChoiceRow(permissions, R.string.microphone, SettingsKeys.PREF_SITE_MICROPHONE);
        addPermissionChoiceRow(permissions, R.string.notifications,
                SettingsKeys.PREF_SITE_NOTIFICATIONS);
        addPermissionChoiceRow(permissions, R.string.local_network,
                SettingsKeys.PREF_SITE_LOCAL_NETWORK);

        addSection(R.string.content);
        LinearLayout contentCard = addCard();
        addChoiceRow(contentCard, R.string.protected_media,
                R.string.default_permission_summary,
                SettingsKeys.PREF_SITE_PROTECTED_MEDIA,
                SettingsKeys.VALUE_ASK,
                permissionLabels(), permissionValues(),
                this::applyRuntimePreferencesIfRunning);
        addChoiceRow(contentCard, R.string.autoplay,
                R.string.default_permission_summary,
                SettingsKeys.PREF_SITE_AUTOPLAY,
                SettingsKeys.VALUE_DENY,
                new int[]{R.string.allowed, R.string.not_allowed},
                new String[]{SettingsKeys.VALUE_ALLOW, SettingsKeys.VALUE_DENY},
                null);
        addSwitchRow(contentCard, R.string.javascript, R.string.javascript_summary,
                "javascript_enabled", true, () -> {
                    GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
                    if (runtime != null) {
                        runtime.getSettings().setJavaScriptEnabled(
                                prefs.getBoolean("javascript_enabled", true));
                    }
                });
        addSwitchRow(contentCard, R.string.block_popups, R.string.block_popups_summary,
                "block_popups", true, null);
        addSwitchRow(contentCard, R.string.desktop_site_default,
                R.string.desktop_site_default_summary,
                SettingsKeys.PREF_DESKTOP_SITE_DEFAULT, false, null);
        addNavigationRow(contentCard, R.string.saved_zoom_for_sites, 0, this::showSavedZoomDialog);
        addNavigationRow(contentCard, R.string.data_stored, 0,
                () -> startActivity(new Intent(this, CookieManagerActivity.class)));
    }

    private void buildTermsOfUsePage() {
        loadLegalDocument(R.string.terms_of_use, R.string.terms_of_use_summary,
                R.string.terms_of_use_intro,
                new LegalSection(R.string.terms_section_service, new int[][]{
                        {R.string.terms_use_of_app_title, R.string.terms_use_of_app_summary},
                        {R.string.terms_no_account_title, R.string.terms_no_account_summary},
                        {R.string.terms_updates_title, R.string.terms_updates_summary}
                }),
                new LegalSection(R.string.terms_section_user_responsibility, new int[][]{
                        {R.string.terms_responsible_use_title,
                                R.string.terms_responsible_use_summary},
                        {R.string.terms_content_responsibility_title,
                                R.string.terms_content_responsibility_summary},
                        {R.string.terms_downloads_files_title,
                                R.string.terms_downloads_files_summary},
                        {R.string.terms_ip_title, R.string.terms_ip_summary}
                }),
                new LegalSection(R.string.terms_section_third_parties, new int[][]{
                        {R.string.terms_third_party_content_title,
                                R.string.terms_third_party_content_summary},
                        {R.string.terms_search_translate_dns_title,
                                R.string.terms_search_translate_dns_summary},
                        {R.string.terms_extensions_styles_title,
                                R.string.terms_extensions_styles_summary},
                        {R.string.terms_external_apps_title,
                                R.string.terms_external_apps_summary}
                }),
                new LegalSection(R.string.terms_section_security, new int[][]{
                        {R.string.terms_privacy_controls_title,
                                R.string.terms_privacy_controls_summary},
                        {R.string.terms_no_warranty_title, R.string.terms_no_warranty_summary},
                        {R.string.terms_contact_title, R.string.terms_contact_summary}
                }));
    }

    private void buildPrivacyPolicyPage() {
        loadLegalDocument(R.string.privacy_policy, R.string.privacy_policy_summary,
                R.string.privacy_policy_intro,
                new LegalSection(R.string.privacy_section_local_storage, new int[][]{
                        {R.string.privacy_browser_records_title,
                                R.string.privacy_browser_records_summary},
                        {R.string.privacy_download_records_title,
                                R.string.privacy_download_records_summary},
                        {R.string.privacy_settings_records_title,
                                R.string.privacy_settings_records_summary},
                        {R.string.privacy_cache_files_title, R.string.privacy_cache_files_summary}
                }),
                new LegalSection(R.string.privacy_section_network, new int[][]{
                        {R.string.privacy_websites_title, R.string.privacy_websites_summary},
                        {R.string.privacy_search_suggestions_title,
                                R.string.privacy_search_suggestions_summary},
                        {R.string.privacy_extensions_marketplace_title,
                                R.string.privacy_extensions_marketplace_summary},
                        {R.string.privacy_dns_translate_title,
                                R.string.privacy_dns_translate_summary},
                        {R.string.privacy_android_sharing_title,
                                R.string.privacy_android_sharing_summary},
                        {R.string.privacy_analytics_title, R.string.privacy_analytics_summary}
                }),
                new LegalSection(R.string.privacy_section_permissions, new int[][]{
                        {R.string.privacy_permissions_title, R.string.privacy_permissions_summary},
                        {R.string.privacy_file_upload_title, R.string.privacy_file_upload_summary},
                        {R.string.privacy_site_permissions_title,
                                R.string.privacy_site_permissions_summary}
                }),
                new LegalSection(R.string.privacy_section_private_mode, new int[][]{
                        {R.string.privacy_private_tabs_title,
                                R.string.privacy_private_tabs_summary},
                        {R.string.privacy_private_limits_title,
                                R.string.privacy_private_limits_summary}
                }),
                new LegalSection(R.string.privacy_section_control, new int[][]{
                        {R.string.privacy_backup_title, R.string.privacy_backup_summary},
                        {R.string.privacy_retention_title, R.string.privacy_retention_summary},
                        {R.string.privacy_delete_data_title, R.string.privacy_delete_data_summary},
                        {R.string.privacy_sharing_title, R.string.privacy_sharing_summary},
                        {R.string.privacy_contact_title, R.string.privacy_contact_summary}
                }));
    }

    private void buildIpInfringementPage() {
        loadLegalDocument(R.string.ip_infringement, R.string.ip_infringement_summary,
                R.string.ip_infringement_intro,
                new LegalSection(R.string.ip_infringement_section_rules, new int[][]{
                        {R.string.ip_respect_rights_title, R.string.ip_respect_rights_summary},
                        {R.string.ip_no_unauthorized_use_title,
                                R.string.ip_no_unauthorized_use_summary},
                        {R.string.ip_no_circumvention_title,
                                R.string.ip_no_circumvention_summary},
                        {R.string.ip_downloads_uploads_title,
                                R.string.ip_downloads_uploads_summary}
                }),
                new LegalSection(R.string.ip_infringement_section_scope, new int[][]{
                        {R.string.ip_third_party_sites_title,
                                R.string.ip_third_party_sites_summary},
                        {R.string.ip_extensions_styles_title,
                                R.string.ip_extensions_styles_summary},
                        {R.string.ip_app_assets_title, R.string.ip_app_assets_summary}
                }),
                new LegalSection(R.string.ip_infringement_section_reports, new int[][]{
                        {R.string.ip_report_notice_title, R.string.ip_report_notice_summary},
                        {R.string.ip_report_destination_title,
                                R.string.ip_report_destination_summary},
                        {R.string.ip_review_action_title, R.string.ip_review_action_summary},
                        {R.string.ip_mistake_title, R.string.ip_mistake_summary}
                }));
    }

    private void buildDataCompliancePage() {
        loadLegalDocument(R.string.data_compliance, R.string.data_compliance_summary,
                R.string.data_compliance_intro,
                new LegalSection(R.string.data_compliance_section_inventory, new int[][]{
                        {R.string.data_compliance_data_inventory_title,
                                R.string.data_compliance_data_inventory_summary},
                        {R.string.data_compliance_sdk_inventory_title,
                                R.string.data_compliance_sdk_inventory_summary},
                        {R.string.data_compliance_network_inventory_title,
                                R.string.data_compliance_network_inventory_summary}
                }),
                new LegalSection(R.string.data_compliance_section_practices, new int[][]{
                        {R.string.data_compliance_local_first_title,
                                R.string.data_compliance_local_first_summary},
                        {R.string.data_compliance_permissions_title,
                                R.string.data_compliance_permissions_summary},
                        {R.string.data_compliance_security_title,
                                R.string.data_compliance_security_summary},
                        {R.string.data_compliance_private_mode_title,
                                R.string.data_compliance_private_mode_summary}
                }),
                new LegalSection(R.string.data_compliance_section_governance, new int[][]{
                        {R.string.data_compliance_disclosures_title,
                                R.string.data_compliance_disclosures_summary},
                        {R.string.data_compliance_play_safety_title,
                                R.string.data_compliance_play_safety_summary},
                        {R.string.data_compliance_deletion_title,
                                R.string.data_compliance_deletion_summary},
                        {R.string.data_compliance_release_review_title,
                                R.string.data_compliance_release_review_summary},
                        {R.string.data_compliance_limits_title,
                                R.string.data_compliance_limits_summary}
                }));
    }

    private boolean isLegalPage(String pageId) {
        return PAGE_TERMS_OF_USE.equals(pageId)
                || PAGE_PRIVACY_POLICY.equals(pageId)
                || PAGE_IP_INFRINGEMENT.equals(pageId)
                || PAGE_DATA_COMPLIANCE.equals(pageId);
    }

    private void configureLegalWebView() {
        if (legalWebView == null) {
            return;
        }
        legalWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor));
        legalWebView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        WebSettings settings = legalWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadsImagesAutomatically(false);
        settings.setBlockNetworkImage(true);
        settings.setBlockNetworkLoads(true);
        settings.setSupportZoom(false);
    }

    private void loadLegalDocument(int titleRes, int subtitleRes, int introRes,
                                   LegalSection... sections) {
        if (legalWebView == null) {
            return;
        }
        legalWebView.loadDataWithBaseURL("https://easybrowser.local/legal/",
                buildLegalHtml(titleRes, subtitleRes, introRes, sections),
                "text/html", "UTF-8", null);
    }

    private String buildLegalHtml(int titleRes, int subtitleRes, int introRes,
                                  LegalSection... sections) {
        String background = cssColor(R.color.backgroundColor);
        String surface = cssColor(R.color.colorSurface);
        String surfaceVariant = cssColor(R.color.surface_variant);
        String primary = cssColor(R.color.colorPrimary);
        String primaryVariant = cssColor(R.color.colorPrimaryVariant);
        String secondary = cssColor(R.color.colorSecondary);
        String onPrimary = cssColor(R.color.colorOnPrimary);
        String text = cssColor(R.color.colorOnBackground);
        String muted = cssColor(R.color.text_secondary);
        String border = cssColor(R.color.border_color);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<title>").append(htmlText(titleRes)).append("</title>")
                .append("<style>")
                .append(":root{color-scheme:light dark;}")
                .append("*{box-sizing:border-box;}")
                .append("html{background:").append(background).append(";color:").append(text)
                .append(";font-family:Roboto,system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;}")
                .append("body{margin:0;line-height:1.58;}")
                .append(".page{max-width:900px;margin:0 auto;padding:24px 18px 42px;}")
                .append(".hero{padding:10px 0 22px;border-bottom:1px solid ").append(border)
                .append(";}")
                .append(".label{display:inline-flex;align-items:center;gap:8px;padding:7px 10px;")
                .append("border-radius:999px;background:").append(surfaceVariant).append(";color:")
                .append(primaryVariant).append(";font-size:12px;font-weight:700;text-transform:uppercase;")
                .append("letter-spacing:.08em;}")
                .append(".dot{width:7px;height:7px;border-radius:999px;background:").append(secondary)
                .append(";display:inline-block;}")
                .append("h1{margin:18px 0 8px;font-size:34px;line-height:1.05;color:").append(text)
                .append(";letter-spacing:0;font-weight:800;}")
                .append(".deck{margin:0;max-width:720px;color:").append(muted)
                .append(";font-size:15px;}")
                .append(".lead{margin:20px 0 26px;padding:16px 17px;border:1px solid ").append(border)
                .append(";border-left:5px solid ").append(primary).append(";border-radius:8px;background:")
                .append(surface).append(";font-size:15px;color:").append(text).append(";}")
                .append(".section{margin-top:28px;}")
                .append(".section-head{display:flex;align-items:center;gap:12px;margin-bottom:10px;}")
                .append(".num{flex:0 0 auto;display:inline-flex;align-items:center;justify-content:center;")
                .append("width:34px;height:34px;border-radius:999px;background:").append(primary)
                .append(";color:").append(onPrimary).append(";font-size:13px;font-weight:800;}")
                .append("h2{margin:0;font-size:22px;line-height:1.2;color:").append(primaryVariant)
                .append(";letter-spacing:0;}")
                .append(".items{background:").append(surface).append(";border:1px solid ").append(border)
                .append(";border-radius:8px;overflow:hidden;}")
                .append(".item{padding:16px 17px;border-top:1px solid ").append(border).append(";}")
                .append(".item:first-child{border-top:0;}")
                .append("h3{margin:0 0 6px;font-size:16px;line-height:1.28;color:").append(text)
                .append(";letter-spacing:0;}")
                .append(".item p{margin:0;color:").append(muted).append(";font-size:14px;}")
                .append(".footer{margin-top:32px;padding-top:16px;border-top:1px solid ").append(border)
                .append(";color:").append(muted).append(";font-size:12px;}")
                .append("@media (min-width:720px){.page{padding:34px 30px 56px;}h1{font-size:44px;}")
                .append(".deck{font-size:16px;}.lead{font-size:16px;padding:18px 20px;}")
                .append(".item{padding:18px 20px;}h2{font-size:25px;}}")
                .append("</style></head><body><main class='page'>")
                .append("<header class='hero'><div class='label'><span class='dot'></span>")
                .append(htmlText(R.string.legal_document_label)).append("</div><h1>")
                .append(htmlText(titleRes)).append("</h1><p class='deck'>")
                .append(htmlText(subtitleRes)).append("</p></header><p class='lead'>")
                .append(htmlText(introRes)).append("</p>");

        for (int i = 0; i < sections.length; i++) {
            appendLegalSection(html, sections[i], i + 1);
        }

        html.append("<p class='footer'>")
                .append(htmlText(getString(R.string.legal_document_footer, BuildConfig.VERSION_NAME)))
                .append("</p></main></body></html>");
        return html.toString();
    }

    private void appendLegalSection(StringBuilder html, LegalSection section, int index) {
        html.append("<section class='section'><div class='section-head'><span class='num'>")
                .append(String.format(Locale.US, "%02d", index))
                .append("</span><h2>").append(htmlText(section.titleRes)).append("</h2></div>")
                .append("<div class='items'>");
        for (int[] item : section.items) {
            if (item.length < 2) {
                continue;
            }
            html.append("<article class='item'><h3>").append(htmlText(item[0]))
                    .append("</h3><p>").append(htmlText(item[1]))
                    .append("</p></article>");
        }
        html.append("</div></section>");
    }

    private String htmlText(int resId) {
        return htmlText(getString(resId));
    }

    private String htmlText(String text) {
        return TextUtils.htmlEncode(text != null ? text : "").replace("\n", "<br>");
    }

    private String cssColor(int colorRes) {
        return String.format(Locale.US, "#%06X",
                0xFFFFFF & ContextCompat.getColor(this, colorRes));
    }

    private static final class LegalSection {
        final int titleRes;
        final int[][] items;

        LegalSection(int titleRes, int[][] items) {
            this.titleRes = titleRes;
            this.items = items;
        }
    }

    private void buildTabsPage() {
        LinearLayout card = addCard();
        addStaticRow(card, getString(R.string.move_to_inactive_section),
                getInactiveDaysLabel(), () -> openSubpage(PAGE_INACTIVE_TABS));
        addSwitchRow(card, R.string.show_undo_closed_tabs,
                R.string.show_undo_closed_tabs_summary,
                SettingsKeys.PREF_SHOW_TAB_UNDO, true, null);
        addSwitchRow(card, R.string.only_open_links_current_group,
                R.string.only_open_links_current_group_summary,
                SettingsKeys.PREF_ONLY_OPEN_LINKS_IN_CURRENT_GROUP, false, null);
    }

    private void buildInactiveTabsPage() {
        addParagraph(R.string.inactive_page_summary);

        LinearLayout timing = addCard();
        addRadioRow(timing, R.string.never, 0,
                SettingsKeys.PREF_INACTIVE_TAB_DAYS, SettingsKeys.VALUE_OFF, null);
        addRadioRow(timing, R.string.after_7_days, 0,
                SettingsKeys.PREF_INACTIVE_TAB_DAYS, "7", null);
        addRadioRow(timing, R.string.after_14_days, 0,
                SettingsKeys.PREF_INACTIVE_TAB_DAYS, "14", null);
        addRadioRow(timing, R.string.after_21_days, 0,
                SettingsKeys.PREF_INACTIVE_TAB_DAYS, "21", null);

        LinearLayout automation = addCard();
        addSwitchRow(automation, R.string.archive_duplicate_tabs,
                R.string.archive_duplicate_tabs_summary,
                SettingsKeys.PREF_ARCHIVE_DUPLICATE_TABS, true, null);
        addSwitchRow(automation, R.string.auto_close_inactive_items,
                R.string.auto_close_inactive_items_summary,
                SettingsKeys.PREF_AUTO_CLOSE_INACTIVE_ITEMS, true, null);
    }

    private String getInactiveDaysLabel() {
        String value = prefs.getString(SettingsKeys.PREF_INACTIVE_TAB_DAYS, "21");
        if (SettingsKeys.VALUE_OFF.equals(value)) {
            return getString(R.string.never);
        }
        if ("7".equals(value)) {
            return getString(R.string.after_7_days);
        }
        if ("14".equals(value)) {
            return getString(R.string.after_14_days);
        }
        if ("30".equals(value)) {
            return getString(R.string.after_30_days);
        }
        return getString(R.string.after_21_days);
    }

    private void buildNotificationsPage() {
        addParagraph(R.string.notifications_page_summary);

        LinearLayout system = addCard();
        addStaticRow(system, getString(R.string.android_notification_settings),
                getString(R.string.android_notification_settings_summary),
                this::openAppNotificationSettings);

        addSection(R.string.downloads);
        LinearLayout downloads = addCard();
        addSwitchRow(downloads, R.string.active_downloads,
                R.string.active_downloads_summary,
                SettingsKeys.PREF_DOWNLOAD_PROGRESS_NOTIFICATIONS, true, null);
        addSwitchRow(downloads, R.string.completed_downloads,
                R.string.completed_downloads_summary,
                SettingsKeys.PREF_DOWNLOAD_COMPLETION_NOTIFICATIONS, true, null);

        addSection(R.string.permissions);
        LinearLayout permissions = addCard();
        addPermissionChoiceRow(permissions, R.string.website_notifications,
                SettingsKeys.PREF_SITE_NOTIFICATIONS);
    }

    private void buildDownloadsPage() {
        LinearLayout location = addCard();
        addStaticRow(location, getString(R.string.download_location),
                getDownloadLocationSummary(), this::showDownloadsFolderDialog);

        addSection(R.string.download_behavior);
        LinearLayout behavior = addCard();
        addSwitchRow(behavior, R.string.download_auto_open,
                R.string.download_auto_open_summary,
                SettingsKeys.PREF_DOWNLOAD_AUTO_OPEN, false, null);
        addSwitchRow(behavior, R.string.download_wifi_only_title,
                R.string.download_wifi_only_summary,
                SettingsKeys.PREF_DOWNLOAD_WIFI_ONLY, false, null);
        addStaticRow(behavior, getString(R.string.download_bandwidth_limit_title),
                getDownloadBandwidthLimitLabel(), this::showDownloadBandwidthLimitDialog);

        addSection(R.string.notifications);
        LinearLayout notifications = addCard();
        addSwitchRow(notifications, R.string.active_downloads,
                R.string.active_downloads_summary,
                SettingsKeys.PREF_DOWNLOAD_PROGRESS_NOTIFICATIONS, true, null);
        addSwitchRow(notifications, R.string.completed_downloads,
                R.string.completed_downloads_summary,
                SettingsKeys.PREF_DOWNLOAD_COMPLETION_NOTIFICATIONS, true, null);
    }

    private void buildShieldsPage() {
        addParagraph(R.string.shields_page_summary);

        LinearLayout blocking = addCard();
        addChoiceRow(blocking, R.string.ad_blocking,
                R.string.ad_blocking_summary,
                "ad_blocking_level",
                "balanced",
                new int[]{
                        R.string.ad_blocking_off,
                        R.string.ad_blocking_balanced,
                        R.string.ad_blocking_aggressive
                },
                new String[]{"off", "balanced", "aggressive"},
                this::applyContentBlockingPreferencesIfRunning);
        addSwitchRow(blocking, R.string.block_cookie_banners,
                R.string.block_cookie_banners_summary,
                "block_cookie_banners", true,
                this::applyContentBlockingPreferencesIfRunning);
        addSwitchRow(blocking, R.string.strip_tracking_params,
                R.string.strip_tracking_params_summary,
                "strip_tracking_params", true,
                this::applyContentBlockingPreferencesIfRunning);

        addSection(R.string.privacy_security_settings);
        LinearLayout privacy = addCard();
        addSwitchRow(privacy, R.string.https_only_mode,
                R.string.https_only_mode_summary,
                "https_only", true, null);
        addSwitchRow(privacy, R.string.do_not_track,
                R.string.do_not_track_summary,
                "do_not_track", true,
                this::applyDoNotTrackPreferenceIfRunning);
        addSwitchRow(privacy, R.string.prevent_screenshots,
                R.string.prevent_screenshots_summary,
                ScreenshotProtection.PREF_PREVENT_SCREENSHOTS,
                ScreenshotProtection.DEFAULT_PREVENT_SCREENSHOTS,
                () -> ScreenshotProtection.apply(this));
        addNavigationRow(privacy, R.string.site_settings,
                R.string.site_permissions_summary,
                () -> openSubpage(PAGE_SITE_SETTINGS));
    }

    private void buildMediaPage() {
        addSection(R.string.general_settings);
        LinearLayout general = addCard();
        addSwitchRow(general, R.string.widevine_drm, R.string.widevine_drm_summary,
                SettingsKeys.PREF_PROTECTED_MEDIA_ENABLED, true, null);
        addSwitchRow(general, R.string.background_play, R.string.background_play_summary,
                SettingsKeys.PREF_BACKGROUND_PLAY_ENABLED, false, null);
    }

    private void buildAppearancePage() {
        LinearLayout main = addCard();
        addNavigationRow(main, R.string.theme, 0, () -> openSubpage(PAGE_THEME));
        addNavigationRow(main, R.string.customise_menu, 0,
                () -> openSubpage(PAGE_CUSTOMIZE_MENU));
        addNavigationRow(main, R.string.toolbar_shortcut, 0,
                () -> openSubpage(PAGE_TOOLBAR_SHORTCUT));
        addNavigationRow(main, R.string.address_bar, 0,
                () -> openSubpage(PAGE_ADDRESS_BAR));

        LinearLayout controls = addCard();
        addSwitchRow(controls, R.string.enable_bottom_navigation_toolbar,
                R.string.enable_bottom_navigation_summary,
                SettingsKeys.PREF_BOTTOM_NAVIGATION_ENABLED, true, null);
        addSwitchRow(controls, R.string.show_undo_closed_tabs,
                R.string.show_undo_closed_tabs_summary,
                SettingsKeys.PREF_SHOW_TAB_UNDO, true, null);
    }

    private void buildNewTabPage() {
        LinearLayout card = addCard();
        addSwitchRow(card, R.string.show_privacy_stats,
                R.string.show_privacy_stats_summary,
                "show_privacy_stats", true, null);
        addSwitchRow(card, R.string.show_quick_access,
                R.string.show_quick_access_summary,
                "show_quick_access", true, null);
    }

    private void buildAccessibilityPage() {
        addZoomSlider();
        LinearLayout card = addCard();
        addNavigationRow(card, R.string.saved_zoom_for_sites, 0, this::showSavedZoomDialog);
        addSwitchRow(card, R.string.force_enable_zoom,
                R.string.force_enable_zoom_summary,
                SettingsKeys.PREF_FORCE_ENABLE_ZOOM, false, this::applyRuntimePreferencesIfRunning);
        addSwitchRow(card, R.string.simplified_view_for_web_pages,
                R.string.simplified_view_summary,
                SettingsKeys.PREF_READER_MODE_MENU_ENABLED, true, null);
        addNavigationRow(card, R.string.captions, R.string.captions_summary,
                this::openCaptionSettings);
    }

    private void buildLanguagesPage() {
        addSection(R.string.browser_language);
        LinearLayout browserLanguage = addCard();
        addStaticRow(browserLanguage, R.string.current_device_language,
                Locale.getDefault().getDisplayName());
        addSwitchRow(browserLanguage, R.string.use_browser_translate, 0,
                SettingsKeys.PREF_TRANSLATIONS_OFFER_POPUP, true,
                this::applyRuntimePreferencesIfRunning);

        addSection(R.string.preferred_languages);
        addParagraph(R.string.preferred_languages_summary);
        LinearLayout languages = addCard();
        List<String> tags = getPreferredLanguageTags();
        for (int i = 0; i < tags.size(); i++) {
            final int index = i;
            addStaticRow(languages, languageDisplayName(tags.get(i)), null,
                    () -> showLanguageActions(index));
        }
        addNavigationRow(languages, R.string.add_language, 0, this::showAddLanguageDialog);
    }

    private void buildCustomizeMenuPage() {
        addSection(R.string.customize_menu_main_menu);
        LinearLayout main = addCard();
        addMenuSwitch(main, R.string.new_tab, SettingsKeys.PREF_MENU_NEW_TAB);
        addMenuSwitch(main, R.string.new_private_tab, SettingsKeys.PREF_MENU_NEW_PRIVATE_TAB);
        addMenuSwitch(main, R.string.history, SettingsKeys.PREF_MENU_HISTORY);
        addMenuSwitch(main, R.string.downloads, SettingsKeys.PREF_MENU_DOWNLOADS);
        addMenuSwitch(main, R.string.bookmarks, SettingsKeys.PREF_MENU_BOOKMARKS);
        addMenuSwitch(main, R.string.reading_list, SettingsKeys.PREF_MENU_READING_LIST);
        addMenuSwitch(main, R.string.extensions, SettingsKeys.PREF_MENU_EXTENSIONS);
        addMenuSwitch(main, R.string.exit, SettingsKeys.PREF_MENU_EXIT);

        addSection(R.string.customize_menu_page_actions);
        LinearLayout pageActions = addCard();
        addMenuSwitch(pageActions, R.string.find_in_page, SettingsKeys.PREF_MENU_FIND_IN_PAGE);
        addMenuSwitch(pageActions, R.string.desktop_site, SettingsKeys.PREF_MENU_DESKTOP_SITE);
        addMenuSwitch(pageActions, R.string.reader_mode, SettingsKeys.PREF_MENU_READER_MODE);
        addMenuSwitch(pageActions, R.string.add_to_quick_access,
                SettingsKeys.PREF_MENU_ADD_TO_QUICK_ACCESS);
        addMenuSwitch(pageActions, R.string.zoom_for_site, SettingsKeys.PREF_MENU_ZOOM);
        addMenuSwitch(pageActions, R.string.save_page, SettingsKeys.PREF_MENU_SAVE_PAGE);
        addMenuSwitch(pageActions, R.string.add_to_home_screen,
                SettingsKeys.PREF_MENU_ADD_TO_HOME_SCREEN);
        addMenuSwitch(pageActions, R.string.save_to_reading_list,
                SettingsKeys.PREF_MENU_SAVE_TO_READING_LIST);
        addMenuSwitch(pageActions, R.string.translate_page, SettingsKeys.PREF_MENU_TRANSLATE);
    }

    private void buildThemePage() {
        LinearLayout card = addCard();
        addRadioRow(card, R.string.theme_system_default, R.string.theme_system_summary,
                SettingsKeys.PREF_THEME_MODE, "system", this::applyThemeMode);
        addRadioRow(card, R.string.theme_light, 0,
                SettingsKeys.PREF_THEME_MODE, "light", this::applyThemeMode);
        addRadioRow(card, R.string.theme_dark, 0,
                SettingsKeys.PREF_THEME_MODE, "dark", this::applyThemeMode);
    }

    private void buildToolbarShortcutPage() {
        addParagraph(R.string.toolbar_shortcut_summary);
        LinearLayout card = addCard();
        addRadioRow(card, R.string.toolbar_shortcut_off, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, SettingsKeys.VALUE_OFF, null);
        addRadioRow(card, R.string.toolbar_shortcut_new_tab, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "new_tab", null);
        addRadioRow(card, R.string.toolbar_shortcut_bookmarks, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "bookmarks", null);
        addRadioRow(card, R.string.toolbar_shortcut_history, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "history", null);
        addRadioRow(card, R.string.toolbar_shortcut_downloads, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "downloads", null);
        addRadioRow(card, R.string.toolbar_shortcut_share, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "share", null);
        addRadioRow(card, R.string.toolbar_shortcut_translate, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "translate", null);
        addRadioRow(card, R.string.toolbar_shortcut_find_in_page, 0,
                SettingsKeys.PREF_TOOLBAR_SHORTCUT, "find", null);
    }

    private void buildAddressBarPage() {
        addParagraph(R.string.address_bar_summary);
        LinearLayout card = addCard();
        addRadioRow(card, R.string.address_bar_top, 0,
                SettingsKeys.PREF_ADDRESS_BAR_POSITION, "top", null);
        addRadioRow(card, R.string.address_bar_bottom, 0,
                SettingsKeys.PREF_ADDRESS_BAR_POSITION, "bottom", null);
    }

    private void addPermissionChoiceRow(LinearLayout card, int titleRes, String prefKey) {
        addChoiceRow(card, titleRes, R.string.default_permission_summary,
                prefKey, SettingsKeys.VALUE_ASK,
                permissionLabels(), permissionValues(),
                SettingsKeys.PREF_SITE_LOCAL_NETWORK.equals(prefKey)
                        ? this::applyRuntimePreferencesIfRunning : null);
    }

    private int[] permissionLabels() {
        return new int[]{R.string.ask_first, R.string.allowed, R.string.not_allowed};
    }

    private String[] permissionValues() {
        return new String[]{
                SettingsKeys.VALUE_ASK,
                SettingsKeys.VALUE_ALLOW,
                SettingsKeys.VALUE_DENY
        };
    }

    private void addZoomSlider() {
        MaterialCardView card = createCard();
        LinearLayout box = createCardContent();
        card.addView(box);

        TextView title = createTitle(R.string.default_zoom);
        box.addView(title);

        TextView summary = createSummary(R.string.default_zoom_summary);
        addTopMargin(summary, dp(4));
        box.addView(summary);

        TextView value = createSummary(getString(R.string.text_size_percent,
                prefs.getInt("text_size_percent", 100)));
        value.setGravity(Gravity.CENTER);
        addTopMargin(value, dp(20));
        box.addView(value);

        Slider slider = new Slider(this);
        slider.setValueFrom(50f);
        slider.setValueTo(200f);
        slider.setStepSize(25f);
        slider.setValue(clampZoom(prefs.getInt("text_size_percent", 100)));
        slider.addOnChangeListener((s, selectedValue, fromUser) -> {
            int percent = Math.round(selectedValue);
            value.setText(getString(R.string.text_size_percent, percent));
            if (fromUser) {
                prefs.edit().putInt("text_size_percent", percent).apply();
                GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
                if (runtime != null) {
                    runtime.getSettings().setFontSizeFactor(percent / 100f);
                }
            }
        });
        box.addView(slider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addCardToContent(card);
    }

    private float clampZoom(int percent) {
        if (percent < 50) {
            return 50f;
        }
        if (percent > 200) {
            return 200f;
        }
        return percent;
    }

    private void addMenuSwitch(LinearLayout card, int titleRes, String key) {
        addSwitchRow(card, titleRes, 0, key, true, null);
    }

    private LinearLayout addCard() {
        MaterialCardView card = createCard();
        LinearLayout rows = createCardContent();
        rows.setPadding(0, 0, 0, 0);
        card.addView(rows);
        addCardToContent(card);
        return rows;
    }

    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(0);
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(ContextCompat.getColor(this,
                R.color.settings_card_background));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border_color));
        return card;
    }

    private LinearLayout createCardContent() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(16), dp(16), dp(16));
        return rows;
    }

    private void addCardToContent(MaterialCardView card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(16));
        content.addView(card, params);
    }

    private void addNavigationRow(LinearLayout card, int titleRes, int summaryRes, Runnable action) {
        addStaticRow(card, getString(titleRes),
                summaryRes != 0 ? getString(summaryRes) : null, action);
    }

    private void addInfoRow(LinearLayout card, int titleRes, int summaryRes) {
        addStaticRow(card, titleRes, getString(summaryRes));
    }

    private void addStaticRow(LinearLayout card, int titleRes, String summary) {
        addStaticRow(card, getString(titleRes), summary, null);
    }

    private void addStaticRow(LinearLayout card, String title, String summary, Runnable action) {
        LinearLayout row = createHorizontalRow();
        LinearLayout texts = createTextColumn();
        TextView titleView = createTitle(title);
        texts.addView(titleView);
        if (summary != null && !summary.trim().isEmpty()) {
            TextView summaryView = createSummary(summary);
            addTopMargin(summaryView, dp(4));
            texts.addView(summaryView);
        }
        row.addView(texts);
        if (action != null) {
            TextView chevron = createValue(">");
            row.addView(chevron);
            row.setOnClickListener(v -> action.run());
        }
        addRow(card, row);
    }

    private void addSwitchRow(LinearLayout card, int titleRes, int summaryRes,
                              String key, boolean defaultValue, Runnable onChanged) {
        LinearLayout row = createHorizontalRow();
        LinearLayout texts = createTextColumn();
        texts.addView(createTitle(titleRes));
        if (summaryRes != 0) {
            TextView summary = createSummary(summaryRes);
            addTopMargin(summary, dp(4));
            texts.addView(summary);
        }
        row.addView(texts);

        SwitchMaterial toggle = new SwitchMaterial(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).apply();
            if (onChanged != null) {
                onChanged.run();
            }
        });
        row.addView(toggle);
        row.setOnClickListener(v -> toggle.toggle());
        addRow(card, row);
    }

    private void addChoiceRow(LinearLayout card, int titleRes, int summaryRes,
                              String key, String defaultValue, int[] labelResIds,
                              String[] values, Runnable onChanged) {
        LinearLayout row = createHorizontalRow();
        LinearLayout texts = createTextColumn();
        texts.addView(createTitle(titleRes));
        TextView valueView = createSummary(choiceLabel(
                prefs.getString(key, defaultValue), labelResIds, values));
        addTopMargin(valueView, dp(4));
        texts.addView(valueView);
        if (summaryRes != 0) {
            TextView summary = createSummary(summaryRes);
            addTopMargin(summary, dp(4));
            texts.addView(summary);
        }
        row.addView(texts);
        row.addView(createValue(">"));
        row.setOnClickListener(v -> showChoiceDialog(titleRes, key, defaultValue,
                labelResIds, values, valueView, onChanged));
        addRow(card, row);
    }

    private void showChoiceDialog(int titleRes, String key, String defaultValue,
                                  int[] labelResIds, String[] values,
                                  TextView valueView, Runnable onChanged) {
        String[] labels = new String[labelResIds.length];
        for (int i = 0; i < labelResIds.length; i++) {
            labels[i] = getString(labelResIds[i]);
        }
        String current = prefs.getString(key, defaultValue);
        int checked = indexOf(values, current);
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    prefs.edit().putString(key, values[which]).apply();
                    valueView.setText(labels[which]);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDownloadsFolderDialog() {
        EditText editText = new EditText(this);
        editText.setHint(R.string.downloads_folder_hint);
        editText.setSingleLine(true);
        editText.setText(prefs.getString(SettingsKeys.PREF_DOWNLOADS_FOLDER_CUSTOM, ""));
        int padding = dp(16);
        editText.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.downloads_folder)
                .setView(editText)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String folder = editText.getText().toString().trim();
                    prefs.edit()
                            .putString(SettingsKeys.PREF_DOWNLOADS_FOLDER_CUSTOM, folder)
                            .apply();
                    Toast.makeText(this, R.string.downloads_folder_updated,
                            Toast.LENGTH_SHORT).show();
                    buildPage();
                })
                .setNeutralButton(R.string.downloads_folder_default, (dialog, which) -> {
                    prefs.edit()
                            .putString(SettingsKeys.PREF_DOWNLOADS_FOLDER_CUSTOM, "")
                            .apply();
                    buildPage();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getDownloadLocationSummary() {
        String customFolder = prefs.getString(SettingsKeys.PREF_DOWNLOADS_FOLDER_CUSTOM, "");
        String folder = TextUtils.isEmpty(customFolder) ? "Easy Browser" : customFolder;
        return Environment.DIRECTORY_DOWNLOADS + "/" + folder;
    }

    private void showDownloadBandwidthLimitDialog() {
        String[] labels = getResources().getStringArray(R.array.bandwidth_limit_names);
        String[] values = getResources().getStringArray(R.array.bandwidth_limit_values);
        String current = prefs.getString(SettingsKeys.PREF_DOWNLOAD_BANDWIDTH_LIMIT, "0");
        int checkedItem = indexOf(values, current);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.download_bandwidth_limit_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    prefs.edit()
                            .putString(SettingsKeys.PREF_DOWNLOAD_BANDWIDTH_LIMIT, values[which])
                            .apply();
                    dialog.dismiss();
                    buildPage();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getDownloadBandwidthLimitLabel() {
        String[] labels = getResources().getStringArray(R.array.bandwidth_limit_names);
        String[] values = getResources().getStringArray(R.array.bandwidth_limit_values);
        String current = prefs.getString(SettingsKeys.PREF_DOWNLOAD_BANDWIDTH_LIMIT, "0");
        int index = indexOf(values, current);
        return labels[Math.max(0, Math.min(index, labels.length - 1))];
    }

    private void addRadioRow(LinearLayout card, int titleRes, int summaryRes,
                             String key, String value, Runnable onChanged) {
        LinearLayout row = createHorizontalRow();
        RadioButton radio = new RadioButton(this);
        radio.setClickable(false);
        radio.setChecked(value.equals(prefs.getString(key, defaultForRadioKey(key))));
        row.addView(radio);

        LinearLayout texts = createTextColumn();
        texts.addView(createTitle(titleRes));
        if (summaryRes != 0) {
            TextView summary = createSummary(summaryRes);
            addTopMargin(summary, dp(4));
            texts.addView(summary);
        }
        row.addView(texts);
        row.setOnClickListener(v -> {
            prefs.edit().putString(key, value).apply();
            if (onChanged != null) {
                onChanged.run();
            }
            buildPage();
        });
        addRow(card, row);
    }

    private String defaultForRadioKey(String key) {
        if (SettingsKeys.PREF_THEME_MODE.equals(key)) {
            return "system";
        }
        if (SettingsKeys.PREF_ADDRESS_BAR_POSITION.equals(key)) {
            return "top";
        }
        if (SettingsKeys.PREF_INACTIVE_TAB_DAYS.equals(key)) {
            return "21";
        }
        return SettingsKeys.VALUE_OFF;
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setMinimumHeight(dp(64));
        row.setBackgroundResource(resolveAttr(android.R.attr.selectableItemBackground));
        return row;
    }

    private LinearLayout createTextColumn() {
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return texts;
    }

    private TextView createTitle(int titleRes) {
        return createTitle(getString(titleRes));
    }

    private TextView createTitle(String title) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextSize(16);
        view.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        return view;
    }

    private TextView createSummary(int summaryRes) {
        return createSummary(getString(summaryRes));
    }

    private TextView createSummary(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        return view;
    }

    private TextView createValue(String text) {
        TextView view = createSummary(text);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void addSection(int titleRes) {
        TextView title = createSummary(titleRes);
        title.setTextColor(ContextCompat.getColor(this, R.color.section_heading));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(4));
        content.addView(title, params);
    }

    private void addParagraph(int textRes) {
        TextView text = createSummary(textRes);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(8));
        content.addView(text, params);
    }

    private void addRow(LinearLayout card, View row) {
        if (card.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundResource(resolveAttr(android.R.attr.listDivider));
            card.addView(divider, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addTopMargin(View view, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = margin;
        view.setLayoutParams(params);
    }

    private String choiceLabel(String current, int[] labelResIds, String[] values) {
        int index = indexOf(values, current);
        if (index < 0 || index >= labelResIds.length) {
            index = 0;
        }
        return getString(labelResIds[index]);
    }

    private int indexOf(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                return i;
            }
        }
        return 0;
    }

    private void showSavedZoomDialog() {
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("zoom_") && entry.getValue() instanceof Float) {
                keys.add(entry.getKey());
                labels.add(entry.getKey().substring("zoom_".length()) + " - "
                        + getString(R.string.text_size_percent,
                        Math.round(((Float) entry.getValue()) * 100f)));
            }
        }
        if (keys.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.saved_zoom_for_sites)
                    .setMessage(R.string.no_saved_zoom)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.saved_zoom_for_sites)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    prefs.edit().remove(keys.get(which)).apply();
                    Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private List<String> getPreferredLanguageTags() {
        String raw = prefs.getString(SettingsKeys.PREF_PREFERRED_LANGUAGES, "");
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>(Arrays.asList(Locale.getDefault().toLanguageTag()));
        }
        List<String> tags = new ArrayList<>();
        for (String tag : raw.split(",")) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        if (tags.isEmpty()) {
            tags.add(Locale.getDefault().toLanguageTag());
        }
        return tags;
    }

    private void savePreferredLanguageTags(List<String> tags) {
        prefs.edit().putString(SettingsKeys.PREF_PREFERRED_LANGUAGES,
                android.text.TextUtils.join(",", tags)).apply();
        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime != null) {
            runtime.getSettings().setLocales(tags.toArray(new String[0]));
        }
    }

    private String languageDisplayName(String tag) {
        Locale locale = Locale.forLanguageTag(tag);
        String displayName = locale.getDisplayName();
        return displayName == null || displayName.trim().isEmpty() ? tag : displayName;
    }

    private void showLanguageActions(int index) {
        List<String> tags = getPreferredLanguageTags();
        if (index < 0 || index >= tags.size()) {
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (index > 0) {
            labels.add(getString(R.string.move_up));
            actions.add(() -> {
                String tag = tags.remove(index);
                tags.add(index - 1, tag);
                savePreferredLanguageTags(tags);
                buildPage();
            });
        }
        if (index < tags.size() - 1) {
            labels.add(getString(R.string.move_down));
            actions.add(() -> {
                String tag = tags.remove(index);
                tags.add(index + 1, tag);
                savePreferredLanguageTags(tags);
                buildPage();
            });
        }
        if (tags.size() > 1) {
            labels.add(getString(R.string.remove_language));
            actions.add(() -> {
                tags.remove(index);
                savePreferredLanguageTags(tags);
                buildPage();
            });
        }
        if (labels.isEmpty()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(languageDisplayName(tags.get(index)))
                .setItems(labels.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void showAddLanguageDialog() {
        String[] tags = {
                "en-US", "en-GB", "hi-IN", "bn-IN", "es-ES",
                "fr-FR", "de-DE", "pt-BR", "ja-JP", "ko-KR"
        };
        String[] labels = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            labels[i] = languageDisplayName(tags[i]);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_language)
                .setItems(labels, (dialog, which) -> {
                    List<String> current = getPreferredLanguageTags();
                    if (!current.contains(tags[which])) {
                        current.add(tags[which]);
                        savePreferredLanguageTags(current);
                        buildPage();
                    }
                })
                .show();
    }

    private void applyThemeMode() {
        String mode = prefs.getString(SettingsKeys.PREF_THEME_MODE, "system");
        switch (mode) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime != null) {
            runtime.getSettings().setPreferredColorScheme(getPreferredColorScheme(mode));
        }
    }

    private int getPreferredColorScheme(String mode) {
        if ("light".equals(mode)) {
            return GeckoRuntimeSettings.COLOR_SCHEME_LIGHT;
        }
        if ("dark".equals(mode)) {
            return GeckoRuntimeSettings.COLOR_SCHEME_DARK;
        }
        return GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM;
    }

    private void applyRuntimePreferencesIfRunning() {
        if (RuntimeManager.getExistingRuntime() != null) {
            RuntimeManager.getRuntime(this);
        }
    }

    private void applyDoNotTrackPreferenceIfRunning() {
        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime != null) {
            runtime.getSettings().setGlobalPrivacyControl(
                    prefs.getBoolean("do_not_track", true));
        }
    }

    private void applyContentBlockingPreferencesIfRunning() {
        GeckoRuntime runtime = RuntimeManager.getExistingRuntime();
        if (runtime == null) {
            return;
        }
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

        ContentBlocking.Settings settings = runtime.getSettings().getContentBlocking();
        settings.setAntiTracking(antiTracking)
                .setEnhancedTrackingProtectionLevel(etpLevel)
                .setSafeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .setCookieBannerMode(prefs.getBoolean("block_cookie_banners", true)
                        ? ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT
                        : ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
                .setCookieBannerModePrivateBrowsing(
                        ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
                .setQueryParameterStrippingEnabled(
                        prefs.getBoolean("strip_tracking_params", true))
                .setQueryParameterStrippingPrivateBrowsingEnabled(true);
    }

    private void openCaptionSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
        }
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSubpage(String targetPage) {
        Intent intent = new Intent(this, SettingsSubpageActivity.class);
        intent.putExtra(EXTRA_PAGE, targetPage);
        startActivity(intent);
    }

    private int resolveAttr(int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.resourceId != 0 ? value.resourceId : value.data;
    }

    private int resolveColor(int attr) {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(this, value.resourceId);
            }
            return value.data;
        }
        return ContextCompat.getColor(this, R.color.colorOnSurface);
    }

    private int getActionBarSize() {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            return TypedValue.complexToDimensionPixelSize(value.data,
                    getResources().getDisplayMetrics());
        }
        return dp(56);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
