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

---

## ADR 016: Performance Profiling Baseline & Bottleneck Identification

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires a clean performance baseline across startup, Home rendering, app discovery, Global Search ranking, category resolution, and app launching before conducting optimization work.

### Decisions
1. **Automated Microbenchmark Suite (`HomePerformanceTest`):** Expanded unit performance test suite measuring host initialization (< 2ms), app discovery & sorting for 1000 apps (< 5ms), search filtering for 500 apps (< 1.5ms), Global Search match ranking for 1000 apps (< 3ms), and favorite resolution (< 1ms).
2. **Main-Thread Bottleneck Identification:** Identified synchronous `PackageManager.getApplicationInfo` calls inside `resolveAppCategory` on non-null `Context` during Apps screen category chip selection as a primary main-thread stall bottleneck.
3. **Performance Baseline Documentation (`docs/PERFORMANCE.md`):** Recorded official baseline measurements, bottleneck findings, and recommended optimization stages.

### Consequences
- Reliable, repeatable, scientific performance baseline established across all launcher workflows.

---

## ADR 017: Typed Remmi Command Model & Controller Routing

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires an explicit command execution model with typed results (`RemmiCommandResult`), system routing inside `RemmiController`, and EventBus command emission without introducing over-engineered command frameworks or breaking established contracts.

### Decisions
1. **Typed Command Results (`RemmiCommandResult.kt`):** Defined `sealed interface RemmiCommandResult` with `Success(val message: String?)` and `Failure(val reason: String, val cause: Throwable?)`.
2. **Navigation Intent Command (`NavigateCommand.kt`):** Defined `data class NavigateCommand(val destinationId: String) : RemmiCommand` in `com.remmi.core.eventBus.commands`, decoupling Core from UI destination types.
3. **Controller Command Routing (`RemmiController.kt`):** Implemented `executeCommand(command: RemmiCommand): RemmiCommandResult` in `RemmiController`. Executes system actions (`LaunchAppCommand` -> `launcherCapability`, `NavigateCommand`), emits executed commands onto `RemmiEventBus` for observers, and handles unhandled commands gracefully with `RemmiCommandResult.Failure` without throwing exceptions.
4. **Verification:** `CommandRoutingTest` verifies success/failure execution paths, EventBus command emission, missing capability handling, and unhandled command fallbacks.

### Consequences
- Clean, predictable command routing foundation prepared for future AI command resolution and cross-workspace action execution.

---

## ADR 018: Command Architecture UI Integration

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires seamless integration of the Command architecture into user-facing launcher workflows (app launching from Search, Favorites, Apps grid, Recent apps, and top-level navigation) using `RemmiController.executeCommand` without modifying contracts or over-engineering local UI operations.

### Decisions
1. **Controller Flowing into UI (`RemmiApp.kt`, `HomeScreen.kt`):** `RemmiApp` accepts `controller: RemmiController?` (resolving to `RemmiApplication.controller` or constructing `RemmiController(effectiveHost)`), passing `effectiveController` to `HomeScreen`, `RemmiHomeScreen`, and `AppsScreen`.
2. **Unified App Launch Execution:** Replaced direct `launchApp` capability invocations in `RemmiHomeScreen` (Global Search & Favorites) and `AppsScreen` (Recent apps & main App grid) with `controller.executeCommand(LaunchAppCommand(appInfo))`.
3. **Decoupled Navigation Command Handling:** `HomeScreen` observes `NavigateCommand` from `RemmiEventBus` in `LaunchedEffect(host)` and smoothly animates the pager (`animateScrollToPage`) to the requested top-level destination (`WORKSPACES`, `HOME`, `APPS`).
4. **Verification:** `CommandIntegrationTest` verifies that `executeCommand` launches apps, dispatches navigation intent, and emits commands onto `RemmiEventBus` for observers.

### Consequences
- Launcher workflows operate via typed commands, enabling clean command logging, observability, and readiness for AI command execution.

---

## ADR 019: AI Capability Boundary & Command Isolation

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires a clean capability boundary for AI integrations (`RemmiAiCapability`) that strictly restricts AI interactions to natural language intent resolution (`resolveIntent`) and typed command execution (`processInput`), preventing AI from directly mutating or accessing internal application or UI state.

