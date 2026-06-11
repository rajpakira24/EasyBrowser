# Easy Browser Master Plan Checklist (2026-2028)

Derived from `EasyBrowser_MASTER_PLAN_2026_2028.md`.

Rule: after any roadmap task is implemented or completed, update this checklist in the same local planning pass. Do not mark code tasks complete for planning-only edits.

## Completed Planning Work

- [x] Recreated the local master plan file after the revert removed it from git.
- [x] Rewrote `PHASE 1 - FOUNDATION REBUILD` into an implementation-ready plan.
- [x] Recreated `EasyBrowser_MASTER_PLAN_CHECKLIST.md`.
- [x] Recreated `PROJECT_ARCHITECTURE_REPORT.md`.

## Completed Implementation Work

- [x] Fixed Pixel 9 Pro XL home search bar overlap by spacing it from the measured bottom navigation height.
- [x] Polished Pixel tab manager grid behavior so a single tab spans the available width and the initial view-toggle icon matches the next available action.
- [x] Moved core tab/group runtime state rules into `BrowserStateStore`, including active tab fallback, grouping, reorder, pinning, and persistable regular-tab normalization.
- [x] Added `BrowserStateStore` regression coverage for tab creation, private mode, switching, close, close all, group mutations, reorder, pinning, and persistable tabs.
- [x] Added JVM restore-state coverage for standalone, grouped, pinned, and inactive regular tabs after persisted state is loaded into `BrowserStateStore`.
- [x] Made Room `tabs` and `tab_groups` the primary persistence source for regular tabs, with legacy `saved_tabs` isolated as a migration-only fallback.
- [x] Added Room migration and repository instrumentation coverage for tab/group schema, `pinned`, group foreign-key cleanup, regular snapshot replacement, group cleanup, and private-state deletion.
- [x] Hardened application startup so locked-device instrumentation and direct-boot startup do not touch credential-encrypted preferences before unlock.
- [x] Added v2.7 locked-tab support with persistence, tab-manager lock/unlock actions, grouped-tab propagation, and close protection.
- [x] Added v2.7 user-wallpaper crop flow with an internal pan/zoom crop screen and private cropped image storage.
- [x] Expanded the v2.7 Site Info panel with cookie-banner status, saved host permissions, permission defaults, and existing per-host clear actions.
- [x] Added v2.7 in-browser security alerts for warning-level Smart Shield states with a Site Info action.
- [x] Added v2.7 notification channels for browser, downloads, privacy, weather, rewards, AI, and updates with Settings toggles and app startup registration.
- [x] Added v2.7 tracker/privacy reports with total stats plus today, week, and month protected-page and blocked-item summaries.
- [x] Added v2.7 multiple saved user wallpapers with crop output storage, preview tiles, saved URI selection, and active wallpaper mode switching.
- [x] Added a typed serialized tab action contract for tab manager results, wired Browser/Home consumers to prefer it, and covered serialization/parsing with JVM tests.
- [x] Added v2.7 realtime weather with device-location permission flow, manual fallback, Open-Meteo customer endpoint/API-key hooks, and provider attribution.
- [x] Updated the legacy tabs surface to emit typed tab action results while keeping legacy extras as compatibility fallback.
- [x] Added an explicit Settings action to request missing declared app runtime permissions while preserving contextual permission prompts.
- [x] Moved notification permission to first app open, kept weather location permission on Weather screen entry, and rejected coarse, stale, low-accuracy, or far reverse-geocoded weather locations.
- [x] Standardized `ReadingListRepository` on the shared Room database executor instead of a standalone repository executor.
- [x] Centralized repeated homepage, search engine, custom search engine, user-agent, and home-card SharedPreferences keys in `SettingsKeys`.
- [x] Added `AppSettings` as a typed facade for wallpaper and tab-overview preferences.
- [x] Added v2.7 wallpaper favorites, favorites-only rotation, and offline wallpaper-pack caching controls.
- [x] Added persisted tab-group collapse/expand behavior with overview transition polish.

## Pre-Work

- [x] Decide whether these planning files stay local-only or should be committed later.
- [x] Confirm current app version and local build/test health.
- [x] Record production crash/ANR verification status.
- [x] Capture startup time baseline.
- [x] Capture memory usage baseline.
- [x] Define owner, priority, status, and target date fields if this moves to GitHub Issues or a project board.

Pre-Work Results:

