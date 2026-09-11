ALTER TABLE model_deployments ADD COLUMN calibration_target BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX model_deployments_one_calibration_target
  ON model_deployments ((calibration_target)) WHERE calibration_target;
