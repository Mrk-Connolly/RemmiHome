# Architecture Decisions Log

## ADR 001: Minimal Bootstrap Architecture

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a clean, fast, resilient foundation that remains easy to understand and safe for AI agents and human developers to modify without architectural bloat.

### Decisions
1. **Directory Boundaries:** Adopt three high-level packages (`core/`, `ui/`, `plugins/`).
2. **Framework Minimization:** Avoid dependency injection frameworks, custom event buses, state-management frameworks, or speculative utility directories (`managers/`, `services/`, `helpers/`).
3. **UI Engine:** Use Jetpack Compose with Material 3.
4. **Build System:** Gradle Kotlin DSL with Version Catalog (`gradle/libs.versions.toml`).

---

## ADR 002: Core Foundation & EventBus Primitive

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a lightweight, type-safe communication mechanism between independent systems (`Command` and `Event`) and a clear host container for core infrastructure without introducing third-party frameworks.

### Decisions
1. **EventBus Design:** Built using Kotlin `MutableSharedFlow` with non-blocking buffer emission (`BufferOverflow.DROP_OLDEST`) and reified type filtering (`eventsOfType<T>()`, `commandsOfType<T>()`).
2. **Command vs Event:**
   - `RemmiCommand`: Represents intent ("something should happen").
   - `RemmiEvent`: Represents fact ("something happened").
3. **Core Ownership:** `RemmiHost` owns `RemmiEventBus` and application `CoroutineScope`. `RemmiController` acts as coordinator.
4. **Resilience & Isolation:** Coroutine collector failure in a subscriber does not interrupt emission or other subscribers.

---

## ADR 003: Plugin Contract Primitive

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a minimal, clean contract for independent feature plugins to identify themselves, initialize against public Core capabilities, and shut down cleanly without coupling to Core implementation internals or DI frameworks.

### Decisions
1. **`RemmiPlugin` Interface:** Exposes `id: String`, `name: String`, `onInitialize(context: RemmiPluginContext)`, and `onShutdown()`.
2. **`RemmiPluginContext` API:** Encapsulates public Core capabilities (`RemmiEventBus` and `CoroutineScope`) passed during plugin initialization.
3. **Core Isolation:** Plugins depend solely on public Core contracts (`RemmiEventBus`, `RemmiEvent`, `RemmiCommand`). They do not access Core implementation internals or UI internals directly.
4. **Failure Isolation:** Plugin lifecycle failures do not impact or crash Core infrastructure or unrelated plugins.

---

## ADR 004: Core Plugin Registry

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a minimal, reliable Core-owned component to register `RemmiPlugin` instances, reject duplicate plugin IDs, orchestrate plugin lifecycle (`initializeAll`, `shutdownAll`), and enforce failure isolation.

### Decisions
1. **Registry Ownership:** `RemmiPluginRegistry` belongs in `com.remmi.core.plugin` and is owned by `RemmiHost`.
2. **Duplicate Prevention:** Registration explicitly throws `IllegalArgumentException` if a plugin with the same ID is already registered.
3. **Deterministic Lifecycle & Failure Isolation:**
   - `initializeAll(host)` initializes plugins in registration order, catching exceptions (`runCatching`) per plugin so errors in one plugin do not halt initialization of others.
   - `shutdownAll()` shuts down initialized plugins in reverse registration order, catching exceptions per plugin.
4. **State Protection:** External access to registered plugins is read-only (`List<RemmiPlugin>`).

---

## ADR 005: Self-Contained Directory Ownership & Contract Protection

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
To ensure clear architectural boundaries and filesystem readability, resources must live inside the specific system directory that owns them rather than generic dumping grounds (`core/models/`). Furthermore, established contracts must not be modified without explicit authorization.

### Decisions
1. **Rule 1 (Contract Protection):** Existing contracts (interfaces, architecture rules, GEMINI.md policy) cannot be altered by AI coders without explicit authorization.
2. **Rule 2 (Self-Contained Ownership):**
   - Eliminate generic dumping grounds like `core/models/`.
   - `RemmiCommand` relocated to `com.remmi.core.eventBus.commands.RemmiCommand`.
   - `RemmiEvent` relocated to `com.remmi.core.eventBus.events.RemmiEvent`.
