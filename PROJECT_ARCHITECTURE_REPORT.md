# Easy Browser Project Architecture Report

Generated for the current Easy Browser codebase.

## Summary

Easy Browser is a single-module Android application built around Mozilla GeckoView. The app uses Java, XML/AppCompat UI, Room persistence, repository classes, AndroidX ViewModels, and manager classes for browser runtime, tab state, privacy, thumbnails, analytics, and downloads.

There are no Jetpack Compose screens in the current codebase. UI is implemented with Android XML resources, ViewBinding/DataBinding, AppCompat activities, Material Components, RecyclerViews, adapters, dialogs, and bottom sheets.

## Module And Folder Structure

```text
EasyBrowser/
  app/
    build.gradle
    schemas/
    src/main/
      AndroidManifest.xml
      java/com/webstudio/easybrowser/
        EasyBrowserApplication.java
        adapters/
        database/
          AppDatabase.java
          dao/
          entity/
          relation/
        managers/
        models/
        repository/
        ui/activity/
        utils/
      res/
        drawable/
        layout/
        menu/
        mipmap/
        navigation/
        values/
        values-night/
        xml/
  build.gradle
  settings.gradle
  gradlew / gradlew.bat
```

## Technologies Used

- Android single-module Gradle project (`:app` only).
- Java 11 source and target compatibility.
- Android Gradle Plugin through version catalog aliases.
- GeckoView `143.0.20251003115653` for browser rendering.
- Room `2.6.1` for local database persistence.
- OkHttp `5.3.2` for app-managed downloads.
- Glide `5.0.7` for image loading.
- Gson `2.14.0` for JSON parsing.
- AndroidX AppCompat, Activity, ConstraintLayout, Preference, SwipeRefreshLayout, SplashScreen.
- Material Components.
- Firebase Analytics and Crashlytics dependencies are conditional on `app/google-services.json`.
- ViewBinding and DataBinding are enabled.
- No Kotlin source is currently compiled (`compileDebugKotlin NO-SOURCE` in previous checks).
- No Jetpack Compose dependencies or Compose UI files are present.

## Build Configuration

- Namespace: `com.webstudio.easybrowser`.
- Application ID: `com.webstudio.easybrowser`.
- `compileSdk`: 37.
- `minSdk`: 21.
- `targetSdk`: 37.
- Version: `versionCode 25`, `versionName "2.6.1"`.
- Debug build:
  - `applicationIdSuffix = ".debug"`.
  - `versionNameSuffix = "-debug"`.
- Release build:
  - `minifyEnabled = true`.
  - `shrinkResources = true`.
  - Uses `proguard-rules.pro`.
  - Uses `keystore.properties` only when present.
- Room schema export path: `app/schemas`.
- Android instrumentation tests include exported Room schemas as assets for migration validation.
- GeckoView repository: `https://maven.mozilla.org/maven2/`.
- JitPack is configured.

## Manifest And Entry Points

Main manifest activities:

- `MainActivity`: launcher activity, handles HTTP/HTTPS browser intents, text sharing, and app shortcuts.
- `BrowserActivity`: core browser activity, `launchMode="singleTask"`, supports PiP, handles browser session UI.
- `TabsActivity`: legacy tab switcher surface.
- `TabManagerActivity`: current tab/group management surface.
- `InactiveTabsActivity`: inactive tabs management.
- `GroupTabsActivity`: tab group creation/editing flow.
- `SettingsActivity` and `SettingsSubpageActivity`: settings.
- `BookmarksActivity`, `HistoryActivity`, `DownloadsActivity`, `ReadingListActivity`: list/data screens.
- `ExtensionsActivity`, `CookieManagerActivity`, `SitePermissionsActivity`, `UserStylesActivity`: browser tools and management surfaces.

Permissions include internet/network, location, storage up to API 28, camera, audio recording, and notifications.

## Room Database

Database class: `database/AppDatabase.java`.

Database name: `browser.db`.

Database version: 7.

Entities:

| Entity | Purpose |
|---|---|
| `BookmarkEntity` | Saved bookmarks |
| `HistoryEntity` | Browsing history |
| `QuickAccessEntity` | Quick access / frequently visited entries |
| `DownloadEntity` | Download metadata and state |
| `ReadingListEntity` | Saved reading-list items |
| `TabGroupEntity` | Persisted non-private tab groups |
| `TabEntity` | Persisted non-private tabs |

DAOs:

- `BookmarkDao`
- `HistoryDao`
- `QuickAccessDao`
- `DownloadDao`
- `ReadingListDao`
- `TabGroupDao`

Relations:

- `TabGroupWithTabs` maps `TabGroupEntity` rows with related `TabEntity` rows.

Migrations:

- `1->2`: creates `downloads`.
- `2->3`: creates `reading_list`.
- `3->4`: creates `tab_groups` and `tabs`.
- `4->5`: adds private/group metadata columns and favicon path handling.
- `5->6`: rebuilds tabs with nullable group references and cleanup rules.
- `6->7`: adds `pinned` to `tabs`.

The database uses `fallbackToDestructiveMigrationOnDowngrade()` to avoid downgrade crashes.

Room tab/group migration coverage now validates the version 3-to-7 tab schema, the version 7 `pinned` default, and group delete behavior that sets child tab `groupId` values to null.

## Repositories

Repository classes:

- `BookmarkRepository`: bookmark CRUD through `BookmarkDao`.
- `HistoryRepository`: history CRUD/query operations.
- `QuickAccessRepository`: quick access records.
- `DownloadRepository`: download records.
- `ReadingListRepository`: reading-list save/load/delete.
- `TabRepository`: tab and tab-group persistence, movement, counts, group cleanup, blocking reads for restore flows.

