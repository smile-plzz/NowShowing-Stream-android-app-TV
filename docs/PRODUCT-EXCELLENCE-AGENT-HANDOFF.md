# NowShowing TV — Product Excellence Agent Handoff

**Purpose:** End-to-end product refinement, optimization, functional consistency, interaction design and visual polish.  
**Target:** Android TV / Google TV, remote-first, landscape, minimum API 26.  
**Repository:** https://github.com/smile-plzz/NowShowing-Stream-android-app-TV  
**Baseline reference:** README/docs report **1.6.0-rc.5 / versionCode 16004**. The Kotlin source tree was not successfully fetched for the original documentation review; source-level claims below are hypotheses until verified.  
**Status:** Audit and refinement specification. Do not assume this document authorizes a release, signing changes, destructive data migration or a broad rewrite. Work in a dedicated branch and preserve the current working app.

---

## Authority and precedence

The owner-supplied **7 November 2026 Product Polish and Refinement Handoff** is incorporated below as the more specific product and implementation brief. Where it is more specific than earlier guidance in this document, follow the owner-supplied brief. In particular, its explicit workstreams for web-session continuation, per-episode watched state, backup/restore, database schema v3, design tokens, navigation, README overhaul, screenshot tests and measured performance baselines are now part of the requested scope. These are implementation proposals, not permission to publish, alter signing, introduce new providers/APIs, or make irreversible data changes. Verify all proposed behavior against source before implementation.

## 1. Mission

Turn NowShowing into a coherent, dependable, premium-feeling television product. Improve the whole experience—from app launch and finding something worth watching, through details and provider playback, to returning to the exact place the viewer left—without sacrificing the recent stability and browsing improvements.

The intended visual language is **minimal, cinematic, calm, modern and Apple-like in restraint**. This means generous but purposeful spacing, strong typography hierarchy, restrained color, beautiful artwork, quiet surfaces, consistent focus behavior and simple navigation. It does not mean copying Apple TV screens or adding ornamental blur, gradients, animations and oversized cards everywhere.

### Product promise

> Find something worth watching. Understand what it is. Open it with minimal friction. Return without losing your place.

### Success criteria

1. A first-time viewer can understand the app and find a title without instruction.
2. A returning viewer can resume browsing, reopen a saved title and recover from a failed provider.
3. Every visible control has a clear purpose, focus state, activation behavior and Back behavior.
4. The UI feels like one product across Home, Browse, Mood, Search, Watchlist, Details, Player, Settings and diagnostics.
5. Browsing remains responsive on modest Android TV hardware; visual polish must not create jank or memory pressure.
6. Playback limitations are communicated honestly. A loaded provider page is not presented as confirmed video playback or measured progress.
7. Existing user data, provider behavior, metadata keys, privacy boundaries and release identity are preserved.

---

## 2. Read the current repository before changing anything

The repository contains historical plans and implementation notes. They are not all current specifications. Start with this precedence order:

1. Current source on the checked-out branch.
2. Current README and current release notes.
3. Current implementation/status and acceptance documents.
4. Architecture and feature-specific documents after checking their dates and source references.
5. Historical PRDs, old handoffs and dev logs for context only.

### Important documentation drift to resolve

- README identifies 1.6.0-rc.5 / 16004 and 158 verified unit tests.
- `docs/ARCHITECTURE.md` and several feature documents still describe 1.5.1-era behavior.
- `docs/FINAL-PRODUCT-REFINEMENT-HANDOFF.md` describes the October 20 refinement plan; `docs/REFINEMENT_IMPLEMENTATION_STATUS.md` records that mood labels/year filtering/theme ranking/About links were subsequently implemented.
- `docs/DAILY_USE_ASSESSMENT.md`, `docs/CRASH_AUDIT.md`, release notes and README describe later fixes, but some older statements remain in other documents.
- Cloud catalog behavior has evolved. Inspect the current parser, `catalog/catalog.json`, mood seeds and current UI before relying on old section counts or limits.
- The older PRD/dev log includes obsolete native/demo playback claims. Current documented movie/series provider playback uses Android System WebView; do not reintroduce demo streams or assume web completion/progress is available.

Before implementation, record the exact checked-out commit, branch, versionName/versionCode, build variant, test count and CI status. Update stale documentation when implementation changes make it misleading.

---

## 3. Product and architecture constraints

### Preserve these decisions

- Kotlin, Jetpack Compose, existing navigation/ViewModel/StateFlow approach.
- Existing OMDb metadata integration, local SQLiteOpenHelper storage and cache strategy unless measurements demonstrate a real problem.
- Existing WebView provider playback and SourceRegistry behavior. Do not replace, extract or rewrite provider playback as a design-polish shortcut.
- Existing local watchlist, recently opened/history and manual watched state.
- Existing cloud catalog contract and bundled fallback. Do not claim curated data is live trending or complete catalog coverage.
- Existing two-request global metadata concurrency bound, cache limits, privacy filtering and safe diagnostic-report boundaries.
- Existing focus restoration and stable IMDb-ID item keys.
- Existing release signing and package identity. Never change signing, package IDs, versioning or publication state casually.

### Explicit non-goals unless separately approved

- No new movie metadata API, paid AI service, account system, cloud sync, backend or mandatory user key.
- No stream extraction, DRM circumvention, arbitrary DOM/pixel click simulation or privileged JS bridge.
- No automatic web episode completion/progress claims. Manual Next episode remains the honest behavior where providers do not expose verified completion.
- No cast-photo feature merely for visual completeness.
- No broad ad-blocking expansion or provider-specific bypass.
- No full architecture rewrite, Room migration, dependency refresh or speculative optimization without evidence.
- No feature bloat: avoid widgets, badges, carousels, controls and settings that do not help the viewing journey.

---

## 4. Workstream priorities

Use the following order. Fix correctness and friction before decorative polish.

### P0 — Product correctness and daily-use reliability

