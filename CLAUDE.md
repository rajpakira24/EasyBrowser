# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

For the full current architecture inventory, see `PROJECT_ARCHITECTURE_REPORT.md`.

## Build Commands

All commands are run via the Gradle wrapper from the project root.

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires a real keystore - update signingConfigs in app/build.gradle first)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.webstudio.easybrowser.ExampleUnitTest"

# Lint
./gradlew lintDebug

# Clean
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

GeckoView is fetched from `https://maven.mozilla.org/maven2/`; an internet connection is required on first build.

## Architecture Overview

**Language:** Java. **Min SDK:** 21. **Target SDK:** 37.

### Browser Engine

The app uses **Mozilla GeckoView** rather than Android WebView. The singleton `GeckoRuntime` is managed by `RuntimeManager`, which must be initialized once per process. Each `Tab` owns exactly one `GeckoSession`.

### Tab Lifecycle

```text
BrowserViewModel (AndroidViewModel)
  -> TabManager
       -> BrowserStateStore       runtime tabs, groups, active ids, private mode
       -> TabRepository           Room persistence for regular tabs and groups
       -> Tab                     GeckoSession plus metadata
            -> GeckoSession       browser session opened against GeckoRuntime
```

`BrowserViewModel` survives configuration changes and holds `TabManager`. `BrowserActivity` retrieves it via `ViewModelProvider`. When switching tabs, `BrowserActivity.attachTabToView()` calls `geckoView.setSession(session)` because GeckoView renders one session at a time.

`TabManager` restores normal tabs from Room first. If Room restore fails or has no usable state, it falls back to the legacy `saved_tabs` SharedPreferences JSON and then persists migrated state. The fallback is capped by `MAX_PERSIST_BYTES` to avoid oversized SharedPreferences payloads.

Private tabs are runtime-only. `TabRepository` skips private tabs/groups on save, and `TabManager` clears any persisted private tab/group rows while restoring.

**Important:** Delegates on `GeckoSession` are set by the browser activity delegate classes (`BrowserNavigationDelegate`, `BrowserContentDelegate`, `BrowserProgressDelegate`, `BrowserPermissionDelegate`, `BrowserPromptDelegate`, and related helpers). `TabManager` does not own Gecko delegate behavior; it owns tab/session state and persistence coordination.

### Data Layer

Room database: `browser.db`, version 7. The project uses DAO -> Repository -> Activity/ViewModel style with callback-based async APIs.

| Entity | DAO | Repository | Purpose |
|---|---|---|---|
| `BookmarkEntity` | `BookmarkDao` | `BookmarkRepository` | Saved bookmarks |
| `HistoryEntity` | `HistoryDao` | `HistoryRepository` | Browsing history |
| `QuickAccessEntity` | `QuickAccessDao` | `QuickAccessRepository` | Most-visited / quick access sites |
| `DownloadEntity` | `DownloadDao` | `DownloadRepository` | Download records |
| `ReadingListEntity` | `ReadingListDao` | `ReadingListRepository` | Saved reading-list items |
| `TabGroupEntity` | `TabGroupDao` | `TabRepository` | Persisted tab groups |
| `TabEntity` | `TabGroupDao` | `TabRepository` | Persisted normal tabs |

`AppDatabase` defines migrations `1->2` through `6->7`. Version 7 adds the `pinned` column to `tabs`. Most repositories use `AppDatabase.getDatabaseExecutor()`; `ReadingListRepository` still creates its own single-thread executor, which is tracked as technical debt.

### Download Manager

`AppDownloadManager` is a singleton that uses **OkHttp** for HTTP downloads. Downloads are written to `getCacheDir()/downloads` first, then published to MediaStore on API 29+ or to `Environment.DIRECTORY_DOWNLOADS/Easy Browser` on older Android versions. It supports pause via HTTP Range resume, cancel, progress tracking, and notifications.

### Privacy / Content Blocking

Privacy settings flow: `SettingsActivity` / `SettingsSubpageActivity` -> `SharedPreferences` -> `RuntimeManager.getRuntime()` applies live Gecko runtime settings.

Two layers of ad/tracker blocking run in parallel:

1. **GeckoView ContentBlocking** for enhanced tracking protection levels, anti-tracking flags, cookie-banner rejection, and tracking parameter stripping.
2. **URL hostname blocklist** in `UrlUtils.isBlockedByAdBlock()`, checked by browser navigation before GeckoView loads a URL.

Blocking levels (`ad_blocking_level` pref): `"off"`, `"balanced"` default, and `"aggressive"`.

### Activity / Screen Map

- `MainActivity` - home screen with search, quick access, privacy counters, and restored-tab entry.
- `BrowserActivity` - core browser; single instance (`launchMode="singleTask"`); handles deep links via `onNewIntent`.
- `TabManagerActivity` - modern tab/group manager surface.
- `TabsActivity` - legacy tab switcher surface still present in the codebase.
- `GroupTabsActivity` - group editing and tab assignment flow.
- `InactiveTabsActivity` - inactive tab management.
- `SettingsActivity` and `SettingsSubpageActivity` - preferences and nested settings flows.
- `BookmarksActivity`, `HistoryActivity`, `DownloadsActivity`, `ReadingListActivity` - Room-backed list screens.
- `CookieManagerActivity`, `SitePermissionsActivity`, `UserStylesActivity` - privacy/customization tools.
- `ExtensionsActivity` - extension discovery/placeholder surface.

### URL Handling

`UrlUtils.getUrlOrSearchUrl()` is the single entry point for user text input. It calls `isSearchQuery()` to decide whether to build a configured search URL or treat input as a URL to sanitize. The new-tab page is generated in `UrlUtils.getNewTabPageUrl()`.

### SharedPreferences Keys

Most preference keys should be routed through `SettingsKeys` when available. Existing raw keys still appear in older code paths and should be normalized during focused refactors.

| Key | Default | Used in |
|---|---|---|
| `search_engine_url` | DuckDuckGo URL | `UrlUtils`, browser/settings flows |
| `homepage` | `https://duckduckgo.com` | `UrlUtils`, settings flows |
| `ad_blocking_level` | `"balanced"` | `RuntimeManager`, browser/settings flows |
| `javascript_enabled` | `true` | `RuntimeManager`, `TabManager`, browser/settings flows |
| `block_popups` | `true` | Browser navigation/prompt handling |
| `save_history` | `true` | `BrowserActivity` |
| `do_not_track` | `false` | `RuntimeManager`, settings flows |
| `block_cookie_banners` | `true` | `RuntimeManager`, settings flows |
| `strip_tracking_params` | `true` | `RuntimeManager`, settings flows |
| `show_privacy_stats` | `true` | `MainActivity`, settings flows |
| `show_quick_access` | `true` | `MainActivity`, settings flows |
| `privacy_pages_protected` | 0 | `PrivacyStatsManager`, home/browser flows |
| `privacy_items_blocked` | 0 | `PrivacyStatsManager`, home/browser flows |
| `saved_tabs` | legacy fallback | `TabManager` |
| `current_tab_id` | legacy fallback | `TabManager` |

### Firebase / Crashlytics

Optional. The Crashlytics and Google Services Gradle plugins are applied only when `app/google-services.json` is present. Local debug builds work without it.

### Release Signing

`app/build.gradle` contains placeholder signing config (`path/to/your/keystore.jks`). Update `signingConfigs.release` with real values before a release build. ProGuard/R8 is currently disabled (`minifyEnabled false`); enabling it requires adding GeckoView-specific keep rules.
