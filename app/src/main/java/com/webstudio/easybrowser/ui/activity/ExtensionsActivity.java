package com.webstudio.easybrowser.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.utils.ThemeEngine;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.Image;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ExtensionsActivity extends AppCompatActivity {
    private static final int SECTION_INSTALLED = 0;
    private static final int SECTION_RECOMMENDED = 1;
    private static final int SECTION_MARKETPLACE = 2;
    private static final String MARKETPLACE_DEFAULT_QUERY = "privacy";

    private static final RecommendedExtension[] RECOMMENDED_EXTENSIONS = {
            new RecommendedExtension(R.string.extension_ublock_origin,
                    R.string.extension_ublock_origin_summary,
                    "uBlock0@raymondhill.net",
                    "uBlock Origin",
                    "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
                    "https://addons.mozilla.org/user-media/addon_icons/607/607454-64.png"),
            new RecommendedExtension(R.string.extension_privacy_badger,
                    R.string.extension_privacy_badger_summary,
                    "jid1-MnnxcxisBPnSXQ@jetpack",
                    "Privacy Badger",
                    "https://addons.mozilla.org/firefox/downloads/latest/privacy-badger17/latest.xpi",
                    "https://addons.mozilla.org/user-media/addon_icons/506/506646-64.png"),
            new RecommendedExtension(R.string.extension_clearurls,
                    R.string.extension_clearurls_summary,
                    "{74145f27-f039-47ce-a470-a662b129930a}",
                    "ClearURLs",
                    "https://addons.mozilla.org/firefox/downloads/latest/clearurls/latest.xpi",
                    "https://addons.mozilla.org/user-media/addon_icons/839/839767-64.png"),
            new RecommendedExtension(R.string.extension_dark_reader,
                    R.string.extension_dark_reader_summary,
                    "addon@darkreader.org",
                    "Dark Reader",
                    "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
                    "https://addons.mozilla.org/user-media/addon_icons/855/855413-64.png"),
            new RecommendedExtension(R.string.extension_decentraleyes,
                    R.string.extension_decentraleyes_summary,
                    "jid1-BoFifL9Vbdl2zQ@jetpack",
                    "Decentraleyes",
                    "https://addons.mozilla.org/firefox/downloads/latest/decentraleyes/latest.xpi",
                    "https://addons.mozilla.org/user-media/addon_icons/521/521554-64.png")
    };

    private final OkHttpClient httpClient = new OkHttpClient();
    private final List<WebExtension> installedExtensions = new ArrayList<>();
    private final List<MarketplaceExtension> marketplaceExtensions = new ArrayList<>();

    private LinearLayout installedSection;
    private LinearLayout recommendedSection;
    private LinearLayout marketplaceSection;
    private LinearLayout extensionList;
    private LinearLayout recommendedExtensionList;
    private LinearLayout marketplaceList;
    private TextView marketplaceStatus;
    private TextInputEditText extensionUrlInput;
    private TextInputEditText marketplaceSearchInput;
    private MaterialButton installedTab;
    private MaterialButton recommendedTab;
    private MaterialButton marketplaceTab;
    private WebExtensionController extensionController;
    private Call marketplaceCall;
    private int currentSection = SECTION_INSTALLED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extensionController = RuntimeManager.getRuntime(this).getWebExtensionController();
        extensionController.setPromptDelegate(new WebExtensionController.PromptDelegate() {
            @Override
            public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(
                    @NonNull WebExtension extension,
                    @NonNull String[] permissions,
                    @NonNull String[] origins,
                    @NonNull String[] dataCollectionPermissions) {
                return GeckoResult.fromValue(
                        new WebExtension.PermissionPromptResponse(true, true, true));
            }
        });

        setContentView(createContentView());
        loadExtensions();
        loadMarketplace(MARKETPLACE_DEFAULT_QUERY);
    }

    @Override
    protected void onDestroy() {
        if (marketplaceCall != null) {
            marketplaceCall.cancel();
        }
        super.onDestroy();
    }

    private View createContentView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(R.string.extensions);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ThemeEngine.applyChrome(this, toolbar);
        screen.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)));

        screen.addView(createSectionTabs(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        root.setPadding(padding, padding, padding, dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));

        installedSection = createInstalledSection();
        recommendedSection = createRecommendedSection();
        marketplaceSection = createMarketplaceSection();
        root.addView(installedSection);
        root.addView(recommendedSection);
        root.addView(marketplaceSection);
        showSection(SECTION_INSTALLED);
        return screen;
    }

    private View createSectionTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(12), dp(10), dp(12), dp(6));
        tabs.setBackgroundColor(ThemeEngine.settingsChromeColor(this));

        installedTab = createTabButton(R.string.extension_tab_installed, SECTION_INSTALLED);
        recommendedTab = createTabButton(R.string.extension_tab_recommended, SECTION_RECOMMENDED);
        marketplaceTab = createTabButton(R.string.extension_marketplace, SECTION_MARKETPLACE);
        tabs.addView(installedTab, tabLayoutParams());
        tabs.addView(recommendedTab, tabLayoutParams());
        tabs.addView(marketplaceTab, tabLayoutParams());
        return tabs;
    }

    private MaterialButton createTabButton(int titleResId, int section) {
        MaterialButton button = new MaterialButton(this);
        button.setText(titleResId);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setTextSize(14);
        button.setMinHeight(dp(40));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setOnClickListener(v -> showSection(section));
        return button;
    }

    private LinearLayout.LayoutParams tabLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(44),
                1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private LinearLayout createInstalledSection() {
        LinearLayout section = createSectionContainer();
        section.addView(createManualInstallCard(), cardLayoutParams());
        addSectionTitle(section, R.string.installed_extensions, dp(18));
        extensionList = new LinearLayout(this);
        extensionList.setOrientation(LinearLayout.VERTICAL);
        section.addView(extensionList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return section;
    }

    private LinearLayout createRecommendedSection() {
        LinearLayout section = createSectionContainer();
        addSectionTitle(section, R.string.recommended_extensions, 0);
        recommendedExtensionList = new LinearLayout(this);
        recommendedExtensionList.setOrientation(LinearLayout.VERTICAL);
        section.addView(recommendedExtensionList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return section;
    }

    private LinearLayout createMarketplaceSection() {
        LinearLayout section = createSectionContainer();
        MaterialCardView searchCard = createCard();
        LinearLayout searchContent = new LinearLayout(this);
        searchContent.setOrientation(LinearLayout.VERTICAL);
        searchContent.setPadding(dp(16), dp(16), dp(16), dp(16));
        searchCard.addView(searchContent);

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(getString(R.string.search_extensions));
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        marketplaceSearchInput = new TextInputEditText(inputLayout.getContext());
        marketplaceSearchInput.setSingleLine(true);
        inputLayout.addView(marketplaceSearchInput, new TextInputLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        searchContent.addView(inputLayout);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton privacyButton = createSearchChip("Privacy", "privacy");
        MaterialButton blockersButton = createSearchChip("Blockers", "ad blocker");
        MaterialButton searchButton = new MaterialButton(this);
        searchButton.setText(R.string.search);
        searchButton.setOnClickListener(v -> searchMarketplaceFromInput());
        buttons.addView(privacyButton);
        buttons.addView(blockersButton);
        buttons.addView(searchButton);
        searchContent.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        section.addView(searchCard, cardLayoutParams());
        marketplaceStatus = createSecondaryText(getString(R.string.extension_marketplace_loading));
        marketplaceStatus.setPadding(dp(16), dp(10), dp(16), dp(10));
        section.addView(marketplaceStatus);

        marketplaceList = new LinearLayout(this);
        marketplaceList.setOrientation(LinearLayout.VERTICAL);
        section.addView(marketplaceList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return section;
    }

    private MaterialButton createSearchChip(String label, String query) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setOnClickListener(v -> {
            marketplaceSearchInput.setText(query);
            loadMarketplace(query);
        });
        return button;
    }

    private MaterialCardView createManualInstallCard() {
        MaterialCardView installCard = createCard();
        LinearLayout installContent = new LinearLayout(this);
        installContent.setOrientation(LinearLayout.VERTICAL);
        installContent.setPadding(dp(16), dp(16), dp(16), dp(16));
        installCard.addView(installContent);

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(getString(R.string.extension_url_hint));
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        extensionUrlInput = new TextInputEditText(inputLayout.getContext());
        extensionUrlInput.setSingleLine(true);
        extensionUrlInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        inputLayout.addView(extensionUrlInput, new TextInputLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        installContent.addView(inputLayout);

        MaterialButton installButton = new MaterialButton(this);
        installButton.setText(R.string.install_extension);
        installButton.setOnClickListener(v -> installExtension());
        LinearLayout.LayoutParams installButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        installButtonParams.gravity = Gravity.END;
        installButtonParams.setMargins(0, dp(12), 0, 0);
        installContent.addView(installButton, installButtonParams);
        return installCard;
    }

    private LinearLayout createSectionContainer() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        return section;
    }

    private void showSection(int section) {
        currentSection = section;
        installedSection.setVisibility(section == SECTION_INSTALLED ? View.VISIBLE : View.GONE);
        recommendedSection.setVisibility(section == SECTION_RECOMMENDED ? View.VISIBLE : View.GONE);
        marketplaceSection.setVisibility(section == SECTION_MARKETPLACE ? View.VISIBLE : View.GONE);
        updateTabButton(installedTab, section == SECTION_INSTALLED);
        updateTabButton(recommendedTab, section == SECTION_RECOMMENDED);
        updateTabButton(marketplaceTab, section == SECTION_MARKETPLACE);
    }

    private void updateTabButton(MaterialButton button, boolean selected) {
        ThemeEngine.Palette palette = ThemeEngine.homePalette(this);
        int background = selected ? palette.accent : palette.searchBackground;
        int foreground = selected ? ThemeEngine.foregroundFor(background) : palette.onSurface;
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(background));
        button.setTextColor(foreground);
    }

    private void searchMarketplaceFromInput() {
        String query = marketplaceSearchInput.getText() != null
                ? marketplaceSearchInput.getText().toString().trim()
                : "";
        loadMarketplace(TextUtils.isEmpty(query) ? MARKETPLACE_DEFAULT_QUERY : query);
    }

    private void loadMarketplace(String query) {
        if (marketplaceCall != null) {
            marketplaceCall.cancel();
        }
        marketplaceStatus.setText(R.string.extension_marketplace_loading);
        marketplaceStatus.setVisibility(View.VISIBLE);
        marketplaceList.removeAllViews();

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            encodedQuery = MARKETPLACE_DEFAULT_QUERY;
        }
        String url = "https://addons.mozilla.org/api/v5/addons/search/"
                + "?app=android&type=extension&page_size=20&q=" + encodedQuery;
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();
        marketplaceCall = httpClient.newCall(request);
        marketplaceCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) {
                    return;
                }
                runOnUiThread(() -> {
                    marketplaceExtensions.clear();
                    marketplaceStatus.setText(R.string.extension_marketplace_failed);
                    marketplaceStatus.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String json = body != null ? body.string() : "";
                    List<MarketplaceExtension> parsed = response.isSuccessful()
                            ? parseMarketplaceExtensions(json)
                            : new ArrayList<>();
                    runOnUiThread(() -> {
                        marketplaceExtensions.clear();
                        marketplaceExtensions.addAll(parsed);
                        renderExtensions();
                        renderRecommendedExtensions();
                        renderMarketplaceExtensions();
                    });
                }
            }
        });
    }

    private List<MarketplaceExtension> parseMarketplaceExtensions(String json) {
        List<MarketplaceExtension> extensions = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return extensions;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = root.has("results") && root.get("results").isJsonArray()
                ? root.getAsJsonArray("results")
                : new JsonArray();
        for (JsonElement element : results) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            MarketplaceExtension extension = new MarketplaceExtension();
            extension.id = getStringValue(item, "guid");
            extension.slug = getStringValue(item, "slug");
            extension.name = getLocalizedValue(item, "name");
            extension.summary = getLocalizedValue(item, "summary");
            extension.iconUrl = getStringValue(item, "icon_url");
            extension.detailUrl = getAbsoluteAmoUrl(getStringValue(item, "url"), extension.slug);
            extension.author = getFirstAuthor(item);
            extension.rating = getRating(item);
            extension.userCount = getIntValue(item, "average_daily_users");
            extension.installUrl = getInstallUrl(item, extension.slug);
            if (!TextUtils.isEmpty(extension.name) && !TextUtils.isEmpty(extension.installUrl)) {
                extensions.add(extension);
            }
        }
        return extensions;
    }

    private void renderMarketplaceExtensions() {
        marketplaceList.removeAllViews();
        if (marketplaceExtensions.isEmpty()) {
            marketplaceStatus.setText(R.string.extension_marketplace_empty);
            marketplaceStatus.setVisibility(View.VISIBLE);
            return;
        }
        marketplaceStatus.setVisibility(View.GONE);
        for (MarketplaceExtension extension : marketplaceExtensions) {
            marketplaceList.addView(createMarketplaceExtensionCard(extension), cardLayoutParams());
        }
    }

    private MaterialCardView createMarketplaceExtensionCard(MarketplaceExtension extension) {
        MaterialCardView card = createCard();
        LinearLayout row = createExtensionRow();
        card.addView(row);
        addIcon(row, extension.iconUrl);

        LinearLayout textColumn = createTextColumn();
        textColumn.addView(createPrimaryText(extension.name));
        if (!TextUtils.isEmpty(extension.summary)) {
            TextView summary = createSecondaryText(extension.summary);
            summary.setMaxLines(2);
            summary.setEllipsize(TextUtils.TruncateAt.END);
            textColumn.addView(summary, topMarginParams(4));
        }
        String meta = buildMarketplaceMeta(extension);
        if (!TextUtils.isEmpty(meta)) {
            textColumn.addView(createSecondaryText(meta), topMarginParams(4));
        }
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        MaterialButton installButton = new MaterialButton(this);
        boolean installed = isMarketplaceExtensionInstalled(extension);
        installButton.setText(installed ? R.string.extension_installed_label : R.string.install_extension);
        installButton.setEnabled(!installed);
        installButton.setOnClickListener(v -> installExtension(extension.installUrl));
        row.addView(installButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setOnClickListener(v -> showMarketplaceDetails(extension));
        return card;
    }

    private void showMarketplaceDetails(MarketplaceExtension extension) {
        StringBuilder message = new StringBuilder();
        if (!TextUtils.isEmpty(extension.summary)) {
            message.append(extension.summary);
        }
        String meta = buildMarketplaceMeta(extension);
        if (!TextUtils.isEmpty(meta)) {
            message.append("\n\n").append(meta);
        }
        if (!TextUtils.isEmpty(extension.detailUrl)) {
            message.append("\n\n").append(extension.detailUrl);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(extension.name)
                .setMessage(message.toString())
                .setPositiveButton(R.string.install_extension, (dialog, which) ->
                        installExtension(extension.installUrl))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String buildMarketplaceMeta(MarketplaceExtension extension) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(extension.author)) {
            parts.add(extension.author);
        }
        if (extension.rating > 0) {
            parts.add(String.format(Locale.US, "%.1f", extension.rating));
        }
        if (extension.userCount > 0) {
            parts.add(getString(R.string.extension_users_count, extension.userCount));
        }
        return TextUtils.join("  -  ", parts);
    }

    private void installExtension() {
        String url = extensionUrlInput.getText() != null
                ? extensionUrlInput.getText().toString().trim()
                : "";
        installExtension(url);
    }

    private void installExtension(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Toast.makeText(this, R.string.extension_installing, Toast.LENGTH_SHORT).show();
        extensionController.install(url, WebExtensionController.INSTALLATION_METHOD_MANAGER)
                .accept(extension -> runOnUiThread(() -> {
                            AnalyticsManager.logExtensionInstall(this, getInstallSource(url), true);
                            Toast.makeText(this, R.string.extension_installed, Toast.LENGTH_SHORT).show();
                            if (extensionUrlInput != null) {
                                extensionUrlInput.setText("");
                            }
                            loadExtensions();
                        }),
                        exception -> runOnUiThread(() -> {
                            AnalyticsManager.logExtensionInstall(this, getInstallSource(url), false);
                            Toast.makeText(this, R.string.extension_install_failed,
                                    Toast.LENGTH_LONG).show();
                        }));
    }

    private String getInstallSource(String url) {
        if (TextUtils.isEmpty(url)) {
            return "unknown";
        }
        if (url.contains("addons.mozilla.org")) {
            return "mozilla_addons";
        }
        return "manual";
    }

    private void loadExtensions() {
        extensionController.list()
                .accept(extensions -> runOnUiThread(() -> {
                            installedExtensions.clear();
                            if (extensions != null) {
                                installedExtensions.addAll(extensions);
                            }
                            renderExtensions();
                            renderRecommendedExtensions();
                            renderMarketplaceExtensions();
                        }),
                        exception -> runOnUiThread(() -> {
                            installedExtensions.clear();
                            renderExtensions();
                            renderRecommendedExtensions();
                            renderMarketplaceExtensions();
                        }));
    }

    private void renderExtensions() {
        extensionList.removeAllViews();
        if (installedExtensions.isEmpty()) {
            extensionList.addView(createEmptyText(R.string.no_extensions));
            return;
        }
        for (WebExtension extension : installedExtensions) {
            extensionList.addView(createInstalledExtensionCard(extension), cardLayoutParams());
        }
    }

    private void renderRecommendedExtensions() {
        recommendedExtensionList.removeAllViews();
        boolean hasRecommendation = false;
        for (RecommendedExtension extension : RECOMMENDED_EXTENSIONS) {
            if (isRecommendedExtensionInstalled(extension)) {
                continue;
            }
            hasRecommendation = true;
            recommendedExtensionList.addView(createRecommendedExtensionCard(extension),
                    cardLayoutParams());
        }
        if (!hasRecommendation) {
            recommendedExtensionList.addView(createEmptyText(
                    R.string.all_recommended_extensions_installed));
        }
    }

    private MaterialCardView createInstalledExtensionCard(WebExtension extension) {
        MaterialCardView card = createCard();
        LinearLayout row = createExtensionRow();
        card.addView(row);
        addInstalledIcon(row, extension);

        LinearLayout content = createTextColumn();
        content.addView(createPrimaryText(getExtensionName(extension)));
        String summaryText = !TextUtils.isEmpty(extension.metaData.description)
                ? extension.metaData.description
                : extension.metaData.version;
        if (!TextUtils.isEmpty(summaryText)) {
            content.addView(createSecondaryText(summaryText), topMarginParams(4));
        }
        row.addView(content, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        MaterialButton removeButton = new MaterialButton(this);
        removeButton.setText(R.string.remove);
        removeButton.setOnClickListener(v -> confirmRemoveExtension(extension));
        row.addView(removeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private MaterialCardView createRecommendedExtensionCard(RecommendedExtension extension) {
        MaterialCardView card = createCard();
        LinearLayout row = createExtensionRow();
        card.addView(row);
        addIcon(row, extension.iconUrl);

        LinearLayout textColumn = createTextColumn();
        textColumn.addView(createPrimaryText(getString(extension.titleResId)));
        TextView summary = createSecondaryText(getString(extension.summaryResId));
        summary.setMaxLines(2);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(summary, topMarginParams(4));
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        MaterialButton installButton = new MaterialButton(this);
        installButton.setText(R.string.install_extension);
        installButton.setOnClickListener(v -> installExtension(extension.xpiUrl));
        row.addView(installButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private LinearLayout createExtensionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        return row;
    }

    private LinearLayout createTextColumn() {
        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        return textColumn;
    }

    private void addIcon(LinearLayout row, String iconUrl) {
        ImageView icon = createExtensionIconView(row);
        if (TextUtils.isEmpty(iconUrl)) {
            icon.setImageResource(R.drawable.ic_globe);
            return;
        }
        loadIconUrl(icon, iconUrl);
    }

    private void addInstalledIcon(LinearLayout row, WebExtension extension) {
        ImageView icon = createExtensionIconView(row);
        String fallbackIconUrl = getInstalledExtensionIconUrl(extension);
        Image packagedIcon = extension != null && extension.metaData != null
                ? extension.metaData.icon
                : null;
        if (packagedIcon == null) {
            loadExtensionIconFallback(icon, fallbackIconUrl);
            return;
        }
        icon.setImageResource(R.drawable.ic_globe);
        packagedIcon.getBitmap(dp(64)).accept(bitmap -> runOnUiThread(() -> {
                    if (bitmap != null) {
                        icon.setImageBitmap(bitmap);
                    } else {
                        loadExtensionIconFallback(icon, fallbackIconUrl);
                    }
                }),
                exception -> runOnUiThread(() -> loadExtensionIconFallback(icon, fallbackIconUrl)));
    }

    private ImageView createExtensionIconView(LinearLayout row) {
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackgroundColor(ContextCompat.getColor(this, R.color.search_bar_background));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(0, 0, dp(12), 0);
        row.addView(icon, params);
        return icon;
    }

    private void loadExtensionIconFallback(ImageView icon, String iconUrl) {
        if (TextUtils.isEmpty(iconUrl)) {
            icon.setImageResource(R.drawable.ic_globe);
            return;
        }
        loadIconUrl(icon, iconUrl);
    }

    private void loadIconUrl(ImageView icon, String iconUrl) {
        Glide.with(icon)
                .load(iconUrl)
                .placeholder(R.drawable.ic_globe)
                .error(R.drawable.ic_globe)
                .into(icon);
    }

    private String getInstalledExtensionIconUrl(WebExtension extension) {
        RecommendedExtension recommendedExtension = findRecommendedExtension(extension);
        if (recommendedExtension != null) {
            return recommendedExtension.iconUrl;
        }
        MarketplaceExtension marketplaceExtension = findMarketplaceExtension(extension);
        return marketplaceExtension != null ? marketplaceExtension.iconUrl : "";
    }

    private RecommendedExtension findRecommendedExtension(WebExtension extension) {
        if (extension == null) {
            return null;
        }
        String installedId = extension.id != null ? extension.id : "";
        String installedName = getExtensionName(extension);
        for (RecommendedExtension recommendedExtension : RECOMMENDED_EXTENSIONS) {
            if (installedId.equalsIgnoreCase(recommendedExtension.extensionId)
                    || installedName.equalsIgnoreCase(recommendedExtension.displayName)) {
                return recommendedExtension;
            }
        }
        return null;
    }

    private MarketplaceExtension findMarketplaceExtension(WebExtension extension) {
        if (extension == null) {
            return null;
        }
        String installedId = extension.id != null ? extension.id : "";
        String installedName = getExtensionName(extension);
        for (MarketplaceExtension marketplaceExtension : marketplaceExtensions) {
            if ((!TextUtils.isEmpty(marketplaceExtension.id)
                    && installedId.equalsIgnoreCase(marketplaceExtension.id))
                    || installedName.equalsIgnoreCase(marketplaceExtension.name)) {
                return marketplaceExtension;
            }
        }
        return null;
    }

    private void confirmRemoveExtension(WebExtension extension) {
        String extensionName = getExtensionName(extension);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_extension)
                .setMessage(getString(R.string.remove_extension_confirm, extensionName))
                .setPositiveButton(R.string.remove, (dialog, which) -> removeExtension(extension))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeExtension(WebExtension extension) {
        extensionController.uninstall(extension)
                .accept(result -> runOnUiThread(() -> {
                            Toast.makeText(this, R.string.extension_removed,
                                    Toast.LENGTH_SHORT).show();
                            loadExtensions();
                        }),
                        exception -> runOnUiThread(() ->
                                Toast.makeText(this, R.string.extension_remove_failed,
                                        Toast.LENGTH_LONG).show()));
    }

    private void addSectionTitle(LinearLayout root, int titleResId, int topMargin) {
        TextView title = new TextView(this);
        title.setText(titleResId);
        title.setTextColor(ContextCompat.getColor(this, R.color.colorOnBackground));
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, topMargin, 0, dp(10));
        root.addView(title, params);
    }

    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface));
        card.setCardElevation(dp(1));
        card.setRadius(dp(8));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.border_color));
        card.setStrokeWidth(1);
        return card;
    }

    private TextView createPrimaryText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        view.setTextSize(16);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView createSecondaryText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.gray));
        view.setTextSize(13);
        return view;
    }

    private TextView createEmptyText(int textResId) {
        TextView empty = createSecondaryText(getString(textResId));
        empty.setPadding(dp(16), dp(14), dp(16), dp(14));
        return empty;
    }

    private LinearLayout.LayoutParams cardLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private LinearLayout.LayoutParams topMarginParams(int marginTopDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(marginTopDp), 0, 0);
        return params;
    }

    private boolean isRecommendedExtensionInstalled(RecommendedExtension recommendedExtension) {
        for (WebExtension installedExtension : installedExtensions) {
            String installedId = installedExtension.id != null ? installedExtension.id : "";
            String installedName = getExtensionName(installedExtension);
            if (installedId.equalsIgnoreCase(recommendedExtension.extensionId)
                    || installedName.equalsIgnoreCase(recommendedExtension.displayName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMarketplaceExtensionInstalled(MarketplaceExtension extension) {
        for (WebExtension installedExtension : installedExtensions) {
            String installedId = installedExtension.id != null ? installedExtension.id : "";
            String installedName = getExtensionName(installedExtension);
            if ((!TextUtils.isEmpty(extension.id) && installedId.equalsIgnoreCase(extension.id))
                    || installedName.equalsIgnoreCase(extension.name)) {
                return true;
            }
        }
        return false;
    }

    private String getExtensionName(WebExtension extension) {
        return extension.metaData.name != null
                ? extension.metaData.name
                : extension.id;
    }

    private String getInstallUrl(JsonObject item, String slug) {
        if (!item.has("current_version") || !item.get("current_version").isJsonObject()) {
            return getLatestDownloadUrl(slug);
        }
        JsonObject version = item.getAsJsonObject("current_version");
        if (!version.has("files") || !version.get("files").isJsonArray()) {
            return getLatestDownloadUrl(slug);
        }
        JsonArray files = version.getAsJsonArray("files");
        String fallback = "";
        for (JsonElement element : files) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            String url = getStringValue(file, "url");
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            if (TextUtils.isEmpty(fallback)) {
                fallback = url;
            }
            String platform = getStringValue(file, "platform");
            if ("android".equalsIgnoreCase(platform) || "all".equalsIgnoreCase(platform)) {
                return url;
            }
        }
        return TextUtils.isEmpty(fallback) ? getLatestDownloadUrl(slug) : fallback;
    }

    private String getLatestDownloadUrl(String slug) {
        return TextUtils.isEmpty(slug)
                ? ""
                : "https://addons.mozilla.org/android/downloads/latest/" + slug + "/latest.xpi";
    }

    private String getLocalizedValue(JsonObject item, String name) {
        if (!item.has(name) || item.get(name).isJsonNull()) {
            return "";
        }
        JsonElement value = item.get(name);
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        JsonObject translations = value.getAsJsonObject();
        if (translations.has("en-US") && !translations.get("en-US").isJsonNull()) {
            return translations.get("en-US").getAsString();
        }
        for (String key : translations.keySet()) {
            JsonElement translation = translations.get(key);
            if (!translation.isJsonNull()) {
                return translation.getAsString();
            }
        }
        return "";
    }

    private String getStringValue(JsonObject item, String name) {
        if (!item.has(name) || item.get(name).isJsonNull()) {
            return "";
        }
        return item.get(name).getAsString();
    }

    private int getIntValue(JsonObject item, String name) {
        if (!item.has(name) || item.get(name).isJsonNull()) {
            return 0;
        }
        return item.get(name).getAsInt();
    }

    private double getRating(JsonObject item) {
        if (!item.has("ratings") || !item.get("ratings").isJsonObject()) {
            return 0;
        }
        JsonObject ratings = item.getAsJsonObject("ratings");
        if (!ratings.has("average") || ratings.get("average").isJsonNull()) {
            return 0;
        }
        return ratings.get("average").getAsDouble();
    }

    private String getFirstAuthor(JsonObject item) {
        if (!item.has("authors") || !item.get("authors").isJsonArray()) {
            return "";
        }
        JsonArray authors = item.getAsJsonArray("authors");
        if (authors.size() == 0 || !authors.get(0).isJsonObject()) {
            return "";
        }
        return getStringValue(authors.get(0).getAsJsonObject(), "name");
    }

    private String getAbsoluteAmoUrl(String rawUrl, String slug) {
        if (!TextUtils.isEmpty(rawUrl)) {
            if (rawUrl.startsWith("http")) {
                return rawUrl;
            }
            return "https://addons.mozilla.org" + rawUrl;
        }
        return TextUtils.isEmpty(slug) ? "" : "https://addons.mozilla.org/android/addon/" + slug + "/";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class RecommendedExtension {
        final int titleResId;
        final int summaryResId;
        final String extensionId;
        final String displayName;
        final String xpiUrl;
        final String iconUrl;

        RecommendedExtension(int titleResId, int summaryResId, String extensionId,
                             String displayName, String xpiUrl, String iconUrl) {
            this.titleResId = titleResId;
            this.summaryResId = summaryResId;
            this.extensionId = extensionId;
            this.displayName = displayName;
            this.xpiUrl = xpiUrl;
            this.iconUrl = iconUrl;
        }
    }

    private static class MarketplaceExtension {
        String id;
        String slug;
        String name;
        String summary;
        String iconUrl;
        String installUrl;
        String detailUrl;
        String author;
        double rating;
        int userCount;
    }
}
