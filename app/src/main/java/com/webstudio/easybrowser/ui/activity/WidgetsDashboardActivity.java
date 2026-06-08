package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.ThemeEngine;

public class WidgetsDashboardActivity extends AppCompatActivity {
    private static final long OPEN_ANIMATION_MS = 220L;
    private static final long CLOSE_ANIMATION_MS = 180L;

    private SharedPreferences prefs;
    private ThemeEngine.Palette palette;
    private LinearLayout content;
    private boolean closing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        palette = ThemeEngine.homePalette(this);
        setContentView(createContentView());
        runOpenAnimation();
    }

    private View createContentView() {
        int chrome = ThemeEngine.settingsChromeColor(this);
        SystemBarUtils.apply(this, chrome, chrome, ThemeEngine.useDarkSystemBarIcons(chrome));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.widgets_dashboard);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setOnClickListener(v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
        toolbar.setNavigationOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            closeWithAnimation();
        });
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getActionBarSize()));
        ThemeEngine.applyChrome(this, toolbar);

        NestedScrollView scrollView = new NestedScrollView(this);
        scrollView.setFillViewport(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(28));
        scrollView.addView(content, new NestedScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(createHeader());
        content.addView(createWidgetSwitchCard(R.drawable.ic_weather_partly_cloudy,
                R.string.show_weather_widget,
                R.string.show_weather_widget_summary,
                SettingsKeys.PREF_SHOW_WEATHER_WIDGET,
                true));
        content.addView(createWidgetSwitchCard(R.drawable.ic_security,
                R.string.show_privacy_stats,
                R.string.show_privacy_stats_summary,
                SettingsKeys.PREF_SHOW_PRIVACY_STATS,
                true));
        content.addView(createWidgetSwitchCard(R.drawable.ic_view_grid,
                R.string.show_quick_access,
                R.string.show_quick_access_summary,
                SettingsKeys.PREF_SHOW_QUICK_ACCESS,
                true));
        content.addView(createActionCard());

        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        TextView title = new TextView(this);
        title.setText(R.string.widgets_dashboard_title);
        title.setTextColor(resolveThemeTextColor(android.R.attr.textColorPrimary));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        header.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView summary = new TextView(this);
        summary.setText(R.string.widgets_dashboard_summary);
        summary.setTextColor(resolveThemeTextColor(android.R.attr.textColorSecondary));
        summary.setTextSize(14);
        summary.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(8);
        header.addView(summary, summaryParams);
        return header;
    }

    private View createWidgetSwitchCard(@DrawableRes int iconRes,
                                        @StringRes int titleRes,
                                        @StringRes int summaryRes,
                                        String prefKey,
                                        boolean defaultValue) {
        MaterialCardView card = createBaseCard();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(18), dp(16), dp(16), dp(16));
        card.addView(row, new MaterialCardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView icon = createIconBubble(iconRes);
        row.addView(icon);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.leftMargin = dp(16);
        labelsParams.rightMargin = dp(12);
        row.addView(labels, labelsParams);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextColor(resolveThemeTextColor(android.R.attr.textColorPrimary));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        labels.addView(title);

        TextView summary = new TextView(this);
        summary.setText(summaryRes);
        summary.setTextColor(resolveThemeTextColor(android.R.attr.textColorSecondary));
        summary.setTextSize(13);
        summary.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(4);
        labels.addView(summary, summaryParams);

        SwitchMaterial toggle = new SwitchMaterial(this);
        toggle.setChecked(prefs.getBoolean(prefKey, defaultValue));
        toggle.setThumbTintList(ThemeEngine.switchThumbTint(this));
        toggle.setTrackTintList(ThemeEngine.switchTrackTint(this));
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View.OnClickListener toggleListener = v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            boolean checked = !toggle.isChecked();
            toggle.setChecked(checked);
            prefs.edit().putBoolean(prefKey, checked).apply();
        };
        row.setOnClickListener(toggleListener);
        card.setOnClickListener(toggleListener);
        toggle.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            prefs.edit().putBoolean(prefKey, toggle.isChecked()).apply();
        });
        return card;
    }

    private View createActionCard() {
        MaterialCardView card = createBaseCard();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(layout, new MaterialCardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(R.string.widgets_dashboard_actions);
        title.setTextColor(resolveThemeTextColor(android.R.attr.textColorPrimary));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        layout.addView(title);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsParams.topMargin = dp(14);
        layout.addView(buttons, buttonsParams);

        MaterialButton weatherButton = createActionButton(R.drawable.ic_weather_partly_cloudy,
                R.string.weather);
        weatherButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(this, WeatherActivity.class));
        });
        buttons.addView(weatherButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton settingsButton = createActionButton(R.drawable.ic_settings,
                R.string.new_tab_page);
        settingsButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(this, SettingsSubpageActivity.class);
            intent.putExtra(SettingsSubpageActivity.EXTRA_PAGE, SettingsSubpageActivity.PAGE_NEW_TAB);
            startActivity(intent);
        });
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        settingsParams.leftMargin = dp(10);
        buttons.addView(settingsButton, settingsParams);
        return card;
    }

    private MaterialCardView createBaseCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(resolveThemeSurfaceColor());
        card.setRadius(dp(28));
        card.setCardElevation(dp(4));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(palette.panelBorder);
        card.setClickable(true);
        card.setFocusable(true);
        int rippleRes = resolveSelectableItemBackground();
        if (rippleRes != 0) {
            card.setForeground(ResourcesCompat.getDrawable(getResources(), rippleRes, getTheme()));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        card.setLayoutParams(params);
        return card;
    }

    private ImageView createIconBubble(@DrawableRes int iconRes) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ThemeEngine.foregroundFor(palette.accent));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(palette.accent);
        icon.setBackground(background);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(46));
        icon.setLayoutParams(params);
        return icon;
    }

    private MaterialButton createActionButton(@DrawableRes int iconRes, @StringRes int labelRes) {
        MaterialButton button = new MaterialButton(this);
        button.setText(labelRes);
        button.setIconResource(iconRes);
        button.setIconTint(ColorStateList.valueOf(ThemeEngine.foregroundFor(palette.accent)));
        button.setBackgroundTintList(ColorStateList.valueOf(palette.accent));
        button.setTextColor(ThemeEngine.foregroundFor(palette.accent));
        button.setCornerRadius(dp(22));
        button.setMinHeight(dp(48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private int resolveThemeTextColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return ContextCompat.getColor(this, value.resourceId);
        }
        return value.data != 0 ? value.data : Color.WHITE;
    }

    private int resolveThemeSurfaceColor() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true);
        if (value.resourceId != 0) {
            return ContextCompat.getColor(this, value.resourceId);
        }
        return value.data != 0 ? value.data : ContextCompat.getColor(this, R.color.colorSurface);
    }

    private int resolveSelectableItemBackground() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId;
    }

    private int getActionBarSize() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true);
        return value.data != 0
                ? android.util.TypedValue.complexToDimensionPixelSize(value.data,
                getResources().getDisplayMetrics())
                : dp(56);
    }

    private void runOpenAnimation() {
        if (content == null) {
            return;
        }
        content.setAlpha(0f);
        content.setScaleX(0.95f);
        content.setScaleY(0.95f);
        content.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(OPEN_ANIMATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closeWithAnimation() {
        if (closing || content == null) {
            finish();
            return;
        }
        closing = true;
        content.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(CLOSE_ANIMATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::finish)
                .start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
