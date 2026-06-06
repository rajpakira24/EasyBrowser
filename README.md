# Easy Browser

Easy Browser is a privacy-focused Android browser built on Mozilla GeckoView, not the system WebView. It includes native tab management, tab groups, persistent browser state, ad and tracker blocking, HTTPS-only mode, private browsing, downloads with pause/resume, bookmarks, history, reading list, site permissions, and per-site user styles.

## Screenshots

| Home | Search overlay |
|:---:|:---:|
| ![Easy Browser home screen with privacy stats and photo background](test_screens/01_main.png) | ![Search overlay with focused address field](test_screens/02_search_popup.png) |
| **Page loaded** | **Tab manager** |
| ![Example Domain loaded in Easy Browser](test_screens/03_page_loaded.png) | ![Tab manager showing an open tab](test_screens/04_tab_manager.png) |

## Why GeckoView

Most alternative Android browsers wrap the system WebView, which means they inherit whatever the device vendor ships. Easy Browser embeds Gecko, the same engine that powers Firefox, giving consistent rendering and modern browser security features across Android 5.0+ devices.

## Features

- **Tabs and groups** - persistent normal tabs, private tabs that stay off disk, tab search, grouped tabs, inactive tabs, pinned tab metadata, thumbnails, and quick tab switching
- **Privacy** - three blocking levels, GeckoView Enhanced Tracking Protection, cookie-banner rejection, tracking-parameter stripping, Do Not Track, HTTPS-only mode, popup blocking, and optional screenshot protection
- **Downloads** - OkHttp-backed downloads with Range resume, MediaStore publishing on API 29+, Wi-Fi-only queuing, speed tracking, and pause/resume handling
- **Search** - DuckDuckGo by default, with configurable engines such as Brave Search, Google, Bing, Ecosia, Yahoo, and custom providers
- **Home screen** - privacy stats, quick access, photo-backed background, and bottom navigation for repeated browser workflows
- **Per-site controls** - user CSS injection, custom zoom, permissions, cookies, and storage controls
- **Reading list** - save pages for later reading
- **Extensions** - GeckoView web extension support hooks and extension-management UI

## Tech Stack

| | |
|---|---|
| Language | Java 11 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 37 |
| Engine | Mozilla GeckoView 143 |
| Database | Room 2.6.1 (`browser.db`, v7) |
| Network | OkHttp 5.3.2 |
| Image loading | Glide 5.0.7 |
| DI | None - singletons plus Android `ViewModel` / `AndroidViewModel` classes |

## Build

Use the Gradle wrapper from the repository root.

```powershell
# Debug APK
.\gradlew.bat assembleDebug

# Unit tests
.\gradlew.bat testDebugUnitTest

# Instrumented tests (requires a connected device or emulator)
.\gradlew.bat connectedDebugAndroidTest

# Lint
.\gradlew.bat lintDebug

# Clean generated build output
.\gradlew.bat clean
```

Release builds read signing values from `keystore.properties` at the project root. Copy `keystore.properties.example`, fill in real values locally, and keep the real file out of Git.

GeckoView is fetched from Mozilla's Maven repository, so the first build needs internet access.

## Architecture

```text
MainActivity
  -> BrowserActivity
        -> BrowserViewModel
              -> TabManager
                    -> Tab / TabGroup
                          -> GeckoSession
                                -> singleton GeckoRuntime

Room database
  -> DAO
        -> Repository
              -> Activity / ViewModel
```

- Browser delegates for navigation, content, progress, permissions, prompts, and history are configured from `BrowserActivity`.
- Normal tab and group metadata is persisted through Room entities and repository classes; private tab state is kept out of durable storage.
- `BrowserStateStore` keeps lightweight browser state in preferences, while `TabRepository` handles persisted tabs and groups.
- Repository background work uses the shared executor from `AppDatabase.getDatabaseExecutor()`.
- Blocking runs through GeckoView content blocking plus URL and host checks in `UrlUtils`.

See [CLAUDE.md](CLAUDE.md) for deeper architecture and preference notes.

## Security Posture

Recent hardening covers:

- `allowBackup="false"` and restrictive data-extraction rules
- FileProvider paths scoped to app-controlled download cache paths
- Intent extras and incoming URLs validated before navigation
- JavaScript and CSS injection paths escaped or encoded before use
- Path-traversal and bidi-control sanitization for downloaded filenames
- Range-download safeguards when a server returns HTTP 200 to a resumed request
- Immutable `PendingIntent` usage
- Optional `FLAG_SECURE` screenshot protection across activities
- HTTPS-only enforcement for top-level navigation

## License

[MIT](LICENSE) (c) 2026 Riju Pakira
