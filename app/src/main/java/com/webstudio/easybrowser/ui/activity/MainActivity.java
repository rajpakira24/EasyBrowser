package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.MenuItem;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.QuickAccessAdapter;
import com.webstudio.easybrowser.managers.AnalyticsManager;
import com.webstudio.easybrowser.managers.PrivacyStatsManager;
import com.webstudio.easybrowser.models.QuickAccessItem;
import com.webstudio.easybrowser.repository.QuickAccessRepository;
import com.webstudio.easybrowser.repository.TabRepository;
import com.webstudio.easybrowser.utils.HomeBackgroundProvider;
import com.webstudio.easybrowser.utils.SearchSuggestionProvider;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.UrlUtils;

import com.webstudio.easybrowser.adapters.SuggestionsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;

public class MainActivity extends AppCompatActivity implements QuickAccessAdapter.OnQuickAccessClickListener {
    private static final int HOME_SEARCH_NAV_GAP_DP = 14;
    private static final int HOME_SEARCH_MIN_BOTTOM_MARGIN_DP = 118;
    private static final int HOME_PHOTO_CREDIT_GAP_DP = 18;
    private static final int HOME_SCROLL_BOTTOM_GAP_DP = 28;

    private EditText urlInput;
    private ImageButton micButton;
    private ImageButton securityButton;
    private ImageButton clearButton;
    private ImageButton searchButton;
    private RecyclerView quickAccessRecycler;
    private View privacyStatsCard;
    private TextView quickAccessTitle;
    private TextView protectedPagesStat;
    private TextView blockedItemsStat;
    private TextView timeSavedStat;
    private ImageView homeBackgroundImage;
    private TextView photoCredit;
    private View homeContentContainer;
    private View homeSearchBar;
    private BottomNavigationView bottomNav;
    private QuickAccessRepository quickAccessRepository;
    private TabRepository tabRepository;
    private QuickAccessAdapter quickAccessAdapter;
    private HomeBackgroundProvider.Photo currentHomePhoto;
    private ActivityResultLauncher<Intent> tabManagerLauncher;
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
        SystemBarUtils.apply(this,
                ContextCompat.getColor(this, R.color.home_background),
                ContextCompat.getColor(this, R.color.home_panel_background));

        initializeRepositories();
        initializeViews();
        setupToolbar();
        setupUrlInput();
        setupQuickAccess();
        setupTabManagerLauncher();
        setupBottomNavigation();
        setupHomeBottomChromeSpacing();
        setupClickListeners();
        setupSpeechRecognition();
        setupHomeBackground();
        applyHomeSectionVisibility();
        updatePrivacyStats();
        handleIncomingIntent(getIntent());
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
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(privacyStatsListener);
        applyHomeSectionVisibility();
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

    private void initializeViews() {
        urlInput = findViewById(R.id.url_input);
        micButton = findViewById(R.id.btn_mic);
        securityButton = findViewById(R.id.btn_security);
        clearButton = findViewById(R.id.btn_clear);
        searchButton = findViewById(R.id.btn_search);
        quickAccessRecycler = findViewById(R.id.quick_access_recycler);
        privacyStatsCard = findViewById(R.id.privacy_stats_card);
        quickAccessTitle = findViewById(R.id.quick_access_title);
        protectedPagesStat = findViewById(R.id.stat_protected_pages);
        blockedItemsStat = findViewById(R.id.stat_blocked_items);
        timeSavedStat = findViewById(R.id.stat_time_saved);
        homeBackgroundImage = findViewById(R.id.home_background_image);
        photoCredit = findViewById(R.id.home_photo_credit);
        homeContentContainer = findViewById(R.id.home_content_container);
        homeSearchBar = findViewById(R.id.home_edge_search_bar);
        bottomNav = findViewById(R.id.bottom_navigation);
        updateTabCountIcon();
    }

