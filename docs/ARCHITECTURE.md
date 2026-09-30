# Remmi Architecture

## Current Architecture & Technology
Remmi is built as a lightweight, clean Android application using standard modern Android toolchains:
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose + Material 3
* **Concurrency:** Kotlin Coroutines & Flow
* **Build System:** Gradle (Kotlin DSL) + Version Catalog (`gradle/libs.versions.toml`)

## Architectural Boundaries

```text
com.remmi/
├── core/       # Fundamental application infrastructure and entry point
├── ui/         # Compose UI screens, components, and Material 3 theme
└── plugins/    # Feature/plugin module boundaries
```

* **`core/`**: Contains `RemmiApplication` (application lifecycle entry point). Contains zero UI components.
* **`ui/`**: Contains `MainActivity`, `RemmiApp`, and theme definition (`Theme.kt`, `Color.kt`, `Type.kt`).
* **`plugins/`**: Isolated boundary reserved for feature extensions. Currently contains `PluginsMarker`.

No dependency injection frameworks (Hilt, Koin, Dagger), custom event buses, navigation frameworks, or speculative abstraction layers are present in this foundation.
