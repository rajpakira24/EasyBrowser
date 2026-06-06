package com.webstudio.easybrowser.ui.activity;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.databinding.DialogTabGroupBinding;
import com.webstudio.easybrowser.repository.TabRepository;

final class TabGroupDialogHelper {
    interface Callback {
        void onSave(String groupName, int groupColor);
    }

    private TabGroupDialogHelper() {
    }

    static void show(Context context, int titleRes, String initialName, int initialColor,
                     boolean requireName, Callback callback) {
        DialogTabGroupBinding binding = DialogTabGroupBinding.inflate(LayoutInflater.from(context));
        if (initialName != null) {
            binding.groupNameInput.setText(initialName);
            binding.groupNameInput.selectAll();
        }
        if (!requireName) {
            binding.groupNameLayout.setVisibility(View.GONE);
        }
        int[] selectedColor = {
                initialColor != 0 ? initialColor : TabRepository.getDefaultGroupColor(context)
        };
        renderSwatches(context, binding.colorContainer, selectedColor);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = binding.groupNameInput.getText() != null
                    ? binding.groupNameInput.getText().toString().trim()
                    : "";
            if (requireName && name.isEmpty()) {
                binding.groupNameLayout.setError(context.getString(R.string.group_name));
                return;
            }
            callback.onSave(name, selectedColor[0]);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private static void renderSwatches(Context context, LinearLayout container, int[] selectedColor) {
        container.removeAllViews();
        for (int color : TabRepository.getDefaultGroupColors(context)) {
            View swatch = new View(context);
            int size = dp(context, 40);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, dp(context, 10), 0);
            swatch.setLayoutParams(params);
            swatch.setBackground(createSwatch(context, color, color == selectedColor[0]));
            swatch.setOnClickListener(v -> {
                selectedColor[0] = color;
                renderSwatches(context, container, selectedColor);
            });
            container.addView(swatch);
        }
    }

    private static GradientDrawable createSwatch(Context context, int color, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(selected ? 4 : 1,
                ContextCompat.getColor(context,
                        selected ? R.color.colorOnSurface : R.color.border_color));
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

}
