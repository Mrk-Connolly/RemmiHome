# Remmi Food System - Implementation Roadmap

**Architecture:** Local-first, no database (no Room/SQLite), no Remmi backend, JSON file persistence  
**Primary Surfaces:** Home Tools (Stock, Recipes), Home Quick Actions, Meal Register, Health Integration

---

## Roadmap Phases & Task Breakdown

### PHASE 1 — INSPECTION & CONTRACT DEFINITION
- **F01**: Inspect existing repository architecture and file organization.
- **F02**: Inspect Home launcher shell and quick action architecture (`com.remmi.ui.home`).
- **F03**: Inspect Health Workspace plugin and existing nutrition/food models (`com.remmi.plugins.health`).
- **F04**: Inspect local JSON persistence patterns (`PlanningTaskRepository`, `ClassRepository`).
- **F05**: Inspect AI capability infrastructure (`RemmiAiCapability`, `DefaultAiCapability`).
- **F06**: Inspect notification capabilities (`RemmiNotificationCapability`).
- **F07**: Inspect camera capability requirements and platform integration boundaries.
- **F08**: Inspect Home quick action patterns and event bus commands.
- **F09**: Create/Update `docs/FOOD_SPECIFICATION.md` with complete domain requirements.
- **F10**: Create/Update `docs/FOOD_ROADMAP.md`.

---

### PHASE 2 — INGREDIENT FOUNDATION
- **F11**: Implement `Ingredient` data model (`id`, `name`, `foodGroup`, `icon`, `allergens`, `densityGramsPerMl`, `nutritionPer100g`).
- **F12**: Implement `Supplier` data model (`id`, `name`, `notes`).
- **F13**: Implement `Brand` data model (`id`, `name`).
- **F14**: Implement `NutritionProfile` data model (fiber, fats, carbs, protein, salt, sodium, sugars, calories, vitamins, minerals).
- **F15**: Implement `Unit` enum (`UNITS`, `KG`, `G`, `LITERS`, `ML`, `CUPS`, `TEASPOONS`, `TABLESPOONS`).
- **F16**: Implement `UnitConversion` engine (`kg ↔ g`, `L ↔ ml`, density-based volume-to-weight).
- **F17**: Implement `FoodGroup` enum (`VEGETABLES`, `FRUITS`, `MEAT`, `FISH`, `DAIRY`, `GRAINS`, `LEGUMES`, `EGGS`, `OILS_FATS`, `SWEETS`, `SNACKS`, `PREMADE`, `OTHER`).
- **F18**: Add Allergen metadata flags to `Ingredient`.
- **F19**: Build local JSON persistence repository for Ingredients (`food_ingredients.json`) on `Dispatchers.IO`.
- **F20**: Build Ingredient CRUD operations in repository.
- **F21**: Build Supplier CRUD operations in repository.
- **F22**: Build Brand CRUD operations in repository.
- **F23**: Build Nutrition storage and validator in repository.

---

### PHASE 3 — STOCK MANAGEMENT & STOCK MAIN UI
- **F24**: Implement `StockBatch` data model (`id`, `ingredientId`, `quantity`, `unit`, `expiryDateIso`, `supplierId`, `brandId`, `price`, `qualityNotes`).
- **F25**: Implement `StockTransaction` data model (`id`, `batchId`, `type`, `quantityDeducted`, `timestampIso`, `reason`).
- **F26**: Implement stock balance calculator across batches for an ingredient.
- **F27**: Implement FIFO (First In, First Out) batch deduction logic preferring oldest non-expired batch.
- **F28**: Implement expiry-aware stock ordering engine (1. Expired, 2. Expiring soon [< 7 days], 3. Later, 4. No expiry).
- **F29**: Implement waste / spoilage logging and transaction recording (`EXPIRED`, `SPOILED`, `DISCARDED`).
- **F30**: Implement stock count correction transaction type (`CORRECTED`).
- **F31**: Implement food group filter chips for stock.
- **F32**: Build `StockMainScreen` UI (Home Tool) with expiry ordering, stock rows, and quick consume action.
- **F33**: Build `IngredientDetailScreen` UI showing active batches, metadata, and nutrition graph.
- **F34**: Build `HexagonalNutritionRadar` Compose component for 6 primary axes (Fiber, Fats, Carbs, Protein, Salt/Sodium, Sugars).
- **F35**: Build Ingredient Edit Screen.
- **F36**: Build Ingredient Delete workflow with stock dependency check.
- **F37**: Build Manual Add Stock flow (PLUS button dialog/screen).
- **F38**: Implement multi-unit entry and approximate unit display in Add Stock.
- **F39**: Implement batch-specific details entry (supplier, brand, price, expiry).
- **F40**: Build Smart Nutrition Search confirmation screen with editable fields.

