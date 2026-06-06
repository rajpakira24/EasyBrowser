# Easy Browser Master Plan Checklist (2026-2028)

Derived from `EasyBrowser_MASTER_PLAN_2026_2028.md`.

## Pre-Work

- [x] Confirm current app version and local build/test health (`versionName 2.6.1`, `versionCode 25`).
- [ ] Confirm crash/ANR status from Play Console, Crashlytics, or another production telemetry source.
- [ ] Capture startup time baseline.
- [ ] Capture memory usage baseline.
- [x] Keep `PROJECT_ARCHITECTURE_REPORT.md` updated with current architecture.
- [x] Fix text encoding issues in the master plan file where symbols render incorrectly.
- [ ] Define owner/priority/status fields for roadmap tracking.
- [ ] Decide whether this checklist will be tracked in GitHub issues, a project board, or Markdown only.

## Phase 1: Foundation Rebuild (Q1 2026)

Goal: stabilize architecture, improve maintainability, and prepare for AI features.

- [ ] Make `BrowserState` / `BrowserStateStore` the single source of truth.
- [ ] Remove dependency on legacy `saved_tabs` SharedPreferences fallback after migration safety is proven.
- [ ] Add Room migration tests for tab and group schemas.
- [ ] Simplify tab manager activity-result contracts.
- [ ] Replace parallel tab metadata arrays with typed models or a single serialized contract.
- [ ] Refactor `BrowserActivity` into smaller coordinators.
- [ ] Refactor `TabManager` responsibilities into state, session, persistence, and undo components.
- [ ] Standardize repository threading on one executor or lifecycle-aware streams.
- [ ] Remove `ReadingListRepository`'s standalone executor or add a lifecycle/shutdown plan.
- [ ] Centralize SharedPreferences access behind a typed settings facade.
- [ ] Introduce Kotlin gradually in low-risk utility or model areas.
- [ ] Add regression tests for `BrowserStateStore`, `TabRepository`, and tab group rules.

Success criteria:

- [ ] Normal and private tabs restore correctly.
- [ ] Tab grouping, pinning, closing, and undo are covered by tests.
- [ ] No legacy tab persistence dependency remains for normal startup.
- [ ] Large controllers have clear boundaries and smaller responsibilities.

## Phase 2: Modern UI (Q2 2026)

Goal: improve polish, consistency, animation, layout quality, and long-term UI maintainability.

- [ ] Decide Compose adoption strategy: full migration, hybrid migration, or XML-only polish.
- [ ] Add Compose dependencies only after the strategy is approved.
- [ ] Create app design tokens for color, typography, spacing, shape, and elevation.
- [ ] Design a modern Material 3 visual language.
- [ ] Rebuild or modernize the tab manager.
- [ ] Rebuild or modernize the group manager.
- [ ] Improve quick tab strip visual hierarchy and interaction states.
- [ ] Improve page/search/home transitions.
- [ ] Improve animations for tab creation, close, reorder, and group movement.
- [ ] Add adaptive layouts for compact and large screens.
- [ ] Verify mobile layouts against screenshots before release.

Success criteria:

- [ ] Home, browser, tab manager, group manager, settings, and list screens share one visual system.
- [ ] Text fits on small screens.
- [ ] Tab workflows feel smooth and predictable.
- [ ] No mixed visual language between old and new surfaces.

## Phase 3: Easy AI (Q3 2026)

Goal: ship the first AI-powered browser features.

- [ ] Define privacy policy and user consent model for AI requests.
- [ ] Define cloud-only AI architecture.
- [ ] Create provider abstraction for Gemini, Claude, OpenAI, DeepSeek, and Qwen APIs.
- [ ] Start with Gemini Flash as default low-cost provider.
- [ ] Add API-key/server configuration strategy.
- [ ] Add AI usage logging without storing sensitive page content unnecessarily.
- [ ] Implement page text extraction pipeline.
- [ ] Implement Summarize Page.
- [ ] Implement Explain Page.
- [ ] Implement Ask About Current Page.
- [ ] Implement Translate Page or improve existing translation flow.
- [ ] Implement Rewrite Content where user-selected content is available.
- [ ] Add free daily quota tracking.
- [ ] Add AI Pro entitlement checks.
- [ ] Add error, timeout, and offline states.

