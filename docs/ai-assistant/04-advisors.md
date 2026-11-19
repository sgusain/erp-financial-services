# 04. Advisors (Course 1 part 5)

## Goal
Add cross-cutting behavior around every model call using advisors.

## Steps
1. Add the built-in logging advisor (dev profile only).
2. `PiiMaskingAdvisor` (custom, ordered first): before the prompt is sent, mask account numbers and emails (keep last 4 digits). Keep a per-request map so the response can be un-masked only if needed. Make the patterns configurable.
3. `TokenUsageAdvisor` (custom): after the call, read token usage from response metadata, log it with the correlation id and user id, and record Micrometer counters `ai.tokens.input` and `ai.tokens.output` tagged with provider.
4. Register advisors on the default `ChatClient` with a clear order (PII mask, memory later, logging, token usage).

## Done when
- A prompt containing a 12-digit account number reaches the model masked (assert with a stub model that captures the prompt).
- Token metrics appear at `/actuator/metrics`. Expose `metrics` in the management include list for this service.
- Unit tests for the masking patterns, including edge cases (no digits, many numbers, emails).

## Interview notes
- "An advisor is like a servlet filter for AI calls. I would use one to mask account numbers before the prompt leaves my system and another to track token cost."
