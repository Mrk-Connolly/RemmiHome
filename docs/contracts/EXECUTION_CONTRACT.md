# Execution Contract (`EXEC-01`)

## 1. Identity and status
* **Identifier**: `EXEC-01`
* **Name**: Execution Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines coroutine scopes, dispatcher use, main-thread restrictions, and background work policies.

## 3. Normative Requirements
- **Structured Concurrency:** MUST be used for all asynchronous work.
- **Main Thread Restrictions:**
  - NEVER use `runBlocking` on a UI path.
  - NEVER use blocking I/O (Disk reads/writes, network) on the main thread.
  - NEVER use `Thread.sleep`.
- **GlobalScope:** Prohibited. Use `viewModelScope` for UI, plugin scope (via `RemmiPluginContext`), or application scope (via `RemmiHost`) for appropriate lifetimes.

## 4. Background Work
- Persistent, deferrable background execution MUST use `WorkManager`.
- Immediate non-durable work should use appropriate coroutine dispatchers (`Dispatchers.IO`, `Dispatchers.Default`).

## 5. Failure Behavior
- Exceptions in asynchronous blocks MUST be caught or propagated to a defined error handler.
- Operations that may hang MUST use `withTimeout`.

## 6. Enforcement
StrictMode MUST be enabled in development to automatically detect and flag main-thread I/O violations.