3. **Contract Preservation:** Interface signatures and behavior of `RemmiCommand` and `RemmiEvent` remain 100% unchanged.

---

## ADR 006: Local-First Storage & Absence of Remmi Database/Backend

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a lightweight, local-first storage architecture that avoids heavy relational database infrastructure (Room, SQLite, DAOs) and custom server backends.

### Decisions
1. **No Relational Database:** Remmi will not use Room, SQLite, or DAOs. Relational database infrastructure is removed from current and future plans.
2. **Local-First Storage:** Remmi uses native Android storage mechanisms appropriate to specific feature requirements (e.g. DataStore for preferences, app-private files for documents, cache storage for temporary data).
3. **No Remmi Backend:** Remmi has no custom server or backend database.
4. **Direct Google Services:** Integrates directly with Google Services (Calendar, Tasks, Contacts) as external sources of truth with minimal local caching.
5. **Core Remains Small:** Storage decisions are made when features requiring persistence are implemented rather than creating premature generic persistence frameworks.

---

## ADR 007: Android Capability Boundary

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a clean capability boundary under `core/android/` that isolates Android platform implementation details behind Remmi-owned contracts, preventing Android platform types (`Context`, `PackageManager`, `Activity`) from leaking into Core or plugin-facing contracts.

### Decisions
1. **Dependency Direction:** `Plugin / Core` -> `Core capability contract` -> `Android capability implementation` -> `Android Platform APIs`.
2. **Self-Contained Capability Ownership:** Each capability lives inside its owning sub-directory (e.g. `core/android/systemInfo/`). Generic dumping grounds (`utils/`, `helpers/`, `services/`, `managers/`) are strictly prohibited.
3. **Platform Encapsulation:** Capabilities expose Remmi-owned models (`RemmiSystemInfo`) and contracts (`RemmiSystemInfoCapability`). Platform calls (`android.os.Build`) remain strictly inside the implementation (`AndroidSystemInfoCapability`).
4. **Host Integration:** `RemmiHost` holds capability contract references (`systemInfoCapability`).

---

## ADR 008: Remmi Home Screen Launcher Shell

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi's primary user-facing priority is to function as an Android launcher shell / Home screen replacement capable of discovering and launching installed Android applications without depending on plugins.

### Decisions
1. **Android Launcher Declaration:** `MainActivity` configured with `android.intent.category.HOME` and `android.intent.category.DEFAULT` intent filters, `launchMode="singleTask"`, and `stateNotNeeded="true"`.
2. **Self-Contained Launcher Capability (`core/android/launcher/`):**
   - `RemmiAppInfo`: Remmi-owned data model (`packageName`, `activityName`, `label`).
   - `RemmiLauncherCapability`: Interface exposing `installedApps: StateFlow<List<RemmiAppInfo>>`, `refreshInstalledApps()`, and `launchApp()`.
   - `AndroidLauncherCapability`: Encapsulates `PackageManager` calls off the main thread (`Dispatchers.IO`) and activity launching.
3. **Home UI (`ui/home/`):** Material 3 Compose `HomeScreen` featuring real-time app search filtering, adaptive app launcher grid with touch feedback and accessibility semantics (`Role.Button`), and non-blocking background app discovery.
4. **Plugin Independence:** Home Screen operates 100% independently from plugins.

---

## ADR 009: Core Remmi Home User Experience & Launcher Interaction Flow

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi requires a complete, responsive, and predictable launcher user journey (`Android Home` -> `Remmi Home` -> `View/Search Apps` -> `Launch App` -> `Return to Remmi Home`) operating independently without plugin dependencies.

