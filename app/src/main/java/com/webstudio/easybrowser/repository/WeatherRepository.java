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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private static final String KEY_CACHE_JSON = "weather_cache_json";
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
                        cacheSnapshot(snapshot);
                        callback.onWeatherLoaded(snapshot, false);
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
        return prefs.getBoolean(SettingsKeys.PREF_WEATHER_USE_CURRENT_LOCATION, true);
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
        Uri.Builder builder = Uri.parse(weatherEndpoint()).buildUpon()
                .appendQueryParameter("latitude", location.latitudeString)
                .appendQueryParameter("longitude", location.longitudeString)
                .appendQueryParameter("current",
                        "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                .appendQueryParameter("daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max")
                .appendQueryParameter("timezone", "auto")
                .appendQueryParameter("forecast_days", "7");
        if (UNITS_FAHRENHEIT.equals(getUnits())) {
            builder.appendQueryParameter("temperature_unit", "fahrenheit")
                    .appendQueryParameter("wind_speed_unit", "mph");
        }
        if (!isBlank(BuildConfig.OPEN_METEO_API_KEY)) {
            builder.appendQueryParameter("apikey", BuildConfig.OPEN_METEO_API_KEY.trim());
        }
        return builder.build().toString();
    }

    private String weatherEndpoint() {
        if (isBlank(BuildConfig.OPEN_METEO_BASE_URL)) {
            return "https://api.open-meteo.com/v1/forecast";
        }
        return BuildConfig.OPEN_METEO_BASE_URL.trim();
    }

    private WeatherSnapshot parseForecast(String raw, WeatherLocation location) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject current = root.getJSONObject("current");
        JSONObject daily = root.optJSONObject("daily");
        List<WeatherSnapshot.ForecastDay> days = parseDailyForecast(daily);
        String sunrise = "";
        String sunset = "";
        if (daily != null) {
            sunrise = firstString(daily.optJSONArray("sunrise"));
            sunset = firstString(daily.optJSONArray("sunset"));
        }
        int code = current.optInt("weather_code", 0);
        return new WeatherSnapshot(
                location.name,
                location.latitude,
                location.longitude,
                current.optDouble("temperature_2m", 0),
                current.optDouble("apparent_temperature", 0),
                current.optInt("relative_humidity_2m", 0),
                current.optDouble("wind_speed_10m", 0),
                code,
                conditionForCode(code),
                formatIsoTime(sunrise),
                formatIsoTime(sunset),
                System.currentTimeMillis(),
                days);
    }

    private List<WeatherSnapshot.ForecastDay> parseDailyForecast(JSONObject daily) {
        List<WeatherSnapshot.ForecastDay> days = new ArrayList<>();
        if (daily == null) {
            return days;
        }
        JSONArray dates = daily.optJSONArray("time");
        JSONArray codes = daily.optJSONArray("weather_code");
        JSONArray max = daily.optJSONArray("temperature_2m_max");
        JSONArray min = daily.optJSONArray("temperature_2m_min");
        JSONArray precipitation = daily.optJSONArray("precipitation_probability_max");
        if (dates == null) {
            return days;
        }
        for (int i = 0; i < dates.length(); i++) {
            int code = optInt(codes, i, 0);
            days.add(new WeatherSnapshot.ForecastDay(
                    dates.optString(i, ""),
                    code,
                    conditionForCode(code),
                    optDouble(min, i, 0),
                    optDouble(max, i, 0),
                    optInt(precipitation, i, 0)));
        }
        return days;
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
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
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