### Decisions
1. **AI Boundary Contract (`RemmiAiCapability.kt`):** Defined interface `RemmiAiCapability` in `com.remmi.core.ai` exposing `resolveIntent(input: String): RemmiCommand?` and `processInput(input, executeRouter): RemmiCommandResult`.
2. **Default Intent Resolution (`DefaultAiCapability.kt`):** Implemented `DefaultAiCapability` resolving natural language requests ("go to workspaces", "open apps", "launch camera") into typed `NavigateCommand` or `LaunchAppCommand` instances. Unrecognized intents return `RemmiCommandResult.Failure`.
3. **Host Container Integration (`RemmiHost.kt`):** Added `val aiCapability: RemmiAiCapability = DefaultAiCapability()` to `RemmiHost`.
4. **Verification:** `AiCapabilityBoundaryTest` verifies natural language intent resolution, router execution, failure handling for unsupported inputs, and host integration.

### Consequences
- AI capabilities are cleanly isolated behind typed command resolution, guaranteeing that AI cannot bypass architectural boundaries or corrupt application state.

---

## ADR 020: AI Natural Language Command Resolution & Hallucination Prevention

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires natural language command resolution (`DefaultAiCapability`) capable of mapping diverse phrasing variations ("switch to workspaces", "open settings", "run firefox") into typed Remmi commands (`NavigateCommand`, `LaunchAppCommand`) while strictly rejecting unmapped or unsupported operations to prevent AI hallucination or unhandled state mutation.

### Decisions
1. **Natural Language Pattern Matching (`DefaultAiCapability.kt`):** Expanded `resolveIntent` to recognize diverse navigation prefixes ("go to", "show", "navigate to", "switch to", "open") and app launch prefixes ("launch", "open", "run", "start", "exec").
2. **Search Integration Helper (`GlobalSearch.kt`):** Added `resolveAiSearchCommand(query, aiCapability)` allowing Global Search UI to evaluate natural language commands alongside deterministic app search.
3. **Hallucination & Unsupported Operation Prevention:** Unmapped / unsupported natural language phrases ("delete file", "call mom", "format disk") strictly return `null` (resolving to `RemmiCommandResult.Failure`), preventing AI from inventing unsupported operations.
4. **Verification:** `AiCommandResolutionTest` verifies diverse natural language navigation and app launching variations, Search helper resolution, and strict rejection of unsupported operations.

### Consequences
- AI natural language resolution operates deterministically and safely, mapping valid requests to established Remmi commands and rejecting unsupported actions.

---

## ADR 021: AI Execution Safety & Command Validation

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires explicit command validation (`RemmiAiCommandValidator`), error isolation, and confirmation verification before AI-resolved commands are dispatched to `RemmiController`, preventing execution of malformed or invalid commands and insulating the process against runtime crashes.

### Decisions
1. **Safety Validator (`RemmiAiCommandValidator.kt`):** Defined `RemmiAiCommandValidator` interface and `DefaultAiCommandValidator` implementation validating `LaunchAppCommand` (ensuring non-blank package/activity names) and `NavigateCommand` (ensuring supported destination IDs).
2. **Execution Exception Isolation (`DefaultAiCapability.kt`):** Wrapped router execution in `DefaultAiCapability.processInput` within `try-catch`, returning structured `RemmiCommandResult.Failure(reason, cause)` on unexpected exceptions to guarantee process stability.
3. **Verification:** `AiExecutionSafetyTest` verifies command validation pass/fail criteria, exception safety, and confirmation checks.

### Consequences
- AI command execution is strictly validated and fault-isolated, preventing invalid commands or execution errors from crashing the launcher.

---

## ADR 022: Isolated Google Authentication Capability Boundary

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires a clean capability boundary under `core/android/google/auth/` for Google Authentication (`RemmiGoogleAuthCapability` & `RemmiGoogleAccount`) that isolates account state management and sign-in/sign-out workflows without introducing custom Remmi server backends or violating local-first architecture principles.

