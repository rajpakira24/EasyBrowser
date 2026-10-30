browser.runtime.onMessage.addListener(function (message) {
    if (!message || message.type !== 'isSiteDisabled') {
        return undefined;
    }
    return browser.runtime.sendNativeMessage('browser', { host: message.host });
});
