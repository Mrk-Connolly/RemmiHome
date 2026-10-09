# Remmi Health Local Persistence Strategy

**Status:** Authoritative strategy decision  
**Scope:** Remmi Health plugin / workspace persistence  
**Compliance:** Strict alignment with `GEMINI.md` (NO DATABASE, NO BACKEND, Local-First)

---

## 1. Core Principles

1. **Strictly Local-First**: All Health user data resides locally in app-private device storage (`context.filesDir`).
2. **Zero Database Frameworks**: No Room, SQLite, SQL DAOs, ORMs, DB migrations, or database repositories.
3. **Domain-Owned Files**: Health owns its data storage directly without creating abstract layer generic storage dumping grounds.
4. **Asynchronous & Non-Blocking**: Disk I/O operations are offloaded to `Dispatchers.IO` using Kotlin Coroutines and exposed via immutable `StateFlow`.

---

## 2. Persistence Map

| Category | Data Types | Mechanism | Storage Location |
| :--- | :--- | :--- | :--- |
| **Settings & Preferences** | Notification levels, quiet hours, UI defaults | DataStore / `health_settings.json` | `context.filesDir/health_settings.json` |
| **Subjective Observations** | Mood, Stress, Energy, Focus, Wellbeing, Motivation ratings & notes | App-private JSON file | `context.filesDir/health_observations.json` |
| **Sleep Data** | Bedtime, wake time, duration, quality, awakenings, notes | App-private JSON file | `context.filesDir/health_sleep.json` |
| **Activity Logs** | Exercise type, duration, intensity, steps, notes | App-private JSON file | `context.filesDir/health_activity.json` |
| **Food & Hydration** | Meals, hydration (ml), caffeine (mg) | App-private JSON file | `context.filesDir/health_food.json` |
| **Habits & Routines** | Habit definitions, completion timestamps, schedules | App-private JSON file | `context.filesDir/health_habits.json` |
| **Personal Experiments**| Hypotheses, interventions, active status, outcomes | App-private JSON file | `context.filesDir/health_experiments.json` |
| **Larger Local Files** | Future local exports or raw trace/export caches | App-private directory | `context.filesDir/health/` |

---

## 3. Data Flow & Concurrency

```
[UI Layer / Jetpack Compose]
        │
        ▼ (Coroutines / StateFlow)
[HealthRepository]
        │
        ▼ (Dispatchers.IO)
[App-Private JSON Storage] (context.filesDir/*.json)
```

1. **Read**: On initialization or app launch, `HealthRepository.loadFromStorage()` reads and parses JSON files into immutable data models and updates memory `StateFlow` holders.
2. **Write**: When new entries are logged or mutated, updated immutable collections are written to file on `Dispatchers.IO` and UI state is immediately updated via `StateFlow`.
3. **Thread Safety**: Repository methods mutate internal `StateFlow` values on `Dispatchers.IO` using synchronized or thread-safe atomic collection updates.

---

## 4. Verification

- Verified zero DB dependencies in build configurations (`build.gradle.kts`).
- Verified persistence latency under 100ms in `HealthAndWorkspacePerformanceTest`.
