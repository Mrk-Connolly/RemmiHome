# Architecture Contract (`ARCH-01`)

## 1. Identity and status
* **Identifier**: `ARCH-01`
* **Name**: Architecture Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines the module boundaries, dependency direction, public API policy, and composition root for Remmi.

## 3. Normative Requirements & Dependency Rules
Remmi is splitting into a multi-module architecture:
- `:app`: The composition root. Assembles implementations. Depends on `:core` and `:plugins`.
- `:plugins`: Feature boundaries (Food, Health, Knowledge, Planning). Depends on `:core`.
- `:core`: Infrastructure, Android capabilities, EventBus, API contracts. Depends only on standard libraries.

**Dependency Rules:**
- Features (`:plugins`) MUST NOT depend on `:app`.
- `:core` MUST NOT depend on `:plugins` or `:app`.
- Plugins MUST NOT depend on the internal implementations of other plugins.
- Cross-feature communication MUST be mediated via `RemmiEventBus` using `RemmiCommand` and `RemmiEvent` located in `:core`.

## 4. Public API Rules
- Use `internal` and `private` visibility by default for implementation details.
- Avoid exposing Android platform types (`Context`, `PackageManager`) in domain-level contracts.
- Do NOT use heavy dependency injection frameworks (e.g., Hilt/Dagger). Dependencies are manually wired in the `:app` composition root via `RemmiApplication` and `RemmiHost`.

## 5. Change Procedure
Modifications to module boundaries require a formal contract amendment and review of the Gradle dependency graph.