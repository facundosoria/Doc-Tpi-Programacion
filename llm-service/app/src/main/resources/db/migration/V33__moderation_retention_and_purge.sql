-- LLM-S13-H02 · EP-08: Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue.
-- Tabla de políticas de retención versionadas y soporte para purga irreversible (Data Minimization / GDPR).

-- 1. Campos de purga y flexibilización de constraints de texto para permitir destrucción irreversible a NULL
ALTER TABLE llm.moderation_incidents
  ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS moderation_incidents_purged_created_idx
  ON llm.moderation_incidents (purged_at, created_at);

ALTER TABLE llm.moderation_decisions
  ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ;

ALTER TABLE llm.moderation_appeals
  ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ,
  ALTER COLUMN appeal_reason DROP NOT NULL;

ALTER TABLE llm.moderation_resolutions
  ADD COLUMN IF NOT EXISTS purged_at TIMESTAMPTZ,
  ALTER COLUMN resolution_reason DROP NOT NULL;

-- 2. Tabla de políticas de retención configurables y versionadas (T1 / CA4)
CREATE TABLE IF NOT EXISTS llm.moderation_retention_policies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_type VARCHAR(50) NOT NULL,
  severity VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
  retention_days INT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
  version BIGINT NOT NULL DEFAULT 1,
  CONSTRAINT uq_moderation_retention_policy UNIQUE (incident_type, severity)
);

CREATE INDEX IF NOT EXISTS moderation_retention_policies_type_idx
  ON llm.moderation_retention_policies (incident_type);

-- 3. Semillas iniciales según especificación de producto (T1):
-- Incidentes BLOCK confirmados sin apelación: 30 días
-- Incidentes apelados con resolución REVERSED (falsos positivos para calibración): 90 días
-- Incidentes PENDING_REVIEW sin resolver: 90 días
-- Política default: 30 días
INSERT INTO llm.moderation_retention_policies (incident_type, severity, retention_days, updated_by, version)
VALUES
  ('BLOCK_CONFIRMED', 'DEFAULT', 30, 'SEED_MIGRATION', 1),
  ('BLOCK_REVERSED', 'DEFAULT', 90, 'SEED_MIGRATION', 1),
  ('PENDING_REVIEW', 'DEFAULT', 90, 'SEED_MIGRATION', 1),
  ('DEFAULT', 'DEFAULT', 30, 'SEED_MIGRATION', 1)
ON CONFLICT (incident_type, severity) DO NOTHING;
