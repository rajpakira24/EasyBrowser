package com.webstudio.easybrowser.utils;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.WeatherSnapshot;

import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WeatherAnimationMapperTest {
    @Test
    public void animationFor_hotClearDay_usesSunnyAnimation() {
        WeatherSnapshot snapshot = snapshot(0, 34d, 8d, atHour(13));

        int animation = WeatherAnimationMapper.animationFor(snapshot, false, "celsius");

        assertEquals(R.raw.weather_sunny, animation);
    }

    @Test
    public void animationFor_clearNight_usesMoonAnimation() {
        WeatherSnapshot snapshot = snapshot(0, 27d, 8d, atHour(23));

        int animation = WeatherAnimationMapper.animationFor(snapshot, true, "celsius");

        assertEquals(R.raw.weather_moon, animation);
    }

    @Test
    public void animationFor_rainyNight_usesNightRainAnimation() {
        WeatherSnapshot snapshot = snapshot(61, 22d, 10d, atHour(23));

        int animation = WeatherAnimationMapper.animationFor(snapshot, true, "celsius");

        assertEquals(R.raw.weather_rainy_night, animation);
    }

    @Test
    public void animationFor_fahrenheitWind_convertsThresholdToKmh() {
        WeatherSnapshot snapshot = snapshot(2, 75d, 25d, atHour(13));

        int animation = WeatherAnimationMapper.animationFor(snapshot, false, "fahrenheit");

        assertEquals(R.raw.weather_windy, animation);
    }

    @Test
    public void isNight_afterSunset_returnsTrue() {
        WeatherSnapshot snapshot = snapshot(3, 25d, 5d, atHour(22));

        assertTrue(WeatherAnimationMapper.isNight(snapshot));
    }

    private static WeatherSnapshot snapshot(int code, double temperature, double windSpeed,
                                            long fetchedAt) {
        return new WeatherSnapshot(
                "Test",
                0d,
                0d,
                temperature,
                temperature,
                50,
                windSpeed,
                code,
                "Test",
                "06:00",
                "18:00",
                fetchedAt,
                Collections.emptyList());
    }

    private static long atHour(int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
