package com.webstudio.easybrowser.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class WeatherSceneView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private ValueAnimator animator;
    private float phase;
    private int weatherCode;

    public WeatherSceneView(Context context) {
        super(context);
        init();
    }

    public WeatherSceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeatherSceneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) * 0.36f;

        drawDepthBase(canvas, width, height);
        if (isClear()) {
            drawSun(canvas, cx, cy, radius * 0.82f, true);
        } else if (isPartlyCloudy()) {
            drawSun(canvas, cx - radius * 0.38f, cy - radius * 0.35f, radius * 0.55f, false);
            drawCloud(canvas, cx + radius * 0.06f, cy + radius * 0.08f, radius * 0.95f);
        } else {
            drawCloud(canvas, cx, cy - radius * 0.03f, radius);
        }

        if (isRain()) {
            drawRain(canvas, cx, cy + radius * 0.55f, radius);
        } else if (isSnow()) {
            drawSnow(canvas, cx, cy + radius * 0.62f, radius);
        } else if (isStorm()) {
            drawLightning(canvas, cx, cy + radius * 0.34f, radius);
        } else if (isFog()) {
            drawFog(canvas, cx, cy + radius * 0.66f, radius);
        }
    }

    private void drawDepthBase(Canvas canvas, float width, float height) {
        float inset = Math.min(width, height) * 0.08f;
        rect.set(inset, inset, width - inset, height - inset);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShadowLayer(inset * 0.35f, 0, inset * 0.28f, 0x330B2742);
        paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                new int[]{0xFFFFFFFF, 0xFFE7F8FD, 0xFFD0EFF7},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, inset * 1.4f, inset * 1.4f, paint);

        paint.clearShadowLayer();
        paint.setShader(new RadialGradient(rect.left + rect.width() * 0.28f,
                rect.top + rect.height() * 0.22f, rect.width() * 0.7f,
                new int[]{0xAAFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, inset * 1.4f, inset * 1.4f, paint);
        paint.setShader(null);
    }

    private void drawSun(Canvas canvas, float cx, float cy, float radius, boolean large) {
        float ray = large ? radius * 1.45f : radius * 1.35f;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(3f, radius * 0.12f));
        paint.setColor(0xFFFFD84C);
        canvas.save();
        canvas.rotate(phase * 16f, cx, cy);
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2d * i / 10d;
            float sx = cx + (float) Math.cos(angle) * radius * 0.95f;
            float sy = cy + (float) Math.sin(angle) * radius * 0.95f;
            float ex = cx + (float) Math.cos(angle) * ray;
            float ey = cy + (float) Math.sin(angle) * ray;
            canvas.drawLine(sx, sy, ex, ey, paint);
        }
        canvas.restore();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx - radius * 0.25f, cy - radius * 0.28f,
                radius * 1.25f, new int[]{0xFFFFF39A, 0xFFFFCA35, 0xFFFFA726},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 0.75f + phase * radius * 0.05f, paint);
        paint.setShader(null);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float radius) {
        float bob = (phase - 0.5f) * radius * 0.08f;
        float x = cx;
        float y = cy + bob;

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(radius * 0.12f, 0, radius * 0.08f, 0x22000000);
        paint.setColor(0xFFBDECF5);
        canvas.drawCircle(x - radius * 0.36f, y + radius * 0.05f, radius * 0.34f, paint);
        canvas.drawCircle(x - radius * 0.05f, y - radius * 0.14f, radius * 0.44f, paint);
        canvas.drawCircle(x + radius * 0.35f, y + radius * 0.04f, radius * 0.36f, paint);
        rect.set(x - radius * 0.68f, y, x + radius * 0.72f, y + radius * 0.42f);
        canvas.drawRoundRect(rect, radius * 0.22f, radius * 0.22f, paint);

        paint.clearShadowLayer();
        paint.setColor(0x99FFFFFF);
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.03f, radius * 0.18f, paint);
        canvas.drawCircle(x - radius * 0.03f, y - radius * 0.22f, radius * 0.2f, paint);
    }

    private void drawRain(Canvas canvas, float cx, float cy, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(0xFF8DEBFF);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(3f, radius * 0.08f));
        float[] offsets = {-0.45f, -0.15f, 0.15f, 0.45f};
        for (int i = 0; i < offsets.length; i++) {
            float dropPhase = (phase + i * 0.22f) % 1f;
            float x = cx + offsets[i] * radius;
            float y = cy + dropPhase * radius * 0.42f;
            canvas.drawLine(x, y, x - radius * 0.08f, y + radius * 0.28f, paint);
        }
    }

    private void drawSnow(Canvas canvas, float cx, float cy, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2f, radius * 0.05f));
        float[] offsets = {-0.42f, -0.1f, 0.25f, 0.52f};
        for (int i = 0; i < offsets.length; i++) {
            float snowPhase = (phase + i * 0.18f) % 1f;
            float x = cx + offsets[i] * radius;
            float y = cy + snowPhase * radius * 0.4f;
            float s = radius * 0.11f;
            canvas.drawLine(x - s, y, x + s, y, paint);
            canvas.drawLine(x, y - s, x, y + s, paint);
            canvas.drawLine(x - s * 0.7f, y - s * 0.7f, x + s * 0.7f, y + s * 0.7f, paint);
            canvas.drawLine(x - s * 0.7f, y + s * 0.7f, x + s * 0.7f, y - s * 0.7f, paint);
        }
    }

    private void drawLightning(Canvas canvas, float cx, float cy, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(phase > 0.65f ? 0xFFFFF176 : 0xFFFFC83D);
        Path bolt = new Path();
        bolt.moveTo(cx + radius * 0.03f, cy - radius * 0.25f);
        bolt.lineTo(cx - radius * 0.22f, cy + radius * 0.32f);
        bolt.lineTo(cx + radius * 0.05f, cy + radius * 0.21f);
        bolt.lineTo(cx - radius * 0.08f, cy + radius * 0.77f);
        bolt.lineTo(cx + radius * 0.36f, cy + radius * 0.02f);
        bolt.lineTo(cx + radius * 0.1f, cy + radius * 0.12f);
        bolt.close();
        canvas.drawPath(bolt, paint);
    }

    private void drawFog(Canvas canvas, float cx, float cy, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(0xEFFFFFFF);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(3f, radius * 0.08f));
        float shift = (phase - 0.5f) * radius * 0.16f;
        canvas.drawLine(cx - radius * 0.72f + shift, cy, cx + radius * 0.58f + shift, cy, paint);
        canvas.drawLine(cx - radius * 0.52f - shift, cy + radius * 0.25f,
                cx + radius * 0.72f - shift, cy + radius * 0.25f, paint);
        canvas.drawLine(cx - radius * 0.38f + shift, cy + radius * 0.5f,
                cx + radius * 0.42f + shift, cy + radius * 0.5f, paint);
    }

    private boolean isClear() {
        return weatherCode == 0;
    }

    private boolean isPartlyCloudy() {
        return weatherCode == 1 || weatherCode == 2;
    }

    private boolean isRain() {
        return (weatherCode >= 51 && weatherCode <= 67)
                || (weatherCode >= 80 && weatherCode <= 82);
    }

    private boolean isSnow() {
        return (weatherCode >= 71 && weatherCode <= 77)
                || (weatherCode >= 85 && weatherCode <= 86);
    }

    private boolean isStorm() {
        return weatherCode >= 95 && weatherCode <= 99;
    }

    private boolean isFog() {
        return weatherCode == 45 || weatherCode == 48;
    }
}