| Item | Result |
|---|---|
| Planning file policy | Local-only unless explicitly requested with "commit and push" |
| App version | `versionName 2.6.1`, `versionCode 25` |
| SDK baseline | `minSdk 21`, `targetSdk 37`, `compileSdk 37` |
| Debug package | `com.webstudio.easybrowser.debug` |
| Local compile | Passed: `.\gradlew.bat compileDebugJavaWithJavac` |
| Local unit tests | Passed: `.\gradlew.bat testDebugUnitTest` |
| Production crash/ANR | Not verifiable from this workspace; use Play Console, Crashlytics dashboard, or another production telemetry source before release decisions |
| Crashlytics config | `app/google-services.json` exists and Gradle conditionally enables Crashlytics when config is valid |
| Startup baseline | Cold start on `Pixel_9_Pro_XL_API_Baklava`: `TotalTime 1620 ms`, `WaitTime 1624 ms` |
| Memory baseline | After startup idle: `TOTAL PSS 94185 KB`, `TOTAL RSS 207212 KB`, `TOTAL SWAP PSS 268 KB` |
| Heap detail | Java heap PSS `17556 KB`, native heap PSS `28168 KB` |
| UI object count | `Activities 1`, `Views 75`, `WebViews 0` |
| Emulator note | `Pixel_9_Pro_XL_API_34` did not have enough free `/data` space for the debug APK; baseline used `Pixel_9_Pro_XL_API_Baklava` |

Tracker Fields:

| Field | Values |
|---|---|
| Owner | `Riju`, `Codex`, or named contributor |
| Phase | `Pre-Work`, `Phase 1`, `Phase 2`, `Phase 3`, `Phase 4`, `Phase 5` |
| Workstream | `State`, `Persistence`, `Contracts`, `Controller`, `Repository`, `Settings`, `UI`, `AI`, `Metrics` |
| Priority | `P0`, `P1`, `P2`, `P3` |
| Status | `Not Started`, `In Progress`, `Blocked`, `Done`, `Deferred` |
| Target Date | ISO date, for example `2026-03-31` |
| Verification | Command, test name, screenshot, telemetry source, or manual QA note |

## Phase 1: Foundation Rebuild (Q1 2026)

Goal: create a stable, testable browser-state foundation before UI, AI, and agentic features are added.

### Browser State Ownership

- [x] Make `BrowserStateStore` the runtime source of truth for regular tabs, private tabs, active tab ids, groups, and private mode.
- [x] Move pure tab/group state mutation rules out of `TabManager` into smaller testable components.
- [x] Keep `TabManager` as the public coordinator for Gecko sessions, persistence, and listeners.
- [x] Add regression tests for regular tab creation, private tab creation, tab switching, close, close all, and active-tab fallback.
- [x] Add regression tests for group create, rename, color change, ungroup, close group, and reorder.

### Room Tab Persistence

- [x] Treat Room `tabs` and `tab_groups` as the normal persistent source for non-private tabs.
- [x] Add migration tests for tab/group schema versions.
- [x] Test group delete behavior and foreign-key cleanup.
- [x] Test the version 7 `pinned` column migration.
- [x] Add restore tests for standalone tabs.
- [x] Add restore tests for grouped tabs.
- [x] Add restore tests for pinned tabs.
- [x] Add restore tests for inactive tabs.
- [x] Add restore tests for corrupt or partial persisted state.
- [x] Keep private tabs runtime-only.
- [x] Prove no private tabs/groups are saved or restored.
- [x] Remove or isolate the legacy `saved_tabs` fallback after migration safety is proven.

### Tab Manager Contracts

- [x] Replace parallel tab result arrays with one typed serialized action contract. New typed contract is active; legacy extras are still emitted as compatibility fallback.
- [x] Standardize action payloads for select, close, close group, move to group, create group, pin, unpin, archive, restore, and reorder.
- [x] Make result parsing deterministic and unit-testable.
- [x] Keep `BrowserActivity` responsible for applying UI results after parsing.

### Controller Refactor

- [ ] Split browser navigation handling into a focused coordinator or delegate.
- [ ] Split search/address input handling into a focused coordinator.
- [ ] Split tab UI state updates into a focused coordinator.
- [ ] Split permission and prompt handling into focused helpers where not already covered by delegate classes.
- [ ] Split download actions/status handling from the main activity body.
- [ ] Keep Gecko delegate ownership explicit.

### Repository And Settings Cleanup

- [x] Standardize repository background work on `AppDatabase.getDatabaseExecutor()` or a documented lifecycle-aware alternative.
- [x] Remove `ReadingListRepository`'s standalone executor or add lifecycle/shutdown handling.
- [x] Route SharedPreferences keys through `SettingsKeys` where practical.
- [x] Add a typed settings facade before larger privacy or AI preference expansion.

### Kotlin Adoption

- [ ] Pick one low-risk utility, model, or contract area for first Kotlin adoption.
- [ ] Add Kotlin only after Java tests cover the behavior being moved.
- [ ] Avoid migrating `BrowserActivity`, Gecko delegates, or Room schema code during Phase 1 unless required by a focused refactor.

### Phase 1 Exit Criteria

