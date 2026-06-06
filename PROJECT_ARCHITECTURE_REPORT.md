# Easy Browser Project Architecture Report

Generated from the current repository state.

## Executive Summary

Easy Browser is a single-module Android application written in Java. It uses AppCompat, Material Components, XML layouts, RecyclerView adapters, AndroidX ViewModel, Room, OkHttp, Glide, and Mozilla GeckoView. The browser core is built around one singleton `GeckoRuntime`, one `GeckoSession` per tab, a runtime `TabManager`, and Room-backed persistence for normal tabs and tab groups.

There is no Jetpack Compose usage in the current codebase. All screens are AppCompat activities with XML layouts and imperative Java controllers.

## Folder Structure

```text
.
+-- app/
|   +-- build.gradle
|   +-- schemas/com.webstudio.easybrowser.database.AppDatabase/
|   |   +-- 3.json
|   |   +-- 4.json
|   |   +-- 5.json
|   |   +-- 6.json
|   |   +-- 7.json
|   +-- src/
|       +-- main/
|       |   +-- AndroidManifest.xml
|       |   +-- java/com/webstudio/easybrowser/
|       |   |   +-- EasyBrowserApplication.java
|       |   |   +-- adapters/
|       |   |   +-- database/
|       |   |   |   +-- AppDatabase.java
|       |   |   |   +-- dao/
|       |   |   |   +-- entity/
|       |   |   |   +-- relation/
|       |   |   +-- managers/
|       |   |   +-- models/
|       |   |   +-- repository/
|       |   |   +-- ui/activity/
|       |   |   +-- utils/
|       |   +-- res/
|       |       +-- anim/
|       |       +-- color/
|       |       +-- drawable/
|       |       +-- drawable-night/
|       |       +-- layout/
|       |       +-- menu/
|       |       +-- mipmap-*/
|       |       +-- values/
|       |       +-- values-night/
|       |       +-- xml/
|       +-- test/java/com/webstudio/easybrowser/
+-- gradle/libs.versions.toml
+-- build.gradle
+-- settings.gradle
+-- README.md
+-- AGENTS.md
```

## Technologies Used

| Area | Technology |
|---|---|
| Language | Java 11 |
| UI | AppCompat, Material Components, XML layouts, ViewBinding/DataBinding |
| Browser engine | Mozilla GeckoView `143.0.20251003115653` |
| Persistence | Room `2.6.1`, SharedPreferences |
| Networking | OkHttp `5.3.2` |
| Image loading | Glide `5.0.7` |
| Architecture support | AndroidX `ViewModel` / `AndroidViewModel` |
| Testing | JUnit 4, AndroidX Test, Espresso |
| Optional services | Firebase Analytics and Crashlytics, applied only when `app/google-services.json` is present |

## Build Configuration

- Project shape: single Gradle module, `:app`.
- Android Gradle Plugin: `9.2.1`.
- Namespace/application ID: `com.webstudio.easybrowser`.
- `compileSdk`: 37.
- `minSdk`: 21.
- `targetSdk`: 37.
- Version: `versionCode 25`, `versionName "2.6.1"`.
- Debug build:
  - `applicationIdSuffix = ".debug"`.
  - `versionNameSuffix = "-debug"`.
  - `BuildConfig.DEBUG = true`.
- Release build:
  - R8 minification and resource shrinking enabled.
  - Reads signing config from root `keystore.properties` when present.
- Build features:
  - `viewBinding = true`.
  - `dataBinding = true`.
  - `buildConfig = true`.
- Room schema export:
  - `room.schemaLocation = "$projectDir/schemas"`.
  - `room.incremental = true`.
- Repositories:
  - Google Maven.
  - Maven Central.
  - JitPack.
  - Mozilla Maven for GeckoView.

## Android Manifest and App Entry Points

`EasyBrowserApplication` applies the saved theme and reapplies screenshot protection during activity creation/resume.

Declared runtime-facing activities:

