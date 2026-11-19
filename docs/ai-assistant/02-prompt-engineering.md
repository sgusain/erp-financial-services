# 02. Prompt engineering (Course 1 part 4, Course 2 section 3)

## Goal
Make answers reliable with a system prompt, templates, examples and a two-step prompt chain.

## Steps
1. Put prompts in `src/main/resources/prompts/` as files: `system-finance-assistant.st`, `explain-transaction.st`, `classify-intent.st`. No long prompt strings in Java.
2. System prompt rules: you are an ERP finance assistant; answer only from provided data; if data is missing say "I don't know"; never invent numbers; keep answers short.
3. `POST /api/ai/explain-transaction` takes a transaction JSON and uses the template with `PromptTemplate`.
4. Prompt chain in `ChatService`: step 1 classifies intent (`BALANCE`, `TRANSACTIONS`, `BUDGET`, `POLICY`, `OTHER`), step 2 answers using the intent. Log both steps with the correlation id.
5. Add `src/test/resources/samples/transactions-20.json` (20 sample transactions with expected categories).
6. Add a small comparison test class (disabled by default, enabled with a property) that runs the same 20 items with zero-shot, few-shot (5 examples) and many-shot (15 examples) and prints accuracy. It is a learning tool, not a CI test.
7. Hallucination check: ask about a transaction that does not exist and confirm the answer is "I don't know".

## Done when
- Prompts are in files and loaded by name.
- Chain works and both steps appear in logs.
- Unit tests check the rendered prompts (template variables filled, no leftover placeholders).
- Comparison output shows few-shot beats zero-shot on the sample set (note the numbers in the PR description).

## Interview notes
- Zero-shot: no examples. Few-shot: a few examples. Many-shot: many examples. Chain of thought: ask the model to reason step by step.
- "To reduce hallucination I would give real data, tell the model to say I don't know, and use examples for the format."