- Audit every route and action against current implementation and README.
- Verify cold start, warm start, refresh, offline mode, timeout, malformed metadata, quota/key failure, empty result, stale cache and Retry behavior.
- Verify Back behavior through every navigation layer and player state.
- Verify focus restoration after Details, Settings, dialogs, picker dismissal, player exit, refresh and list updates.
- Verify rapid navigation does not publish stale ViewModel results or leak/corrupt state.
- Verify loading/error/empty/success states are distinct and actionable.
- Verify a movie opened after a series cannot inherit stale season/episode/provider state.
- Verify watchlist add/remove is consistent across Home, Details and Watchlist.
- Verify app lifecycle, WebView renderer death, disposal, pause/resume, repeated open/close and source switching.
- Preserve data on cache clearing and upgrades. Never recommend uninstalling the installed app as a casual test step.

### P1 — Navigation and interaction clarity

- Simplify the primary navigation hierarchy. Every destination should be understandable without reading a manual.
- Establish consistent Back semantics: close transient layer first, then return to previous destination, then exit only when appropriate.
- Make the currently focused item unmistakable from across a room without using a huge glow or excessive scaling.
- Keep focus movement spatially predictable. Avoid focus traps, disappearing focus, off-screen focus and accidental jumps after list refresh.
- Ensure all primary tasks are achievable with a standard D-pad/OK/Back remote. Optional hardware keys and voice search are enhancements, never the only path.
- Keep first-use guidance short, dismissible and repeatable from Help.
- Use confirmation dialogs only for destructive or hard-to-reverse actions.

### P2 — Visual system and UI polish

Create one reusable design language for all screens. Avoid screen-by-screen styling patches.

**Visual principles**
- Dark cinematic foundation with near-black background, subtly differentiated elevated surfaces and high-contrast readable text.
- One restrained accent color for selected/focused/active states. Avoid cyan everywhere, rainbow category colors, unnecessary gradients and decorative borders.
- Strong typographic hierarchy: large title, concise section heading, readable supporting copy, quiet metadata.
- Artwork is the visual hero. UI chrome should frame it rather than compete with it.
- Consistent spacing, corner radii, card dimensions, icon sizing and button heights.
- Use translucency only when it improves hierarchy or preserves context; do not use blur as a default surface.
- Prefer subtle 120–200 ms transitions. Motion must never delay input, obscure focus or run continuously.
- Respect TV overscan/safe areas and different screen densities/resolutions.

**Focus states**
- Default: calm surface with no unnecessary outline.
- Focused: clear high-contrast outline or surface shift, small scale change, restrained elevation.
- Selected: persistent selection indicator distinct from temporary focus.
- Disabled: legible but clearly unavailable.
- Loading: stable geometry; no layout jumps.
- Error: concise message plus one obvious recovery action.

Do not rely on color alone to communicate focus or selection. Test focus visibility on both dark and bright artwork.

### P3 — Performance and resource efficiency

The app targets modest TV hardware. Do not assume a modern phone-class device.

- Profile actual routes before changing image caches, memory caps, concurrency, list sizes or database behavior.
- Keep lazy lists/grids, stable keys, bounded work and lifecycle-aware collection.
- Avoid expensive per-card composition, nested unbounded scrolling, synchronous disk/network work on Main, repeated image decoding and recomposition cascades.
- Keep poster/backdrop requests appropriately sized; do not load full-resolution backdrops into poster cells.
- Avoid unnecessary crossfades, blur, large shadows, animated background layers and continuously running effects.
- Make cached content appear before slow network hydration where existing repository contracts allow.
- Cancel obsolete requests and prevent stale results from overwriting current state.
- Keep retry bounded and preserve last-known-good content.
- Distinguish app heap, system memory, frame duration, video frame rate and input latency. Do not call one measurement another.
- Use the existing App health recorder for comparable route samples. A zero-sample or unmeasured metric is not a healthy result.

### P4 — Discovery quality

- Home should communicate what is available and give a clear next action, not become a wall of shelves.
- Give each shelf a distinct purpose; remove duplicate/redundant rows.
- Curated collections must be labeled honestly. Do not imply live popularity or global catalog coverage.
- Mood choices should be immediately understandable and genuinely distinct. Keep the existing stable mood IDs/rules separate from display labels.
- Year filtering defaults to All Years and must not silently change editorial/relevance ordering.
- More Like This should prioritize meaningful theme/subgenre evidence, then compatible genre/tone/setting; actor overlap is a weak signal. Prefer fewer strong results to filler.
- Search must distinguish no results from metadata/network/quota failures and preserve prior pages when a later page fails.
- Watchlist should make saved titles easy to find, filter and sort without hiding core actions.

### P5 — Playback experience and recovery

- Preserve direct entry to provider WebView and existing remote pointer interaction.
- Keep provider-owned play interaction explicit where required. Never fake playback success based on page load.
- Make Retry source, Change source, Next episode, Episodes and Back to title obvious and consistently ordered.
- Next Source must preserve IMDb ID, season and episode and explain which source changed.
- Physical media-key handling must safely no-op when the page/video/frame is unavailable; test repeat presses and source changes.
- Test provider fullscreen Back behavior, pointer position, focus, loading timeout, renderer crash and repeated disposal.
- Keep provider limitations honest: subtitles, seeking, fullscreen and availability vary by provider.
- Avoid UI overlays that cover provider controls or persist over actual playback.

### P6 — Settings, diagnostics, privacy and About

- Organize settings by user intent, not internal architecture.
- Keep advanced technical detail out of the primary viewing flow.
- Explain consequences before clearing metadata/artwork or changing source exceptions.
- Preserve masked/local handling of personal OMDb keys; never log or commit credentials.
- Keep diagnostics opt-in, local, frozen, readable and free of title history, provider URLs, keys and raw exception messages.
- Make report sharing explicitly user-initiated and remind users to review device/build details.
- About should be concise, credible and readable from a couch. Preserve correct creator/source/social/release links; no long biography wall.
- Update checking should clearly distinguish current version, available update, network failure and prerelease status.

---

## 5. Screen-by-screen product specification

### Home
- Establish one clear first-screen hierarchy: brand/navigation, a single strong featured moment or compact continuation entry, then a small number of useful shelves.
- Avoid multiple competing hero banners.
- Continue Watching must not imply measured progress for web pages if only recently opened history exists. Label the data accurately (e.g. Recently Opened where appropriate).
- Keep shelf titles short and useful. Provide View all only when a meaningful destination exists.
- Preserve scroll position and focused card after opening Details and returning.
- Do not show empty shelves, duplicate titles or placeholder content after a failed request.
- Support warm cached display and incremental hydration without shifting the user's focus.