- `MainActivity`: launcher, HTTP/HTTPS deep links, shared text entry point, shortcuts metadata.
- `BrowserActivity`: main browser surface, `singleTask`, GeckoView host, Picture-in-Picture capable.
- `TabsActivity`: older tab-management activity still declared.
- `TabManagerActivity`: current tab manager overview.
- `InactiveTabsActivity`: inactive/duplicate tab management.
- `GroupTabsActivity`: group detail overlay.
- `SettingsActivity` and `SettingsSubpageActivity`.
- `BookmarksActivity`, `DownloadsActivity`, `HistoryActivity`, `ExtensionsActivity`.
- `CookieManagerActivity`, `SitePermissionsActivity`.
- `ReadingListActivity`, `UserStylesActivity`.

Important manifest-level configuration:

- `allowBackup="false"`.
- data extraction and backup XML resources are present.
- `largeHeap="true"`.
- `networkSecurityConfig="@xml/network_security_config"`.
- `FileProvider` authority: `${applicationId}.fileprovider`.

## UI Architecture

The app uses Java activities and XML layouts. There is no Compose screen layer.

Primary activity roles:

- `MainActivity`
  - Home screen.
  - Photo-backed start page, privacy stats, quick access, search overlay, bottom navigation.
  - Opens `BrowserActivity` for navigation.
- `BrowserActivity`
  - Core browser UI.
  - Owns visible `GeckoView`, URL bar, progress/security controls, bookmark controls, bottom navigation, quick tab strip, more menu, find-in-page, downloads, page sharing, PDF saving, per-site zoom, reader/translation entry points, and tab-manager launches.
- `TabManagerActivity`
  - Main tab overview.
  - Restores runtime tab data from intent extras.
  - Combines runtime tabs with persisted groups via `TabGroupsViewModel`.
  - Supports grouping, moving, pinning, closing, merging, inactive tab entry, and result propagation back to `BrowserActivity`.
- `GroupTabsActivity`
  - Group detail overlay for one tab group.
  - Supports drag/drop, selection, rename, recolor, close/delete group, share/bookmark selected tabs, and tab movement.
- `InactiveTabsActivity`
  - Filters stale/duplicate tabs based on preferences and returns close/restore results.
- `SettingsActivity` and `SettingsSubpageActivity`
  - Preference-driven configuration UI.
  - Writes to default SharedPreferences and updates runtime settings where needed.
- Data-list activities
  - Bookmarks, downloads, history, reading list, user styles, cookies, site permissions, and extensions are activity-backed list/detail screens.

Adapters:

- `BookmarksAdapter`, `DownloadsAdapter`, `HistoryAdapter`, `QuickAccessAdapter`, `ReadingListAdapter`, `UserStylesAdapter`.
- `TabsAdapter`, `TabGroupAdapter`, `GroupTabsAdapter`, `QuickTabStripAdapter`.
- `SuggestionsAdapter`.
- `TabItemTouchHelperCallback` handles reorder/drag behavior.

## Compose Screens

No Compose dependencies, `@Composable` declarations, or `setContent {}` usage were found. The current architecture is XML + AppCompat + RecyclerView. If Compose is introduced later, it should be treated as a new UI layer rather than an existing one.

## Room Database

Database class: `AppDatabase`.

- Database name: `browser.db`.
- Version: `7`.
- Executor: shared fixed thread pool of 2 via `AppDatabase.getDatabaseExecutor()`.
- Migrations:
  - `1 -> 2`: downloads table.
  - `2 -> 3`: reading list table.
  - `3 -> 4`: tab groups and tabs.
  - `4 -> 5`: private/update metadata, favicon path, tab privacy.
  - `5 -> 6`: nullable group IDs, `ON DELETE SET NULL`, cleanup of default/private default groups.
  - `6 -> 7`: pinned tabs.
- Downgrade behavior: `fallbackToDestructiveMigrationOnDowngrade()`.

### Entities