### Decisions
1. **Header App Count & Layout Polish:** `HomeScreen` presents a Material 3 header showing real-time available app count, single-line search field with instant clear (`✕`), and Material 3 Expressive avatar color mapping.
2. **Explicit User Flow & Failure Feedback:** Tapping an app launches it; launch failures trigger a non-disruptive Material 3 `Snackbar` alert ("Unable to launch [App Name]").
3. **End-to-End Flow Verification:** `HomeLauncherFlowTest` verifies the full lifecycle journey (discovery -> search -> selection -> launch -> failure handling).

### Consequences
- Polished, reliable, zero-leak launcher experience ready for daily user interaction.

---

## ADR 010: Functional Android Home Launcher Application

* **Date:** 2026-09-30
* **Status:** Accepted

### Context
Remmi must function as a real, default-eligible Android Home / Launcher application capable of discovering all launchable on-device applications, handling system Home button events, and preserving application state when navigating between external apps and Remmi.

### Decisions
1. **Package Visibility Querying:** Declared `android.permission.QUERY_ALL_PACKAGES` in `AndroidManifest.xml` to allow `AndroidLauncherCapability` (`queryIntentActivities`) to discover all installed applications across the device on Android 11+ (API 30–35).
2. **Home Activity Configuration & Soft Input:** `MainActivity` configured with `android:launchMode="singleTask"`, `android:stateNotNeeded="true"`, `android:exported="true"`, `android:windowSoftInputMode="adjustResize"`, and intent filters for `MAIN`, `HOME`, `DEFAULT`, and `LAUNCHER`.
3. **Back Press & Lifecycle Preservation:** Registered `OnBackPressedCallback` in `MainActivity` delegating back presses to `moveTaskToBack(true)`. This prevents the Home activity from finishing or being killed when the user presses Back while on the Home screen.
4. **Flicker-Free Off-Thread Updates:** `AndroidLauncherCapability.refreshInstalledApps()` performs querying on `Dispatchers.IO` and updates `_installedApps.value` without clearing existing items during query execution, ensuring smooth transitions when returning to Home.

### Consequences
- Remmi is fully recognized and functional as an Android Home role application on device/emulator.

---

## ADR 011: Home Performance Baseline & Minimal Recomputation

* **Date:** 2026-10-01
* **Status:** Accepted

### Context
Remmi requires a fast, lightweight, and predictable Home performance profile across cold startup, destination switching, search filtering, and return-to-home lifecycle transitions without introducing heavy third-party state frameworks or relational databases.

### Decisions
1. **Redundant Discovery Elimination:** `AppsContent` in `HomeScreen.kt` verifies `if (installedApps.isEmpty())` before invoking `refreshInstalledApps()`, eliminating redundant Package Manager queries when switching between `Remmi Home`, `Workspaces`, and `Apps` tabs.
2. **Recomposition & Allocation Reductions:** Static domains list in `WorkspacesContent` is memoized via `remember` to prevent heap allocations on recomposition. Stable keys (`key = { "${it.packageName}/${it.activityName}" }`) are enforced for `LazyVerticalGrid` item rendering.
3. **Performance Baseline Metrics:** Automated performance benchmark suite `HomePerformanceTest` verifies host initialization completes in < 50ms (measured ~1-2ms) and search filtering across 500 `RemmiAppInfo` items completes in < 15ms (measured ~1ms).

### Consequences
- Lightning-fast startup and smooth 60+ fps tab transitions with zero main-thread stalls.

---

## ADR 012: Apps Screen Organization, Categorization, and Recent App Tracking

* **Date:** 2026-10-01
* **Status:** Accepted

### Context
Remmi's Apps screen requires a structured, fast organization system for installed applications (category filter chips, recent application tracking, case-insensitive search, and recent app row) without modifying established Core contracts or introducing relational databases.

