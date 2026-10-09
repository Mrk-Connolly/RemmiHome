# Remmi Food System Functional Specification

**Status:** Authoritative Functional Contract for Remmi Food System  
**Scope:** Household Workspace (`WorkspaceDomain.HOUSEHOLD`), Home Quick Actions, Meal Register, Health Integration  
**UI Layout:** 2 Consolidated Primary Screens (`Stock & Shopping` and `Recipes & Meals`)  
**Architecture:** Local-first, no database (no Room/SQLite), no Remmi backend/server, local JSON file persistence, direct capability integrations  
**Primary Goal:** Provide comprehensive, local-first management of food ingredients, stock batches, recipes, cooking events, meal logging, nutrition tracking, waste reduction, AI recipe recommendations, and eventual shopping list planning.

---

## 1. Domain Separation & Core Entities

The Food system strictly distinguishes five core domain concepts:

1. **INGREDIENT**: What the food item is (e.g. "Milk", "Tomato", "Pasta"). Persistent catalogue entry containing baseline metadata, food group, allergen tags, and nutrition profile per 100g.
2. **STOCK BATCH**: A specific physical quantity of an ingredient currently owned by the user (e.g. "1 L Milk, expires Oct 14, Mercadona, Hacendado, €1.80"). Belongs to an Ingredient.
3. **RECIPE**: Instructions and ingredient requirements for preparing food (e.g. "Pancakes"). References Ingredients with required quantities and steps.
4. **COOKING EVENT**: The act of preparing a recipe and deducting consumed ingredient quantities from stock batches.
5. **MEAL**: The record of what the user actually ate (e.g. "User ate 2 pancakes at 08:30"). Stores a frozen historical snapshot of nutrition received at consumption time.

> [!IMPORTANT]
> **COOKING IS NOT EATING**: Cooking a recipe deducts ingredients from stock and records a Cooking Event. It does NOT automatically assume the user consumed the entire prepared dish unless explicitly indicated.

---

## 2. Ownership Boundaries

To maintain strict architectural clean boundaries, system ownership is defined as follows:

* **Food Workspace Owns**:
  - Ingredients catalogue
  - Stock & Stock Batches
  - Suppliers & Brands
  - Food Groups & Allergen metadata
  - Nutrition profiles (per 100g)
  - Recipes, Recipe Ingredients, & Preparation Steps
  - Cooking Events
  - Meal Records & Meal History
  - Food Consumption & Spoilage/Waste logs
  - Food Analytics & Waste trends
  - AI Recipe Recommendations
  - Shopping List (final phase)

* **Other Workspaces / Systems**:
  - **Health**: Owns health state, sleep, activity, exercise, mental wellbeing, study performance, and overall health evaluation. **Health CONSUMES Food meal/nutrition data**; Health does NOT own Food data.
  - **Exercise**: Owned by Health.
  - **Planning**: Owns tasks, projects, workload, study planning, schedules.
  - **Calendar**: Owns calendar events, appointments, classes, commitments.
  - **Apps**: Owns installed app usage and screen time.
  - **Home**: Owns Home launcher UI, launcher layout, favorite app grid, and Home quick actions.

---

## 3. Storage & Persistence Architecture

- **100% Local-First**: All Food data is stored in app-private JSON files (`context.filesDir/food_*.json`).
- **No Relational Database**: Strictly NO Room, NO SQLite, NO DAOs, NO ORMs, and NO SQL queries.
- **No Custom Server/Backend**: NO Remmi backend server or cloud database.
- **Asynchronous Execution**: All disk I/O occurs off the main thread using Kotlin Coroutines (`Dispatchers.IO`).
- **Offline Reliability**: Full offline functionality for viewing stock, adding stock, editing ingredients, cooking, logging meals, and calculating nutrition totals.

---

## 4. Stock System & Main Screen

