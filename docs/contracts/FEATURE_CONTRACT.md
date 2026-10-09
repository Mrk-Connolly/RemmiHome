# Feature Contract (`FEAT-01`)

## 1. Identity and status
* **Identifier**: `FEAT-01`
* **Name**: Feature Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines how a feature (plugin) exposes capabilities, owns its internal implementation, integrates with navigation, and accesses data.

## 3. Ownership and Scope
Each feature is encapsulated as a Plugin (e.g., `PlanningPlugin`, `FoodPlugin`) implementing the `RemmiPlugin` interface.
- A feature OWNS its domain models, UI state, repositories, and persistence logic.
- A feature DOES NOT OWN the core EventBus, system capabilities, or AI provider implementations.

## 4. Public Interface
- Plugins expose themselves to the core via the `RemmiPlugin` interface (`id`, `name`, `onInitialize`, `onShutdown`).
- Cross-feature capabilities are exposed via `RemmiCommand` implementations and `RemmiEvent` broadcasts.

## 5. UI and Presentation
- Features MUST use Jetpack Compose and Material 3.
- ViewModels (or equivalent state holders) MUST use `StateFlow` to expose observable UI state.
- Features MUST handle loading, error, and empty states explicitly.

## 6. Integration
- Integration with external systems (e.g., Calendar, Tasks) MUST be abstracted behind interfaces defined in `:core`. Plugins rely on capabilities provided through `RemmiPluginContext`.

## 7. Change Procedure
Adding a new Plugin requires registering it in the `:app` module's `RemmiApplication` composition root. Cross-plugin dependency is prohibited without elevating the shared contract to `:core`.