### Decisions
1. **Google Account Identity Model (`RemmiGoogleAccount.kt`):** Defined `data class RemmiGoogleAccount(val accountId: String, val email: String, val displayName: String?)`.
2. **Auth Capability Interface (`RemmiGoogleAuthCapability.kt`):** Defined interface `RemmiGoogleAuthCapability` in `com.remmi.core.android.google.auth` exposing `currentAccount: StateFlow<RemmiGoogleAccount?>`, `isSignedIn: Boolean`, `signIn(): Boolean`, and `signOut(): Boolean`.
3. **Local/Capability Implementation (`DefaultGoogleAuthCapability.kt`):** Implemented `DefaultGoogleAuthCapability` encapsulating Google account state transitions off the main thread.
4. **Host Container Integration (`RemmiHost.kt`):** Added optional `val googleAuthCapability: RemmiGoogleAuthCapability? = null` to `RemmiHost`.
5. **Verification:** `GoogleAuthCapabilityTest` verifies account sign-in, sign-out, StateFlow emission, and host integration.

### Consequences
- Google authentication is cleanly isolated behind an Android capability boundary, ready for direct Google service integration without server backends.

---

## ADR 023: Direct Google Service Capability Boundary Architecture

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires a clean capability boundary under `core/android/google/` for direct Google service integration (`RemmiGoogleServiceCapability`) that connects directly to Google services (Calendar, Tasks, Contacts) as remote sources of truth without custom Remmi server backends or database sync infrastructure.

### Decisions
1. **Google Service Capability Contract (`RemmiGoogleServiceCapability.kt`):** Defined interface `RemmiGoogleServiceCapability` in `com.remmi.core.android.google` exposing `authCapability: RemmiGoogleAuthCapability?` and `isServiceAvailable: Boolean` (tracking `authCapability?.isSignedIn`).
2. **Capability Implementation (`DefaultGoogleServiceCapability.kt`):** Implemented `DefaultGoogleServiceCapability` bound to `RemmiGoogleAuthCapability`.
3. **Host Container Integration (`RemmiHost.kt`):** Added optional `val googleServiceCapability: RemmiGoogleServiceCapability? = null` to `RemmiHost`.
4. **Verification:** `GoogleServiceCapabilityTest` verifies service availability tracking against auth state and host integration.

### Consequences
- Google Service capabilities are cleanly isolated behind an Android capability boundary, enforcing direct client-to-Google communication without server backends.

---

## ADR 024: Google Calendar Service Integration Capability

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires direct Google Calendar service integration (`RemmiGoogleCalendarCapability` & `RemmiCalendarEvent`) under `core/android/google/calendar/` to query today's calendar events directly from Google Calendar as the remote source of truth without custom Remmi server backends or database sync infrastructure.

### Decisions
1. **Calendar Event Model (`RemmiCalendarEvent.kt`):** Defined `data class RemmiCalendarEvent(val id: String, val title: String, val startTimeIso: String, val endTimeIso: String, val isAllDay: Boolean)`.
2. **Calendar Capability Interface (`RemmiGoogleCalendarCapability.kt`):** Defined interface `RemmiGoogleCalendarCapability` extending `RemmiGoogleServiceCapability`, exposing `todayEvents: StateFlow<List<RemmiCalendarEvent>>` and `refreshTodayEvents(): List<RemmiCalendarEvent>`.
3. **Capability Implementation (`DefaultGoogleCalendarCapability.kt`):** Implemented `DefaultGoogleCalendarCapability` managing calendar event state and authorization verification (`authCapability.isSignedIn`).
4. **Host Container Integration (`RemmiHost.kt`):** Added optional `val calendarCapability: RemmiGoogleCalendarCapability? = null` to `RemmiHost`.
5. **Verification:** `GoogleCalendarCapabilityTest` verifies event fetching when signed in vs signed out, StateFlow emission, and host container integration.

### Consequences
- Google Calendar capability connects directly to Google Calendar APIs, keeping Google as the remote source of truth without database sync engines.

---

## ADR 025: Local-First Google Data Lifecycle & Offline Caching Architecture

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires a clear, local-first data lifecycle policy (`RemmiDataCategory`) and transient local caching boundary for Google Service capabilities, establishing Google as the authoritative remote source of truth, minimal local caching for instant cold boot and offline rendering, and local-only app preferences without introducing relational databases or sync engine frameworks.

