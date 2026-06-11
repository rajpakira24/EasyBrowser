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
import android.widget.ImageButton;
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
import com.webstudio.easybrowser.utils.SystemBarUtils;
import com.webstudio.easybrowser.utils.ThemeEngine;
import com.webstudio.easybrowser.utils.WeatherAnimationMapper;
import com.webstudio.easybrowser.utils.WeatherIconMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {
    public static final String EXTRA_REQUEST_CURRENT_LOCATION = "request_current_location";

    private static final int EDGE_TOP_BAR = 0xFF071015;
    private static final int EDGE_BACKGROUND_TOP = 0xFF2D3944;
    private static final int EDGE_BACKGROUND_BOTTOM = 0xFF151E24;
    private static final int EDGE_SURFACE = 0xCC202A32;
    private static final int EDGE_SURFACE_STRONG = 0xEE24313A;
    private static final int EDGE_STROKE = 0x40596B76;
    private static final int EDGE_TEXT = 0xFFF4F7FA;
    private static final int EDGE_TEXT_MUTED = 0xFFB8C2CA;
    private static final int EDGE_BLUE = 0xFF1A5CC8;

    private WeatherRepository repository;
    private LinearLayout content;
    private String units;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new WeatherRepository(this);
        units = repository.getUnits();
        setupLocationPermissionLauncher();
        buildShell();
        WeatherSnapshot cached = repository.getCachedSnapshot();
        if (cached != null) {
            showWeather(cached, true);
        } else {
            showLoading();
        }
        boolean requestCurrentLocation =
                getIntent().getBooleanExtra(EXTRA_REQUEST_CURRENT_LOCATION, false);
        if (requestCurrentLocation && !repository.isUsingCurrentLocation()) {
            repository.saveCurrentLocationEnabled(true);
        }
        loadWeatherWithPermission(requestCurrentLocation);
    }

    private void buildShell() {
        SystemBarUtils.apply(this, EDGE_TOP_BAR, EDGE_BACKGROUND_BOTTOM, false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(createVerticalGradient(EDGE_BACKGROUND_TOP, EDGE_BACKGROUND_BOTTOM));
        setContentView(root);

        root.addView(createWeatherTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        NestedScrollView scrollView = new NestedScrollView(this);
        scrollView.setFillViewport(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(26));
        scrollView.addView(content, new NestedScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private LinearLayout createWeatherTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), 0, dp(10), 0);
        bar.setBackgroundColor(EDGE_TOP_BAR);

        ImageButton back = createTopBarButton(R.drawable.ic_arrow_back,
                getString(R.string.backward));
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        bar.addView(createUnitSwitcher(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));

        ImageButton refresh = createTopBarButton(R.drawable.ic_reload,
                getString(R.string.weather_refresh));
        refresh.setOnClickListener(v -> {
            showLoading();
            loadWeatherWithPermission(true);
        });
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        refreshParams.leftMargin = dp(4);
        bar.addView(refresh, refreshParams);
        return bar;
    }

    private ImageButton createTopBarButton(int iconRes, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setColorFilter(EDGE_TEXT);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setContentDescription(description);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        return button;
    }

    private LinearLayout createUnitSwitcher() {
        LinearLayout switcher = new LinearLayout(this);
        switcher.setOrientation(LinearLayout.HORIZONTAL);
        switcher.setGravity(Gravity.CENTER);
        switcher.setPadding(dp(3), dp(3), dp(3), dp(3));
        switcher.setBackground(createRoundedDrawable(0x222A3640, dp(14)));
        switcher.addView(createUnitChip(WeatherRepository.UNITS_FAHRENHEIT, "\u00B0F"));
        switcher.addView(createUnitChip(WeatherRepository.UNITS_CELSIUS, "\u00B0C"));
        return switcher;
    }

    private TextView createUnitChip(String value, String label) {
        TextView chip = new TextView(this);
        boolean selected = value.equals(units);
        chip.setText(label);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(14);
        chip.setTypeface(chip.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(selected ? EDGE_TEXT : EDGE_TEXT_MUTED);
        chip.setBackground(createRoundedDrawable(selected ? 0xFF2E3942 : Color.TRANSPARENT,
                dp(11)));
        chip.setOnClickListener(v -> setWeatherUnits(value));
        chip.setMinWidth(dp(42));
        return chip;
    }

    private void setWeatherUnits(String value) {
        if (value.equals(units)) {
            return;
        }
        units = value;
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(SettingsKeys.PREF_WEATHER_UNITS, value)
                .apply();
        showLoading();
        loadWeatherWithPermission(true);
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
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"));
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
        LinearLayout current = addHeroPanel(snapshot, weatherNight);
        current.addView(createHeroHeader(snapshot, true, weatherNight));
        current.addView(createTodayBand(snapshot, true, weatherNight));
        addEdgeMetricStrip(snapshot);
        addSunMoonPanel(snapshot);
        addHealthActivitiesPanel(snapshot);
        addHourlyConditions(snapshot, true);

        TextView forecastTitle = createSection(R.string.weather_daily_forecast);
        content.addView(forecastTitle);
        LinearLayout forecast = addCard();
        for (WeatherSnapshot.ForecastDay day : snapshot.getForecastDays()) {
            addForecastRow(forecast, day);
        }
    }

    private LinearLayout createHeroHeader(WeatherSnapshot snapshot, boolean darkPalette,
                                          boolean weatherNight) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);

        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setOrientation(LinearLayout.HORIZONTAL);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView location = createTitle(snapshot.getLocationName() + " \u25BE");
        location.setTextSize(22);
        location.setTextColor(EDGE_TEXT);
        location.setMaxLines(1);
        location.setEllipsize(TextUtils.TruncateAt.END);
        locationRow.addView(location, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView updated = createSummary(DateFormat.getTimeFormat(this)
                .format(new Date(snapshot.getFetchedAt())));
        updated.setTextColor(EDGE_TEXT_MUTED);
        updated.setTextSize(13);
        updated.setGravity(Gravity.END);
        locationRow.addView(updated, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(locationRow);

        TextView currentLabel = createSummary(getString(R.string.weather_current_weather));
        currentLabel.setTextColor(0xCCE8EEF2);
        currentLabel.setTextSize(13);
        currentLabel.setTypeface(currentLabel.getTypeface(), Typeface.BOLD);
        addTopMargin(currentLabel, dp(16));
        header.addView(currentLabel);

        LinearLayout weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(8);
        header.addView(weatherRow, rowParams);

        LottieAnimationView icon = createRealtimeWeatherIcon(snapshot, weatherNight);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        iconParams.rightMargin = dp(12);
        weatherRow.addView(icon, iconParams);

        TextView temperature = createTitle(snapshot.formatTemperature(units));
        temperature.setTextSize(62);
        temperature.setTextColor(EDGE_TEXT);
        temperature.setIncludeFontPadding(false);
        temperature.setSingleLine(true);
        weatherRow.addView(temperature, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout conditionColumn = new LinearLayout(this);
        conditionColumn.setOrientation(LinearLayout.VERTICAL);
        conditionColumn.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams conditionColumnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        conditionColumnParams.leftMargin = dp(14);
        weatherRow.addView(conditionColumn, conditionColumnParams);

        TextView condition = createTitle(snapshot.getCondition());
        condition.setTextSize(21);
        condition.setTextColor(EDGE_TEXT);
        condition.setMaxLines(2);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        conditionColumn.addView(condition);

        TextView feels = createSummary(getString(R.string.weather_feels_like,
                snapshot.formatFeelsLike(units)));
        feels.setTextColor(EDGE_TEXT_MUTED);
        feels.setTextSize(15);
        addTopMargin(feels, dp(4));
        conditionColumn.addView(feels);
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

    private void addEdgeMetricStrip(WeatherSnapshot snapshot) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));
        scrollView.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(createEdgeMetricChip(R.drawable.ic_weather_humidity,
                getString(R.string.weather_metric_air_quality),
                snapshot.hasAirQuality()
                        ? String.valueOf(snapshot.getAirQualityIndex())
                        : "--"));
        row.addView(createEdgeMetricChip(R.drawable.ic_weather_wind,
                getString(R.string.weather_metric_wind),
                String.format(Locale.US, "%.0f %s",
                        snapshot.getWindSpeed(),
                        WeatherRepository.UNITS_FAHRENHEIT.equals(units) ? "mph" : "km/h")));
        row.addView(createEdgeMetricChip(R.drawable.ic_weather_humidity,
                getString(R.string.weather_metric_humidity),
                snapshot.getHumidity() + "%"));
        row.addView(createEdgeMetricChip(R.drawable.ic_weather_temperature,
                getString(R.string.weather_metric_feels_like),
                snapshot.formatFeelsLike(units)));

        WeatherSnapshot.ForecastDay today = snapshot.getForecastDays().isEmpty()
                ? null
                : snapshot.getForecastDays().get(0);
        row.addView(createEdgeMetricChip(R.drawable.ic_weather_rain,
                getString(R.string.weather_metric_rain),
                today == null ? "--" : today.getPrecipitationProbability() + "%"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        content.addView(scrollView, params);
    }

    private LinearLayout createEdgeMetricChip(int iconRes, String label, String value) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), dp(10), dp(12), dp(10));
        chip.setMinimumHeight(dp(78));
        chip.setBackground(createRoundedStrokeDrawable(0x8826323B, EDGE_STROKE, dp(12), dp(1)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(122),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        chip.setLayoutParams(params);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFF7AC8E6);
        labelRow.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView labelView = createSummary(label + " \u203A");
        labelView.setTextColor(EDGE_TEXT_MUTED);
        labelView.setTextSize(12);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = dp(6);
        labelRow.addView(labelView, labelParams);
        chip.addView(labelRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView valueView = createTitle(value);
        valueView.setTextColor(EDGE_TEXT);
        valueView.setTextSize(20);
        valueView.setSingleLine(true);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(valueView, dp(8));
        chip.addView(valueView);
        return chip;
    }

    private void addSunMoonPanel(WeatherSnapshot snapshot) {
        LinearLayout box = addWeatherInfoCard(R.string.weather_sun_moon_title);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(createInsightTile(R.drawable.ic_weather_sunrise,
                getString(R.string.weather_metric_sunrise),
                snapshot.getSunrise().isEmpty() ? "--" : snapshot.getSunrise()));
        row.addView(createInsightTile(R.drawable.ic_weather_sunset,
                getString(R.string.weather_metric_sunset),
                snapshot.getSunset().isEmpty() ? "--" : snapshot.getSunset()));
        box.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addInsightRow(box, R.drawable.ic_weather_sunny,
                getString(R.string.weather_daylight),
                getDaylightSummary(snapshot));
        addInsightRow(box, R.drawable.ic_weather_partly_cloudy,
                getString(R.string.weather_moon),
                getMoonSummary(snapshot));
    }

    private void addHealthActivitiesPanel(WeatherSnapshot snapshot) {
        WeatherSnapshot.ForecastDay today = snapshot.getForecastDays().isEmpty()
                ? null
                : snapshot.getForecastDays().get(0);
        int rainChance = today == null ? 0 : today.getPrecipitationProbability();

        LinearLayout box = addWeatherInfoCard(R.string.weather_health_activities_title);
        addInsightRow(box, R.drawable.ic_weather_partly_cloudy,
                getString(R.string.weather_outdoor_activity),
                getOutdoorActivitySummary(snapshot, rainChance));
        addInsightRow(box, R.drawable.ic_weather_humidity,
                getString(R.string.weather_metric_air_quality),
                snapshot.hasAirQuality()
                        ? snapshot.getAirQualityIndex() + " - " + snapshot.getAirQualityDescription()
                        : getString(R.string.weather_aqi_unavailable));
        addInsightRow(box, R.drawable.ic_weather_rain,
                getString(R.string.weather_metric_rain),
                getString(R.string.weather_precipitation, rainChance));
        addInsightRow(box, R.drawable.ic_weather_humidity,
                getString(R.string.weather_hydration),
                getHydrationSummary(snapshot));
    }

    private LinearLayout addWeatherInfoCard(int titleRes) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(EDGE_SURFACE);
        card.setStrokeColor(EDGE_STROKE);
        card.setStrokeWidth(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(box);

        TextView title = createTitle(getString(titleRes));
        title.setTextSize(19);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(12);
        box.addView(title, titleParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(16));
        content.addView(card, params);
        return box;
    }

    private LinearLayout createInsightTile(int iconRes, String label, String value) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(12), dp(12), dp(12), dp(12));
        tile.setBackground(createRoundedDrawable(0xFF253640, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        tile.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFF8AD2F0);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView valueView = createTitle(value);
        valueView.setTextSize(20);
        valueView.setGravity(Gravity.CENTER);
        valueView.setSingleLine(true);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(valueView, dp(8));
        tile.addView(valueView);

        TextView labelView = createSummary(label);
        labelView.setTextSize(12);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(labelView, dp(2));
        tile.addView(labelView);
        return tile;
    }

    private void addInsightRow(LinearLayout box, int iconRes, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(10), dp(2), dp(2));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFF8AD2F0);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.rightMargin = dp(10);
        row.addView(icon, iconParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        row.addView(textColumn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView labelView = createTitle(label);
        labelView.setTextSize(15);
        textColumn.addView(labelView);

        TextView valueView = createSummary(value);
        valueView.setTextSize(13);
        valueView.setMaxLines(2);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(valueView, dp(2));
        textColumn.addView(valueView);
        box.addView(row);
    }

    private String getDaylightSummary(WeatherSnapshot snapshot) {
        String sunrise = snapshot.getSunrise().isEmpty() ? "--" : snapshot.getSunrise();
        String sunset = snapshot.getSunset().isEmpty() ? "--" : snapshot.getSunset();
        return getString(R.string.weather_daylight_summary, sunrise, sunset);
    }

    private String getMoonSummary(WeatherSnapshot snapshot) {
        if (snapshot.getWeatherCode() >= 51 || snapshot.getHumidity() >= 90) {
            return getString(R.string.weather_moon_cloudy);
        }
        return getString(R.string.weather_moon_clear);
    }

    private String getOutdoorActivitySummary(WeatherSnapshot snapshot, int rainChance) {
        if (snapshot.getWeatherCode() >= 95 || rainChance >= 70) {
            return getString(R.string.weather_activity_indoor);
        }
        if (snapshot.hasAirQuality() && snapshot.getAirQualityIndex() >= 150) {
            return getString(R.string.weather_activity_limited);
        }
        if (snapshot.getHumidity() >= 85) {
            return getString(R.string.weather_activity_humid);
        }
        return getString(R.string.weather_activity_good);
    }

    private String getHydrationSummary(WeatherSnapshot snapshot) {
        double highFeelsLike = WeatherRepository.UNITS_FAHRENHEIT.equals(units) ? 90d : 32d;
        if (snapshot.getHumidity() >= 85 || snapshot.getFeelsLike() >= highFeelsLike) {
            return getString(R.string.weather_hydration_high);
        }
        return getString(R.string.weather_hydration_normal);
    }

    private void addForecastRow(LinearLayout card, WeatherSnapshot.ForecastDay day) {
        int index = card.getChildCount();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));
        if (index == 0) {
            row.setBackground(createRoundedDrawable(0xAA2B3A44, dp(8)));
        }

        FrameLayout iconHost = new FrameLayout(this);
        iconHost.setBackground(createRoundedDrawable(0xFF274653, dp(24)));
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
        content.addView(createSection(R.string.weather_today_forecast));
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(EDGE_SURFACE);
        card.setStrokeColor(EDGE_STROKE);
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
        chip.setBackground(createRoundedDrawable(0xFF253640, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(74),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        chip.setLayoutParams(params);

        TextView label = createSummary(hour.getLabel());
        label.setGravity(Gravity.CENTER);
        label.setTextSize(12);
        label.setTextColor(EDGE_TEXT_MUTED);
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
        card.setCardElevation(dp(6));
        card.setCardBackgroundColor(Color.TRANSPARENT);
        card.setStrokeWidth(0);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(14));
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
            colors = new int[]{0xFF1D2E3A, 0xFF344B5F};
        } else if (code >= 95 && code <= 99) {
            colors = new int[]{0xFF26303A, 0xFF43505C};
        } else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            colors = new int[]{0xFF263743, 0xFF3E5663};
        } else {
            colors = new int[]{0xFF253746, 0xFF41556B};
        }
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private int heroTextColor(boolean night) {
        return EDGE_TEXT;
    }

    private int heroSubTextColor(boolean night) {
        return EDGE_TEXT_MUTED;
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

    private GradientDrawable createRoundedStrokeDrawable(int color, int strokeColor,
                                                         int radius, int strokeWidth) {
        GradientDrawable drawable = createRoundedDrawable(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
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
        card.setCardBackgroundColor(EDGE_SURFACE);
        card.setStrokeColor(EDGE_STROKE);
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
        view.setTextColor(EDGE_TEXT);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setTextSize(17);
        view.setPadding(dp(2), 0, 0, dp(6));
        return view;
    }

    private TextView createTitle(String title) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextColor(EDGE_TEXT);
        view.setTextSize(16);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView createSummary(String summary) {
        TextView view = new TextView(this);
        view.setText(summary);
        view.setTextColor(EDGE_TEXT_MUTED);
        view.setTextSize(14);
        return view;
    }

    private boolean isAppDarkMode() {
        return true;
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
