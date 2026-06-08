package com.webstudio.easybrowser.managers;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.WeatherSnapshot;
import com.webstudio.easybrowser.ui.activity.WeatherActivity;
import com.webstudio.easybrowser.utils.SettingsKeys;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class WeatherAlertManager {
    private static final String KEY_LAST_ALERT_BUCKET = "weather_last_alert_bucket";
    private static final String KEY_LAST_ALERT_TYPE = "weather_last_alert_type";
    private static final int NOTIFICATION_ID = 2706;

    private static final String TYPE_DAILY = "daily";
    private static final String TYPE_RAIN = "rain";
    private static final String TYPE_SEVERE = "severe";

    private WeatherAlertManager() {
    }

    public static void maybeNotify(Context context, WeatherSnapshot snapshot, String units) {
        if (snapshot == null || !isEnabled(context) || !canPostNotifications(context)) {
            return;
        }
        Alert alert = chooseAlert(context, snapshot, units);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int today = dayBucket();
        if (today == prefs.getInt(KEY_LAST_ALERT_BUCKET, 0)
                && alert.type.equals(prefs.getString(KEY_LAST_ALERT_TYPE, ""))) {
            return;
        }

        AppNotificationChannels.ensureCreated(context);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_WEATHER)
                        .setSmallIcon(alert.iconRes)
                        .setContentTitle(alert.title)
                        .setContentText(alert.text)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(alert.text))
                        .setContentIntent(createWeatherIntent(context))
                        .setAutoCancel(true)
                        .setPriority(alert.priority);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
            prefs.edit()
                    .putInt(KEY_LAST_ALERT_BUCKET, today)
                    .putString(KEY_LAST_ALERT_TYPE, alert.type)
                    .apply();
        } catch (SecurityException ignored) {
        }
    }

    private static Alert chooseAlert(Context context, WeatherSnapshot snapshot, String units) {
        WeatherSnapshot.ForecastDay today = snapshot.getForecastDays().isEmpty()
                ? null
                : snapshot.getForecastDays().get(0);
        int currentCode = snapshot.getWeatherCode();
        int forecastCode = today != null ? today.getWeatherCode() : currentCode;
        int rainChance = today != null ? today.getPrecipitationProbability() : 0;

        if (isSevere(currentCode) || isSevere(forecastCode)) {
            return new Alert(TYPE_SEVERE,
                    context.getString(R.string.weather_notification_severe_title),
                    context.getString(R.string.weather_notification_severe_text,
                            snapshot.getLocationName(), formatForecast(today, snapshot, units)),
                    R.drawable.ic_weather_storm,
                    NotificationCompat.PRIORITY_HIGH);
        }
        if (isRain(currentCode) || rainChance >= 60) {
            return new Alert(TYPE_RAIN,
                    context.getString(R.string.weather_notification_rain_title),
                    context.getString(R.string.weather_notification_rain_text,
                            snapshot.getLocationName(), rainChance,
                            formatForecast(today, snapshot, units)),
                    R.drawable.ic_weather_rain,
                    NotificationCompat.PRIORITY_DEFAULT);
        }
        return new Alert(TYPE_DAILY,
                context.getString(R.string.weather_notification_daily_title),
                context.getString(R.string.weather_notification_daily_text,
                        snapshot.getLocationName(), snapshot.getCondition(),
                        snapshot.formatTemperature(units)),
                R.drawable.ic_weather_partly_cloudy,
                NotificationCompat.PRIORITY_LOW);
    }

    private static String formatForecast(WeatherSnapshot.ForecastDay today,
                                         WeatherSnapshot snapshot, String units) {
        if (today == null) {
            return snapshot.getCondition() + ", " + snapshot.formatTemperature(units);
        }
        return String.format(Locale.US, "%s %.0f/%.0f%s",
                today.getCondition(), today.getMaxTemperature(), today.getMinTemperature(),
                WeatherSnapshot.unitSuffix(units));
    }

    private static PendingIntent createWeatherIntent(Context context) {
        Intent intent = new Intent(context, WeatherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 2706, intent, flags);
    }

    private static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsKeys.PREF_WEATHER_NOTIFICATIONS, true);
    }

    private static boolean canPostNotifications(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
                && (Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED);
    }

    private static boolean isRain(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
    }

    private static boolean isSevere(int code) {
        return code >= 95 && code <= 99;
    }

    private static int dayBucket() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private static final class Alert {
        final String type;
        final String title;
        final String text;
        final int iconRes;
        final int priority;

        Alert(String type, String title, String text, int iconRes, int priority) {
            this.type = type;
            this.title = title;
            this.text = text;
            this.iconRes = iconRes;
            this.priority = priority;
        }
    }
}