| Entity | Table | Main Fields |
|---|---|---|
| `BookmarkEntity` | `bookmarks` | `id`, `title`, `url`, `favicon`, `createdAt`, `folder` |
| `HistoryEntity` | `history` | `id`, `title`, `url`, `favicon`, `visitTime`, `visitCount` |
| `QuickAccessEntity` | `quick_access` | `id`, `title`, `url`, `faviconUrl`, `visitCount`, `lastVisited` |
| `DownloadEntity` | `downloads` | `id`, `url`, `fileName`, `mimeType`, `destinationPath`, byte counters, status, error, timing/speed fields |
| `ReadingListEntity` | `reading_list` | `id`, `title`, `url`, `favicon`, `savedAt`, `contentPath` |
| `TabGroupEntity` | `tab_groups` | `groupId`, `groupName`, `groupColor`, `isPrivate`, `createdAt`, `updatedAt` |
| `TabEntity` | `tabs` | `tabId`, nullable `groupId`, `title`, `url`, `favicon`, `faviconPath`, `thumbnailPath`, `sessionState`, `isPrivate`, `lastAccessed`, `position`, `pinned` |

`TabEntity` has a nullable foreign key to `TabGroupEntity.groupId` with `ON DELETE SET NULL`, plus indexes on `groupId`, `lastAccessed`, and `(groupId, position)`.

### DAO Layer

- `BookmarkDao`: list all, list by folder, folder names, find by URL, insert/update/delete/delete all.
- `HistoryDao`: list all, find by URL, range queries, insert/update/delete/delete all, delete by age/range.
- `QuickAccessDao`: list all, most visited, find by URL, insert/update/delete/delete all.
- `DownloadDao`: list all, list by status, find by ID, insert/update/delete, clear completed/failed, clear all.
- `ReadingListDao`: list all, find by ID, insert, delete, clear all.
- `TabGroupDao`: group-with-tabs transactions, standalone tabs, tab counts, group/tab CRUD, movement, thumbnail/favicon updates, ordering, private-state cleanup.

### Relations

- `TabGroupWithTabs` embeds `TabGroupEntity` and relates it to all `TabEntity` rows with the same `groupId`.

## Repositories

Repositories convert Room entities to model classes and expose callback-style async APIs.

- `BookmarkRepository`
  - Uses shared database executor.
  - Loads bookmarks/folders, adds, removes, updates.
- `HistoryRepository`
  - Uses shared database executor.
  - Adds visits with URL de-duplication and visit-count increments.
  - Clears all or time-windowed history.
- `QuickAccessRepository`
  - Uses shared database executor.
  - Builds most-visited quick-access items.
  - Skips internal pages and search-result pages.
  - Normalizes URLs through `UrlUtils`.
- `DownloadRepository`
  - Uses shared database executor.
  - Persists download status and metadata.
- `ReadingListRepository`
  - Persists saved reading-list pages.
  - Uses its own `Executors.newSingleThreadExecutor()`.
- `TabRepository`
  - Uses shared database executor.
  - Owns persisted tab/group CRUD, grouping, ungrouping, movement, pinning metadata, thumbnails, ordering, and cleanup of groups smaller than two tabs.
  - Provides async callback APIs and blocking helper APIs used by `TabManager`.

## ViewModels

- `BrowserViewModel`
  - Extends `AndroidViewModel`.
  - Creates the singleton `GeckoRuntime` via `RuntimeManager`.
  - Owns one `TabManager`.
  - Releases sessions in `onCleared()`.
- `TabGroupsViewModel`
  - Wraps `TabRepository`.
  - Loads overview data: groups plus standalone tabs.
  - Filters groups/tabs by search query.
  - Exposes tab/group mutation operations to `TabManagerActivity`.
- `GroupTabsViewModel`
  - Wraps `TabRepository`.
  - Loads tabs for one group.
  - Exposes group rename/color/delete, tab delete/move/ungroup, group creation, and ordering for `GroupTabsActivity`.

No repository currently exposes LiveData, Flow, or lifecycle-aware streams; callbacks are manually posted to the main thread in ViewModels/activities.

