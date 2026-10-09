# Remmi Health Functional Specification

**Status:** Authoritative Functional Contract  
**Scope:** Health workspace / plugin  
**Architecture:** Local-first, no database (no Room/SQLite), no Remmi backend/server, direct Google Services & Android capabilities only  
**Primary Goal:** Help the user understand their overall wellbeing, identify meaningful life patterns, optimize cognitive/study conditions, and learn what works for them long-term.

---

## 1. Purpose & Core Loop

Health is a personal wellbeing, recovery, and cognitive self-management workspace.

It integrates:
- Unified daily assessment tests
- Objective Android/Google sensor measurements
- Sleep duration, timing, and recovery
- Physical activity & exercise
- Mind, focus, and study performance context
- Physical health & lifestyle signals (hydration, food timing, caffeine, digital usage)
- Personal baselines & trend analytics
- Non-medical local insights & AI-assisted interpretation

**Core Product Loop:**
LIFE → UNIFIED TEST & SENSORS → HEALTH MODEL → DERIVED DIMENSIONS → OVERVIEW RADAR PROFILE → PATTERNS → EXPLAINABLE RECOMMENDATIONS → ACTION → OUTCOME → PERSONAL LEARNING

Health optimizes for better daily decisions and study conditions, not maximum data collection.

---

## 2. Functional Principles & Rules

1. **Local-First Privacy**: Stored in app-private files (`context.filesDir/health_*.json`). Operates 100% offline.
2. **No Database**: No Room, SQLite, DAOs, ORMs, or SQL migrations.
3. **No Remmi Backend**: Direct Google Services integration only (Google Calendar, Tasks). No custom server or cloud database.
4. **No Duplicated Ownership**:
   - **Calendar** owns events, commitments, exams.
   - **Planning** owns tasks, projects, study workload.
   - **Apps** owns installed application metadata and launcher interaction.
   - **Android Capability Systems** own system APIs (`SensorManager`, `POST_NOTIFICATIONS`).
   - **Health** consumes and interprets signals from those systems without copying ownership.
5. **Non-Medical Language**: Strictly self-management and wellbeing terminology (e.g. "Rest Required", "Elevated Stress", "Below Usual Baseline"). Zero clinical diagnoses or treatment claims.

---

## 3. Health Workspace Navigation & Primary Screens

Health participates in standard Remmi workspace navigation using the existing icon-only floating bottom selector:

```text
WORKSPACES ↔ HOME ↔ APPS ↔ HEALTH
```

The Health workspace is organized around **Six Primary User-Facing Screens**:

1. **Overview** — "How am I doing overall?"
2. **Tests** — "How was my day / experience?"
3. **Sleep** — "Am I recovering properly?"
4. **Exercise** — "Am I moving and exercising in a way that supports my wellbeing?"
5. **Mind** — "How can I think, focus, learn, and study better?"
6. **Health** — "How is my physical wellbeing and lifestyle?"

Underlying health data domains (Mood, Stress, Energy, Focus, Food, Habits, Digital behavior, Recovery) feed these six primary screens rather than acting as standalone top-level tabs.

---

## 4. Overview Screen

Purpose: "How am I doing overall?" — A concise, high-level synthesis dashboard.

### Mandatory Elements
1. **Health Profile Radar Graph**:
   - Variable N-sided polygon graph (e.g. Sleep, Physical Health, Mood, Stress, Energy, Focus, Exercise, Recovery, Mind).
   - Dimensions normalized to 0–100 Remmi wellbeing scores derived from the Health model.
   - Visual background threshold rings:
     - `0–24` = Bad
     - `25–49` = OK
     - `50–74` = Good
     - `75–100` = Excellent
   - Non-medical visualization thresholds.
2. **Previous-Period Overlay**:
   - Subdued comparison graph overlaid beneath current profile.
   - Daily = Today vs Yesterday.
   - Weekly = Current Week vs Previous Week.
   - Monthly = Current Month vs Previous Month.
3. **Period Selector**: Compact control (`DAILY | WEEKLY | MONTHLY`).
4. **Quick Test Action**: Prominent "Take today's test" button launching the unified test flow.

---

## 5. Tests Screen & Assessment Engine

Replaces direct 1–5 manual rating buttons with a **Unified Daily Test**.

### 5.1 Assessment Flow
- Adaptive questioning covering Sleep, Exercise, Mood, Stress, Energy, Focus, Mental Fatigue, Motivation, Habits, Digital behavior, Workload, and Food/Caffeine.
- Answers describe actual experiences rather than arbitrary numbers (e.g. "Did you wake feeling rested?", "How easy was it to concentrate?", "Did you feel overwhelmed?").
- Supported answer types: Yes/No, multiple choice, ranges, duration, frequency, categorical choices, short text.

