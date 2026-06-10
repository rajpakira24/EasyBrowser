package com.webstudio.easybrowser.utils;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.WeatherSnapshot;

import java.util.Calendar;

public final class WeatherAnimationMapper {
    private static final String UNITS_FAHRENHEIT = "fahrenheit";
    private static final double HOT_CELSIUS = 32d;
    private static final double FREEZING_CELSIUS = 1d;
    private static final double WINDY_KMH = 35d;

    private WeatherAnimationMapper() {
    }

    public static int animationFor(WeatherSnapshot snapshot, String units) {
        return animationFor(snapshot, isNight(snapshot), units);
    }

    public static int animationFor(WeatherSnapshot snapshot, boolean night) {
        return animationFor(snapshot, night, null);
    }

    public static int animationFor(WeatherSnapshot snapshot, boolean night, String units) {
        if (snapshot == null) {
            return R.raw.weather_simple_partly_cloudy;
        }
        int code = snapshot.getWeatherCode();
        double temperatureCelsius = temperatureCelsius(snapshot, units);
        double windKmh = windKmh(snapshot, units);

        if (code == 0) {
            if (night) {
                return R.raw.weather_moon;
            }
            return temperatureCelsius >= HOT_CELSIUS
                    ? R.raw.weather_sunny
                    : R.raw.weather_simple_sun;
        }
        if (!isWetOrStormy(code) && windKmh >= WINDY_KMH) {
            return R.raw.weather_windy;
        }
        if (code == 1) {
            return night ? R.raw.weather_moon_cloudy : R.raw.weather_cloud_sun;
        }
        if (code == 2) {
            return night ? R.raw.weather_moon_cloudy : R.raw.weather_partly_cloudy_day;
        }
        if (code == 3) {
            return night ? R.raw.weather_moon_cloudy : R.raw.weather_simple_clouds;
        }
        if (code == 45 || code == 48) {
            return night ? R.raw.weather_mist : R.raw.weather_day_fog;
        }
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            if (temperatureCelsius <= FREEZING_CELSIUS && (code == 56 || code == 57
                    || code == 66 || code == 67)) {
                return night ? R.raw.weather_snow_night : R.raw.weather_snow;
            }
            return night ? R.raw.weather_rainy_night : R.raw.weather_rainy_day;
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            if (night) {
                return R.raw.weather_snow_night;
            }
            return temperatureCelsius <= FREEZING_CELSIUS
                    ? R.raw.weather_snow
                    : R.raw.weather_snow_sunny;
        }
        if (code == 95) {
            return R.raw.weather_storm_compact;
        }
        if (code == 96 || code == 99) {
            return R.raw.weather_thunder;
        }
        return night ? R.raw.weather_moon_cloudy : R.raw.weather_simple_partly_cloudy;
    }

    public static boolean isNight(WeatherSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        int sunrise = parseTimeMinutes(snapshot.getSunrise());
        int sunset = parseTimeMinutes(snapshot.getSunset());
        if (sunrise < 0 || sunset < 0) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(snapshot.getFetchedAt());
        int current = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return current < sunrise || current > sunset;
    }

    private static boolean isWetOrStormy(int code) {
        return (code >= 51 && code <= 67)
                || (code >= 71 && code <= 77)
                || (code >= 80 && code <= 86)
                || (code >= 95 && code <= 99);
    }

    private static double temperatureCelsius(WeatherSnapshot snapshot, String units) {
        double temperature = snapshot.getTemperature();
        if (UNITS_FAHRENHEIT.equals(units)) {
            return (temperature - 32d) * 5d / 9d;
        }
        return temperature;
    }

    private static double windKmh(WeatherSnapshot snapshot, String units) {
        double wind = snapshot.getWindSpeed();
        if (UNITS_FAHRENHEIT.equals(units)) {
            return wind * 1.609344d;
        }
        return wind;
    }

    private static int parseTimeMinutes(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1;
        }
        String[] parts = value.trim().split(":");
        if (parts.length < 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
