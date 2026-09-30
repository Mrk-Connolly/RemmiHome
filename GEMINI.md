# Remmi Engineering Context & Rules

These foundational principles apply to every task in Remmi:

* **Inspect existing code before changing it.** Never assume structure or behavior.
* **Reuse existing systems.** Do not duplicate concepts, wrappers, or infrastructure.
* **Make the smallest correct change.** Avoid unnecessary edits or broad scope creep.
* **Do not invent architecture.** Build only what is needed right now; prefer standard Kotlin/Android primitives over layers of abstractions.
* **Do not perform unrelated refactoring.** Focus strictly on the task at hand.
* **Preserve working behavior.** Ensure changes do not break established functionality.
* **Do not add dependencies without justification.** Keep external library usage minimal and justified.
* **Keep Core small.** `core/` contains only fundamental application infrastructure.
* **Keep UI separate from infrastructure.** `ui/` contains Compose views and UI logic; it does not contain system infrastructure.
* **Keep plugin functionality inside plugins.** `plugins/` isolates distinct feature modules.
* **Compile and test changes.** Always verify build status and test results before concluding work.
* **Never claim verification that was not actually performed.** Be explicit about what was run and what passed.
* **Stop and ask/review when an architectural decision is genuinely unclear.** Clarify ambiguity before proceeding.
