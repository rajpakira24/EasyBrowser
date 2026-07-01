package com.webstudio.easybrowser.ui.downloads

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.webstudio.easybrowser.managers.AppDownloadManager
import com.webstudio.easybrowser.models.DownloadItem
import com.webstudio.easybrowser.repository.DownloadRepository
import java.io.File

enum class DownloadSort { DATE_NEWEST, DATE_OLDEST, NAME_ASC, NAME_DESC, SIZE_LARGEST, SIZE_SMALLEST }

/**
 * Backing state + actions for the Compose Downloads screen. Bridges [DownloadRepository] (Room)
 * and [AppDownloadManager]; the transition sequences mirror the previous Java DownloadsActivity so
 * pause/resume/retry behaviour is unchanged. File-open and share stay in the Activity (they need an
 * Activity context + FileProvider).
 */
class DownloadsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DownloadRepository(app)
    private val manager = AppDownloadManager.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    val downloads = mutableStateListOf<DownloadItem>()
    var sort by mutableStateOf(DownloadSort.DATE_NEWEST)
        private set

    private var resumed = false
    private val pollRunnable = Runnable { load() }

    fun onResume() {
        resumed = true
        load()
    }

    fun onPause() {
        resumed = false
        mainHandler.removeCallbacks(pollRunnable)
    }

    fun load() {
        repository.getAllDownloads(object : DownloadRepository.DownloadCallback {
            override fun onDownloadsLoaded(list: MutableList<DownloadItem>) {
                mainHandler.post {
                    downloads.clear()
                    downloads.addAll(applySort(list))
                    scheduleNextRefreshIfNeeded()
                }
            }

            override fun onDownloadUpdated(download: DownloadItem) {}
            override fun onDownloadRemoved(download: DownloadItem) {}
        })
    }

    // Only poll while something is actively moving and the screen is visible, so an idle list
    // doesn't hit the DB every second (matches the old DownloadsActivity behaviour).
    private fun scheduleNextRefreshIfNeeded() {
        mainHandler.removeCallbacks(pollRunnable)
        val active = downloads.any {
            it.status == DownloadItem.Status.DOWNLOADING || it.status == DownloadItem.Status.PENDING
        }
        if (resumed && active) {
            mainHandler.postDelayed(pollRunnable, 1000)
        }
    }

    fun updateSort(mode: DownloadSort) {
        sort = mode
        val sorted = applySort(downloads.toList())
        downloads.clear()
        downloads.addAll(sorted)
    }

    private fun applySort(list: List<DownloadItem>): List<DownloadItem> = when (sort) {
        DownloadSort.DATE_NEWEST -> list.sortedByDescending { it.startTime }
        DownloadSort.DATE_OLDEST -> list.sortedBy { it.startTime }
        DownloadSort.NAME_ASC -> list.sortedBy { it.fileName.lowercase() }
        DownloadSort.NAME_DESC -> list.sortedByDescending { it.fileName.lowercase() }
        DownloadSort.SIZE_LARGEST -> list.sortedByDescending { it.totalBytes }
        DownloadSort.SIZE_SMALLEST -> list.sortedBy { it.totalBytes }
    }

    fun pause(item: DownloadItem) {
        manager.pauseDownload(item.id)
        item.status = DownloadItem.Status.PAUSED
        repository.updateDownload(item, reloadOnUpdate())
    }

    fun resume(item: DownloadItem) {
        item.status = DownloadItem.Status.DOWNLOADING
        repository.saveDownload(item, startThenReload())
    }

    fun cancel(item: DownloadItem) {
        manager.cancelDownload(item.id)
        item.status = DownloadItem.Status.CANCELLED
        repository.updateDownload(item, reloadOnUpdate())
    }

    fun retry(item: DownloadItem) {
        item.status = DownloadItem.Status.DOWNLOADING
        item.errorMessage = null
        item.downloadedBytes = 0
        deleteDownloadedFile(item)
        repository.saveDownload(item, startThenReload())
    }

    fun startQueuedNow(item: DownloadItem) {
        manager.removeFromWifiQueue(item.id)
        item.status = DownloadItem.Status.PENDING
        repository.saveDownload(item, startThenReload())
    }

    fun delete(item: DownloadItem) {
        manager.cancelDownload(item.id)
        deleteDownloadedFile(item)
        repository.removeDownload(item, object : DownloadRepository.DownloadCallback {
            override fun onDownloadsLoaded(list: MutableList<DownloadItem>) {}
            override fun onDownloadUpdated(download: DownloadItem) {}
            override fun onDownloadRemoved(download: DownloadItem) {
                mainHandler.post { load() }
            }
        })
    }

    fun clearCompleted() {
        // clear + reload enqueue on the same serial DB executor, so ordering is guaranteed.
        repository.clearCompletedDownloads()
        load()
    }

    fun clearAll() {
        repository.clearAllDownloads()
        downloads.clear()
    }

    private fun reloadOnUpdate() = object : DownloadRepository.DownloadCallback {
        override fun onDownloadsLoaded(list: MutableList<DownloadItem>) {}
        override fun onDownloadUpdated(download: DownloadItem) {
            mainHandler.post { load() }
        }
        override fun onDownloadRemoved(download: DownloadItem) {}
    }

    private fun startThenReload() = object : DownloadRepository.DownloadCallback {
        override fun onDownloadsLoaded(list: MutableList<DownloadItem>) {}
        override fun onDownloadUpdated(download: DownloadItem) {
            manager.startExistingDownload(getApplication<Application>(), download)
            mainHandler.post { load() }
        }
        override fun onDownloadRemoved(download: DownloadItem) {}
    }

    private fun deleteDownloadedFile(item: DownloadItem) {
        val path = item.destinationPath
        if (path.isNullOrBlank()) return
        if (path.startsWith("content://")) {
            try {
                getApplication<Application>().contentResolver.delete(Uri.parse(path), null, null)
            } catch (ignored: SecurityException) {
            } catch (ignored: IllegalArgumentException) {
            }
            return
        }
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
