# NowShowing TV — Performance & Resource Baseline
**Date:** 7 November 2026  
**Version:** 1.6.0-rc.5 (versionCode 16004)  
**Baseline commit:** `3584823`  
**Variant:** `release` (minified with R8, resource shrunk) & `debug` (preview)

---

## 1. Measured Build & Storage Artifact Baselines

| Metric | Measured Baseline | Notes / Constraints |
|---|---|---|
| **Debug APK size (`app-debug.apk`)** | 22,786,690 bytes (21.7 MB) | Development-signed Preview package (`.preview`) |
| **Release APK size (`app-release.apk`)** | 10,537,526 bytes (10.0 MB) | ProGuard/R8 full mode, resource shrinking enabled |
| **JVM Unit Test Count** | 158 tests passing | Robolectric + JUnit 4 across 39 test suites |
| **Release Packaging Test Count** | 12 tests passing | Bundle verification, version truth, package integrity |
| **Full Build Time (`assembleRelease`)** | ~7m 30s | Clean compile, R8 minification, lintVital checks |

---

## 2. In-Memory and Cache Allocation Bounds (Verified in Source)

- **Coil Decoded Image Cache:**  
  - In-memory bitmap pool: capped at **16 MiB**  
  - On-disk cache: capped at **50 MiB**  
  - Downsampling: Posters requested at **300×450**, backdrops at **1280×720**  
  - Lazy grid optimization: Flat placeholders, no image crossfades in LazyRows/LazyColumns.

- **Metadata Repository & SQLite:**  
  - Concurrent network calls: bounded globally to **2 active requests** across all repository instances  
  - Mutex striping: **64 striped mutexes** to prevent duplicate concurrent queries for identical IMDb IDs  
  - In-memory LruCache: **128 entries**, 24-hour TTL  
  - Persistent SQLite payload store (`metadata_cache`): capped at **500 records**, **64 KiB** max per payload (~32 MiB logical cap)  
  - Pruning: performed automatically on app startup and cache insertion.

---

## 3. Physical TV Measurement Protocol (Pending Owner Device Session)

To satisfy the brief's requirement that no performance claims ("smoother", "faster") be made without comparable numbers, the following protocol is established for physical TV evaluation using the built-in App Health Recorder (`BrowsingRecorder`):

### Standard Benchmark Route:
1. **Cold Launch:** App start from closed state → Home first paint → cached shelves visible → network hydration complete.
2. **Navigation Loop:**
   - Home → scroll first shelf
   - Navigate to Movies tab → scroll grid down 3 rows
   - Open title Details → verify initial Play focus
   - Press Back → verify exact scroll and card focus restoration
   - Navigate to Watchlist
   - Navigate to Search → enter query "Batman" → clear query
   - Navigate to Details → select Play → enter WebView provider
   - Press Back → exit fullscreen/controls → return to title
3. **Repetition:**
   - 3 cold runs (clearing process between runs)
   - 3 warm runs (navigating route repeatedly in a single session)

### Metrics to Record from App Health Dialog:
- Frame count & duration
- Median frame time (ms)
- 95th percentile frame time (p95 ms)
- 99th percentile frame time (p99 ms)
- Slow frame count (>16.6ms for 60Hz, >33.3ms for 30Hz)
- Pss / Java heap footprint at route completion
- Target TV Model, Android OS version, WebView implementation version

*Note:* In accordance with ground rule 7 of the handoff brief, all optimization work will be guarded against this protocol.

