# Remmi AI System Architecture Audit & Master Roadmap Integration

This document provides a comprehensive architecture audit of the RemmiHome application and establishes the integration strategy for the **Personal AI Life Management System** (Master Implementation Roadmap).

---

## 1. System Architecture Overview

RemmiHome is built as a lightweight, clean, local-first Android application adhering to modern Android engineering principles:
* **Architecture:** Layered, plugin-driven modular architecture (`core/`, `ui/`, `plugins/`).
* **UI Toolkit:** Jetpack Compose with Material 3 design system.
* **Concurrency:** Kotlin Coroutines & Flow (structured concurrency, zero thread-blocking on UI).
* **Communication Primitive:** EventBus supporting type-safe `RemmiCommand` (intent) and `RemmiEvent` (notification).
* **Persistence:** Local-first storage (app-private JSON files on `Dispatchers.IO`, `RemmiPreferences` SharedPreferences/DataStore). Strictly **NO relational database** (no Room, SQLite, or DAOs).
* **Remote Integration:** Direct client-to-service integration (Google Services, Gemini API) as external sources of truth. Strictly **NO custom Remmi server backend**.

---

## 2. Comprehensive Subsystem Audit

### 2.1 Project Structure & Modules
* **Gradle Modules:** Single top-level subproject (`:app`).
* **Package Topology (`com.remmi`):**
  - `core/`: Fundamental infrastructure (`RemmiApplication`, `RemmiHost`, `RemmiController`, `RemmiEventBus`, `RemmiPluginRegistry`, `core/android/` capabilities, `core/intelligence/`).
  - `ui/`: Core launcher shell UI (`MainActivity`, `RemmiApp`, `HomeScreen`, `AppsScreen`, `SettingsScreen`, `Theme`).
  - `plugins/`: Isolated feature modules (`planning`, `knowledge`, `health`).

### 2.2 Navigation System
* **Primary Navigation (`ui/home/HomeScreen.kt`):** Compose `HorizontalPager` managing three top-level destinations (`WORKSPACES`, `HOME`, `APPS`).
* **Workspace Navigation:** Infinite circular vertical carousel for workspace plugin switching (`Planning`, `Knowledge`, `Health`).
* **Search / AI Navigation:** Direct natural language command-driven navigation via `NavigateCommand` intercepted by `RemmiController` and `RemmiEventBus`.

### 2.3 Storage & Persistence Architecture
* **Strategy:** Local-First Storage.
* **JSON File Storage:** `PlanningTaskRepository`, `ClassRepository`, `LectureRepository`, `HealthRepository` store structured domain models in app-private JSON files off the main thread (`Dispatchers.IO`).
* **Preferences & State:** `RemmiPreferences` handles simple key-value pairs (favorites, UI state).
* **Absence of Relational DB:** Strictly no Room, SQLite, or DAOs per ADR 006.

### 2.4 Calendar & Schedule Subsystem
* **Local Planning Calendar (`plugins/planning/calendar/`):** `CalendarEventRepository`, `CalendarEvent` models, and `RecurrenceEngine`.
* **Google Calendar Integration (`core/android/google/calendar/`):** `RemmiGoogleCalendarCapability` & `DefaultGoogleCalendarCapability` fetching today's events directly from Google Calendar, with transient local caching for instant boot.

### 2.5 Tasks & Action Items Subsystem
* **Planning Tasks (`plugins/planning/tasks/`):** `TaskRepository` & `TaskItem` managing local actionable tasks.
* **Planning Workspace Tasks (`plugins/planning/repository/`):** `PlanningTaskRepository` managing workspace tasks with status filtering and JSON persistence.

### 2.6 Alarms Subsystem
* **Local Alarm Management (`plugins/planning/alarms/`):** `AlarmRepository` & `AlarmItem`.
* **Platform Alarm Capability (`core/android/alarm/`):** `RemmiAlarmCapability` & `AlarmBootReceiver` encapsulating Android `AlarmManager` for exact/inexact alarm scheduling and device reboot handling.

### 2.7 Contacts Subsystem
* **Status:** Prepared capability slot under `core/android/google/contacts/` or Android Contacts API capability boundary. Currently handled via direct Google API integration points.

### 2.8 Health & Wellbeing Subsystem
* **Plugin (`plugins/health/`):** Fully implemented Health Workspace plugin (`HealthPlugin`).
* **Core Models (`plugins/health/models/`):** 8 Health Dimensions (`HealthDimension8`), `DimensionScoringEngine`, `DimensionQuestionsCatalog`, `HealthContextSnapshot`, `SleepLog`, `ActivityLog`, `FoodLog`, `Habit`.
* **AI Evaluation:** `HealthAiEvaluator` providing local assessment, trend analysis, and recommendations.
* **UI Tools:** Octagonal profile radar (`HealthProfileRadar`), `HealthTodayScreen`, `SubjectiveLoggingScreen`, and dimension details.