Success criteria:

- [ ] AI features never run without user action or clear consent.
- [ ] Page content handling is documented.
- [ ] Quotas and subscription gates are enforced.
- [ ] AI responses are useful, fast, and cancelable.

## Phase 4: Smart Browser (Q4 2026)

Goal: make browsing organization intelligent and automatic.

- [ ] Define workspace and smart tab group data model.
- [ ] Implement smart tab group suggestions.
- [ ] Implement AI workspace naming.
- [ ] Implement tab auto-categorization.
- [ ] Implement tab recommendations.
- [ ] Implement reading insights.
- [ ] Add shopping, research, and travel workspace examples.
- [ ] Add user controls to accept, reject, rename, or disable smart grouping.
- [ ] Add privacy controls for whether URLs/titles/content can be used by AI.

Success criteria:

- [ ] Smart grouping does not disrupt manual tab organization.
- [ ] Users can undo AI organization decisions.
- [ ] Smart workspace names are explainable and editable.

## Phase 5: Agentic Browser (2027)

Goal: build agentic automation for research, shopping, travel, and productivity.

- [ ] Define agent safety model and allowed actions.
- [ ] Define browser automation API for opening tabs, reading pages, comparing sources, and generating reports.
- [ ] Add human confirmation for sensitive actions.
- [ ] Build Research Agent.
- [ ] Build Shopping Agent.
- [ ] Build Travel Agent.
- [ ] Build Productivity Agent.
- [ ] Add source citation/report generation.
- [ ] Add task history and resumable sessions.
- [ ] Add rate limits and abuse prevention.
- [ ] Add safe handling for forms, checkout pages, and account pages.

Success criteria:

- [ ] Agents can be stopped at any time.
- [ ] Agents explain what they are doing.
- [ ] Agents do not perform sensitive actions without confirmation.
- [ ] Reports include sources and are reproducible.

## VPN Strategy

- [ ] Stage 1: strengthen Secure DNS experience.
- [ ] Stage 2: evaluate VPN partner integrations.
- [ ] Stage 2: compare Guardian, Proton VPN, NordVPN, and Surfshark.
- [ ] Stage 3: define Premium Bundle packaging.
- [ ] Stage 4: revisit own VPN network only after 50,000+ active users.

## Monetization

- [ ] Confirm current AdMob implementation and policy compliance.
- [ ] Define AI Pro benefits.
- [ ] Define Browser Pro benefits.
- [ ] Define Premium Bundle benefits.
- [ ] Validate proposed pricing.
- [ ] Add subscription entitlement model.
- [ ] Add trial, quota, renewal, and cancellation handling.
- [ ] Evaluate affiliate shopping and search partnership constraints.
- [ ] Ensure monetization does not weaken privacy promises.

## Growth Targets

- [ ] Establish active-user measurement.
- [ ] Track 1,000 active users.
- [ ] Track 10,000 active users.
- [ ] Track 50,000 active users.
- [ ] Track 100,000 active users.
- [ ] Define acquisition channels.
- [ ] Define retention experiments.

## Success Metrics

Technical:

- [ ] Crash rate.
- [ ] ANR rate.
- [ ] Startup time.
- [ ] Memory usage.
- [ ] Gecko runtime/session failure rate.
- [ ] Tab restore success rate.

Product:

- [ ] DAU.
- [ ] MAU.
- [ ] Retention.
- [ ] Session duration.
- [ ] Tabs per session.
- [ ] AI feature activation.

Business:

- [ ] Revenue.
- [ ] AI usage.
- [ ] Subscription conversion.
- [ ] Free-to-paid conversion.
- [ ] Churn.

## Documentation

- [ ] Keep `README.md` aligned with shipped features.
- [x] Keep architecture report aligned with Room and tab system changes.
- [x] Replace or update stale references in `CLAUDE.md`.
- [ ] Turn roadmap phases into implementation specs before coding.
- [ ] Add release notes for each phase.