### 4.1 Stock Main Screen
- Accessible as a Home Tool (**STOCK**).
- Displays current active stock batches grouped by ingredient.
- **Ordering Priority**:
  1. Expired stock (highlighted red/caution)
  2. Expiring soon (expires within 7 days)
  3. Later expiry
  4. No expiry specified
- **Ingredient Row Display**: Icon/avatar, Ingredient name, Total current amount across batches, Soonest expiry date (if < 7 days), and Quick consume/remove action button.
- Tapping an ingredient opens **Ingredient Detail Screen**.

### 4.2 Food Group Filters
Support filtering by food categories:
- Vegetables, Fruits, Meat, Fish, Dairy/Lactose, Grains, Legumes, Eggs, Oils/Fats, Sweets, Snacks, Premade, Other.
- Category architecture must be extensible for future custom groups.

### 4.3 Adding Stock
Two distinct entry flows:
1. **Manual Single Ingredient (PLUS Button)**: Search/select existing ingredient or create new; enter batch quantity, unit, optional alternative measurements, expiry date, supplier, brand, purchase price, quality notes, and nutrition per 100g.
2. **Ingredient List Importer (LIST / CAMERA Button)**: Uses camera / text extraction to capture receipt/shopping list details, presenting a structured confirmation screen before creating stock batches.

### 4.4 Stock Batches, Expiry & FIFO
- **Batches**: An ingredient can have multiple active batches with different suppliers, brands, purchase prices, and expiry dates.
- **Expiry**: Expiry belongs to individual STOCK BATCHES, never globally to an ingredient.
- **FIFO (First In, First Out)**: When deducting stock for cooking or consumption, the system automatically consumes from the oldest non-expired batch first.
- **Zero-Stock Preservation**: When stock reaches 0, the Ingredient definition remains in the persistent catalogue. Buying it again creates a new stock batch under the existing ingredient.

### 4.5 Units & Conversions
- **Supported Units**: units/pieces, kg, g, liters, ml, cups, teaspoons, tablespoons.
- **Deterministic Conversions**:
  - `kg ↔ g` (1 kg = 1000 g)
  - `liters ↔ ml` (1 L = 1000 ml)
- **Density-Dependent Conversions**: Volume to weight conversions (e.g. `cups ↔ grams`) require ingredient-specific density metadata. If density is unknown, original units are preserved without inventing false conversion precision.
- **Multiple Approximate Measurements**: Optional stored conversions (e.g. 1 kg flour ≈ 8 cups), explicitly flagged as approximate in the UI.

### 4.6 Stock Transactions & Spoilage/Waste
- Every stock modification creates a `StockTransaction`: `ADDED`, `CONSUMED`, `COOKED`, `WASTED`, `EXPIRED`, `CORRECTED`.
- Users can log stock loss specifying status (`expired`, `spoiled`, `discarded`), quantity, reason, and date to support food waste analytics.

---

## 5. Ingredient Detail, Metadata & Nutrition Radar

### 5.1 Ingredient Detail
Displays:
- Icon / visual indicator & Name
- Current total stock & list of active batches (with individual expiries, suppliers, brands, prices)
- Default Supplier & Brand associations
- Quality notes
- Nutrition per 100g & Hexagonal Radar Graph

### 5.2 Hexagonal Nutrition Radar Graph
- **6 Primary Axes**:
  1. Fiber
  2. Fats
  3. Carbohydrates
  4. Protein
  5. Sodium/Salt
  6. Sugars
- Represents nutritional composition of the ingredient per 100g.
- **Mandatory UX Rule**: Graph MUST NOT be labeled or presented as a universal "health score".
- Additional micronutrients (vitamins, minerals) rendered in a structured detail list below the graph.

### 5.3 Salt vs Sodium
- System strictly distinguishes **Salt** (g) from **Sodium** (mg).
- Never label salt as sodium. Explicit conversion math (`Sodium mg = Salt g * 393.4`) is applied when conversion is required.

### 5.4 Smart Nutrition Search
- Smart Search action uses AI / external food data capability to retrieve proposed nutritional values.
- Retrieves proposed values, presents them in an editable form, and **requires explicit user confirmation** before saving.
- Never silently overwrite existing trusted ingredient data or invent missing values.

