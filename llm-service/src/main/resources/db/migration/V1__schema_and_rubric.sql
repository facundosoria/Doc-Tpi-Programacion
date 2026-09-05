CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE rubric_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  version VARCHAR(32) NOT NULL UNIQUE,
  language CHAR(2) NOT NULL CHECK (language = 'es'),
  definition JSONB NOT NULL,
  checksum CHAR(64) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(120) NOT NULL
);

CREATE TABLE rubric_dimensions (
  rubric_version_id UUID NOT NULL REFERENCES rubric_versions(id) ON DELETE RESTRICT,
  code VARCHAR(32) NOT NULL CHECK (code IN ('autonomy', 'clarity', 'progression', 'compliance', 'efficiency')),
  display_name VARCHAR(160) NOT NULL,
  weight SMALLINT NOT NULL CHECK (weight BETWEEN 0 AND 100),
  anchors JSONB NOT NULL DEFAULT '[]'::jsonb,
  PRIMARY KEY (rubric_version_id, code)
);

INSERT INTO rubric_versions (id, version, language, definition, checksum, created_by)
VALUES (
  '00000000-0000-0000-0000-000000000001', '1.0', 'es',
  '{"version":"1.0","language":"es","dimensions":["autonomy","clarity","progression","compliance","efficiency"]}'::jsonb,
  'b93ccdc6845fb7ccbb64cc1f64bf5772fb4b904622e0a8874f8cb980e74ff48a', 'migration'
);

INSERT INTO rubric_dimensions (rubric_version_id, code, display_name, weight) VALUES
  ('00000000-0000-0000-0000-000000000001', 'autonomy', 'Autonomía y pensamiento crítico', 30),
  ('00000000-0000-0000-0000-000000000001', 'clarity', 'Claridad y especificidad de los prompts', 25),
  ('00000000-0000-0000-0000-000000000001', 'progression', 'Progresión e iteración lógica', 20),
  ('00000000-0000-0000-0000-000000000001', 'compliance', 'Cumplimiento de límites', 15),
  ('00000000-0000-0000-0000-000000000001', 'efficiency', 'Eficiencia de la interacción', 10);

CREATE SEQUENCE golden_set_version_seq;

CREATE OR REPLACE FUNCTION valid_reference_scores(value JSONB) RETURNS BOOLEAN LANGUAGE sql IMMUTABLE AS $$
  SELECT jsonb_typeof(value) = 'object'
    AND (SELECT array_agg(key ORDER BY key) FROM jsonb_object_keys(value) key) = ARRAY['autonomy','clarity','compliance','efficiency','progression']
    AND NOT EXISTS (
      SELECT 1 FROM jsonb_each(value) score
      WHERE jsonb_typeof(score.value) <> 'number'
         OR (score.value #>> '{}') !~ '^(0|[1-9][0-9]?|100)$'
    );
$$;

CREATE TABLE golden_sets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  version_no BIGINT NOT NULL UNIQUE DEFAULT nextval('golden_set_version_seq'),
  rubric_version_id UUID NOT NULL REFERENCES rubric_versions(id) ON DELETE RESTRICT,
  language CHAR(2) NOT NULL CHECK (language = 'es'),
  created_by_user_id UUID NOT NULL,
  created_by_service VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX golden_sets_created_at_idx ON golden_sets (created_at DESC);
CREATE INDEX golden_sets_rubric_version_idx ON golden_sets (rubric_version_id);

CREATE TABLE golden_set_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  golden_set_id UUID NOT NULL REFERENCES golden_sets(id) ON DELETE RESTRICT,
  transcript JSONB NOT NULL CHECK (jsonb_typeof(transcript) = 'array' AND jsonb_array_length(transcript) > 0),
  reference_scores JSONB NOT NULL CHECK (valid_reference_scores(reference_scores)),
  content_hash CHAR(64) NOT NULL,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX golden_set_entries_golden_set_idx ON golden_set_entries (golden_set_id, created_at);

CREATE TABLE idempotency_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  operation VARCHAR(100) NOT NULL,
  caller_service VARCHAR(120) NOT NULL,
  delegated_user_id UUID NOT NULL,
  idempotency_key UUID NOT NULL,
  request_hash CHAR(64) NOT NULL,
  resource_id UUID,
  response_status SMALLINT,
  response_body JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,
  UNIQUE (operation, caller_service, delegated_user_id, idempotency_key)
);

CREATE TABLE audit_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  action VARCHAR(100) NOT NULL,
  actor_service VARCHAR(120) NOT NULL,
  actor_user_id UUID NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id UUID NOT NULL,
  request_id VARCHAR(200),
  traceparent VARCHAR(200),
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_events_resource_idx ON audit_events (resource_type, resource_id, occurred_at DESC);

CREATE OR REPLACE FUNCTION prevent_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'append-only table % cannot be changed', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER rubric_versions_append_only BEFORE UPDATE OR DELETE ON rubric_versions FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER rubric_dimensions_append_only BEFORE UPDATE OR DELETE ON rubric_dimensions FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER golden_sets_append_only BEFORE UPDATE OR DELETE ON golden_sets FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER golden_set_entries_append_only BEFORE UPDATE OR DELETE ON golden_set_entries FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
CREATE TRIGGER audit_events_append_only BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
