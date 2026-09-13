-- Explicitly authorized reset: only course Golden Set drafts created for the workbench are purged.
-- Published and superseded evidence is deliberately untouched.
DELETE FROM llm.import_batches
WHERE golden_set_version_id IN (
  SELECT v.id FROM llm.golden_set_versions v
  JOIN llm.golden_set_families f ON f.id = v.family_id
  WHERE f.scope = 'COURSE' AND v.state = 'DRAFT'
);

DELETE FROM llm.golden_set_cases
WHERE golden_set_version_id IN (
  SELECT v.id FROM llm.golden_set_versions v
  JOIN llm.golden_set_families f ON f.id = v.family_id
  WHERE f.scope = 'COURSE' AND v.state = 'DRAFT'
);

DELETE FROM llm.golden_set_versions v
USING llm.golden_set_families f
WHERE v.family_id = f.id AND f.scope = 'COURSE' AND v.state = 'DRAFT';

ALTER TABLE llm.golden_set_cases
  DROP CONSTRAINT IF EXISTS golden_set_cases_challenge_context_check,
  ALTER COLUMN challenge_context DROP NOT NULL;
