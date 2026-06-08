package com.webstudio.easybrowser.utils;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.WeatherSnapshot;

public final class WeatherAnimationMapper {
    private WeatherAnimationMapper() {
    }

    public static int animationFor(WeatherSnapshot snapshot, boolean night) {
        if (snapshot == null) {
            return R.raw.weather_simple_partly_cloudy;
        }
        int code = snapshot.getWeatherCode();
        if (!isWetOrStormy(code) && snapshot.getWindSpeed() >= 35d) {
            return R.raw.weather_simple_wind;
        }
        if (code == 0) {
            return night ? R.raw.weather_moon : R.raw.weather_simple_sun;
        }
        if (code == 1 || code == 2) {
            return night ? R.raw.weather_sun_cloud_orbit : R.raw.weather_simple_partly_cloudy;
        }
        if (code == 3) {
            return night ? R.raw.weather_moon_cloudy : R.raw.weather_simple_clouds;
        }
        if (code == 45) {
            return R.raw.weather_day_fog;
        }
        if (code == 48) {
            return R.raw.weather_mist;
        }
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return R.raw.weather_simple_rain_medium;
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            return night ? R.raw.weather_snow_night : R.raw.weather_snow_sunny;
        }
        if (code == 95) {
            return R.raw.weather_storm_compact;
        }
        if (code == 96 || code == 99) {
            return R.raw.weather_storm_compact;
        }
        return night ? R.raw.weather_sun_cloud_orbit : R.raw.weather_simple_partly_cloudy;
    }

    private static boolean isWetOrStormy(int code) {
        return (code >= 51 && code <= 67)
                || (code >= 71 && code <= 77)
                || (code >= 80 && code <= 86)
                || (code >= 95 && code <= 99);
    }
}
