ALTER TABLE model_deployments
  ADD COLUMN candidate_slot SMALLINT,
  ADD COLUMN candidate_archived_at TIMESTAMPTZ,
  ADD COLUMN chat_verified_at TIMESTAMPTZ;

ALTER TABLE model_deployments
  ADD CONSTRAINT model_deployments_candidate_slot_check CHECK (candidate_slot IS NULL OR candidate_slot BETWEEN 1 AND 3);

CREATE UNIQUE INDEX model_deployments_active_candidate_slot_unique
  ON model_deployments (candidate_slot)
  WHERE candidate_slot IS NOT NULL AND candidate_archived_at IS NULL;
