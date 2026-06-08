package com.webstudio.easybrowser.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.UserStylesAdapter;
import com.webstudio.easybrowser.utils.ThemeEngine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserStylesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserStylesAdapter adapter;
    private TextView emptyView;
    private SharedPreferences prefs;
    private List<UserStylesAdapter.UserStyleEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_styles);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setupToolbar();
        initializeViews();
        loadStyles();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.user_styles);
        }
        ThemeEngine.applyChrome(this, toolbar);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.styles_recycler);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddDialog(null));

        adapter = new UserStylesAdapter(entries, new UserStylesAdapter.OnItemInteractionListener() {
            @Override
            public void onEnabledChanged(UserStylesAdapter.UserStyleEntry entry, boolean enabled) {
                entry.enabled = enabled;
                saveEntry(entry);
            }
            @Override
            public void onItemLongClick(UserStylesAdapter.UserStyleEntry entry) {
                showLongPressMenu(entry);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadStyles() {
        entries.clear();
        Map<String, ?> allPrefs = prefs.getAll();
        for (Map.Entry<String, ?> prefEntry : allPrefs.entrySet()) {
            String key = prefEntry.getKey();
            if (!key.startsWith("userstyle_")) continue;
            String hostname = key.substring("userstyle_".length());
            try {
                JSONObject obj = new JSONObject((String) prefEntry.getValue());
                String css = obj.optString("css", "");
                boolean enabled = obj.optBoolean("enabled", true);
                entries.add(new UserStylesAdapter.UserStyleEntry(hostname, css, enabled));
            } catch (Exception ignored) {}
        }
        adapter.setEntries(new ArrayList<>(entries));
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showAddDialog(UserStylesAdapter.UserStyleEntry existingEntry) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        EditText hostnameInput = new EditText(this);
        hostnameInput.setHint(getString(R.string.user_style_hostname));
        if (existingEntry != null) hostnameInput.setText(existingEntry.hostname);
        layout.addView(hostnameInput);

        EditText cssInput = new EditText(this);
        cssInput.setHint(getString(R.string.user_style_css));
        cssInput.setMinLines(4);
        cssInput.setGravity(android.view.Gravity.TOP);
        if (existingEntry != null) cssInput.setText(existingEntry.css);
        layout.addView(cssInput);

        String title = existingEntry == null ? getString(R.string.add_user_style) : getString(R.string.edit_user_style);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String host = hostnameInput.getText().toString().trim().toLowerCase();
                    String css = cssInput.getText().toString();
                    if (host.isEmpty()) {
                        Toast.makeText(this, R.string.user_style_hostname_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserStylesAdapter.UserStyleEntry entry = new UserStylesAdapter.UserStyleEntry(host, css, true);
                    saveEntry(entry);
                    loadStyles();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLongPressMenu(UserStylesAdapter.UserStyleEntry entry) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(entry.hostname)
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) showAddDialog(entry);
                    else deleteEntry(entry);
                })
                .show();
    }

    private void saveEntry(UserStylesAdapter.UserStyleEntry entry) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("css", entry.css);
            obj.put("enabled", entry.enabled);
            prefs.edit().putString("userstyle_" + entry.hostname, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    private void deleteEntry(UserStylesAdapter.UserStyleEntry entry) {
        prefs.edit().remove("userstyle_" + entry.hostname).apply();
        loadStyles();
        Toast.makeText(this, R.string.user_style_deleted, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.recommended_styles)
                .setIcon(android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            showRecommendedDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRecommendedDialog() {
        final String[] names = {
                "Force Dark Mode",
                "Reader Layout",
                "Hide Distractions",
                "Large Text",
                "High Contrast"
        };
        final String[] cssTemplates = {
                "html{filter:invert(1) hue-rotate(180deg)!important}img,video{filter:invert(1) hue-rotate(180deg)!important}",
                "body{max-width:720px!important;margin:0 auto!important;padding:20px!important;font-family:Georgia,serif!important;font-size:18px!important;line-height:1.7!important}",
                "[class*='ad'],[id*='ad'],[class*='banner'],[class*='popup'],[class*='cookie']{display:none!important}",
                "*{font-size:120%!important;line-height:1.5!important}",
                "body{background:#fff!important;color:#000!important}a{color:#00f!important}"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recommended_styles)
                .setItems(names, (dialog, which) -> {
                    dialog.dismiss();
                    showApplyHostnameDialog(names[which], cssTemplates[which]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showApplyHostnameDialog(String presetName, String css) {
        EditText hostnameInput = new EditText(this);
        hostnameInput.setHint(getString(R.string.user_style_hostname));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        hostnameInput.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle(presetName)
                .setMessage(R.string.apply_to_hostname)
                .setView(hostnameInput)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String host = hostnameInput.getText().toString().trim().toLowerCase();
                    if (host.isEmpty()) {
                        Toast.makeText(this, R.string.user_style_hostname_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserStylesAdapter.UserStyleEntry entry =
                            new UserStylesAdapter.UserStyleEntry(host, css, true);
                    saveEntry(entry);
                    loadStyles();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