    private void initializeRepositories() {
        quickAccessRepository = new QuickAccessRepository(this);
        tabRepository = new TabRepository(this);
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
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", UrlUtils.getUrlOrSearchUrl(this, input));
        startActivity(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
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

        // Load quick access items
        quickAccessRepository.getMostVisitedItems(8, new QuickAccessRepository.QuickAccessCallback() {
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

    private void reloadQuickAccess() {
        quickAccessRepository.getMostVisitedItems(8, new QuickAccessRepository.QuickAccessCallback() {
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

    private void setupHomeBackground() {
        currentHomePhoto = HomeBackgroundProvider.nextPhoto();
        if (homeBackgroundImage != null) {
            Glide.with(this)
                    .load(currentHomePhoto.getImageUrl())
                    .placeholder(R.drawable.bg_home_photo_scrim)
                    .error(R.drawable.bg_home_photo_scrim)
                    .centerCrop()
                    .into(homeBackgroundImage);
        }
        if (photoCredit != null) {
            photoCredit.setText(currentHomePhoto.getCreditText());
            photoCredit.setOnClickListener(v -> handleUrlInput(currentHomePhoto.getSourceUrl()));
        }
    }

    private void applyHomeSectionVisibility() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showPrivacyStats = prefs.getBoolean("show_privacy_stats", true);
        boolean showQuickAccess = prefs.getBoolean("show_quick_access", true);
        privacyStatsCard.setVisibility(showPrivacyStats ? View.VISIBLE : View.GONE);
        quickAccessTitle.setVisibility(showQuickAccess ? View.VISIBLE : View.GONE);
        quickAccessRecycler.setVisibility(showQuickAccess ? View.VISIBLE : View.GONE);
    }

    private void updatePrivacyStats() {
        PrivacyStatsManager.Stats stats = PrivacyStatsManager.getStats(this);

        protectedPagesStat.setText(String.valueOf(stats.pagesProtected));
        blockedItemsStat.setText(String.valueOf(stats.itemsBlocked));
        timeSavedStat.setText(formatTimeSaved(stats.timeSavedSeconds));
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
                dp(HOME_SEARCH_MIN_BOTTOM_MARGIN_DP));

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
        if (bottomNav == null) {
            return;
        }
        MenuItem tabsItem = bottomNav.getMenu().findItem(R.id.nav_tabs);
        if (tabsItem == null) {
            return;
        }
        int count = Math.max(1, getSavedPublicTabCount());
        tabsItem.setIcon(createTabCountIcon(count));
        bottomNav.post(() -> tabsItem.setIcon(createTabCountIcon(Math.max(1, getSavedPublicTabCount()))));
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

        securityButton.setOnClickListener(v -> showSearchPopup());
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
        });

        EditText searchInput = dialog.findViewById(R.id.search_popup_input);
        ImageButton popupMic = dialog.findViewById(R.id.btn_search_popup_mic);
        ImageButton popupGo = dialog.findViewById(R.id.btn_search_popup_go);
        RecyclerView suggestionsRecycler = dialog.findViewById(R.id.suggestions_recycler);

        SuggestionsAdapter suggestionsAdapter = new SuggestionsAdapter(suggestion -> {
            handleUrlInput(suggestion);
            dialog.dismiss();
        });
        suggestionsRecycler.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));
        suggestionsRecycler.setAdapter(suggestionsAdapter);

        searchInput.setText(urlInput.getText());
        searchInput.selectAll();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    suggestionsRecycler.setVisibility(View.GONE);
                    return;
                }
                suggestionProvider.fetchSuggestions(query, suggestions -> runOnUiThread(() -> {
                    if (suggestions.isEmpty()) {
                        suggestionsRecycler.setVisibility(View.GONE);
                    } else {
                        suggestionsAdapter.setSuggestions(suggestions);
                        suggestionsRecycler.setVisibility(View.VISIBLE);
                    }
                }));
            }
        });

        popupMic.setOnClickListener(v -> startVoiceRecognition());
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
        // Show options menu for the quick access item
        new MaterialAlertDialogBuilder(this)
                .setItems(new String[]{
                        getString(R.string.edit),
                        getString(R.string.remove)
                }, (dialog, which) -> {
                    if (which == 0) {
                        showEditQuickAccessDialog(item);
                    } else {
                        removeQuickAccessItem(item);
                    }
                })
                .show();
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
