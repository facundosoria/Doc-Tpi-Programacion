-- EP-05 (histórico multi-turno del tutor, revisitando la decisión de no portarlo — ver
-- docs/estado-implementacion/ep-05/conversations.md) y EP-09 (RAG y consulta de material).
-- Portado y adaptado de demoLLMSpringAi/BE/src/main/resources/db/migration/V1__init_pgvector.sql:
-- UUID nativo en vez de VARCHAR(64), y `course_cohort_id` como partición obligatoria en cada
-- tabla (AGENTS.md §2), que la demo no tenía por ser single-tenant.

CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_cohort_id UUID NOT NULL,
  learner_id UUID NOT NULL,
  challenge_id UUID,
  titulo VARCHAR(255) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX conversations_learner_idx ON conversations (learner_id, created_at DESC);
CREATE INDEX conversations_course_idx ON conversations (course_cohort_id, created_at DESC);

CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  rol VARCHAR(10) NOT NULL CHECK (rol IN ('alumno', 'tutor')),
  contenido TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX messages_conversation_idx ON messages (conversation_id, created_at);

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rag_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_cohort_id UUID NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_size_bytes BIGINT NOT NULL,
  page_count INT NOT NULL,
  chunk_count INT NOT NULL,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  preview_text TEXT,
  -- Decisión: solo bytes en DB, sin filesystem (no multi-instancia-safe) — ver plan de EP-09.
  pdf_bytes BYTEA,
  -- Retirar una fuente es borrado lógico: deja de usarse en búsquedas pero conserva su
  -- historial de auditoría (docs/epicas/ep-09.md).
  active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX rag_documents_course_idx ON rag_documents (course_cohort_id, uploaded_at DESC);

CREATE TABLE rag_chunks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL REFERENCES rag_documents(id) ON DELETE CASCADE,
  document_name VARCHAR(255) NOT NULL,
  page_number INT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  -- Dimensión 768 = text-embedding-004 (Google), el proveedor de referencia de la demo. El
  -- adaptador real de embeddings todavía no está conectado (solo FakeEmbeddingAdapter, ver
  -- LLM-S01-H10) — si el proveedor real cambia de dimensión, esta columna requiere otra migración.
  embedding vector(768)
);
CREATE INDEX rag_chunks_document_idx ON rag_chunks (document_id);
CREATE INDEX rag_chunks_hnsw_idx ON rag_chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- LLM-S01-H10 (V13) declaró function_model_config.function con un CHECK cerrado a 4 valores.
-- EP-09 necesita una quinta función (embedding) que no genera texto — se resuelve por
-- EmbeddingPort, no por ModelInvocationPort, pero comparte la misma tabla función→proveedor+modelo.
ALTER TABLE function_model_config DROP CONSTRAINT function_model_config_function_check;
ALTER TABLE function_model_config ADD CONSTRAINT function_model_config_function_check
  CHECK (function IN ('tutor', 'evaluator', 'moderator', 'generator', 'embedding'));

-- ─────────────────────────────────────────────────────────────────────────────
-- Integración main↔dev (2026-09-21): la V26 (provider_spi_modular_architecture)
-- elimina function_model_config.provider / model_id / model_version y exige
-- model_deployment_id NOT NULL contra un despliegue real. Esta migración corre
-- DESPUÉS de la V26, así que la semilla original con el proveedor 'fake' ya no
-- es representable: la función se asigna ahora por el endpoint de administración
-- (PUT /api/llm/admin/model-assignments/{function}) sobre un despliegue creado
-- desde una credencial. Se conserva el ensanche del CHECK, que sigue siendo
-- necesario para la función 'embedding' de EP-09.
-- ─────────────────────────────────────────────────────────────────────────────
-- (semilla 'embedding' con proveedor fake retirada por la V26)
