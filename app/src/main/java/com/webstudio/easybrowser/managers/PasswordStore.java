package com.webstudio.easybrowser.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Saved logins. The whole list is one JSON document encrypted with an AES-GCM key that
 * lives in the Android Keystore, so the plain text never reaches SharedPreferences.
 */
public final class PasswordStore {
    public static final String PREF_SAVE_PASSWORDS = "save_passwords";

    private static final String TAG = "PasswordStore";
    private static final String KEY_ALIAS = "easybrowser_passwords";
    private static final String PREF_DATA = "saved_logins_encrypted";
    private static final Object LOCK = new Object();

    public static final class Login {
        public final String id;
        public final String origin;
        public final String username;
        public final String password;

        public Login(String id, String origin, String username, String password) {
            this.id = id;
            this.origin = origin;
            this.username = username;
            this.password = password;
        }
    }

    private final SharedPreferences prefs;

    public PasswordStore(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public boolean isSavingEnabled() {
        return prefs.getBoolean(PREF_SAVE_PASSWORDS, true);
    }

    public List<Login> getAll() {
        synchronized (LOCK) {
            return read();
        }
    }

    public List<Login> getForOrigin(@Nullable String origin) {
        List<Login> matches = new ArrayList<>();
        if (origin == null) {
            return matches;
        }
        for (Login login : getAll()) {
            if (login.origin.equalsIgnoreCase(origin)) {
                matches.add(login);
            }
        }
        return matches;
    }

    /** Adds a login, or updates the password when the origin and username already exist. */
    public void save(@NonNull String origin, @NonNull String username, @NonNull String password) {
        synchronized (LOCK) {
            List<Login> logins = read();
            boolean updated = false;
            for (int i = 0; i < logins.size(); i++) {
                Login existing = logins.get(i);
                if (existing.origin.equalsIgnoreCase(origin)
                        && existing.username.equals(username)) {
                    logins.set(i, new Login(existing.id, origin, username, password));
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                logins.add(new Login(UUID.randomUUID().toString(), origin, username, password));
            }
            write(logins);
        }
    }

    public void delete(@NonNull String id) {
        synchronized (LOCK) {
            List<Login> logins = read();
            for (int i = 0; i < logins.size(); i++) {
                if (logins.get(i).id.equals(id)) {
                    logins.remove(i);
                    break;
                }
            }
            write(logins);
        }
    }

    public void clear() {
        synchronized (LOCK) {
            prefs.edit().remove(PREF_DATA).apply();
        }
    }

    private List<Login> read() {
        List<Login> logins = new ArrayList<>();
        String stored = prefs.getString(PREF_DATA, null);
        if (stored == null) {
            return logins;
        }
        try {
            byte[] blob = Base64.decode(stored, Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, blob, 0, 12));
            String json = new String(cipher.doFinal(blob, 12, blob.length - 12), "UTF-8");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                logins.add(new Login(item.getString("id"), item.getString("origin"),
                        item.optString("username"), item.optString("password")));
            }
        } catch (GeneralSecurityException | JSONException | java.io.IOException e) {
            Log.e(TAG, "Failed to read saved logins", e);
        }
        return logins;
    }

    private void write(List<Login> logins) {
        try {
            JSONArray array = new JSONArray();
            for (Login login : logins) {
                array.put(new JSONObject()
                        .put("id", login.id)
                        .put("origin", login.origin)
                        .put("username", login.username)
                        .put("password", login.password));
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(array.toString().getBytes("UTF-8"));
            byte[] blob = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, blob, 0, iv.length);
            System.arraycopy(encrypted, 0, blob, iv.length, encrypted.length);
            prefs.edit().putString(PREF_DATA, Base64.encodeToString(blob, Base64.NO_WRAP)).apply();
        } catch (GeneralSecurityException | JSONException | java.io.IOException e) {
            Log.e(TAG, "Failed to save logins", e);
        }
    }

    private SecretKey getKey() throws GeneralSecurityException, java.io.IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