- [ ] Normal tabs restore correctly after process death.
- [ ] Grouped tabs restore correctly after process death.
- [ ] Pinned tabs restore correctly after process death.
- [ ] Inactive tabs restore correctly after process death.
- [x] Private tabs are never persisted or restored.
- [x] Legacy `saved_tabs` fallback is removed or isolated behind a documented migration-only path.
- [ ] `BrowserActivity` has clearer coordinator boundaries.
- [ ] `TabManager` has clearer state, persistence, session, and undo boundaries.
- [x] Room tab/group migrations have focused tests.
- [x] Core tab operations have regression coverage.
- [ ] Local build and JVM tests pass before Phase 2 starts.

## Phase 2: Modern UI (Q2 2026)

- [x] Fix Pixel 9 Pro XL home search bar overlap with bottom navigation.
- [x] Polish Pixel tab manager single-card layout and view-toggle affordance.
- [ ] Decide Compose adoption strategy: full migration, hybrid migration, or XML-only polish.
- [ ] Add Compose dependencies only after strategy approval.
- [ ] Create design tokens for color, typography, spacing, shape, and elevation.
- [ ] Modernize the tab manager surface.
- [ ] Modernize the group manager surface.
- [ ] Improve quick tab strip hierarchy and interaction states.
- [ ] Improve page, search, and home transitions.
- [ ] Add adaptive layouts for compact and large screens.
- [ ] Verify layouts against screenshots before release.

## Phase 3: Easy AI (Q3 2026)

- [ ] Define privacy policy and consent model for AI requests.
- [ ] Define cloud-only AI architecture.
- [ ] Create provider abstraction for Gemini, Claude, OpenAI, DeepSeek, and Qwen APIs.
- [ ] Start with Gemini Flash as the default low-cost provider.
- [ ] Add API-key/server configuration strategy.
- [ ] Add AI usage logging without storing sensitive page content unnecessarily.
- [ ] Implement page text extraction.
- [ ] Implement Summarize Page.
- [ ] Implement Explain Page.
- [ ] Implement Ask About Current Page.
- [ ] Implement Translate Page or improve the existing translation flow.
- [ ] Add free daily quota tracking.
- [ ] Add AI Pro entitlement checks.
- [ ] Add error, timeout, cancellation, and offline states.

## Phase 4: Smart Browser (Q4 2026)

- [ ] Define workspace and smart tab group data model.
- [ ] Implement smart tab group suggestions.
- [ ] Implement AI workspace naming.
- [ ] Implement tab auto-categorization.
- [ ] Implement tab recommendations.
- [ ] Implement reading insights.
- [ ] Add user controls to accept, reject, rename, undo, or disable smart grouping.
- [ ] Add privacy controls for URL/title/content use.

## Phase 5: Agentic Browser (2027)

- [ ] Define agent safety model and allowed actions.
- [ ] Define browser automation API for opening tabs, reading pages, comparing sources, and generating reports.
- [ ] Add human confirmation for sensitive actions.
- [ ] Build Research Agent.
- [ ] Build Shopping Agent.
- [ ] Build Travel Agent.
- [ ] Build Productivity Agent.
- [ ] Add source citation and report generation.
- [ ] Add task history and resumable sessions.
- [ ] Add rate limits and abuse prevention.
- [ ] Add safe handling for forms, checkout pages, and account pages.

## VPN Strategy

- [ ] Strengthen Secure DNS experience.
- [ ] Evaluate VPN partner integrations.
- [ ] Compare Guardian, Proton VPN, NordVPN, and Surfshark.
- [ ] Define Premium Bundle packaging.
- [ ] Revisit own VPN network only after 50,000+ active users.

## Monetization

- [ ] Confirm current AdMob implementation and policy compliance.
- [ ] Define AI Pro benefits.
- [ ] Define Browser Pro benefits.
- [ ] Define Premium Bundle benefits.
- [ ] Validate proposed INR pricing.
- [ ] Add subscription entitlement model.
- [ ] Add trial, quota, renewal, and cancellation handling.
- [ ] Ensure monetization does not weaken privacy promises.

## Success Metrics

- [ ] Crash rate.
- [ ] ANR rate.
- [ ] Startup time.
- [ ] Memory usage.
- [ ] Gecko runtime/session failure rate.
- [ ] Tab restore success rate.
- [ ] DAU.
- [ ] MAU.
- [ ] Retention.
- [ ] Session duration.
- [ ] AI feature activation.
- [ ] Revenue.
- [ ] Subscription conversion.
- [ ] Churn.

## Documentation

- [ ] Keep `README.md` aligned with shipped features.
- [ ] Keep `PROJECT_ARCHITECTURE_REPORT.md` aligned with Room and tab system changes.
- [ ] Keep this checklist updated after each completed implementation task.
- [ ] Turn roadmap phases into implementation specs before coding.
- [ ] Add release notes for each shipped phase.
