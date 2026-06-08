package com.webstudio.easybrowser.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.repository.HistoryRepository;
import com.webstudio.easybrowser.utils.ThemeEngine;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CookieManagerActivity extends AppCompatActivity {

    private LinearLayout hostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(root);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.cookie_manager);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ThemeEngine.applyChrome(this, toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56)));

        TextView hint = new TextView(this);
        hint.setText(R.string.cookie_manager_summary);
        hint.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8));
        hint.setTextColor(ContextCompat.getColor(this, R.color.gray));
        hint.setTextSize(13);
        root.addView(hint);

        hostList = new LinearLayout(this);
        hostList.setOrientation(LinearLayout.VERTICAL);
        root.addView(hostList);

        setContentView(scrollView);
        loadHosts();
    }

    private void loadHosts() {
        HistoryRepository repo = new HistoryRepository(this);
        repo.getAllHistory(new HistoryRepository.HistoryCallback() {
            @Override
            public void onHistoryLoaded(List<com.webstudio.easybrowser.models.HistoryItem> items) {
                Set<String> hosts = new LinkedHashSet<>();
                for (com.webstudio.easybrowser.models.HistoryItem item : items) {
                    String host = extractHost(item.getUrl());
                    if (host != null && !host.isEmpty()) hosts.add(host);
                }
                runOnUiThread(() -> populateHosts(new ArrayList<>(hosts)));
            }
            @Override public void onHistoryItemAdded(com.webstudio.easybrowser.models.HistoryItem item) {}
            @Override public void onHistoryCleared() {}
        });
    }

    private void populateHosts(List<String> hosts) {
        hostList.removeAllViews();
        if (hosts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_cookie_sites);
            empty.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray));
            hostList.addView(empty);
            return;
        }
        for (String host : hosts) {
            addHostRow(host);
        }
    }

    private void addHostRow(String host) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView hostView = new TextView(this);
        hostView.setText(host);
        hostView.setTextSize(15);
        hostView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hostView.setLayoutParams(lp);
        row.addView(hostView);

        TextView clearBtn = new TextView(this);
        clearBtn.setText(R.string.clear_site_cookies);
        clearBtn.setTextSize(13);
        clearBtn.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        clearBtn.setPadding(dpToPx(8), 0, 0, 0);
        clearBtn.setOnClickListener(v -> clearCookies(host, row));
        row.addView(clearBtn);

        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
        divider.setAlpha(0.3f);

        hostList.addView(row);
        hostList.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void clearCookies(String host, LinearLayout row) {
        try {
            GeckoRuntime runtime = RuntimeManager.getRuntime(this);
            runtime.getStorageController().clearDataFromHost(host,
                    StorageController.ClearFlags.COOKIES);
            int idx = hostList.indexOfChild(row);
            if (idx >= 0) {
                View divider = hostList.getChildAt(idx + 1);
                hostList.removeView(row);
                if (divider != null) hostList.removeView(divider);
            }
            Toast.makeText(this, R.string.cookies_cleared, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String extractHost(String url) {
        if (url == null) return null;
        try {
            Uri uri = Uri.parse(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
