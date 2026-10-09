# Remmi Health Workspace - Updated Implementation Roadmap

**Product Direction:** 6 Primary Screens (`Overview | Tests | Sleep | Exercise | Mind | Health`)  
**Architecture:** Local-first, no database, no Remmi backend, direct Google Services & Android capabilities only

---

## Roadmap Phases

### Phase 1: Health Foundation & Navigation
- **H01**: Re-align `HealthTab` enum to 6 primary screens (`OVERVIEW`, `TESTS`, `SLEEP`, `EXERCISE`, `MIND`, `HEALTH`).
- **H02**: Update bottom floating icon navigation in `HealthWorkspaceScreen` for 6 primary screens using existing Remmi workspace navigation.
- **H03**: Update `HealthPlugin` registration and initialization lifecycle.

### Phase 2: Health Data Model
- **H04**: Implement core `HealthModel` and 0–100 Wellbeing normalization structure.
- **H05**: Define data provenance (`HealthSource`: MANUAL, ANDROID, GOOGLE, APPS, DERIVED, AI).

### Phase 3: Unified Daily Test & Adaptive Questioning
- **H06**: Implement `DailyTest` model (Yes/No, multiple choice, duration, frequency, categorical choices).
- **H07**: Implement adaptive questioning engine (ask core questions first, conditionally show follow-up questions).
- **H08**: Build `TestsScreen` UI with "Start Daily Test" workflow.
- **H09**: Build test results calculator mapping test answers to Health Model dimensions.
- **H10**: Build Test History view showing completed test answers, derived scores, and evidence.

### Phase 4: Sleep Screen & Recovery State
- **H11**: Build `SleepScreen` UI showing Last Night metrics (bedtime, wake time, duration, quality, morning energy).
- **H12**: Implement Sleep Baseline comparison and bedtime/wake-time consistency metrics.
- **H13**: Implement plain-language Recovery State engine (`Restored & Ready`, `Moderate Recovery`, `Rest Required`).
- **H14**: Add 7-day and 30-day sleep duration trends.

### Phase 5: Exercise Screen & Movement Tracking
- **H15**: Build `ExerciseScreen` UI for Today's movement (exercise duration, steps, active time, sedentary time).
- **H16**: Implement Weekly Exercise summary (workout sessions count, total active duration, outdoor time, workout types).
- **H17**: Integrate `TYPE_STEP_COUNTER` hardware sensor capability with manual exercise fallback.

### Phase 6: Physical Health & Lifestyle Screen
- **H18**: Build `HealthScreen` UI (Physical wellbeing & lifestyle synthesis).
- **H19**: Implement Hydration tracking and daily water intake totals.
- **H20**: Implement Food & Meal timing tracking (breakfast, lunch, dinner, snack, timestamp, description).
- **H21**: Implement Caffeine and Alcohol timing tracking.

### Phase 7: Health Baselines & Sufficiency States
- **H22**: Implement personal baseline calculator across all health dimensions.
- **H23**: Implement baseline sufficiency states (`INSUFFICIENT_DATA`, `PRELIMINARY`, `ESTABLISHED`).
- **H24**: Add baseline quality descriptions preventing false precision.

### Phase 8: Health Scoring Model & Derived Dimensions
- **H25**: Implement 0–100 Wellbeing Score normalization math for Sleep, Physical, Mood, Stress, Energy, Focus, Exercise, Recovery, Mind.
- **H26**: Add score metadata (value, confidence, data completeness, evidence citations).

### Phase 9: Overview Radar Profile & Previous-Period Overlay
- **H27**: Build variable N-sided `HealthProfileRadar` graph supporting dynamic dimensions.
- **H28**: Draw visualization background threshold rings (0-24 BAD, 25-49 OK, 50-74 GOOD, 75-100 EXCELLENT).
- **H29**: Implement Previous-Period Overlay graph (Daily: Yesterday, Weekly: Previous Week, Monthly: Previous Month).
- **H30**: Build compact `DAILY | WEEKLY | MONTHLY` Period Selector.
- **H31**: Add "Take Today's Test" Quick Test action button launching the Unified Test flow.

