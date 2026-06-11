package com.webstudio.easybrowser.managers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.ui.activity.BrowserActivity;

public class BackgroundMediaService extends Service {
    public static final String ACTION_UPDATE =
            "com.webstudio.easybrowser.action.MEDIA_NOTIFICATION_UPDATE";
    public static final String ACTION_PLAY =
            "com.webstudio.easybrowser.action.MEDIA_NOTIFICATION_PLAY";
    public static final String ACTION_PAUSE =
            "com.webstudio.easybrowser.action.MEDIA_NOTIFICATION_PAUSE";
    public static final String ACTION_STOP =
            "com.webstudio.easybrowser.action.MEDIA_NOTIFICATION_STOP";
    public static final String ACTION_MEDIA_CONTROL =
            "com.webstudio.easybrowser.action.MEDIA_NOTIFICATION_CONTROL";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_PLAYING = "playing";
    public static final String EXTRA_COMMAND = "command";

    public static final String COMMAND_PLAY = "play";
    public static final String COMMAND_PAUSE = "pause";
    public static final String COMMAND_STOP = "stop";

    private static final int NOTIFICATION_ID = 4207;

    private MediaSession mediaSession;
    private String title;
    private String artist;
    private boolean playing;

    public static Intent createUpdateIntent(android.content.Context context, String title,
                                            String artist, boolean playing) {
        return new Intent(context, BackgroundMediaService.class)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_ARTIST, artist)
                .putExtra(EXTRA_PLAYING, playing);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppNotificationChannels.ensureCreated(this);
        title = getString(R.string.app_name);
        artist = "";
        mediaSession = new MediaSession(this, "EasyBrowserMedia");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                sendControl(COMMAND_PLAY);
            }

            @Override
            public void onPause() {
                sendControl(COMMAND_PAUSE);
            }

            @Override
            public void onStop() {
                sendControl(COMMAND_STOP);
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PLAY.equals(action)) {
            sendControl(COMMAND_PLAY);
            return START_STICKY;
        }
        if (ACTION_PAUSE.equals(action)) {
            playing = false;
            sendControl(COMMAND_PAUSE);
            publishNotification();
            return START_STICKY;
        }
        if (ACTION_STOP.equals(action)) {
            sendControl(COMMAND_STOP);
            stopSelfWithNotification();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            String nextTitle = intent.getStringExtra(EXTRA_TITLE);
            String nextArtist = intent.getStringExtra(EXTRA_ARTIST);
            title = !TextUtils.isEmpty(nextTitle) ? nextTitle : getString(R.string.app_name);
            artist = nextArtist != null ? nextArtist : "";
            playing = intent.getBooleanExtra(EXTRA_PLAYING, playing);
        }
        publishNotification();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }

    private void publishNotification() {
        if (mediaSession == null) {
            return;
        }
        updateMediaSessionState();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateMediaSessionState() {
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .build());
        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_STOP;
        int state = playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        float speed = playing ? 1f : 0f;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, speed)
                .build());
    }

    private Notification buildNotification() {
        Intent contentIntent = new Intent(this, BrowserActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent, pendingIntentFlags());
        PendingIntent actionPendingIntent = PendingIntent.getService(
                this,
                playing ? 2 : 1,
                new Intent(this, BackgroundMediaService.class)
                        .setAction(playing ? ACTION_PAUSE : ACTION_PLAY),
                pendingIntentFlags());
        int actionIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        String actionTitle = getString(playing
                ? R.string.media_action_pause
                : R.string.media_action_play);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, AppNotificationChannels.CHANNEL_MEDIA)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_audio)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(contentPendingIntent)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setOngoing(playing)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(new Notification.Action.Builder(
                        actionIcon, actionTitle, actionPendingIntent).build())
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        return builder.build();
    }

    private int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private void sendControl(String command) {
        Intent intent = new Intent(ACTION_MEDIA_CONTROL)
                .setPackage(getPackageName())
                .putExtra(EXTRA_COMMAND, command);
        sendBroadcast(intent);
    }

    private void stopSelfWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }
}
