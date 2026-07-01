package com.webstudio.easybrowser.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.webstudio.easybrowser.R
import com.webstudio.easybrowser.models.DownloadItem
import com.webstudio.easybrowser.ui.downloads.DownloadsScreen
import com.webstudio.easybrowser.ui.downloads.DownloadsViewModel
import java.io.File

/**
 * Jetpack Compose downloads screen. UI + list state live in [DownloadsScreen] / [DownloadsViewModel];
 * opening and sharing a finished file stay here because they need an Activity context + FileProvider.
 */
class DownloadsActivity : ComponentActivity() {

    private val viewModel: DownloadsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadsScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onOpenFile = ::openFile,
                onShareFile = ::shareFile,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

    private fun openFile(item: DownloadItem) {
        if (item.destinationPath == null) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri(item), item.mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_opening_file, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(item: DownloadItem) {
        if (item.destinationPath == null) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri(item))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun fileUri(item: DownloadItem): Uri {
        val path = item.destinationPath
        if (path != null && path.startsWith("content://")) {
            return Uri.parse(path)
        }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", File(path!!))
    }
}