---

## 6. Camera Ingredient & Shopping List Importer

- Opens camera capture or text/receipt scanning interface.
- Extracts: Ingredient name, quantity, unit, supplier, brand, price.
- **Confidence Model**:
  - `CONFIDENT`: High certainty match.
  - `UNCERTAIN`: Requires user verification.
  - `UNKNOWN`: Missing or unrecognized details.
- Displays a structured **Confirmation & Correction Screen** before creating any stock batches.
- Unclear ingredients prompt the user to match against existing catalogue or create a new ingredient.
- Unclear prices or brands are left blank for manual user input.

---

## 7. Recipe System & Cooking Engine

### 7.1 Recipe Model & Main Screen
- Accessible as a Home Tool (**RECIPES**).
- **Recipe Data**: Title, optional image URI, description, default servings count, meal type (Breakfast, Lunch, Supper, Snack), preparation durations (total, cooking, oven, freezer/resting), ordered preparation steps (with optional timers), and required recipe ingredients.
- **Recipe Ingredients**: Reference persistent Food Ingredients by ID, specifying required quantity and unit.
- **Recipe Nutrition**: Automatically computed total recipe nutrition and per-serving nutrition based on ingredient quantities. Highlights missing nutrition data without inventing values.

### 7.2 Recipe Detail & Edit
- View-only screen showing full recipe details, scaled ingredients, step-by-step instructions, per-serving nutrition, and nutrition radar graph.
- Edit screen allows modifying all recipe metadata, ingredients, quantities, and steps with Cancel / Delete / Save actions.

### 7.3 Cooking Workflow & Stock Consumption
1. User taps **Cook** action.
2. Prompts for target number of servings (defaulting to recipe default).
3. Scales ingredient quantities accordingly.
4. Checks current stock sufficiency for each ingredient.
5. Displays a view-only **Cooking Confirmation Screen**.
6. **Cancel**: No stock change occurs.
7. **Save**:
   - Validates stock availability.
   - Atomically deducts scaled ingredient quantities from stock batches using FIFO.
   - Records a `CookingEvent` (recipe ID, timestamp, servings prepared, batches consumed).
   - If stock is insufficient, shows exact missing ingredient quantities and halts deduction (unless user explicitly confirms partial cooking).

---

## 8. Meal Register & Nutrition History

### 8.1 Persistent Meal Register
- Every actual consumption event creates a immutable `MealRecord`.
- **Meal Fields**: Unique ID, human-readable name (e.g. "Chicken Rice Bowl", "Greek Yogurt", "Banana"), timestamp, meal type (`Breakfast`, `Lunch`, `Supper`, `Snack`), source (`Recipe`, `Premade`, `Manual`), quantity/servings, ingredients consumed, nutrition received snapshot, optional notes.

### 8.2 Historical Nutrition Snapshot Rule
> [!CRITICAL]
> Historical meal records store a **frozen snapshot of nutrients received at the exact time of consumption**. Editing or updating ingredient or recipe nutrition definitions later MUST NEVER recalculate or mutate historical meal records.

### 8.3 Meal History & Aggregations
- Provides complete Meal History views.
- Aggregates actual nutrition totals: Daily, Weekly, Monthly, by Meal Type, and by Recipe.

---

## 9. Premade Foods, Snacks & Home "Eat Snack" Quick Action

### 9.1 Premade Foods & Snacks
- Registered as Ingredients in the catalogue with premade or snack flow flags.
- May include heating/cooking metadata (oven temperature, air fryer duration, frying pan details).

### 9.2 Home "Eat Snack" Quick Action
- Added to Home screen quick actions row.
- **Flow**:
  1. Displays quick picker of available snacks and premade items in stock.
  2. User selects snack item.
  3. User selects quantity: Quick options (`Whole`, `1/2`, `1/4`, `1/8`) or custom amount.
  4. User confirms consumption.
  5. Deducts stock batch via FIFO.
  6. Creates a `MealRecord` (MealType = `Snack`, Source = `Premade` / `Manual`).
  7. Stores historical nutrition received snapshot.