## GeckoView Integration

### Runtime

`RuntimeManager` owns a process-wide singleton `GeckoRuntime`.

Runtime settings come from SharedPreferences:

- JavaScript enabled.
- Remote debugging.
- Global Privacy Control / Do Not Track.
- Font size factor and force user scalable.
- Translation popup preferences.
- Preferred locales.
- Preferred color scheme.
- Local network access blocking.
- DNS-over-HTTPS mode/provider/URI.
- Gecko content blocking settings.

Content blocking includes:

- Anti-tracking mode based on `off`, `balanced`, or `aggressive`.
- Enhanced Tracking Protection level.
- Safe Browsing defaults.
- Cookie banner rejection.
- Query parameter stripping.

### Sessions

`TabManager.createSession()` builds one `GeckoSession` per tab:

- Private tabs use `GeckoSessionSettings.usePrivateMode(true)`.
- JavaScript follows preferences.
- Media suspension follows background-play preference.
- User-agent and viewport mode follow mobile/desktop/custom presets.
- iPhone/iPad/custom user-agent overrides are supported.

`BrowserActivity.configureSession()` wires each visible session:

- `BrowserNavigationDelegate`.
- `BrowserContentDelegate`.
- `BrowserContentBlockingDelegate`.
- `BrowserProgressDelegate`.
- `GeckoSession.ScrollDelegate`.
- `BrowserPromptDelegate`.
- `BrowserPermissionDelegate`.
- `BrowserHistoryDelegate`.

### Delegates

- `BrowserNavigationDelegate`
  - Tracks URL changes, bookmark status, per-site zoom, and user-style injection.
  - Enforces HTTPS-only upgrades and blocks top-level `data:`/`javascript:` navigation when HTTPS-only mode is on.
  - Blocks URLs via `UrlUtils.isBlockedByAdBlock`.
  - Handles popup blocking and new-window-to-new-tab behavior.
  - Sends non-browser schemes to external apps through a chooser.
  - Builds Gecko error-page HTML for load failures.
- `BrowserContentDelegate`
  - Handles title changes, context menus, external responses/downloads, and fullscreen/Picture-in-Picture.
- `BrowserProgressDelegate`
  - Updates progress UI, security icon, and session-state persistence.
- `BrowserPermissionDelegate`
  - Bridges Android permissions, content permissions, media permissions, site-permission preferences, and permission prompts.
- `BrowserPromptDelegate`
  - Handles file picker prompts.
- `BrowserHistoryDelegate`
  - Stores the last Gecko history list for tab-history UI.
- `BrowserContentBlockingDelegate`
  - Records privacy stats when Gecko blocks content.

## Tab Management System

### Runtime Model

`Tab` is the runtime model for each browser tab:

- ID, title, URL.
- `GeckoSession`.
- favicon bitmap/URI.
- private flag.
- group ID/name/color.
- thumbnail path.
- created/last accessed timestamps.
- position and pinned state.
- back/forward state.
- session-state string.
- parent tab ID.

`TabGroup` represents UI/database groups:

- group ID/name/color.
- private flag.
- created/updated timestamps.
- list of tabs.

`BrowserState` is an immutable snapshot of regular tabs, private tabs, groups, active tab IDs, active group ID, and private-mode state.

### State Store

`BrowserStateStore` is an in-memory synchronized store:

- Maintains regular tabs, private tabs, and groups in `LinkedHashMap`s.
- Tracks active regular/private tab ID and current private-mode flag.
- Creates groups only when at least two tabs are selected.
- Normalizes invalid group membership.
- Removes groups that fall below two tabs.
- Supports reorder, move, ungroup, close group, close all, and snapshots.

### Manager

`TabManager` is the bridge between Gecko sessions, in-memory browser state, and persistence.

Responsibilities:

