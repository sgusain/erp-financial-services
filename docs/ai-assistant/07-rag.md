# 07. RAG (Course 1 part 9)

## Goal
Answer policy questions from ERP documents, with sources, and refuse when there is no match.

## Steps
1. Add 3 sample documents in `src/main/resources/policies/`: `expense-policy.md`, `approval-limits.md`, `budget-rules.md`. Make them realistic (limits, who approves, deadlines).
2. Ingestion pipeline (`IngestionService`): read, split (token-based, chunk size and overlap in config), add metadata (`type=policy`, `source`, `version`), embed, store. `POST /api/ai/ingest/policies` (admin role only) re-ingests: delete old chunks for that source first.
3. Answering: use the Spring AI retrieval advisor with `topK` and `similarityThreshold` in config and filter `type == 'policy'`.
4. System prompt for policy answers: answer only from the context, cite the source file name, say "I don't know" if the context does not contain the answer.
5. Return `{ answer, sources: [..] }` from `POST /api/ai/ask-policy`.
6. Evaluation: `src/test/resources/rag-eval.json` with 20 questions (15 answerable, 5 not) and expected source file. A disabled-by-default test reports hit rate and refusal rate. Use it to tune chunk size, topK and threshold; note results in the PR.

## Done when
- Answerable questions cite the right file. Unanswerable ones return "I don't know".
- Changing a policy file and re-ingesting changes the answer (no stale chunks).
- Unit tests cover chunk metadata and the admin-only rule.

## Interview notes
- "RAG finds the right documents first and then lets the model answer from them. It is cheaper than fine-tuning and answers can show sources. When a policy changes I only re-ingest that document. I would measure it with a fixed question set."
