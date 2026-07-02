package com.webstudio.easybrowser.ui.downloads

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.webstudio.easybrowser.R
import com.webstudio.easybrowser.models.DownloadItem
import java.io.File
import java.util.Locale

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FC7FF),
    onPrimary = Color(0xFF00344F),
    background = Color(0xFF15181C),
    surface = Color(0xFF1D2126),
    surfaceVariant = Color(0xFF2A2F35),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFB6B3BB),
)

private val StatusGray = Color(0xFF9AA0A6)
private val StatusGreen = Color(0xFF4DAA89)
private val StatusAmber = Color(0xFFE0A82E)
private val StatusRed = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onBack: () -> Unit,
    onOpenFile: (DownloadItem) -> Unit,
    onShareFile: (DownloadItem) -> Unit,
) {
    MaterialTheme(colorScheme = DarkColors) {
        var confirmCancel by remember { mutableStateOf<DownloadItem?>(null) }
        var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
        var confirmRedownload by remember { mutableStateOf<DownloadItem?>(null) }
        var detailsItem by remember { mutableStateOf<DownloadItem?>(null) }
        var confirmClearCompleted by remember { mutableStateOf(false) }
        var confirmClearAll by remember { mutableStateOf(false) }
        var searchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val searchFocusRequester = remember { FocusRequester() }

        val visibleDownloads = if (searchQuery.isBlank()) {
            viewModel.downloads
        } else {
            viewModel.downloads.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                placeholder = { Text(stringResource(R.string.search_downloads)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                            )
                        } else {
                            Text(stringResource(R.string.downloads), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchActive) {
                                searchActive = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        if (searchActive) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            }
                        } else {
                            IconButton(onClick = { searchActive = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_downloads),
                                )
                            }
                            OverflowMenu(
                                currentSort = viewModel.sort,
                                onSort = { viewModel.updateSort(it) },
                                onClearCompleted = { confirmClearCompleted = true },
                                onClearAll = { confirmClearAll = true },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            if (visibleDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(
                            if (searchQuery.isNotBlank()) R.string.no_download_search_results
                            else R.string.no_downloads
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleDownloads, key = { it.id }) { item ->
                        val missing = item.status == DownloadItem.Status.COMPLETED &&
                            viewModel.fileExists[item.id] == false
                        DownloadRow(
                            item = item,
                            missing = missing,
                            onOpen = { onOpenFile(item) },
                            onShare = { onShareFile(item) },
                            onDetails = { detailsItem = item },
                            onConfirmCancel = { confirmCancel = item },
                            onConfirmDelete = { confirmDelete = item },
                            onConfirmRedownload = { confirmRedownload = item },
                            onPause = { viewModel.pause(item) },
                            onResume = { viewModel.resume(item) },
                            onRetry = { viewModel.retry(item) },
                            onStartNow = { viewModel.startQueuedNow(item) },
                        )
                    }
                }
            }
        }

        confirmCancel?.let { item ->
            ConfirmDialog(
                title = stringResource(R.string.download_cancel),
                message = stringResource(R.string.confirm_cancel_download, item.fileName),
                confirmText = stringResource(R.string.dialog_yes),
                dismissText = stringResource(R.string.dialog_no),
                onConfirm = { viewModel.cancel(item); confirmCancel = null },
                onDismiss = { confirmCancel = null },
            )
        }
        confirmDelete?.let { item ->
            ConfirmDialog(
                title = stringResource(R.string.delete_download),
                message = stringResource(R.string.confirm_delete_download, item.fileName),
                confirmText = stringResource(R.string.dialog_delete),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { viewModel.delete(item); confirmDelete = null },
                onDismiss = { confirmDelete = null },
            )
        }
        confirmRedownload?.let { item ->
            ConfirmDialog(
                title = stringResource(R.string.download_file_removed),
                message = stringResource(R.string.redownload_confirm_message, item.fileName),
                confirmText = stringResource(R.string.redownload),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { viewModel.redownload(item); confirmRedownload = null },
                onDismiss = { confirmRedownload = null },
            )
        }
        if (confirmClearCompleted) {
            ConfirmDialog(
                title = stringResource(R.string.clear_completed_downloads),
                message = stringResource(R.string.confirm_clear_completed),
                confirmText = stringResource(R.string.dialog_clear),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { viewModel.clearCompleted(); confirmClearCompleted = false },
                onDismiss = { confirmClearCompleted = false },
            )
        }
        if (confirmClearAll) {
            ConfirmDialog(
                title = stringResource(R.string.clear_all_downloads),
                message = stringResource(R.string.confirm_clear_all_downloads),
                confirmText = stringResource(R.string.dialog_clear),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { viewModel.clearAll(); confirmClearAll = false },
                onDismiss = { confirmClearAll = false },
            )
        }
        detailsItem?.let { item ->
            DetailsDialog(item = item, onDismiss = { detailsItem = null })
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    missing: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onConfirmCancel: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmRedownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onStartNow: () -> Unit,
) {
    val status = item.status
    val primary: () -> Unit = when {
        missing -> onConfirmRedownload
        status == DownloadItem.Status.COMPLETED -> onOpen
        status == DownloadItem.Status.PAUSED -> onResume
        status == DownloadItem.Status.FAILED -> onRetry
        status == DownloadItem.Status.QUEUED -> onStartNow
        status == DownloadItem.Status.PENDING -> onConfirmCancel
        status == DownloadItem.Status.DOWNLOADING -> onPause
        else -> onRetry
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = primary)
            .alpha(if (missing) 0.55f else 1f)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(item, missing)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatFileSize(item.totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "  •  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (missing) stringResource(R.string.download_file_removed) else statusText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (missing) StatusRed else statusColor(status),
                )
            }
            if (status == DownloadItem.Status.DOWNLOADING || status == DownloadItem.Status.PAUSED) {
                Spacer(Modifier.height(8.dp))
                val paused = status == DownloadItem.Status.PAUSED
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (paused) StatusGray else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (paused) {
                        "${item.progress}%"
                    } else {
                        "${item.progress}% • ${formatSpeed(item.speedBytesPerSecond)}/s • " +
                            "${formatTime(item.remainingSeconds)} left"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = primary) {
            Icon(
                painter = painterResource(
                    if (missing) R.drawable.ic_download else primaryActionIcon(status)
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        RowOverflow(
            status = status,
            missing = missing,
            onOpen = onOpen,
            onShare = onShare,
            onDelete = onConfirmDelete,
            onDetails = onDetails,
            onRedownload = onConfirmRedownload,
            onPause = onPause,
            onResume = onResume,
            onCancel = onConfirmCancel,
            onRetry = onRetry,
            onStartNow = onStartNow,
        )
    }
}

@Composable
private fun TypeBadge(item: DownloadItem, missing: Boolean = false) {
    val type = DownloadFileType.resolve(item.mimeType, item.fileName)
    // A removed file can't show a thumbnail and is grayed to read as inactive.
    val showThumb = !missing && type == DownloadFileType.IMAGE &&
        item.status == DownloadItem.Status.COMPLETED &&
        !item.destinationPath.isNullOrBlank()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    missing -> StatusGray
                    showThumb -> MaterialTheme.colorScheme.surfaceVariant
                    else -> type.tint
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showThumb) {
            val path = item.destinationPath!!
            val model: Any = if (path.startsWith("content://")) Uri.parse(path) else File(path)
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(type.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        if (missing) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = StatusRed,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun RowOverflow(
    status: DownloadItem.Status,
    missing: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onRedownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onStartNow: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.view_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            @Composable
            fun item(textRes: Int, action: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(stringResource(textRes)) },
                    onClick = { expanded = false; action() },
                )
            }
            when {
                // File removed from device: Open/Share are useless, offer to fetch it again.
                missing -> {
                    item(R.string.redownload, onRedownload)
                    item(R.string.delete, onDelete)
                }
                status == DownloadItem.Status.COMPLETED -> {
                    item(R.string.open_file, onOpen)
                    item(R.string.share, onShare)
                    item(R.string.delete, onDelete)
                }
                status == DownloadItem.Status.DOWNLOADING -> {
                    item(R.string.download_pause, onPause)
                    item(R.string.download_cancel, onCancel)
                }
                status == DownloadItem.Status.PAUSED -> {
                    item(R.string.download_resume, onResume)
                    item(R.string.download_cancel, onCancel)
                }
                status == DownloadItem.Status.FAILED -> {
                    item(R.string.retry_download, onRetry)
                    item(R.string.delete, onDelete)
                }
                status == DownloadItem.Status.QUEUED -> {
                    item(R.string.download_start_now, onStartNow)
                    item(R.string.delete, onDelete)
                }
                else -> {
                    item(R.string.delete, onDelete)
                }
            }
            item(R.string.view_details, onDetails)
        }
    }
}

@Composable
private fun OverflowMenu(
    currentSort: DownloadSort,
    onSort: (DownloadSort) -> Unit,
    onClearCompleted: () -> Unit,
    onClearAll: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_sort))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clear_completed_downloads)) },
            onClick = { expanded = false; onClearCompleted() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clear_all_downloads)) },
            onClick = { expanded = false; onClearAll() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sort)) },
            onClick = { expanded = false; sortExpanded = true },
        )
    }
    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
        @Composable
        fun sortItem(textRes: Int, mode: DownloadSort) {
            DropdownMenuItem(
                text = { Text(stringResource(textRes)) },
                onClick = { sortExpanded = false; expanded = false; onSort(mode) },
            )
        }
        sortItem(R.string.sort_by_date_newest, DownloadSort.DATE_NEWEST)
        sortItem(R.string.sort_by_date_oldest, DownloadSort.DATE_OLDEST)
        sortItem(R.string.sort_by_name_asc, DownloadSort.NAME_ASC)
        sortItem(R.string.sort_by_name_desc, DownloadSort.NAME_DESC)
        sortItem(R.string.sort_by_size_largest, DownloadSort.SIZE_LARGEST)
        sortItem(R.string.sort_by_size_smallest, DownloadSort.SIZE_SMALLEST)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
    )
}

