package com.webstudio.easybrowser.ui.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.LinkedHashSet;
import java.util.Locale;

class BrowserPermissionDelegate implements GeckoSession.PermissionDelegate {

    private final BrowserActivity activity;

    BrowserPermissionDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onAndroidPermissionsRequest(@NonNull GeckoSession session,
                                            @Nullable String[] permissions,
                                            @NonNull Callback callback) {
        String[] requestedPermissions = normalizeAndroidPermissions(permissions);
        activity.pendingPermissionCallback = callback;
        if (requestedPermissions.length > 0) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    activity, requestedPermissions, BrowserActivity.REQUEST_GECKO_PERMISSIONS);
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
        if (SitePermissionsActivity.isPermissionDenied(prefs, origin, permissionLabel)) {
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
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
        if (permission.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
            showLocationPermissionPrompt(permission, result);
            return;
        }
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

    private void showLocationPermissionPrompt(ContentPermission permission,
                                              GeckoResult<Integer> result) {
        String origin = getDisplayOrigin(permission.uri);
        String host = getDisplayHost(permission.uri);
        String permissionLabel = getPermissionLabel(permission.permission);
        boolean[] completed = {false};

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(unused -> completeLocationPrompt(dialog, result,
                ContentPermission.VALUE_DENY, origin, permissionLabel, false, false, completed));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int horizontalPadding = dpToPx(24);
        card.setPadding(horizontalPadding, dpToPx(28), horizontalPadding, dpToPx(22));
        card.setBackground(createRoundedDrawable(0xFF2A2A2A, dpToPx(28)));

        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_location_pin);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        icon.setBackground(createOvalDrawable(0xFF155AC6));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        iconParams.bottomMargin = dpToPx(22);
        card.addView(icon, iconParams);

        TextView title = new TextView(activity);
        title.setText(createLocationPermissionTitle(host));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(dpToPx(2), 1.0f);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dpToPx(26);
        card.addView(title, titleParams);

        TextView allowVisit = createLocationPermissionButton(
                activity.getString(R.string.site_permission_allow_while_visiting));
        allowVisit.setOnClickListener(v -> completeLocationPrompt(dialog, result,
                ContentPermission.VALUE_ALLOW, origin, permissionLabel, true, false, completed));
        card.addView(allowVisit, createButtonParams(dpToPx(0)));

        TextView allowOnce = createLocationPermissionButton(
                activity.getString(R.string.site_permission_allow_this_time));
        allowOnce.setOnClickListener(v -> completeLocationPrompt(dialog, result,
                ContentPermission.VALUE_ALLOW, origin, permissionLabel, false, false, completed));
        card.addView(allowOnce, createButtonParams(dpToPx(8)));

        TextView deny = createLocationPermissionButton(
                activity.getString(R.string.site_permission_never_allow));
        deny.setOnClickListener(v -> completeLocationPrompt(dialog, result,
                ContentPermission.VALUE_DENY, origin, permissionLabel, false, true, completed));
        card.addView(deny, createButtonParams(dpToPx(8)));

        dialog.setContentView(card);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = getPromptWidth();
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.62f;
            window.setAttributes(params);
        }
    }

    private SpannableString createLocationPermissionTitle(String host) {
        String title = activity.getString(R.string.site_location_permission_title, host);
        SpannableString spannable = new SpannableString(title);
        int hostStart = title.indexOf(host);
        if (hostStart >= 0) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), hostStart,
                    hostStart + host.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    private TextView createLocationPermissionButton(String text) {
        TextView button = new TextView(activity);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dpToPx(56));
        button.setIncludeFontPadding(false);
        button.setBackground(createRoundedDrawable(0xFF1453B8, dpToPx(14)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private LinearLayout.LayoutParams createButtonParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56));
        params.topMargin = topMargin;
        return params;
    }

    private void completeLocationPrompt(Dialog dialog, GeckoResult<Integer> result, int value,
                                        String origin, String permissionLabel,
                                        boolean rememberAllow, boolean rememberDeny,
                                        boolean[] completed) {
        if (completed[0]) {
            return;
        }
        completed[0] = true;
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (rememberAllow) {
            SitePermissionsActivity.recordPermission(prefs, origin, permissionLabel);
        } else if (rememberDeny) {
            SitePermissionsActivity.recordDeniedPermission(prefs, origin, permissionLabel);
        }
        result.complete(value);
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createOvalDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private int getPromptWidth() {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int width = screenWidth - dpToPx(88);
        int maxWidth = dpToPx(380);
        if (width > maxWidth) {
            return maxWidth;
        }
        return Math.max(width, dpToPx(260));
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
        String scheme = parsed.getScheme();
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(scheme)) {
            // data:, blob:, about: and other host-less URIs should never be shown
            // verbatim — the URI body can be used to spoof a legitimate origin.
            return activity.getString(R.string.unknown_site);
        }
        scheme = scheme.toLowerCase(Locale.US);
        host = host.toLowerCase(Locale.US);
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        StringBuilder origin = new StringBuilder(scheme)
                .append("://")
                .append(host);
        int port = parsed.getPort();
        if (port >= 0 && port != defaultPortForScheme(scheme)) {
            origin.append(':').append(port);
        }
        return origin.toString();
    }

    private String getDisplayHost(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return activity.getString(R.string.unknown_site);
        }
        try {
            Uri parsed = Uri.parse(uri);
            String host = parsed != null ? parsed.getHost() : null;
            if (!TextUtils.isEmpty(host)) {
                return host.toLowerCase(Locale.US);
            }
        } catch (Exception ignored) {
        }
        return getDisplayOrigin(uri);
    }

    private int defaultPortForScheme(String scheme) {
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        return -1;
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

    private String[] normalizeAndroidPermissions(@Nullable String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return new String[0];
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        boolean requestsLocation = false;
        for (String permission : permissions) {
            if (TextUtils.isEmpty(permission)) {
                continue;
            }
            if (isAndroidLocationPermission(permission)) {
                requestsLocation = true;
            }
            normalized.add(permission);
        }
        if (requestsLocation) {
            normalized.add(Manifest.permission.ACCESS_FINE_LOCATION);
            normalized.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return normalized.toArray(new String[0]);
    }

    private boolean isAndroidLocationPermission(String permission) {
        return Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)
                || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}
