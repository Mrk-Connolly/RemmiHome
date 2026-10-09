# Performance Contract (`PERF-01`)

## 1. Identity and status
* **Identifier**: `PERF-01`
* **Name**: Performance Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines latency budgets, measurement methodologies, and startup expectations.

## 3. Performance Budgets
Based on baseline measurements, the following target budgets apply:
- **Cold Host Initialization:** < 50 ms.
- **App List Sorting/Processing (1000 apps):** < 25 ms.
- **App Category Resolution:** < 15 ms (MUST be performed off main thread).
- **Global Search Ranking:** < 20 ms.
- **JSON Batch I/O Operations:** < 500 ms (off main thread).

## 4. Main-Thread Rules
- No synchronous IPC calls (e.g., `PackageManager`) or disk I/O (e.g., `SharedPreferences`) on the main thread during UI rendering or startup.

## 5. Measurement and Verification
- Microbenchmarks (JUnit) are configured for deterministic paths.
- Optimizations MUST be proven by comparing before-and-after measurements.
- Android system tracing (Perfetto) SHOULD be used to investigate jank or main-thread stalls.