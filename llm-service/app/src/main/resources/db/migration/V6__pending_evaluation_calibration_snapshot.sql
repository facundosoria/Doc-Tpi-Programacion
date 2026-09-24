-- The challenge assignment may be inspected or joined later, but each queued evaluation
-- needs its own immutable reference to the calibration that applies to the attempt.
ALTER TABLE pending_evaluations ADD COLUMN calibration_run_id UUID;

UPDATE pending_evaluations pending
SET calibration_run_id = assignment.calibration_run_id
FROM challenge_calibration_assignments assignment
WHERE assignment.challenge_id = pending.assignment_challenge_id;

ALTER TABLE pending_evaluations
  ALTER COLUMN calibration_run_id SET NOT NULL,
  ADD CONSTRAINT pending_evaluations_calibration_run_fk
    FOREIGN KEY (calibration_run_id) REFERENCES calibration_runs(id) ON DELETE RESTRICT;

CREATE INDEX pending_evaluations_calibration_run_idx ON pending_evaluations (calibration_run_id);
