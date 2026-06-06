package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.models.Tab;

import java.io.File;
import java.io.FileOutputStream;

public final class TabThumbnailManager {
    private static final String THUMBNAIL_DIR = "tab_thumbnails";
    private static final int MAX_WIDTH = 640;
    private static final int JPEG_QUALITY = 78;

    private TabThumbnailManager() {
    }

    public static String saveThumbnail(Context context, String tabId, Bitmap bitmap) {
        if (context == null || tabId == null || bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        File dir = new File(context.getFilesDir(), THUMBNAIL_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File file = new File(dir, tabId + ".jpg");
        Bitmap output = bitmap;
        try {
            if (bitmap.getWidth() > MAX_WIDTH) {
                int height = Math.max(1, Math.round(bitmap.getHeight() * (MAX_WIDTH / (float) bitmap.getWidth())));
                output = Bitmap.createScaledBitmap(bitmap, MAX_WIDTH, height, true);
            }
            try (FileOutputStream stream = new FileOutputStream(file, false)) {
                output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
            }
            return file.getAbsolutePath();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (output != bitmap && output != null && !output.isRecycled()) {
                output.recycle();
            }
        }
    }

    public static void loadThumbnail(ImageView imageView, Tab tab) {
        if (imageView == null) {
            return;
        }
        String path = tab != null ? tab.getThumbnailPath() : null;
        if (path == null || path.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }
        File file = new File(path);
        imageView.setImageDrawable(null);
        Glide.with(imageView)
                .load(file)
                .signature(new ObjectKey(file.lastModified() + ":" + file.length()))
                .placeholder((android.graphics.drawable.Drawable) null)
                .error((android.graphics.drawable.Drawable) null)
                .centerCrop()
                .into(imageView);
    }

    public static void deleteThumbnail(String thumbnailPath) {
        if (thumbnailPath == null || thumbnailPath.trim().isEmpty()) {
            return;
        }
        File file = new File(thumbnailPath);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
