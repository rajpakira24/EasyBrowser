package com.webstudio.easybrowser.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.webstudio.easybrowser.R;

import java.util.List;

class MoreMenuPopup {
    private static Dialog currentDialog;

    static class Action {
        final int iconResId;
        final int titleResId;
        final boolean enabled;
        final Runnable runnable;

        Action(int iconResId, int titleResId, boolean enabled, Runnable runnable) {
            this.iconResId = iconResId;
            this.titleResId = titleResId;
            this.enabled = enabled;
            this.runnable = runnable;
        }
    }

    static void show(Context context, View anchor, List<Action> navigationActions,
                     List<Action> menuActions) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
            return;
        }

        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> {
            if (currentDialog == dialog) {
                currentDialog = null;
            }
        });
        ScrollView scrollView = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 12));
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSurface));
        root.setScaleX(0.92f);
        root.setScaleY(0.92f);
        root.setAlpha(0f);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout navRow = new LinearLayout(context);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);
        root.addView(navRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (Action action : navigationActions) {
            navRow.addView(createIconAction(context, action, dialog),
                    new LinearLayout.LayoutParams(0, dp(context, 50), 1));
        }

        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.border_color));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, dp(context, 8), 0, dp(context, 8));
        root.addView(divider, dividerParams);

        for (Action action : menuActions) {
            root.addView(createTextAction(context, action, dialog),
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(context, 48)));
        }

        dialog.setContentView(scrollView);
        configureWindow(dialog, context);
        dialog.show();
        currentDialog = dialog;
        configureWindow(dialog, context);
        root.post(() -> {
            root.setPivotX(root.getWidth());
            root.setPivotY(root.getHeight());
            root.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(140)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }

    private static void configureWindow(Dialog dialog, Context context) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(getMenuWidth(context), WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM | Gravity.END);
        window.setWindowAnimations(0);
    }

    private static ImageButton createIconAction(Context context, Action action, Dialog dialog) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(action.iconResId);
        tintIcon(context, button);
        button.setContentDescription(context.getString(action.titleResId));
        button.setBackgroundResource(resolveAttr(context,
                android.R.attr.selectableItemBackgroundBorderless));
        button.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        button.setEnabled(action.enabled);
        button.setAlpha(action.enabled ? 1f : 0.35f);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            if (action.enabled) {
                action.runnable.run();
            }
        });
        return button;
    }

    private static LinearLayout createTextAction(Context context, Action action, Dialog dialog) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        row.setBackgroundResource(resolveAttr(context, android.R.attr.selectableItemBackground));
        row.setEnabled(action.enabled);
        row.setAlpha(action.enabled ? 1f : 0.35f);

        ImageView icon = new ImageView(context);
        icon.setImageResource(action.iconResId);
        tintIcon(context, icon);
        row.addView(icon, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)));

        TextView label = new TextView(context);
        label.setText(action.titleResId);
        label.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        label.setTextSize(15);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(context, 16), 0, 0, 0);
        row.addView(label, labelParams);

        row.setOnClickListener(v -> {
            dialog.dismiss();
            if (action.enabled) {
                action.runnable.run();
            }
        });
        return row;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int getMenuWidth(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(context, 280), (int) (screenWidth * 0.6f));
    }

    private static void tintIcon(Context context, ImageView icon) {
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.colorOnSurface)));
    }

    private static int resolveAttr(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId;
    }
}
