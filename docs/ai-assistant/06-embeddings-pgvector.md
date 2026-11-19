# 06. Embeddings and vector search (Course 1 part 8)

## Goal
Semantic search over ERP text using embeddings stored in pgvector.

## Steps
1. Flyway `V2__enable_pgvector.sql`: `CREATE EXTENSION IF NOT EXISTS vector;`. Compose uses the pgvector image (spec 01). Testcontainers must use `pgvector/pgvector:pg15` for this service.
2. Configure the Spring AI pgvector store. Let Flyway own the table (schema init off), dimension matching the embedding model, cosine distance, HNSW index. Pick one embedding model (OpenAI or Ollama `nomic-embed-text`) and write the dimension in config. Changing the model means re-embedding.
3. `EmbeddingService.indexTransaction(id, description, amount, userId)` stores a document with metadata `userId`, `type=transaction`, `sourceId`.
4. `POST /api/ai/index/transactions` loads the caller's recent transactions from transaction-service (Feign, forward the JWT) and indexes them. Idempotent: re-running does not duplicate (delete by sourceId first).
5. `GET /api/ai/search?q=...&limit=5` returns similar items. Always filter by `userId` from the token.

## Done when
- Searching "office supplies" returns stationery or printer items without exact word match (show in a test with fixed vectors from a stub embedding model, plus a manual run notes in the PR).
- Integration test proves user filter: user A never sees user B's items.
- Re-indexing does not create duplicates.

## Interview notes
- "Normal search matches words. Vector search matches meaning. I would use pgvector in the Postgres I already run, so a first version needs no extra database. I always filter by user so one user can not see another's data."