### 2.9 Home Screen & App-Management Subsystem
* **Android Home Intent Filter:** `MainActivity` configured with `android.intent.category.HOME` and `QUERY_ALL_PACKAGES`.
* **Launcher Capability (`core/android/launcher/`):** `AndroidLauncherCapability` performing off-thread package querying (`Dispatchers.IO`), returning `RemmiAppInfo`.
* **Launcher UI (`ui/home/`):** `HomeScreen` and `RemmiHomeScreen` providing 2x3 Favorite Apps grid, global app search, category chips, and recent apps tracking.
* **Execution Boundary:** App launches execute via `LaunchAppCommand` processed by `RemmiController`.

### 2.10 Background Services & Automation
* **WorkManager Integration (`core/intelligence/worker/`):** `CrossWorkspaceEvaluationWorker` and `CrossWorkspaceAutomationScheduler` running periodic background evaluation.
* **Broadcast Receivers:** `AlarmBootReceiver` re-registering scheduled alarms on system startup.

### 2.11 Permissions Subsystem
* **Centralized Settings (`ui/settings/SettingsScreen.kt`):** Centralized permission management.
* **Capability Ownership:** Permission knowledge contained strictly inside owning capability (`RemmiAudioCapability`, `RemmiAlarmCapability`, `RemmiNotificationCapability`).

### 2.12 Networking & Gemini Integration
* **Gemini Capabilities (`core/ai/`):** `RemmiAiCapability`, `DefaultAiCapability`, `RemmiAiCommandValidator`.
* **Gemini Live Streaming (`plugins/knowledge/lecturerecorder/transcription/`):** `GeminiLiveTranscriptionProvider` using Ktor WebSockets for real-time PCM audio streaming and interim transcript receiving.
* **API Key Safety:** Credentials provided securely via local configuration/environment, never hardcoded.

### 2.13 Cross-Workspace Intelligence Engine
* **Engine (`core/intelligence/`):** `CrossWorkspaceIntelligenceEngine` correlating signals across Health, Planning, and Knowledge workspaces to generate `CrossWorkspaceRecommendation` items.

### 2.14 Test Infrastructure
* **Unit Tests (`app/src/test/`):** 208 passing automated tests covering Core, EventBus, Capabilities, Controller, Plugins, Health, Knowledge, Planning, Search, Security, and Performance.
* **UI Tests (`app/src/androidTest/`):** Android Compose UI tests (`HomeScreenTest`).
* **Benchmarks:** Microbenchmarks in `HomePerformanceTest`, `HealthAndWorkspacePerformanceTest`, and `KnowledgePerformanceTest`.

---

## 3. Reusable Components & Capabilities Inventory

| Subsystem | Reusable Interface / Contract | File Location | Purpose |
| :--- | :--- | :--- | :--- |
| **System EventBus** | `RemmiEventBus` | `core/eventBus/RemmiEventBus.kt` | Decoupled coroutine communication |
| **Command Architecture** | `RemmiCommand`, `RemmiCommandResult` | `core/eventBus/commands/` | Action execution intents |
| **Host Infrastructure** | `RemmiHost` | `core/host/RemmiHost.kt` | Host scope & capability container |
| **Controller Router** | `RemmiController` | `core/controller/RemmiController.kt` | System command coordinator |
| **Plugin System** | `RemmiPlugin`, `RemmiPluginContext` | `plugins/` | Feature module isolation boundary |
| **Launcher Capability** | `RemmiLauncherCapability` | `core/android/launcher/` | On-device package discovery & launch |
| **AI Capability** | `RemmiAiCapability` | `core/ai/` | Natural language intent resolution |
| **Google Auth & Services** | `RemmiGoogleServiceCapability` | `core/android/google/` | Direct Google Services client |
| **Alarm Capability** | `RemmiAlarmCapability` | `core/android/alarm/` | Exact/inexact Android AlarmManager |
| **Audio Capability** | `RemmiAudioCapability` | `core/android/audio/` | Microphone PCM audio capture |
| **Health Repository** | `HealthRepository` | `plugins/health/repository/` | Local JSON health & daily state persistence |
| **Planning Repositories** | `PlanningTaskRepository`, `TaskRepository`, `CalendarEventRepository`, `AlarmRepository` | `plugins/planning/` | Local JSON task, calendar, alarm persistence |
| **Knowledge Repositories**| `ClassRepository`, `LectureRepository` | `plugins/knowledge/` | Local JSON class & lecture persistence |

---

## 4. Dependencies & Version Catalog