### Browse / Movies / TV
- Make Movies and TV type boundaries obvious.
- Use a clean grid with stable card dimensions and enough spacing for focus scaling.
- Keep sort and filter controls compact, visible and remotely operable.
- Clearly communicate that genre/year filtering applies to the loaded curated collection, not every title in existence.
- Retain cards during refresh and show a non-blocking refresh state.
- Empty and failure states need a direct Retry or relevant Settings action.

### Mood
- Keep the mood picker compact and scannable; do not let a 35-choice list dominate the screen.
- Display approved short phrases, not internal keys. Stable IDs remain the logic contract.
- Year picker: All Years default; open anchored or in a compact dialog; current value focused; OK applies; Back cancels; focus returns to opener.
- Keep mood/year state and result focus when returning from Details.
- Preserve existing candidate snapshot while changing year locally; don't re-fetch for every year change.
- Use honest sparse-state copy: no matches in the current curated pool, not no films exist.
- Mood matching should be explainable, deterministic and tested with positive/negative examples. Do not use OMDb title search as a plot/genre search.

### Search
- Give title entry, type and optional year a clear order.
- Voice search is an optional shortcut with graceful fallback when speech recognition is unavailable.
- Keep system keyboard and D-pad keyboard behavior predictable.
- Show submitted query, loaded count and pagination state.
- A failed Load more must retain earlier results and retry the failed page.
- Clear stale errors when the user changes the query or filter.
- Distinguish genuine no-results from quota, invalid key, service and network errors.

### Watchlist / Library
- Keep the saved-title state local and predictable.
- Make filtering/sorting discoverable without adding a large toolbar.
- Confirm removals only if accidental loss is likely; undo is preferable when easy and reliable.
- Ensure changes propagate to Home/Details without stale snapshots.
- Preserve stable IMDb-ID keys, focus and scroll position.

### Details
- Artwork, title and primary Play action should establish hierarchy immediately.
- Play should be the initial focus target when available.
- Metadata should be scannable: year, runtime, genre, rating and concise plot; do not dump every OMDb field.
- Keep Watchlist and source/episode actions visually secondary to Play.
- Series season/episode loading, empty, error and retry states must be explicit.
- Related titles should be absent when no credible matches exist rather than filled with weak suggestions.
- Returning from playback or a related title should restore the originating context.

### Player / Provider WebView
- Treat the provider page as the main content, not a background for app chrome.
- Controls should appear only when requested and dismiss predictably.
- Back hierarchy and pointer movement must remain consistent.
- Show source identity and actionable recovery when a provider fails.
- Do not claim a page is playing until there is reliable evidence. Do not claim automatic episode progression for WebView sources.
- Verify media keys, next-source cycling, renderer recovery and repeated lifecycle transitions on device.

### Settings / App health / About
- Keep a small set of clearly named sections with concise descriptions.
- Put common user actions first; technical details and diagnostics lower.
- Disable busy actions while work is in progress and show completion/failure feedback in context.
- Ensure long diagnostic text scrolls and remains readable at TV distance.
- Keep About short, with clear creator, project, source, release and issue links.

---

## 6. Design-system implementation guidance

First inspect existing theme tokens and shared components before adding new ones. Centralize tokens; avoid one-off hex values and copied styling.

Create or refine shared tokens for:
- Background, surface, elevated surface, primary/secondary/muted text, accent, focus, selected, disabled, divider and error.
- Spacing scale and safe-area horizontal/vertical insets.
- Typography scale and line heights.
- Corner radii, card dimensions, focus scale/elevation and motion durations.
- Poster/backdrop aspect ratios and image-loading placeholders.

Refine shared components before screens:
- Top navigation and section tabs.
- Focusable card/action/button.
- Poster card and content shelf.
- Metadata label/chip.
- Loading skeleton and image fallback.
- Empty/error/retry state.
- Dialog/picker and focus restoration helpers.
- Player control row and transient HUD.

A screen-level polish change is incomplete until it uses the shared components and has D-pad tests. Do not introduce a second parallel component system.

---

## 7. Accessibility and 10-foot usability

- Validate text size and contrast from normal couch distance, not only emulator close-up.
- Keep touch-only assumptions out of the primary TV experience.
- Provide meaningful Compose semantics and state descriptions for selected, focused, disabled and toggle controls.
- Ensure icons have labels where the icon alone is ambiguous.
- Avoid tiny metadata, low-contrast captions and dense multi-column settings.
- Keep focus targets large enough for imprecise remotes.
- Test long titles, non-Latin text, missing artwork, unusually long plots and different display densities.
- Ensure no critical action requires color recognition or precise pointer movement.

---

## 8. Reliability, privacy and data-integrity checklist

- No API key, personal URL, title history or raw exception text in logs or diagnostics.
- No network or database call on the Compose main thread.
- No unbounded retries, parallel request fan-out or unbounded in-memory collection.
- No destructive cache action that also deletes watchlist/history/preferences.
- No stale coroutine result overwriting a newer query/title/season/source.
- No crash on malformed/null provider or OMDb payloads.
- WebView renderer death must detach/destroy once and present manual recovery without reload loops.
- No automatic provider navigation outside the selected source policy.
- No weakening TLS/certificate validation or enabling popup/file/content access for convenience.
- No broad domain block rules without evidence; verify false positives against real providers.
- No crash recovery loop or auto-relaunch that hides the original failure.
- Preserve the existing report allowlist and privacy disclosure.

---

## 9. Testing and acceptance strategy

### Automated checks

