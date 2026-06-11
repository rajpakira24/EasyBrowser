package com.webstudio.easybrowser.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.webstudio.easybrowser.R;

public final class AppNotificationChannels {
    public static final String CHANNEL_BROWSER = "browser_alerts";
    public static final String CHANNEL_DOWNLOADS = "downloads";
    public static final String CHANNEL_MEDIA = "media_playback";
    public static final String CHANNEL_PRIVACY = "privacy";
    public static final String CHANNEL_WEATHER = "weather";
    public static final String CHANNEL_REWARDS = "rewards";
    public static final String CHANNEL_AI = "ai";
    public static final String CHANNEL_UPDATES = "updates";

    private AppNotificationChannels() {
    }

    public static void ensureCreated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        create(manager, CHANNEL_BROWSER,
                context.getString(R.string.notification_channel_browser),
                context.getString(R.string.notification_channel_browser_description),
                NotificationManager.IMPORTANCE_DEFAULT);
        create(manager, CHANNEL_DOWNLOADS,
                context.getString(R.string.notification_channel_downloads),
                context.getString(R.string.notification_channel_downloads_description),
                NotificationManager.IMPORTANCE_LOW);
        create(manager, CHANNEL_MEDIA,
                context.getString(R.string.notification_channel_media),
                context.getString(R.string.notification_channel_media_description),
                NotificationManager.IMPORTANCE_LOW);
        create(manager, CHANNEL_PRIVACY,
                context.getString(R.string.notification_channel_privacy),
                context.getString(R.string.notification_channel_privacy_description),
                NotificationManager.IMPORTANCE_DEFAULT);
        create(manager, CHANNEL_WEATHER,
                context.getString(R.string.notification_channel_weather),
                context.getString(R.string.notification_channel_weather_description),
                NotificationManager.IMPORTANCE_LOW);
        create(manager, CHANNEL_REWARDS,
                context.getString(R.string.notification_channel_rewards),
                context.getString(R.string.notification_channel_rewards_description),
                NotificationManager.IMPORTANCE_LOW);
        create(manager, CHANNEL_AI,
                context.getString(R.string.notification_channel_ai),
                context.getString(R.string.notification_channel_ai_description),
                NotificationManager.IMPORTANCE_LOW);
        create(manager, CHANNEL_UPDATES,
                context.getString(R.string.notification_channel_updates),
                context.getString(R.string.notification_channel_updates_description),
                NotificationManager.IMPORTANCE_DEFAULT);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void create(NotificationManager manager, String id, String name,
                               String description, int importance) {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        manager.createNotificationChannel(channel);
    }
}
