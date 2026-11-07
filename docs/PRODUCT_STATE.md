# NowShowing TV — Verified Product State & Audit
**Date:** 7 November 2026  
**Auditor:** Antigravity Product Excellence Agent  
**Baseline commit:** `3584823` (`main`)  
**Active branch:** `feat/product-excellence-and-polish`  
**Current build configuration:** `versionName = "1.6.0-rc.5"`, `versionCode = 16004`, `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`

---

## 1. Executive Summary & Verification Findings

This document records the mandatory Step 0 audit required by Section H of `docs/PRODUCT-EXCELLENCE-AGENT-HANDOFF.md`. Every hypothesis in the handoff brief was checked directly against checked-out Kotlin source files, test suites, Gradle scripts, CI workflows, and build outputs.

### Key Confirmations and Corrections:

1. **Repository Canonical Name:**  
   - Git remote: `https://github.com/smile-plzz/NowShowing-Stream-Android-TV.git`
   - In-app `UpdateChecker.kt`: `https://api.github.com/repos/smile-plzz/NowShowing-Stream-Android-TV/releases/latest`
   - CI Workflow `.github/workflows/build-apk.yml`: targets `NowShowing-Stream-Android-TV`
   - **Correction:** The URL `NowShowing-Stream-android-app-TV` appeared only in the handoff document itself; the canonical repo name across all code and releases is `NowShowing-Stream-Android-TV`.

2. **Build and Automated Test Baseline:**  
   - `./gradlew testDebugUnitTest`: **158 unit tests passed** (0 failures, 0 errors, 0 skipped across 39 test suites).
   - `python3 -m unittest discover -s scripts -p 'test_*.py'`: **8 tests passed** (0 failures, 0 errors).
   - `./gradlew assembleDebug assembleRelease`: **BUILD SUCCESSFUL**. Both APKs generated:
     - Debug (preview): `app/build/outputs/apk/debug/app-debug.apk` (`com.nowshowing.tv.preview`, 22.8 MB)
     - Release (minified with R8 & resource shrunk): `app/build/outputs/apk/release/app-release.apk` (`com.nowshowing.tv`, 10.5 MB)

3. **Database Architecture & Migration State:**  
   - Implemented via `SQLiteOpenHelper` in `NowShowingDatabase.kt` (`nowshowing_tv.db`).
   - Current schema version: **Version 2**.
   - Tables in v2: `watchlist`, `continue_watching`, `metadata_cache`, `search_history`, `recently_opened`.
   - **Confirmation:** The proposed **Schema Version 3** migration for per-episode watched tracking is the exact and proper next version.

4. **Playback & Continuation Reality:**  
   - Playback operates via `Android System WebView` (`WebEmbedScreen.kt` & `PointerWebView`).
   - Media3 infrastructure exists in source for direct streaming, but 0 native sources are currently registered.
   - Provider web embeds do not expose cross-origin completion/playback events.
   - `recently_opened` tracks when a provider page was opened (title, season, episode, source).
   - **Confirmation:** The brief's insistence that "Continue Watching" must be presented honestly as web session reopening (or "Recently Opened"), rather than false measured timestamp resume or automatic web episode advancement, is 100% accurate.

5. **Mood Engine & Discovery Implementation:**  
   - `MoodRecommendationEngine.kt` evaluates 35 distinct moods against `MediaRepository.kt`.
   - **Verification:** The hypothesis that OMDb `s` (title search) was misused for genre/theme discovery was investigated. In fact, `MediaRepository.moodCandidates(mood)` already sources candidates from `cachedCandidates()` and bounded authored curated seeds (`curatedMoodIds(mood)`), scored deterministically via `LocalRelevance.moodScore`. OMDb is NOT queried with genre or mood strings.
   - The UI picker currently displays 35 items in a single list and needs the cleaner phrase-tile design and honest sparse state copy ("No matches in the current collection").

