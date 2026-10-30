package com.webstudio.easybrowser.ui.activity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A slim strip of saved logins above the keyboard. Unlike a dialog it never takes focus, so
 * the user can ignore it and keep typing.
 */
final class LoginSuggestionBar {
    private static final long AUTO_HIDE_MS = 20_000L;
    private static final String TAG = "login_suggestion_bar";

    private LoginSuggestionBar() {
    }

    /** Shows the bar; exactly one of {@code onPick} or {@code onDismiss} runs, once. */
    static void show(Activity activity, String[] usernames,
                     java.util.function.IntConsumer onPick, Runnable onDismiss) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) {
            onDismiss.run();
            return;
        }
        hide(content);

        ThemeEngine.Palette palette = ThemeEngine.homePalette(activity);
        float density = activity.getResources().getDisplayMetrics().density;
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean done = new AtomicBoolean(false);

        LinearLayout bar = new LinearLayout(activity);
        bar.setTag(TAG);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(density, 8), dp(density, 6), dp(density, 4), dp(density, 6));
        bar.setBackgroundColor(0xFF16222C);
        bar.setElevation(dp(density, 8));

        ImageView lock = new ImageView(activity);
        lock.setImageResource(R.drawable.ic_lock);
        lock.setColorFilter(palette.accent);
        lock.setPadding(dp(density, 4), 0, dp(density, 8), 0);
        bar.addView(lock, new LinearLayout.LayoutParams(dp(density, 28), dp(density, 20)));

        LinearLayout chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        bar.addView(scroll, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Runnable[] timeout = new Runnable[1];
        Runnable remove = () -> {
            handler.removeCallbacks(timeout[0]);
            content.removeView(bar);
        };

        for (int i = 0; i < usernames.length; i++) {
            final int index = i;
            String name = usernames[i].isEmpty()
                    ? activity.getString(R.string.password_no_username) : usernames[i];
            TextView chip = new TextView(activity);
            chip.setText(name);
            chip.setTextSize(14);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setTextColor(palette.onSurface);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setMaxWidth(dp(density, 260));
            chip.setPadding(dp(density, 14), dp(density, 8), dp(density, 14), dp(density, 8));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x33FFFFFF);
            bg.setCornerRadius(dp(density, 18));
            chip.setBackground(bg);
            chip.setOnClickListener(v -> {
                if (done.compareAndSet(false, true)) {
                    remove.run();
                    onPick.accept(index);
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(density, 8);
            chips.addView(chip, params);
        }

        ImageButton close = new ImageButton(activity);
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(palette.onSurfaceMuted);
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setContentDescription(activity.getString(R.string.cancel));
        close.setOnClickListener(v -> {
            if (done.compareAndSet(false, true)) {
                remove.run();
                onDismiss.run();
            }
        });
        bar.addView(close, new LinearLayout.LayoutParams(dp(density, 40), dp(density, 40)));

        // Dropped silently by the next show() or on timeout, which still counts as a dismissal.
        bar.setTag(R.id.login_suggestion_dismiss, (Runnable) () -> {
            if (done.compareAndSet(false, true)) {
                remove.run();
                onDismiss.run();
            }
        });
        timeout[0] = () -> {
            if (done.compareAndSet(false, true)) {
                content.removeView(bar);
                onDismiss.run();
            }
        };
        handler.postDelayed(timeout[0], AUTO_HIDE_MS);

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        content.addView(bar, barParams);
    }

    /** Removes any bar that is showing, counting it as dismissed. */
    static void hide(ViewGroup content) {
        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            View child = content.getChildAt(i);
            if (TAG.equals(child.getTag())) {
                Object dismiss = child.getTag(R.id.login_suggestion_dismiss);
                if (dismiss instanceof Runnable) {
                    ((Runnable) dismiss).run();
                } else {
                    content.removeView(child);
                }
            }
        }
    }

    private static int dp(float density, int value) {
        return Math.round(value * density);
    }
}
