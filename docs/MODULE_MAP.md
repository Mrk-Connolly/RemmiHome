# Module Map

This document outlines the authoritative directory and module structure for Remmi.

*(Note: Remmi is currently transitioning to a multi-module Gradle project: `:app`, `:core`, `:plugins`)*

## Current Project Structure

```text
RemmiHome/
├── GEMINI.md                            # Root engineering principles & context
├── AI_IMPLEMENTATION_STATE.md           # Tracking document for AI-driven engineering tasks
├── docs/                                # Project documentation
│   ├── contracts/                       # Canonical Engineering Contracts (ARCH, FEAT, NAV, DATA, etc.)
│   ├── MODULE_MAP.md                    # This file
│   └── ...                              # ADRs, Specifications, Performance baseline
├── app/                                 # Primary App Module (Composition Root)
│   ├── src/main/java/com/remmi/
│   │   ├── core/                        # Infrastructure, EventBus, API boundaries, Android Capabilities
│   │   ├── plugins/                     # Feature boundaries (Food, Health, Knowledge, Planning)
│   │   └── ui/                          # Jetpack Compose UI (Screens, Theme)
│   └── src/test/                        # Unit tests and performance microbenchmarks
```

## Intended Target Architecture (Post-Phase 5 Split)

### `:app` (Composition Root)
- **Responsibility:** Wires the application together.
- **Contents:** `RemmiApplication`, `MainActivity`, Application-level DI/Initialization.
- **Dependencies:** `:core`, `:plugins`.

### `:core` (Infrastructure)
- **Responsibility:** Core abstractions, EventBus, AI integrations, Android capabilities.
- **Contents:** `RemmiHost`, `RemmiController`, `RemmiPluginRegistry`, `RemmiEventBus`, capabilities (`android/`), AI logic (`ai/`).
- **Dependencies:** Standard Android/Kotlin libraries only. NO dependency on `:app` or `:plugins`.

### `:plugins` (Feature Modules)
- **Responsibility:** Encapsulated domain logic, UI, and data management for specific features.
- **Contents:** `PlanningPlugin`, `FoodPlugin`, `HealthPlugin`, `KnowledgePlugin`.
- **Dependencies:** `:core`. NO dependency on `:app` or other plugins' internal implementation.

## Where to Look for Specific Responsibilities
- **Cross-Feature Communication:** Look at `core/eventBus/`.
- **AI Tool Execution / Chat:** Look at `core/ai/` and `ui/ai/` (PendingActionConfirmDialog).
- **Data Serialization / Storage:** Handled within individual plugins under `plugins/<feature>/repository/`.
- **System Capabilities (Sensors, Audio, Launcher):** Look at `core/android/`.
- **Theming & Global UI Shell:** Look at `ui/theme/` and `ui/home/`.