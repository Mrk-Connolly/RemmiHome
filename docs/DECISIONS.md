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

### Consequences
- Architectural boundaries are immediately visible from the directory structure.
- Small build footprint and fast build times.
- Simple, direct dependency flows.