Run the repository's current documented checks, adapting only after inspecting the workflow:
```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Also inspect GitHub Actions and PR checks. Report exact pass/fail counts and commit SHA. Never convert a failed/unavailable test into a pass claim.

Add or update tests for:
- Navigation/back-stack and focus restoration.
- Stable item keys and list refresh.
- Search pagination retry and filter changes.
- Mood/year selection, cancellation and deterministic ranking.
- Catalog parsing, fallback, malformed data and duplicate IDs.
- Watchlist rapid toggles and consistency.
- Series season changes, movie/series reset and episode metadata failure.
- Source switching and media-key guards.
- WebView renderer recovery and lifecycle/disposal.
- Diagnostics allowlist and secret exclusion.
- Theme/component semantics and focus states.

### Real-TV acceptance

Use the owner's actual TV only after recording installed version, variant, Android/API, WebView version, signing certificate and cache/network conditions. Prefer NowShowing Preview for side-by-side testing; it has separate storage and is not proof of release performance.

Run a repeatable route:
1. Cold launch → Home first paint → cached shelves → completed hydration.
2. Browse Movies and TV; scroll deeply and horizontally; refresh populated lists.
3. Open Details → Back; verify exact card/focus/scroll restoration.
4. Mood → change mood → year picker → Details → Back.
5. Search → load more → simulate network failure → retry → preserve previous results.
6. Watchlist add/remove → navigate away → return; verify consistency.
7. Open a series → change season rapidly → select episode → change source → Next episode.
8. Open a movie after a series; verify no stale episode/provider state.
9. Enter provider → remote pointer → OK → Back through fullscreen/controls/details.
10. Test physical Play/Pause/FF/Rewind/Next keys, voice-search unavailable path, provider failure and renderer recovery.
11. Open Settings/App health; record/freeze report; photograph all pages; confirm no secrets/title history/provider URLs.
12. Repeat the route at least three times. Compare warm/cold behavior and median/p95/p99 frame durations with equivalent conditions.

Do not set a numerical performance target before capturing a trustworthy baseline. Distinguish instrumentation overhead from actual app behavior. A successful CI build does not certify a physical TV.

### Release gates

- All automated tests and both APK build variants pass.
- Release APK has exact version, versionCode, commit and checksum evidence.
- Signing certificate is compared with the installed app before any in-place upgrade.
- Candidate is tested on the physical TV.
- Release notes, README, architecture, known limitations and acceptance docs agree with shipped behavior.
- Development-signed artifacts remain prereleases. Never label a candidate stable solely because CI is green.
- Do not publish, rotate signing keys or remove an installed app without explicit owner approval.

---

## 10. Agent execution protocol

1. **Audit first.** Inspect all current source, tests, resources, manifests, Gradle configuration, workflows, release notes and current docs. Build a source-of-truth map. Identify discrepancies rather than guessing.
2. **Baseline.** Run available tests/builds and record exact outcomes, current app screenshots, route timings, frame metrics and device environment where available.
3. **Prioritize.** Create a severity-ranked issue list: P0 correctness/crash/data loss, P1 navigation/usability, P2 visual consistency, P3 performance/discovery improvements, P4 optional enhancements.
4. **Propose before broad edits.** Present the smallest coherent milestone plan with affected files, user-visible change, risk, tests and rollback for each workstream.
5. **Implement in vertical slices.** Shared design tokens/components first, then one user journey at a time. Keep PRs focused and reviewable.
6. **Preserve behavior.** Do not silently change provider URLs, metadata contracts, catalog schema, local database schema, package identity or privacy behavior.
7. **Validate continuously.** Add regression tests with each fix. Do not batch all tests until the end.
8. **Review the result as a product.** Inspect all screens at TV resolution, every focus state, empty/loading/error states, long content and Back navigation.
9. **Update documentation.** Remove stale claims and keep README, architecture, limitations, release notes and handoff synchronized with source.
10. **Report honestly.** Separate implemented, tested in CI, tested on emulator, tested on physical TV, measured and still unverified. Include exact commit/PR and artifacts.

### Required final report format

- Executive summary and current baseline.
- Findings ranked by severity and user impact.
- Before/after behavior and screenshots for each UI workstream.
- Files/components changed and architecture decisions.
- Test/build commands with exact results.
- Physical-TV acceptance results or explicit blockers.
- Performance measurements with methodology and device conditions.
- Known limitations and deferred items.
- Data/signing/release risks.
- Exact branch, commit, PR and next owner decision.

---

## 11. Definition of done

The milestone is complete only when:

- The app looks and behaves like one cohesive, minimal, cinematic product.
- Primary journeys are intuitive with a TV remote and require no explanatory knowledge.
- Focus, selection, Back and restoration are predictable on every screen.
- Loading, empty, error and retry behavior are consistent and truthful.
- Existing playback/provider functionality and user data are preserved.
- No new unbounded work, avoidable memory pressure or visual jank is introduced.
- Automated checks pass and physical-TV checks are either completed or clearly listed as outstanding.
- Documentation describes the actual shipped behavior.
- The owner can understand exactly what changed, what was tested and what remains uncertain.

**Guiding rule:** Make the product feel simpler, not merely make the code look newer. Every enhancement must reduce friction, improve confidence, improve relevance or measurably improve quality. If it does none of those, do not add it.


---

# Owner-supplied Product Polish and Refinement Brief
**Prepared:** 7 November 2026 (Asia/Dhaka)  
**Owner:** Ismail Hossain  
**Audience:** Coding agent (Claude Code or similar) picking up the project cold  
**Baseline reported by README/docs:** 1.6.0-rc.5 / versionCode 16004

> This section is the owner's detailed product specification. It complements the broader product-excellence guidance above and takes precedence where it is more specific.

## A. Evidence limits and mandatory source audit

This brief was prepared from README and documentation including ARCHITECTURE, WEB_PLAYBACK, KNOWN_LIMITATIONS, APP_HEALTH, DAILY_USE_ASSESSMENT, FINAL-PRODUCT-REFINEMENT-HANDOFF and context.md. The Kotlin source tree was not fetched during preparation. Treat every code-behavior statement below as a hypothesis derived from documentation, not verified fact.

**Step 0 is mandatory:** inspect the actual source under `app/src/main/java/com/nowshowing/tv/`, tests, resources, Gradle files, manifests, CI workflows and scripts. Confirm or correct every behavior claim in this brief before editing. Record discrepancies and the actual product state in `docs/PRODUCT_STATE.md`.

### Binding ground rules

1. Preserve owner-approved smoother browsing, working WebView playback and current ad handling. Do not rewrite the player, swap providers or expand ad blocking in this milestone.
2. No new movie metadata API, paid AI service, account system or backend without explicit owner approval. OMDb remains the metadata source.
3. Never fake capability. If web-provider playback completion cannot be detected, do not claim autoplay or measured progress.
4. No stream extraction, DRM circumvention, new third-party provider sources or scraping.
5. Do not publish, bump a stable version or touch signing without explicit owner instruction. Any new build must use a prerelease version code greater than 16004, after verifying the current branch/version has not advanced.
6. Run the existing checks before and after changes:
   ```sh
   ./gradlew testDebugUnitTest
   python3 -m unittest discover -s scripts -p 'test_*.py'
   ./gradlew assembleDebug assembleRelease
   ```
7. Any claim of "smoother" or "faster" requires comparable App health recorder numbers from the same physical TV, same route and same build variant.

## B. Product snapshot to verify

Reported product: standalone movie and series discovery for Android TV / Google TV. Reported stack: Kotlin, Jetpack Compose, ViewModels/StateFlow, Retrofit/OkHttp, Coil, SQLiteOpenHelper (schema v2, not Room), WebView, Media3 infrastructure only. Minimum API 26, target/compile SDK 35, JDK 21. OMDb is the metadata source (bundled demo key or up to five user keys). Playback uses third-party web providers inside Android System WebView, with a pointer emulated by the D-pad.

Reported surfaces: Home, Browse (Movies/TV), Mood, Search (keyboard/system keyboard/voice), Watchlist, Details, Player (WebView), Settings (Playback, Metadata, Privacy, Help, App health, About), in-app GitHub update check, cloud JSON catalog and crash diagnostics.

Reported quality signals: 158 JVM tests including Robolectric, CI packaging both APKs with checksums and certificate information, opt-in frame-metrics recording and bounded caches. Verify counts and current status rather than repeating them as facts.

## C. Product diagnosis: highest-impact weaknesses to investigate

1. **Web-page playback with emulated pointer.** Completion, progress, resume, subtitles and fullscreen are provider-dependent; manual Next episode is required. Design the surrounding experience to make these limitations intentional, calm and transparent.
2. **Continue Watching is not real resume today.** Documentation suggests the table stores measured native progress while no native sources are registered; recently opened means a page was opened. Verify this in source. The proposed web-session continuation below is best-effort reopening, not timestamp resume.
3. **Documentation contradictions.** Reported inconsistencies include README links using both `NowShowing-Stream-android-app-TV` and `NowShowing-Stream-Android-TV`; ARCHITECTURE, WEB_PLAYBACK and KNOWN_LIMITATIONS headed 1.5.1; a KNOWN_LIMITATIONS section saying 1.6.0-rc.3 is not yet built; README linking rc.3 notes while showing rc.5; context.md describing native-only playback and Room. Verify and correct all actual instances.
4. **README as changelog.** It reportedly lacks a concise value proposition and screenshots; GitHub About has no description, website or topics.
5. **Curated discovery is shallow.** Shelves are curated; Browse filters apply only to loaded items; search is title-only; related titles may be empty on cold install.
6. **Settings density.** Six technical sections reportedly share one TV screen, intimidating non-technical household users.
7. **No data safety net.** No accounts/sync/backup restore; signing-certificate mismatch can prevent in-place upgrade.
8. **Performance claims lack real-TV before/after numbers.**
9. **Public embedded OMDb demo key can hit shared quota.**
10. **Provider/legal exposure.** The app embeds third-party sites whose licensing it does not control. Distribution channels such as Play and Fire TV are outside this milestone. Document the boundary; do not engineer around provider restrictions.

## D. Product principles

1. Calm over clever: fewer elements, more space, one obvious next action.
2. Honest states: loading, empty, stale and failed each use one short truthful sentence and one action.
3. Remote first: every essential action works with D-pad, OK, Back, Menu and Play/Pause; no dead ends or focus traps.
4. Instant feel: cached content first, no network blocking, no reordering under focus.
5. Quiet power: advanced controls (keys, filters, diagnostics) exist one level down.

## E. Design system: minimal Apple-like TV / tvOS restraint

Goal: dark, content-forward, soft depth, large type and almost no chrome. Implement as a small token layer and reusable composables. Treat these values as design targets; validate readability and performance on the actual television before locking them.

### E1. Color tokens

Centralize in one `ui/theme/` package:
- Background base: `#000000` to `#0B0B0D` (near-black).
- Surface 1: `#161618`; Surface 2: `#1F1F22`.
- Primary text: `#F5F5F7`.
- Secondary text: `rgba(245,245,247,0.64)`.
- Tertiary text: `rgba(245,245,247,0.40)`.
- Separator: `rgba(255,255,255,0.08)`.
- Exactly one accent: calm blue `#0A84FF` for focus ring, selected tab and primary action.
- Red is reserved for destructive/error states. No second accent.
- Avoid pure white body copy on pure black; use the primary text token to reduce glare.