- Creates default/new/private tabs.
- Creates and opens `GeckoSession`s.
- Switches active tabs and maintains a tab back stack.
- Persists regular tabs.
- Restores Room tab state first, then legacy SharedPreferences JSON.
- Clears persisted private tab state.
- Saves Gecko session-state strings.
- Maintains up to 10 recently closed non-private tabs for undo.
- Handles close, restore, pinning, grouping, ungrouping, metadata updates, thumbnails, and tab counts.

Persistence behavior:

- Normal tab/group state goes to Room through `TabRepository`.
- Legacy `saved_tabs` JSON remains as a fallback and is capped at 256 KB.
- Private tabs are intentionally kept out of durable persistence.

### Tab Manager UI Flow

`BrowserActivity` launches `TabManagerActivity` with parallel arrays of runtime tab metadata. `TabManagerActivity` reconstructs runtime tab models, merges in repository-backed group data, and returns mutations through result extras:

- selected tab ID.
- closed tab IDs.
- private tab creation requests.
- restore URL/private flag.
- group mutation tab IDs/group IDs/names/colors.
- created private group metadata.
- reordered private tab IDs.
- pinned/unpinned tab IDs.
- restored inactive tab IDs.

`GroupTabsActivity` performs group-specific operations and returns a similar mutation set upward.

## Navigation Flow

High-level user flow:

```text
Launcher / http(s) deep link / shared text
  -> MainActivity
       -> BrowserActivity
            -> GeckoView + active GeckoSession
            -> TabManagerActivity
                 -> GroupTabsActivity
                 -> InactiveTabsActivity
            -> BookmarksActivity
            -> HistoryActivity
            -> DownloadsActivity
            -> ReadingListActivity
            -> ExtensionsActivity
            -> SettingsActivity
                 -> SettingsSubpageActivity
                      -> SitePermissionsActivity
                      -> CookieManagerActivity
```

Important navigation mechanics:

- `MainActivity` handles launch/deep links/shared text and opens `BrowserActivity`.
- `BrowserActivity` is `singleTask` and handles new intents.
- `BrowserActivity.loadUrl()` validates/transforms input through `UrlUtils`.
- Browser tabs are not separate activities; they are Gecko sessions swapped into one `GeckoView`.
- Tab management screens communicate by activity result extras, not a navigation component.
- External links and non-browser schemes are routed through Android intents.

## Downloads

`AppDownloadManager` is a singleton service-like manager, not an Android Service.

Key behavior:

- OkHttp download client with manual redirect handling.
- Blocks HTTPS-to-HTTP downgrade redirects.
- Stores partial downloads in app cache first.
- Publishes completed files to MediaStore on API 29+ or public Downloads on legacy devices.
- Supports pause, cancel, Range resume, progress notifications, completion notifications, Wi-Fi-only queueing, and optional auto-open.
- Sanitizes filenames from URL/content-disposition, including path traversal and bidi-control characters.
- Uses a three-thread executor and per-download active call tracking.

## Settings and Preferences

Most product configuration is stored in default SharedPreferences. Examples:

- Search engine and homepage.
- Privacy/ad-blocking level.
- HTTPS-only mode.
- JavaScript.
- User-agent preset.
- Theme mode.
- Screenshot protection.
- Site permission defaults.
- DNS-over-HTTPS.
- Translation settings.
- Download preferences.
- Tab inactivity threshold and duplicate tab behavior.

`SettingsKeys` centralizes many newer preference keys, but older literal keys are still spread across activities, managers, and utilities.

## Security and Privacy Architecture

- Gecko content blocking and app-level URL blocking run together.
- HTTPS-only logic upgrades HTTP top-level navigation where possible.
- External schemes go through a chooser instead of silent dispatch.
- Runtime permission prompts integrate site-level allow/deny storage.
- Screenshot protection applies through `EasyBrowserApplication`.
- FileProvider is used for sharing/opening app-owned files.
- Downloads sanitize filenames and block unsafe redirect behavior.
- Private tabs are not persisted and persisted private rows are cleared during tab restoration.

## Dependencies

Direct app dependencies:

