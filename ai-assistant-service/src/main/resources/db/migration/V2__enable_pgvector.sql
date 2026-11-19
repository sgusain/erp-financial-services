-- Needs the pgvector image (pgvector/pgvector:pg15), not plain postgres.
CREATE EXTENSION IF NOT EXISTS vector;

-- Table used by the Spring AI PgVectorStore. The vector size comes from the
-- Flyway placeholder embedding_dimensions (see application.yml). If you change
-- the embedding model, add a new migration and re-embed everything.
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding vector(${embedding_dimensions})
);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING HNSW (embedding vector_cosine_ops);
