package com.webstudio.easybrowser.ui.activity;

import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.PasswordStore;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.List;
import java.util.Locale;

/** Saved logins behind the device lock screen: search, view, copy, open or delete. */
public class PasswordsActivity extends AppCompatActivity {
    private static final int[] AVATAR_COLORS = {
            0xFF3D7BE0, 0xFF8E5CD9, 0xFF1FA67A, 0xFFE0833D, 0xFFD6527A, 0xFF3AA6C9
    };

    private PasswordStore store;
    private ThemeEngine.Palette palette;
    private float density;
    private boolean unlocked;
    private String query = "";
    private LinearLayout list;
    private TextView countView;
    private ActivityResultLauncher<Intent> unlockLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new PasswordStore(this);
        palette = ThemeEngine.homePalette(this);
        density = getResources().getDisplayMetrics().density;
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
        Intent intent = keyguard != null && keyguard.isDeviceSecure()
                ? keyguard.createConfirmDeviceCredentialIntent(
                getString(R.string.passwords_unlock_title), null)
                : null;
        if (intent == null) {
            // Without a device lock there is nothing to check against.
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E161D);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        root.addView(makeHeader());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(4), dp(16), dp(24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        body.addView(makeSaveToggleCard());
        body.addView(makeSearchField());
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        refresh();
    }

