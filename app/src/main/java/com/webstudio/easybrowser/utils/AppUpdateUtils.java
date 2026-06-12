package com.webstudio.easybrowser.utils;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

public final class AppUpdateUtils {
    public interface Callback {
        void onNoUpdateAvailable();

        void onUpdateCheckFailed();

        void onFlexibleUpdateDownloaded(Runnable completeUpdate);
    }

    private final AppUpdateManager appUpdateManager;
    private InstallStateUpdatedListener installStateUpdatedListener;

    public AppUpdateUtils(Activity activity) {
        appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    public void checkForUpdates(ActivityResultLauncher<IntentSenderRequest> launcher,
                                Callback callback) {
        appUpdateManager.getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo ->
                        handleUpdateInfo(appUpdateInfo, launcher, callback, true))
                .addOnFailureListener(error -> notifyUpdateCheckFailed(callback));
    }

    public void resumePendingUpdate(ActivityResultLauncher<IntentSenderRequest> launcher,
                                    Callback callback) {
        appUpdateManager.getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        notifyFlexibleUpdateDownloaded(callback);
                    } else if (appUpdateInfo.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        startUpdateFlow(appUpdateInfo, launcher, AppUpdateType.IMMEDIATE,
                                callback);
                    }
                });
    }

    public void release() {
        unregisterFlexibleListener();
    }

    private void handleUpdateInfo(AppUpdateInfo appUpdateInfo,
                                  ActivityResultLauncher<IntentSenderRequest> launcher,
                                  Callback callback,
                                  boolean notifyNoUpdate) {
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            notifyFlexibleUpdateDownloaded(callback);
            return;
        }
        if (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            startUpdateFlow(appUpdateInfo, launcher, AppUpdateType.IMMEDIATE, callback);
            return;
        }
        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            if (notifyNoUpdate && callback != null) {
                callback.onNoUpdateAvailable();
            }
            return;
        }

        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            registerFlexibleListener(callback);
            startUpdateFlow(appUpdateInfo, launcher, AppUpdateType.FLEXIBLE, callback);
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startUpdateFlow(appUpdateInfo, launcher, AppUpdateType.IMMEDIATE, callback);
        } else {
            notifyUpdateCheckFailed(callback);
        }
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo,
                                 ActivityResultLauncher<IntentSenderRequest> launcher,
                                 int updateType,
                                 Callback callback) {
        if (launcher == null) {
            notifyUpdateCheckFailed(callback);
            return;
        }
        try {
            boolean started = appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    launcher,
                    AppUpdateOptions.newBuilder(updateType).build());
            if (!started) {
                notifyUpdateCheckFailed(callback);
            }
        } catch (RuntimeException e) {
            notifyUpdateCheckFailed(callback);
        }
    }

    private void registerFlexibleListener(Callback callback) {
        unregisterFlexibleListener();
        installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                unregisterFlexibleListener();
                notifyFlexibleUpdateDownloaded(callback);
            } else if (state.installStatus() == InstallStatus.FAILED) {
                unregisterFlexibleListener();
                notifyUpdateCheckFailed(callback);
            } else if (state.installStatus() == InstallStatus.CANCELED) {
                unregisterFlexibleListener();
            }
        };
        try {
            appUpdateManager.registerListener(installStateUpdatedListener);
        } catch (RuntimeException e) {
            installStateUpdatedListener = null;
            notifyUpdateCheckFailed(callback);
        }
    }

    private void unregisterFlexibleListener() {
        if (installStateUpdatedListener == null) {
            return;
        }
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        } catch (RuntimeException ignored) {
        } finally {
            installStateUpdatedListener = null;
        }
    }

    private void notifyFlexibleUpdateDownloaded(Callback callback) {
        if (callback != null) {
            callback.onFlexibleUpdateDownloaded(() -> appUpdateManager.completeUpdate());
        }
    }

    private void notifyUpdateCheckFailed(Callback callback) {
        if (callback != null) {
            callback.onUpdateCheckFailed();
        }
    }
}
