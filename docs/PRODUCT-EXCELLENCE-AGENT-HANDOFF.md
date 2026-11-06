# NowShowing TV — Product Excellence Agent Handoff

**Purpose:** End-to-end product refinement, optimization, functional consistency, interaction design and visual polish.  
**Target:** Android TV / Google TV, remote-first, landscape, minimum API 26.  
**Repository:** https://github.com/smile-plzz/NowShowing-Stream-android-app-TV  
**Baseline reviewed:** `main`, repository search snapshot `5b486a0900a072d525d0e1458b64f816418a6272`; README identifies **1.6.0-rc.5 / versionCode 16004**.  
**Status:** Audit and refinement specification. Do not assume this document authorizes a release, signing changes, destructive data migration or a broad rewrite. Work in a dedicated branch and preserve the current working app.

---

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
