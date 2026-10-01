# Remmi Architecture

## Current Architecture & Technology
Remmi is built as a lightweight, clean, local-first Android application using standard modern Android toolchains:
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose + Material 3
* **Concurrency:** Kotlin Coroutines & Flow
* **Persistence:** Local-First Storage (DataStore / App-Private Files / Cache — No Database / Room)
* **Build System:** Gradle (Kotlin DSL) + Version Catalog (`gradle/libs.versions.toml`)

## Architectural Boundaries

```text
com.remmi/
├── core/       # Fundamental application infrastructure, EventBus, Host, Controller, Plugin Registry, and Android Capabilities
│   ├── eventBus/  # Type-safe coroutine-friendly EventBus
│   │   ├── commands/ # RemmiCommand interface
│   │   └── events/   # RemmiEvent interface
│   ├── host/      # RemmiHost infrastructure owner
│   ├── controller/# RemmiController coordinator
│   ├── plugin/    # RemmiPluginRegistry collection & lifecycle manager
│   └── android/   # Android capability contracts and platform implementations
│       ├── systemInfo/ # RemmiSystemInfoCapability & AndroidSystemInfoCapability
│       └── launcher/   # RemmiLauncherCapability & AndroidLauncherCapability
├── ui/         # Compose UI screens, components, and Material 3 theme
│   └── home/   # HomeScreen launcher shell container
└── plugins/    # Independent feature plugin boundary
    ├── RemmiPlugin.kt         # Plugin contract interface
    └── api/RemmiPluginContext.kt # Context provided to plugins
```

* **`core/`**:
  - `RemmiApplication`: Lifecycle entry point. Initializes `RemmiHost` and `RemmiController`.
  - `eventBus/`: `RemmiEventBus` supporting decoupled communication via `RemmiCommand` (`commands/`) and `RemmiEvent` (`events/`).
  - `host/`: `RemmiHost` holding `RemmiEventBus`, `RemmiPluginRegistry`, `systemInfoCapability`, `launcherCapability`, and application `CoroutineScope`.
  - `controller/`: `RemmiController` coordinating core workflows.
  - `plugin/`: `RemmiPluginRegistry` managing plugin registration, duplicate rejection, and lifecycle orchestration.
  - `android/`: Self-contained Android capability contracts (`systemInfo/`, `launcher/`) and platform implementations.
* **`ui/`**: Contains `MainActivity`, `RemmiApp`, `home/HomeScreen`, and theme definition (`Theme.kt`, `Color.kt`, `Type.kt`).
* **`plugins/`**: Isolated contract boundary for feature extensions.
  - `RemmiPlugin`: Contract with `id`, `name`, `onInitialize(context)`, `onShutdown()`.
  - `RemmiPluginContext`: Public Core context exposing `RemmiEventBus` and plugin `CoroutineScope`.

Plugins depend strictly on public Core contracts (`RemmiPluginContext`, `RemmiEventBus`, `RemmiEvent`, `RemmiCommand`, `RemmiSystemInfoCapability`, `RemmiLauncherCapability`), never on Core implementation internals, Android platform types (`Context`, `Activity`, `PackageManager`), or UI internals.