    private View makeHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(8), dp(16), dp(8));

        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_arrow_back);
        back.setColorFilter(palette.onSurface);
        back.setBackground(rounded(0x00000000, dp(24)));
        back.setContentDescription(getString(R.string.back));
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(4), 0, 0, 0);
        TextView title = text(getString(R.string.passwords), 22, palette.onSurface, true);
        countView = text("", 13, palette.onSurfaceMuted, false);
        titles.addView(title);
        titles.addView(countView);
        header.addView(titles, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return header;
    }

    private View makeSaveToggleCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(12), dp(14));
        card.setBackground(card(palette.accent));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(getString(R.string.save_passwords), 16, palette.onSurface, true));
        TextView hint = text(getString(R.string.save_passwords_summary), 13,
                palette.onSurfaceMuted, false);
        hint.setPadding(0, dp(2), 0, 0);
        texts.addView(hint);
        card.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setChecked(store.isSavingEnabled());
        toggle.setOnCheckedChangeListener((button, checked) ->
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(PasswordStore.PREF_SAVE_PASSWORDS, checked).apply());
        card.addView(toggle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        return card;
    }

    private View makeSearchField() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), 0, dp(14), 0);
        box.setBackground(rounded(0x1FFFFFFF, dp(26)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_search);
        icon.setColorFilter(palette.onSurfaceMuted);
        box.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        EditText input = new EditText(this);
        input.setHint(R.string.passwords_search_hint);
        input.setHintTextColor(palette.onSurfaceMuted);
        input.setTextColor(palette.onSurface);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setBackground(null);
        input.setPadding(dp(10), dp(14), 0, dp(14));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.US);
                refresh();
            }
        });
        box.addView(input, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        box.setLayoutParams(params);
        return box;
    }

    private void refresh() {
        list.removeAllViews();
        List<PasswordStore.Login> all = store.getAll();
        countView.setText(getString(R.string.passwords_count, all.size()));
        if (all.isEmpty()) {
            list.addView(makeEmptyState(R.string.passwords_empty, R.string.passwords_empty_hint));
            return;
        }
        int shown = 0;
        for (PasswordStore.Login login : all) {
            if (!query.isEmpty() && !login.origin.toLowerCase(Locale.US).contains(query)
                    && !login.username.toLowerCase(Locale.US).contains(query)) {
                continue;
            }
            list.addView(makeRow(login));
            shown++;
        }
        if (shown == 0) {
            list.addView(makeEmptyState(R.string.passwords_no_results, 0));
        }
    }

    private View makeEmptyState(int titleRes, int hintRes) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setPadding(dp(24), dp(56), dp(24), 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_lock);
        icon.setColorFilter(palette.accent);
        icon.setPadding(dp(22), dp(22), dp(22), dp(22));
        icon.setBackground(rounded(0x24FFFFFF, dp(48)));
        empty.addView(icon, new LinearLayout.LayoutParams(dp(96), dp(96)));

        TextView title = text(getString(titleRes), 18, palette.onSurface, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(18), 0, 0);
        empty.addView(title);
        if (hintRes != 0) {
            TextView hint = text(getString(hintRes), 14, palette.onSurfaceMuted, false);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, dp(8), 0, 0);
            empty.addView(hint);
        }
        return empty;
    }

    private View makeRow(PasswordStore.Login login) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(card(0));
        row.addView(makeAvatar(login.origin, 44, 18));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(14), 0, dp(8), 0);
        TextView site = text(hostOf(login.origin), 16, palette.onSurface, true);
        site.setSingleLine(true);
        site.setEllipsize(TextUtils.TruncateAt.END);
        TextView user = text(login.username.isEmpty()
                ? getString(R.string.password_no_username) : login.username,
                13, palette.onSurfaceMuted, false);
        user.setSingleLine(true);
        user.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(site);
        texts.addView(user);
        row.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_arrow_forward);
        chevron.setColorFilter(palette.onSurfaceMuted);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        row.setOnClickListener(v -> showDetails(login));
        return row;
    }

    private void showDetails(PasswordStore.Login login) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setGravity(Gravity.CENTER_HORIZONTAL);
        sheet.setPadding(dp(20), dp(24), dp(20), dp(24));
        GradientDrawable bg = rounded(0xFF16222C, dp(24));
        bg.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        sheet.setBackground(bg);

        sheet.addView(makeAvatar(login.origin, 64, 26));
        TextView site = text(hostOf(login.origin), 20, palette.onSurface, true);
        site.setPadding(0, dp(12), 0, dp(16));
        sheet.addView(site);

        sheet.addView(makeField(R.string.passwords_username, login.username, false));
        sheet.addView(makeField(R.string.passwords_password, login.password, true));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        MaterialButton open = makeButton(R.string.passwords_open_site, false);
        open.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_URL, login.origin);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        MaterialButton delete = makeButton(R.string.delete, true);
        delete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(login);
        });
        actions.addView(open, weightedParams(true));
        actions.addView(delete, weightedParams(false));
        sheet.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(sheet);
        dialog.show();
    }

    private View makeField(int labelRes, String value, boolean secret) {
        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(14), dp(10), dp(6), dp(10));
        field.setBackground(rounded(0x1FFFFFFF, dp(14)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(getString(labelRes), 12, palette.onSurfaceMuted, false));
        TextView valueView = text(value, 16, palette.onSurface, false);
        valueView.setSingleLine(true);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        if (secret) {
            valueView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        texts.addView(valueView);
        field.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (secret) {
            MaterialButton reveal = makeTextButton(R.string.passwords_show);
            reveal.setOnClickListener(v -> {
                boolean hidden = valueView.getTransformationMethod() != null;
                valueView.setTransformationMethod(hidden
                        ? null : PasswordTransformationMethod.getInstance());
                reveal.setText(hidden ? R.string.passwords_hide : R.string.passwords_show);
            });
            field.addView(reveal);
        }
        MaterialButton copy = makeTextButton(R.string.passwords_copy);
        copy.setOnClickListener(v -> copyToClipboard(getString(labelRes), value, secret));
        field.addView(copy);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        field.setLayoutParams(params);
        return field;
    }

    private void copyToClipboard(String label, String value, boolean sensitive) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        ClipData clip = ClipData.newPlainText(label, value);
        if (sensitive) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.passwords_copied, Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(PasswordStore.Login login) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.password_delete_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    store.delete(login.id);
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private View makeAvatar(String origin, int sizeDp, int textSp) {
        String host = hostOf(origin);
        String letter = host.isEmpty() ? "?" : host.substring(0, 1).toUpperCase(Locale.US);
        TextView avatar = text(letter, textSp, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(AVATAR_COLORS[Math.abs(host.hashCode()) % AVATAR_COLORS.length],
                dp(sizeDp) / 2));
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return avatar;
    }

    private MaterialButton makeButton(int labelRes, boolean destructive) {
        MaterialButton button = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(labelRes);
        button.setAllCaps(false);
        button.setCornerRadius(dp(14));
        button.setTextColor(destructive ? 0xFFFF6B6B : palette.accent);
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                destructive ? 0x66FF6B6B : palette.panelBorder));
        return button;
    }

    private MaterialButton makeTextButton(int labelRes) {
        MaterialButton button = new MaterialButton(this, null,
                android.R.attr.borderlessButtonStyle);
        button.setText(labelRes);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(palette.accent);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        return button;
    }

    private LinearLayout.LayoutParams weightedParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = first ? 0 : dp(8);
        return params;
    }

    private GradientDrawable card(int accent) {
        GradientDrawable drawable = rounded(0x1AFFFFFF, dp(16));
        drawable.setStroke(dp(1), accent != 0 ? (accent & 0x00FFFFFF) | 0x55000000 : 0x14FFFFFF);
        return drawable;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private static String hostOf(String origin) {
        String host = Uri.parse(origin).getHost();
        if (host == null) {
            return origin;
        }
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