### Phase 10: Planning / Calendar / App Context Integration
- **H32**: Read Planning workload context (pending tasks, due today, study sessions) without copying Planning tasks into Health.
- **H33**: Read Calendar commitments context (today's events count, exams) without duplicating Calendar event storage.
- **H34**: Read Digital screen time stats without duplicating `Apps` ownership (`docs/HEALTH_DIGITAL_DATA_BOUNDARY.md`).
- **H35**: Create `HealthContextSnapshot` uniting Health + Planning + Calendar + Digital metrics.

### Phase 11: Mind Screen & Cognitive Performance
- **H36**: Build `MindScreen` UI showing concise current state (Focus, Mental Energy, Stress, Motivation, Mental Fatigue).
- **H37**: Integrate Study Performance view reading Planning tasks and study workload.
- **H38**: Implement Study Recommendations engine based on sleep, energy, food timing, and workload.
- **H39**: Implement Interference Patterns detector (highlighting habits associated with study delays using non-causal association phrasing).
- **H40**: Implement Food & Study relationship guidance.

### Phase 12: Local Study-Performance Insights & Non-Causal Associations
- **H41**: Calculate 1-day lagged cross-domain correlations (e.g., yesterday's sleep vs today's focus).
- **H42**: Implement day-of-week pattern detector (e.g. Monday stress spike, Thursday energy dip).
- **H43**: Implement statistical anomaly detector (> 1.5 std-dev deviations).
- **H44**: Enforce strict non-causal association phrasing ("associated with", "tends to coincide with").

### Phase 13: Secondary AI Health Evaluation & "Why?" Evidence Dialog
- **H45**: Build structured AI prompt formatter (`buildHealthAiPrompt`) incorporating baselines, test answers, objective logs, and workload context.
- **H46**: Enforce mandatory non-medical disclaimer on all AI evaluations.
- **H47**: Implement "Why?" evidence dialog displaying supporting evidence, objective findings, and confidence rationale.
- **H48**: Implement 100% offline fallback evaluator for AI execution.

### Phase 14: Actionable Recommendations Engine
- **H49**: Implement "One Useful Action" deterministic recommendation engine on Overview/Today.
- **H50**: Enforce recommendation cap (maximum 2 recommendations total).
- **H51**: Enforce sample-count-driven confidence rating (`Low (limited data)`, `Moderate`, `High`).

### Phase 15: Notifications & Anti-Spam
- **H52**: Implement `HealthNotificationSettings` (Daily Check-in, Recovery Alerts, Habit Reminders).
- **H53**: Implement `HealthNotificationManager` anti-spam rules: Quiet hours (22:00–08:00) suppression and max 1 proactive alert/day limit.
- **H54**: Build Notifications & Anti-Spam configuration dialog.

### Phase 16: Personal Experiments & Personal Learning
- **H55**: Build structured Experiment creation (title, hypothesis, intervention, target dimension, 7/14/30 days duration).
- **H56**: Implement `analyzeExperimentOutcome` calculating pre-experiment baseline vs intervention averages.
- **H57**: Build experiment lifecycle UI in `ExperimentsToolScreen` with analysis results dialog.

### Phase 17: Android & Google Health Integrations
- **H58**: Isolate Health Connect platform capability (`RemmiHealthConnectCapability`) with 100% functional manual logging fallback.
- **H59**: Display Platform Capabilities Status card on Overview (Step Sensor, Health Connect, Direct Google Services).
- **H60**: Display data provenance badges (`[MANUAL]`, `[ANDROID]`, `[GOOGLE]`, `[APPS]`) on history rows.

### Phase 18: Privacy & Security
- **H61**: Re-affirm 100% local-first private storage (`context.filesDir/health_*.json`).
- **H62**: Implement JSON Export, JSON Import, and Clear All Data confirmation dialog.
- **H63**: Enforce AI data minimization (redact free-text notes before transmitting).

### Phase 19: Data Quality, Range Checks & Duplicate Handling
- **H64**: Enforce range check validation (ratings 1–5, sleep 0–24h) and duplicate log suppression.
- **H65**: Enforce corrupted JSON line recovery (`runCatching`) preventing file parse crashes.
- **H66**: Enforce missing data policy (leave unrecorded days blank, never treat as zero).

### Phase 20: Accessibility & Performance Hardening
- **H67**: Implement Compose `HealthErrorBoundary` catching UI rendering exceptions gracefully.
- **H68**: Enforce `Dispatchers.IO` file IO, sub-millisecond local queries, and lazy Flow streams.

### Phase 21: Comprehensive Unit & Integration Test Suite
- **H69**: Build unit tests for 0–100 Wellbeing normalization math and radar profile model.
- **H70**: Build unit tests for Unified Daily Test adaptive questioning and result derivation.
- **H71**: Build unit tests for Sleep, Exercise, Mind, and Health screen calculation engines.
- **H72**: Build unit tests for cross-domain correlations, 1-day lag math, and non-causal association phrasing.
- **H73**: Build unit tests for AI evaluation, non-medical disclaimers, max 2 recommendation cap, and offline fallback.
- **H74**: Build unit tests for anti-spam notification rules (quiet hours 22:00–08:00, max 1 alert/day).

### Phase 22: End-to-End Workflow Verification
- **H75**: Validate end-to-end Daily Test -> derived dimensions -> Overview Radar update workflow.
- **H76**: Validate end-to-end Sleep, Exercise, Mind, and Health workflows.
- **H77**: Validate end-to-end Privacy, Export/Import, and Data Wipe workflow.
- **H78**: Validate `HealthPlugin` registration in `RemmiPluginRegistry` and lifecycle management.

### Phase 23: Final Health Audit & Release Sign-Off
- **H79**: Verify release build compilation via `:app:assembleRelease`.
- **H80**: Execute full unit & integration test suite (`:app:testDebugUnitTest`).
- **H81**: Final Health Workspace audit and release sign-off.
