package com.webstudio.easybrowser.repository;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.BuildConfig;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.WeatherSnapshot;
import com.webstudio.easybrowser.utils.SettingsKeys;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherRepository {
    public interface WeatherCallback {
        void onWeatherLoaded(WeatherSnapshot snapshot, boolean fromCache);
        void onWeatherError(Exception error, WeatherSnapshot cachedSnapshot);
    }

    public static final String DEFAULT_LOCATION_NAME = "Jangipur";
    public static final String DEFAULT_LATITUDE = "24.4700";
    public static final String DEFAULT_LONGITUDE = "88.0700";
    public static final String UNITS_CELSIUS = "celsius";
    public static final String UNITS_FAHRENHEIT = "fahrenheit";

    private static final String KEY_CACHE_JSON = "weather_cache_json_met_no_v1";
    private static final String MET_LOCATIONFORECAST_URL =
            "https://api.met.no/weatherapi/locationforecast/2.0/complete";
    private static final String OPEN_METEO_FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast";
    private static final String OPEN_METEO_AIR_QUALITY_URL =
            "https://air-quality-api.open-meteo.com/v1/air-quality";
    private static final String KEY_LAST_DEVICE_LOCATION_NAME =
            "weather_last_device_location_name";
    private static final String KEY_LAST_DEVICE_LATITUDE = "weather_last_device_latitude";
    private static final String KEY_LAST_DEVICE_LONGITUDE = "weather_last_device_longitude";
    private static final long CACHE_TTL_MILLIS = 15L * 60L * 1000L;
    private static final long LOCATION_FRESHNESS_MILLIS = 5L * 60L * 1000L;
    private static final long LOCATION_MAX_AGE_MILLIS = 30L * 60L * 1000L;
    private static final long LOCATION_TIMEOUT_MILLIS = 10000L;
    private static final float CACHE_LOCATION_TOLERANCE_METERS = 1000f;
    private static final float GOOD_LOCATION_ACCURACY_METERS = 200f;
    private static final float ACCEPTABLE_LOCATION_ACCURACY_METERS = 1000f;
    private static final float SIGNIFICANT_ACCURACY_DELTA_METERS = 100f;
    private static final float MAX_REVERSE_GEOCODE_NAME_DISTANCE_METERS = 2500f;
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ExecutorService LOCATION_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final Context appContext;
    private final SharedPreferences prefs;

    public WeatherRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public void getWeather(boolean forceRefresh, WeatherCallback callback) {
        WeatherSnapshot cached = getCachedSnapshot();
        resolveWeatherLocation(location -> {
            if (!forceRefresh && cached != null && isFreshForLocation(cached, location)) {
                callback.onWeatherLoaded(cached, true);
                return;
            }

            Request request = new Request.Builder()
                    .url(buildForecastUrl(location))
                    .header("Accept", "application/json")
                    .header("User-Agent", weatherUserAgent())
                    .get()
                    .build();
            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onWeatherError(e, cached);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (Response closeable = response) {
                        if (!closeable.isSuccessful() || closeable.body() == null) {
                            callback.onWeatherError(new IOException("Weather request failed: "
                                    + closeable.code()), cached);
                            return;
                        }
                        String body = closeable.body().string();
                        WeatherSnapshot snapshot = parseForecast(body, location);
                        loadCurrentForecast(location, snapshot, callback);
                    } catch (Exception e) {
                        callback.onWeatherError(e, cached);
                    }
                }
            });
        });
    }

    public WeatherSnapshot getCachedSnapshot() {
        String raw = prefs.getString(KEY_CACHE_JSON, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return WeatherSnapshot.fromJson(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void saveManualLocation(String name, String latitude, String longitude) {
        prefs.edit()
                .putBoolean(SettingsKeys.PREF_WEATHER_USE_CURRENT_LOCATION, false)
                .putString(SettingsKeys.PREF_WEATHER_LOCATION_NAME,
                        safeLocationName(name))
                .putString(SettingsKeys.PREF_WEATHER_LATITUDE,
                        safeLatitude(latitude))
                .putString(SettingsKeys.PREF_WEATHER_LONGITUDE,
                        safeLongitude(longitude))
                .remove(KEY_CACHE_JSON)
                .apply();
    }

    public void saveCurrentLocationEnabled(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(SettingsKeys.PREF_WEATHER_USE_CURRENT_LOCATION, enabled)
                .remove(KEY_CACHE_JSON);
        if (enabled) {
            editor.putBoolean(SettingsKeys.PREF_WEATHER_LOCATION_PERMISSION_REQUESTED, false)
                    .putBoolean(SettingsKeys.PREF_WEATHER_PRECISE_LOCATION_PERMISSION_REQUESTED,
                            false);
        }
        editor.apply();
    }

    public boolean isUsingCurrentLocation() {
        return prefs.getBoolean(SettingsKeys.PREF_WEATHER_USE_CURRENT_LOCATION, false);
    }

    public boolean shouldRequestCurrentLocationPermission() {
        return isUsingCurrentLocation() && !hasPreciseLocationPermission(appContext);
    }

    public void markLocationPermissionRequested() {
        prefs.edit()
                .putBoolean(SettingsKeys.PREF_WEATHER_LOCATION_PERMISSION_REQUESTED, true)
                .apply();
    }

    public String getLocationSummary() {
        if (isUsingCurrentLocation()) {
            String name = prefs.getString(KEY_LAST_DEVICE_LOCATION_NAME, "");
            String latitude = prefs.getString(KEY_LAST_DEVICE_LATITUDE, "");
            String longitude = prefs.getString(KEY_LAST_DEVICE_LONGITUDE, "");
            if (name != null && !name.trim().isEmpty()
                    && latitude != null && !latitude.trim().isEmpty()
                    && longitude != null && !longitude.trim().isEmpty()) {
                return name + " (" + latitude + ", " + longitude + ")";
            }
            return appContext.getString(R.string.weather_device_location);
        }
        return getLocationName() + " ("
                + getLatitudeString() + ", " + getLongitudeString() + ")";
    }

    private void cacheSnapshot(WeatherSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        try {
            prefs.edit().putString(KEY_CACHE_JSON, snapshot.toJson().toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void resolveWeatherLocation(WeatherLocationCallback callback) {
        if (!isUsingCurrentLocation() || !hasPreciseLocationPermission(appContext)) {
            callback.onLocationReady(manualWeatherLocation());
            return;
        }

        resolveCurrentLocation(callback);
    }

    private void resolveCurrentLocation(WeatherLocationCallback callback) {
        LocationManager locationManager =
                (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onLocationReady(manualWeatherLocation());
            return;
        }

        Location lastKnown = getBestLastKnownLocation(locationManager);
        if (isFreshGoodLocation(lastKnown)) {
            deliverDeviceLocation(lastKnown, callback);
            return;
        }

        List<String> providers = enabledLocationProviders(locationManager);
        if (providers.isEmpty()) {
            deliverLocationOrManual(lastKnown, callback);
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean delivered = new AtomicBoolean(false);
        Location[] bestLocation = new Location[]{lastKnown};
        LocationListener[] listenerRef = new LocationListener[1];
        Runnable timeout = () -> {
            if (delivered.compareAndSet(false, true)) {
                safelyRemoveUpdates(locationManager, listenerRef[0]);
                deliverLocationOrManual(bestLocation[0], callback);
            }
        };
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                bestLocation[0] = betterLocation(bestLocation[0], location);
                if (isGoodLocation(bestLocation[0]) && delivered.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout);
                    safelyRemoveUpdates(locationManager, this);
                    deliverLocationOrManual(bestLocation[0], callback);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        listenerRef[0] = listener;

        boolean requested = false;
        for (String provider : providers) {
            try {
                locationManager.requestLocationUpdates(provider, 0L, 0f,
                        listener, Looper.getMainLooper());
                requested = true;
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        if (requested) {
            handler.postDelayed(timeout, LOCATION_TIMEOUT_MILLIS);
        } else {
            deliverLocationOrManual(lastKnown, callback);
        }
    }

    private void deliverLocationOrManual(Location location, WeatherLocationCallback callback) {
        if (!isUsableLocation(location)) {
            callback.onLocationReady(manualWeatherLocation());
            return;
        }
        deliverDeviceLocation(location, callback);
    }

    private void deliverDeviceLocation(Location location, WeatherLocationCallback callback) {
        LOCATION_EXECUTOR.execute(() -> callback.onLocationReady(deviceWeatherLocation(location)));
    }

    private WeatherLocation deviceWeatherLocation(Location location) {
        String latitude = formatCoordinate(location.getLatitude());
        String longitude = formatCoordinate(location.getLongitude());
        String name = resolveLocationName(location.getLatitude(), location.getLongitude());
        prefs.edit()
                .putString(KEY_LAST_DEVICE_LOCATION_NAME, name)
                .putString(KEY_LAST_DEVICE_LATITUDE, latitude)
                .putString(KEY_LAST_DEVICE_LONGITUDE, longitude)
                .apply();
        return new WeatherLocation(name, latitude, longitude,
                location.getLatitude(), location.getLongitude());
    }

    private WeatherLocation manualWeatherLocation() {
        String latitude = getLatitudeString();
        String longitude = getLongitudeString();
        return new WeatherLocation(getLocationName(), latitude, longitude,
                parseDouble(latitude, 0), parseDouble(longitude, 0));
    }

    private String buildForecastUrl(WeatherLocation location) {
        return Uri.parse(MET_LOCATIONFORECAST_URL).buildUpon()
                .appendQueryParameter("lat", location.latitudeString)
                .appendQueryParameter("lon", location.longitudeString)
                .build()
                .toString();
    }

    private String buildCurrentForecastUrl(WeatherLocation location) {
        return Uri.parse(OPEN_METEO_FORECAST_URL).buildUpon()
                .appendQueryParameter("latitude", location.latitudeString)
                .appendQueryParameter("longitude", location.longitudeString)
                .appendQueryParameter("current", "temperature_2m,relative_humidity_2m,"
                        + "apparent_temperature,precipitation,rain,showers,snowfall,"
                        + "weather_code,cloud_cover,wind_speed_10m")
                .appendQueryParameter("hourly", "temperature_2m,weather_code,"
                        + "precipitation_probability,precipitation,rain,showers,snowfall,"
                        + "cloud_cover")
                .appendQueryParameter("daily", "weather_code,temperature_2m_max,"
                        + "temperature_2m_min,precipitation_probability_max")
                .appendQueryParameter("timezone", "auto")
                .appendQueryParameter("forecast_days", "7")
                .appendQueryParameter("forecast_hours", "12")
                .appendQueryParameter("wind_speed_unit", "kmh")
                .build()
                .toString();
    }

    private String weatherUserAgent() {
        return "EasyBrowser/" + BuildConfig.VERSION_NAME + " ("
                + BuildConfig.APPLICATION_ID + "; Android weather client)";
    }

    private String buildAirQualityUrl(WeatherLocation location) {
        return Uri.parse(OPEN_METEO_AIR_QUALITY_URL).buildUpon()
                .appendQueryParameter("latitude", location.latitudeString)
                .appendQueryParameter("longitude", location.longitudeString)
                .appendQueryParameter("current", "us_aqi,pm2_5,pm10")
                .appendQueryParameter("timezone", "auto")
                .build()
                .toString();
    }

    private void loadCurrentForecast(WeatherLocation location, WeatherSnapshot snapshot,
                                     WeatherCallback callback) {
        Request request = new Request.Builder()
                .url(buildCurrentForecastUrl(location))
                .header("Accept", "application/json")
                .get()
                .build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                loadAirQuality(location, snapshot, callback);
            }

            @Override
            public void onResponse(Call call, Response response) {
                WeatherSnapshot result = snapshot;
                try (Response closeable = response) {
                    if (closeable.isSuccessful() && closeable.body() != null) {
                        result = applyCurrentForecast(snapshot, closeable.body().string());
                    }
                } catch (Exception ignored) {
                }
                loadAirQuality(location, result, callback);
            }
        });
    }

    private void loadAirQuality(WeatherLocation location, WeatherSnapshot snapshot,
                                WeatherCallback callback) {
        Request request = new Request.Builder()
                .url(buildAirQualityUrl(location))
                .header("Accept", "application/json")
                .get()
                .build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                cacheSnapshot(snapshot);
                callback.onWeatherLoaded(snapshot, false);
            }

            @Override
            public void onResponse(Call call, Response response) {
                WeatherSnapshot result = snapshot;
                try (Response closeable = response) {
                    if (closeable.isSuccessful() && closeable.body() != null) {
                        result = applyAirQuality(snapshot, closeable.body().string());
                    }
                } catch (Exception ignored) {
                }
                cacheSnapshot(result);
                callback.onWeatherLoaded(result, false);
            }
        });
    }

    private WeatherSnapshot parseForecast(String raw, WeatherLocation location) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject properties = root.getJSONObject("properties");
        JSONArray timeseries = properties.getJSONArray("timeseries");
        if (timeseries.length() == 0) {
            throw new IOException("MET weather response contained no forecast data");
        }
        JSONObject currentEntry = firstForecastEntry(timeseries);
        JSONObject instant = instantDetails(currentEntry);
        if (instant == null) {
            throw new IOException("MET weather response had no current details");
        }
        String currentSymbol = symbolCode(currentEntry);
        int code = weatherCodeForSymbol(currentSymbol);
        double temperature = instant.optDouble("air_temperature", 0);
        String units = getUnits();
        double displayTemperature = displayTemperature(temperature, units);
        double windSpeed = displayWindSpeed(instant.optDouble("wind_speed", 0), units);
        Date currentDate = parseMetTime(currentEntry.optString("time", ""));
        String sunrise = approximateSunTime(location.latitude, location.longitude, currentDate,
                true);
        String sunset = approximateSunTime(location.latitude, location.longitude, currentDate,
                false);
        return new WeatherSnapshot(
                location.name,
                location.latitude,
                location.longitude,
                displayTemperature,
                displayTemperature,
                instant.optInt("relative_humidity", 0),
                windSpeed,
                code,
                conditionForSymbol(currentSymbol),
                formatIsoTime(sunrise),
                formatIsoTime(sunset),
                System.currentTimeMillis(),
                parseDailyForecast(timeseries, units),
                parseHourlyConditions(timeseries, units),
                -1,
                "");
    }

    private List<WeatherSnapshot.ForecastDay> parseDailyForecast(JSONArray timeseries,
                                                                 String units) {
        List<DailyForecastAccumulator> accumulators = new ArrayList<>();
        for (int i = 0; i < timeseries.length(); i++) {
            JSONObject entry = timeseries.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            Date date = parseMetTime(entry.optString("time", ""));
            String dayKey = localDateKey(date);
            if (dayKey.isEmpty()) {
                continue;
            }
            DailyForecastAccumulator accumulator = findAccumulator(accumulators, dayKey);
            if (accumulator == null) {
                if (accumulators.size() >= 7) {
                    continue;
                }
                accumulator = new DailyForecastAccumulator(dayKey);
                accumulators.add(accumulator);
            }
            JSONObject details = instantDetails(entry);
            if (details != null) {
                double temp = displayTemperature(details.optDouble("air_temperature", 0), units);
                accumulator.minTemperature = Math.min(accumulator.minTemperature, temp);
                accumulator.maxTemperature = Math.max(accumulator.maxTemperature, temp);
            }
            String symbol = symbolCode(entry);
            int code = weatherCodeForSymbol(symbol);
            int severity = severityForCode(code);
            if (severity > accumulator.severity) {
                accumulator.severity = severity;
                accumulator.weatherCode = code;
                accumulator.condition = conditionForSymbol(symbol);
            }
            accumulator.precipitationProbability = Math.max(accumulator.precipitationProbability,
                    precipitationProbability(entry));
        }
        List<WeatherSnapshot.ForecastDay> days = new ArrayList<>();
        for (DailyForecastAccumulator accumulator : accumulators) {
            if (accumulator.minTemperature == Double.MAX_VALUE
                    || accumulator.maxTemperature == -Double.MAX_VALUE) {
                continue;
            }
            days.add(new WeatherSnapshot.ForecastDay(
                    accumulator.date,
                    accumulator.weatherCode,
                    accumulator.condition,
                    accumulator.minTemperature,
                    accumulator.maxTemperature,
                    accumulator.precipitationProbability));
        }
        return days;
    }

    private List<WeatherSnapshot.HourlyCondition> parseHourlyConditions(JSONArray timeseries,
                                                                        String units) {
        List<WeatherSnapshot.HourlyCondition> hours = new ArrayList<>();
        for (int i = 0; i < timeseries.length() && hours.size() < 12; i++) {
            JSONObject entry = timeseries.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            JSONObject details = instantDetails(entry);
            if (details == null) {
                continue;
            }
            String symbol = symbolCode(entry);
            int code = weatherCodeForSymbol(symbol);
            hours.add(new WeatherSnapshot.HourlyCondition(
                    hours.isEmpty() ? "Now" : localHourLabel(parseMetTime(entry.optString("time", ""))),
                    code,
                    conditionForSymbol(symbol),
                    displayTemperature(details.optDouble("air_temperature", 0), units)));
        }
        return hours;
    }

    private WeatherSnapshot applyCurrentForecast(WeatherSnapshot snapshot, String raw)
            throws Exception {
        JSONObject root = new JSONObject(raw);
        String units = getUnits();
        List<WeatherSnapshot.ForecastDay> days =
                parseOpenMeteoDaily(root, snapshot.getForecastDays(), units);
        List<WeatherSnapshot.HourlyCondition> hours =
                parseOpenMeteoHourly(root, snapshot.getHourlyConditions(), units);

        JSONObject current = root.optJSONObject("current");
        if (current == null) {
            return new WeatherSnapshot(
                    snapshot.getLocationName(),
                    snapshot.getLatitude(),
                    snapshot.getLongitude(),
                    snapshot.getTemperature(),
                    snapshot.getFeelsLike(),
                    snapshot.getHumidity(),
                    snapshot.getWindSpeed(),
                    snapshot.getWeatherCode(),
                    snapshot.getCondition(),
                    snapshot.getSunrise(),
                    snapshot.getSunset(),
                    System.currentTimeMillis(),
                    days,
                    hours,
                    snapshot.getAirQualityIndex(),
                    snapshot.getAirQualityDescription());
        }

        int providerCode = current.has("weather_code")
                ? current.optInt("weather_code", snapshot.getWeatherCode())
                : snapshot.getWeatherCode();
        int currentCode = currentSkyCode(current, providerCode, days);
        double temperature = hasNumber(current, "temperature_2m")
                ? displayTemperature(current.optDouble("temperature_2m"), units)
                : snapshot.getTemperature();
        double feelsLike = hasNumber(current, "apparent_temperature")
                ? displayTemperature(current.optDouble("apparent_temperature"), units)
                : snapshot.getFeelsLike();
        int humidity = hasNumber(current, "relative_humidity_2m")
                ? clampPercent(current.optDouble("relative_humidity_2m"))
                : snapshot.getHumidity();
        double windSpeed = hasNumber(current, "wind_speed_10m")
                ? displayOpenMeteoWindSpeed(current.optDouble("wind_speed_10m"), units)
                : snapshot.getWindSpeed();

        days = withCurrentConditionForToday(days, currentCode, measuredPrecipitation(current));
        hours = withCurrentHour(hours, currentCode, conditionForCode(currentCode), temperature);

        return new WeatherSnapshot(
                snapshot.getLocationName(),
                snapshot.getLatitude(),
                snapshot.getLongitude(),
                temperature,
                feelsLike,
                humidity,
                windSpeed,
                currentCode,
                conditionForCode(currentCode),
                snapshot.getSunrise(),
                snapshot.getSunset(),
                System.currentTimeMillis(),
                days,
                hours,
                snapshot.getAirQualityIndex(),
                snapshot.getAirQualityDescription());
    }

    private List<WeatherSnapshot.ForecastDay> parseOpenMeteoDaily(
            JSONObject root, List<WeatherSnapshot.ForecastDay> fallback, String units) {
        JSONObject daily = root.optJSONObject("daily");
        JSONArray dates = daily == null ? null : daily.optJSONArray("time");
        if (dates == null || dates.length() == 0) {
            return fallback;
        }
        JSONArray codes = daily.optJSONArray("weather_code");
        JSONArray maxTemps = daily.optJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.optJSONArray("temperature_2m_min");
        JSONArray probabilities = daily.optJSONArray("precipitation_probability_max");
        List<WeatherSnapshot.ForecastDay> days = new ArrayList<>();
        for (int i = 0; i < dates.length() && days.size() < 7; i++) {
            WeatherSnapshot.ForecastDay fallbackDay =
                    fallback != null && i < fallback.size() ? fallback.get(i) : null;
            String date = dates.optString(i, fallbackDay == null ? "" : fallbackDay.getDate());
            int code = optInt(codes, i, fallbackDay == null ? 0 : fallbackDay.getWeatherCode());
            double maxTemp = fallbackDay == null ? 0 : fallbackDay.getMaxTemperature();
            if (hasArrayNumber(maxTemps, i)) {
                maxTemp = displayTemperature(maxTemps.optDouble(i), units);
            }
            double minTemp = fallbackDay == null ? maxTemp : fallbackDay.getMinTemperature();
            if (hasArrayNumber(minTemps, i)) {
                minTemp = displayTemperature(minTemps.optDouble(i), units);
            }
            int probability = clampPercent(optDouble(probabilities, i,
                    fallbackDay == null ? 0 : fallbackDay.getPrecipitationProbability()));
            days.add(new WeatherSnapshot.ForecastDay(
                    date,
                    code,
                    conditionForCode(code),
                    minTemp,
                    maxTemp,
                    probability));
        }
        return days.isEmpty() ? fallback : days;
    }

    private List<WeatherSnapshot.HourlyCondition> parseOpenMeteoHourly(
            JSONObject root, List<WeatherSnapshot.HourlyCondition> fallback, String units) {
        JSONObject hourly = root.optJSONObject("hourly");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
        if (times == null || times.length() == 0) {
            return fallback;
        }
        JSONArray codes = hourly.optJSONArray("weather_code");
        JSONArray temperatures = hourly.optJSONArray("temperature_2m");
        JSONArray precipitation = hourly.optJSONArray("precipitation");
        JSONArray rain = hourly.optJSONArray("rain");
        JSONArray showers = hourly.optJSONArray("showers");
        JSONArray snowfall = hourly.optJSONArray("snowfall");

        List<WeatherSnapshot.HourlyCondition> hours = new ArrayList<>();
        for (int i = 0; i < times.length() && hours.size() < 12; i++) {
            WeatherSnapshot.HourlyCondition fallbackHour =
                    fallback != null && i < fallback.size() ? fallback.get(i) : null;
            int providerCode = optInt(codes, i,
                    fallbackHour == null ? 0 : fallbackHour.getWeatherCode());
            int code = weatherCodeForMeasuredPrecipitation(
                    optDouble(precipitation, i, Double.NaN),
                    optDouble(rain, i, Double.NaN),
                    optDouble(showers, i, Double.NaN),
                    optDouble(snowfall, i, Double.NaN),
                    providerCode);
            double temperature = fallbackHour == null ? 0 : fallbackHour.getTemperature();
            if (hasArrayNumber(temperatures, i)) {
                temperature = displayTemperature(temperatures.optDouble(i), units);
            }
            String label = hours.isEmpty()
                    ? "Now"
                    : localHourLabel(parseOpenMeteoTime(times.optString(i, "")));
            hours.add(new WeatherSnapshot.HourlyCondition(
                    label,
                    code,
                    conditionForCode(code),
                    temperature));
        }
        return hours.isEmpty() ? fallback : hours;
    }

    private List<WeatherSnapshot.ForecastDay> withCurrentConditionForToday(
            List<WeatherSnapshot.ForecastDay> days, int currentCode,
            double currentPrecipitation) {
        if (days == null || days.isEmpty()) {
            return days;
        }
        List<WeatherSnapshot.ForecastDay> updated = new ArrayList<>(days);
        WeatherSnapshot.ForecastDay today = updated.get(0);
        int code = today.getWeatherCode();
        if (isWetCode(currentCode) && !isWetCode(code)) {
            code = currentCode;
        } else if (severityForCode(currentCode) > severityForCode(code)) {
            code = currentCode;
        }
        int probability = today.getPrecipitationProbability();
        if (currentPrecipitation > 0.01d || isWetCode(currentCode)) {
            probability = Math.max(probability, 70);
        }
        updated.set(0, new WeatherSnapshot.ForecastDay(
                today.getDate(),
                code,
                conditionForCode(code),
                today.getMinTemperature(),
                today.getMaxTemperature(),
                probability));
        return updated;
    }

    private List<WeatherSnapshot.HourlyCondition> withCurrentHour(
            List<WeatherSnapshot.HourlyCondition> hours, int code, String condition,
            double temperature) {
        List<WeatherSnapshot.HourlyCondition> updated =
                hours == null ? new ArrayList<>() : new ArrayList<>(hours);
        WeatherSnapshot.HourlyCondition current =
                new WeatherSnapshot.HourlyCondition("Now", code, condition, temperature);
        if (updated.isEmpty()) {
            updated.add(current);
        } else {
            updated.set(0, current);
        }
        return updated;
    }

    private WeatherSnapshot applyAirQuality(WeatherSnapshot snapshot, String raw)
            throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject current = root.optJSONObject("current");
        if (current == null) {
            return snapshot;
        }
        int aqi = current.optInt("us_aqi", -1);
        if (aqi < 0) {
            return snapshot;
        }
        return snapshot.withAirQuality(aqi, airQualityDescription(aqi));
    }

    private static JSONObject instantDetails(JSONObject entry) {
        JSONObject data = entry == null ? null : entry.optJSONObject("data");
        JSONObject instant = data == null ? null : data.optJSONObject("instant");
        return instant == null ? null : instant.optJSONObject("details");
    }

    private static JSONObject nextDetails(JSONObject entry) {
        JSONObject data = entry == null ? null : entry.optJSONObject("data");
        if (data == null) {
            return null;
        }
        String[] keys = {"next_1_hours", "next_6_hours", "next_12_hours"};
        for (String key : keys) {
            JSONObject next = data.optJSONObject(key);
            JSONObject details = next == null ? null : next.optJSONObject("details");
            if (details != null) {
                return details;
            }
        }
        return null;
    }

    private static JSONObject firstForecastEntry(JSONArray timeseries) {
        long now = System.currentTimeMillis();
        JSONObject fallback = timeseries.optJSONObject(0);
        for (int i = 0; i < timeseries.length(); i++) {
            JSONObject entry = timeseries.optJSONObject(i);
            Date date = parseMetTime(entry == null ? "" : entry.optString("time", ""));
            if (date.getTime() >= now - 30L * 60L * 1000L) {
                return entry;
            }
        }
        return fallback;
    }

    private static DailyForecastAccumulator findAccumulator(
            List<DailyForecastAccumulator> accumulators, String date) {
        for (DailyForecastAccumulator accumulator : accumulators) {
            if (accumulator.date.equals(date)) {
                return accumulator;
            }
        }
        return null;
    }

    private static String symbolCode(JSONObject entry) {
        JSONObject data = entry == null ? null : entry.optJSONObject("data");
        if (data == null) {
            return "";
        }
        String[] keys = {"next_1_hours", "next_6_hours", "next_12_hours"};
        for (String key : keys) {
            JSONObject next = data.optJSONObject(key);
            JSONObject summary = next == null ? null : next.optJSONObject("summary");
            String symbol = summary == null ? "" : summary.optString("symbol_code", "");
            if (!isBlank(symbol)) {
                return symbol;
            }
        }
        return "";
    }

    private static int precipitationProbability(JSONObject entry) {
        JSONObject details = nextDetails(entry);
        if (details == null) {
            return 0;
        }
        if (details.has("probability_of_precipitation")) {
            return clampPercent(details.optDouble("probability_of_precipitation", 0));
        }
        double amount = details.optDouble("precipitation_amount", 0);
        return amount > 0d ? 70 : 0;
    }

    private static int clampPercent(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private static String localDateKey(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
    }

    private static String localHourLabel(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("ha", Locale.US);
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date).toLowerCase(Locale.US);
    }

    private static Date parseMetTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new Date();
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = formatter.parse(value);
            return parsed == null ? new Date() : parsed;
        } catch (Exception ignored) {
            return new Date();
        }
    }

    private static double displayTemperature(double celsius, String units) {
        if (UNITS_FAHRENHEIT.equals(units)) {
            return celsius * 9d / 5d + 32d;
        }
        return celsius;
    }

    private static double displayWindSpeed(double metersPerSecond, String units) {
        if (UNITS_FAHRENHEIT.equals(units)) {
            return metersPerSecond * 2.2369362921d;
        }
        return metersPerSecond * 3.6d;
    }

    private static double displayOpenMeteoWindSpeed(double kilometersPerHour, String units) {
        if (UNITS_FAHRENHEIT.equals(units)) {
            return kilometersPerHour * 0.6213711922d;
        }
        return kilometersPerHour;
    }

    private static int weatherCodeForSymbol(String symbol) {
        String value = symbol == null ? "" : symbol.toLowerCase(Locale.US);
        if (value.contains("thunder")) {
            return 95;
        }
        if (value.contains("snow") || value.contains("sleet")) {
            return 71;
        }
        if (value.contains("rain")) {
            return 61;
        }
        if (value.contains("fog")) {
            return 45;
        }
        if (value.contains("cloudy")) {
            return value.contains("partly") ? 2 : 3;
        }
        if (value.contains("fair")) {
            return 1;
        }
        if (value.contains("clear")) {
            return 0;
        }
        return 2;
    }

    private static String conditionForSymbol(String symbol) {
        String value = symbol == null ? "" : symbol.toLowerCase(Locale.US);
        if (value.contains("thunder")) {
            return "Storm";
        }
        if (value.contains("snow") || value.contains("sleet")) {
            return "Snow";
        }
        if (value.contains("rain")) {
            return "Rain";
        }
        if (value.contains("fog")) {
            return "Fog";
        }
        if (value.contains("cloudy")) {
            return value.contains("partly") ? "Partly cloudy" : "Cloudy";
        }
        if (value.contains("fair")) {
            return "Partly cloudy";
        }
        if (value.contains("clear")) {
            return "Clear";
        }
        return "Partly cloudy";
    }

    private static int severityForCode(int code) {
        if (code >= 95 && code <= 99) {
            return 6;
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            return 5;
        }
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return 4;
        }
        if (code == 45 || code == 48) {
            return 3;
        }
        if (code == 3) {
            return 2;
        }
        if (code == 1 || code == 2) {
            return 1;
        }
        return 0;
    }

    private static Date parseOpenMeteoTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new Date();
        }
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
                formatter.setTimeZone(TimeZone.getDefault());
                Date parsed = formatter.parse(value);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return new Date();
    }

    private static String airQualityDescription(int aqi) {
        if (aqi <= 50) {
            return "Good";
        }
        if (aqi <= 100) {
            return "Moderate";
        }
        if (aqi <= 150) {
            return "Unhealthy for sensitive groups";
        }
        if (aqi <= 200) {
            return "Unhealthy";
        }
        if (aqi <= 300) {
            return "Very unhealthy";
        }
        return "Hazardous";
    }

    private static String approximateSunTime(double latitude, double longitude, Date date,
                                             boolean sunrise) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTime(date);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        double lngHour = longitude / 15d;
        double t = dayOfYear + (((sunrise ? 6d : 18d) - lngHour) / 24d);
        double meanAnomaly = (0.9856d * t) - 3.289d;
        double trueLongitude = meanAnomaly
                + (1.916d * Math.sin(Math.toRadians(meanAnomaly)))
                + (0.020d * Math.sin(Math.toRadians(2d * meanAnomaly)))
                + 282.634d;
        trueLongitude = normalizeDegrees(trueLongitude);
        double rightAscension = Math.toDegrees(Math.atan(0.91764d
                * Math.tan(Math.toRadians(trueLongitude))));
        rightAscension = normalizeDegrees(rightAscension);
        double lQuadrant = Math.floor(trueLongitude / 90d) * 90d;
        double raQuadrant = Math.floor(rightAscension / 90d) * 90d;
        rightAscension = (rightAscension + (lQuadrant - raQuadrant)) / 15d;
        double sinDec = 0.39782d * Math.sin(Math.toRadians(trueLongitude));
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos(Math.toRadians(90.833d))
                - (sinDec * Math.sin(Math.toRadians(latitude))))
                / (cosDec * Math.cos(Math.toRadians(latitude)));
        if (cosH > 1d || cosH < -1d) {
            return "";
        }
        double hourAngle = Math.toDegrees(Math.acos(cosH));
        if (sunrise) {
            hourAngle = 360d - hourAngle;
        }
        hourAngle /= 15d;
        double localMeanTime = hourAngle + rightAscension - (0.06571d * t) - 6.622d;
        double utcTime = normalizeHours(localMeanTime - lngHour);
        int rawOffsetMillis = TimeZone.getDefault().getOffset(date.getTime());
        double localTime = normalizeHours(utcTime + rawOffsetMillis / 3600000d);
        int hour = (int) Math.floor(localTime);
        int minute = (int) Math.round((localTime - hour) * 60d);
        if (minute >= 60) {
            hour = (hour + 1) % 24;
            minute -= 60;
        }
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private static double normalizeDegrees(double value) {
        value %= 360d;
        return value < 0d ? value + 360d : value;
    }

    private static double normalizeHours(double value) {
        value %= 24d;
        return value < 0d ? value + 24d : value;
    }

    private static final class DailyForecastAccumulator {
        final String date;
        double minTemperature = Double.MAX_VALUE;
        double maxTemperature = -Double.MAX_VALUE;
        int weatherCode = 0;
        String condition = "Clear";
        int precipitationProbability = 0;
        int severity = -1;

        DailyForecastAccumulator(String date) {
            this.date = date;
        }
    }

    private String getLocationName() {
        return safeLocationName(prefs.getString(SettingsKeys.PREF_WEATHER_LOCATION_NAME,
                DEFAULT_LOCATION_NAME));
    }

    private String getLatitudeString() {
        return safeLatitude(prefs.getString(SettingsKeys.PREF_WEATHER_LATITUDE,
                DEFAULT_LATITUDE));
    }

    private String getLongitudeString() {
        return safeLongitude(prefs.getString(SettingsKeys.PREF_WEATHER_LONGITUDE,
                DEFAULT_LONGITUDE));
    }

    public String getUnits() {
        return prefs.getString(SettingsKeys.PREF_WEATHER_UNITS, UNITS_CELSIUS);
    }

    private String safeLocationName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_LOCATION_NAME;
        }
        return value.trim();
    }

    private String safeLatitude(String value) {
        return safeCoordinate(value, DEFAULT_LATITUDE, -90d, 90d);
    }

    private String safeLongitude(String value) {
        return safeCoordinate(value, DEFAULT_LONGITUDE, -180d, 180d);
    }

    private String safeCoordinate(String value, String fallback, double min, double max) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed < min || parsed > max) {
                return fallback;
            }
            return String.valueOf(parsed);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isFreshForLocation(WeatherSnapshot cached, WeatherLocation location) {
        if (System.currentTimeMillis() - cached.getFetchedAt() >= CACHE_TTL_MILLIS) {
            return false;
        }
        return distanceMeters(cached.getLatitude(), cached.getLongitude(),
                location.latitude, location.longitude) <= CACHE_LOCATION_TOLERANCE_METERS;
    }

    private Location getBestLastKnownLocation(LocationManager locationManager) {
        Location best = null;
        for (String provider : lastKnownLocationProviders()) {
            try {
                best = betterLocation(best, locationManager.getLastKnownLocation(provider));
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        return best;
    }

    private List<String> enabledLocationProviders(LocationManager locationManager) {
        List<String> providers = new ArrayList<>();
        for (String provider : requestLocationProviders()) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    providers.add(provider);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return providers;
    }

    private List<String> requestLocationProviders() {
        List<String> providers = new ArrayList<>();
        if (!hasPreciseLocationPermission(appContext)) {
            return providers;
        }
        providers.add(LocationManager.GPS_PROVIDER);
        providers.add(LocationManager.NETWORK_PROVIDER);
        return providers;
    }

    private List<String> lastKnownLocationProviders() {
        List<String> providers = requestLocationProviders();
        providers.add(LocationManager.PASSIVE_PROVIDER);
        return providers;
    }

    private Location betterLocation(Location currentBest, Location candidate) {
        if (!isUsableLocation(candidate)) {
            return currentBest;
        }
        if (!isUsableLocation(currentBest)) {
            return candidate;
        }
        if (isGoodLocation(candidate) && !isGoodLocation(currentBest)) {
            return candidate;
        }
        if (!isGoodLocation(candidate) && isGoodLocation(currentBest)) {
            return currentBest;
        }
        float accuracyDelta = candidate.getAccuracy() - currentBest.getAccuracy();
        if (accuracyDelta < -SIGNIFICANT_ACCURACY_DELTA_METERS) {
            return candidate;
        }
        if (accuracyDelta > SIGNIFICANT_ACCURACY_DELTA_METERS) {
            return currentBest;
        }
        if (candidate.getTime() > currentBest.getTime()) {
            return candidate;
        }
        return currentBest;
    }

    private boolean isFreshGoodLocation(Location location) {
        return isFreshLocation(location) && isGoodLocation(location);
    }

    private boolean isFreshLocation(Location location) {
        return location != null
                && location.getTime() > 0
                && System.currentTimeMillis() - location.getTime() < LOCATION_FRESHNESS_MILLIS;
    }

    private boolean isGoodLocation(Location location) {
        return isUsableLocation(location)
                && location.getAccuracy() <= GOOD_LOCATION_ACCURACY_METERS;
    }

    private boolean isUsableLocation(Location location) {
        if (location == null || !location.hasAccuracy()) {
            return false;
        }
        long ageMillis = System.currentTimeMillis() - location.getTime();
        return location.getTime() > 0
                && ageMillis >= 0
                && ageMillis <= LOCATION_MAX_AGE_MILLIS
                && location.getAccuracy() <= ACCEPTABLE_LOCATION_ACCURACY_METERS;
    }

    private void safelyRemoveUpdates(LocationManager locationManager, LocationListener listener) {
        if (listener == null) {
            return;
        }
        try {
            locationManager.removeUpdates(listener);
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    private String resolveLocationName(double latitude, double longitude) {
        String fallback = appContext.getString(R.string.weather_current_location);
        if (!Geocoder.isPresent()) {
            return fallback;
        }
        try {
            List<Address> addresses = new Geocoder(appContext, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.isEmpty()) {
                return fallback;
            }
            Address address = addresses.get(0);
            if (isReverseGeocodeResultFar(address, latitude, longitude)) {
                return fallback;
            }
            String specific = firstMeaningfulPlace(address.getSubLocality(),
                    address.getFeatureName(), address.getThoroughfare());
            String city = firstMeaningfulPlace(address.getLocality(), address.getSubAdminArea(),
                    address.getAdminArea());
            String country = address.getCountryName();
            if (!isBlank(specific) && !samePlaceName(specific, city)) {
                if (!isBlank(city)) {
                    return specific + ", " + city;
                }
                if (!isBlank(country)) {
                    return specific + ", " + country;
                }
                return specific;
            }
            if (!isBlank(city) && !isBlank(country)) {
                return city + ", " + country;
            }
            if (!isBlank(city)) {
                return city;
            }
            if (!isBlank(country)) {
                return country;
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return fallback;
    }

    private static boolean isReverseGeocodeResultFar(Address address,
                                                    double latitude,
                                                    double longitude) {
        return address.hasLatitude()
                && address.hasLongitude()
                && distanceMeters(latitude, longitude, address.getLatitude(),
                address.getLongitude()) > MAX_REVERSE_GEOCODE_NAME_DISTANCE_METERS;
    }

    private static String firstMeaningfulPlace(String... values) {
        for (String value : values) {
            if (isMeaningfulPlace(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isMeaningfulPlace(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        return !lower.equals("unnamed road")
                && !lower.equals("unknown")
                && !trimmed.matches("[0-9\\-/ ]+");
    }

    private static boolean samePlaceName(String first, String second) {
        return !isBlank(first)
                && !isBlank(second)
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String formatCoordinate(double coordinate) {
        return String.format(Locale.US, "%.5f", coordinate);
    }

    private static float distanceMeters(double fromLatitude, double fromLongitude,
                                        double toLatitude, double toLongitude) {
        try {
            float[] results = new float[1];
            Location.distanceBetween(fromLatitude, fromLongitude,
                    toLatitude, toLongitude, results);
            return results[0];
        } catch (IllegalArgumentException ignored) {
            return Float.MAX_VALUE;
        }
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPreciseLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String firstString(JSONArray array) {
        return array != null && array.length() > 0 ? array.optString(0, "") : "";
    }

    private static boolean hasNumber(JSONObject object, String key) {
        return object != null
                && object.has(key)
                && !object.isNull(key)
                && !Double.isNaN(object.optDouble(key, Double.NaN));
    }

    private static boolean hasArrayNumber(JSONArray array, int index) {
        return array != null
                && index >= 0
                && index < array.length()
                && !array.isNull(index)
                && !Double.isNaN(array.optDouble(index, Double.NaN));
    }

    private static int optInt(JSONArray array, int index, int fallback) {
        return array != null && index < array.length() ? array.optInt(index, fallback) : fallback;
    }

    private static double optDouble(JSONArray array, int index, double fallback) {
        return array != null && index < array.length() ? array.optDouble(index, fallback) : fallback;
    }

    private static String formatIsoTime(String isoTime) {
        if (isoTime == null) {
            return "";
        }
        int separator = isoTime.indexOf('T');
        if (separator >= 0 && separator + 1 < isoTime.length()) {
            return isoTime.substring(separator + 1);
        }
        return isoTime;
    }

    private static int currentSkyCode(JSONObject current, int providerCode,
                                      List<WeatherSnapshot.ForecastDay> days) {
        if (current == null) {
            return providerCode;
        }
        if (measuredPrecipitation(current) > 0.01d) {
            return weatherCodeForMeasuredPrecipitation(
                    current.optDouble("precipitation", Double.NaN),
                    current.optDouble("rain", Double.NaN),
                    current.optDouble("showers", Double.NaN),
                    current.optDouble("snowfall", Double.NaN),
                    providerCode);
        }
        if (isFogCode(providerCode)) {
            return providerCode;
        }
        if (isWetCode(providerCode)) {
            return providerCode;
        }
        if (providerCode == 0) {
            int cloudCode = cloudCoverCode(current.optDouble("cloud_cover", Double.NaN));
            if (cloudCode > 0) {
                return cloudCode;
            }
            return hasCloudyRainContext(current, days) ? 2 : providerCode;
        }
        return providerCode;
    }

    private static int weatherCodeForMeasuredPrecipitation(double precipitation, double rain,
                                                           double showers, double snowfall,
                                                           int providerCode) {
        double snowAmount = positiveOrZero(snowfall);
        double rainAmount = Math.max(positiveOrZero(precipitation),
                positiveOrZero(rain) + positiveOrZero(showers));
        if (snowAmount > 0.01d) {
            return isWetCode(providerCode) ? providerCode : 71;
        }
        if (rainAmount > 0.01d) {
            return isWetCode(providerCode) ? providerCode : rainCodeForAmount(rainAmount);
        }
        return providerCode;
    }

    private static int rainCodeForAmount(double millimetersPerHour) {
        if (millimetersPerHour >= 7.6d) {
            return 65;
        }
        if (millimetersPerHour >= 2.5d) {
            return 63;
        }
        if (millimetersPerHour >= 0.5d) {
            return 61;
        }
        return 51;
    }

    private static int cloudCoverCode(double cloudCover) {
        if (Double.isNaN(cloudCover)) {
            return -1;
        }
        if (cloudCover <= 15d) {
            return 0;
        }
        if (cloudCover <= 85d) {
            return 2;
        }
        return 3;
    }

    private static boolean hasCloudyRainContext(JSONObject current,
                                                List<WeatherSnapshot.ForecastDay> days) {
        int humidity = current == null ? -1 : current.optInt("relative_humidity_2m", -1);
        if (humidity < 85 || days == null || days.isEmpty()) {
            return false;
        }
        WeatherSnapshot.ForecastDay today = days.get(0);
        return today.getPrecipitationProbability() >= 35 || isWetCode(today.getWeatherCode());
    }

    private static double positiveOrZero(double value) {
        if (Double.isNaN(value) || value < 0d) {
            return 0d;
        }
        return value;
    }

    private static double measuredPrecipitation(JSONObject current) {
        if (current == null) {
            return 0d;
        }
        double combined = positiveOrZero(current.optDouble("rain", Double.NaN))
                + positiveOrZero(current.optDouble("showers", Double.NaN))
                + positiveOrZero(current.optDouble("snowfall", Double.NaN));
        return Math.max(positiveOrZero(current.optDouble("precipitation", Double.NaN)),
                combined);
    }

    private static boolean isFogCode(int code) {
        return code == 45 || code == 48;
    }

    private static boolean isWetCode(int code) {
        return (code >= 51 && code <= 67)
                || (code >= 71 && code <= 77)
                || (code >= 80 && code <= 86)
                || (code >= 95 && code <= 99);
    }

    public static String conditionForCode(int code) {
        if (code == 0) {
            return "Clear";
        }
        if (code == 1 || code == 2) {
            return "Partly cloudy";
        }
        if (code == 3) {
            return "Cloudy";
        }
        if (code == 45 || code == 48) {
            return "Fog";
        }
        if (code == 65 || code == 82) {
            return "Heavy rain";
        }
        if (code == 51 || code == 53 || code == 80) {
            return "Light rain";
        }
        if ((code >= 55 && code <= 67) || code == 61 || code == 63 || code == 81) {
            return "Rain";
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            return "Snow";
        }
        if (code >= 95 && code <= 99) {
            return "Storm";
        }
        return String.format(Locale.US, "Code %d", code);
    }

    private interface WeatherLocationCallback {
        void onLocationReady(WeatherLocation location);
    }

    private static final class WeatherLocation {
        final String name;
        final String latitudeString;
        final String longitudeString;
        final double latitude;
        final double longitude;

        WeatherLocation(String name, String latitudeString, String longitudeString,
                        double latitude, double longitude) {
            this.name = name;
            this.latitudeString = latitudeString;
            this.longitudeString = longitudeString;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