---

### PHASE 4 — CAMERA INGREDIENT & RECEIPT IMPORT
- **F41**: Conduct Android Camera capability audit (`RemmiCameraCapability`).
- **F42**: Build camera preview and image capture interface for ingredient lists/receipts.
- **F43**: Build text/OCR extraction pipeline for captured receipt images.
- **F44**: Implement Confidence Model (`CONFIDENT`, `UNCERTAIN`, `UNKNOWN`).
- **F45**: Implement ingredient name fuzzy matching against existing catalogue.
- **F46**: Implement supplier detection from receipt header text.
- **F47**: Implement brand detection from line item text.
- **F48**: Implement price detection and numerical parsing.
- **F49**: Implement quantity and unit parsing.
- **F50**: Build Camera Import Confirmation UI displaying extracted items with confidence status.
- **F51**: Build correction flow for uncertain or unknown camera items.
- **F52**: Build confirmed batch creation workflow saving verified items to stock.

---

### PHASE 5 — RECIPES WORKSPACE
- **F53**: Implement `Recipe` data model (`id`, `title`, `imageUri`, `description`, `servings`, `mealType`, `totalDurationMins`, `cookingDurationMins`, `ovenDurationMins`, `restingDurationMins`).
- **F54**: Implement `RecipeIngredient` model (`ingredientId`, `quantity`, `unit`, `notes`).
- **F55**: Implement `RecipeStep` model (`stepNumber`, `instruction`, `timerMinutes`).
- **F56**: Implement `MealType` enum (`BREAKFAST`, `LUNCH`, `SUPPER`, `SNACK`).
- **F57**: Build local JSON persistence for Recipes (`food_recipes.json`).
- **F58**: Implement recipe nutrition calculator (computing Total and Per-Serving nutrition from ingredients).
- **F59**: Implement recipe serving scaling engine (scaling ingredient amounts dynamically).
- **F60**: Build `RecipeMainScreen` UI (Home Tool) displaying recipe cards with Cook action.
- **F61**: Build Recipe List grid view with images and durations.
- **F62**: Build Meal Type and Preparation Time filters.
- **F63**: Implement recipe search based on available stock.
- **F64**: Implement expiry-aware recipe ranking (prioritizing recipes that use expiring stock).
- **F65**: Build Add Recipe Screen with step-by-step ingredient picker and timer inputs.
- **F66**: Build Recipe Detail Screen view-only layout.
- **F67**: Build Recipe Nutrition Radar graph (per-serving).
- **F68**: Build Recipe Edit Screen.
- **F69**: Build Recipe Delete workflow.

---

### PHASE 6 — COOKING ENGINE
- **F70**: Build Cook Action dialog prompting for target servings.
- **F71**: Implement serving selection slider/stepper.
- **F72**: Calculate scaled required ingredient quantities.
- **F73**: Check current stock sufficiency for each required ingredient.
- **F74**: Build view-only Cooking Confirmation Screen.
- **F75**: Implement atomic FIFO stock deduction across batches upon confirmation.
- **F76**: Execute atomic stock transaction update saving batch updates.
- **F77**: Record `CookingEvent` log (`recipeId`, `timestamp`, `servingsPrepared`, `consumedBatches`).
- **F78**: Implement Cancel behavior (zero stock impact).
- **F79**: Implement Insufficient Stock warning screen highlighting missing ingredient amounts.

