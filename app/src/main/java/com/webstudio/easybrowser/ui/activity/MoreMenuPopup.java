package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.card.MaterialCardView;
import com.webstudio.easybrowser.R;

import java.util.List;

class MoreMenuPopup {
    private static PopupWindow currentPopup;

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
        if (currentPopup != null && currentPopup.isShowing()) {
            currentPopup.dismiss();
            currentPopup = null;
            return;
        }

        PopupWindow popup = new PopupWindow(context);
        popup.setWidth(getMenuWidth(context));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setClippingEnabled(true);
        popup.setAnimationStyle(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(context, 0));
        }
        popup.setOnDismissListener(() -> {
            if (currentPopup == popup) {
                currentPopup = null;
            }
        });

        FrameLayout container = new FrameLayout(context);
        container.setClipToPadding(false);
        container.setPadding(dp(context, 6), dp(context, 8), 0, dp(context, 14));

        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurface));
        card.setRadius(dp(context, 28));
        card.setCardElevation(dp(context, 14));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(ContextCompat.getColor(context, R.color.border_color));
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setAlpha(0f);

        MaxHeightScrollView scrollView = new MaxHeightScrollView(context, getMenuMaxHeight(context));
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 12));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(scrollView, new MaterialCardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout navRow = new LinearLayout(context);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);
        root.addView(navRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (Action action : navigationActions) {
            navRow.addView(createIconAction(context, action, popup),
                    new LinearLayout.LayoutParams(0, dp(context, 54), 1));
        }

        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.border_color));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, dp(context, 8), 0, dp(context, 8));
        root.addView(divider, dividerParams);

        for (Action action : menuActions) {
            root.addView(createTextAction(context, action, popup),
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(context, 52)));
        }

        popup.setContentView(container);
        currentPopup = popup;
        View parent = anchor != null && anchor.getRootView() != null
                ? anchor.getRootView()
                : anchor;
        popup.showAtLocation(parent, Gravity.BOTTOM | Gravity.END, 0, dp(context, 10));
        card.post(() -> {
            card.setPivotX(card.getWidth());
            card.setPivotY(card.getHeight());
            card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(140)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }

    private static ImageButton createIconAction(Context context, Action action, PopupWindow popup) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(action.iconResId);
        tintIcon(context, button);
        button.setContentDescription(context.getString(action.titleResId));
        button.setBackgroundResource(resolveAttr(context,
                android.R.attr.selectableItemBackgroundBorderless));
        button.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        button.setEnabled(action.enabled);
        button.setAlpha(action.enabled ? 1f : 0.35f);
        button.setOnClickListener(v -> {
            popup.dismiss();
            if (action.enabled) {
                action.runnable.run();
            }
        });
        return button;
    }

    private static LinearLayout createTextAction(Context context, Action action, PopupWindow popup) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 18), 0, dp(context, 16), 0);
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
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER_VERTICAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            label.setFallbackLineSpacing(false);
        }
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(context, 18), 0, 0, 0);
        row.addView(label, labelParams);

        row.setOnClickListener(v -> {
            popup.dismiss();
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
        int desired = Math.max(dp(context, 286), (int) (screenWidth * 0.60f));
        return Math.min(screenWidth - dp(context, 8), desired);
    }

    private static int getMenuMaxHeight(Context context) {
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        return Math.max(dp(context, 360), screenHeight - dp(context, 118));
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

    private static final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        MaxHeightScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int constrainedHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, constrainedHeightSpec);
        }
    }
}
