package com.webstudio.easybrowser.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.WeatherAlertManager;
import com.webstudio.easybrowser.models.WeatherSnapshot;
import com.webstudio.easybrowser.repository.WeatherRepository;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.WeatherAnimationMapper;
import com.webstudio.easybrowser.utils.WeatherIconMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {
    private WeatherRepository repository;
    private LinearLayout content;
    private String units;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new WeatherRepository(this);
        units = repository.getUnits();
        ThemeEngine.applyChrome(this, null);
        setupLocationPermissionLauncher();
        buildShell();
        WeatherSnapshot cached = repository.getCachedSnapshot();
        if (cached != null) {
            showWeather(cached, true);
        } else {
            showLoading();
        }
        loadWeatherWithPermission(false);
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(isAppDarkMode()
                ? createVerticalGradient(0xFF101A1D, 0xFF0B1215)
                : createVerticalGradient(0xFFF5FBFF, 0xFFFFFCF6));
        setContentView(root);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.weather);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        MenuItem refresh = toolbar.getMenu().add(R.string.weather_refresh);
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.setOnMenuItemClickListener(item -> {
            showLoading();
            loadWeatherWithPermission(true);
            return true;
        });
        ThemeEngine.applyChrome(this, toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getActionBarSize()));

        NestedScrollView scrollView = new NestedScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(content, new NestedScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (!WeatherRepository.hasPreciseLocationPermission(this)
                            && repository.isUsingCurrentLocation()) {
                        Toast.makeText(this, R.string.weather_precise_location_required,
                                Toast.LENGTH_SHORT).show();
                    }
                    loadWeather(true);
                });
    }

    private void loadWeatherWithPermission(boolean forceRefresh) {
        if (repository.shouldRequestCurrentLocationPermission()
                && locationPermissionLauncher != null) {
            repository.markLocationPermissionRequested();
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        loadWeather(forceRefresh);
    }

    private void loadWeather(boolean forceRefresh) {
        repository.getWeather(forceRefresh, new WeatherRepository.WeatherCallback() {
            @Override
            public void onWeatherLoaded(WeatherSnapshot snapshot, boolean fromCache) {
                runOnUiThread(() -> {
                    showWeather(snapshot, fromCache);
                    if (!fromCache) {
                        WeatherAlertManager.maybeNotify(WeatherActivity.this, snapshot, units);
                    }
                });
            }

            @Override
            public void onWeatherError(Exception error, WeatherSnapshot cachedSnapshot) {
                runOnUiThread(() -> {
                    if (cachedSnapshot != null) {
                        showWeather(cachedSnapshot, true);
                        Toast.makeText(WeatherActivity.this,
                                R.string.weather_unavailable, Toast.LENGTH_SHORT).show();
                    } else {
                        showMessage(getString(R.string.weather_unavailable));
                    }
                });
            }
        });
    }

    private void openWeatherProvider() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.met.no/"));
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.weather_provider, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading() {
        content.removeAllViews();
        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER_HORIZONTAL);
        loading.setPadding(0, dp(28), 0, dp(28));

        LottieAnimationView animation = createLoopingAnimation(R.raw.weather_clock_landscape);
        loading.addView(animation, new LinearLayout.LayoutParams(dp(160), dp(120)));

        TextView text = createSummary(getString(R.string.weather_loading));
        text.setGravity(Gravity.CENTER);
        text.setTextSize(16);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = dp(10);
        loading.addView(text, textParams);

        content.addView(loading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void showMessage(String message) {
        content.removeAllViews();
        TextView text = createSummary(message);
        text.setTextSize(16);
        content.addView(text);
    }

    private void showWeather(WeatherSnapshot snapshot, boolean fromCache) {
        content.removeAllViews();

        boolean weatherNight = isNight(snapshot);
        boolean darkPalette = isAppDarkMode();
        LinearLayout current = addHeroPanel(snapshot, darkPalette);
        current.addView(createHeroHeader(snapshot, darkPalette, weatherNight));
        current.addView(createTodayBand(snapshot, darkPalette, weatherNight));

        LinearLayout metricGrid = new LinearLayout(this);
        metricGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams metricGridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metricGridParams.topMargin = dp(12);
        current.addView(metricGrid, metricGridParams);

        LinearLayout row1 = createMetricRow();
        row1.addView(createMetricChip(R.drawable.ic_weather_temperature,
                R.string.weather_metric_feels_like,
                snapshot.formatFeelsLike(units), darkPalette));
        row1.addView(createMetricChip(R.drawable.ic_weather_humidity,
                R.string.weather_metric_humidity,
                snapshot.getHumidity() + "%", darkPalette));
        row1.addView(createMetricChip(R.drawable.ic_weather_wind,
                R.string.weather_metric_wind,
                String.format(Locale.US, "%.0f", snapshot.getWindSpeed()), darkPalette));
        metricGrid.addView(row1);

        LinearLayout row2 = createMetricRow();
        row2.addView(createMetricChip(R.drawable.ic_weather_sunrise,
                R.string.weather_metric_sunrise,
                snapshot.getSunrise().isEmpty() ? "--" : snapshot.getSunrise(), darkPalette));
        row2.addView(createMetricChip(R.drawable.ic_weather_sunset,
                R.string.weather_metric_sunset,
                snapshot.getSunset().isEmpty() ? "--" : snapshot.getSunset(), darkPalette));
        row2.addView(createMetricChip(R.drawable.ic_weather_updated,
                R.string.weather_metric_updated,
                DateFormat.getTimeFormat(this).format(new Date(snapshot.getFetchedAt())), darkPalette));
        metricGrid.addView(row2);

        TextView provider = createSummary(getString(R.string.weather_provider));
        provider.setTextColor(heroSubTextColor(darkPalette));
        provider.setGravity(Gravity.CENTER);
        provider.setTextSize(13);
        provider.setContentDescription(getString(R.string.weather_provider_open));
        provider.setOnClickListener(v -> openWeatherProvider());
        addTopMargin(provider, dp(14));
        current.addView(provider);

        addHourlyConditions(snapshot, darkPalette);
        addAirQuality(snapshot);

        TextView forecastTitle = createSection(R.string.weather_forecast);
        content.addView(forecastTitle);
        LinearLayout forecast = addCard();
        for (WeatherSnapshot.ForecastDay day : snapshot.getForecastDays()) {
            addForecastRow(forecast, day);
        }
    }

    private LinearLayout createHeroHeader(WeatherSnapshot snapshot, boolean darkPalette,
                                          boolean weatherNight) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(textColumn, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setOrientation(LinearLayout.HORIZONTAL);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        LottieAnimationView locationPin = createLoopingAnimation(R.raw.weather_location_pin);
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        pinParams.rightMargin = dp(5);
        locationRow.addView(locationPin, pinParams);

        TextView location = createTitle(snapshot.getLocationName());
        location.setTextSize(24);
        location.setTextColor(heroTextColor(darkPalette));
        location.setMaxLines(1);
        location.setEllipsize(TextUtils.TruncateAt.END);
        locationRow.addView(location, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textColumn.addView(locationRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView temperature = createTitle(snapshot.formatTemperature(units));
        temperature.setTextSize(56);
        temperature.setTextColor(heroTextColor(darkPalette));
        temperature.setIncludeFontPadding(false);
        addTopMargin(temperature, dp(8));
        textColumn.addView(temperature);

        TextView condition = createHeroPill(snapshot.getCondition(), darkPalette);
        LinearLayout.LayoutParams conditionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        conditionParams.topMargin = dp(10);
        textColumn.addView(condition, conditionParams);

        TextView updated = createSummary(getString(R.string.weather_last_updated,
                DateFormat.getTimeFormat(this).format(new Date(snapshot.getFetchedAt()))));
        updated.setTextColor(heroSubTextColor(darkPalette));
        updated.setTextSize(12);
        addTopMargin(updated, dp(8));
        textColumn.addView(updated);

        FrameLayout heroScene = createHeroScene(snapshot, weatherNight);
        LinearLayout.LayoutParams sceneParams = new LinearLayout.LayoutParams(dp(132), dp(132));
        sceneParams.leftMargin = dp(14);
        header.addView(heroScene, sceneParams);
        return header;
    }

    private TextView createHeroPill(String text, boolean night) {
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextColor(heroTextColor(night));
        pill.setTextSize(14);
        pill.setTypeface(pill.getTypeface(), Typeface.BOLD);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setPadding(dp(12), dp(6), dp(12), dp(6));
        pill.setBackground(createRoundedDrawable(night ? 0x26FFFFFF : 0xDFFFFFFF, dp(8)));
        return pill;
    }

    private LinearLayout createTodayBand(WeatherSnapshot snapshot, boolean night,
                                         boolean weatherNight) {
        WeatherSnapshot.ForecastDay today = snapshot.getForecastDays().isEmpty()
                ? null
                : snapshot.getForecastDays().get(0);
        LinearLayout band = new LinearLayout(this);
        band.setOrientation(LinearLayout.HORIZONTAL);
        band.setGravity(Gravity.CENTER_VERTICAL);
        band.setPadding(dp(12), dp(10), dp(12), dp(10));
        band.setBackground(createRoundedDrawable(night ? 0x22FFFFFF : 0xD9FFFFFF, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(16);
        band.setLayoutParams(params);

        LottieAnimationView icon = createRealtimeWeatherIcon(snapshot, weatherNight);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.rightMargin = dp(8);
        band.addView(icon, iconParams);

        String summaryText = today == null
                ? snapshot.getCondition()
                : snapshot.getCondition() + " - " + getString(R.string.weather_precipitation,
                today.getPrecipitationProbability());
        TextView summary = createSummary(summaryText);
        summary.setTextColor(heroSubTextColor(night));
        summary.setTextSize(13);
        summary.setSingleLine(true);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        band.addView(summary, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        band.addView(createHighLowPill(today, snapshot, night));
        return band;
    }

    private LinearLayout createHighLowPill(WeatherSnapshot.ForecastDay today,
                                           WeatherSnapshot snapshot,
                                           boolean night) {
        double high = today == null ? snapshot.getTemperature() : today.getMaxTemperature();
        double low = today == null ? snapshot.getTemperature() : today.getMinTemperature();
        String suffix = WeatherSnapshot.unitSuffix(units);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(9), dp(5), dp(9), dp(5));
        pill.setBackground(createRoundedDrawable(night ? 0x24FFFFFF : 0xE6FFFFFF, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(8);
        pill.setLayoutParams(params);

        TextView highView = createSummary(String.format(Locale.US, "High %.0f%s", high, suffix));
        highView.setTextColor(heroSubTextColor(night));
        highView.setTextSize(11);
        highView.setSingleLine(true);
        highView.setIncludeFontPadding(false);
        pill.addView(highView);

        TextView lowView = createTitle(String.format(Locale.US, "Low %.0f%s", low, suffix));
        lowView.setTextColor(heroTextColor(night));
        lowView.setTextSize(12);
        lowView.setSingleLine(true);
        lowView.setIncludeFontPadding(false);
        pill.addView(lowView);
        return pill;
    }

    private void addForecastRow(LinearLayout card, WeatherSnapshot.ForecastDay day) {
        int index = card.getChildCount();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));
        if (index == 0) {
            row.setBackground(createRoundedDrawable(isAppDarkMode() ? 0xFF20343A : 0xFFEAF8FB,
                    dp(8)));
        }

        FrameLayout iconHost = new FrameLayout(this);
        iconHost.setBackground(createRoundedDrawable(isAppDarkMode() ? 0xFF24444D : 0xFFDFF8FE,
                dp(24)));
        ImageView icon = new ImageView(this);
        icon.setImageResource(WeatherIconMapper.iconForCode(day.getWeatherCode()));
        icon.setContentDescription(day.getCondition());
        iconHost.addView(icon, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        iconHost.setPadding(dp(7), dp(7), dp(7), dp(7));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.rightMargin = dp(14);
        row.addView(iconHost, iconParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView date = createTitle(formatForecastDate(day.getDate(), card.getChildCount()));
        date.setTextSize(index == 0 ? 18 : 17);
        textColumn.addView(date);
        TextView condition = createSummary(day.getCondition() + " - "
                + getString(R.string.weather_precipitation,
                day.getPrecipitationProbability()));
        condition.setSingleLine(true);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(condition);
        row.addView(textColumn);

        TextView range = createTitle(String.format(Locale.US, "%.0f/%.0f%s",
                day.getMaxTemperature(), day.getMinTemperature(),
                WeatherSnapshot.unitSuffix(units)));
        range.setTextSize(16);
        range.setGravity(Gravity.END);
        row.addView(range);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (index > 0) {
            rowParams.topMargin = dp(4);
        }
        card.addView(row, rowParams);
    }

    private void addHourlyConditions(WeatherSnapshot snapshot, boolean darkPalette) {
        if (snapshot.getHourlyConditions().isEmpty()) {
            return;
        }
        content.addView(createSection(R.string.weather_hourly_conditions));
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(darkPalette ? 0xFF172529 : 0xFFFFFFFF);
        card.setStrokeColor(darkPalette ? 0xFF2E4247 : 0xFFE0EAF1);
        card.setStrokeWidth(dp(1));

        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));
        scrollView.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (WeatherSnapshot.HourlyCondition hour : snapshot.getHourlyConditions()) {
            row.addView(createHourlyChip(hour, darkPalette));
        }

        card.addView(scrollView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(16));
        content.addView(card, params);
    }

    private LinearLayout createHourlyChip(WeatherSnapshot.HourlyCondition hour,
                                          boolean darkPalette) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(9), dp(8), dp(9));
        chip.setBackground(createRoundedDrawable(darkPalette ? 0xFF20343A : 0xFFEAF8FB,
                dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(74),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        chip.setLayoutParams(params);

        TextView label = createSummary(hour.getLabel());
        label.setGravity(Gravity.CENTER);
        label.setTextSize(12);
        label.setTextColor(darkPalette ? 0xFFB8C8CC : 0xFF5F6B7D);
        chip.addView(label);

        ImageView icon = new ImageView(this);
        icon.setImageResource(WeatherIconMapper.iconForCode(hour.getWeatherCode()));
        icon.setContentDescription(hour.getCondition());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        iconParams.topMargin = dp(6);
        chip.addView(icon, iconParams);

        TextView temp = createTitle(String.format(Locale.US, "%.0f%s",
                hour.getTemperature(), WeatherSnapshot.unitSuffix(units)));
        temp.setGravity(Gravity.CENTER);
        temp.setTextSize(14);
        addTopMargin(temp, dp(6));
        chip.addView(temp);
        return chip;
    }

    private void addAirQuality(WeatherSnapshot snapshot) {
        content.addView(createSection(R.string.weather_air_quality));
        LinearLayout card = addCard();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        row.addView(textColumn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = createTitle(getString(R.string.weather_us_aqi));
        title.setTextSize(17);
        textColumn.addView(title);

        TextView description = createSummary(snapshot.hasAirQuality()
                ? getString(R.string.weather_aqi_us_scale) + " - "
                + snapshot.getAirQualityDescription()
                : getString(R.string.weather_aqi_unavailable));
        addTopMargin(description, dp(3));
        textColumn.addView(description);

        TextView value = createTitle(snapshot.hasAirQuality()
                ? String.valueOf(snapshot.getAirQualityIndex())
                : "--");
        value.setTextSize(26);
        value.setGravity(Gravity.END);
        row.addView(value);
    }

    private FrameLayout createHeroScene(WeatherSnapshot snapshot, boolean night) {
        FrameLayout sceneHost = new FrameLayout(this);
        FrameLayout iconTile = new FrameLayout(this);
        iconTile.setBackground(createRoundedDrawable(0xFF86B9DB, dp(24)));
        iconTile.setElevation(dp(10));
        iconTile.setPadding(dp(10), dp(10), dp(10), dp(10));

        LottieAnimationView animation = createLoopingAnimation(
                WeatherAnimationMapper.animationFor(snapshot, night, units));
        iconTile.addView(animation, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sceneHost.addView(iconTile, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        return sceneHost;
    }

    private LottieAnimationView createLoopingAnimation(int animationRes) {
        LottieAnimationView animation = new LottieAnimationView(this);
        animation.setAnimation(animationRes);
        animation.setRepeatCount(LottieDrawable.INFINITE);
        animation.setRepeatMode(LottieDrawable.RESTART);
        animation.setSpeed(1f);
        animation.setScaleType(ImageView.ScaleType.FIT_CENTER);
        animation.playAnimation();
        return animation;
    }

    private LottieAnimationView createRealtimeWeatherIcon(WeatherSnapshot snapshot,
                                                          boolean weatherNight) {
        LottieAnimationView animation = createLoopingAnimation(
                WeatherAnimationMapper.animationFor(snapshot, weatherNight, units));
        animation.setContentDescription(snapshot.getCondition());
        return animation;
    }

    private boolean isNight(WeatherSnapshot snapshot) {
        return WeatherAnimationMapper.isNight(snapshot);
    }

    private LinearLayout createMetricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout createMetricChip(int iconRes, int labelRes, String value, boolean night) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(9), dp(8), dp(10));
        chip.setMinimumHeight(dp(78));
        chip.setBackground(createRoundedDrawable(night ? 0x26FFFFFF : 0xE6FFFFFF, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        chip.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(night ? 0xFFEAF4FF : 0xFF355A78);
        icon.setAlpha(0.92f);
        icon.setContentDescription(getString(labelRes));
        chip.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView label = createSummary(getString(labelRes));
        label.setGravity(Gravity.CENTER);
        label.setTextSize(11);
        label.setTextColor(night ? 0xCCEAF4FF : 0xFF667283);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(label, dp(2));
        chip.addView(label);

        TextView valueView = createTitle(value);
        valueView.setGravity(Gravity.CENTER);
        valueView.setTextSize(16);
        valueView.setTextColor(night ? 0xFFFFFFFF : 0xFF26324E);
        addTopMargin(valueView, dp(2));
        chip.addView(valueView);
        return chip;
    }

    private LinearLayout addHeroPanel(WeatherSnapshot snapshot, boolean night) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(8));
        card.setCardBackgroundColor(Color.TRANSPARENT);
        card.setStrokeWidth(0);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(16));
        box.setBackground(createWeatherGradient(snapshot, night));
        card.addView(box);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(18));
        content.addView(card, params);
        return box;
    }

    private GradientDrawable createWeatherGradient(WeatherSnapshot snapshot, boolean night) {
        int code = snapshot.getWeatherCode();
        int[] colors;
        if (night) {
            colors = new int[]{0xFF203A5E, 0xFF436A91};
        } else if (code >= 95 && code <= 99) {
            colors = new int[]{0xFFE6EFF8, 0xFFD7ECF7};
        } else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            colors = new int[]{0xFFE4F4FA, 0xFFF7FCFE};
        } else {
            colors = new int[]{0xFFEAF8FE, 0xFFFFF5E5};
        }
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private int heroTextColor(boolean night) {
        return night ? 0xFFFFFFFF : 0xFF26324E;
    }

    private int heroSubTextColor(boolean night) {
        return night ? 0xDDEAF4FF : 0xFF5F6B7D;
    }

    private GradientDrawable createVerticalGradient(int startColor, int endColor) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor});
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private String formatForecastDate(String rawDate, int index) {
        if (index == 0) {
            return "Today";
        }
        if (index == 1) {
            return "Tomorrow";
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat output = new SimpleDateFormat("EEE, MMM d", Locale.US);
            Date parsed = input.parse(rawDate);
            if (parsed != null) {
                return output.format(parsed);
            }
        } catch (Exception ignored) {
        }
        return rawDate;
    }

    private LinearLayout addCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(isAppDarkMode()
                ? 0xFF172529
                : ContextCompat.getColor(this, R.color.settings_card_background));
        card.setStrokeColor(isAppDarkMode() ? 0xFF2E4247 : 0xFFE0EAF1);
        card.setStrokeWidth(dp(1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(box);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(16));
        content.addView(card, params);
        return box;
    }

    private TextView addLine(LinearLayout card, String text) {
        TextView view = createSummary(text);
        addTopMargin(view, dp(4));
        card.addView(view);
        return view;
    }

    private TextView createSection(int titleRes) {
        return createSection(getString(titleRes));
    }

    private TextView createSection(String title) {
        TextView view = createSummary(title);
        view.setTextColor(isAppDarkMode() ? 0xFFE7F2F2 : 0xFF0F4C75);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setTextSize(18);
        view.setPadding(dp(2), 0, 0, dp(6));
        return view;
    }

    private TextView createTitle(String title) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextColor(isAppDarkMode() ? 0xFFE7F2F2 : 0xFF26324E);
        view.setTextSize(16);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView createSummary(String summary) {
        TextView view = new TextView(this);
        view.setText(summary);
        view.setTextColor(isAppDarkMode() ? 0xFFB8C8CC : 0xFF5F6B7D);
        view.setTextSize(14);
        return view;
    }

    private boolean isAppDarkMode() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String mode = prefs.getString(SettingsKeys.PREF_THEME_MODE, "system");
        if ("light".equals(mode)) {
            return false;
        }
        if ("dark".equals(mode)) {
            return true;
        }
        int systemMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return systemMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void addTopMargin(TextView view, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = margin;
        view.setLayoutParams(params);
    }

    private int getActionBarSize() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        return android.util.TypedValue.complexToDimensionPixelSize(
                typedValue.data, getResources().getDisplayMetrics());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
