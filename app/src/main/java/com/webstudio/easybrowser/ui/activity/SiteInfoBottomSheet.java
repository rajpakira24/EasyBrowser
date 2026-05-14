package com.webstudio.easybrowser.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;

import org.mozilla.geckoview.StorageController;

public class SiteInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_URL = "url";
    private static final String ARG_SECURE = "secure";
    private static final String ARG_HOST = "host";

    public static SiteInfoBottomSheet newInstance(String url, boolean isSecure, String host) {
        SiteInfoBottomSheet sheet = new SiteInfoBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putBoolean(ARG_SECURE, isSecure);
        args.putString(ARG_HOST, host);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String host = args.getString(ARG_HOST, "");
        boolean isSecure = args.getBoolean(ARG_SECURE, false);
        String url = args.getString(ARG_URL, "");

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int dp16 = dp(16);
        int dp8 = dp(8);
        root.setPadding(dp16, dp16, dp16, dp16);

        TextView hostView = new TextView(requireContext());
        hostView.setText(host.isEmpty() ? url : host);
        hostView.setTextSize(18);
        hostView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(hostView);

        TextView secureView = new TextView(requireContext());
        secureView.setText(isSecure ? getString(R.string.site_info_secure) : getString(R.string.site_info_not_secure));
        secureView.setTextSize(14);
        secureView.setPadding(0, dp8, 0, dp16);
        root.addView(secureView);

        root.addView(makeButton(getString(R.string.clear_site_cookies), v -> clearData(host,
                StorageController.ClearFlags.COOKIES | StorageController.ClearFlags.AUTH_SESSIONS)));
        root.addView(makeButton(getString(R.string.clear_site_cache), v -> clearData(host,
                StorageController.ClearFlags.ALL_CACHES)));
        root.addView(makeButton(getString(R.string.clear_site_storage), v -> clearData(host,
                StorageController.ClearFlags.DOM_STORAGES)));
        root.addView(makeButton(getString(R.string.clear_site_permissions), v -> clearData(host,
                StorageController.ClearFlags.PERMISSIONS)));

        return root;
    }

    private void clearData(String host, long flags) {
        RuntimeManager.getRuntime(requireContext())
                .getStorageController()
                .clearDataFromHost(host, flags)
                .accept(v -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), R.string.site_data_cleared, Toast.LENGTH_SHORT).show());
                    }
                }, e -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private MaterialButton makeButton(String label, View.OnClickListener click) {
        MaterialButton btn = new MaterialButton(requireContext(),
                null, com.google.android.material.R.attr.borderlessButtonStyle);
        btn.setText(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        btn.setLayoutParams(params);
        btn.setOnClickListener(click);
        return btn;
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
