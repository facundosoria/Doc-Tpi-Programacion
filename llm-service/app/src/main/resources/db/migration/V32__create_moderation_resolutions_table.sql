-- LLM-S12-H02 · EP-08: Resolución docente y supervisión de incidentes de moderación.
-- Tabla append-only e inmutable de resoluciones docentes con FK a moderation_incidents.
-- Constraint de unicidad sobre incident_id para garantizar inmutabilidad y evitar re-resoluciones (409 Conflict).

CREATE TABLE IF NOT EXISTS moderation_resolutions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id UUID NOT NULL UNIQUE,
  resolved_by VARCHAR(255) NOT NULL,
  resolution VARCHAR(50) NOT NULL,
  resolution_reason VARCHAR(500) NOT NULL,
  resolved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_moderation_resolutions_incident FOREIGN KEY (incident_id) REFERENCES moderation_incidents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS moderation_resolutions_incident_idx ON moderation_resolutions (incident_id);
CREATE INDEX IF NOT EXISTS moderation_resolutions_resolved_by_idx ON moderation_resolutions (resolved_by);
