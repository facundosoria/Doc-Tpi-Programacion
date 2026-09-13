-- ==============================================================================
-- MIGRACIÓN DDL: Inicialización de pgvector y tablas RAG para Supabase / PostgreSQL
-- ==============================================================================

-- 1. Habilitar extensión pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabla de Fuentes (Documentos)
CREATE TABLE IF NOT EXISTS rag_documentos (
    document_id VARCHAR(64) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    page_count INT NOT NULL,
    chunk_count INT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    preview_text TEXT
);

-- 3. Tabla de Chunks Vectorizados con dimensión 768 (text-embedding-004)
CREATE TABLE IF NOT EXISTS rag_chunks (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES rag_documentos(document_id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL,
    page_number INT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768)
);

-- 4. Índices para acelerar el filtrado por fuentes y la búsqueda HNSW por distancia coseno
CREATE INDEX IF NOT EXISTS idx_rag_chunks_document_id ON rag_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_hnsw ON rag_chunks USING hnsw (embedding vector_cosine_ops);