---

### PHASE 7 — PREMADE FOODS & SNACK LOGGING
- **F80**: Implement `PremadeFood` model extension for Ingredients (cooking method, oven temp, air fryer mins).
- **F81**: Build Premade Food detail and heating instructions UI.
- **F82**: Add premade cooking metadata fields in Ingredient Edit.
- **F83**: Add snack flag to Ingredients.
- **F84**: Build Home Quick Action "Eat Snack" component.
- **F85**: Build Snack quantity picker (Whole, 1/2, 1/4, 1/8 options).
- **F86**: Define explicit participant/servings semantics ("Servings consumed by user").
- **F87**: Execute stock deduction and meal logging for Home snack consumption.

---

### PHASE 8 — MEAL REGISTER & NUTRITION HISTORY
- **F88**: Implement `MealRecord` data model (`id`, `name`, `timestampIso`, `mealType`, `source`, `quantity`, `nutritionSnapshot`).
- **F89**: Implement `NutritionSnapshot` model holding frozen historical nutrients received.
- **F90**: Implement `MealSource` enum (`RECIPE`, `PREMADE`, `MANUAL`).
- **F91**: Implement `MealType` categorization.
- **F92**: Enforce human-readable Meal Name generation ("Chicken Rice Bowl", "Greek Yogurt").
- **F93**: Build `MealHistoryScreen` UI.
- **F94**: Build Manual Meal Logging dialog.
- **F95**: Build Breakfast logging shortcut.
- **F96**: Build Lunch logging shortcut.
- **F97**: Build Supper logging shortcut.
- **F98**: Build Snack logging shortcut.
- **F99**: Build "Nothing Eaten" log handler.
- **F100**: Implement Daily Nutrition Totals calculator from Meal records.
- **F101**: Implement Weekly Nutrition Totals calculator.
- **F102**: Implement Monthly Nutrition Totals calculator.
- **F103**: Implement Nutrition breakdown by Meal Type.

---

### PHASE 9 — MEAL NOTIFICATIONS & REMINDERS
- **F104**: Implement Breakfast notification trigger (11:00 AM check).
- **F105**: Implement Lunch notification trigger (16:00 PM check).
- **F106**: Implement Supper notification trigger (22:00 PM check).
- **F107**: Implement notification deduplication preventing repeated alerts for same period.
- **F108**: Implement Quiet Hours suppression (22:00 - 08:00).
- **F109**: Implement notification dismissal handling.
- **F110**: Implement notification quick actions ("Choose Recipe", "Choose Snack", "Manual Meal", "Nothing").

---

### PHASE 10 — HEALTH / EXERCISE / MIND INTEGRATION
- **F111**: Establish Food → Health clean data boundary (Health reads Meal Records).
- **F112**: Export Meal Timing data to Health.
- **F113**: Export Nutrition History snapshot data to Health.
- **F114**: Export Food Patterns summary to Health.
- **F115**: Export Caffeine intake logs to Health.
- **F116**: Export Alcohol intake logs to Health.
- **F117**: Integrate Exercise recovery context into Food recommendations.
- **F118**: Integrate Mind / Study workload context into Food recommendations.
- **F119**: Pass Food context to Health AI Evaluator.

---

### PHASE 11 — AI RECIPE RECOMMENDER
- **F120**: Verify AI provider and API key setup (`RemmiAiCapability`).
- **F121**: Implement recommendation prompt contract (`buildRecipeRecommendationPrompt`).
- **F122**: Incorporate Health recovery state into AI prompt.
- **F123**: Incorporate Exercise intensity into AI prompt.
- **F124**: Incorporate Study workload (Planning) into AI prompt.
- **F125**: Incorporate Calendar commitments into AI prompt.
- **F126**: Incorporate active Stock Batches into AI prompt.
- **F127**: Incorporate expiring stock urgency into AI prompt.
- **F128**: Incorporate recipe target nutrition into AI prompt.
- **F129**: Implement Daily Recipe Recommendations engine.
- **F130**: Implement Weekly Recipe Plan Recommender.
- **F131**: Add explainability text to recommendations.
- **F132**: Add confidence ratings and non-medical disclaimers.
- **F133**: Implement offline / AI failure fallback recommendations.

