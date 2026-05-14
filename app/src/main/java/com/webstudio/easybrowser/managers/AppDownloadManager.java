package com.webstudio.easybrowser.managers;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.database.AppDatabase;
import com.webstudio.easybrowser.database.entity.DownloadEntity;
import com.webstudio.easybrowser.models.DownloadItem;
import com.webstudio.easybrowser.repository.DownloadRepository;
import com.webstudio.easybrowser.ui.activity.DownloadsActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppDownloadManager {
    private static final String DOWNLOAD_CHANNEL_ID = "downloads";
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 500;
    private static final long STALE_DOWNLOAD_AGE_MS = 60 * 60 * 1000;
    private static volatile AppDownloadManager instance;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // Follow redirects only when they stay on HTTPS — refuse HTTP-downgrade
            // redirects to keep MITM-flipped downloads from succeeding silently.
            .followSslRedirects(true)
            .followRedirects(true)
            .build();
    private final ConcurrentHashMap<String, Call> activeCalls = new ConcurrentHashMap<>();
    private final Set<String> pausedDownloads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> cancelledDownloads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> wifiPendingQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile ConnectivityManager.NetworkCallback networkCallback = null;

    public static AppDownloadManager getInstance() {
        if (instance == null) {
            synchronized (AppDownloadManager.class) {
                if (instance == null) {
                    instance = new AppDownloadManager();
                }
            }
        }
        return instance;
    }

    public void startDownload(Context context, String url, String fileName, String mimeType) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        cleanupStaleCacheDownloads(appContext);
        String resolvedFileName = !TextUtils.isEmpty(fileName)
                ? fileName
                : guessFileName(url, mimeType);
        File targetFile = createUniqueFile(appContext, resolvedFileName);
        if (targetFile == null) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadItem item = new DownloadItem(url, targetFile.getName(), mimeType);
        item.setDestinationPath(targetFile.getAbsolutePath());
        DownloadRepository repository = new DownloadRepository(appContext);

        boolean wifiOnly = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(appContext)
                .getBoolean("download_wifi_only", false);
        if (wifiOnly && isMetered(appContext)) {
            item.setStatus(DownloadItem.Status.QUEUED);
            repository.saveDownload(item, null);
            wifiPendingQueue.add(item.getId());
            registerNetworkCallbackIfNeeded(appContext);
            Toast.makeText(context, R.string.download_queued_wifi, Toast.LENGTH_SHORT).show();
            return;
        }

        item.setStatus(DownloadItem.Status.PENDING);
        repository.saveDownload(item, null);
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show();
        startExistingDownload(appContext, item);
    }

    public void startExistingDownload(Context context, DownloadItem item) {
        pausedDownloads.remove(item.getId());
        cancelledDownloads.remove(item.getId());
        executor.execute(() -> download(context.getApplicationContext(), item));
    }

    public void pauseDownload(String downloadId) {
        pausedDownloads.add(downloadId);
        Call call = activeCalls.get(downloadId);
        if (call != null) {
            call.cancel();
        }
    }

    public void cancelDownload(String downloadId) {
        cancelledDownloads.add(downloadId);
        Call call = activeCalls.get(downloadId);
        if (call != null) {
            call.cancel();
        }
    }

    private void download(Context context, DownloadItem item) {
        int limitBytesPerSec;
        try {
            limitBytesPerSec = Integer.parseInt(
                    androidx.preference.PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .getString("download_bandwidth_limit", "0"));
        } catch (NumberFormatException e) {
            limitBytesPerSec = 0;
        }
        DownloadRepository repository = new DownloadRepository(context);
        File outputFile = new File(item.getDestinationPath());
        long existingBytes = outputFile.exists() ? outputFile.length() : 0;
        boolean resume = existingBytes > 0 && item.getDownloadedBytes() > 0;

        item.setStatus(DownloadItem.Status.DOWNLOADING);
        item.setDownloadedBytes(existingBytes);
        repository.saveDownload(item, null);

        Request.Builder requestBuilder = new Request.Builder()
                .url(item.getUrl())
                .header("User-Agent", "Mozilla/5.0 (Android) EasyBrowser");
        if (resume) {
            requestBuilder.header("Range", "bytes=" + existingBytes + "-");
        }

        Call call = client.newCall(requestBuilder.build());
        activeCalls.put(item.getId(), call);

        try (Response response = call.execute()) {
            if (!response.isSuccessful() && response.code() != 206) {
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response");
            }

            String responseMimeType = normalizeMimeType(response.header("content-type"));
            String resolvedFileName = resolveFileName(
                    item.getUrl(),
                    item.getFileName(),
                    response.header("content-disposition"),
                    responseMimeType != null ? responseMimeType : item.getMimeType());
            File resolvedFile = maybeRenameTargetFile(context, outputFile, item, resolvedFileName);
            if (resolvedFile != null) {
                outputFile = resolvedFile;
            }
            if (!TextUtils.isEmpty(responseMimeType)) {
                item.setMimeType(responseMimeType);
            }

            // A 206 response is the only case where the server honored our Range
            // request and we may safely append to the local file. If the server
            // returned 200 (or anything else with a body), it ignored Range and is
            // sending the file from the beginning — we must truncate or we will
            // append fresh bytes onto stale partial data and corrupt the file.
            // Additionally verify Content-Range starts at the byte we asked for,
            // so a server that returns 206 with a different range can't trick us.
            boolean append = false;
            if (resume && response.code() == 206) {
                String contentRange = response.header("Content-Range");
                String expectedPrefix = "bytes " + existingBytes + "-";
                if (contentRange != null
                        && contentRange.toLowerCase(Locale.US).startsWith(expectedPrefix.toLowerCase(Locale.US))) {
                    append = true;
                }
            }
            if (resume && !append) {
                existingBytes = 0;
                // Truncate the partial file so write-from-byte-0 does not append onto stale data.
                try (FileOutputStream truncate = new FileOutputStream(outputFile, false)) {
                    // Opened in non-append mode to zero out; nothing to write here.
                    truncate.getChannel().truncate(0);
                } catch (IOException ignored) {
                }
            }
            long contentLength = body.contentLength();
            long totalBytes = contentLength > 0 ? existingBytes + contentLength : item.getTotalBytes();
            item.setTotalBytes(totalBytes);

            try (InputStream input = body.byteStream();
                 FileOutputStream output = new FileOutputStream(outputFile, append)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long downloadedBytes = existingBytes;
                long lastProgressUpdate = 0;
                long speedWindowStartTime = System.currentTimeMillis();
                long speedWindowStartBytes = downloadedBytes;
                long throttleWindowStart = speedWindowStartTime;
                long throttleWindowBytes = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (pausedDownloads.contains(item.getId())) {
                        markPaused(context, repository, item, downloadedBytes);
                        return;
                    }
                    if (cancelledDownloads.contains(item.getId())) {
                        markCancelled(context, repository, item, outputFile);
                        return;
                    }
                    output.write(buffer, 0, read);
                    downloadedBytes += read;
                    throttleWindowBytes += read;
                    throttleIfNeeded(throttleWindowBytes, throttleWindowStart, limitBytesPerSec);
                    long nowThrottle = System.currentTimeMillis();
                    if (nowThrottle - throttleWindowStart >= 1000) {
                        throttleWindowStart = nowThrottle;
                        throttleWindowBytes = 0;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                        long speedBytesPerSecond = calculateSpeed(
                                downloadedBytes - speedWindowStartBytes,
                                now - speedWindowStartTime);
                        long remainingSeconds = calculateRemainingSeconds(
                                totalBytes, downloadedBytes, speedBytesPerSecond);
                        item.setDownloadedBytes(downloadedBytes);
                        item.setTotalBytes(totalBytes);
                        item.setSpeedBytesPerSecond(speedBytesPerSecond);
                        item.setRemainingSeconds(remainingSeconds);
                        item.setStatus(DownloadItem.Status.DOWNLOADING);
                        repository.saveDownload(item, null);
                        showProgressNotification(context, item);
                        lastProgressUpdate = now;
                        speedWindowStartTime = now;
                        speedWindowStartBytes = downloadedBytes;
                    }
                }
                output.flush();
                item.setDownloadedBytes(downloadedBytes);
                item.setTotalBytes(totalBytes > 0 ? totalBytes : downloadedBytes);
                item.setSpeedBytesPerSecond(0);
                item.setRemainingSeconds(0);
                publishToDownloads(context, item, outputFile);
                item.setStatus(DownloadItem.Status.COMPLETED);
                item.setErrorMessage(null);
                repository.saveDownload(item, null);
                showCompletedNotification(context, item);
            }
        } catch (IOException e) {
            if (pausedDownloads.contains(item.getId())) {
                markPaused(context, repository, item, outputFile.exists() ? outputFile.length() : item.getDownloadedBytes());
            } else if (cancelledDownloads.contains(item.getId())) {
                markCancelled(context, repository, item, outputFile);
            } else {
                item.setStatus(DownloadItem.Status.FAILED);
                item.setErrorMessage(e.getMessage());
                item.setDownloadedBytes(outputFile.exists() ? outputFile.length() : item.getDownloadedBytes());
                repository.saveDownload(item, null);
                showFailedNotification(context, item);
            }
        } finally {
            activeCalls.remove(item.getId());
        }
    }

    private void markPaused(Context context, DownloadRepository repository, DownloadItem item, long downloadedBytes) {
        item.setDownloadedBytes(downloadedBytes);
        item.setSpeedBytesPerSecond(0);
        item.setRemainingSeconds(0);
        item.setStatus(DownloadItem.Status.PAUSED);
        repository.saveDownload(item, null);
        cancelNotification(context, item);
        pausedDownloads.remove(item.getId());
    }

    private void markCancelled(Context context, DownloadRepository repository, DownloadItem item, File outputFile) {
        if (outputFile.exists()) {
            outputFile.delete();
        }
        item.setDownloadedBytes(0);
        item.setSpeedBytesPerSecond(0);
        item.setRemainingSeconds(0);
        item.setStatus(DownloadItem.Status.CANCELLED);
        repository.saveDownload(item, null);
        cancelNotification(context, item);
        cancelledDownloads.remove(item.getId());
    }

    private File maybeRenameTargetFile(Context context, File currentFile, DownloadItem item,
                                       String resolvedFileName) {
        if (TextUtils.isEmpty(resolvedFileName) || resolvedFileName.equals(item.getFileName())) {
            return null;
        }
        File newFile = createUniqueFile(context, resolvedFileName);
        if (newFile == null) {
            return null;
        }
        boolean hasExistingData = currentFile.exists() && currentFile.length() > 0;
        if (hasExistingData && !currentFile.renameTo(newFile)) {
            return null;
        }
        item.setFileName(newFile.getName());
        item.setDestinationPath(newFile.getAbsolutePath());
        return newFile;
    }

    private long calculateSpeed(long bytes, long elapsedMs) {
        if (bytes <= 0 || elapsedMs <= 0) {
            return 0;
        }
        return (bytes * 1000L) / elapsedMs;
    }

    private long calculateRemainingSeconds(long totalBytes, long downloadedBytes, long speedBytesPerSecond) {
        if (totalBytes <= 0 || downloadedBytes <= 0 || speedBytesPerSecond <= 0) {
            return 0;
        }
        long remainingBytes = Math.max(0, totalBytes - downloadedBytes);
        return remainingBytes / speedBytesPerSecond;
    }

    private File createUniqueFile(Context context, String fileName) {
        File downloadDir = new File(context.getCacheDir(), "downloads");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            return null;
        }

        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        File file = new File(downloadDir, safeName);
        if (!file.exists()) {
            return file;
        }

        String baseName = safeName;
        String extension = "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeName.substring(0, dotIndex);
            extension = safeName.substring(dotIndex);
        }
        int index = 1;
        do {
            file = new File(downloadDir, baseName + " (" + index + ")" + extension);
            index++;
        } while (file.exists());
        return file;
    }

    private void cleanupStaleCacheDownloads(Context context) {
        executor.execute(() -> {
            File downloadDir = new File(context.getCacheDir(), "downloads");
            if (!downloadDir.isDirectory()) {
                return;
            }

            AppDatabase database = AppDatabase.getInstance(context);
            Set<String> keepPaths = new HashSet<>();
            long staleBefore = System.currentTimeMillis() - STALE_DOWNLOAD_AGE_MS;
            for (DownloadEntity entity : database.downloadDao().getAllDownloads()) {
                String destinationPath = entity.getDestinationPath();
                if (TextUtils.isEmpty(destinationPath) || !isCacheDownloadPath(downloadDir, destinationPath)) {
                    continue;
                }
                DownloadItem.Status status = DownloadItem.Status.valueOf(entity.getStatus());
                if (status == DownloadItem.Status.PAUSED) {
                    keepPaths.add(new File(destinationPath).getAbsolutePath());
                    continue;
                }
                if ((status == DownloadItem.Status.PENDING || status == DownloadItem.Status.DOWNLOADING)
                        && entity.getLastModified() > staleBefore) {
                    keepPaths.add(new File(destinationPath).getAbsolutePath());
                    continue;
                }
                File staleFile = new File(destinationPath);
                if (staleFile.exists()) {
                    staleFile.delete();
                }
                if (status == DownloadItem.Status.PENDING || status == DownloadItem.Status.DOWNLOADING) {
                    entity.setStatus(DownloadItem.Status.FAILED.name());
                    entity.setErrorMessage("Download interrupted");
                    entity.setDownloadedBytes(0);
                    entity.setSpeedBytesPerSecond(0);
                    entity.setRemainingSeconds(0);
                    database.downloadDao().update(entity);
                }
            }

            File[] files = downloadDir.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file.isFile()
                        && file.lastModified() <= staleBefore
                        && !keepPaths.contains(file.getAbsolutePath())) {
                    file.delete();
                }
            }
        });
    }

    private boolean isCacheDownloadPath(File downloadDir, String path) {
        try {
            String cachePath = downloadDir.getCanonicalPath();
            String filePath = new File(path).getCanonicalPath();
            return filePath.equals(cachePath) || filePath.startsWith(cachePath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private void publishToDownloads(Context context, DownloadItem item, File sourceFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            publishWithMediaStore(context, item, sourceFile);
        } else {
            publishToPublicDownloadsLegacy(item, sourceFile);
        }
    }

    @androidx.annotation.RequiresApi(29)
    private void publishWithMediaStore(Context context, DownloadItem item, File sourceFile) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, item.getFileName());
        values.put(MediaStore.Downloads.MIME_TYPE, !TextUtils.isEmpty(item.getMimeType())
                ? item.getMimeType()
                : "application/octet-stream");
        String customFolder = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString("downloads_folder_custom", "");
        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/"
                + (customFolder.isEmpty() ? "Easy Browser" : customFolder);
        values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Cannot create public download");
        }
        try (InputStream input = new java.io.FileInputStream(sourceFile);
             java.io.OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Cannot open public download");
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        item.setDestinationPath(uri.toString());
        sourceFile.delete();
    }

    private void publishToPublicDownloadsLegacy(DownloadItem item, File sourceFile) throws IOException {
        File publicDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Easy Browser");
        if (!publicDir.exists() && !publicDir.mkdirs()) {
            throw new IOException("Cannot create public Downloads folder");
        }
        File destination = createUniqueFileInDirectory(publicDir, item.getFileName());
        if (!sourceFile.renameTo(destination)) {
            try (InputStream input = new java.io.FileInputStream(sourceFile);
                 FileOutputStream output = new FileOutputStream(destination)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            sourceFile.delete();
        }
        item.setFileName(destination.getName());
        item.setDestinationPath(destination.getAbsolutePath());
    }

    private File createUniqueFileInDirectory(File directory, String fileName) {
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        File file = new File(directory, safeName);
        if (!file.exists()) {
            return file;
        }
        String baseName = safeName;
        String extension = "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeName.substring(0, dotIndex);
            extension = safeName.substring(dotIndex);
        }
        int index = 1;
        do {
            file = new File(directory, baseName + " (" + index + ")" + extension);
            index++;
        } while (file.exists());
        return file;
    }

    private String guessFileName(String url, String mimeType) {
        String name = getFileNameFromUrl(url);
        return ensureUsefulExtension(
                !TextUtils.isEmpty(name) ? name : "download_" + System.currentTimeMillis(),
                normalizeMimeType(mimeType));
    }

    private String resolveFileName(String url, String currentName, String contentDisposition,
                                   String mimeType) {
        String dispositionName = getFileNameFromContentDisposition(contentDisposition);
        if (!TextUtils.isEmpty(dispositionName)) {
            return ensureUsefulExtension(dispositionName, mimeType);
        }

        String urlName = getFileNameFromUrl(url);
        if (!TextUtils.isEmpty(urlName)) {
            return ensureUsefulExtension(urlName, mimeType);
        }

        return ensureUsefulExtension(currentName, mimeType);
    }

    private String getFileNameFromContentDisposition(String contentDisposition) {
        if (TextUtils.isEmpty(contentDisposition)) {
            return null;
        }
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            String lower = trimmed.toLowerCase(Locale.US);
            if (lower.startsWith("filename*=")) {
                int index = trimmed.indexOf("''");
                String encoded = index >= 0 ? trimmed.substring(index + 2) : trimmed.substring(10);
                return decodeFileName(encoded.replace("\"", ""));
            }
            if (lower.startsWith("filename=")) {
                return decodeFileName(trimmed.substring(9).replace("\"", ""));
            }
        }
        return null;
    }

    private String getFileNameFromUrl(String url) {
        android.net.Uri uri = android.net.Uri.parse(url);
        String lastPathSegment = uri.getLastPathSegment();
        if (!TextUtils.isEmpty(lastPathSegment) && lastPathSegment.contains(".")) {
            return decodeFileName(lastPathSegment);
        }
        for (String parameterName : uri.getQueryParameterNames()) {
            String value = uri.getQueryParameter(parameterName);
            if (!TextUtils.isEmpty(value) && value.contains(".")) {
                return decodeFileName(value);
            }
        }
        return !TextUtils.isEmpty(lastPathSegment) ? decodeFileName(lastPathSegment) : null;
    }

    private String decodeFileName(String value) {
        String decoded;
        try {
            decoded = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            decoded = value;
        }
        return sanitizeDecodedName(decoded);
    }

    /**
     * Reject path-traversal payloads and bidi-control spoofing after URL decoding.
     * createUniqueFile() escapes a few literal chars but the regex won't catch ".."
     * sequences once URL-decoded ("..%2F..%2F" → "../../"). Returning null/timestamp
     * here forces the caller into the safe fallback path.
     */
    private String sanitizeDecodedName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String stripped = name.replace("\\", "/");
        int lastSlash = stripped.lastIndexOf('/');
        if (lastSlash >= 0) {
            stripped = stripped.substring(lastSlash + 1);
        }
        StringBuilder cleaned = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == 0x202A || c == 0x202B || c == 0x202C || c == 0x202D || c == 0x202E
                    || c == 0x200E || c == 0x200F || c < 0x20 || c == 0x7F) {
                continue;
            }
            cleaned.append(c);
        }
        String result = cleaned.toString().trim();
        if (result.isEmpty() || ".".equals(result) || "..".equals(result)
                || result.startsWith(".") || result.contains("..")) {
            return "download_" + System.currentTimeMillis();
        }
        if (result.length() > 200) {
            result = result.substring(0, 200);
        }
        return result;
    }

    private String ensureUsefulExtension(String fileName, String mimeType) {
        String safeName = TextUtils.isEmpty(fileName)
                ? "download_" + System.currentTimeMillis()
                : fileName;
        int dotIndex = safeName.lastIndexOf('.');
        String currentExtension = dotIndex >= 0 ? safeName.substring(dotIndex + 1).toLowerCase(Locale.US) : "";
        String extension = getExtensionForMimeType(mimeType);
        boolean hasUsefulExtension = !TextUtils.isEmpty(currentExtension) && !"bin".equals(currentExtension);
        if (hasUsefulExtension || TextUtils.isEmpty(extension)) {
            return safeName;
        }
        if (dotIndex >= 0) {
            safeName = safeName.substring(0, dotIndex);
        }
        return safeName + "." + extension;
    }

    private String normalizeMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }
        return mimeType.split(";")[0].trim().toLowerCase(Locale.US);
    }

    private String getExtensionForMimeType(String mimeType) {
        mimeType = normalizeMimeType(mimeType);
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }
        switch (mimeType) {
            case "audio/mpeg":
            case "audio/mp3":
                return "mp3";
            case "audio/mp4":
            case "audio/x-m4a":
                return "m4a";
            case "audio/wav":
            case "audio/x-wav":
                return "wav";
            case "audio/ogg":
                return "ogg";
            case "video/mp4":
                return "mp4";
            case "video/x-matroska":
                return "mkv";
            case "video/webm":
                return "webm";
            case "video/x-msvideo":
                return "avi";
            case "application/pdf":
                return "pdf";
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/svg+xml":
                return "svg";
            case "application/zip":
            case "application/x-zip-compressed":
                return "zip";
            case "application/x-rar-compressed":
            case "application/vnd.rar":
                return "rar";
            case "application/x-7z-compressed":
                return "7z";
            case "application/vnd.android.package-archive":
                return "apk";
            case "text/plain":
                return "txt";
            case "text/html":
                return "html";
            default:
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                return !TextUtils.isEmpty(extension) && !"bin".equals(extension) ? extension : null;
        }
    }

    private void showProgressNotification(Context context, DownloadItem item) {
        if (!canPostNotifications(context)) {
            return;
        }
        createNotificationChannel(context);
        int progress = item.getProgress();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(item.getFileName())
                .setContentText(formatFileSize(item.getSpeedBytesPerSecond()) + "/s • "
                        + formatRemainingTime(item.getRemainingSeconds()) + " left")
                .setContentIntent(createDownloadsPendingIntent(context))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, progress, item.getTotalBytes() <= 0);
        notify(context, item, builder);
    }

    private void showCompletedNotification(Context context, DownloadItem item) {
        if (!canPostNotifications(context)) {
            return;
        }
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(context.getString(R.string.download_complete))
                .setContentText(item.getFileName())
                .setContentIntent(createDownloadsPendingIntent(context))
                .setAutoCancel(true)
                .setOngoing(false);
        notify(context, item, builder);
    }

    private void showFailedNotification(Context context, DownloadItem item) {
        if (!canPostNotifications(context)) {
            return;
        }
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(context.getString(R.string.download_failed))
                .setContentText(item.getFileName())
                .setContentIntent(createDownloadsPendingIntent(context))
                .setAutoCancel(true)
                .setOngoing(false);
        notify(context, item, builder);
    }

    private void cancelNotification(Context context, DownloadItem item) {
        if (!canPostNotifications(context)) {
            return;
        }
        NotificationManagerCompat.from(context).cancel(getNotificationId(item));
    }

    private void notify(Context context, DownloadItem item, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(getNotificationId(item), builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private int getNotificationId(DownloadItem item) {
        return Math.abs(item.getId().hashCode());
    }

    private boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                context.getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.notification_channel_downloads_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createDownloadsPendingIntent(Context context) {
        Intent intent = new Intent(context, DownloadsActivity.class);
        // FLAG_IMMUTABLE is a no-op below API 23 but is safe to OR in unconditionally,
        // and it prevents other apps on API 23+ from mutating the intent's extras.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "--";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void throttleIfNeeded(long bytesInWindow, long windowStartMs, int limitBytesPerSec) {
        if (limitBytesPerSec <= 0) return;
        long expectedMs = (bytesInWindow * 1000L) / limitBytesPerSec;
        long actualMs = System.currentTimeMillis() - windowStartMs;
        if (actualMs < expectedMs) {
            try {
                Thread.sleep(expectedMs - actualMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isMetered(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.isActiveNetworkMetered();
    }

    public void removeFromWifiQueue(String downloadId) {
        wifiPendingQueue.remove(downloadId);
    }

    private synchronized void registerNetworkCallbackIfNeeded(Context ctx) {
        if (networkCallback != null) return;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    drainWifiQueue(ctx.getApplicationContext());
                }
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        try {
            cm.registerNetworkCallback(request, networkCallback);
        } catch (Exception ignored) {
        }
    }

    private synchronized void drainWifiQueue(Context ctx) {
        if (wifiPendingQueue.isEmpty()) return;
        DownloadRepository repository = new DownloadRepository(ctx);
        repository.getAllDownloads(new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(java.util.List<DownloadItem> downloads) {
                for (DownloadItem item : downloads) {
                    if (wifiPendingQueue.contains(item.getId())) {
                        item.setStatus(DownloadItem.Status.PENDING);
                        repository.saveDownload(item, null);
                        startExistingDownload(ctx, item);
                    }
                }
                wifiPendingQueue.clear();
                unregisterNetworkCallback(ctx);
            }

            @Override public void onDownloadUpdated(DownloadItem download) {}
            @Override public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private synchronized void unregisterNetworkCallback(Context ctx) {
        if (networkCallback == null) return;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
        networkCallback = null;
    }

    private String formatRemainingTime(long seconds) {
        if (seconds <= 0) return "--";
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
