package com.webstudio.easybrowser.ui.activity;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.utils.SettingsKeys;
import com.webstudio.easybrowser.utils.ThemeEngine;

import org.mozilla.geckoview.StorageController;

import java.util.List;

public class SiteInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_URL = "url";
    private static final String ARG_SECURE = "secure";
    private static final String ARG_HOST = "host";
    private static final String ARG_SHIELD_LEVEL = "shield_level";
    private static final String ARG_PRIVACY_SCORE = "privacy_score";
    private static final String ARG_BLOCKED_TODAY = "blocked_today";

    public static SiteInfoBottomSheet newInstance(String url, boolean isSecure, String host,
                                                  String shieldLevel, int privacyScore,
                                                  int blockedToday) {
        SiteInfoBottomSheet sheet = new SiteInfoBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putBoolean(ARG_SECURE, isSecure);
        args.putString(ARG_HOST, host);
        args.putString(ARG_SHIELD_LEVEL, shieldLevel);
        args.putInt(ARG_PRIVACY_SCORE, privacyScore);
        args.putInt(ARG_BLOCKED_TODAY, blockedToday);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String host = args.getString(ARG_HOST, "");
        String url = args.getString(ARG_URL, "");
        boolean isSecure = args.getBoolean(ARG_SECURE, false);
        String shieldLevel = args.getString(ARG_SHIELD_LEVEL, "MODERATE");
        int privacyScore = args.getInt(ARG_PRIVACY_SCORE, 0);
        int blockedToday = args.getInt(ARG_BLOCKED_TODAY, 0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        ThemeEngine.Palette palette = ThemeEngine.homePalette(requireContext());
        int shieldColor = getShieldColor(shieldLevel);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(12));

        View handle = new View(requireContext());
        handle.setBackground(createRoundedDrawable(0x55FFFFFF, dp(2)));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(12);
        root.addView(handle, handleParams);

        root.addView(makeHeader(host.isEmpty() ? url : host, isSecure, shieldColor, palette));
        root.addView(makeStatusRow(shieldLevel, privacyScore, blockedToday, shieldColor, palette));
        root.addView(makeInfoRow(R.drawable.ic_settings,
                getString(R.string.site_info_cookies_title),
                getCookieCompactSummary(prefs),
                palette.accent,
                palette));
        root.addView(makeInfoRow(R.drawable.ic_lock,
                getString(R.string.site_info_permissions_title),
                getPermissionsCompactSummary(prefs, host),
                shieldColor,
                palette));
        root.addView(makeActionsTitle(palette));
        root.addView(makeActionRow(
                makeActionButton(R.drawable.ic_clear,
                        getString(R.string.clear_site_cookies_short),
                        v -> clearData(host, StorageController.ClearFlags.COOKIES
                                | StorageController.ClearFlags.AUTH_SESSIONS),
                        palette),
                makeActionButton(R.drawable.ic_clear,
                        getString(R.string.clear_site_cache_short),
                        v -> clearData(host, StorageController.ClearFlags.ALL_CACHES),
                        palette)));
        root.addView(makeActionRow(
                makeActionButton(R.drawable.ic_file,
                        getString(R.string.clear_site_storage_short),
                        v -> clearData(host, StorageController.ClearFlags.DOM_STORAGES),
                        palette),
                makeActionButton(R.drawable.ic_lock,
                        getString(R.string.clear_site_permissions_short),
                        v -> clearData(host, StorageController.ClearFlags.PERMISSIONS),
                        palette)));

        return root;
    }

    private LinearLayout makeHeader(String title, boolean isSecure, int accent,
                                    ThemeEngine.Palette palette) {
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.addView(makeIconChip(R.drawable.ic_security, accent, dp(44)));

        LinearLayout titleColumn = new LinearLayout(requireContext());
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(10);
        titleColumn.setLayoutParams(params);

        TextView titleView = makeText(title, 17, palette.onSurface, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleColumn.addView(titleView);

        TextView secureView = makeText(
                isSecure ? getString(R.string.site_info_secure) : getString(R.string.site_info_not_secure),
                12, palette.onSurfaceMuted, false);
        secureView.setSingleLine(true);
        secureView.setEllipsize(TextUtils.TruncateAt.END);
        titleColumn.addView(secureView);
        header.addView(titleColumn);
        return header;
    }

    private LinearLayout makeStatusRow(String shieldLevel, int privacyScore, int blockedToday,
                                       int shieldColor, ThemeEngine.Palette palette) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(8));
        row.addView(makeStatusChip(R.drawable.ic_security,
                getSmartShieldLabel(shieldLevel),
                privacyScore + "/100",
                shieldColor,
                palette));
        row.addView(makeStatusChip(R.drawable.ic_clear,
                getString(R.string.blocked),
                String.valueOf(blockedToday),
                palette.accent,
                palette));
        return row;
    }

    private LinearLayout makeStatusChip(int iconRes, String label, String value, int accent,
                                        ThemeEngine.Palette palette) {
        LinearLayout chip = new LinearLayout(requireContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(9), dp(7), dp(9), dp(7));
        chip.setBackground(createRoundedStrokeDrawable(
                blend(accent, Color.BLACK, 0.78f),
                blend(accent, Color.WHITE, 0.65f),
                dp(12),
                dp(1)));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        chip.addView(icon, new LinearLayout.LayoutParams(dp(19), dp(19)));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(7);
        texts.setLayoutParams(textParams);
        TextView labelView = makeText(label, 11, palette.onSurfaceMuted, false);
        labelView.setSingleLine(true);
        TextView valueView = makeText(value, 15, palette.onSurface, true);
        valueView.setSingleLine(true);
        texts.addView(labelView);
        texts.addView(valueView);
        chip.addView(texts);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private LinearLayout makeInfoRow(int iconRes, String title, String summary, int accent,
                                     ThemeEngine.Palette palette) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(7), dp(2), dp(7));
        row.addView(makeIconChip(iconRes, accent, dp(34)));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(10);
        texts.setLayoutParams(params);
        TextView titleView = makeText(title, 13, palette.onSurface, true);
        TextView summaryView = makeText(summary, 12, palette.onSurfaceMuted, false);
        summaryView.setMaxLines(2);
        summaryView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(titleView);
        texts.addView(summaryView);
        row.addView(texts);
        return row;
    }

    private TextView makeActionsTitle(ThemeEngine.Palette palette) {
        TextView title = makeText(getString(R.string.site_info_actions_title),
                13, palette.onSurfaceMuted, true);
        title.setPadding(dp(2), dp(8), 0, dp(4));
        return title;
    }

    private LinearLayout makeActionRow(MaterialButton first, MaterialButton second) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(first);
        row.addView(second);
        return row;
    }

    private MaterialButton makeActionButton(int iconRes, String label, View.OnClickListener click,
                                            ThemeEngine.Palette palette) {
        MaterialButton button = new MaterialButton(requireContext(),
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(label);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setAllCaps(false);
        button.setMinHeight(dp(42));
        button.setMinimumHeight(dp(42));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setIconResource(iconRes);
        button.setIconSize(dp(17));
        button.setIconPadding(dp(6));
        button.setIconTint(ColorStateList.valueOf(palette.accent));
        button.setTextColor(palette.accent);
        button.setStrokeColor(ColorStateList.valueOf(palette.panelBorder));
        button.setBackgroundTintList(ColorStateList.valueOf(palette.panelBackground));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMargins(dp(2), dp(3), dp(2), dp(3));
        button.setLayoutParams(params);
        button.setOnClickListener(click);
        return button;
    }

    private ImageView makeIconChip(int iconRes, int iconColor, int size) {
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        icon.setBackground(createRoundedDrawable(blend(iconColor, Color.BLACK, 0.82f), size / 2));
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private TextView makeText(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private String getCookieCompactSummary(SharedPreferences prefs) {
        boolean bannersBlocked = prefs.getBoolean("block_cookie_banners", true);
        return getString(R.string.site_info_cookie_compact,
                getString(bannersBlocked
                        ? R.string.site_info_status_on
                        : R.string.site_info_status_off));
    }

    private String getPermissionsCompactSummary(SharedPreferences prefs, String host) {
        List<String> permissions = SitePermissionsActivity.getPermissionsForHost(prefs, host);
        String grants = permissions.isEmpty()
                ? getString(R.string.site_info_permissions_none)
                : getString(R.string.site_info_permissions_granted,
                TextUtils.join(", ", permissions));
        String defaults = getString(R.string.site_info_permission_defaults_compact,
                getPermissionValueLabel(prefs.getString(SettingsKeys.PREF_SITE_LOCATION,
                        SettingsKeys.VALUE_ASK)),
                getPermissionValueLabel(prefs.getString(SettingsKeys.PREF_SITE_CAMERA,
                        SettingsKeys.VALUE_ASK)),
                getPermissionValueLabel(prefs.getString(SettingsKeys.PREF_SITE_MICROPHONE,
                        SettingsKeys.VALUE_ASK)),
                getPermissionValueLabel(prefs.getString(SettingsKeys.PREF_SITE_NOTIFICATIONS,
                        SettingsKeys.VALUE_ASK)));
        return grants + " " + defaults;
    }

    private String getPermissionValueLabel(String value) {
        if (SettingsKeys.VALUE_ALLOW.equals(value)) {
            return getString(R.string.allowed);
        }
        if (SettingsKeys.VALUE_DENY.equals(value)) {
            return getString(R.string.not_allowed);
        }
        return getString(R.string.ask_first);
    }

    private void clearData(String host, long flags) {
        RuntimeManager.getRuntime(requireContext())
                .getStorageController()
                .clearDataFromHost(host, flags)
                .accept(v -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        R.string.site_data_cleared,
                                        Toast.LENGTH_SHORT).show());
                    }
                }, e -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        R.string.error_generic,
                                        Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private String getSmartShieldLabel(String level) {
        if ("SAFE".equals(level)) {
            return getString(R.string.smart_shield_safe_label);
        }
        if ("WARNING".equals(level)) {
            return getString(R.string.smart_shield_warning_label);
        }
        return getString(R.string.smart_shield_moderate_label);
    }

    private int getShieldColor(String level) {
        if ("SAFE".equals(level)) {
            return ContextCompat.getColor(requireContext(), R.color.smart_shield_safe);
        }
        if ("WARNING".equals(level)) {
            return ContextCompat.getColor(requireContext(), R.color.smart_shield_warning);
        }
        return ContextCompat.getColor(requireContext(), R.color.smart_shield_moderate);
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createRoundedStrokeDrawable(int color, int strokeColor,
                                                        int radius, int strokeWidth) {
        GradientDrawable drawable = createRoundedDrawable(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int blend(int color, int target, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        float source = 1f - clamped;
        return Color.rgb(
                Math.round(Color.red(color) * source + Color.red(target) * clamped),
                Math.round(Color.green(color) * source + Color.green(target) * clamped),
                Math.round(Color.blue(color) * source + Color.blue(target) * clamped));
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
