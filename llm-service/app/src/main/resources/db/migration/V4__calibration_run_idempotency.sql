ALTER TABLE calibration_runs ADD COLUMN idempotency_key UUID;
CREATE UNIQUE INDEX calibration_runs_course_idempotency_key_unique ON calibration_runs (course_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
