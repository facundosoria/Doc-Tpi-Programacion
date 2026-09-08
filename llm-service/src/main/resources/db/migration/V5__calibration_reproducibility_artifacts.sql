CREATE TABLE calibration_run_artifacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  calibration_run_id UUID NOT NULL REFERENCES calibration_runs(id) ON DELETE RESTRICT,
  artifact_type VARCHAR(40) NOT NULL,
  content JSONB NOT NULL,
  content_sha256 VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (calibration_run_id, artifact_type)
);
CREATE INDEX calibration_run_artifacts_run_idx ON calibration_run_artifacts (calibration_run_id);