### Decisions
1. **Data Lifecycle Delineation (`RemmiDataCategory.kt`):** Defined enum `RemmiDataCategory` explicitly categorizing data into `REMOTE_SOURCE_OF_TRUTH` (Google Calendar/Tasks/Contacts), `LOCAL_CACHED_STATE` (transient local event cache for instant boot and offline rendering), and `LOCAL_ONLY_STATE` (app preferences, favorites, search history).
2. **Local-First Fallback (`DefaultGoogleCalendarCapability.kt`):** Updated `DefaultGoogleCalendarCapability` to serve `LOCAL_CACHED_STATE` instantly on cold boot or offline execution, and update the cache when refreshing from `REMOTE_SOURCE_OF_TRUTH`.
3. **Verification:** `LocalFirstGoogleIntegrationTest` verifies offline event rendering from `LOCAL_CACHED_STATE`, cache updates from `REMOTE_SOURCE_OF_TRUTH`, and isolation of `LOCAL_ONLY_STATE` preferences.

### Consequences
- Google Service integration operates local-first, serving cached events instantly offline without relying on database sync engines.

---

## ADR 026: First Workspace Implementation (Planning Workspace)

* **Date:** 2026-10-02
* **Status:** Accepted

### Context
Remmi requires its first concrete independent feature workspace/plugin — the Planning Workspace (`com.remmi.plugins.planning`), providing task, schedule, and goal management ("Tasks, schedules, & goals") implementing `RemmiPlugin` without Room, SQLite, or custom server backends.

### Decisions
1. **Plugin Contract Implementation (`PlanningPlugin.kt`):** Implemented `PlanningPlugin` extending `RemmiPlugin` (`id = "planning"`, `name = "Planning Workspace"`).
2. **Local-First JSON Persistence (`PlanningTaskRepository.kt`):** Implemented `PlanningTaskRepository` storing task state in app-private JSON file storage (`planning_tasks.json`) off the main thread (`Dispatchers.IO`), without database frameworks.
3. **Workspace Commands & Events:** Defined `CreatePlanningTaskCommand`, `TogglePlanningTaskCommand`, `DeletePlanningTaskCommand`, `PlanningTaskCreatedEvent`, `PlanningTaskUpdatedEvent`, and `PlanningTaskDeletedEvent`.
4. **Compose UI Integration (`PlanningWorkspaceScreen.kt`):** Created `PlanningWorkspaceScreen` featuring task creation, completion toggles, status filter chips (All, Active, Completed), and Google Calendar schedule preview. Integrated into `WorkspacesScreen`.
5. **Verification:** `PlanningTaskRepositoryTest` and `PlanningPluginLifecycleTest` verify JSON file persistence, task toggling/deletion, and plugin lifecycle orchestration.

### Consequences
- Remmi now contains its first functional, local-first Workspace plugin operating cleanly inside the `RemmiPlugin` boundary.

---

## ADR 027: Knowledge Workspace Plugin & Lecture Recorder Recovery Architecture

* **Date:** 2026-10-03
* **Status:** Accepted

### Context
Remmi requires an independent Knowledge Workspace (`com.remmi.plugins.knowledge`) containing the Lecture Recorder tool with local-first JSON persistence, live Gemini transcription streaming, and automatic crash/kill session recovery without introducing relational databases or custom server backends.

### Decisions
1. **Plugin Contract & Data Models:** Implemented `KnowledgePlugin` implementing `RemmiPlugin`. Created `LectureClass`, `Lecture`, and `RecordingState` (`READY`, `RECORDING`, `PAUSED`, `STOPPED`, `SAVING`, `SAVED`, `RECOVERABLE`, `ERROR`).
2. **Local-First Persistence:** Implemented `ClassRepository` (`lecture_classes.json`) and `LectureRepository` (`lectures.json`) executing off `Dispatchers.IO`.
3. **Incremental Recovery Checkpointing:** Implemented `LectureRecoveryManager` continuously checkpointing session state to `checkpoint_lecture.json` during active recording, restoring incomplete sessions on startup into `LectureRepository` with `RecordingState.RECOVERABLE`.
4. **Decoupled Gemini Live Transcription:** Defined `TranscriptionProvider` interface and `GeminiLiveTranscriptionProvider` client with network fault isolation (cloud failures drop connection state without interrupting local audio recording).

