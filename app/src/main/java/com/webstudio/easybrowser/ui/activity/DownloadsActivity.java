package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.DownloadsAdapter;
import com.webstudio.easybrowser.managers.AppDownloadManager;
import com.webstudio.easybrowser.models.DownloadItem;
import com.webstudio.easybrowser.repository.DownloadRepository;

import java.util.ArrayList;
import java.io.File;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity implements DownloadsAdapter.OnDownloadClickListener {
    private RecyclerView downloadsRecycler;
    private DownloadsAdapter adapter;
    private TextView emptyView;
    private DownloadRepository repository;
    private List<DownloadItem> downloadItems;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshDownloadsRunnable = new Runnable() {
        @Override
        public void run() {
            loadDownloads();
            refreshHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadDownloads();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshDownloadsRunnable);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshDownloadsRunnable);
        super.onPause();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.downloads);
        }
    }

    private void initializeViews() {
        downloadsRecycler = findViewById(R.id.downloads_recycler);
        emptyView = findViewById(R.id.empty_view);

        repository = new DownloadRepository(this);
        downloadItems = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new DownloadsAdapter(downloadItems, this);
        downloadsRecycler.setLayoutManager(new LinearLayoutManager(this));
        downloadsRecycler.setAdapter(adapter);
    }

    private void loadDownloads() {
        repository.getAllDownloads(new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {
                runOnUiThread(() -> {
                    downloadItems.clear();
                    downloadItems.addAll(downloads);
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                });
            }

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                runOnUiThread(() -> {
                    int index = downloadItems.indexOf(download);
                    if (index != -1) {
                        downloadItems.set(index, download);
                        adapter.notifyItemChanged(index);
                    }
                });
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {
                runOnUiThread(() -> {
                    downloadItems.remove(download);
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                });
            }
        });
    }

    private void updateEmptyView() {
        if (downloadItems.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            downloadsRecycler.setVisibility(View.GONE);
            emptyView.setText(R.string.no_downloads);
        } else {
            emptyView.setVisibility(View.GONE);
            downloadsRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDownloadClick(DownloadItem item) {
        if (item.getStatus() == DownloadItem.Status.COMPLETED) {
            openDownloadedFile(item);
        } else if (item.getStatus() == DownloadItem.Status.DOWNLOADING) {
            showDownloadOptions(item);
        } else if (item.getStatus() == DownloadItem.Status.FAILED) {
            retryDownload(item);
        } else if (item.getStatus() == DownloadItem.Status.PAUSED) {
            resumeDownload(item);
        } else if (item.getStatus() == DownloadItem.Status.QUEUED) {
            startQueuedNow(item);
        }
    }

    @Override
    public void onDownloadLongClick(DownloadItem item) {
        showDownloadContextMenu(item);
    }

    private void openDownloadedFile(DownloadItem item) {
        if (item.getDestinationPath() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = getFileUri(item);
            intent.setDataAndType(uri, item.getMimeType());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, R.string.error_opening_file, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDownloadOptions(DownloadItem item) {
        String[] options = new String[]{
                getString(R.string.download_pause),
                getString(R.string.download_cancel)
        };

        new MaterialAlertDialogBuilder(this)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Pause
                            pauseDownload(item);
                            break;
                        case 1: // Cancel
                            confirmCancelDownload(item);
                            break;
                    }
                })
                .show();
    }

    private void showDownloadContextMenu(DownloadItem item) {
        List<String> options = new ArrayList<>();

        switch (item.getStatus()) {
            case COMPLETED:
                options.add(getString(R.string.open_file));
                options.add(getString(R.string.share));
                options.add(getString(R.string.delete));
                break;
            case DOWNLOADING:
                options.add(getString(R.string.download_pause));
                options.add(getString(R.string.download_cancel));
                break;
            case PAUSED:
                options.add(getString(R.string.download_resume));
                options.add(getString(R.string.download_cancel));
                break;
            case FAILED:
                options.add(getString(R.string.retry_download));
                options.add(getString(R.string.delete));
                break;
            case QUEUED:
                options.add(getString(R.string.download_start_now));
                options.add(getString(R.string.delete));
                break;
            default:
                options.add(getString(R.string.delete));
                break;
        }

        options.add(getString(R.string.view_details));

        new MaterialAlertDialogBuilder(this)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    handleContextMenuAction(item, options.get(which));
                })
                .show();
    }

    private void handleContextMenuAction(DownloadItem item, String action) {
        if (action.equals(getString(R.string.open_file))) {
            openDownloadedFile(item);
        } else if (action.equals(getString(R.string.share))) {
            shareFile(item);
        } else if (action.equals(getString(R.string.delete))) {
            confirmDeleteDownload(item);
        } else if (action.equals(getString(R.string.download_pause))) {
            pauseDownload(item);
        } else if (action.equals(getString(R.string.download_resume))) {
            resumeDownload(item);
        } else if (action.equals(getString(R.string.download_cancel))) {
            confirmCancelDownload(item);
        } else if (action.equals(getString(R.string.retry_download))) {
            retryDownload(item);
        } else if (action.equals(getString(R.string.download_start_now))) {
            startQueuedNow(item);
        } else if (action.equals(getString(R.string.view_details))) {
            showDownloadDetails(item);
        }
    }

    private void pauseDownload(DownloadItem item) {
        AppDownloadManager.getInstance().pauseDownload(item.getId());
        item.setStatus(DownloadItem.Status.PAUSED);
        repository.updateDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                loadDownloads();
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private void resumeDownload(DownloadItem item) {
        item.setStatus(DownloadItem.Status.DOWNLOADING);
        repository.saveDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                AppDownloadManager.getInstance().startExistingDownload(DownloadsActivity.this, download);
                loadDownloads();
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private void confirmCancelDownload(DownloadItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.download_cancel)
                .setMessage(getString(R.string.confirm_cancel_download, item.getFileName()))
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> cancelDownload(item))
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private void cancelDownload(DownloadItem item) {
        AppDownloadManager.getInstance().cancelDownload(item.getId());
        item.setStatus(DownloadItem.Status.CANCELLED);
        repository.updateDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                loadDownloads();
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private void startQueuedNow(DownloadItem item) {
        AppDownloadManager.getInstance().removeFromWifiQueue(item.getId());
        item.setStatus(DownloadItem.Status.PENDING);
        repository.saveDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                AppDownloadManager.getInstance().startExistingDownload(DownloadsActivity.this, download);
                loadDownloads();
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private void retryDownload(DownloadItem item) {
        item.setStatus(DownloadItem.Status.DOWNLOADING);
        item.setErrorMessage(null);
        item.setDownloadedBytes(0);
        File file = item.getDestinationPath() != null ? new File(item.getDestinationPath()) : null;
        if (file != null && file.exists()) {
            file.delete();
        }
        repository.saveDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {
                AppDownloadManager.getInstance().startExistingDownload(DownloadsActivity.this, download);
                loadDownloads();
            }

            @Override
            public void onDownloadRemoved(DownloadItem download) {}
        });
    }

    private void confirmDeleteDownload(DownloadItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_download)
                .setMessage(getString(R.string.confirm_delete_download, item.getFileName()))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteDownload(item))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteDownload(DownloadItem item) {
        AppDownloadManager.getInstance().cancelDownload(item.getId());
        if (item.getDestinationPath() != null) {
            File file = new File(item.getDestinationPath());
            if (file.exists()) {
                file.delete();
            }
        }
        repository.removeDownload(item, new DownloadRepository.DownloadCallback() {
            @Override
            public void onDownloadsLoaded(List<DownloadItem> downloads) {}

            @Override
            public void onDownloadUpdated(DownloadItem download) {}

            @Override
            public void onDownloadRemoved(DownloadItem download) {
                loadDownloads();
                Toast.makeText(DownloadsActivity.this, R.string.download_deleted, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareFile(DownloadItem item) {
        if (item.getDestinationPath() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            Uri uri = getFileUri(item);
            intent.setType(item.getMimeType());
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
        }
    }

    private Uri getFileUri(DownloadItem item) {
        if (item.getDestinationPath() != null && item.getDestinationPath().startsWith("content://")) {
            return Uri.parse(item.getDestinationPath());
        }
        File file = new File(item.getDestinationPath());
        return FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                file
        );
    }

    private void showDownloadDetails(DownloadItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_details, null);

        TextView fileName = dialogView.findViewById(R.id.download_file_name);
        TextView fileSize = dialogView.findViewById(R.id.download_file_size);
        TextView status = dialogView.findViewById(R.id.download_status);
        TextView url = dialogView.findViewById(R.id.download_url);
        TextView progress = dialogView.findViewById(R.id.download_progress);

        fileName.setText(item.getFileName());
        fileSize.setText(formatFileSize(item.getTotalBytes()));
        status.setText(item.getStatus().toString());
        url.setText(item.getUrl());
        progress.setText(item.getProgress() + "%");

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.download_details)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.downloads_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_completed) {
            confirmClearCompleted();
            return true;
        } else if (id == R.id.action_clear_all) {
            confirmClearAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearCompleted() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_completed_downloads)
                .setMessage(R.string.confirm_clear_completed)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> {
                    repository.clearCompletedDownloads();
                    loadDownloads();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_all_downloads)
                .setMessage(R.string.confirm_clear_all_downloads)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> {
                    repository.clearAllDownloads();
                    downloadItems.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
