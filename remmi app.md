# REMMI APPLICATION ARCHITECTURE & SYSTEM DIRECTORY REFERENCE
**Document Version:** 1.0  
**Target Audience:** AI Coding Agents & System Engineers  
**Context Year:** 2026

This document is the authoritative technical reference for the **Remmi** Android Application. It contains a complete map of all project files, user screens, functional capabilities, data flows, and architectural constraints necessary for an AI agent to understand, navigate, extend, and optimize the codebase.

---

## 1. EXECUTIVE APPLICATION PROFILE & CORE PHILOSOPHY

* **Application Name**: Remmi (Package `com.remmi`)
* **Type**: Personal Workspace, Custom Home Screen Launcher, and AI Assistant Platform for Android.
* **Core Philosophy & Architectural Mandates**:
  1. **Strictly Local-First & Database-Free (ADR 006)**:
     - **NO SQLite, NO Room, NO DAOs, NO ORMs, NO DB repositories.**
     - All persistence utilizes local-first JSON file storage in app-private device storage (`context.filesDir`) executed on `Dispatchers.IO` using atomic temporary-file swapping (`.tmp` -> target file).
  2. **Zero Custom Server Backend**:
     - No cloud databases or intermediate servers. External integrations connect directly client-to-service (Google Calendar/Tasks/Auth APIs, Gemini LLM API via OkHttp).
  3. **Plugin-Based Workspace Architecture**:
     - Core infrastructure (`core/`) remains lightweight. Domain features are isolated inside independent feature plugins (`plugins/planning`, `plugins/food`, `plugins/health`, `plugins/knowledge`).
  4. **Command & EventBus Architecture (ADR 002, ADR 017)**:
     - UI dispatches typed `RemmiCommand` instances to `RemmiController`.
     - Independent modules communicate via `RemmiEventBus` (`MutableSharedFlow` with atomic dropped-count metrics).
  5. **100% Jetpack Compose + Material 3 UI**:
     - Declarative Compose UI, dynamic accent themes, custom 3-dot page indicators, 3-panel horizontal launcher pager, and drag-and-drop favoriting.

---

## 2. PROJECT DIRECTORY MAP & FILE LOCATIONS

### 2.1 Core Infrastructure (`app/src/main/java/com/remmi/core/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `core/RemmiApplication.kt` | Fundamental `Application` subclass. Instantiates `RemmiHost` & `RemmiController`, registers default plugins, and triggers non-blocking background warming. |
| `core/host/RemmiHost.kt` | Core dependency container. Holds `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, `RemmiEventBus`, `RemmiPluginRegistry`, and system capabilities. |
| `core/controller/RemmiController.kt` | Central command router. Executes `RemmiCommand` intents, routes them to owning capabilities/repositories, and emits to `RemmiEventBus`. |
| `core/eventBus/RemmiEventBus.kt` | Coroutine-native `SharedFlow` event bus separating intent (`RemmiCommand`) from notification (`RemmiEvent`). Tracks dropped message metrics. |
| `core/eventBus/commands/RemmiCommand.kt` | Base interface for system commands ("something should happen"). |
| `core/eventBus/commands/RemmiCommandResult.kt` | Sealed result interface (`Success` / `Failure`). |
| `core/eventBus/commands/LaunchAppCommand.kt` | Command representing explicit app launch request. |
| `core/eventBus/commands/NavigateCommand.kt` | Command representing navigation to a target screen/workspace. |
| `core/eventBus/events/RemmiEvent.kt` | Base interface for system events ("something happened"). |
| `core/plugin/RemmiPluginRegistry.kt` | Thread-safe plugin manager providing failure-isolated `initializeAll` and non-blocking `initializeAllAsync`. |

### 2.2 Core AI Subsystem (`app/src/main/java/com/remmi/core/ai/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `core/ai/RemmiAiCapability.kt` | Core contract interface for intent resolution and input processing. |
| `core/ai/DefaultAiCapability.kt` | Intent resolution parser matching natural language inputs to commands (`NavigateCommand`, `LaunchAppCommand`, `CreateTaskCommand`, `CreateAlarmCommand`). |
| `core/ai/RemmiAiCommandValidator.kt` | Safety validator checking command bounds before execution. |
| `core/ai/agent/PersonalAiAgentOrchestrator.kt` | Master AI orchestrator unifying context, tool calls, briefings, next best actions, and proactive alerts. Answers the 4 Home Questions. |
| `core/ai/briefing/DailyAiBriefingEngine.kt` | Generates structured daily briefings ("What matters today?"). |
| `core/ai/action/NextBestActionEngine.kt` | Calculates time-sensitive recommended next actions. |
| `core/ai/proactive/ProactiveIntelligenceEngine.kt` | Evaluates proactive alerts and health/planning warnings. |
| `core/ai/patterns/PersonalPatternAnalysisEngine.kt` | Identifies historical behavioral and productivity patterns. |
| `core/ai/context/GeminiContextBuilder.kt` | Assembles domain-specific structured context for Gemini LLM calls. |
| `core/ai/tools/RemmiAiToolRegistry.kt` | Registry executing structured tool calls across calendar, tasks, health, and apps. |
| `core/ai/permissions/repository/AiPermissionsRepository.kt` | Manages AI tool permissions, access levels, and audit history. |