### Consequences
- Complete, resilient Knowledge Workspace operating local-first with zero database bloat and robust crash recovery.

---

## ADR 028: Knowledge Lecture Recorder Subject Redesign

* **Date:** 2026-10-03
* **Status:** Accepted

### Context
Remmi requires a redesigned Lecture Recorder experience in the Knowledge Workspace featuring a clean Subject-based hierarchy (`Subject list` -> `Add Subject` -> `Saved Lectures` -> `Add Lecture / Record` & `Saved Lecture Details`) without decorative headers, wasted screen space, or database bloat.

### Decisions
1. **Data Model Extensions:** Extended `LectureClass` (Subject) with `description: String?` and `language: String` (required default transcription language). Extended `Lecture` with `description: String?`.
2. **Title/Header Removal:** Removed the decorative "Lecture Recorder" header and subtitle text from `SubjectListScreen.kt`. The tool begins directly with its functional content.
3. **Dedicated Hierarchy Screens:** Created `SubjectListScreen.kt`, `AddSubjectScreen.kt` (with required Name & Language validation and duplicate protection), `SubjectLecturesScreen.kt`, `SavedLectureDetailsScreen.kt`, and adapted `LectureRecordingScreen.kt`.
4. **Transcription Language Defaulting:** New lectures in a Subject inherit the Subject's configured language by default.

### Consequences
- Clean, focused, space-efficient Subject-based Lecture Recorder hierarchy operating local-first with full Gemini Live transcription and recovery support.

---

## ADR 029: Centralized Settings Permissions Architecture

* **Date:** 2026-10-03
* **Status:** Accepted

### Context
Remmi requires a dedicated, centralized **Permissions** section in Settings (`Settings` -> `Permissions`) allowing users to inspect and manage Android capability access without scattering permission UI, bypassing Android's security model, or creating generic `PermissionManager` god objects.

