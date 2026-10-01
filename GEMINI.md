# Remmi Engineering Context & Rules

These foundational principles apply to every task in Remmi:

* **Inspect existing code before changing it.** Never assume structure or behavior.
* **Reuse existing systems.** Do not duplicate concepts, wrappers, or infrastructure.
* **Make the smallest correct change.** Avoid unnecessary edits or broad scope creep.
* **Do not invent architecture.** Build only what is needed right now; prefer standard Kotlin/Android primitives over layers of abstractions.
* **Do not perform unrelated refactoring.** Focus strictly on the task at hand.
* **Preserve working behavior.** Ensure changes do not break established functionality.
* **Do not add dependencies without justification.** Keep external library usage minimal and justified.
* **Keep Core small.** `core/` contains only fundamental application infrastructure.
* **Keep UI separate from infrastructure.** `ui/` contains Compose views and UI logic; it does not contain system infrastructure.
* **Keep plugin functionality inside plugins.** `plugins/` isolates distinct feature modules.
* **Compile and test changes.** Always verify build status and test results before concluding work.
* **Never claim verification that was not actually performed.** Be explicit about what was run and what passed.
* **Stop and ask/review when an architectural decision is genuinely unclear.** Clarify ambiguity before proceeding.

---

# Permanent Architecture Rules

These rules are permanent and take precedence over convenience or speculative refactoring:

## RULE 1 — AI CODER MUST NEVER MODIFY CONTRACTS
The AI coder must **NEVER modify an existing Remmi contract** unless the user explicitly authorizes that specific contract change.
Contracts include `GEMINI.md`, architectural rules, public interfaces, Core/plugin contracts, EventBus contracts, Android capability contracts, testing contracts, UI design contracts, and API boundaries.

If an implementation task appears to require changing an existing contract:
**STOP.** Report `CONTRACT CONFLICT` detailing the contract, reason, and required decision, and wait for explicit authorization.

## RULE 2 — SELF-CONTAINED DIRECTORY OWNERSHIP
Every system/class/concept should be organized so that its implementation resources live inside the directory that owns that concept. The filesystem should make ownership obvious.
* Do NOT create generic dumping-ground directories (`models/`, `utils/`, `helpers/`, `common/`, `shared/`, `misc/`).
* Do NOT use `core/models/` as a general-purpose storage location.
* Keep related types inside their owning system directory (e.g. `core/eventBus/commands/` and `core/eventBus/events/`).
* A type may live in a shared Core location only when multiple independent systems genuinely own/use it and placing it under one system would create incorrect ownership.

---

# Permanent Technology Stack Policy

The following technology stack is the **default technology stack for the entire Remmi application**.
Apply these rules automatically to future coding tasks.

## 1. Primary Language: Kotlin
Remmi application code must be **Kotlin-first**.
* Use Kotlin for application code, Core, UI, plugins, Android integrations, repositories, models, state, tests, background work, and performance tooling.
* Do not create new Java application code. Java may exist only when required by Android platform constraints, external libraries, generated code, or unavoidable interop.
* Prefer modern, idiomatic Kotlin: data classes, sealed classes/interfaces, null safety, extension functions where clear, structured concurrency, and immutable data across boundaries.

## 2. Android UI: Jetpack Compose
* Use **Jetpack Compose** as the primary UI framework for Home, plugin UI, screens, components, dialogs, sheets, launcher UI, settings, widgets, and interactive surfaces.
* Do not introduce XML layouts for new Remmi UI without a concrete platform reason.

## 3. Design System: Material 3
* Use **Material 3** as the default design system (theming, dynamic color, accessibility, responsive/adaptive layouts, typography hierarchy, proper touch targets, light/dark themes).

## 4. Material 3 Expressive
* Use Material 3 Expressive capabilities where supported by toolchain versions and providing genuine UX value. Expressive design must not compromise performance, accessibility, clarity, or stability.

## 5. Asynchronous Programming: Kotlin Coroutines
* Use **Kotlin Coroutines** for asynchronous work with structured concurrency.
* Never perform blocking work on the main/UI thread.
* Do not create manual thread management or callback-heavy architecture.

