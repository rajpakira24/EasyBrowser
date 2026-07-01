package com.webstudio.easybrowser.managers;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.webstudio.easybrowser.R;

/**
 * Lightweight foreground service that keeps the process alive while downloads are running.
 * The actual transfer work stays in {@link AppDownloadManager}'s executor; this service only
 * raises the process priority so an active download isn't killed when the app is backgrounded,
 * swiped away, or hits Doze. Started when the first download begins, stopped when the last ends.
 */
public class DownloadService extends Service {
    // Shared with AppDownloadManager: the progress notification is posted on this id so it
    // updates the foreground-service notification in place rather than showing a duplicate.
    static final int FOREGROUND_ID = 1;
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    static boolean isRunning() {
        return RUNNING.get();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), DownloadService.class);
        try {
            ContextCompat.startForegroundService(context.getApplicationContext(), intent);
        } catch (RuntimeException ignored) {
            // API 31+ background-start restrictions can reject this; the download still runs
            // in-process, just without elevated priority. Never crash the download for it.
        }
    }

    public static void stop(Context context) {
        try {
            context.getApplicationContext()
                    .stopService(new Intent(context.getApplicationContext(), DownloadService.class));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppNotificationChannels.ensureCreated(this);
        Notification notification = new NotificationCompat.Builder(
                this, AppNotificationChannels.CHANNEL_DOWNLOADS)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(getString(R.string.download_in_progress))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                // Indeterminate until AppDownloadManager updates this same id with real
                // progress (within ~500ms). Avoids a separate duplicate progress notification.
                .setProgress(0, 0, true)
                .build();
        // Must call startForeground promptly (within ~5s of startForegroundService) or the
        // platform throws. We call it immediately here.
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(FOREGROUND_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
        RUNNING.set(true);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        RUNNING.set(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
