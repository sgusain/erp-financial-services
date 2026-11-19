# 08. Tool calling (Course 1 part 10)

## Goal
Let the model call ERP data through safe, typed Java tools.

## Steps
1. Feign clients in `tool/client/`: `AccountClient`, `TransactionClient`, `BudgetClient` (Eureka names `ACCOUNT-SERVICE`, `TRANSACTION-SERVICE`, `BUDGET-SERVICE`). A Feign request interceptor forwards the caller's JWT. Read endpoints from the existing controllers; do not change other services.
2. `ErpReadTools` with `@Tool` methods and clear descriptions: `listAccounts()`, `getAccount(accountId)`, `listRecentTransactions(accountId, days)`, `getBudgetStatus(fiscalYear)`. A budget has `totalAmount`, `spentAmount`, `fiscalYear` and `status` (ACTIVE, INACTIVE, EXCEEDED). It has no month or category, so the status tool takes a fiscal year and returns percent used. Read tools are separate from the write tool on purpose (the MCP server in spec 10 shows only the read tools).
3. Write tool: `requestBudgetChange(budgetId, newAmount, reason)`. It must not execute directly. It creates a `pending_action` row (Flyway `V3`) with status `PENDING` and returns a confirmation token. `POST /api/ai/actions/{id}/confirm` (user clicks confirm) executes it through the budget API: read the budget, set the new `totalAmount`, `PUT /api/budgets/{id}`.
4. Safety rules: the ERP has no per-user ownership of accounts, budgets or transactions (the data is company-wide). So safety comes from: (a) every ERP call forwards the user's own JWT, so the ERP services and their roles decide (for example, deleting is ADMIN only); (b) the user id for pending actions comes from the JWT, never from tool arguments or model text; (c) a pending action can only be confirmed by the user who created it; (d) results are limited (max 50 rows); (e) Feign timeouts.
5. Wire tools into the ChatClient for chat. Log every tool call with correlation id, user id, tool name and result size (not full data).

## Done when
- "What is the balance of account 1?" triggers `getAccount` and answers from real data.
- Test: user B cannot confirm or see user A's pending action.
- Test: the write tool only creates a pending action. Execution happens only after confirm.
- Feign failure returns a safe tool error, not a stack trace.

## Interview notes
- "The model does not run code. It picks a tool and my app runs it. I forward the user's own token so the ERP roles still apply, and any write needs the user to confirm."