Threading:

- Most repositories use `AppDatabase.getDatabaseExecutor()`.
- `ReadingListRepository` still creates its own `newSingleThreadExecutor()`, which is current technical debt.
- Repository APIs are callback-based rather than LiveData, Flow, or Coroutines.

## ViewModels

ViewModels:

- `BrowserViewModel`: holds `TabManager` across configuration changes.
- `TabGroupsViewModel`: loads, filters, and saves persisted tab groups through `TabRepository`.
- `GroupTabsViewModel`: supports group tab selection and grouping workflows.

The app currently uses AndroidX `AndroidViewModel` classes, not Compose state holders.

## Compose Screen Status

There are no Compose screens.

Current UI surfaces are Java activities plus XML layouts and adapters. Compose is a roadmap item for Phase 2, not implemented.

## GeckoView Integration

Key classes:

- `RuntimeManager`: owns singleton `GeckoRuntime`, runtime settings, content blocking, privacy features, and GeckoView runtime initialization.
- `Tab`: owns one `GeckoSession` plus browser metadata.
- `TabManager`: creates Gecko sessions, tracks current tab, coordinates state and persistence.
- `BrowserActivity`: attaches the current session to the single visible `GeckoView`.
- Browser delegate classes:
  - `BrowserNavigationDelegate`
  - `BrowserContentDelegate`
  - `BrowserContentBlockingDelegate`
  - `BrowserProgressDelegate`
  - `BrowserPermissionDelegate`
  - `BrowserPromptDelegate`
  - `BrowserHistoryDelegate`

GeckoView renders one attached `GeckoSession` at a time. Switching tabs changes the session attached to the visible `GeckoView`.

## Tab Management System

Runtime state:

- `BrowserStateStore` is the runtime source of truth for regular tabs, private tabs, groups, active regular/private tab ids, private mode, ordering, pinning, and group membership rules.
- `TabManager` coordinates public tab operations, Gecko session creation, recently closed tabs, back stack, persistence, and listener callbacks; its `tabs` and `currentTab` fields are derived mirrors synced from `BrowserStateStore`.

Persistence:

- `TabRepository` persists non-private tabs/groups into Room as the normal tab persistence source.
- Private tabs are runtime-only.
- `TabManager` restores Room tab state first.
- Legacy `saved_tabs` SharedPreferences restore still exists only as a migration fallback and is removed after a successful Room save.
- `TabManager.MAX_PERSIST_BYTES` caps legacy SharedPreferences tab payload size.
- `TabRepository.saveTabsBlocking()` writes a full regular-tab Room snapshot, deletes stale regular tabs, skips private tabs, and cleans up invalid one-tab groups.

Tab features:

- Regular tabs.
- Private tabs.
- Tab groups.
- Pinned tabs.
- Inactive tabs.
- Recently closed tabs.
- Group rename/color changes.
- Move tabs between groups.
- Reorder support.
- Thumbnail cache/manager support.

Technical risk:

- `TabManager` still has broad Gecko session, persistence, recently-closed, and undo/back-stack responsibilities.
- Result contracts between tab-management screens and browser activity still need simplification.

## Navigation Flow

Home/start:

```text
MainActivity
  -> BrowserActivity for search/url/deep link/text share
```

Browser:

```text
BrowserActivity
  -> TabManager / BrowserViewModel
  -> GeckoSession attached to GeckoView
```

Tab management:

```text
BrowserActivity
  -> TabManagerActivity
      -> GroupTabsActivity
      -> InactiveTabsActivity
```

Data screens:

```text
BrowserActivity or MainActivity
  -> BookmarksActivity
  -> HistoryActivity
  -> DownloadsActivity
  -> ReadingListActivity
```

Settings/tools:

```text
MainActivity or BrowserActivity
  -> SettingsActivity
      -> SettingsSubpageActivity
      -> SitePermissionsActivity
  -> CookieManagerActivity
  -> UserStylesActivity
  -> ExtensionsActivity
```

## Utilities And Managers

Managers:

- `RuntimeManager`: Gecko runtime and runtime settings.
- `TabManager`: tab/session coordinator.
- `BrowserStateStore`: runtime browser state store.
- `AppDownloadManager`: OkHttp download execution, pause/resume/cancel, MediaStore publication, notifications.
- `PrivacyStatsManager`: privacy counters.
- `TabThumbnailManager` and `TabThumbnailCache`: tab thumbnails.
- `AnalyticsManager`: analytics wrapper.

Utilities:

- `UrlUtils`: URL/search parsing, new-tab page, ad-block URL checks.
- `SettingsKeys`: preference key constants.
- `SearchSuggestionProvider`: suggestions.
- `ScreenshotProtection`: screenshot/privacy handling.
- `HomeBackgroundProvider`: home background selection.
- `SystemBarUtils`: system bar styling.

## Known Technical Debt

- `BrowserActivity` remains large and owns many workflows.
- `TabManager` mixes session, state, persistence, grouping, undo, and listener responsibilities.
- Legacy `saved_tabs` fallback is still present as migration-only code and can be removed after release soak.
- Tab manager result contracts use complex intent extras and should be consolidated.
- Repository APIs are callback-heavy.
- `ReadingListRepository` still owns a standalone executor.
- SharedPreferences keys are not fully centralized.
- UI is XML-only while the roadmap calls for Compose/Material 3 modernization.
- Compose migration strategy is undecided.
- Corrupt or partial persisted-tab restore tests still need to be added.
- Crash/ANR/startup/memory baselines need production telemetry or profiling confirmation.
