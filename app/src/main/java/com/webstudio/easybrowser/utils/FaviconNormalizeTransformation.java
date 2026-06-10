package com.webstudio.easybrowser.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class FaviconNormalizeTransformation extends BitmapTransformation {
    private static final String ID =
            "com.webstudio.easybrowser.utils.FaviconNormalizeTransformation.v2";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);
    private static final float TARGET_CONTENT_RATIO = 0.86f;

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap source,
                               int outWidth, int outHeight) {
        Rect bounds = findContentBounds(source);
        if (bounds == null) {
            return source;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int contentWidth = bounds.width();
        int contentHeight = bounds.height();

        int outputSide = Math.max(Math.max(contentWidth, contentHeight), Math.min(width, height));
        Bitmap result = pool.get(outputSide, outputSide, Bitmap.Config.ARGB_8888);
        result.setHasAlpha(true);
        result.eraseColor(Color.TRANSPARENT);

        float targetSide = outputSide * TARGET_CONTENT_RATIO;
        float scale = Math.min(targetSide / contentWidth, targetSide / contentHeight);
        float drawWidth = contentWidth * scale;
        float drawHeight = contentHeight * scale;
        float left = (outputSide - drawWidth) / 2f;
        float top = (outputSide - drawHeight) / 2f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        new Canvas(result).drawBitmap(source, bounds,
                new RectF(left, top, left + drawWidth, top + drawHeight), paint);
        return result;
    }

    private Rect findContentBounds(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isContentPixel(bitmap.getPixel(x, y))) {
                    continue;
                }
                if (x < left) left = x;
                if (x > right) right = x;
                if (y < top) top = y;
                if (y > bottom) bottom = y;
            }
        }

        if (right < left || bottom < top) {
            return null;
        }
        return new Rect(left, top, right + 1, bottom + 1);
    }

    private boolean isContentPixel(int pixel) {
        int alpha = Color.alpha(pixel);
        if (alpha <= 24) {
            return false;
        }

        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        boolean lightNeutral = min >= 224 && max - min <= 30;
        return !lightNeutral;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FaviconNormalizeTransformation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
}
