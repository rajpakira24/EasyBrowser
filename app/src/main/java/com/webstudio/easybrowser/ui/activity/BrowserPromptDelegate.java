package com.webstudio.easybrowser.ui.activity;

import androidx.annotation.NonNull;

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
}