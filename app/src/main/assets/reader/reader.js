/*
 * Easy Browser reader mode.
 * Injected into the current page via a trusted javascript: URI. Extracts the main
 * article (Readability-style scoring), rebuilds it from a whitelist of tags and shows it
 * in a full-screen shadow-DOM overlay. Font size, font family and theme are applied from
 * the config object the app passes in (see BrowserActivity#applyReaderMode).
 */
(function () {
  if (window.__ebReader) {
    return;
  }

  var OVERLAY_ID = '__eb_reader_host';
  var SKIP_TAGS = /^(script|style|noscript|nav|aside|footer|form|iframe|object|embed|button|input|select|textarea|svg|canvas|dialog|template|link|meta)$/i;
  var KEEP_TAGS = /^(p|h1|h2|h3|h4|h5|h6|ul|ol|li|blockquote|pre|code|em|strong|b|i|u|a|img|figure|figcaption|br|hr|table|thead|tbody|tr|th|td|sup|sub|dl|dt|dd)$/i;
  var UNLIKELY = /comment|disqus|sidebar|sponsor|advert|promo|banner|cookie|popup|modal|social|share|related|recommend|newsletter|subscribe|footer|header|menu|breadcrumb|pagination|widget/i;
  var LIKELY = /article|body|content|entry|main|page|post|story|text|blog/i;

  var FONTS = {
    sans: '-apple-system, "Roboto", "Segoe UI", "Helvetica Neue", Arial, sans-serif',
    serif: 'Georgia, "Noto Serif", "Times New Roman", serif',
    mono: '"SFMono-Regular", Menlo, Consolas, "Roboto Mono", monospace'
  };
  var THEMES = {
    light: { bg: '#ffffff', fg: '#1b1b1b', muted: '#6b6b6b', link: '#1a5fb4', rule: '#e2e2e2' },
    dark: { bg: '#121212', fg: '#e4e4e4', muted: '#9a9a9a', link: '#8ab4f8', rule: '#2c2c2c' },
    sepia: { bg: '#f4ecd8', fg: '#433422', muted: '#7a6a55', link: '#8a4b14', rule: '#dccfb2' }
  };

  var state = { host: null, root: null, prevHtmlOverflow: '', cfg: null };

  function text(el) {
    return (el.textContent || '').replace(/\s+/g, ' ').trim();
  }

  function isHidden(el) {
    if (el.hidden || el.getAttribute('aria-hidden') === 'true') {
      return true;
    }
    var style = window.getComputedStyle(el);
    return style.display === 'none' || style.visibility === 'hidden';
  }

  function linkDensity(el) {
    var total = text(el).length;
    if (!total) {
      return 0;
    }
    var linked = 0;
    var links = el.getElementsByTagName('a');
    for (var i = 0; i < links.length; i++) {
      linked += text(links[i]).length;
    }
    return linked / total;
  }

  function weightOf(el) {
    var hint = (el.className || '') + ' ' + (el.id || '');
    var weight = 0;
    if (typeof hint === 'string') {
      if (UNLIKELY.test(hint) && !LIKELY.test(hint)) {
        weight -= 25;
      }
      if (LIKELY.test(hint)) {
        weight += 25;
      }
    }
    return weight;
  }

  function findArticleRoot() {
    var scores = new Map();
    var paragraphs = document.querySelectorAll('p, pre, td');
    var i;
    for (i = 0; i < paragraphs.length; i++) {
      var p = paragraphs[i];
      var content = text(p);
      if (content.length < 40 || isHidden(p)) {
        continue;
      }
      var score = 1 + content.split(',').length + Math.min(Math.floor(content.length / 100), 3);
      var parent = p.parentElement;
      var level = 0;
      while (parent && parent !== document.body && level < 3) {
        if (!scores.has(parent)) {
          scores.set(parent, weightOf(parent) + (parent.tagName === 'ARTICLE' ? 20 : 0));
        }
        scores.set(parent, scores.get(parent) + score / (level === 0 ? 1 : level * 2));
        parent = parent.parentElement;
        level++;
      }
    }
    var best = null;
    var bestScore = 0;
    scores.forEach(function (value, el) {
      var adjusted = value * (1 - linkDensity(el));
      if (adjusted > bestScore) {
        bestScore = adjusted;
        best = el;
      }
    });
    if (best) {
      return best;
    }
    return document.querySelector('article, main, [role=main]');
  }

  function absoluteUrl(url) {
    try {
      return new URL(url, document.baseURI).href;
    } catch (e) {
      return '';
    }
  }

  // Rebuilds the node from whitelisted tags only: no scripts, handlers, inline styles or classes survive.
  function sanitize(node, out) {
    for (var child = node.firstChild; child; child = child.nextSibling) {
      if (child.nodeType === 3) {
        out.appendChild(document.createTextNode(child.nodeValue));
        continue;
      }
      if (child.nodeType !== 1 || SKIP_TAGS.test(child.tagName) || isHidden(child)) {
        continue;
      }
      var tag = child.tagName.toLowerCase();
      var hint = (child.className || '') + ' ' + (child.id || '');
      if (typeof hint === 'string' && UNLIKELY.test(hint) && !LIKELY.test(hint)
          && tag !== 'p' && !/^h[1-6]$/.test(tag)) {
        continue;
      }
      if ((tag === 'div' || tag === 'section' || tag === 'ul' || tag === 'ol') && linkDensity(child) > 0.6
          && text(child).length < 600) {
        continue;
      }
      if (tag === 'picture') {
        var inner = child.querySelector('img');
        if (inner) {
          child = inner;
          tag = 'img';
        }
      }
      if (!KEEP_TAGS.test(tag)) {
        sanitize(child, out);
        continue;
      }
      if (tag === 'img') {
        var src = child.getAttribute('src') || child.getAttribute('data-src')
            || child.getAttribute('data-lazy-src') || child.getAttribute('data-original');
        var resolved = src && !/^data:/i.test(src) ? absoluteUrl(src) : '';
        if (!/^https?:/i.test(resolved) || (child.naturalWidth && child.naturalWidth < 80)) {
          continue;
        }
        var img = document.createElement('img');
        img.src = resolved;
        img.alt = child.getAttribute('alt') || '';
        img.loading = 'lazy';
        out.appendChild(img);
        continue;
      }
      var copy = document.createElement(tag);
      if (tag === 'a') {
        var href = absoluteUrl(child.getAttribute('href') || '');
        if (/^https?:/i.test(href)) {
          copy.href = href;
          copy.rel = 'noopener noreferrer';
        }
      }
      if (tag !== 'br' && tag !== 'hr') {
        sanitize(child, copy);
        if (tag !== 'td' && tag !== 'th' && tag !== 'tr' && !copy.querySelector('img')
            && !text(copy)) {
          continue;
        }
      }
      out.appendChild(copy);
    }
  }

  function meta(name) {
    var el = document.querySelector('meta[property="' + name + '"], meta[name="' + name + '"]');
    return el ? (el.getAttribute('content') || '').trim() : '';
  }

  function extract() {
    var root = findArticleRoot();
    if (!root) {
      return null;
    }
    var body = document.createElement('div');
    sanitize(root, body);
    var words = text(body).split(' ').filter(Boolean).length;
    if (words < 60) {
      return null;
    }
    var heading = root.querySelector('h1') || document.querySelector('h1');
    var title = (heading && text(heading)) || meta('og:title') || document.title || location.hostname;
    var firstHeading = body.querySelector('h1');
    if (firstHeading && text(firstHeading) === title) {
      firstHeading.parentNode.removeChild(firstHeading);
    }
    var byline = meta('author') || meta('article:author');
    var authorEl = root.querySelector('[rel=author], [itemprop=author], .byline, .author');
    if (!byline && authorEl && text(authorEl).length < 80) {
      byline = text(authorEl);
    }
    return { title: title, byline: byline, body: body, words: words };
  }

  function css(cfg) {
    var theme = THEMES[cfg.theme] || THEMES.light;
    var font = FONTS[cfg.fontFamily] || FONTS.sans;
    var size = Math.max(12, Math.min(40, Number(cfg.fontSize) || 18));
    return ':host{all:initial}'
      + '.wrap{position:fixed;inset:0;overflow-y:auto;-webkit-overflow-scrolling:touch;'
      + 'background:' + theme.bg + ';color:' + theme.fg + ';font-family:' + font + ';font-size:' + size + 'px;'
      + 'line-height:1.65;overflow-wrap:anywhere}'
      + 'article{max-width:42em;margin:0 auto;padding:24px 20px 96px}'
      + '.site{color:' + theme.muted + ';font-size:.75em;text-transform:uppercase;letter-spacing:.06em}'
      + 'h1.title{font-size:1.7em;line-height:1.25;margin:.4em 0 .3em}'
      + '.meta{color:' + theme.muted + ';font-size:.85em;margin-bottom:1.4em;padding-bottom:1em;'
      + 'border-bottom:1px solid ' + theme.rule + '}'
      + 'h2,h3,h4,h5,h6{line-height:1.3;margin:1.6em 0 .5em}h2{font-size:1.35em}h3{font-size:1.15em}'
      + 'p{margin:0 0 1em}a{color:' + theme.link + '}'
      + 'img{max-width:100%;height:auto;border-radius:6px;display:block;margin:1em auto}'
      + 'figure{margin:1em 0}figcaption{color:' + theme.muted + ';font-size:.8em;text-align:center}'
      + 'blockquote{margin:1em 0;padding:.1em 0 .1em 1em;border-left:3px solid ' + theme.rule + ';color:' + theme.muted + '}'
      + 'pre,code{font-family:' + FONTS.mono + ';font-size:.88em}'
      + 'pre{overflow-x:auto;padding:12px;border-radius:6px;background:' + theme.rule + '}'
      + 'table{border-collapse:collapse;display:block;overflow-x:auto}'
      + 'td,th{border:1px solid ' + theme.rule + ';padding:6px 10px}'
      + 'hr{border:0;border-top:1px solid ' + theme.rule + ';margin:1.5em 0}'
      + '.listen{position:fixed;right:16px;bottom:24px;padding:10px 18px;border:0;border-radius:24px;'
      + 'background:' + theme.link + ';color:' + theme.bg + ';font:600 14px sans-serif;box-shadow:0 2px 8px rgba(0,0,0,.3)}'
      + '.toast{position:fixed;left:50%;bottom:32px;transform:translateX(-50%);padding:10px 16px;'
      + 'border-radius:20px;background:#222;color:#fff;font:14px sans-serif}';
  }

  function applyStyle(cfg) {
    state.cfg = cfg;
    if (state.root) {
      state.root.querySelector('style').textContent = css(cfg);
    }
  }

  function toast(message) {
    var host = document.createElement('div');
    var root = host.attachShadow({ mode: 'closed' });
    var style = document.createElement('style');
    style.textContent = css(state.cfg || {});
    var note = document.createElement('div');
    note.className = 'toast';
    note.textContent = message;
    root.appendChild(style);
    root.appendChild(note);
    host.style.cssText = 'all:initial;position:fixed;z-index:2147483647;left:0;right:0;bottom:0';
    document.documentElement.appendChild(host);
    setTimeout(function () {
      if (host.parentNode) {
        host.parentNode.removeChild(host);
      }
    }, 2500);
  }

  var TTS_PREFIX = '__EASY_BROWSER_READER_TTS__';

  function sendSpeech(command) {
    var old = document.title;
    document.title = TTS_PREFIX + command;
    setTimeout(function () {
      if (document.title === TTS_PREFIX + command) {
        document.title = old;
      }
    }, 80);
  }

  function setSpeaking(speaking) {
    state.speaking = speaking;
    if (state.listen) {
      var strings = (state.cfg && state.cfg.strings) || {};
      state.listen.textContent = speaking ? (strings.stopListening || 'Stop') : (strings.listen || 'Listen');
    }
  }

  function close() {
    if (state.speaking) {
      sendSpeech('stop');
      state.speaking = false;
    }
    state.listen = null;
    if (state.host && state.host.parentNode) {
      state.host.parentNode.removeChild(state.host);
    }
    state.host = null;
    state.root = null;
    document.documentElement.style.overflow = state.prevHtmlOverflow;
  }

  function open(cfg) {
    state.cfg = cfg;
    if (state.host) {
      applyStyle(cfg);
      return;
    }
    var strings = cfg.strings || {};
    var article = extract();
    if (!article) {
      toast(strings.noArticle || 'No readable article found on this page');
      return;
    }
    var host = document.createElement('div');
    host.id = OVERLAY_ID;
    host.style.cssText = 'all:initial;position:fixed;inset:0;z-index:2147483647';
    var root = host.attachShadow({ mode: 'open' });
    var style = document.createElement('style');
    style.textContent = css(cfg);
    var wrap = document.createElement('div');
    wrap.className = 'wrap';
    var page = document.createElement('article');
    var site = document.createElement('div');
    site.className = 'site';
    site.textContent = location.hostname.replace(/^www\./, '');
    var title = document.createElement('h1');
    title.className = 'title';
    title.textContent = article.title;
    var info = document.createElement('div');
    info.className = 'meta';
    var minutes = Math.max(1, Math.round(article.words / 220));
    info.textContent = (article.byline ? article.byline + ' · ' : '')
        + minutes + ' ' + (strings.minRead || 'min read');
    page.appendChild(site);
    page.appendChild(title);
    page.appendChild(info);
    page.appendChild(article.body);
    wrap.appendChild(page);
    var listen = document.createElement('button');
    listen.className = 'listen';
    listen.textContent = strings.listen || 'Listen';
    listen.addEventListener('click', function () {
      if (state.speaking) {
        sendSpeech('stop');
        setSpeaking(false);
        return;
      }
      var spoken = article.title + '. ' + (article.body.textContent || '').replace(/\s+/g, ' ').trim();
      sendSpeech('play:' + encodeURIComponent(spoken));
      setSpeaking(true);
    });
    state.listen = listen;
    root.appendChild(style);
    root.appendChild(wrap);
    root.appendChild(listen);
    state.prevHtmlOverflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden';
    document.documentElement.appendChild(host);
    state.host = host;
    state.root = root;
  }

  window.__ebReader = {
    open: open,
    apply: applyStyle,
    close: close,
    speechDone: function () { setSpeaking(false); }
  };
})();