### 2.3 Android Platform Capabilities (`app/src/main/java/com/remmi/core/android/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `core/android/launcher/RemmiLauncherCapability.kt` | Core contract for querying and launching installed applications. |
| `core/android/launcher/AndroidLauncherCapability.kt` | Platform implementation using `PackageManager`, 2-stage instant discovery, `ConcurrentHashMap` icon/category caching, and broadcast receivers. |
| `core/android/launcher/RemmiAppInfo.kt` | Primitive model representing an installed app (`packageName`, `activityName`, `label`, `icon`, `category`). |
| `core/android/audio/RemmiAudioCapability.kt` | Core contract for microphone PCM audio capture. |
| `core/android/audio/AndroidAudioCapability.kt` | `AudioRecord` platform implementation capturing 16kHz 16-bit PCM mono audio on `Dispatchers.IO` with RMS amplitude calculation. |
| `core/android/alarm/RemmiAlarmCapability.kt` | Core contract for system alarm scheduling. |
| `core/android/alarm/DefaultAlarmCapability.kt` | `AlarmManager` platform implementation with Android 12+ exact alarm fallback safety. |
| `core/android/alarm/AlarmBootReceiver.kt` | `BroadcastReceiver` listening for `ACTION_BOOT_COMPLETED` to reschedule active alarms. |
| `core/android/notification/RemmiNotificationCapability.kt` | Core contract for posting system notifications. |
| `core/android/notification/DefaultNotificationCapability.kt` | Platform implementation wrapping Android `NotificationManager`. |
| `core/android/systemInfo/RemmiSystemInfoCapability.kt` | Contract for querying OS version and device model details. |
| `core/android/systemInfo/AndroidSystemInfoCapability.kt` | Platform implementation reading `Build.VERSION` and `Build.MODEL`. |
| `core/android/google/auth/RemmiGoogleAuthCapability.kt` | Contract for Google Account sign-in and credentials. |
| `core/android/google/calendar/RemmiGoogleCalendarCapability.kt` | Contract for Google Calendar sync with local cached state. |

### 2.4 Personal Context & Intelligence (`app/src/main/java/com/remmi/core/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `core/context/repository/PersonalContextRepository.kt` | Local-first repository managing `personal_context.json` (Profile, Habits, Life Context, Goals, Preferences, AI Memories). |
| `core/context/dailystate/DailyStateRepository.kt` | Manages daily subjective state tracking and score history. |
| `core/intelligence/CrossWorkspaceIntelligenceEngine.kt` | Deterministic multi-domain engine evaluating snapshots across Planning, Health, and Knowledge. |
| `core/intelligence/worker/CrossWorkspaceAutomationScheduler.kt` | Background coroutine runner evaluating recommendations on `Dispatchers.IO`. |
| `core/intelligence/worker/CrossWorkspaceEvaluationWorker.kt` | WorkManager delegate worker for background intelligence runs. |