### 5.2 Adaptive Logic
- Small core set of questions asked daily.
- Detailed follow-up questions triggered only when relevant (e.g., exercise follow-ups asked only if exercise = Yes).
- Target duration: 2–5 minutes.

### 5.3 Test Results & History
- Answers feed the deterministic Health Model to derive numerical dimensions (0–100).
- Recent completed tests viewable with answers, derived scores, baseline comparison, and evidence.

---

## 6. Sleep Screen

Purpose: "Am I recovering properly?"

### Key Elements
- **Last Night**: Bedtime, wake time, duration, quality, morning energy, awakenings/naps.
- **Baseline Comparison**: Typical sleep duration, deviation from personal baseline, data sufficiency state.
- **Consistency**: Bedtime variability, wake-time variability, duration variability.
- **Context & Trends**: 7d/30d visual trends, exercise/caffeine/screen time/stress context.
- **Recovery State**: Plain-language classification (`Restored & Ready`, `Moderate Recovery`, `Rest Required`).

---

## 7. Exercise Screen

Purpose: "Am I moving and exercising to support my wellbeing?"

### Key Elements
- **Today**: Duration, steps (via `TYPE_STEP_COUNTER`), sedentary time, active time.
- **Weekly Summary**: Exercise session count, total active duration, outdoor time, workout types (walking, running, cycling, strength, mobility, sports).
- **Historical Associations**: Observed relationships between physical movement and focus/study duration (non-causal phrasing: "associated with", "tends to coincide with").

---

## 8. Mind Screen

Purpose: "How can I think, focus, learn, and study better?"

### Key Elements
- **Current State**: Focus, mental energy, stress, motivation, mental fatigue (derived from Test & Planning context).
- **Study Performance Integration**: Reads Planning tasks and study sessions (workload, deadlines, completion rates). Health does not own Planning tasks.
- **Study Recommendations**: Evidence-backed suggestions on timing, breaks, and study conditions (e.g. "Your hardest study work is usually more successful in the morning").
- **Interference Patterns**: Highlights habits associated with study delays (e.g. late-night screen time, frequent app switching) using correlation phrasing.
- **Food & Study Relationships**: Food timing, hydration, and caffeine guidance relative to study sessions.

---

## 9. Health Screen (Physical & Lifestyle)

Purpose: "How is my physical wellbeing and lifestyle?"

### Key Elements
- **Activity & Movement**: Steps, active/sedentary time.
- **Hydration & Food Timing**: Water intake, meal timing, late meals.
- **Caffeine & Alcohol**: Consumption timing, late caffeine alerts.
- **Outdoors & Recovery**: Outdoor time and physical lifestyle indicators.

---

## 10. Health Model & Scoring Architecture

```text
RAW TEST RESPONSES & SENSOR LOGS
               │
               ▼
     [HEALTH MODEL ENGINE]
 (Baselines, Averages, Variances)
               │
               ▼
   [DERIVED DIMENSIONS (0-100)]
               │
               ├──────────────────────┐
               ▼                      ▼
    [OVERVIEW RADAR PROFILE]   [MIND / SLEEP / EXERCISE / HEALTH]
               │
               ▼
      [LOCAL INSIGHTS]
 (Deterministic Association Rules)
               │
               ▼
     [SECONDARY AI EVALUATOR]
 (Natural Language Explanation & "Why?" Evidence)
```

1. All 0–100 Wellbeing scores are derived deterministically from the Health Model.
2. AI **NEVER** invents or modifies fundamental numeric scores.
3. Every score includes metadata: value, confidence, data completeness, evidence.

---

## 11. AI Role & Safety Boundaries

- **Secondary Role**: AI provides natural language interpretation, contextual explanations, and "Why?" evidence dialogs.
- **Mandatory Disclaimer**: Included on all AI evaluations:  
  *"This self-management evaluation is for personal wellbeing tracking only and is not a medical diagnosis or clinical advice."*
- **Recommendation Cap**: Maximum 2 actionable recommendations.
- **Offline Fallback**: 100% operational offline using local rule engine.

---

## 12. Privacy, Anti-Spam & Hardening

- **Data Minimization**: AI prompts send minimal necessary summary metrics, redacting notes.
- **Notification Anti-Spam**: Quiet hours (22:00–08:00) enforced; max 1 proactive notification per day.
- **Data Management**: JSON Export, JSON Import, and Clear All Data confirmation dialog.
- **Error Boundaries**: Compose `HealthErrorBoundary` isolates UI exceptions.
