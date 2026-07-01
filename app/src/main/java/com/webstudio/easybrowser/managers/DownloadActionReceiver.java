package com.webstudio.easybrowser.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * Handles the Pause / Resume / Cancel actions on download notifications. The actual state transition and
 * notification cleanup happen inside {@link AppDownloadManager}'s download loop; this receiver
 * just forwards the user's intent.
 */
public class DownloadActionReceiver extends BroadcastReceiver {
    static final String ACTION_PAUSE = "com.webstudio.easybrowser.action.DOWNLOAD_PAUSE";
    static final String ACTION_RESUME = "com.webstudio.easybrowser.action.DOWNLOAD_RESUME";
    static final String ACTION_CANCEL = "com.webstudio.easybrowser.action.DOWNLOAD_CANCEL";
    static final String EXTRA_DOWNLOAD_ID = "download_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID);
        if (TextUtils.isEmpty(downloadId)) {
            return;
        }
        AppDownloadManager manager = AppDownloadManager.getInstance();
        if (ACTION_PAUSE.equals(intent.getAction())) {
            manager.pauseDownload(downloadId);
        } else if (ACTION_RESUME.equals(intent.getAction())) {
            manager.resumeDownload(context, downloadId);
        } else if (ACTION_CANCEL.equals(intent.getAction())) {
            manager.cancelDownload(downloadId);
        }
    }
}
