# 06. Embeddings and vector search (Course 1 part 8)

## Goal
Semantic search over ERP text using embeddings stored in pgvector.

## Steps
1. Flyway `V2__enable_pgvector.sql`: `CREATE EXTENSION IF NOT EXISTS vector;`. Compose uses the pgvector image (spec 01). Testcontainers must use `pgvector/pgvector:pg15` for this service.
2. Configure the Spring AI pgvector store. Let Flyway own the table (schema init off), dimension matching the embedding model, cosine distance, HNSW index. Pick one embedding model (OpenAI or Ollama `nomic-embed-text`) and write the dimension in config. Changing the model means re-embedding.
3. `TransactionSearchService.index(token, days)` stores one document per transaction with metadata `type=transaction`, `sourceId`, `accountId`. Policy chunks (spec 07) use `type=policy`, so the two never mix.
4. `POST /api/ai/index/transactions?days=90` (ADMIN only, it costs embedding tokens) loads recent transactions from transaction-service (Feign, forward the JWT) and indexes them. Idempotent: it deletes the old `type=transaction` chunks first.
5. `GET /api/ai/search?q=...&limit=5` returns the most similar transactions. ERP data is company-wide (there is no per-user ownership in the ERP), so there is no user filter. Access is controlled by the JWT forwarded to transaction-service and by the ADMIN-only index endpoint.

## Done when
- Searching "office supplies" returns stationery or printer items without exact word match (show in a test with fixed vectors from a stub embedding model, plus a manual run notes in the PR).
- Searching only returns `type=transaction` chunks, never policy chunks.
- Re-indexing does not create duplicates.

## Interview notes
- "Normal search matches words. Vector search matches meaning. I would use pgvector in the Postgres I already run, so a first version needs no extra database. The ERP data is company-wide, so I control access with the user's token and role, not a per-user filter."
