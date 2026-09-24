-- LLM-S11-H01 · EP-08: Moderación integrada de mensajes de chat.
-- Tabla append-only de auditoría e idempotencia para decisiones de moderación (POST /moderation/v1/decisions).
-- Si decision == 'ALLOW', message_text es estrictamente NULL (minimización de datos, CA2).

CREATE TABLE IF NOT EXISTS moderation_decisions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id VARCHAR(255) NOT NULL UNIQUE,
  course_id VARCHAR(255),
  sender_role VARCHAR(50),
  decision VARCHAR(50) NOT NULL,
  reason_code VARCHAR(100) NOT NULL,
  classifier_used VARCHAR(100) NOT NULL,
  latency_ms BIGINT NOT NULL,
  incident_id UUID,
  content_hash VARCHAR(64) NOT NULL,
  message_text TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS moderation_decisions_created_idx ON moderation_decisions (created_at DESC);
