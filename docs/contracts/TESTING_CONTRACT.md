# Testing Contract (`TEST-01`)

## 1. Identity and status
* **Identifier**: `TEST-01`
* **Name**: Testing Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines test coverage expectations, architecture enforcement, and required verification reporting.

## 3. Required Test Categories
- **Unit Tests:** For business rules, data mappings, command resolution, and state management.
- **Architecture Tests:** To enforce module boundaries (e.g., prohibiting a plugin from depending on another plugin).
- **Migration Tests:** For any JSON file format changes to ensure data preservation.
- **AI Validation Tests:** To ensure AI tool execution properly routes through the validation and confirmation pipeline.
- **Performance Tests:** JUnit-based microbenchmarks for critical latency paths (startup, search).

## 4. Enforcement
- All applicable test suites MUST pass before completing an implementation phase.
- Do not delete or weaken failing tests to achieve a passing build. If a test is fundamentally flawed due to a contract change, it MUST be explicitly updated in accordance with the contract change procedure.