- **Participant Semantics**: Explicitly defined in UI options as "Servings Consumed by User" to avoid ambiguity regarding shared portions versus individual consumption.

---

## 10. Meal Logging, Reminders & Notifications

### 10.1 Meal Logging
- Supports logging Breakfast, Lunch, Supper, Snack, Manual meals, or explicitly logging "Nothing".

### 10.2 Timed Meal Reminders
- System checks for registered meals at scheduled periods:
  - **Breakfast**: Reminder after 11:00 if no breakfast logged.
  - **Lunch**: Reminder after 16:00 if no lunch logged.
  - **Supper**: Reminder after 22:00 if no supper logged.
- Notification presents quick choices: Choose Recipe, Choose Snack/Premade, Manual Meal, Nothing.
- Suppressed during quiet hours (22:00 - 08:00), deduplicated, and limited to max 1 reminder per meal period.

---

## 11. Recipe Search by Stock & Filtering

- Allows selecting one or multiple available ingredients.
- Filters and ranks recipes based on current stock:
  1. Fully makeable recipes with available stock quantities.
  2. Recipes using ingredients approaching expiry (reduces food waste).
  3. Recipes missing 1 or 2 items (flagged clearly).
- Filter by Meal Type (Breakfast, Lunch, Supper, Snack).

---

## 12. Cross-System Integrations

### 12.1 Health Integration
- Health CONSUMES Food meal history, nutrition snapshots, meal timing, hydration, caffeine, and alcohol logs.
- Health uses Food data for wellbeing trends, recovery state, cognitive focus analysis, and AI evaluations. Food remains sole data owner.

### 12.2 Exercise Context
- Recipe recommendation engine considers physical activity, exercise intensity, and recovery needs from Health.

### 12.3 Mind / Study Context
- Recipe recommendation engine considers study workload, exam schedules, and focus demands from Planning.
- Uses non-causal language (e.g. "may support focus", "is consistent with study preparation").

---

## 13. AI Recipe Recommender

- Provides **Daily** and **Weekly** meal recommendations.
- **Inputs**: Health recovery state, sleep, activity/exercise, study workload (Planning), calendar commitments, current stock batches, expiring ingredients, recipe nutrition, dietary preferences, and available cooking time.
- Every recommendation includes explainable rationale (e.g. "Uses spinach expiring tomorrow and requires only 20 minutes before your scheduled study session").
- Includes non-medical disclaimers. Displays "Insufficient information" when context is sparse.

---

## 14. Food Analytics

- Analyzes meal timing consistency, daily/weekly macro/micro nutrient intake, frequently consumed foods, and food waste/spoilage patterns.
- Strictly distinguishes **Observation** from **Association** from **Causation**.

---

## 15. Shopping List System (Final Phase)

- **Implementation Gate**: Implemented strictly after all core Stock, Recipe, Cooking, and Meal Register functionality is 100% complete and verified.
- **Capabilities**:
  - Calculates missing ingredient quantities from out-of-stock items, low-stock thresholds, planned recipes, and weekly AI recommendations.
  - Distinguishes **NEEDED** items from **OPTIONAL** items.
  - Manual shopping list item entry and editing.
  - "Mark Purchased" action converts purchased items directly into new Stock Batches (capturing quantity, price, supplier, brand, and expiry).

---

## 16. Privacy, Security, Accessibility & Performance

- **Privacy**: No sensitive health or dietary details logged in cleartext system logs. No privileged API keys in APK.
- **Performance**: Heavy calculations, JSON serialization, and camera/AI work execute on `Dispatchers.IO`. Zero main-thread UI stalls. Lazy list key stability enforced.
- **Accessibility**: Full ContentDescription support on stock items, buttons, radar graphs, camera preview controls, and dialogs.