---

### PHASE 12 — FOOD ANALYTICS
- **F134**: Implement meal timing consistency analysis.
- **F135**: Implement macro and micro nutrient intake trend analysis.
- **F136**: Implement food consumption pattern detector.
- **F137**: Implement food waste and spoilage frequency analyzer.
- **F138**: Implement expiry waste cost analysis.
- **F139**: Identify frequently consumed vs wasted ingredients.
- **F140**: Calculate Food → Focus/Energy non-causal associations.
- **F141**: Implement uncertainty handling in analytics reporting.

---

### PHASE 13 — PRIVACY & SECURITY
- **F142**: Conduct privacy audit on Food JSON storage.
- **F143**: Audit Health data exposure boundary.
- **F144**: Minimize AI payload size and sanitize prompt context.
- **F145**: Audit system logs to ensure zero cleartext food/health logging.
- **F146**: Build clear user data deletion handler for Food data.
- **F147**: Build historical meal record correction tool.
- **F148**: Enforce historical meal record immutability.

---

### PHASE 14 — ACCESSIBILITY
- **F149**: Add screen reader ContentDescriptions to Stock Main UI.
- **F150**: Add ContentDescriptions to Recipe list and Cook action dialog.
- **F151**: Add accessibility semantics to Meal History.
- **F152**: Add accessible tabular text alternative to Hexagonal Nutrition Radar Graph.
- **F153**: Add accessibility support to Camera Import interface.
- **F154**: Add explicit action descriptions to meal notifications.

---

### PHASE 15 — PERFORMANCE OPTIMIZATION
- **F155**: Optimize Stock list rendering with stable LazyColumn keys.
- **F156**: Optimize Recipe list grid performance.
- **F157**: Optimize Meal History lazy list allocations.
- **F158**: Benchmark nutrition aggregation calculations (< 5ms on `Dispatchers.IO`).
- **F159**: Optimize Camera image frame processing off main thread.
- **F160**: Enforce asynchronous execution for AI recommendations.
- **F161**: Verify zero startup latency impact from Food plugin initialization.

---

### PHASE 16 — AUTOMATED TESTING SUITE
- **F162**: Write unit tests for `Ingredient` model and JSON serialization.
- **F163**: Write unit tests for `UnitConversion` engine (`kg ↔ g`, `L ↔ ml`, density).
- **F164**: Write unit tests for FIFO stock batch deduction math.
- **F165**: Write unit tests for expiry-aware stock ordering.
- **F166**: Write unit tests for `StockTransaction` logs.
- **F167**: Write unit tests for waste/spoilage transaction recording.
- **F168**: Write unit tests for per-100g nutrition scaling math.
- **F169**: Write unit tests for Recipe nutrition calculations.
- **F170**: Write unit tests for Recipe serving scaling.
- **F171**: Write unit tests for Cooking Event atomic stock updates.
- **F172**: Write unit tests for `MealRecord` creation and historical immutability.
- **F173**: Write unit tests verifying ingredient nutrition edits DO NOT alter historical meal records.
- **F174**: Write unit tests for Home snack logging.
- **F175**: Write unit tests for meal reminder notification triggers.
- **F176**: Write unit tests for local JSON repository persistence.
- **F177**: Write Compose UI tests for Stock Main Screen.
- **F178**: Write Compose UI tests for Recipe Main Screen.
- **F179**: Write Compose UI tests for Meal History Screen.
- **F180**: Write tests for Camera import confidence classification.
- **F181**: Write unit tests for Food → Health data export boundary.
- **F182**: Write contract tests for AI recipe recommendation prompts.
- **F183**: Write offline fallback execution tests.
- **F184**: Write failure handling and missing stock tests.

