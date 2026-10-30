package com.webstudio.easybrowser.ui.activity;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.PasswordStore;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.List;

/** Lists saved logins behind the device lock screen and lets the user view or delete them. */
public class PasswordsActivity extends AppCompatActivity {
    private PasswordStore store;
    private LinearLayout list;
    private ThemeEngine.Palette palette;
    private boolean unlocked;
    private ActivityResultLauncher<Intent> unlockLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.passwords);
        store = new PasswordStore(this);
        palette = ThemeEngine.homePalette(this);
        unlockLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        unlocked = true;
                        render();
                    } else {
                        finish();
                    }
                });
        unlock();
    }

    private void unlock() {
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguard == null || !keyguard.isDeviceSecure()) {
            // Without a device lock there is nothing to check against.
            unlocked = true;
            render();
            return;
        }
        Intent intent = keyguard.createConfirmDeviceCredentialIntent(
                getString(R.string.passwords_unlock_title), null);
        if (intent == null) {
            unlocked = true;
            render();
            return;
        }
        unlockLauncher.launch(intent);
    }

    private void render() {
        if (!unlocked) {
            return;
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(palette.panelStrongBackground);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(root);

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setText(R.string.save_passwords);
        toggle.setTextColor(palette.onSurface);
        toggle.setChecked(store.isSavingEnabled());
        toggle.setOnCheckedChangeListener((button, checked) ->
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(PasswordStore.PREF_SAVE_PASSWORDS, checked).apply());
        root.addView(toggle);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        List<PasswordStore.Login> logins = store.getAll();
        if (logins.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.passwords_empty);
            empty.setTextColor(palette.onSurfaceMuted);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, 0);
            list.addView(empty);
            return;
        }
        for (PasswordStore.Login login : logins) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(12), 0, dp(12));
            TextView origin = new TextView(this);
            origin.setText(login.origin);
            origin.setTextColor(palette.onSurface);
            origin.setTextSize(15);
            TextView user = new TextView(this);
            user.setText(login.username.isEmpty()
                    ? getString(R.string.password_no_username) : login.username);
            user.setTextColor(palette.onSurfaceMuted);
            row.addView(origin);
            row.addView(user);
            row.setOnClickListener(v -> showLogin(login));
            list.addView(row);
        }
    }

    private void showLogin(PasswordStore.Login login) {
        TextView body = new TextView(this);
        body.setPadding(dp(24), dp(12), dp(24), 0);
        body.setText(login.password);
        body.setTransformationMethod(PasswordTransformationMethod.getInstance());
        new AlertDialog.Builder(this)
                .setTitle(login.origin)
                .setMessage(login.username)
                .setView(body)
                .setNeutralButton(R.string.passwords_show, null)
                .setNegativeButton(R.string.delete, (d, w) -> confirmDelete(login))
                .setPositiveButton(R.string.cancel, null)
                .show()
                .getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener((View v) -> body.setTransformationMethod(null));
    }

    private void confirmDelete(PasswordStore.Login login) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.password_delete_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    store.delete(login.id);
                    refresh();
                    Toast.makeText(this, R.string.delete, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
