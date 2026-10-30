package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.managers.PasswordStore;
import com.webstudio.easybrowser.models.Tab;

import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

class BrowserPromptDelegate implements GeckoSession.PromptDelegate {

    private final BrowserActivity activity;

    BrowserPromptDelegate(BrowserActivity activity) {
        this.activity = activity;
    }

    @Override
    public GeckoResult<PromptResponse> onFilePrompt(@NonNull GeckoSession session,
                                                    @NonNull FilePrompt prompt) {
        activity.pendingFilePrompt = prompt;
        activity.pendingFileResult = new GeckoResult<>();
        activity.launchFilePicker(prompt);
        return activity.pendingFileResult;
    }

    /** Asks before saving; the login itself is stored by LoginStorageDelegate once confirmed. */
    @Nullable
    @Override
    public GeckoResult<PromptResponse> onLoginSave(
            @NonNull GeckoSession session,
            @NonNull AutocompleteRequest<Autocomplete.LoginSaveOption> request) {
        Tab tab = activity.tabManager != null
                ? activity.tabManager.findTabBySession(session) : null;
        if (!new PasswordStore(activity).isSavingEnabled()
                || (tab != null && tab.isPrivate())
                || request.options.length == 0) {
            return GeckoResult.fromValue(request.dismiss());
        }
        Autocomplete.LoginSaveOption option = request.options[0];
        Autocomplete.LoginEntry entry = option.value;
        GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle(R.string.password_save_title)
                .setMessage(activity.getString(R.string.password_save_message,
                        entry.username == null ? "" : entry.username, entry.origin))
                .setPositiveButton(R.string.save,
                        (d, w) -> result.complete(request.confirm(option)))
                .setNegativeButton(R.string.password_not_now,
                        (d, w) -> result.complete(request.dismiss()))
                .setOnCancelListener(d -> result.complete(request.dismiss()))
                .show());
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onLoginSelect(
            @NonNull GeckoSession session,
            @NonNull AutocompleteRequest<Autocomplete.LoginSelectOption> request) {
        if (request.options.length == 0) {
            return GeckoResult.fromValue(request.dismiss());
        }
        String[] labels = new String[request.options.length];
        for (int i = 0; i < labels.length; i++) {
            String username = request.options[i].value.username;
            labels[i] = username == null || username.isEmpty()
                    ? activity.getString(R.string.password_no_username) : username;
        }
        GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle(R.string.password_fill_title)
                .setItems(labels, (d, which) ->
                        result.complete(request.confirm(request.options[which])))
                .setOnCancelListener(d -> result.complete(request.dismiss()))
                .show());
        return result;
    }
}