## 6. Reactive State: Kotlin Flow / StateFlow
* Use `Flow` for streams of data, `StateFlow` for current observable state, and `SharedFlow` for transient shared streams.
* Prefer simple direct function calls for simple local operations.

## 7. EventBus: Remmi EventBus
* Remmi has **one EventBus** for decoupled communication between independent systems (`Command` = intent, `Event` = notification).
* Do NOT use EventBus for ordinary local function calls, parent/child UI communication, or simple object coordination.

## 8. Persistence: Local-First Storage (No Database)
* Remmi will **NOT use a database** (no Room, SQLite, DAOs, DB entities, DB repositories, or generic CRUD frameworks).
* Use appropriate Android-native local storage mechanisms for actual requirements (DataStore for application preferences/settings, app-private files for documents, cache storage for temporary info).
* **No Remmi Backend**: Remmi does not have its own server or cloud database.
* **Direct Google Services**: Integrates directly with Google Services where needed (Google Calendar, Tasks, Contacts). Google remains the remote source of truth; Remmi maintains minimal local state/cache as needed.

## 9. Background Work: WorkManager
* Use **WorkManager** for deferrable, reliable/persistent scheduled background execution.
* Do not use WorkManager for immediate application operations or as a substitute for Coroutines or Flow.

## 10. Code Optimization: R8
* Use **R8** for release builds. Ensure release builds remain functional when adding libraries or reflection. Only add required, documented keep rules.

## 11. Performance Profiles: Baseline Profiles
* Use **Baseline Profiles** for critical user journeys (startup, Home, launcher, app search, app launching, plugin switching, scrolling).

## 12. Performance Benchmarking: Macrobenchmark
* Use **Jetpack Macrobenchmark** to measure end-to-end performance on release configurations (cold/warm startup, Home rendering, app search, plugin switching).

## 13. Performance Diagnostics: Perfetto / Android System Tracing
* Use Android tracing and Perfetto to investigate startup delays, main-thread stalls, jank, or CPU usage. Measure first, optimize second.

## 14. Performance Principles
* UI work belongs on the main thread; heavy work must not block the main thread.
* Avoid unnecessary initialization during startup; prefer lazy initialization.
* Keep Home responsive; a plugin must not freeze or crash the main Home experience.

## 15. Testing Technology
* Unit tests for pure logic (`Kotlin`/`JUnit`).
* Compose UI tests for observable UI behavior.
* Android/instrumented tests for platform behavior.
* Macrobenchmark for performance workflows.

## 16. Architecture Technology Rule
* The technology stack does NOT authorize adding unneeded architecture (no Hilt, Dagger, Koin, extra DI frameworks, extra event buses, extra persistence abstractions, or unnecessary abstraction layers).

## 17. Technology Decision Rule
* Reuse the existing technology stack. If a new technology appears necessary, stop and explain the problem, why the existing stack cannot solve it, proposed technology, architectural cost, and lifecycle implications.

## 18. Version Rule
* Use versions compatible with the project's current AGP, Kotlin, Gradle, and compile SDK. Prefer stable releases.

## 19. Default Stack Summary
```text
Language:               Kotlin
UI:                     Jetpack Compose
Design:                 Material 3 (Expressive where appropriate)
Async:                  Kotlin Coroutines
Reactive State:         Kotlin Flow / StateFlow / SharedFlow
System Communication:   Remmi EventBus
Persistence:            Local-First Storage (DataStore / Files / Cache - No Database)
Background Work:        WorkManager
Release Optimization:   R8
Startup Optimization:   Baseline Profiles
Benchmarking:           Macrobenchmark
Diagnostics:            Perfetto / Android System Trace
Testing:                Kotlin/JUnit, Compose Testing, Android Instrumentation
```

## 20. Most Important Rule
**Use the existing Remmi technology stack by default.**
Keep Remmi's technology stack small, modern, stable, fast, and understandable.
