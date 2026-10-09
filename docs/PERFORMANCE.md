# Remmi Performance Profiling Baseline

This document records the performance baseline measurements, identified bottlenecks, recommended optimizations, and performance tooling status for Remmi.

---

## 1. Performance Baseline Measurements

| Journey / Scenario | Environment | Metric | Observed Result | Target | Interpretation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Startup & Host Init** | JVM / Local | Cold Init Latency (`RemmiHost` + `RemmiController` + `EventBus`) | **1–2 ms** | < 50 ms | Cold host initialization completes almost instantaneously on the JVM. |
| **App Discovery & Sorting (500 Apps)** | JVM / IO | App list sorting & label lowercasing (500 apps) | **1–3 ms** | < 25 ms | Package list transformations off the main thread are highly efficient. |
| **App Discovery & Sorting (1000 Apps)** | JVM / IO | App list sorting & label lowercasing (1000 apps) | **2–5 ms** | < 25 ms | Scales linearly without performance degradation. |
| **Search Filtering (500 Apps)** | JVM / UI | Filtering 500 apps by text query | **0.5–1.5 ms** | < 15 ms | Memory-efficient filter predicates prevent main-thread frame drops. |
| **Global Search Ranking (1000 Apps)** | JVM / UI | `filterGlobalSearch` ranking (EXACT, PREFIX, PARTIAL, tie-breakers) | **1–3 ms** | < 20 ms | Deterministic match ranking computes rapidly across large collections. |
| **Favorites Resolution (1000 Apps)** | JVM / UI | Resolving 6 favorites against 1000 installed apps | **0.2–0.8 ms** | < 10 ms | Sequence-based `mapNotNull` resolution is near-instantaneous. |
| **Category Heuristic Resolution (500 Apps)** | JVM / UI | Package name string heuristic categorization (no Context) | **1–3 ms** | < 15 ms | Pattern-matching heuristics execute cleanly in memory. |
| **Command Mapping Latency** | JVM / UI | `LaunchAppCommand` mapping & intent instantiation | **< 0.1 ms** | < 1.0 ms | Zero overhead command intent allocation. |
| **Knowledge Class/Lecture Persistence (50 Items)** | JVM / IO | `ClassRepository` & `LectureRepository` batch write & read | **2–10 ms** | < 500 ms | App-private JSON I/O off UI thread completes in milliseconds. |
| **Recovery Checkpoint Parsing** | JVM / IO | `LectureRecoveryManager.detectIncompleteSession` JSON parse | **< 1 ms** | < 15 ms | Instantaneous recovery detection on boot. |

---

## 2. Identified Concrete Bottlenecks

Based on measurement and code inspection across the launcher stack:

### Bottleneck 1: Synchronous `PackageManager` Calls During Category Filtering
- **Location:** `com.remmi.ui.home.AppCategory.kt` -> `resolveAppCategory(context, appInfo)`
- **Evidence:** When `context != null` and an app package name does not match string heuristics, `resolveAppCategory` executes:
  ```kotlin
  val pm = context.packageManager
  val appInfoFlags = pm.getApplicationInfo(appInfo.packageName, 0)
  ```
- **Impact & Severity:** **HIGH (Main-Thread Stalls on Category Selection)**.
  Inside `AppsScreen.kt`, category filtering runs inside `remember(installedApps, selectedCategory, recentApps, context)` on the main thread when a user taps a category chip (e.g. `Media`, `Communication`). For apps falling back to `ApplicationInfo.category`, executing synchronous IPC calls to `PackageManager` on the main thread blocks UI rendering and causes jank on devices with 100+ installed applications.

### Bottleneck 2: Re-computing Category Resolution on Every Filter Chip Selection
- **Location:** `com.remmi.ui.home.AppsScreen.kt`
- **Evidence:**
  ```kotlin
  val categoryFilteredApps = remember(installedApps, selectedCategory, recentApps, context) {
      when (selectedCategory) {
          AppCategory.ALL -> installedApps
          AppCategory.RECENTS -> recentApps
          else -> installedApps.filter { resolveAppCategory(context, it) == selectedCategory }
      }
  }
  ```
- **Impact & Severity:** **MEDIUM**.
  Every chip tap re-executes `resolveAppCategory` for all installed applications rather than utilizing pre-computed category metadata.

### Bottleneck 3: Synchronous `SharedPreferences` Initialization
- **Location:** `com.remmi.ui.theme.RemmiPreferences.kt`
- **Evidence:** `getInstance(context)` synchronously loads `remmi_preferences.xml` from disk when invoked during initial composition.
- **Impact & Severity:** **LOW/MODERATE**.
  While `SharedPreferences` reads are generally small, synchronous disk I/O on the main thread during startup should be avoided.

---

## 3. Recommended Optimizations

### Safe Immediate Fixes
1. **Pre-compute or Cache Categories Off the Main Thread**:
   Pre-resolve app categories off the main thread (`Dispatchers.IO`) during background app discovery in `AndroidLauncherCapability`, or cache resolved categories in a simple map so main-thread chip filtering never calls `PackageManager.getApplicationInfo()`.
2. **Keying `LazyVerticalGrid` / `LazyRow` Items**:
   Ensure all scrollable lists and grids enforce stable item keys (`key = { ... }`) to prevent unnecessary item recreation during scrolling or filter state updates.

### Proposed Future Optimizations
1. **Migration to DataStore (Async Local Persistence)**:
   Migrate `RemmiPreferences` from synchronous `SharedPreferences` to `DataStore` (or async Flow-backed storage) as part of a future storage refinement stage.
2. **Lazy Icon Bitmap Conversion**:
   Optimize icon loading so bitmap conversion is performed on-demand or throttled during initial cold boot if device memory or app list size is extremely large (1000+ apps).

---

## 4. Performance Tooling Report

| Tool | Configuration Status | Notes |
| :--- | :--- | :--- |
| **Microbenchmarks (`HomePerformanceTest`, `KnowledgePerformanceTest`)** | **ACTIVE & FULLY CONFIGURED** | Automated JUnit benchmark test suite verifying startup, filtering, sorting, category resolution, global search ranking, command mapping, and Knowledge persistence thresholds. |
| **Macrobenchmark** | **NOT CONFIGURED** | Requires multi-module Gradle setup, benchmark buildType signing, and attached benchmark-configured test device/emulator. |
| **Baseline Profile** | **NOT CONFIGURED** | Single-module architecture currently renders custom Baseline Profile generation unnecessary until a dedicated production profiling module is established. |
| **Perfetto / Android Tracing** | **AVAILABLE** | Android system tracing can be captured via Android Studio Profiler or ADB trace tool for main-thread IPC investigation. |