Defined in `gradle/libs.versions.toml`:
* **Kotlin:** 2.0.21
* **Compose UI & Material 3:** `androidx.compose.ui`, `androidx.compose.material3` (Expressive enabled)
* **Coroutines:** `kotlinx-coroutines-android`, `kotlinx-coroutines-core`
* **Serialization:** `kotlinx-serialization-json` (1.7.3)
* **Ktor (Networking/WebSockets):** `ktor-client-core`, `ktor-client-cio`, `ktor-client-websockets` (2.3.12)
* **WorkManager:** `androidx.work:work-runtime-ktx`
* **DataStore / SharedPreferences:** `androidx.datastore:datastore-preferences`
* **HealthConnect:** `androidx.health.connect:connect-client`

---

## 5. Architectural Risks & Mitigation Strategies

1. **Risk: Local Storage Scalability & IO Concurrency**
   * *Mitigation:* Ensure all JSON serialization/deserialization runs strictly on `Dispatchers.IO` using mutex locks or atomic file swaps to prevent state corruption during concurrent background WorkManager writes.
2. **Risk: AI Hallucination & Execution Safety**
   * *Mitigation:* Strictly validate every AI-resolved command using `RemmiAiCommandValidator`. Reject unmapped or high-risk operations without execution.
3. **Risk: API Key Exposure & Network Reliability**
   * *Mitigation:* Zero embedded secrets in source code. Isolate network provider calls so network outages drop connectivity state gracefully without crashing local UI or corrupting local state.
4. **Risk: Main Thread Jank During Workspace Switching**
   * *Mitigation:* Keep Compose screens lazy-loaded and memoized using `remember`. Defer non-critical calculations off the UI thread.

---

## 6. Proposed Integration Architecture for Roadmap Phases

The 14-phase Personal AI Life Management System roadmap integrates seamlessly into Remmi's existing modular architecture:

```text
                               ┌──────────────────────────────────────────┐
                               │            AI HOME SCREEN (Phase 8)      │
                               │  Primary Conversational AI Interface    │
                               └────────────────────┬─────────────────────┘
                                                    │
                               ┌────────────────────▼─────────────────────┐
                               │        GEMINI CONTEXT ENGINE (Phase 6)   │
                               │  Dynamic Selective Context Builder       │
                               └────────────────────┬─────────────────────┘
                                                    │
         ┌──────────────────────────────────────────┼──────────────────────────────────────────┐
         │                                          │                                          │
┌────────▼───────────────────┐             ┌────────▼───────────────────┐             ┌────────▼───────────────────┐
│ PERSONAL CONTEXT (Phase 2) │             │  DAILY STATE (Phase 3)    │             │   HEALTH & WELLBEING      │
│ Profile, Habits, Goals,    │             │  Sleep, Energy, Focus,    │             │   8 Dimensions (Phase 4)  │
│ Preferences, AI Memory     │             │  Stress, Mood, Nutrition  │             │   Assessment & Scoring    │
└────────────────────────────┘             └────────────────────────────┘             └────────────────────────────┘
         │                                          │                                          │
┌────────▼───────────────────┐             ┌────────▼───────────────────┐             ┌────────▼───────────────────┐
│ ACADEMIC SYSTEM (Phase 5)  │             │   CONTROLLED AI TOOLS      │             │  PROACTIVE INTELLIGENCE    │
│ Degree, Courses, Exams,    │             │   Calendar, Tasks, Health, │             │  Warnings, Daily Briefings,│
│ Deadlines, Preparation     │             │   Habits, App Tools (Phase 7)│             │  Next Best Action (Ph 9-12)│
└────────────────────────────┘             └────────────────────────────┘             └────────────────────────────┘
```

### Mapping Matrix:
* **Phase 2 (Personal Context Data Model):** Extend `core/intelligence/` and local-first JSON persistence (`personal_context.json`) with strict category distinction (`FACT`, `PREFERENCE`, `GOAL`, `CURRENT_STATE`, `OBSERVATION`, `RECOMMENDATION`).
* **Phase 3 (Daily State System):** Build upon `plugins/health/models/` and `HealthRepository` for daily timestamped state tracking.
* **Phase 4 (Health & Wellbeing 8 Dimensions):** Re-verify and enhance existing `HealthDimension8` scoring and octagonal UI graph in `plugins/health/`.
* **Phase 5 (Academic System):** Extend `plugins/knowledge/` with structured Master's degree models (Courses, Exams, Deadlines, Preparation Status) integrating with `Planning` calendar/tasks.
* **Phase 6 (Gemini Context Engine):** Implement context selector layer in `core/ai/` returning structured domain slices without sending raw full storage dumps.
* **Phase 7 (AI Tool System):** Map tool requests onto typed `RemmiCommand` instances with READ vs WRITE segregation and confirmation safeguards.
* **Phase 8 (AI Home Screen):** Evolve `HomeScreen.kt` to make AI Agent the primary interface while preserving Favorite Apps, Calendar, and Search.
* **Phases 9–14 (Briefings, Next Best Action, Proactive Intelligence, Pattern Analysis, Security, Final Integration):** Layer intelligence services over `core/intelligence/` and `core/ai/` using typed warnings and permission confirmation models.
