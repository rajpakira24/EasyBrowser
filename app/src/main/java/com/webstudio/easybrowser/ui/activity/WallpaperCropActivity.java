package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.SystemBarUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class WallpaperCropActivity extends AppCompatActivity {
    public static final String EXTRA_SOURCE_URI = "source_uri";
    public static final String EXTRA_OUTPUT_URI = "output_uri";

    private static final int MAX_DECODE_SIZE = 4096;
    private static final int MAX_OUTPUT_WIDTH = 1440;
    private static final int MAX_OUTPUT_HEIGHT = 2560;

    private CropView cropView;
    private Uri sourceUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBarUtils.apply(this, Color.BLACK, Color.BLACK, false);
        String uriString = getIntent().getStringExtra(EXTRA_SOURCE_URI);
        if (uriString == null || uriString.trim().isEmpty()) {
            finish();
            return;
        }
        sourceUri = Uri.parse(uriString);
        Bitmap bitmap = decodeBitmap(sourceUri);
        if (bitmap == null) {
            Toast.makeText(this, R.string.wallpaper_crop_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        buildUi(bitmap);
    }

    private void buildUi(Bitmap bitmap) {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        cropView = new CropView(this, bitmap);
        root.addView(cropView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.wallpaper_crop);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(0x66000000);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        MenuItem saveItem = toolbar.getMenu().add(R.string.save);
        saveItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.setOnMenuItemClickListener(item -> {
            saveCroppedWallpaper();
            return true;
        });
        root.addView(toolbar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
    }

    private Bitmap decodeBitmap(Uri uri) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight);
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private int calculateSampleSize(int width, int height) {
        int sample = 1;
        while (width / sample > MAX_DECODE_SIZE || height / sample > MAX_DECODE_SIZE) {
            sample *= 2;
        }
        return sample;
    }

    private void saveCroppedWallpaper() {
        if (cropView == null) {
            return;
        }
        cropView.setEnabled(false);
        new Thread(() -> {
            Uri outputUri = null;
            try {
                outputUri = saveBitmap(cropView.createCroppedBitmap());
            } catch (RuntimeException ignored) {
            }
            Uri finalOutputUri = outputUri;
            runOnUiThread(() -> {
                cropView.setEnabled(true);
                if (finalOutputUri == null) {
                    Toast.makeText(this, R.string.wallpaper_crop_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent result = new Intent();
                result.putExtra(EXTRA_OUTPUT_URI, finalOutputUri.toString());
                setResult(RESULT_OK, result);
                finish();
            });
        }, "wallpaper-crop-save").start();
    }

    private Uri saveBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        File directory = new File(getFilesDir(), "wallpapers");
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File output = new File(directory, String.format(Locale.US,
                "user_wallpaper_%d.jpg", System.currentTimeMillis()));
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                return null;
            }
            return Uri.fromFile(output);
        } catch (IOException ignored) {
            return null;
        } finally {
            bitmap.recycle();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class CropView extends View {
        private static final float ASPECT_WIDTH = 9f;
        private static final float ASPECT_HEIGHT = 16f;

        private final Bitmap bitmap;
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix matrix = new Matrix();
        private final Matrix inverse = new Matrix();
        private final RectF cropFrame = new RectF();
        private final RectF imageFrame = new RectF();
        private final ScaleGestureDetector scaleDetector;
        private float scale = 1f;
        private float minScale = 1f;
        private float maxScale = 4f;
        private float translationX;
        private float translationY;
        private float lastX;
        private float lastY;
        private boolean dragging;

        CropView(android.content.Context context, Bitmap bitmap) {
            super(context);
            this.bitmap = bitmap;
            overlayPaint.setColor(0x99000000);
            framePaint.setColor(Color.WHITE);
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(dp(context, 2));
            scaleDetector = new ScaleGestureDetector(context,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            scale *= detector.getScaleFactor();
                            scale = Math.max(minScale, Math.min(scale, maxScale));
                            updateMatrix();
                            ensureImageCoversCropFrame();
                            invalidate();
                            return true;
                        }
                    });
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            int padding = dp(getContext(), 22);
            float availableWidth = Math.max(1, width - padding * 2f);
            float availableHeight = Math.max(1, height - padding * 2f);
            float cropWidth = availableWidth;
            float cropHeight = cropWidth * ASPECT_HEIGHT / ASPECT_WIDTH;
            if (cropHeight > availableHeight) {
                cropHeight = availableHeight;
                cropWidth = cropHeight * ASPECT_WIDTH / ASPECT_HEIGHT;
            }
            float left = (width - cropWidth) / 2f;
            float top = (height - cropHeight) / 2f;
            cropFrame.set(left, top, left + cropWidth, top + cropHeight);
            minScale = Math.max(cropFrame.width() / bitmap.getWidth(),
                    cropFrame.height() / bitmap.getHeight());
            maxScale = Math.max(minScale * 4f, minScale + 1f);
            scale = minScale;
            translationX = cropFrame.centerX() - bitmap.getWidth() * scale / 2f;
            translationY = cropFrame.centerY() - bitmap.getHeight() * scale / 2f;
            updateMatrix();
            ensureImageCoversCropFrame();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(bitmap, matrix, bitmapPaint);
            drawOverlay(canvas);
            drawGrid(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                dragging = false;
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    dragging = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        translationX += dx;
                        translationY += dy;
                        lastX = event.getX();
                        lastY = event.getY();
                        updateMatrix();
                        ensureImageCoversCropFrame();
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
                default:
                    return true;
            }
        }

        Bitmap createCroppedBitmap() {
            if (!matrix.invert(inverse)) {
                return null;
            }
            RectF mappedCrop = new RectF(cropFrame);
            inverse.mapRect(mappedCrop);
            int left = clamp(Math.round(mappedCrop.left), 0, bitmap.getWidth() - 1);
            int top = clamp(Math.round(mappedCrop.top), 0, bitmap.getHeight() - 1);
            int right = clamp(Math.round(mappedCrop.right), left + 1, bitmap.getWidth());
            int bottom = clamp(Math.round(mappedCrop.bottom), top + 1, bitmap.getHeight());
            Bitmap cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
            float outputScale = Math.min(
                    MAX_OUTPUT_WIDTH / (float) cropped.getWidth(),
                    MAX_OUTPUT_HEIGHT / (float) cropped.getHeight());
            if (outputScale >= 1f) {
                return cropped;
            }
            Bitmap scaled = Bitmap.createScaledBitmap(cropped,
                    Math.max(1, Math.round(cropped.getWidth() * outputScale)),
                    Math.max(1, Math.round(cropped.getHeight() * outputScale)),
                    true);
            cropped.recycle();
            return scaled;
        }

        private void drawOverlay(Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), cropFrame.top, overlayPaint);
            canvas.drawRect(0, cropFrame.bottom, getWidth(), getHeight(), overlayPaint);
            canvas.drawRect(0, cropFrame.top, cropFrame.left, cropFrame.bottom, overlayPaint);
            canvas.drawRect(cropFrame.right, cropFrame.top, getWidth(), cropFrame.bottom, overlayPaint);
            canvas.drawRect(cropFrame, framePaint);
        }

        private void drawGrid(Canvas canvas) {
            float thirdWidth = cropFrame.width() / 3f;
            float thirdHeight = cropFrame.height() / 3f;
            framePaint.setAlpha(120);
            canvas.drawLine(cropFrame.left + thirdWidth, cropFrame.top,
                    cropFrame.left + thirdWidth, cropFrame.bottom, framePaint);
            canvas.drawLine(cropFrame.left + thirdWidth * 2f, cropFrame.top,
                    cropFrame.left + thirdWidth * 2f, cropFrame.bottom, framePaint);
            canvas.drawLine(cropFrame.left, cropFrame.top + thirdHeight,
                    cropFrame.right, cropFrame.top + thirdHeight, framePaint);
            canvas.drawLine(cropFrame.left, cropFrame.top + thirdHeight * 2f,
                    cropFrame.right, cropFrame.top + thirdHeight * 2f, framePaint);
            framePaint.setAlpha(255);
        }

        private void updateMatrix() {
            matrix.reset();
            matrix.postScale(scale, scale);
            matrix.postTranslate(translationX, translationY);
            imageFrame.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            matrix.mapRect(imageFrame);
        }

        private void ensureImageCoversCropFrame() {
            if (imageFrame.width() < cropFrame.width() || imageFrame.height() < cropFrame.height()) {
                minScale = Math.max(cropFrame.width() / bitmap.getWidth(),
                        cropFrame.height() / bitmap.getHeight());
                scale = Math.max(scale, minScale);
                updateMatrix();
            }
            if (imageFrame.left > cropFrame.left) {
                translationX -= imageFrame.left - cropFrame.left;
            }
            if (imageFrame.right < cropFrame.right) {
                translationX += cropFrame.right - imageFrame.right;
            }
            if (imageFrame.top > cropFrame.top) {
                translationY -= imageFrame.top - cropFrame.top;
            }
            if (imageFrame.bottom < cropFrame.bottom) {
                translationY += cropFrame.bottom - imageFrame.bottom;
            }
            updateMatrix();
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(value, max));
        }

        private static int dp(android.content.Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