### E2. Typography

Use one family. Prefer a system-adjacent geometric sans bundled as a font resource (Inter or similar), but check APK size, licensing and rendering before adding a font dependency. Suggested TV scale: Display 56 sp, Title 34 sp, Headline 26 sp, Body 22 sp, Caption 18 sp, Micro 16 sp. Never below 16 sp on TV. Weights: Regular 400, Medium 500, Semibold 600. Tight tracking on Display; normal elsewhere. Verify font scaling and non-Latin fallback.

### E3. Spacing, safe area and shape

- 8-pt spacing scale: 8, 16, 24, 32, 48, 64, 96.
- Target 5% overscan-safe margins (approximately 96 dp horizontal and 54 dp vertical at 1080p); derive dp from actual density and resolution rather than hard-coding pixel assumptions.
- Corner radii: 12 dp small controls, 20 dp cards/dialogs, 28 dp hero panels.
- Prefer continuous curves where supported.
- No hard borders or heavy drop shadows. A soft shadow only on focused items (target blur 32, y-offset 12, black at 30%) plus subtle scale; profile cost.

### E4. Motion, focus, icons and glass

- Motion target 180–240 ms; ease-out for focus, ease-in-out for screen transitions.
- Poster focus scale 1.06–1.08; buttons 1.04. Respect system reduced-motion setting.
- Crossfade only for hero backdrop changes. Preserve the no-poster-crossfade decision for lazy-grid items.
- No parallax tilt unless it is measured as effectively free.
- Focused item: scale + soft shadow + 3 dp accent outline, inset 2–3%; no bright block fill. Unfocused items remain flat. Indicator must be visible from 3 metres and consistent across components.
- One outline icon set, 2 dp stroke, 28–32 dp. Emojis only in Mood when intentionally part of content.
- Blur only for top navigation/overlays if its cost is measured acceptable on a mid-range TV. Otherwise use solid Surface 1 around 92% opacity.