### 2.5 Feature Plugins (`app/src/main/java/com/remmi/plugins/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `plugins/RemmiPlugin.kt` | Base interface contract for independent Remmi feature plugins. |
| `plugins/planning/PlanningPlugin.kt` | Plugin owner for Planning Workspace (Calendar, Tasks, Alarms, Goals). |
| `plugins/planning/calendar/repository/CalendarEventRepository.kt` | Repository for `planning_calendar_events.json` & `planning_calendar_groups.json`. |
| `plugins/planning/tasks/repository/TaskRepository.kt` | Repository for `planning_tasks.json`. |
| `plugins/planning/alarms/repository/AlarmRepository.kt` | Repository for `planning_alarms.json`. |
| `plugins/food/FoodPlugin.kt` | Plugin owner for Household Food & Inventory Workspace. |
| `plugins/food/repository/FoodRepository.kt` | Comprehensive repository managing 13 food JSON files (Ingredients, Stock Batches, Transactions, Recipes, Cooking Events, Meals, Thresholds, Shops, Shopping Items, Purchases). |
| `plugins/health/HealthPlugin.kt` | Plugin owner for Health & Wellbeing Workspace. |
| `plugins/health/repository/HealthRepository.kt` | Repository managing 9 health JSON files (Observations, Sleep, Activity, Food, Habits, Experiments, Digital usage, Tests). |
| `plugins/health/models/DimensionScoringEngine.kt` | Calculation engine for 8 Health Dimensions, Sleep scores, and Learning Readiness. |
| `plugins/knowledge/KnowledgePlugin.kt` | Plugin owner for Knowledge & Academic Workspace. |
| `plugins/knowledge/academic/repository/AcademicRepository.kt` | Repository for courses, exams, assignments, and study requirements. |
| `plugins/knowledge/lecturerecorder/repository/LectureRepository.kt` | Repository for lectures, transcripts, and audio files. |
| `plugins/knowledge/lecturerecorder/recovery/LectureRecoveryManager.kt` | Atomic checkpointing manager (`checkpoint_lecture.json`) for live recording crash recovery. |

### 2.6 User Interface & Compose Screens (`app/src/main/java/com/remmi/ui/`)

