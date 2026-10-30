package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        activity.runOnUiThread(() -> LoginPromptSheets.showSave(activity, entry.origin,
                entry.username,
                () -> result.complete(request.confirm(option)),
                () -> result.complete(request.dismiss())));
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
        GeckoResult<PromptResponse> result = new GeckoResult<>();
        String origin = request.options[0].value.origin;
        activity.runOnUiThread(() -> LoginPromptSheets.showSelect(activity, origin,
                usernames(request.options),
                which -> result.complete(request.confirm(request.options[which])),
                () -> result.complete(request.dismiss())));
        return result;
    }

    private static String[] usernames(Autocomplete.LoginSelectOption[] options) {
        String[] names = new String[options.length];
        for (int i = 0; i < names.length; i++) {
            String username = options[i].value.username;
            names[i] = username == null ? "" : username;
        }
        return names;
    }
}
