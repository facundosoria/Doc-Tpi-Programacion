CREATE TABLE llm.calibration_stability_groups (
  id UUID PRIMARY KEY,
  course_id UUID NOT NULL,
  idempotency_key UUID NOT NULL,
  state VARCHAR(24) NOT NULL DEFAULT 'RUNNING' CHECK (state IN ('RUNNING','STABLE_PASSED','FAILED')),
  mae_spread NUMERIC(8,4),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  UNIQUE (course_id, idempotency_key)
);

ALTER TABLE llm.calibration_runs ADD COLUMN stability_group_id UUID REFERENCES llm.calibration_stability_groups(id) ON DELETE RESTRICT;
ALTER TABLE llm.calibration_runs ADD COLUMN stability_ordinal SMALLINT;
ALTER TABLE llm.calibration_runs ADD COLUMN calibration_seed BIGINT;
ALTER TABLE llm.calibration_runs ADD COLUMN inference_policy JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE llm.calibration_runs ADD COLUMN provider_fingerprint VARCHAR(255);
CREATE UNIQUE INDEX calibration_runs_stability_group_ordinal_unique ON llm.calibration_runs(stability_group_id, stability_ordinal) WHERE stability_group_id IS NOT NULL;
