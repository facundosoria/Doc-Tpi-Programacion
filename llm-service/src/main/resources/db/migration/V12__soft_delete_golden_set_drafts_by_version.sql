ALTER TABLE llm.golden_set_versions
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deleted_by_user_id UUID;

CREATE INDEX IF NOT EXISTS golden_set_versions_active_course_idx
  ON llm.golden_set_versions (family_id, version_no DESC)
  WHERE deleted_at IS NULL;