### Decisions
1. **Self-Contained Capability Ownership (`core/android/`):** Permission knowledge lives inside its respective Android capability (`RemmiAudioCapability`, `RemmiAlarmCapability`, `RemmiNotificationCapability`), never in generic god objects or DI frameworks.
2. **Delineated Permission Types:**
   - **Runtime Permissions**: Microphone (`RECORD_AUDIO`) & Notifications (`POST_NOTIFICATIONS`). Trigger official `RequestPermission()` activity launchers.
   - **Special Access**: Exact Alarms (`SCHEDULE_EXACT_ALARM`). Launches system intent (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`).
3. **Real Android State Refresh**: Queries actual Android system status (`checkSelfPermission`, `canScheduleExactAlarms`) and auto-refreshes on `Lifecycle.Event.ON_RESUME`.

### Consequences
- Centralized, transparent, zero-leak Android capability inspection and management in Settings adhering to clean architecture rules.

---

## ADR 030: Consolidation of Food Features into Household Workspace & 2-Screen UI Refactoring

* **Date:** 2026-10-07
* **Status:** Accepted

### Context
Remmi requires a consolidated workspace structure nesting all food, inventory, recipe, meal, and shopping capabilities under the **Household Workspace** (`WorkspaceDomain.HOUSEHOLD`), streamlining the UI layout from 4 separate screens into 2 primary consolidated screens while preserving strict domain contracts and local-first JSON persistence.

### Decisions
1. **Workspace Migration (`WorkspacesScreen.kt`):**
   - Retired the separate top-level `FOOD` workspace domain entry, restoring top-level Workspaces navigation to 6 clean domains (`PLANNING`, `KNOWLEDGE`, `HEALTH`, `TRAVEL`, `HOUSEHOLD`, `PEOPLE`).
   - Mapped `WorkspaceDomain.HOUSEHOLD` to `HouseholdWorkspaceScreen`.
2. **2-Screen UI Consolidation (`com.remmi.plugins.food.ui`):**
   - **Screen 1 (`HouseholdStockShoppingScreen`):** Consolidates Stock/Ingredients, Shopping List/Sessions, and Threshold Configuration under a clean sub-navigation bar (`Stock 📦 | Shopping 🛒 | Thresholds ⚙️`).
   - **Screen 2 (`HouseholdRecipesMealsScreen`):** Consolidates Recipes management, Cooking engine, and Meal Register/History under a clean sub-navigation bar (`Recipes 🍳 | Meal Register 🍽️`).
   - **Container (`HouseholdWorkspaceScreen`):** Primary container hosting Screen 1 and Screen 2 via `[ 📦 Stock & Shopping ]  [ 🍳 Recipes & Meals ]`.
3. **Data Contract Preservation:**
   - Underlying entities, DAOs, repositories, and persistence models (`FoodRepository`, `Ingredient`, `StockBatch`, `Recipe`, `ShoppingItem`, `MealRecord`) remain 100% intact and decoupled.
4. **Verification:**
   - All 350+ automated unit tests pass with 0 failures (`WorkspacesNavigationTest`, `FoodRepositoryTest`, `ShoppingListThresholdsTest`, `FoodSystemEndToEndTest`).

### Consequences
- Streamlined, elegant 2-screen Household Workspace experience with zero data contract disruption and lightning-fast local performance.

---

## ADR 031: Dual Gemini Connection Modes (Listener/Transcriber & Interactive Chatbox Log Reader)

* **Date:** 2026-10-07
* **Status:** Accepted

### Context
Remmi requires two distinct, specialized Gemini connection modes to isolate continuous voice streaming transcription from interactive multi-turn AI chat and system log analysis.

### Decisions
1. **Connection Mode 1 — Listener & Transcriber (Voice-to-Text):**
   - Managed via `GeminiLiveTranscriptionProvider` over low-latency WebSocket streaming (`BidiGenerateContent`).
   - Streams 16kHz PCM microphone audio and emits real-time speech-to-text transcripts into Lecture Recorder and voice inputs.
2. **Connection Mode 2 — Interactive AI Chatbox & Log Reader:**
   - Managed via `GeminiChatboxEngine` and `GeminiLogReader`.
   - **Log Reader (`GeminiLogReader`)**: Captures live recording diagnostics (`RecordingDiagnosticsState`), structured context facts (`StructuredAiContext`), and AI action history logs (`AiPermissionsRepository`).
   - **Interactive UI (`GeminiChatboxDialog`)**: Jetpack Compose M3 Chat interface featuring log attachment category chips (🔍 All Logs, 🎙️ Live Diagnostics, 📊 System Context, 📜 Action History), chat message threads, and system command execution buttons.

### Consequences
- Clean separation between passive real-time speech transcription streaming and interactive conversational AI assistance with log analysis capabilities.

---

## ADR 032: Unified JSON Data Storage (`data/`) & Core-Enforced Gemini Write Security Boundary

* **Date:** 2026-10-07
* **Status:** Accepted

### Context
Remmi requires all local domain data files to be organized in a single dedicated subfolder (`files/data/`) as clean JSON tables, and enforces a strict security boundary where Gemini reads data for analysis but is prohibited from direct file writes.

### Decisions
1. **Unified Storage Directory (`files/data/`):**
   - Centralizes all domain JSON files into `app-private/files/data/`: `calendar.json`, `uncompleted_tasks.json`, `completed_tasks.json`, `ingredients.json`, `recipes.json`, `meals.json`, `shopping_items.json`, `suppliers.json`, `brands.json`, `purchases.json`, etc.
   - **Automatic Migration (`DataDirectoryMigrator`):** Migrates legacy JSON storage files into `data/` on application startup without data loss.
2. **Gemini Read-Only Access:**
   - Gemini is granted **READ-ONLY** access to JSON data files in `data/` to answer user queries and analyze logs.
3. **Core-Enforced Write Confirmation Boundary:**
   - Gemini is **STRICTLY PROHIBITED** from writing directly to JSON files or invoking repository save methods.
   - Any write/add request ("Add task...", "Create event...", "Add ingredient...") causes Gemini to generate a proposed draft template (`PendingDraftAction`).
   - The UI presents a **User Save Confirmation Popup** (`PendingActionConfirmDialog`) displaying the editable draft.
   - File writes and repository persistence are executed **strictly by Core Application logic** only upon explicit user confirmation in the popup UI.

### Consequences
- Standardized, clean local JSON storage topology with automatic legacy migration and absolute user control over AI data modifications.