@Composable
private fun DetailsDialog(item: DownloadItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_details)) },
        text = {
            Column {
                DetailLine(stringResource(R.string.file_size), formatFileSize(item.totalBytes))
                DetailLine(stringResource(R.string.status), item.status.name)
                DetailLine(stringResource(R.string.url), item.url)
                DetailLine("%", "${item.progress}%")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) } },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun primaryActionIcon(status: DownloadItem.Status): Int = when (status) {
    DownloadItem.Status.PENDING -> R.drawable.ic_close
    DownloadItem.Status.DOWNLOADING -> R.drawable.ic_pause
    DownloadItem.Status.PAUSED -> R.drawable.ic_play
    DownloadItem.Status.COMPLETED -> R.drawable.ic_open_in_new
    DownloadItem.Status.FAILED -> R.drawable.ic_retry
    DownloadItem.Status.QUEUED -> R.drawable.ic_play
    DownloadItem.Status.CANCELLED -> R.drawable.ic_retry
}

@Composable
private fun statusText(item: DownloadItem): String = when (item.status) {
    DownloadItem.Status.PENDING -> stringResource(R.string.download_pending)
    DownloadItem.Status.DOWNLOADING -> stringResource(R.string.download_in_progress)
    DownloadItem.Status.PAUSED -> stringResource(R.string.download_paused)
    DownloadItem.Status.COMPLETED -> stringResource(R.string.download_complete)
    DownloadItem.Status.FAILED ->
        item.errorMessage ?: stringResource(R.string.download_failed)
    DownloadItem.Status.CANCELLED -> stringResource(R.string.download_cancelled)
    DownloadItem.Status.QUEUED -> stringResource(R.string.download_queued)
}

private fun statusColor(status: DownloadItem.Status): Color = when (status) {
    DownloadItem.Status.DOWNLOADING -> StatusGreen
    DownloadItem.Status.COMPLETED -> StatusGreen
    DownloadItem.Status.PAUSED -> StatusAmber
    DownloadItem.Status.FAILED -> StatusRed
    else -> StatusGray
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.2f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

private fun formatSpeed(bytesPerSecond: Long): String =
    if (bytesPerSecond <= 0) "--" else formatFileSize(bytesPerSecond)

private fun formatTime(seconds: Long): String = when {
    seconds <= 0 -> "--"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}
