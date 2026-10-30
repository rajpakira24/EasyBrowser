package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bottom sheets for the "save this login" and "fill a saved login" prompts. */
final class LoginPromptSheets {
    private static final int[] AVATAR_COLORS = {
            0xFF3D7BE0, 0xFF8E5CD9, 0xFF1FA67A, 0xFFE0833D, 0xFFD6527A, 0xFF3AA6C9
    };

    private LoginPromptSheets() {
    }

    /** Asks whether to save a login; exactly one of the callbacks runs. */
    static void showSave(Context context, String origin, String username,
                         Runnable onSave, Runnable onDismiss) {
        Sheet sheet = new Sheet(context, origin);
        sheet.root.addView(sheet.text(context.getString(R.string.password_save_title), 20, true,
                sheet.palette.onSurface, Gravity.CENTER_HORIZONTAL, 14));
        String who = username == null || username.isEmpty()
                ? context.getString(R.string.password_no_username) : username;
        sheet.root.addView(sheet.text(context.getString(R.string.password_save_message, who,
                hostOf(origin)), 14, false, sheet.palette.onSurfaceMuted,
                Gravity.CENTER_HORIZONTAL, 6));

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, sheet.dp(20), 0, 0);
        MaterialButton later = sheet.button(R.string.password_not_now, false);
        MaterialButton save = sheet.button(R.string.save, true);
        later.setOnClickListener(v -> sheet.dialog.dismiss());
        save.setOnClickListener(v -> {
            sheet.chosen.set(true);
            sheet.dialog.dismiss();
            onSave.run();
        });
        buttons.addView(later, sheet.weighted(false));
        buttons.addView(save, sheet.weighted(true));
        sheet.root.addView(buttons);
        sheet.show(onDismiss);
    }

    /** Lets the user pick which saved login to fill. */
    static void showSelect(Context context, String origin, String[] usernames,
                           java.util.function.IntConsumer onPick, Runnable onDismiss) {
        Sheet sheet = new Sheet(context, origin);
        sheet.root.addView(sheet.text(context.getString(R.string.password_fill_title), 20, true,
                sheet.palette.onSurface, Gravity.CENTER_HORIZONTAL, 14));
        sheet.root.addView(sheet.text(hostOf(origin), 13, false, sheet.palette.onSurfaceMuted,
                Gravity.CENTER_HORIZONTAL, 2));

        for (int i = 0; i < usernames.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(sheet.dp(14), sheet.dp(12), sheet.dp(14), sheet.dp(12));
            row.setBackground(sheet.rounded(0x1FFFFFFF, sheet.dp(14)));
            row.addView(sheet.avatar(usernames[i].isEmpty() ? hostOf(origin) : usernames[i], 36, 15));
            TextView name = sheet.text(usernames[i].isEmpty()
                            ? context.getString(R.string.password_no_username) : usernames[i],
                    15, false, sheet.palette.onSurface, Gravity.START, 0);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(sheet.dp(12), 0, 0, 0);
            row.addView(name, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ImageView lock = new ImageView(context);
            lock.setImageResource(R.drawable.ic_lock);
            lock.setColorFilter(sheet.palette.accent);
            row.addView(lock, new LinearLayout.LayoutParams(sheet.dp(18), sheet.dp(18)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = sheet.dp(i == 0 ? 16 : 8);
            row.setOnClickListener(v -> {
                sheet.chosen.set(true);
                sheet.dialog.dismiss();
                onPick.accept(index);
            });
            sheet.root.addView(row, params);
        }
        sheet.show(onDismiss);
    }

    private static String hostOf(String origin) {
        String host = origin == null ? null : Uri.parse(origin).getHost();
        if (host == null) {
            return origin == null ? "" : origin;
        }
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private static final class Sheet {
        final Context context;
        final ThemeEngine.Palette palette;
        final float density;
        final BottomSheetDialog dialog;
        final LinearLayout root;
        final AtomicBoolean chosen = new AtomicBoolean(false);

        Sheet(Context context, String origin) {
            this.context = context;
            palette = ThemeEngine.homePalette(context);
            density = context.getResources().getDisplayMetrics().density;
            dialog = new BottomSheetDialog(context);
            root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(dp(20), dp(24), dp(20), dp(24));
            GradientDrawable bg = rounded(0xFF16222C, dp(24));
            bg.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
            root.setBackground(bg);
            root.addView(avatar(hostOf(origin), 56, 24));
        }

        void show(Runnable onDismiss) {
            dialog.setContentView(root);
            dialog.setOnDismissListener(d -> {
                if (!chosen.get()) {
                    onDismiss.run();
                }
            });
            dialog.show();
        }

        View avatar(String seed, int sizeDp, int textSp) {
            String letter = seed.isEmpty() ? "?" : seed.substring(0, 1).toUpperCase(Locale.US);
            TextView avatar = text(letter, textSp, true, Color.WHITE, Gravity.CENTER, 0);
            avatar.setBackground(rounded(
                    AVATAR_COLORS[Math.abs(seed.hashCode()) % AVATAR_COLORS.length],
                    dp(sizeDp) / 2));
            avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
            return avatar;
        }

        TextView text(String value, int sp, boolean bold, int color, int gravity, int topDp) {
            TextView view = new TextView(context);
            view.setText(value);
            view.setTextSize(sp);
            view.setTextColor(color);
            view.setGravity(gravity);
            view.setPadding(0, dp(topDp), 0, 0);
            if (bold) {
                view.setTypeface(null, Typeface.BOLD);
            }
            return view;
        }

        MaterialButton button(int labelRes, boolean primary) {
            MaterialButton button = new MaterialButton(context, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(labelRes);
            button.setAllCaps(false);
            button.setCornerRadius(dp(14));
            if (primary) {
                button.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(palette.accent));
                button.setTextColor(0xFF0E161D);
                button.setStrokeWidth(0);
            } else {
                button.setTextColor(palette.accent);
                button.setStrokeColor(android.content.res.ColorStateList.valueOf(palette.panelBorder));
            }
            return button;
        }

        LinearLayout.LayoutParams weighted(boolean second) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.leftMargin = second ? dp(8) : 0;
            return params;
        }

        GradientDrawable rounded(int color, int radius) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(radius);
            return drawable;
        }

        int dp(int value) {
            return Math.round(value * density);
        }
    }
}