| File Path | Primary Function & Responsibilities |
| :--- | :--- |
| `ui/MainActivity.kt` | Single `ComponentActivity` launcher container. Configures `enableEdgeToEdge()`, handles `onBackPressedDispatcher`, and receives `onNewIntent`. |
| `ui/RemmiApp.kt` | Root composable applying `RemmiTheme` (app theme & accent preferences) and hosting `HomeScreen`. |
| `ui/home/HomeScreen.kt` | **LEVEL 1 UI**: 3-panel `HorizontalPager` (`WORKSPACES` [0], `REMMI_HOME` [1], `APPS` [2]) with top centered 3-dot page indicator and declarative `BackHandler`. |
| `ui/home/RemmiHomeScreen.kt` | **CENTER HOME SURFACE**: 3-element minimalist layout (Top Quick Search Bar + AI Voice Access, Center Schedule / Today's Tasks Widget, Bottom 15-Slot Favorites Dock). |
| `ui/home/WorkspacesScreen.kt` | **WORKSPACES SURFACE**: Full-screen vertical pager across 6 domains (Planning, Knowledge, Health, Travel, Household, People) with left floating dot selector. |
| `ui/home/AppsScreen.kt` | **ALL-APPS SURFACEE**: 4-column vertical grid with top category filter chips, right A-Z alphabet index bar, long-press options, and custom category management. |
| `ui/search/GlobalSearch.kt` | Fast zero-allocation search engine (`filterGlobalSearch`) ranking apps by EXACT -> PREFIX -> PARTIAL matches in < 1ms. |
| `ui/accounts/AccountsScreen.kt` | Google Account sign-in, account switching, and sync management UI. |
| `ui/settings/SettingsScreen.kt` | Application preference settings, theme/accent color pickers, Gemini API key configuration, and AI permission audit section (`AiPermissionsSection.kt`). |
| `ui/theme/RemmiPreferences.kt` | `SharedPreferences`/`DataStore` wrapper managing UI theme, accent color, favorite grid slots, Gemini API key, and custom app categories. |
| `ui/theme/Theme.kt` | `RemmiTheme` Material 3 color schemes, shapes, and custom `remmiColors` palette composition local. |
| `plugins/food/ui/HouseholdWorkspaceScreen.kt` | Primary Household container with bottom bar switcher between `Stock & Shopping` and `Recipes & Meals`. |
| `plugins/food/ui/HouseholdStockShoppingScreen.kt` | Screen 1: Consolidates Stock list, Shopping lists, and Threshold configuration. |
| `plugins/food/ui/HouseholdRecipesMealsScreen.kt` | Screen 2: Consolidates Recipe browser, Cooking engine, and Meal Register/History. |
| `plugins/food/ui/RegisterStockItemScreen.kt` | Dedicated full-screen destination for registering new stock items/ingredients with preloading and two-way linked expiry calculation. |
| `plugins/health/ui/HealthWorkspaceScreen.kt` | Health workspace container hosting Today overview, Octagonal 8-Dimension Radar Graph, Tools, and Assessment Schedule. |
| `plugins/health/ui/HealthProfileRadar.kt` | Optimized 8-sided octagonal radar chart with memoized trigonometric lookup tables (`cosAngles`/`sinAngles`). |
| `plugins/health/ui/TestsScreen.kt` | Time-based daily assessment schedule and randomized wizard dialog. |
| `plugins/health/ui/DigitalToolScreen.kt` | Digital wellbeing screen tracking screen time, late-night usage, and pickup trends without querying `PackageManager`. |

---

## 3. USER SCREENS & VISUAL NAVIGATION MAP

```text
                                 [MainActivity]
                                       │
                                   [RemmiApp]
                                       │
                                  [HomeScreen]
                                       │
     ┌─────────────────────────────────┼─────────────────────────────────┐
     ▼                                 ▼                                 ▼
[Page 0: WORKSPACES]             [Page 1: REMMI HOME]              [Page 2: APPS]
WorkspacesScreen.kt              RemmiHomeScreen.kt                AppsScreen.kt
  │                                    │                                 │
  ├─► Planning Workspace               ├─► Top Quick Search Bar          ├─► Top Category Chips
  │   (Calendar, Tasks, Alarms)        │   + AI Voice Assistant          │   ("All", "Messaging", etc.)
  │                                    │                                 │
  ├─► Knowledge Workspace              ├─► Center Home Widget            ├─► 4-Column App Grid
  │   (Lectures, Notes, Flashcards)    │   (Schedule | Today's Tasks)    │   (Stable composite keys)
  │                                    │                                 │
  ├─► Health Workspace                 └─► Bottom Favorites Dock         ├─► Right A-Z Index Bar
  │   (8-Dim Radar, Tests, Digital)        (15-slot drag dock)           │
  │                                                                      ├─► Long-Press Options
  └─► Household Workspace                                                │   (Favorite / Uninstall)
      (Stock & Shopping, Recipes & Meals)                                │
                                                                         └─► Manage Categories
```

---

## 4. PERSISTENCE ENGINE & ATOMIC STORAGE MAP

All repositories store data in app-private files (`context.filesDir`) on `Dispatchers.IO` using the **Atomic Temporary-File Swapping Pattern** (`atomicWrite` writes to `filename.json.tmp` first, then atomically replaces `filename.json`).

| Subsystem / Domain | Target JSON File Name | Owning Repository Class |
| :--- | :--- | :--- |
| **Personal Context** | `personal_context.json` | `PersonalContextRepository` |
| **Calendar Events** | `planning_calendar_events.json` | `CalendarEventRepository` |
| **Calendar Groups** | `planning_calendar_groups.json` | `CalendarEventRepository` |
| **Tasks** | `planning_tasks.json` | `TaskRepository` |
| **Alarms** | `planning_alarms.json` | `AlarmRepository` |
| **Food Ingredients** | `food_ingredients.json` | `FoodRepository` |
| **Stock Batches** | `food_stock_batches.json` | `FoodRepository` |
| **Stock Transactions** | `food_stock_transactions.json` | `FoodRepository` |
| **Recipes** | `food_recipes.json` | `FoodRepository` |
| **Cooking Events** | `food_cooking_events.json` | `FoodRepository` |
| **Meals History** | `food_meals.json` | `FoodRepository` |
| **Stock Thresholds** | `food_thresholds.json` | `FoodRepository` |
| **Shops & Prices** | `food_shops.json`, `food_shop_prices.json` | `FoodRepository` |
| **Shopping Items** | `food_shopping_items.json` | `FoodRepository` |
| **Purchases** | `food_purchases.json` | `FoodRepository` |
| **Health Observations** | `health_observations.json` | `HealthRepository` |
| **Sleep Logs** | `health_sleep.json` | `HealthRepository` |
| **Activity Logs** | `health_activity.json` | `HealthRepository` |
| **Food & Hydration Logs**| `health_food.json` | `HealthRepository` |
| **Health Habits** | `health_habits.json` | `HealthRepository` |
| **Health Experiments** | `health_experiments.json` | `HealthRepository` |
| **Digital Usage Logs** | `health_digital.json` | `HealthRepository` |
| **Daily Test Responses** | `health_tests.json` | `HealthRepository` |
| **Lecture Checkpoint** | `checkpoint_lecture.json` | `LectureRecoveryManager` |
| **Academic Courses** | `academic_courses.json` | `AcademicRepository` |

---

## 5. PERFORMANCE BENCHMARKS & SLAs

| Benchmark Journey | SLA Target | Measured Latency |
| :--- | :--- | :--- |
| **Host & Controller Cold Startup** | `< 50 ms` | **< 2.0 ms** |
| **Package Discovery & Label Sorting (1000 apps)** | `< 25 ms` | **< 3.0 ms** |
| **Global Search Matching & Ranking (1000 apps)** | `< 20 ms` | **< 1.0 ms** |
| **Favorites Slot Resolution (1000 apps)** | `< 10 ms` | **< 0.1 ms** ($O(1)$ lookup) |
| **JSON Storage Read Latency** | `< 100 ms` | **< 8.0 ms** |
| **Radar Graph Frame Rendering** | `< 16 ms` | **< 0.5 ms** |
| **Cross-Workspace Intelligence Evaluation** | `< 10 ms` | **< 0.5 ms** |
| **Full Unit & Integration Test Suite** | `100% Pass` | **361 / 361 Passed** |

---

## 6. RULES FOR AI CODING AGENTS

1. **NEVER INTRODUCE A DATABASE**:
   - Strictly NO Room, SQLite, DAOs, or ORMs. Preserve local-first JSON file storage on `Dispatchers.IO`.
2. **NEVER MODIFY CONTRACTS WITHOUT AUTHORIZATION (RULE 1)**:
   - Do not change existing public interfaces or core contracts (`RemmiHost`, `RemmiController`, `RemmiEventBus`, `RemmiPluginRegistry`, `RemmiLauncherCapability`, `RemmiAudioCapability`, `RemmiAlarmCapability`, `RemmiSystemInfoCapability`).
3. **PRESERVE ATOMIC FILE persistence**:
   - Always use temporary-file swapping (`.tmp` -> target file) when writing JSON files to prevent storage corruption on process kills.
4. **USE BUILT-IN IDE TOOLS**:
   - Use built-in file tools (`read_file`, `write_file`, `replace_file_content`, `find_files`, `grep`) instead of shell scripts or `sed`/`awk`.
5. **KEEP UI SEPARATE FROM INFRASTRUCTURE**:
   - UI code lives in `ui/` or `plugins/*/ui/`. Core infrastructure lives in `core/`.
