package com.webstudio.easybrowser.ui.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.SettingsKeys;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission;
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.MediaCallback;
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.MediaSource;

class BrowserPermissionDelegate implements GeckoSession.PermissionDelegate {

    private final BrowserActivity activity;

    BrowserPermissionDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onAndroidPermissionsRequest(@NonNull GeckoSession session,
                                            @Nullable String[] permissions,
                                            @NonNull Callback callback) {
        activity.pendingPermissionCallback = callback;
        if (permissions != null && permissions.length > 0) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    activity, permissions, BrowserActivity.REQUEST_GECKO_PERMISSIONS);
        } else {
            // GeckoView shouldn't normally hand us an empty array, but ActivityCompat
            // would refuse it; complete the callback ourselves to keep state consistent.
            activity.pendingPermissionCallback = null;
            callback.reject();
        }
    }

    @Override
    public GeckoResult<Integer> onContentPermissionRequest(@NonNull GeckoSession session,
                                                           @NonNull ContentPermission perm) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
            String autoplay = prefs.getString(SettingsKeys.PREF_SITE_AUTOPLAY,
                    SettingsKeys.VALUE_DENY);
            if (SettingsKeys.VALUE_ALLOW.equals(autoplay)
                    || SettingsKeys.VALUE_DENY.equals(autoplay)
                    || SettingsKeys.VALUE_ASK.equals(autoplay)) {
                return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
            }
            return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
        }
        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
            String autoplay = prefs.getString(SettingsKeys.PREF_SITE_AUTOPLAY,
                    SettingsKeys.VALUE_DENY);
            if (SettingsKeys.VALUE_ALLOW.equals(autoplay)) {
                return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
            }
            if (SettingsKeys.VALUE_DENY.equals(autoplay)) {
                return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
            }
        }
        String origin = getDisplayOrigin(perm.uri);
        String permissionLabel = getPermissionLabel(perm.permission);
        String setting = getContentPermissionSetting(prefs, perm.permission);
        if (SettingsKeys.VALUE_ALLOW.equals(setting)) {
            SitePermissionsActivity.recordPermission(
                    prefs, origin, permissionLabel);
            return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
        }
        if (SettingsKeys.VALUE_DENY.equals(setting)) {
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        }
        if (SitePermissionsActivity.hasPermission(prefs, origin, permissionLabel)) {
            return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
        }
        GeckoResult<Integer> result = new GeckoResult<>();
        activity.runOnUiThread(() -> showContentPermissionPrompt(perm, result));
        return result;
    }

    @Override
    public void onMediaPermissionRequest(@NonNull GeckoSession session, @NonNull String uri,
                                         @Nullable MediaSource[] video,
                                         @Nullable MediaSource[] audio,
                                         @NonNull MediaCallback callback) {
        MediaSource videoSource = video != null && video.length > 0 ? video[0] : null;
        MediaSource audioSource = audio != null && audio.length > 0 ? audio[0] : null;
        if (videoSource != null || audioSource != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            String cameraSetting = prefs.getString(SettingsKeys.PREF_SITE_CAMERA,
                    SettingsKeys.VALUE_ASK);
            String microphoneSetting = prefs.getString(SettingsKeys.PREF_SITE_MICROPHONE,
                    SettingsKeys.VALUE_ASK);
            if ((videoSource != null && SettingsKeys.VALUE_DENY.equals(cameraSetting))
                    || (audioSource != null && SettingsKeys.VALUE_DENY.equals(microphoneSetting))) {
                callback.reject();
                return;
            }
            boolean cameraAllowed = videoSource == null
                    || SettingsKeys.VALUE_ALLOW.equals(cameraSetting);
            boolean microphoneAllowed = audioSource == null
                    || SettingsKeys.VALUE_ALLOW.equals(microphoneSetting);
            String origin = getDisplayOrigin(uri);
            String permissionLabel = getMediaPermissionLabel(videoSource, audioSource);
            boolean siteAllowed = SitePermissionsActivity.hasPermission(prefs, origin, permissionLabel);
            if (cameraAllowed && microphoneAllowed) {
                callback.grant(videoSource, audioSource);
                SitePermissionsActivity.recordPermission(
                        prefs, origin, permissionLabel);
                return;
            }
            if (siteAllowed) {
                callback.grant(videoSource, audioSource);
                return;
            }
            activity.runOnUiThread(() ->
                    showMediaPermissionPrompt(uri, videoSource, audioSource, callback));
        } else {
            callback.reject();
        }
    }

    private void showContentPermissionPrompt(ContentPermission permission,
                                             GeckoResult<Integer> result) {
        String origin = getDisplayOrigin(permission.uri);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.site_permission_request_title,
                        getPermissionLabel(permission.permission)))
                .setMessage(activity.getString(R.string.site_permission_request_message,
                        origin, getPermissionLabel(permission.permission)))
                .setPositiveButton(R.string.allow, (dialog, which) -> {
                    result.complete(ContentPermission.VALUE_ALLOW);
                    SitePermissionsActivity.recordPermission(
                            PreferenceManager.getDefaultSharedPreferences(activity),
                            origin, getPermissionLabel(permission.permission));
                })
                .setNegativeButton(R.string.deny, (dialog, which) ->
                        result.complete(ContentPermission.VALUE_DENY))
                .setOnCancelListener(dialog -> result.complete(ContentPermission.VALUE_DENY))
                .show();
    }

    private void showMediaPermissionPrompt(String uri, MediaSource videoSource,
                                           MediaSource audioSource, MediaCallback callback) {
        String origin = getDisplayOrigin(uri);
        String permissionLabel = getMediaPermissionLabel(videoSource, audioSource);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.site_permission_request_title, permissionLabel))
                .setMessage(activity.getString(R.string.site_permission_request_message,
                        origin, permissionLabel))
                .setPositiveButton(R.string.allow, (dialog, which) -> {
                    callback.grant(videoSource, audioSource);
                    SitePermissionsActivity.recordPermission(
                            PreferenceManager.getDefaultSharedPreferences(activity),
                            origin, permissionLabel);
                })
                .setNegativeButton(R.string.deny, (dialog, which) -> callback.reject())
                .setOnCancelListener(dialog -> callback.reject())
                .show();
    }

    private String getContentPermissionSetting(SharedPreferences prefs, int permission) {
        if (permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
            return prefs.getString(SettingsKeys.PREF_SITE_LOCATION, SettingsKeys.VALUE_ASK);
        }
        if (permission == GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION) {
            return prefs.getString(SettingsKeys.PREF_SITE_NOTIFICATIONS, SettingsKeys.VALUE_ASK);
        }
        if (permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS) {
            if (!prefs.getBoolean(SettingsKeys.PREF_PROTECTED_MEDIA_ENABLED, true)) {
                return SettingsKeys.VALUE_DENY;
            }
            return prefs.getString(SettingsKeys.PREF_SITE_PROTECTED_MEDIA,
                    SettingsKeys.VALUE_ASK);
        }
        if (permission == GeckoSession.PermissionDelegate.PERMISSION_LOCAL_NETWORK_ACCESS) {
            return prefs.getString(SettingsKeys.PREF_SITE_LOCAL_NETWORK, SettingsKeys.VALUE_ASK);
        }
        return SettingsKeys.VALUE_ASK;
    }

    private String getDisplayOrigin(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return activity.getString(R.string.unknown_site);
        }
        Uri parsed;
        try {
            parsed = Uri.parse(uri);
        } catch (Exception e) {
            return activity.getString(R.string.unknown_site);
        }
        if (parsed == null) {
            return activity.getString(R.string.unknown_site);
        }
        String host = parsed.getHost();
        if (TextUtils.isEmpty(host)) {
            // data:, blob:, about: and other host-less URIs should never be shown
            // verbatim — the URI body can be used to spoof a legitimate origin.
            return activity.getString(R.string.unknown_site);
        }
        return host;
    }

    private String getPermissionLabel(int permission) {
        switch (permission) {
            case GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION:
                return activity.getString(R.string.location);
            case GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION:
                return activity.getString(R.string.notifications);
            case GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE:
                return activity.getString(R.string.persistent_storage);
            case GeckoSession.PermissionDelegate.PERMISSION_XR:
                return activity.getString(R.string.webxr);
            case GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE:
            case GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE:
                return activity.getString(R.string.autoplay);
            case GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS:
                return activity.getString(R.string.protected_media);
            case GeckoSession.PermissionDelegate.PERMISSION_LOCAL_NETWORK_ACCESS:
                return activity.getString(R.string.local_network);
            case GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS:
                return activity.getString(R.string.storage_access);
            default:
                return activity.getString(R.string.permission);
        }
    }

    private String getMediaPermissionLabel(MediaSource videoSource, MediaSource audioSource) {
        if (videoSource != null && audioSource != null) {
            return activity.getString(R.string.camera_and_microphone);
        }
        if (videoSource != null) {
            return activity.getString(R.string.camera);
        }
        return activity.getString(R.string.microphone);
    }
}
