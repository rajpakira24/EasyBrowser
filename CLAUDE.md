# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All commands are run via Gradle wrapper from the project root.

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires real keystore — update signingConfigs in app/build.gradle first)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.webstudio.easybrowser.ExampleUnitTest"

# Lint
./gradlew lint

# Clean
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

GeckoView is fetched from `https://maven.mozilla.org/maven2/` — an internet connection is required on first build.

## Architecture Overview

**Language:** Java. **Min SDK:** 21. **Target SDK:** 35.

### Browser Engine

The app uses **Mozilla GeckoView** (not Android WebView) as its rendering engine. The singleton `GeckoRuntime` is managed by `RuntimeManager`, which must be initialized once per process. `GeckoSession` is the per-tab browsing context; each `Tab` object owns exactly one `GeckoSession`.

### Tab Lifecycle

```
BrowserViewModel (AndroidViewModel)
  └── TabManager               — owns List<Tab>, persists non-private tabs to SharedPreferences as JSON
        └── Tab                — holds GeckoSession + metadata (id, title, url, isPrivate)
              └── GeckoSession — the actual browser tab (opened against GeckoRuntime)
```

`BrowserViewModel` survives configuration changes and holds `TabManager`. `BrowserActivity` retrieves it via `ViewModelProvider`. When switching tabs, `BrowserActivity.attachTabToView()` calls `geckoView.setSession(session)` — GeckoView only renders one session at a time.

**Important:** Delegates on `GeckoSession` (NavigationDelegate, ContentDelegate, ProgressDelegate, PermissionDelegate, PromptDelegate) are set exclusively in `BrowserActivity.configureSession()`. `TabManager` does NOT set delegates. `BrowserActivity` explicitly calls `tabManager.updateTabUrl()` / `tabManager.updateTabTitle()` from inside its own delegates to keep tab metadata in sync.

Private tabs are never persisted to SharedPreferences and are lost on process death.

### Data Layer

Room database (`browser.db`, version 2) with four entities and a standard DAO → Repository → Activity pattern. All repository operations are async, using a background `Executor` + callback interfaces (no LiveData or Coroutines).

| Entity | DAO | Repository | Purpose |
|---|---|---|---|
| `BookmarkEntity` | `BookmarkDao` | `BookmarkRepository` | Saved bookmarks |
| `HistoryEntity` | `HistoryDao` | `HistoryRepository` | Browsing history |
| `QuickAccessEntity` | `QuickAccessDao` | `QuickAccessRepository` | Most-visited sites (auto-updated on each page visit) |
| `DownloadEntity` | `DownloadDao` | `DownloadRepository` | Download records |

Database migration from v1→v2 creates the `downloads` table (defined in `AppDatabase`).

### Download Manager

`AppDownloadManager` is a singleton that uses **OkHttp** for HTTP downloads. Downloads are written to `getCacheDir()/downloads` first, then published to MediaStore (API 29+) or `Environment.DIRECTORY_DOWNLOADS/Easy Browser` (legacy). Supports pause (via HTTP Range resume), cancel, and progress notifications.

### Privacy / Content Blocking

Privacy settings flow: `SettingsActivity` → `SharedPreferences` → `RuntimeManager.getRuntime()` applies them live to `GeckoRuntime.getSettings()`.

Two layers of ad/tracker blocking run in parallel:
1. **GeckoView ContentBlocking** — ETP levels (none/default/strict), anti-tracking flags, cookie banner rejection, tracking parameter stripping. Configured in `RuntimeManager`.
2. **URL hostname blocklist** in `UrlUtils.isBlockedByAdBlock()` — checked in `BrowserActivity`'s `onLoadRequest` delegate before GeckoView loads the URL.

Blocking levels (`ad_blocking_level` pref): `"off"`, `"balanced"` (default), `"aggressive"`.

### Activity / Screen Map

- `MainActivity` — home screen: search bar (opens `BrowserActivity`), quick-access grid, privacy stats counters.
- `BrowserActivity` — core browser. Single instance (`launchMode="singleTask"`). Handles deep links via `onNewIntent`.
- `TabsActivity` — tab switcher. Launched via `ActivityResultLauncher`; communicates back to `BrowserActivity` by returning an `Intent` with closed/selected tab IDs.
- `SettingsActivity` — all preferences. Changes take effect immediately via `RuntimeManager`.
- `BookmarksActivity`, `HistoryActivity`, `DownloadsActivity` — list screens backed by Room repositories.
- `ExtensionsActivity` — stub/placeholder.

### URL Handling

`UrlUtils.getUrlOrSearchUrl()` is the single entry point for all user input. It calls `isSearchQuery()` to decide between passing input to the configured search engine or treating it as a URL to sanitize. The new-tab page is a self-contained `data:text/html` page generated in `UrlUtils.getNewTabPageUrl()`.

### SharedPreferences Keys

All preferences use `PreferenceManager.getDefaultSharedPreferences()` (AndroidX). Key names used across multiple files:

| Key | Default | Used in |
|---|---|---|
| `search_engine_url` | DuckDuckGo URL | `UrlUtils`, `BrowserActivity`, `SettingsActivity` |
| `homepage` | `https://duckduckgo.com` | `UrlUtils`, `SettingsActivity` |
| `ad_blocking_level` | `"balanced"` | `RuntimeManager`, `BrowserActivity`, `UrlUtils` |
| `javascript_enabled` | `true` | `RuntimeManager`, `TabManager`, `BrowserActivity` |
| `block_popups` | `true` | `BrowserActivity` |
| `save_history` | `true` | `BrowserActivity` |
| `do_not_track` | `false` | `RuntimeManager`, `SettingsActivity` |
| `block_cookie_banners` | `true` | `RuntimeManager`, `SettingsActivity` |
| `strip_tracking_params` | `true` | `RuntimeManager`, `SettingsActivity` |
| `show_privacy_stats` | `true` | `MainActivity`, `SettingsActivity` |
| `show_quick_access` | `true` | `MainActivity`, `SettingsActivity` |
| `privacy_pages_protected` | 0 | `BrowserActivity`, `MainActivity` |
| `privacy_items_blocked` | 0 | `BrowserActivity`, `MainActivity` |
| `saved_tabs` | — | `TabManager` (JSON array) |

### Firebase / Crashlytics

Optional. The Crashlytics and Google Services Gradle plugins are applied only when `app/google-services.json` is present. Local debug builds work without it.

### Release Signing

`app/build.gradle` contains placeholder signing config (`path/to/your/keystore.jks`). Update `signingConfigs.release` with real values before a release build. ProGuard/R8 is currently disabled (`minifyEnabled false`) — enabling it requires adding GeckoView-specific keep rules.
