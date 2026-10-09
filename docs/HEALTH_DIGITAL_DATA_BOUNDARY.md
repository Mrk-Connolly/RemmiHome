# Remmi Health Digital Data Boundary

**Status:** Authoritative data boundary specification  
**Scope:** Remmi Health plugin / workspace vs Apps & Core Launcher capability  
**Compliance:** Strict alignment with `GEMINI.md` and `HEALTH_SPECIFICATION.md` (No duplicated ownership)

---

## 1. Ownership & Architecture Rules

1. **Launcher & App Ownership**: `RemmiLauncherCapability` (`core/android/launcher/`) owns installed application discovery, app metadata (`RemmiAppInfo`), and app launching. `AppsScreen` owns application presentation and user interaction.
2. **Zero Duplication in Health**: Health **MUST NOT** query `PackageManager`, scan installed packages, or duplicate application catalog storage.
3. **Usage Data Provenance**: If Android app usage stats or screen time metrics are collected, the capability boundary resides in `core/android/launcher/` or `core/android/usage/` with `HealthSource.APPS` provenance.
4. **Health Role**: Health consumes app usage and screen time metrics solely to analyze digital behavior relationships (e.g., late-night screen time vs sleep quality, study interruptions vs focus).

---

## 2. Data Flow

```
[Android Platform (UsageStatsManager / PackageManager)]
               │
               ▼
   [RemmiLauncherCapability]  (core/android/launcher/)
               │
       ┌───────┴───────┐
       ▼               ▼
 [Apps Workspace]   [Health Workspace]
 (App Grid & Launch) (Screen Time & Focus Patterns)
```

---

## 3. Privacy & Permission Boundary

- Screen time / usage stats permissions (`PACKAGE_USAGE_STATS`) require explicit user consent via Android System Settings.
- Health requests digital usage insights only when permission is granted and feature is explicitly enabled.
- App usage metrics sent to AI evaluation payloads are strictly minimized to category-level or summary durations.
