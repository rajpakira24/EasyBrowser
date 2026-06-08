package com.webstudio.easybrowser.utils;

import com.webstudio.easybrowser.R;

public final class WeatherIconMapper {
    private WeatherIconMapper() {
    }

    public static int iconForCode(int code) {
        if (code == 0) {
            return R.drawable.ic_weather_sunny;
        }
        if (code == 1 || code == 2) {
            return R.drawable.ic_weather_partly_cloudy;
        }
        if (code == 3) {
            return R.drawable.ic_weather_cloudy;
        }
        if (code == 45 || code == 48) {
            return R.drawable.ic_weather_fog;
        }
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return R.drawable.ic_weather_rain;
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            return R.drawable.ic_weather_snow;
        }
        if (code >= 95 && code <= 99) {
            return R.drawable.ic_weather_storm;
        }
        return R.drawable.ic_weather_partly_cloudy;
    }
}
