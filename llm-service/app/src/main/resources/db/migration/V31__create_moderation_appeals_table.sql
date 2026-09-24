-- LLM-S12-H01 · EP-08: Apelar un mensaje que bloqueó la moderación.
-- Tablas de incidentes y apelaciones con constraints de integridad referencial e índice único para idempotencia.

CREATE TABLE IF NOT EXISTS moderation_incidents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id VARCHAR(255),
  user_id VARCHAR(255) NOT NULL,
  course_id VARCHAR(255),
  status VARCHAR(50) NOT NULL DEFAULT 'BLOCK',
  reason_code VARCHAR(100),
  message_preview TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS moderation_incidents_user_idx ON moderation_incidents (user_id);

CREATE TABLE IF NOT EXISTS moderation_appeals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id UUID NOT NULL UNIQUE,
  user_id VARCHAR(255) NOT NULL,
  appeal_reason VARCHAR(1000) NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_moderation_appeals_incident FOREIGN KEY (incident_id) REFERENCES moderation_incidents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS moderation_appeals_incident_idx ON moderation_appeals (incident_id);
CREATE INDEX IF NOT EXISTS moderation_appeals_user_idx ON moderation_appeals (user_id);
CREATE INDEX IF NOT EXISTS moderation_appeals_status_idx ON moderation_appeals (status);
