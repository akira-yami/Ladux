-- Production does not let the AI library create application tables at startup.
-- Preserve existing embeddings on upgraded databases.
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSON,
    embedding vector(3072)
);
