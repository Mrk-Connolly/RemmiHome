# AI Integration Contract (`AI-01`)

## 1. Identity and status
* **Identifier**: `AI-01`
* **Name**: AI Integration Contract
* **Status**: Approved
* **Owner**: Architecture Team

## 2. Purpose
Defines provider abstraction, tool execution, permissions, security boundaries, and authorization protocols for AI actions.

## 3. AI Security Boundary & Ownership
- **Provider:** Keep SDKs and request formats inside provider adapters (e.g., `core/ai/chat/`).
- **Read-Only Access:** Gemini has READ-ONLY access to JSON data files for analysis and logging.
- **Write Restrictions:** AI MUST NOT directly invoke repository save methods or write to files.
- **Confirmation Workflow:** Any write/action request (e.g., "Add task") MUST construct a `PendingDraftAction`. The UI MUST present a User Save Confirmation Popup (`PendingActionConfirmDialog`). Writes are executed strictly by Core Application logic only after explicit user confirmation.

## 4. Execution Semantics
- AI models are untrusted input sources.
- Typed tool requests are parsed, validated (`RemmiAiCommandValidator`), and checked for user permissions before conversion into a draft action.

## 5. Transparency
- The AI context and error logging (`GeminiErrorLogger`, `GeminiLogReader`) MUST keep secrets out of persistent user-visible data and logs.