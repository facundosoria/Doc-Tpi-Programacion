CREATE TYPE version_state AS ENUM ('DRAFT', 'PUBLISHED', 'SUPERSEDED');
CREATE TYPE import_state AS ENUM ('DRAFT', 'VALIDATING', 'READY', 'COMMITTED', 'FAILED');
CREATE TYPE calibration_state AS ENUM ('QUEUED', 'RUNNING', 'PASSED', 'FAILED', 'CANCELLED');
CREATE TYPE evaluation_state AS ENUM ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED');
CREATE TYPE model_deployment_state AS ENUM ('ENABLED', 'DISABLED');

CREATE TABLE rubric_families (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  scope VARCHAR(16) NOT NULL CHECK (scope IN ('PLATFORM', 'COURSE')),
  course_id UUID,
  name VARCHAR(160) NOT NULL,
  next_version INTEGER NOT NULL DEFAULT 1 CHECK (next_version > 0),
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((scope = 'PLATFORM' AND course_id IS NULL) OR (scope = 'COURSE' AND course_id IS NOT NULL))
);
CREATE UNIQUE INDEX rubric_families_course_name_unique ON rubric_families (course_id, name) WHERE scope = 'COURSE';

CREATE TABLE rubric_version_v2 (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  family_id UUID NOT NULL REFERENCES rubric_families(id) ON DELETE RESTRICT,
  version_no INTEGER NOT NULL CHECK (version_no > 0),
  state version_state NOT NULL DEFAULT 'DRAFT',
  revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
  based_on_version_id UUID REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ,
  UNIQUE (family_id, version_no),
  CHECK ((state = 'DRAFT' AND published_at IS NULL) OR (state IN ('PUBLISHED', 'SUPERSEDED') AND published_at IS NOT NULL))
);
CREATE INDEX rubric_version_v2_family_state_idx ON rubric_version_v2 (family_id, state, version_no DESC);

CREATE TABLE rubric_dimension_v2 (
  rubric_version_id UUID NOT NULL REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  dimension_key VARCHAR(32) NOT NULL CHECK (dimension_key IN ('AUTONOMY', 'CLARITY', 'PROGRESSION', 'COMPLIANCE', 'EFFICIENCY')),
  label VARCHAR(160) NOT NULL,
  criterion TEXT NOT NULL,
  anchors JSONB NOT NULL CHECK (jsonb_typeof(anchors) = 'object'),
  evaluator_prompt TEXT NOT NULL,
  weight NUMERIC(5,2) NOT NULL CHECK (weight > 0 AND weight <= 100),
  PRIMARY KEY (rubric_version_id, dimension_key)
);

CREATE TABLE golden_set_families (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  scope VARCHAR(16) NOT NULL CHECK (scope IN ('PLATFORM', 'COURSE')),
  course_id UUID,
  name VARCHAR(160) NOT NULL,
  next_version INTEGER NOT NULL DEFAULT 1 CHECK (next_version > 0),
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((scope = 'PLATFORM' AND course_id IS NULL) OR (scope = 'COURSE' AND course_id IS NOT NULL))
);
CREATE UNIQUE INDEX golden_set_families_course_name_unique ON golden_set_families (course_id, name) WHERE scope = 'COURSE';

CREATE TABLE golden_set_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  family_id UUID NOT NULL REFERENCES golden_set_families(id) ON DELETE RESTRICT,
  version_no INTEGER NOT NULL CHECK (version_no > 0),
  state version_state NOT NULL DEFAULT 'DRAFT',
  revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
  based_on_version_id UUID REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ,
  UNIQUE (family_id, version_no),
  CHECK ((state = 'DRAFT' AND published_at IS NULL) OR (state IN ('PUBLISHED', 'SUPERSEDED') AND published_at IS NOT NULL))
);
CREATE INDEX golden_set_versions_family_state_idx ON golden_set_versions (family_id, state, version_no DESC);

CREATE TABLE golden_set_cases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  golden_set_version_id UUID NOT NULL REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  case_order INTEGER NOT NULL CHECK (case_order >= 0),
  review_state VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (review_state IN ('DRAFT', 'REVIEWED')),
  transcript JSONB NOT NULL CHECK (jsonb_typeof(transcript) = 'array' AND jsonb_array_length(transcript) > 0),
  challenge_context JSONB NOT NULL CHECK (jsonb_typeof(challenge_context) = 'object'),
  safe_metadata JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(safe_metadata) = 'object'),
  author VARCHAR(160) NOT NULL,
  reference_scores JSONB NOT NULL CHECK (valid_reference_scores(reference_scores)),
  score_justifications JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(score_justifications) = 'object'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (golden_set_version_id, case_order)
);
CREATE INDEX golden_set_cases_version_idx ON golden_set_cases (golden_set_version_id, case_order);