### E5. Reusable components to build/refactor

Inspect existing shared components first; extend rather than duplicate:
1. `NsPosterCard`: 2:3, stable dimensions, title/year on focus or a one-line caption; flat Surface 1 placeholder instead of CPU-heavy shimmer.
2. `NsShelf`: Headline title, horizontal LazyRow, small peek of next card, aligned start padding.
3. `NsNavBar`: centered text tabs; selected primary text plus 2 dp accent underline; others secondary; no pill backgrounds.
4. `NsButton`: Primary filled accent, Secondary translucent Surface 2, Text-only; consistent 56 dp height.
5. `NsChip` and `NsSegmented`: type and sort controls.
6. `NsDialog` and `NsSheet`: anchored, rounded 20 dp, correct focus containment and restoration.
7. `NsEmptyState` and `NsErrorState`: icon, one sentence, one action.
8. `NsToast`: bottom-center, approximately 3 seconds, never steals focus.
9. `NsSkeleton`: flat blocks, no shimmer by default.

### E6. Screen blueprints

**Home:** Full-bleed 1280×720 backdrop where available, subtle dark bottom gradient, Display title, synopsis capped at two lines, primary Play plus secondary Details and Watchlist toggle. Below: a small set of shelves. Hero should not auto-rotate; change only through explicit user action/focus on a hero selector. Hide empty shelves. Reconcile this with the broader handoff's "single featured moment or compact continuation entry": do not create multiple competing heroes.

**Browse:** Large title, Movies | Series segmented control, one Filters action opening a sheet for Genre, Start year and Sort. Caption: "Showing N from the curated collection" rather than a warning.

**Details:** Poster left; right side title, year, runtime, rating in quiet inline text, plot up to four lines with More on OK, actions Play / Watchlist / Watched. Series episodes below, then More Like This. Cast as names only, one line. Darkened backdrop behind content.

**Search:** Query field with voice mic and system keyboard action; results grid below. D-pad keyboard is a compact on-demand panel, not permanently visible. Recent searches appear as plain text rows when empty.

**Mood:** One scrolling grid of phrase tiles with large type, short descriptor and small emoji. Compact "Year: All Years" button at top-right opens list picker. Results below; honest sparse state.

**Watchlist:** Title, All | Movies | Series segmented filter, sort action, poster grid. Empty state points toward Search.

**Player:** Full-bleed provider page. App overlay is a bottom frosted-dark control bar with episode title, Previous, Next episode (primary), Episodes, Next source, Source and Help. One-line hints appear once and remain available under Help. Overlay auto-hides after approximately four seconds of inactivity and must not permanently cover provider play controls.

**Settings:** Single-column list of four groups: Playback; Library and Data (backup/restore, clear cache); Privacy and Metadata (keys, filters); About and Support. App health/diagnostics move under About and Support > Diagnostics, with a plain-language summary before raw report details.

## F. Prioritized workstreams

### F1. Trust, documentation and version truth (P0)

- Choose one canonical repository name and correct links in README, docs, About, update checker and CI workflows. Verify update checker points to canonical repo.
- Make Gradle versionName/versionCode the source of truth. Script README banner, About version and release-note links where feasible; add a script test so version strings cannot drift.
- Reconcile document headings and release notes. Remove or archive "not yet built" sections. Mark context.md historical and correct obsolete Room/native playback claims.
- Rewrite README: first 15 lines contain one-sentence value proposition, three screenshots (Home, Details, Player overlay), three-step install and documentation links. Move rc changelog material into CHANGELOG.md/release notes.
- Populate GitHub About description and topics: android-tv, google-tv, jetpack-compose, kotlin, movie-discovery. Add website only if the owner-approved project URL exists.
- Add `docs/PRODUCT_STATE.md`: what works, what is provider-dependent and what still needs physical-TV acceptance.
- Acceptance: a new agent can read README + PRODUCT_STATE without contradictory claims; CI fails when version strings disagree.

### F2. Visual system (P0)

Implement E1–E5 as a token layer and reusable components. Migrate screens incrementally behind current navigation. Remove hard borders/heavy fills/second accent. Apply overscan-safe margins to every route. Contrast targets: body text at least 7:1 on Surface 1; secondary at least 4.5:1. Validate actual rendered contrast rather than trusting alpha values. Acceptance: route screenshots at 1080p emulator and real TV show consistent margins, type scale and focus treatment; no TV text below 16 sp unless a documented exception is approved.

### F3. Navigation, focus and information architecture (P0)

- Evaluate a centered text-tab nav: Home, Movies, Series, Mood, Search, Watchlist, Settings. Reuse one grid component for Movies/Series where appropriate; preserve existing navigation contracts.
- Document a focus map per screen: entry focus, directional exits and Back target.
- Back closes transient sheet/dialog first, then pops screen; app exit confirmation only from Home if appropriate.
- Preserve and extend `ReturnFocusScope` for Browse filter sheet and Mood year picker.
- Add Robolectric tests for route entry focus and return focus.
- Acceptance: no focus traps; Back depth is predictable; dialogs always restore focus.

### F4. Home and discovery (P1)

