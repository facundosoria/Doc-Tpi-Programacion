CREATE TYPE calibration_stage AS ENUM ('PLATFORM', 'COURSE');

ALTER TABLE calibration_runs ADD COLUMN stage calibration_stage NOT NULL DEFAULT 'COURSE';
ALTER TABLE calibration_runs ALTER COLUMN course_id DROP NOT NULL;
ALTER TABLE calibration_runs ADD CONSTRAINT calibration_runs_stage_scope_check CHECK (
  (stage = 'COURSE' AND course_id IS NOT NULL) OR
  (stage = 'PLATFORM' AND course_id IS NULL)
);
CREATE INDEX calibration_runs_deployment_stage_state_idx
  ON calibration_runs (model_deployment_id, stage, state, finished_at DESC);

CREATE TABLE institutional_calibration_profiles (
  id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
  golden_set_version_id UUID NOT NULL REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  rubric_version_id UUID NOT NULL REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  configured_by_user_id UUID NOT NULL,
  configured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
