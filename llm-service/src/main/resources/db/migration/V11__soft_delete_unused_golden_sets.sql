ALTER TABLE llm.golden_set_families
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deleted_by_user_id UUID;

CREATE INDEX IF NOT EXISTS golden_set_families_active_course_idx
  ON llm.golden_set_families (course_id, created_at DESC)
  WHERE deleted_at IS NULL;