6. **Documentation Drift Confirmed:**  
   - `docs/ARCHITECTURE.md`, `docs/WEB_PLAYBACK.md`, and `docs/KNOWN_LIMITATIONS.md` are still headed `# ... — 1.5.1`.
   - `docs/KNOWN_LIMITATIONS.md` contains an obsolete section: `Refinement source (1.6.0-rc.3, not yet built)`.
   - `README.md` references 1.6.0-rc.3 notes while reporting 1.6.0-rc.5.
   - `context.md` mentions native-only Media3 playback and Room as if active (though marked historical at top).

7. **Design System & UI Tokens:**  
   - `Color.kt` and `Theme.kt` currently define legacy tokens: `AccentCyan` (mapped to `0xFFD8B49C`), `AccentGold`, `BgDark` (`0xFF141413`), and `FocusGlow`.
   - `TvSectionTab.kt` uses pill backgrounds with solid focus fill and multi-color borders.
   - Cards in `TvFocusableCard.kt` have heavy focus borders and glow effects rather than Apple-like restraint.
   - Typography lacks the unified TV scale (56/34/26/22/18/16 sp) and safe margin standard (5% overscan margins).

8. **Settings IA:**  
   - Settings is currently divided into 6 tabs (`Playback`, `Metadata`, `Privacy`, `Help`, `App health`, `About`).
   - Needs consolidation into the 4 user-intent groups specified in E6/F10:
     1. *Playback*
     2. *Library & Data* (with local JSON Backup & Restore, Clear Cache, Reset App Data)
     3. *Privacy & Metadata* (OMDb personal keys with Test Key action, filters)
     4. *About & Support* (App health diagnostics, remote help, GitHub releases/issues, credits)

---

## 2. Workstream Roadmap & Implementation Gates

Following the brief's Section H order:

| Step | Workstream | Deliverables | Verification Gate |
|---|---|---|---|
| **0** | Mandatory Source Audit | `docs/PRODUCT_STATE.md` | Source matches reality; verified baseline |
| **1** | F1 Truth & Docs + F11 Baseline + Version Check | Canonical URL fixes, sync docs (Architecture, Limitations, Playback to 1.6.0), version sync script & test, performance baseline doc | Python test passes; no version drift; docs truthful |
| **2** | F2 Design Tokens + F3 Navigation | Minimal Apple-like dark tokens (`#0A84FF` single accent, near-black, high contrast text, 5% overscan), `NsNavBar`, `NsButton`, `NsPosterCard`, `NsShelf`, `ReturnFocusScope` focus maps | Screen & focus tests pass; contrast >= 4.5:1 |
| **3** | F5 Details + F4 Home + F6 Search | Honest Continue/Recently Opened shelf, Details screen with Watched toggle and quiet metadata, Search debouncing & clear recovery | Cached-first loading; honest state copy; all unit tests pass |
| **4** | F7 Mood & Year Picker | Refined mood grid, compact year picker dialog (All Years default), honest sparse states ("No matches in the current collection") | Deterministic ranking tests pass |
| **5** | F8 Library: Continuation, Watched Episodes, Backup/Restore | Database schema v3 migration, per-episode watched marks + "Mark previous as watched", Local JSON library backup/restore (excluding keys/secrets), Watchlist undo toast | Migration test v2->v3 passes; backup/restore roundtrip test passes |
| **6** | F9 Player Overlay + F10 Settings Consolidation | 4-second auto-hiding bottom player HUD, softer pointer cursor, pointer speed setting, Settings reorganized into 4 clear groups with Test Key action | Player lifecycle & settings tests pass |
| **7** | F11 Performance, F12 Copy Deck, F13 Accessibility | Shared honest copy deck (offline, quota, slow provider), accessibility content descriptions, TalkBack support, reduced motion respect | JVM tests pass; complete QA check |
| **8** | Release Verification & Candidate Packaging | Run `./gradlew testDebugUnitTest`, `assembleDebug`, `assembleRelease`, Python release packaging | All checks green; zero regressions |