- Add a Continue shelf only when it has real, useful entries (see F8). For web sources, label reopening honestly.
- Hero uses first Featured item, with Play + Details; no auto-rotate.
- Cached-first progressive population; no focus jump. Put last-updated timestamp in Settings/Diagnostics, not Home.
- Surface catalog manifest revision and last refresh in diagnostics; preserve last-known-good snapshot on failure.
- Label shelves honestly: Featured, Picks for Movies, Picks for Series. Do not say Trending or New Releases without a real permitted source.
- Acceptance: cold start shows cached shelves before network completion; failed refresh never empties a visible shelf.

### F5. Details (P1)

- Follow E6 layout. Play is default focus when available.
- Add Watched toggle alongside Watchlist, subject to F8 storage/migration.
- Season selector as horizontal text list; episode rows show number/title and subtle watched indicator.
- "Retry episodes" remains available after metadata failure.
- More Like This: themes first, tone/setting next, actors weak; deduplicate before cap; never pad; show 1–6 credible results or hide section.
- Cast names only.
- Acceptance: title and Play render before recommendations resolve; series never shows stale episodes from previous title.

### F6. Search (P1)

- System keyboard and voice are primary input; D-pad keyboard is secondary/on-demand.
- Debounce typing around 300 ms and cancel superseded requests, if compatible with existing query behavior.
- Keep Load more with retry-next-page behavior.
- Empty state shows last eight recent searches and "Try voice search" hint where voice is available.
- Optional year is a small inline control.
- Acceptance: voice result fills field and triggers exactly one search; editing query clears obsolete errors.

### F7. Mood and year picker (P1)

- Keep stable internal mood IDs separate from display phrases; use short phrase labels and All Years default.
- Year picker has Apply/Back semantics; changing year filters current candidate snapshot locally and causes no request fan-out.
- Fix relevance defect if confirmed: OMDb `s` is title search, not genre/theme search. Never query OMDb with genre or theme words. Use bounded known-ID pools (cache, bundled seeds, cloud collections) and rank by authored mood evidence.
- Sparse state: "No matches in the current collection" with All Years/another mood actions.
- Deterministic ranking; mood/year changes do not reroll. Add positive and counterexample tests per mood.

### F8. Library: watchlist, web-session continuation, watched episodes and backup (P0/P1)

**Web continuation — best-effort reopen, not playback resume:**
- Record per title: last episode (where relevant), last selected source and last-opened timestamp.
- A Continue entry may say "Resume S2 E4" only when it can reopen that exact episode/source. It must not claim timestamp resume or measured playback progress.
- Use an accurate concept/label such as Continue or Recently Opened, and explain that it reopens the title/episode rather than resuming the video position.
- Preserve a useful empty state; hide shelf when empty.

**Per-episode watched marks:**
- Add manual watched toggle per episode, plus one-press "Mark previous as watched" where season/episode ordering is known.
- Store in a new database table with a non-destructive schema v3 migration. Confirm current DB version and schema in source first; if it has already advanced, design the next safe migration instead of downgrading/reusing v3.
- Do not infer watched status from page load or last-opened timestamp.

**Backup and restore:**
- Export watchlist, watched marks and recently opened entries to a local JSON file using a system share/save chooser where supported.
- Import with strict schema/version validation, duplicate handling and merge semantics. Never silently overwrite existing data.
- Exclude API keys, provider URLs, raw browsing data and secrets. Only include fields necessary to restore the user's library.
- Provide a preview/count and explicit confirmation before merge. Handle malformed/oversized files safely.
- This is a local backup, not account sync or cloud backup.

**Watchlist:**
- Keep type filter and sorting.
- Add one-press remove with Undo toast where safe.
- Acceptance: v2 seeded DB migrates with every existing row preserved; export/import round trip restores library; reinstall recovery works from a user-saved export; no provider URL or key in export.

### F9. Player experience (P1)

- Preserve WebView provider and pointer model. Redesign overlay only.
- Next episode is primary; Next Source remains one press; show current source name quietly.
- Pointer appearance: slightly larger, softer cursor with thin white ring. Add Slow / Normal / Fast Pointer speed under Playback only if current pointer code supports safe tuning.
- Hints appear once, one line, then Help.
- At 25-second loading timeout (verify existing timeout), show calm card: "This source is slow. Try next source" with Retry and Next source.
- Renderer crash recovery uses same calm card; no raw exception text.
- Back order: exit provider fullscreen, then controls, then title.
- Smoke-test same provider flow on owner's TV. Overlay auto-hides after ~4 seconds of inactivity and does not permanently cover provider controls.
- No provider rewrite, source addition, extraction, DRM bypass or ad-blocking expansion.

### F10. Settings simplification (P1)

- Four groups as specified in E6: Playback; Library and Data; Privacy and Metadata; About and Support.
- Plain-language labels and one-sentence helper text for toggles.
- Metadata keys stay local. Add Test key with clear result; keep Use demo key separate.
- Diagnostics: plain-language summary first, raw report behind View details; keep opt-in recording and report-page review.
- Add Reset app data (must preserve watchlist as specified) and Clear cache with sizes, but first define exactly which preferences/history/metadata each action clears. Never make Reset app data and Clear cache ambiguous or destructive.
- Acceptance: every common setting reachable within three D-pad moves from Settings root; diagnostics are not required for normal use.

### F11. Performance and measured optimization (P1)

**Baseline before optimization:**
- Use App health recorder on owner's physical TV, release variant.
- Route: Home → Movies → Details → Back → Watchlist → Search → Player → Back.
- Run three repetitions each in cold and warm conditions, with consistent network/cache conditions.
- Save median, p95, p99, slow-frame counts and sample counts in `docs/PERF_BASELINE.md`.
- Capture device/TV model, Android/API, WebView version, app commit, version/variant, recorder settings, route and cold/warm definition.
- Do not invent values or compare incomparable sessions.

**Only then evaluate candidates**, verifying each in source: Compose stability/recomposition in shelves, derivedStateOf for focus-driven UI, Baseline Profiles/startup profile, R8 full mode, resource shrinking, image request size hints (reported 300×450 posters and 1280×720 backdrops), unnecessary unfocused-card shadow overdraw. Add Macrobenchmark/Baseline Profile generation only if it can be maintained and CI supports it; otherwise document pending.

Acceptance: every optimization PR includes before/after numbers from same TV/route/variant; p95 regression greater than 10% blocks merge unless explained and explicitly accepted. No claim of faster/smoother without those numbers.

