# Data Contract (`DATA-01`)

## 1. Identity and status
* **Identifier**: `DATA-01`
* **Name**: Data Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines data ownership, source-of-truth rules, persistence choices, and migration expectations.

## 3. Persistence Choices
- **No Database:** Remmi will NOT use Room, SQLite, or generic CRUD frameworks.
- **Local-First Storage:** All local domain JSON files live in a single dedicated subfolder `app-private/files/data/` (managed via `storageDir`).
- **DataStore:** Used exclusively for simple preferences (migrating away from `SharedPreferences`).

## 4. Ownership and Source of Truth
- Each Plugin exclusively owns its JSON files (e.g., `FoodPlugin` owns `ingredients.json`, `recipes.json`).
- Features MUST NOT read or write to another feature's JSON file directly.

## 5. Migration Requirements
- `DataDirectoryMigrator` handles file location upgrades.
- Schema changes to JSON formats MUST be idempotent and include a migration plan to prevent data loss.
- Legacy sources MUST NOT be deleted until successful migration and validation are confirmed.

## 6. Security & AI Access
- The AI (Gemini) has READ-ONLY access to JSON data files.
- The AI is STRICTLY PROHIBITED from directly writing to these files. Write operations MUST go through the Draft Template & User Confirmation workflow (`PendingActionConfirmDialog`).