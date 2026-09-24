-- Rúbricas modulares por desafío (Parte A del plan)
-- Overlay de rúbrica por desafío sobre el baseline canónico del curso

-- Extender rubric_families para admitir scope CHALLENGE
ALTER TABLE llm.rubric_families
  ADD COLUMN IF NOT EXISTS challenge_id UUID;

-- El constraint original (V2) solo admite PLATFORM/COURSE y bloquea CHALLENGE:
-- se reemplaza por rubric_families_scope_consistency (que cubre los tres).
ALTER TABLE llm.rubric_families
  DROP CONSTRAINT IF EXISTS rubric_families_check;

-- Reemplazar el CHECK de scope por uno que admita CHALLENGE
ALTER TABLE llm.rubric_families
  DROP CONSTRAINT IF EXISTS rubric_families_scope_check,
  ADD CONSTRAINT rubric_families_scope_check
    CHECK (scope IN ('PLATFORM', 'COURSE', 'CHALLENGE'));

-- Invariante de coexistencia
ALTER TABLE llm.rubric_families
  DROP CONSTRAINT IF EXISTS rubric_families_scope_consistency,
  ADD CONSTRAINT rubric_families_scope_consistency
    CHECK (
      (scope = 'PLATFORM'  AND course_id IS NULL     AND challenge_id IS NULL) OR
      (scope = 'COURSE'    AND course_id IS NOT NULL AND challenge_id IS NULL) OR
      (scope = 'CHALLENGE' AND course_id IS NOT NULL AND challenge_id IS NOT NULL)
    );

-- Validación: si scope es CHALLENGE, challenge_id debe estar presente
ALTER TABLE llm.rubric_families
  ADD CONSTRAINT rubric_families_challenge_required_when_scope_challenge
    CHECK (scope <> 'CHALLENGE' OR challenge_id IS NOT NULL);

-- Extender rubric_version_v2 para soportar overlays
ALTER TABLE llm.rubric_version_v2
  ADD COLUMN IF NOT EXISTS baseline_version_id UUID REFERENCES llm.rubric_version_v2(id) ON DELETE RESTRICT,
  ADD COLUMN IF NOT EXISTS user_prompt TEXT NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS rubric_kind VARCHAR(32) NOT NULL DEFAULT 'DEFAULT_INSTITUTIONAL'
    CHECK (rubric_kind IN ('DEFAULT_INSTITUTIONAL', 'MODULAR_CUSTOM'));

-- Tabla de dimensiones custom (reutilizada para overlays)
CREATE TABLE IF NOT EXISTS llm.rubric_custom_dimensions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rubric_version_id UUID NOT NULL REFERENCES llm.rubric_version_v2(id) ON DELETE CASCADE,
  dimension_key VARCHAR(64) NOT NULL,
  label VARCHAR(160) NOT NULL,
  criterion TEXT NOT NULL,
  weight NUMERIC(5,2) NOT NULL CHECK (weight > 0 AND weight <= 100),
  anchors JSONB NOT NULL CHECK (jsonb_typeof(anchors) = 'object'),
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_rubric_custom_dimension_key UNIQUE (rubric_version_id, dimension_key)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_rubric_families_challenge
  ON llm.rubric_families(challenge_id) WHERE scope = 'CHALLENGE';

CREATE INDEX IF NOT EXISTS idx_rubric_custom_dimensions_version
  ON llm.rubric_custom_dimensions(rubric_version_id);
