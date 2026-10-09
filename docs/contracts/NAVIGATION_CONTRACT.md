# Navigation Contract (`NAV-01`)

## 1. Identity and status
* **Identifier**: `NAV-01`
* **Name**: Navigation Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines navigation ownership, routing behavior, and performance constraints during transitions.

## 3. Normative requirements & Performance
- Navigation and initial rendering MUST NOT synchronously depend on:
  - Database queries or JSON disk reads.
  - Network requests.
  - AI model calls.
  - Heavy parsing operations.
- The synchronous portion of navigation MUST be limited to updating the navigation state and rendering the destination layout.
- Data fetching MUST happen asynchronously, updating the UI via state flows when ready.

## 4. Ownership
- The `:app` module's UI layer owns the core navigation graph and back-stack behavior (e.g., `HomeScreen`, `WorkspacesScreen`).
- Plugins handle internal view switching, which must be observable and interceptable via standard back-press handlers.

## 5. Commands
- Remote or cross-feature navigation requests MUST use the `NavigateCommand` broadcast via the `RemmiEventBus`.

## 6. Enforcement
Performance tests (Macrobenchmark/JUnit) MUST enforce that critical navigation paths (like launching the app drawer or opening a workspace) execute in < 50ms.