CREATE TABLE import_batches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id UUID NOT NULL,
  golden_set_version_id UUID NOT NULL REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  format VARCHAR(8) NOT NULL CHECK (format IN ('JSON', 'CSV')),
  state import_state NOT NULL DEFAULT 'DRAFT',
  idempotency_key UUID NOT NULL,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  committed_at TIMESTAMPTZ,
  UNIQUE (course_id, idempotency_key),
  CHECK ((state = 'COMMITTED' AND committed_at IS NOT NULL) OR (state <> 'COMMITTED' AND committed_at IS NULL))
);
CREATE TABLE import_rows (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_id UUID NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
  row_number INTEGER NOT NULL CHECK (row_number > 0),
  state VARCHAR(16) NOT NULL DEFAULT 'INVALID' CHECK (state IN ('VALID', 'INVALID')),
  payload JSONB NOT NULL,
  errors JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(errors) = 'array'),
  UNIQUE (batch_id, row_number)
);

CREATE TABLE model_adapters (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider VARCHAR(120) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by_user_id UUID NOT NULL
);
CREATE TABLE model_deployments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  adapter_id UUID NOT NULL REFERENCES model_adapters(id) ON DELETE RESTRICT,
  model_id VARCHAR(160) NOT NULL,
  model_version VARCHAR(160) NOT NULL,
  state model_deployment_state NOT NULL DEFAULT 'ENABLED',
  capabilities JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (adapter_id, model_id, model_version)
);

CREATE TABLE calibration_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id UUID NOT NULL,
  rubric_version_id UUID NOT NULL REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  golden_set_version_id UUID NOT NULL REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  model_deployment_id UUID NOT NULL REFERENCES model_deployments(id) ON DELETE RESTRICT,
  state calibration_state NOT NULL DEFAULT 'QUEUED',
  reason VARCHAR(20) NOT NULL CHECK (reason IN ('MANUAL', 'MONTHLY', 'MODEL_CHANGED')),
  progress SMALLINT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  mae_final NUMERIC(8,4),
  max_individual_error SMALLINT CHECK (max_individual_error BETWEEN 0 AND 100),
  parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
  effective_prompt TEXT,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  CHECK ((state IN ('PASSED', 'FAILED', 'CANCELLED')) = (finished_at IS NOT NULL))
);
CREATE INDEX calibration_runs_course_created_idx ON calibration_runs (course_id, created_at DESC);

CREATE TABLE calibration_case_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  calibration_run_id UUID NOT NULL REFERENCES calibration_runs(id) ON DELETE RESTRICT,
  golden_set_case_id UUID NOT NULL REFERENCES golden_set_cases(id) ON DELETE RESTRICT,
  model_scores JSONB NOT NULL CHECK (valid_reference_scores(model_scores)),
  human_final_score NUMERIC(8,4) NOT NULL CHECK (human_final_score BETWEEN 0 AND 100),
  model_final_score NUMERIC(8,4) NOT NULL CHECK (model_final_score BETWEEN 0 AND 100),
  dimension_errors JSONB NOT NULL CHECK (valid_reference_scores(dimension_errors)),
  final_error NUMERIC(8,4) NOT NULL CHECK (final_error >= 0),
  output_artifact JSONB NOT NULL DEFAULT '{}'::jsonb,
  UNIQUE (calibration_run_id, golden_set_case_id)
);

CREATE TABLE active_calibrations (
  course_id UUID PRIMARY KEY,
  calibration_run_id UUID NOT NULL UNIQUE REFERENCES calibration_runs(id) ON DELETE RESTRICT,
  activated_by_user_id UUID NOT NULL,
  activated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE challenge_calibration_assignments (
  challenge_id UUID PRIMARY KEY,
  course_id UUID NOT NULL,
  calibration_run_id UUID NOT NULL REFERENCES calibration_runs(id) ON DELETE RESTRICT,
  first_attempt_id UUID,
  locked_at TIMESTAMPTZ,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((first_attempt_id IS NULL AND locked_at IS NULL) OR (first_attempt_id IS NOT NULL AND locked_at IS NOT NULL))
);
CREATE INDEX challenge_calibration_assignments_course_locked_idx ON challenge_calibration_assignments (course_id, locked_at);

CREATE TABLE pending_evaluations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  attempt_id UUID NOT NULL UNIQUE,
  assignment_challenge_id UUID NOT NULL REFERENCES challenge_calibration_assignments(challenge_id) ON DELETE RESTRICT,
  state evaluation_state NOT NULL DEFAULT 'QUEUED',
  reason VARCHAR(40) NOT NULL DEFAULT 'CALIBRATION_UNAVAILABLE',
  idempotency_key UUID NOT NULL UNIQUE,
  queued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  CHECK ((state = 'QUEUED' AND started_at IS NULL AND completed_at IS NULL) OR (state = 'RUNNING' AND started_at IS NOT NULL AND completed_at IS NULL) OR (state IN ('COMPLETED', 'FAILED') AND completed_at IS NOT NULL))
);
CREATE INDEX pending_evaluations_state_queued_idx ON pending_evaluations (state, queued_at);

CREATE OR REPLACE FUNCTION prevent_published_version_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.state IN ('PUBLISHED', 'SUPERSEDED') THEN
    RAISE EXCEPTION 'published version % cannot be changed', OLD.id;
  END IF;
  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER rubric_version_v2_immutable BEFORE UPDATE OR DELETE ON rubric_version_v2 FOR EACH ROW EXECUTE FUNCTION prevent_published_version_mutation();
CREATE TRIGGER golden_set_versions_immutable BEFORE UPDATE OR DELETE ON golden_set_versions FOR EACH ROW EXECUTE FUNCTION prevent_published_version_mutation();