- `androidx.appcompat:appcompat:1.7.0`
- `com.google.android.material:material:1.12.0`
- `androidx.activity:activity:1.10.0`
- `androidx.constraintlayout:constraintlayout:2.2.0`
- `androidx.core:core-splashscreen:1.2.0`
- `androidx.swiperefreshlayout:swiperefreshlayout:1.2.0`
- `androidx.preference:preference:1.2.1`
- `org.mozilla.geckoview:geckoview:143.0.20251003115653`
- `com.github.bumptech.glide:glide:5.0.7`
- `com.google.code.gson:gson:2.14.0`
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-compiler:2.6.1`
- `com.squareup.okhttp3:okhttp:5.3.2`
- `com.google.firebase:firebase-analytics:22.2.0`
- `com.google.firebase:firebase-crashlytics:19.4.3` when configured

Test dependencies:

- `junit:junit:4.13.2`
- `androidx.test.ext:junit:1.2.1`
- `androidx.test.espresso:espresso-core:3.6.1`

## Tests Present

Local JVM tests exist for:

- `UrlUtilsTest`.
- `BrowserStateStoreTest`.
- `ExampleUnitTest`.

No broad instrumented test coverage was found for GeckoView flows, Room migrations, tab manager UI result contracts, or download behavior.

## Known Technical Debt

1. Large controller classes

   `BrowserActivity`, `TabManagerActivity`, `GroupTabsActivity`, `SettingsSubpageActivity`, `TabManager`, and `AppDownloadManager` are large and carry multiple responsibilities. This makes targeted regression testing and future refactoring harder.

2. No Compose layer despite modern UI expectations

   The app is XML/AppCompat only. That is not inherently wrong, but any future Compose migration will require an explicit interop strategy and should avoid mixing state ownership between activities and composables.

3. Manual activity-result contracts

   Tab-management screens pass many parallel arrays and result extras. This is brittle because all arrays must stay aligned by index and every added field expands the contract.

4. Mixed tab persistence

   Normal tab state is now persisted in Room, but legacy `saved_tabs` SharedPreferences JSON still exists as a fallback. This is useful for migration, but it duplicates state concepts and increases restore-path complexity.

5. Blocking repository APIs

   `TabRepository` exposes blocking helpers implemented through `FutureTask` on the database executor. These hide exceptions by returning fallbacks and can block callers if used from sensitive threads.

6. Callback-based repositories

   Repositories use custom callbacks and manual `runOnUiThread`/`Handler` posting rather than lifecycle-aware streams. This increases boilerplate and makes cancellation/lifecycle handling manual.

7. Executor inconsistency

   Most repositories use `AppDatabase.getDatabaseExecutor()`, but `ReadingListRepository` creates its own `newSingleThreadExecutor()` without an explicit shutdown path.

8. Stringly typed preferences

   `SettingsKeys` exists, but many SharedPreferences keys are still literal strings across activities, managers, delegates, and utilities. This increases the chance of typo-driven bugs.

9. Incomplete automated coverage

   Existing tests cover selected utility/state behavior. High-risk areas such as Room migrations, tab/group result contracts, GeckoView delegate behavior, permission flows, and downloads have little or no automated coverage.

10. Documentation drift risk

   `README.md` is current, but older internal documentation such as `CLAUDE.md` still references older database/tab persistence details. Architecture docs should be kept in sync with Room v7 and current tab-group behavior.

## Practical Improvement Roadmap

1. Add Room migration tests for versions 3 through 7.
2. Replace tab-manager parallel-array extras with a typed `Parcelable` contract.
3. Move BrowserActivity feature clusters into smaller coordinators for search, toolbar/menu, downloads, tab strip, and page actions.
4. Standardize repositories on the shared database executor or lifecycle-aware streams.
5. Centralize all SharedPreferences keys and typed accessors in one settings facade.
6. Add integration tests around `BrowserStateStore`, `TabRepository`, and tab/group mutation results.
7. Decide explicitly whether Compose is in scope; if yes, introduce it first in isolated list/detail screens before touching the GeckoView host.
