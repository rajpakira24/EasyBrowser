package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.List;

/** A grid of the less common page tools, so the main menu can stay short. */
final class MoreToolsSheet {
    private static final int COLUMNS = 4;

    private MoreToolsSheet() {
    }

    static void show(Context context, int titleResId, List<MoreMenuPopup.Action> actions) {
        ThemeEngine.Palette palette = ThemeEngine.homePalette(context);
        float density = context.getResources().getDisplayMetrics().density;
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(density, 12), dp(density, 8), dp(density, 12), dp(density, 20));
        root.setBackground(rounded(0xFF16222C, dp(density, 24), true));

        View handle = new View(context);
        handle.setBackground(rounded(0x55FFFFFF, dp(density, 2), false));
        LinearLayout.LayoutParams handleParams =
                new LinearLayout.LayoutParams(dp(density, 40), dp(density, 4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(density, 12);
        root.addView(handle, handleParams);

        TextView title = new TextView(context);
        title.setText(titleResId);
        title.setTextColor(palette.onSurface);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(density, 8), 0, 0, dp(density, 8));
        root.addView(title);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(COLUMNS);
        for (MoreMenuPopup.Action action : actions) {
            grid.addView(makeCell(context, density, palette, action, dialog));
        }
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(root);
        dialog.show();
    }

    private static View makeCell(Context context, float density, ThemeEngine.Palette palette,
                                 MoreMenuPopup.Action action, BottomSheetDialog dialog) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(density, 4), dp(density, 10), dp(density, 4), dp(density, 6));

        ImageView icon = new ImageView(context);
        icon.setImageResource(action.iconResId);
        icon.setColorFilter(action.enabled ? palette.accent : palette.onSurfaceMuted);
        icon.setPadding(dp(density, 13), dp(density, 13), dp(density, 13), dp(density, 13));
        icon.setBackground(rounded(0x24FFFFFF, dp(density, 24), false));
        cell.addView(icon, new LinearLayout.LayoutParams(dp(density, 48), dp(density, 48)));

        TextView label = new TextView(context);
        label.setText(action.titleResId);
        label.setTextSize(11.5f);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setTextColor(action.enabled ? palette.onSurface : palette.onSurfaceMuted);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(density, 6);
        cell.addView(label, labelParams);

        cell.setAlpha(action.enabled ? 1f : 0.45f);
        cell.setEnabled(action.enabled);
        if (action.enabled) {
            cell.setOnClickListener(v -> {
                dialog.dismiss();
                action.runnable.run();
            });
        }
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        cell.setLayoutParams(params);
        return cell;
    }

    private static GradientDrawable rounded(int color, int radius, boolean topOnly) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        if (topOnly) {
            drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        } else {
            drawable.setCornerRadius(radius);
        }
        return drawable;
    }

    private static int dp(float density, int value) {
        return Math.round(value * density);
    }
}
