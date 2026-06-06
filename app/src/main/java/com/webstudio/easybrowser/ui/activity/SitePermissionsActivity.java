package com.webstudio.easybrowser.ui.activity;

import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.RuntimeManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.List;

public class SitePermissionsActivity extends AppCompatActivity {

    public static final String PREF_GRANTED_PERMISSIONS = "granted_permissions";

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
        String host, type;
        try {
            host = entry.getString("host");
            type = entry.getString("type");
        } catch (Exception e) {
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
        hostView.setText(host);
        hostView.setTextSize(15);
        hostView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface));
        textCol.addView(hostView);

        TextView typeView = new TextView(this);
        typeView.setText(type);
        typeView.setTextSize(12);
        typeView.setTextColor(ContextCompat.getColor(this, R.color.gray));
        textCol.addView(typeView);

        row.addView(textCol);

        TextView revokeBtn = new TextView(this);
        revokeBtn.setText(R.string.revoke_permission);
        revokeBtn.setTextSize(13);
        revokeBtn.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        revokeBtn.setPadding(dpToPx(8), 0, 0, 0);
        revokeBtn.setOnClickListener(v -> revokePermission(all, index, host, row));
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

        try {
            RuntimeManager.getRuntime(this).getStorageController()
                    .clearDataFromHost(host, StorageController.ClearFlags.PERMISSIONS);
        } catch (Exception ignored) {}

        int idx = permissionList.indexOfChild(row);
        if (idx >= 0) {
            View divider = permissionList.getChildAt(idx + 1);
            permissionList.removeView(row);
            if (divider != null) permissionList.removeView(divider);
        }
        Toast.makeText(this, R.string.permission_revoked, Toast.LENGTH_SHORT).show();
    }

    public static boolean hasPermission(SharedPreferences prefs, String host, String type) {
        if (prefs == null || host == null || type == null) {
            return false;
        }
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (host.equals(obj.optString("host")) && type.equals(obj.optString("type"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void recordPermission(SharedPreferences prefs, String host, String type) {
        String json = prefs.getString(PREF_GRANTED_PERMISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (host.equals(obj.optString("host")) && type.equals(obj.optString("type"))) {
                    return;
                }
            }
            JSONObject entry = new JSONObject();
            entry.put("host", host);
            entry.put("type", type);
            arr.put(entry);
            prefs.edit().putString(PREF_GRANTED_PERMISSIONS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
