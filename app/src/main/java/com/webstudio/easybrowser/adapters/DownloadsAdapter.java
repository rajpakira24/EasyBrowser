package com.webstudio.easybrowser.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.DownloadItem;

import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.DownloadViewHolder> {
    private List<DownloadItem> downloads;
    private OnDownloadClickListener listener;

    public interface OnDownloadClickListener {
        void onDownloadClick(DownloadItem item);
        void onDownloadLongClick(DownloadItem item);
    }

    public DownloadsAdapter(List<DownloadItem> downloads, OnDownloadClickListener listener) {
        this.downloads = downloads;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = downloads.get(position);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    class DownloadViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileIcon;
        private TextView fileName;
        private TextView fileSize;
        private TextView status;
        private ProgressBar progressBar;
        private TextView progressText;
        private ImageView actionButton;

        DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileSize = itemView.findViewById(R.id.file_size);
            status = itemView.findViewById(R.id.download_status);
            progressBar = itemView.findViewById(R.id.download_progress);
            progressText = itemView.findViewById(R.id.progress_text);
            actionButton = itemView.findViewById(R.id.action_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDownloadClick(downloads.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDownloadLongClick(downloads.get(position));
                    return true;
                }
                return false;
            });

            actionButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDownloadClick(downloads.get(position));
                }
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        void bind(DownloadItem item) {
            fileName.setText(item.getFileName());
            fileSize.setText(formatFileSize(item.getTotalBytes()));

            // Set file icon based on mime type
            setFileIcon(item.getMimeType());

            // Update status and progress based on download state
            switch (item.getStatus()) {
                case PENDING:
                    status.setText(R.string.download_pending);
                    status.setTextColor(itemView.getContext().getColor(R.color.gray));
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_close);
                    break;

                case DOWNLOADING:
                    status.setText(R.string.download_in_progress);
                    status.setTextColor(itemView.getContext().getColor(R.color.colorPrimary));
                    progressBar.setVisibility(View.VISIBLE);
                    progressText.setVisibility(View.VISIBLE);
                    progressBar.setProgress(item.getProgress());
                    progressText.setText(item.getProgress() + "% • "
                            + formatSpeed(item.getSpeedBytesPerSecond())
                            + "/s • " + formatTime(item.getRemainingSeconds()) + " left");
                    actionButton.setImageResource(R.drawable.ic_pause);
                    break;

                case PAUSED:
                    status.setText(R.string.download_paused);
                    status.setTextColor(itemView.getContext().getColor(R.color.warning));
                    progressBar.setVisibility(View.VISIBLE);
                    progressText.setVisibility(View.VISIBLE);
                    progressBar.setProgress(item.getProgress());
                    progressText.setText(item.getProgress() + "%");
                    actionButton.setImageResource(R.drawable.ic_play);
                    break;

                case COMPLETED:
                    status.setText(R.string.download_complete);
                    status.setTextColor(itemView.getContext().getColor(R.color.success));
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_open);
                    break;

                case FAILED:
                    status.setText(item.getErrorMessage() != null ? item.getErrorMessage() :
                            itemView.getContext().getString(R.string.download_failed));
                    status.setTextColor(itemView.getContext().getColor(R.color.error));
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_retry);
                    break;

                case CANCELLED:
                    status.setText(R.string.download_cancelled);
                    status.setTextColor(itemView.getContext().getColor(R.color.gray));
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_close);
                    break;

                case QUEUED:
                    status.setText(R.string.download_queued);
                    status.setTextColor(itemView.getContext().getColor(R.color.gray));
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_play);
                    break;
            }
        }

        private void setFileIcon(String mimeType) {
            if (mimeType == null) {
                fileIcon.setImageResource(R.drawable.ic_file);
                return;
            }

            if (mimeType.startsWith("image/")) {
                fileIcon.setImageResource(R.drawable.ic_image);
            } else if (mimeType.startsWith("video/")) {
                fileIcon.setImageResource(R.drawable.ic_video);
            } else if (mimeType.startsWith("audio/")) {
                fileIcon.setImageResource(R.drawable.ic_audio);
            } else if (mimeType.equals("application/pdf")) {
                fileIcon.setImageResource(R.drawable.ic_pdf);
            } else if (mimeType.equals("application/zip") ||
                    mimeType.equals("application/x-zip-compressed") ||
                    mimeType.equals("application/x-rar-compressed")) {
                fileIcon.setImageResource(R.drawable.ic_archive);
            } else if (mimeType.startsWith("text/")) {
                fileIcon.setImageResource(R.drawable.ic_text);
            } else {
                fileIcon.setImageResource(R.drawable.ic_file);
            }
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }

        private String formatSpeed(long bytesPerSecond) {
            if (bytesPerSecond <= 0) return "--";
            return formatFileSize(bytesPerSecond);
        }

        private String formatTime(long seconds) {
            if (seconds <= 0) return "--";
            if (seconds < 60) return seconds + "s";
            long minutes = seconds / 60;
            if (minutes < 60) return minutes + "m";
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
    }
}
