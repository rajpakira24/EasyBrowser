# Easy Browser

A privacy-focused Android browser built on **Mozilla GeckoView** — not WebView. Native tabs, ad/tracker blocking, HTTPS-only mode, private browsing, downloads with pause/resume, bookmarks, history, and per-site user styles.

## Why GeckoView

Most "alternative" Android browsers wrap the system WebView, which means they inherit whatever the OEM ships. Easy Browser embeds Gecko (the same engine that powers Firefox), giving consistent rendering and modern security features across all Android 5.0+ devices.

## Features

- **Tabs** — full lifecycle management, persistence across app restarts, private tabs that never touch disk
- **Privacy** — three blocking levels (off / balanced / aggressive), GeckoView Enhanced Tracking Protection, cookie-banner rejection, tracking-parameter stripping, Do-Not-Track, HTTPS-only mode
- **Downloads** — OkHttp-backed, Range-resume support, MediaStore publishing on API 29+, Wi-Fi-only queuing, bandwidth throttling
- **Search** — DuckDuckGo by default, configurable engines (Google, Bing, Brave, Ecosia, Yahoo), in-page autocomplete suggestions
- **Per-site features** — user CSS injection, custom zoom per host, granular site permissions
- **Reading list** — save pages for offline reading
- **Quick access** — most-visited grid on the home screen

## Tech stack

| | |
|---|---|
| Language | Java |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 36 |
| Engine | Mozilla GeckoView 143 |
| Database | Room 2.6.1 (`browser.db`, v3) |
| Network | OkHttp 5.3.2 |
| DI | None — singletons + `AndroidViewModel` |

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires keystore.properties — see keystore.properties.example)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests (needs a connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lintDebug
```

GeckoView is fetched from Mozilla's Maven (`https://maven.mozilla.org/maven2/`) — first build needs internet.

## Architecture

```
BrowserActivity        ← single-instance core browser UI
  └── BrowserViewModel
        └── TabManager
              └── Tab (id, title, url, isPrivate)
                    └── GeckoSession  ← one per tab, opened against the singleton GeckoRuntime
```

- **Delegates** (Navigation/Content/Progress/Permission/Prompt/History) are wired exclusively from `BrowserActivity.configureSession()`. `TabManager` does not touch delegates.
- **Persistence** — non-private tabs serialize to `SharedPreferences` as JSON (capped at 256 KB). Private tabs are dropped on process death by design.
- **Data layer** — entity → DAO → Repository → Activity, all on a shared executor from `AppDatabase.getDatabaseExecutor()`.
- **Two blocking paths run in parallel**: GeckoView's ContentBlocking and a hostname blocklist in `UrlUtils.isBlockedByAdBlock()` evaluated in `onLoadRequest`.

See [CLAUDE.md](CLAUDE.md) for a deeper architecture/preferences reference.

## Security posture

Recent hardening pass covers:
- `allowBackup="false"` and restrictive data-extraction rules
- FileProvider paths scoped to `cache/downloads/` only
- Intent-extra and `intent.getData()` validated to `http(s)` only
- Strict JS escaping on the new-tab data: page; user-CSS injection via base64 (no string interpolation)
- Path-traversal + bidi-control sanitization on Content-Disposition and URL-decoded filenames
- HTTP 200 to a Range request truncates the partial file (no append-onto-stale-data corruption)
- `FLAG_IMMUTABLE` unconditional on all PendingIntents
- `FLAG_SECURE` on Tabs/History to keep titles out of screenshots and recent-apps thumbnails
- HTTPS-only mode also blocks top-level `data:`/`javascript:` navigation (except the in-app new-tab page)

Lint is clean; `./gradlew lintDebug` reports zero errors.

## Release signing

`app/build.gradle` reads from `keystore.properties` at the project root. Copy `keystore.properties.example` and fill in real values. The file is in `.gitignore` and must never be committed.

## License

[MIT](LICENSE) © 2026 Riju Pakira
