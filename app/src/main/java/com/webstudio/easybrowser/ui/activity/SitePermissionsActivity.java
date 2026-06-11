package com.webstudio.easybrowser.ui.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;
import com.webstudio.easybrowser.utils.ThemeEngine;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SitePermissionsActivity extends AppCompatActivity {

    public static final String PREF_GRANTED_PERMISSIONS = "granted_permissions";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_HOST = "host";
    private static final String KEY_TYPE = "type";
    private static final String KEY_STATUS = "status";
    private static final String STATUS_ALLOW = "allow";
    private static final String STATUS_DENY = "deny";

    private LinearLayout permissionList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(root);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.site_permissions);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ThemeEngine.applyChrome(this, toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(56)));

        TextView hint = new TextView(this);
        hint.setText(R.string.granted_permissions);
        hint.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8));
        hint.setTextColor(ContextCompat.getColor(this, R.color.gray));
        hint.setTextSize(13);
        root.addView(hint);

        permissionList = new LinearLayout(this);
        permissionList.setOrientation(LinearLayout.VERTICAL);
        root.addView(permissionList);

        setContentView(scrollView);
        loadPermissions();
    }

    private void loadPermissions() {
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        List<JSONObject> entries = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                entries.add(arr.getJSONObject(i));
            }
        } catch (Exception ignored) {}

        permissionList.removeAllViews();
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_site_permissions);
            empty.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray));
            permissionList.addView(empty);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            addPermissionRow(entries, i, entries.get(i));
        }
    }

    private void addPermissionRow(List<JSONObject> all, int index, JSONObject entry) {
        String origin, host, type, status;
        try {
            origin = entry.optString(KEY_ORIGIN);
            host = entry.optString(KEY_HOST);
            type = entry.getString(KEY_TYPE);
            status = entry.optString(KEY_STATUS, STATUS_ALLOW);
        } catch (Exception e) {
            return;
        }
        String displayOrigin = !TextUtils.isEmpty(origin) ? origin : host;
        String clearHost = !TextUtils.isEmpty(host) ? host : hostFromOrigin(displayOrigin);
        if (TextUtils.isEmpty(displayOrigin)) {
            return;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView hostView = new TextView(this);
        hostView.setText(displayOrigin);
        hostView.setTextSize(15);
        hostView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        textCol.addView(hostView);

        TextView typeView = new TextView(this);
        String statusLabel = STATUS_DENY.equals(status)
                ? getString(R.string.blocked)
                : getString(R.string.allowed);
        typeView.setText(getString(R.string.permission_status_format, type, statusLabel));
        typeView.setTextSize(12);
        typeView.setTextColor(ContextCompat.getColor(this, R.color.gray));
        textCol.addView(typeView);

        row.addView(textCol);

        TextView revokeBtn = new TextView(this);
        revokeBtn.setText(R.string.revoke_permission);
        revokeBtn.setTextSize(13);
        revokeBtn.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        revokeBtn.setPadding(dpToPx(8), 0, 0, 0);
        revokeBtn.setOnClickListener(v -> revokePermission(all, index, clearHost, row));
        row.addView(revokeBtn);

        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
        divider.setAlpha(0.3f);

        permissionList.addView(row);
        permissionList.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void revokePermission(List<JSONObject> all, int index, String host, LinearLayout row) {
        all.remove(index);
        JSONArray updated = new JSONArray();
        for (JSONObject obj : all) updated.put(obj);
        prefs.edit().putString(PREF_GRANTED_PERMISSIONS, updated.toString()).apply();

        if (!TextUtils.isEmpty(host)) {
            try {
                RuntimeManager.getRuntime(this).getStorageController()
                        .clearDataFromHost(host, StorageController.ClearFlags.PERMISSIONS);
            } catch (Exception ignored) {}
        }

        int idx = permissionList.indexOfChild(row);
        if (idx >= 0) {
            View divider = permissionList.getChildAt(idx + 1);
            permissionList.removeView(row);
            if (divider != null) permissionList.removeView(divider);
        }
        Toast.makeText(this, R.string.permission_revoked, Toast.LENGTH_SHORT).show();
    }

    public static boolean hasPermission(SharedPreferences prefs, String origin, String type) {
        return hasPermissionStatus(prefs, origin, type, STATUS_ALLOW);
    }

    public static boolean isPermissionDenied(SharedPreferences prefs, String origin, String type) {
        return hasPermissionStatus(prefs, origin, type, STATUS_DENY);
    }

    private static boolean hasPermissionStatus(SharedPreferences prefs, String origin,
                                               String type, String status) {
        if (prefs == null || TextUtils.isEmpty(origin) || TextUtils.isEmpty(type)) {
            return false;
        }
        String host = hostFromOrigin(origin);
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (matchesSitePermission(obj, origin, host, type)
                        && status.equals(obj.optString(KEY_STATUS, STATUS_ALLOW))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static List<String> getPermissionsForHost(SharedPreferences prefs, String host) {
        List<String> permissions = new ArrayList<>();
        if (prefs == null || host == null) {
            return permissions;
        }
        String normalizedHost = host.toLowerCase(Locale.US);
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String origin = obj.optString(KEY_ORIGIN);
                String storedHost = obj.optString(KEY_HOST);
                if (normalizedHost.equals(storedHost)
                        || normalizedHost.equals(hostFromOrigin(origin))) {
                    String type = obj.optString(KEY_TYPE);
                    String status = obj.optString(KEY_STATUS, STATUS_ALLOW);
                    if (type != null && !type.trim().isEmpty() && !permissions.contains(type)) {
                        if (STATUS_DENY.equals(status)) {
                            permissions.add(type + " (blocked)");
                        } else {
                            permissions.add(type);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return permissions;
    }

    public static void recordPermission(SharedPreferences prefs, String origin, String type) {
        recordPermissionDecision(prefs, origin, type, STATUS_ALLOW);
    }

    public static void recordDeniedPermission(SharedPreferences prefs, String origin, String type) {
        recordPermissionDecision(prefs, origin, type, STATUS_DENY);
    }

    private static void recordPermissionDecision(SharedPreferences prefs, String origin,
                                                 String type, String status) {
        if (prefs == null || TextUtils.isEmpty(origin) || TextUtils.isEmpty(type)) {
            return;
        }
        String host = hostFromOrigin(origin);
        if (TextUtils.isEmpty(host)) {
            return;
        }
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray updated = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!matchesSitePermission(obj, origin, host, type)) {
                    updated.put(obj);
                }
            }
            JSONObject entry = new JSONObject();
            entry.put(KEY_ORIGIN, origin);
            entry.put(KEY_HOST, host);
            entry.put(KEY_TYPE, type);
            entry.put(KEY_STATUS, status);
            updated.put(entry);
            prefs.edit().putString(PREF_GRANTED_PERMISSIONS, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static boolean matchesSitePermission(JSONObject obj, String origin, String host,
                                                 String type) {
        if (obj == null || !type.equals(obj.optString(KEY_TYPE))) {
            return false;
        }
        String storedOrigin = obj.optString(KEY_ORIGIN);
        if (origin.equals(storedOrigin)) {
            return true;
        }
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        String storedHost = obj.optString(KEY_HOST);
        return host.equals(storedHost) || host.equals(hostFromOrigin(storedOrigin));
    }

    private static String hostFromOrigin(String origin) {
        if (TextUtils.isEmpty(origin)) {
            return "";
        }
        try {
            String host = Uri.parse(origin).getHost();
            if (!TextUtils.isEmpty(host)) {
                return host.toLowerCase(Locale.US);
            }
        } catch (Exception ignored) {
        }
        if (!origin.contains("://") && !containsWhitespace(origin)) {
            return origin.toLowerCase(Locale.US);
        }
        return "";
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
