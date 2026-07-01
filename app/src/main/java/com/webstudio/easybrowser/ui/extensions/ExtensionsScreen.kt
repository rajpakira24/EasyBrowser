package com.webstudio.easybrowser.ui.extensions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9B6F5),
    onPrimary = Color(0xFF332155),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF2A2930),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFB6B3BB),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: ExtensionsViewModel,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    MaterialTheme(colorScheme = DarkColors) {
        val detailState = viewModel.detail
        if (detailState != null) {
            ExtensionDetailScreen(
                detail = detailState,
                loading = viewModel.detailLoading,
                onBack = { viewModel.closeDetail() },
                onInstall = {
                    viewModel.install(detailState.installUrl)
                    viewModel.closeDetail()
                },
                onOpenUrl = onOpenUrl,
            )
        } else {
        val snackbarHostState = remember { SnackbarHostState() }
        var showInstallDialog by remember { mutableStateOf(false) }

        val status = viewModel.statusMessage
        LaunchedEffect(status) {
            if (status != null) {
                snackbarHostState.showSnackbar(status)
                viewModel.consumeStatus()
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Extensions", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showInstallDialog = true }) {
                            Text("Install URL")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (viewModel.installed.isNotEmpty()) {
                    item { SectionHeader("Enabled") }
                    items(viewModel.installed, key = { "enabled_" + it.id }) { item ->
                        EnabledRow(
                            item = item,
                            icon = viewModel.installedIcons[item.id],
                            onOpen = { item.optionsUrl?.let(onOpenUrl) },
                            onToggle = { viewModel.setEnabled(item, !item.enabled) },
                            onUpdate = { viewModel.update(item) },
                            onRemove = { viewModel.uninstall(item) },
                            onClick = { viewModel.openDetail(item) },
                        )
                    }
                }

                item { SectionHeader("Recommended") }

                if (viewModel.isLoadingStore) {
                    item { LoadingRow() }
                } else if (viewModel.storeError) {
                    item {
                        MessageRow("Couldn't load extensions.", "Retry") { viewModel.loadRecommended() }
                    }
                } else {
                    // Hide add-ons already installed (they appear in Enabled above) and prefix the
                    // key so it can't collide with an Enabled row sharing the same add-on id.
                    val store = viewModel.recommended.filterNot { it.installed }
                    items(store, key = { "store_" + it.id.ifEmpty { it.name } }) { item ->
                        RecommendedRow(
                            item = item,
                            onInstall = { viewModel.install(item.installUrl) },
                            onClick = { viewModel.openDetail(item) },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onOpenUrl("https://addons.mozilla.org/android/") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text("Find more extensions")
                    }
                }
            }
        }

        if (showInstallDialog) {
            InstallUrlDialog(
                onDismiss = { showInstallDialog = false },
                onConfirm = { url ->
                    showInstallDialog = false
                    viewModel.install(url)
                },
            )
        }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EnabledRow(
    item: InstalledExtension,
    icon: ImageBitmap?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onUpdate: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Icon(
                    Icons.Filled.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.description.isNotBlank()) {
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!item.enabled) {
                Text(
                    "Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                RatingRow(item.rating, item.reviewCount)
            }
        }
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (item.enabled && item.optionsUrl != null) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { menuOpen = false; onOpen() })
                }
                DropdownMenuItem(
                    text = { Text(if (item.enabled) "Disable" else "Enable") },
                    onClick = { menuOpen = false; onToggle() },
                )
                DropdownMenuItem(
                    text = { Text("Check for updates") },
                    onClick = { menuOpen = false; onUpdate() },
                )
                DropdownMenuItem(text = { Text("Remove") }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}

@Composable
private fun RecommendedRow(item: StoreExtension, onInstall: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        AsyncImage(
            model = item.iconUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.summary.isNotBlank()) {
                Text(
                    item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RatingRow(item.rating, item.reviewCount)
        }
        IconButton(onClick = onInstall, enabled = !item.installed) {
            Icon(
                if (item.installed) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (item.installed) "Installed" else "Install",
                tint = if (item.installed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun RatingRow(rating: Double, reviewCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        for (i in 1..5) {
            val icon = when {
                rating >= i -> Icons.Filled.Star
                rating >= i - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
        }
        if (reviewCount > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                "Reviews: ${"%,d".format(reviewCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun MessageRow(message: String, action: String, onAction: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onAction) { Text(action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionDetailScreen(
    detail: ExtensionDetail,
    loading: Boolean,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(detail.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (detail.iconUrl != null) {
                    AsyncImage(
                        model = detail.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        detail.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (detail.author.isNotBlank()) {
                        Text(
                            "by ${detail.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            RatingRow(detail.rating, detail.reviewCount)
            Spacer(Modifier.height(16.dp))

            if (!detail.installed) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Add to Easy Browser") }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Installed") }
            }
            Spacer(Modifier.height(16.dp))

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (detail.description.isNotBlank()) {
                Text(
                    detail.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
            }

            if (detail.releaseNotes.isNotBlank()) {
                Text(
                    "What's new in ${detail.version.ifBlank { "this version" }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    detail.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            if (detail.author.isNotBlank()) DetailRow("Author", detail.author)
            if (detail.version.isNotBlank()) DetailRow("Version", detail.version)
            if (detail.lastUpdated.isNotBlank()) DetailRow("Last updated", detail.lastUpdated)
            detail.homepage?.let { DetailLinkRow("Homepage", it, onOpenUrl) }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { onOpenUrl(detail.amoUrl) }) {
                Text("More about this extension")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun DetailLinkRow(label: String, url: String, onOpenUrl: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(url) }
            .padding(vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text("Open", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun InstallUrlDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install from URL") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                label = { Text("Extension .xpi URL") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url.trim()) }, enabled = url.isNotBlank()) {
                Text("Install")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
