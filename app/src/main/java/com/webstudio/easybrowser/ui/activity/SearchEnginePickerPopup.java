package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.utils.UrlUtils;

/**
 * Firefox-style "This time search in:" dropdown. Lists the configured search engines (favicon +
 * name) plus shortcuts (Bookmarks / Tabs / History / Search settings). Selecting an engine is a
 * one-time override — it never changes the default engine.
 */
class SearchEnginePickerPopup {
    private static PopupWindow currentPopup;

    interface Callback {
        void onEngineSelected(String name, String engineUrl);
        void onBookmarks();
        void onTabs();
        void onHistory();
        void onSearchSettings();
    }

    static void show(Context context, View anchor, Callback callback) {
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
        popup.setOnDismissListener(() -> {
            if (currentPopup == popup) {
                currentPopup = null;
            }
        });

        FrameLayout container = new FrameLayout(context);
        container.setClipToPadding(false);
        container.setPadding(dp(context, 6), dp(context, 8), dp(context, 6), dp(context, 14));

        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurface));
        card.setRadius(dp(context, 24));
        card.setCardElevation(dp(context, 14));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(ContextCompat.getColor(context, R.color.border_color));
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setAlpha(0f);

        MaxHeightScrollView scrollView = new MaxHeightScrollView(context, getMenuMaxHeight(context));
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(scrollView, new MaterialCardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(R.string.search_picker_title);
        title.setTextColor(ContextCompat.getColor(context, R.color.gray));
        title.setTextSize(13);
        title.setPadding(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 8));
        root.addView(title);

        String[] names = context.getResources().getStringArray(R.array.search_engine_names);
        String[] values = context.getResources().getStringArray(R.array.search_engine_values);
        int count = Math.min(names.length, values.length);
        for (int i = 0; i < count; i++) {
            final String name = names[i];
            final String url = values[i];
            root.addView(createEngineRow(context, name, url, popup, callback));
        }

        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.border_color));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));
        root.addView(divider, dividerParams);

        root.addView(createShortcutRow(context, R.drawable.ic_bookmarks, R.string.sp_bookmarks,
                popup, callback::onBookmarks));
        root.addView(createShortcutRow(context, R.drawable.ic_tabs, R.string.sp_tabs,
                popup, callback::onTabs));
        root.addView(createShortcutRow(context, R.drawable.ic_history, R.string.sp_history,
                popup, callback::onHistory));
        root.addView(createShortcutRow(context, R.drawable.ic_settings, R.string.sp_search_settings,
                popup, callback::onSearchSettings));

        popup.setContentView(container);
        currentPopup = popup;

        // The search bar is usually bottom-docked, so dropping down clips the list. If the
        // anchor sits in the lower half of the screen, show the picker ABOVE it instead.
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int screenH = context.getResources().getDisplayMetrics().heightPixels;
        final boolean dropUp = loc[1] > screenH / 2;
        if (dropUp) {
            int bottomMargin = screenH - loc[1] + dp(context, 8);
            popup.showAtLocation(anchor.getRootView(), Gravity.BOTTOM | Gravity.START,
                    dp(context, 8), bottomMargin);
        } else {
            popup.showAsDropDown(anchor, 0, dp(context, 4));
        }
        card.post(() -> {
            card.setPivotX(0f);
            card.setPivotY(dropUp ? card.getHeight() : 0f);
            card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(140)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }

    private static LinearLayout createEngineRow(Context context, String name, String url,
                                                PopupWindow popup, Callback callback) {
        LinearLayout row = baseRow(context);
        ImageView icon = new ImageView(context);
        Glide.with(icon)
                .load(UrlUtils.getEngineIconUrl(url))
                .circleCrop()
                .placeholder(R.drawable.ic_search)
                .error(R.drawable.ic_search)
                .into(icon);
        row.addView(icon, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)));

        TextView label = label(context, name);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(context, 16), 0, 0, 0);
        row.addView(label, labelParams);

        row.setOnClickListener(v -> {
            popup.dismiss();
            callback.onEngineSelected(name, url);
        });
        return row;
    }

    private static LinearLayout createShortcutRow(Context context, int iconRes, int titleRes,
                                                  PopupWindow popup, Runnable action) {
        LinearLayout row = baseRow(context);
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.colorOnSurface)));
        row.addView(icon, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)));

        TextView label = label(context, context.getString(titleRes));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(context, 16), 0, 0, 0);
        row.addView(label, labelParams);

        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
        return row;
    }

    private static LinearLayout baseRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 12), dp(context, 16), dp(context, 12));
        row.setBackgroundResource(resolveAttr(context, android.R.attr.selectableItemBackground));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private static TextView label(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int getMenuWidth(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int desired = Math.max(dp(context, 280), (int) (screenWidth * 0.72f));
        return Math.min(screenWidth - dp(context, 16), desired);
    }

    private static int getMenuMaxHeight(Context context) {
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        return Math.max(dp(context, 320), screenHeight - dp(context, 220));
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
            int constrained = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, constrained);
        }
    }
}
