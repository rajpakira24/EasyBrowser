(function () {
    if (window.__easyBrowserAdBlockerInstalled) {
        if (window.__easyBrowserRunAdBlocker) {
            window.__easyBrowserRunAdBlocker();
        }
        return;
    }

    window.__easyBrowserAdBlockerInstalled = true;

    var css = [
        'ins.adsbygoogle',
        'iframe[id^="google_ads_iframe_"]',
        'iframe[src*="doubleclick"]',
        'iframe[src*="googlesyndication"]',
        'iframe[src*="googleadservices"]',
        'iframe[src*="googletagservices"]',
        'iframe[src*="taboola"]',
        'iframe[src*="outbrain"]',
        'a[href^="https://ad.doubleclick.net/"]',
        'a[href^="https://adclick.g.doubleclick.net/"]',
        'a[href^="https://pubads.g.doubleclick.net/"]',
        'a[href^="https://www.googleadservices.com/pagead/aclk?"]',
        '[id^="ad-"]',
        '[id^="ad_"]',
        '[id*="-ad-"]',
        '[id*="_ad_"]',
        '[id*="adslot"]',
        '[id*="ad-slot"]',
        '[id*="outbrain"]',
        '[id*="taboola"]',
        '[class~="ad"]',
        '[class~="ads"]',
        '[class^="ad-"]',
        '[class^="ad_"]',
        '[class*=" ad-"]',
        '[class*=" ad_"]',
        '[class*=" ads-"]',
        '[class*=" ads_"]',
        '[class*="ad-slot"]',
        '[class*="adSlot"]',
        '[class*="ad_container"]',
        '[class*="ad-container"]',
        '[class*="ad__"]',
        '[class*="ads__"]',
        '[class*="advertisement"]',
        '[class*="dfp"]',
        '[class*="sponsor"]',
        '[class*="sponsored"]',
        '[class*="outbrain"]',
        '[class*="taboola"]',
        '[data-ad]',
        '[data-ad-slot]',
        '[data-ad-client]',
        '[data-testid*="ad-"]',
        '[data-testid*="advert"]',
        '[aria-label="Advertisement"]',
        '.GoogleDoubleClick-SponsorText',
        '.doubleClickAd',
        '.doubleclickAds',
        '.OUTBRAIN',
        '.Outbrain',
        '.outbrain',
        '.outbrain-ad-slot',
        '.outbrain-ads',
        '.outbrain-widget',
        '.outbrain-wrapper',
        '.TaboolaMoreToExplore_taboolaContainerWrapper__Ccf_j',
        '.ad-feedback__modal',
        '.ad-slot-dynamic',
        '.ad-slot-header__wrapper',
        '.ad-slot__ad-wrapper',
        '.stack__ads',
        '.zone__ads',
        '#js-outbrain-rightrail-ads-module',
        '#partner-zone',
        '#sponsored-outbrain-1',
        '[data-zone-label="Paid Partner Content"]'
    ].join(',') + '{display:none!important;visibility:hidden!important;}';

    function ensureStyle() {
        if (!document.documentElement) {
            return;
        }
        var style = document.getElementById('easybrowser-adblock-css');
        if (!style) {
            style = document.createElement('style');
            style.id = 'easybrowser-adblock-css';
            (document.head || document.documentElement).appendChild(style);
        }
        style.textContent = css;
    }

    function hide(node) {
        if (!node || node === document.body || node === document.documentElement) {
            return;
        }
        node.setAttribute('data-easybrowser-adblocked', 'true');
        node.style.setProperty('display', 'none', 'important');
        node.style.setProperty('visibility', 'hidden', 'important');
    }

    function isAdResource(value) {
        return /(doubleclick|googlesyndication|googleadservices|googletagservices|adnxs|adsrvr|amazon-adsystem|taboola|outbrain|moatads|scorecardresearch|quantserve|criteo|openx|pubmatic|rubiconproject|yieldmo|zedo)/i.test(value || '');
    }

    function closestAdContainer(node) {
        var current = node;
        for (var i = 0; i < 7 && current && current.parentElement; i++) {
            var rect = current.getBoundingClientRect ? current.getBoundingClientRect() : null;
            if (rect && rect.width > 120 && rect.height > 40 &&
                    rect.height < Math.max(900, window.innerHeight * 0.85)) {
                var text = (current.textContent || '').toLowerCase();
                if (current.querySelector('iframe,img,ins,[data-ad],[data-ad-slot],[class*="ad-slot"],[class*="ad-container"]') ||
                        text.indexOf('ad feedback') >= 0 || text.indexOf('advertisement') >= 0) {
                    return current;
                }
            }
            current = current.parentElement;
        }
        return node;
    }

    function scanAdLabels() {
        var nodes = document.querySelectorAll('div,section,aside,figure,p,span');
        for (var i = 0; i < nodes.length && i < 3000; i++) {
            var node = nodes[i];
            if (node.getAttribute('data-easybrowser-adblocked') === 'true') {
                continue;
            }
            var text = (node.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            if (text === 'advertisement' || text === 'ad feedback' ||
                    text === 'sponsored' || text === 'sponsored content') {
                hide(closestAdContainer(node));
            }
        }
    }

    function scanResources() {
        var nodes = document.querySelectorAll('iframe,img,script,source,ins,a');
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var value = node.src || node.href || node.getAttribute('data-src') || '';
            if (isAdResource(value)) {
                hide(closestAdContainer(node));
            }
        }
    }

    var pending = false;
    window.__easyBrowserRunAdBlocker = function () {
        if (pending) {
            return;
        }
        pending = true;
        var run = window.requestAnimationFrame || function (callback) {
            return setTimeout(callback, 16);
        };
        run(function () {
            pending = false;
            ensureStyle();
            scanResources();
            scanAdLabels();
        });
    };

    ensureStyle();
    window.__easyBrowserRunAdBlocker();
    setTimeout(window.__easyBrowserRunAdBlocker, 500);
    setTimeout(window.__easyBrowserRunAdBlocker, 1500);
    setTimeout(window.__easyBrowserRunAdBlocker, 3500);

    if (window.MutationObserver && document.documentElement) {
        new MutationObserver(window.__easyBrowserRunAdBlocker).observe(
            document.documentElement,
            { childList: true, subtree: true }
        );
    }
})();
