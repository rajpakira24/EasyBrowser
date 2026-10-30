package com.webstudio.easybrowser.managers;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;

import java.util.ArrayList;
import java.util.List;

/** Backs GeckoView's login autofill with {@link PasswordStore}. */
public final class LoginStorageDelegate implements Autocomplete.StorageDelegate {
    private final PasswordStore store;

    public LoginStorageDelegate(Context context) {
        store = new PasswordStore(context);
    }

    @NonNull
    @Override
    public GeckoResult<Autocomplete.LoginEntry[]> onLoginFetch(@NonNull String domain) {
        List<Autocomplete.LoginEntry> entries = new ArrayList<>();
        for (PasswordStore.Login login : store.getAll()) {
            if (login.origin.contains(domain)) {
                entries.add(toEntry(login));
            }
        }
        return GeckoResult.fromValue(entries.toArray(new Autocomplete.LoginEntry[0]));
    }

    @Override
    public void onLoginSave(@NonNull Autocomplete.LoginEntry login) {
        if (login.origin == null || login.password == null || !store.isSavingEnabled()) {
            return;
        }
        store.save(login.origin, login.username == null ? "" : login.username, login.password);
    }

    private static Autocomplete.LoginEntry toEntry(PasswordStore.Login login) {
        return new Autocomplete.LoginEntry.Builder()
                .guid(login.id)
                .origin(login.origin)
                .formActionOrigin(login.origin)
                .username(login.username)
                .password(login.password)
                .build();
    }
}
