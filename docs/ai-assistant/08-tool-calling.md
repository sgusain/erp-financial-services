# 08. Tool calling (Course 1 part 10)

## Goal
Let the model call ERP data through safe, typed Java tools.

## Steps
1. Feign clients in `tool/client/`: `AccountClient`, `TransactionClient`, `BudgetClient` (Eureka names `ACCOUNT-SERVICE`, `TRANSACTION-SERVICE`, `BUDGET-SERVICE`). A Feign request interceptor forwards the caller's JWT. Read endpoints from the existing controllers; do not change other services.
2. `ErpTools` with `@Tool` methods and clear descriptions: `getAccountBalance(accountId)`, `listRecentTransactions(accountId, days)`, `getBudgetStatus(month)`.
3. Write tool: `requestBudgetChange(budgetId, newAmount, reason)`. It must not execute directly. It creates a `pending_action` row (Flyway `V3`) with status `PENDING` and returns a confirmation token. `POST /api/ai/actions/{id}/confirm` (user clicks confirm) executes it through the budget API.
4. Safety rules: user id and account ownership come from the JWT context, never from tool arguments or model text. If the model asks for an account the user does not own, return a tool error "not found". Limit results (max 50 rows). Round-trip timeouts on Feign.
5. Wire tools into the ChatClient for chat. Log every tool call with correlation id, user id, tool name and result size (not full data).

## Done when
- "What is my balance?" triggers `getAccountBalance` and answers from real data.
- Test: a prompt that asks for another user's account returns no data.
- Test: the write tool only creates a pending action. Execution happens only after confirm.
- Feign failure returns a safe tool error, not a stack trace.

## Interview notes
- "The model does not run code. It picks a tool and my app runs it. I take the user id from the token, never from the model, and any write needs the user to confirm."