### F12. Reliability and truthful state copy (P1)

Create a shared copy deck for loading, empty, offline, quota, invalid key, service down, stale cache and no matches. One sentence and one action per state. Examples:
- Offline: "Offline. Showing saved titles."
- Sparse curated pool: "No matches in the current collection."
- Slow provider: "This source is taking longer than expected."
- Demo quota: gentle suggestion to add a personal key in Settings, not an error wall.

Preserve cached content on failure across Home, Browse, Mood, Search and Details. Airplane-mode walkthrough must show no blank screens or raw exception text. Distinguish no-results from quota, key, service and network failures.

### F13. Accessibility (P2)

- Content descriptions for focusable cards: title, year and type.
- Respect system font scale up to 1.3 without clipping at 1080p.
- Respect reduced motion.
- Do not rely on color alone for selected/watched state; pair with icon or text.
- Verify TalkBack announces focus changes on Settings and Details.
- Keep remote-first focus sizes and contrast; test long/non-Latin text.

### F14. Testing and QA (P0 throughout)

- Preserve existing tests; reported baseline is 158, verify current count.
- Add screenshot tests (Roborazzi or Paparazzi after checking Compose/Gradle compatibility) for every route in default and focused states to prevent visual drift.
- Extend `docs/TV_ACCEPTANCE.md` with exact route and checks; reconcile headings and route list with current app.
- Verify upgrade path: install prior release, add watchlist entries, install new build signed with same certificate and confirm data survives.
- Seed a schema-v2 DB and test non-destructive migration to the new schema.
- Run all three required checks before and after implementation. Record exact outputs and failures.

### F15. Privacy, legal and distribution (P0 awareness; owner decision)

- Public embedded OMDb demo key is public by design; retain warning and personal-key path.
- Keep API URL logging disabled and diagnostics allowlisted. Exports must not include keys, provider URLs or title history beyond the minimal explicitly required recently-opened records. Review exact export fields with privacy in mind.
- Update checker must point to canonical GitHub release, verify checksum and require user confirmation before install. Do not weaken validation.
- Third-party provider licensing is outside owner control. No new provider sources, bypasses or claims of guaranteed content availability.
- Document plainly in `docs/PRIVACY_AND_BLOCKING.md` and About: "NowShowing is a discovery app; availability depends on third-party providers."
- Play Store, Fire TV store and other distribution expansion are outside this milestone and require a separate owner decision.

### F16. Release and signing (P0 gate)

- Signing continuity is not proven; prereleases may be development-signed. Before a stable release, require a durable signing key stored as a CI secret, documented certificate fingerprint and tested same-certificate upgrade.
- Never ask owner to uninstall the main app to bypass mismatch. Preview package exists for side-by-side testing.
- Each prerelease build must have a version code greater than 16004 (or greater than the latest actual code if the branch advanced), CI green, both APKs, checksums, certificate report and exact commit in About.
- Do not publish, promote to stable, rotate keys or alter signing without explicit owner approval.

## G. Explicitly out of scope

- Replacing OMDb or adding TMDB, Trakt or similar.
- Accounts, cloud sync or backend services.
- Native playback or stream extraction.
- Ad-blocking expansion, player/provider rewrites.
- Cast photos.
- Tizen, webOS, Fire TV certification or Google Play publication.
- Live trending/popularity rankings without a permitted data source.

## H. Required execution order and gates

| Step | Deliverable | Gate |
|---|---|---|
| 0 | Read source; diff against this brief; record corrections in `docs/PRODUCT_STATE.md` | Brief matches reality |
| 1 | F1 documentation/version truth, F14 screenshot harness, F11 performance baseline | Contradictions removed; baseline saved |
| 2 | F2 + F3 design tokens, components, navigation and focus maps | Screenshots consistent; focus tests green |
| 3 | F5 + F4 + F6 Details, Home and Search | Cached-first and truthful states verified |
| 4 | F7 Mood and year | Deterministic ranking/year tests pass |
| 5 | F8 continuation, watched episodes, backup/restore and safe migration | Round-trip and migration tests pass |
| 6 | F9 + F10 Player overlay and Settings | Physical-TV playback smoke test unchanged |
| 7 | F11 measured optimization, F12 reliability and F13 accessibility | Before/after table and QA evidence |
| 8 | Prerelease candidate with new version code; real-TV acceptance | Owner signs off on actual TV |

One focused PR per step where practical. If dependencies force a different order, explain why and retain the same gates. Do not bundle a schema migration, design-system overhaul and player changes into one unreviewable change.

## I. Definition of done — owner acceptance

- Every route uses shared design tokens/components; no unexplained hard-coded colors/sizes outside theme.
- Focus is visible, consistent and recoverable; Back behavior predictable.
- No blank screen or raw exception in offline/quota/service-failure paths.
- Web Continue reopens the recorded episode/source honestly, without timestamp-resume claims.
- Per-episode watched marks and backup/restore work; schema migration preserves existing rows.
- README, docs, About, update checker and CI agree on repository and version.
- Performance claims have comparable real-TV measurements.
- Existing playback, ad handling, metadata failover and privacy guarantees are unchanged.
- Owner accepts candidate on actual TV.

## J. Starter prompt for the implementation agent

> Read `docs/PRODUCT-EXCELLENCE-AGENT-HANDOFF.md` fully. First inspect the actual source under `app/src/main/java/com/nowshowing/tv/`, tests, resources, Gradle configuration, manifests, scripts and CI. The source tree was not fetched during preparation, so list every place where this brief's code-behavior claims are wrong or unverified, then record corrections in `docs/PRODUCT_STATE.md`. Work in the order of section H, one focused PR per step. Preserve working WebView playback, current ad handling, metadata failure behavior and privacy boundaries. Implement the minimal Apple-like TV design system incrementally. Do not add APIs, providers, accounts, backend or ad-blocking changes. Do not publish or touch signing. For every performance claim, include before/after numbers from the same physical TV, route and build variant. Use a prerelease version code greater than the current baseline only when a build is required. Report completed work, exact tests, what could not be verified without the physical TV, risks, and decisions requiring owner approval.