---

### PHASE 17 — END-TO-END VERIFICATION
- **F185**: E2E test: Create new Ingredient in catalogue.
- **F186**: E2E test: Add multiple stock batches with different expiries and prices.
- **F187**: E2E test: Verify single ingredient catalogue item displays aggregated stock.
- **F188**: E2E test: Verify FIFO stock deduction uses oldest batch first.
- **F189**: E2E test: Create new Recipe with required ingredients.
- **F190**: E2E test: Cook Recipe with 4 target servings.
- **F191**: E2E test: Verify exact stock deduction across ingredient batches.
- **F192**: E2E test: Verify Cooking Event recorded.
- **F193**: E2E test: Consume prepared food as a Meal.
- **F194**: E2E test: Verify `MealRecord` creation.
- **F195**: E2E test: Verify frozen nutrition snapshot in `MealRecord`.
- **F196**: E2E test: Edit ingredient nutrition in catalogue.
- **F197**: E2E test: Verify historical meal record nutrition snapshot remains 100% unchanged.
- **F198**: E2E test: Consume snack from Home quick action.
- **F199**: E2E test: Verify Meal History updates.
- **F200**: E2E test: Verify Health workspace reflects consumed nutrition.
- **F201**: E2E test: Verify expiry-aware recipe stock search ranking.
- **F202**: E2E test: Verify timed meal notification trigger and response.
- **F203**: E2E test: Verify AI recipe recommendation execution.
- **F204**: E2E test: Verify 100% offline operation.
- **F205**: E2E test: Verify process kill and restart recovery from JSON files.

---

### PHASE 18 — SHOPPING LIST (FINAL PHASE)
- **F206**: Define Shopping List requirements and gate check (all Phases 1-17 verified).
- **F207**: Implement `ShoppingListItem` model (`id`, `ingredientId`, `quantityNeeded`, `unit`, `isOptional`, `sourceRecipeId`, `isPurchased`).
- **F208**: Build manual Shopping List item creation UI.
- **F209**: Implement missing stock calculation engine from recipes/meals.
- **F210**: Implement recipe-required ingredient generator for Shopping List.
- **F211**: Implement planned-meal ingredient generator.
- **F212**: Implement low-stock threshold generator.
- **F213**: Implement shopping quantity aggregator.
- **F214**: Build `ShoppingListScreen` UI.
- **F215**: Implement "Mark Purchased" action with checkbox.
- **F216**: Build auto-conversion of purchased shopping items into new Stock Batches.
- **F217**: Prompt for supplier, brand, price, and expiry during purchase conversion.
- **F218**: Build Shopping History log.
- **F219**: Verify Shopping → Stock integration.
- **F220**: Verify Shopping → Recipe integration.
- **F221**: Verify Shopping → Health/meal planning integration.
- **F222**: Write automated unit tests for Shopping List calculations and purchase conversions.

---

### FINAL AUDIT & RELEASE VERIFICATION
- **F223**: Perform Functional Specification compliance audit.
- **F224**: Perform Ownership boundary audit.
- **F225**: Perform Architecture clean code audit.
- **F226**: Perform Local JSON Persistence audit.
- **F227**: Perform Historical Meal Integrity audit.
- **F228**: Perform Nutrition calculation accuracy audit.
- **F229**: Perform FIFO deduction logic audit.
- **F230**: Perform Security audit.
- **F231**: Perform Privacy audit.
- **F232**: Perform Main-Thread performance audit.
- **F233**: Perform Accessibility compliance audit.
- **F234**: Perform Offline resilience audit.
- **F235**: Perform Failure recovery audit.
- **F236**: Verify zero database (no Room/SQLite) usage.
- **F237**: Verify zero duplicate ownership across systems.
- **F238**: Perform Shopping List final audit.
- **F239**: Final Food System Release Verification.