### Decisions
1. **App Categorization Model:** Created `AppCategory` (`ALL`, `RECENTS`, `COMMUNICATION`, `PRODUCTIVITY`, `MEDIA`, `GAMES`, `UTILITIES`, `OTHER`) and deterministic category resolution `resolveAppCategory(context, appInfo)` leveraging Android `ApplicationInfo.category` flags and package heuristics without altering `RemmiAppInfo`.
2. **Recent Applications Tracking:** On application selection, package names are prepended to `recentAppPackages` state (deduplicated, max 10). Uninstalled applications are automatically cleansed by matching against `installedApps`.
3. **Category Chips & Recent Apps Row:** Added horizontal `LazyRow` category chips and a "Recently Used" horizontal row above the main application grid.

### Consequences
- Highly responsive, organized launcher Apps surface with category filtering and recent application tracking.

---

## ADR 013: Persistent Favorite Apps System

* **Date:** 2026-10-01
* **Status:** Accepted

### Context
Remmi's Home screen requires a persistent, manageable Favorite Apps system (up to 6 apps in a 2x3 grid) that survives application process death and device restarts without introducing relational databases or contract modifications.

### Decisions
1. **Lightweight Local Persistence:** Favorite package identifiers are stored in `RemmiPreferences` (`SharedPreferences`) as an ordered, comma-separated list (`favorite_app_packages`).
2. **Dynamic Resolution & Uninstalled Package Cleansing:** `RemmiHomeScreen` resolves stored package names against `installedApps` in user order, automatically filtering out uninstalled packages and capping at 6 items.
3. **Favorites Management:** Users can toggle favorite status using the star button on any app card in `AppsScreen` or long-press to remove a favorite directly from `RemmiHomeScreen`.

### Consequences
- Simple, zero-leak favorite apps persistence surviving process death and device reboots.

---

## ADR 014: Global Search Foundation & Deterministic Match Ranking

* **Date:** 2026-10-01
* **Status:** Accepted

### Context
Remmi requires a lightweight, fast Global Search foundation on the Home screen that searches across installed applications and persistent favorite applications with deterministic match ranking without introducing AI dependencies, databases, or external services.

### Decisions
1. **Search Model (`GlobalSearch.kt`):** Defined `GlobalSearchResult(appInfo, isFavorite, matchType)` and `filterGlobalSearch(installedApps, favoritePackages, query)`.
2. **Deterministic Match Ranking:**
   - Rank 1: Exact label match (case-insensitive)
   - Rank 2: Prefix label match (label starts with query)
   - Rank 3: Partial label match or package name match (contains query)
   - Final tie-breaker: Alphabetical sorting
3. **Home UI Integration:** `RemmiHomeScreen` renders active global search results with favorite star indicators (`★`) when `searchQuery` is non-blank, returning cleanly to the standard Home view (whitespace + 2x3 Favorite Apps grid) when blank.

### Consequences
- Fast, predictable, zero-leak global search foundation integrated cleanly with Home launcher application execution.

---

## ADR 015: Search / Command Architecture Boundary

* **Date:** 2026-10-01
* **Status:** Accepted

### Context
Remmi requires a clean, explicit boundary between Search (discovering items: "What is this?") and Commands (requesting system action: "What should Remmi do?"), utilizing the existing `RemmiCommand` contract without modifying established interfaces, introducing over-engineered routing frameworks, or leaking Android platform dependencies into command definitions.

### Decisions
1. **Application Launch Command (`LaunchAppCommand.kt`):** Defined `data class LaunchAppCommand(val appInfo: RemmiAppInfo) : RemmiCommand` in `com.remmi.core.eventBus.commands`, implementing `RemmiCommand`. Contains only Remmi model primitives (`RemmiAppInfo`), with zero Android platform dependencies (`Context`, `Activity`, `PackageManager`, `Intent`).
2. **Search Discovery to Command Mapping:** Added `fun GlobalSearchResult.toCommand(): LaunchAppCommand` in `GlobalSearch.kt` mapping search discovery results to explicit intent commands.
3. **Capability Execution:** Selection of search results constructs `result.toCommand()` and delegates execution to `RemmiLauncherCapability.launchApp(command.appInfo)`, maintaining platform execution strictly behind the capability boundary.

### Consequences
- Clean, explicit separation between Search discovery and Command intent execution, ready for future workspace and action expansions without architectural clutter.






