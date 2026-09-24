ALTER TABLE calibration_runs
  ADD COLUMN failure_code VARCHAR(64),
  ADD COLUMN failure_detail VARCHAR(280);
