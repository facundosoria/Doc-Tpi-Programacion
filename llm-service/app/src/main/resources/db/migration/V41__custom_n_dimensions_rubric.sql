-- Agregar discriminador de tipo y mensaje orientador en la tabla de versiones
ALTER TABLE llm.rubric_version_v2
  ADD COLUMN IF NOT EXISTS user_prompt TEXT DEFAULT '',
  ADD COLUMN IF NOT EXISTS rubric_kind VARCHAR(32) DEFAULT 'DEFAULT_INSTITUTIONAL';

-- Crear tabla para dimensiones dinámicas modulares (sin tocar rubric_dimension_v2)
CREATE TABLE IF NOT EXISTS llm.rubric_custom_dimensions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rubric_version_id UUID NOT NULL REFERENCES llm.rubric_version_v2(id) ON DELETE CASCADE,
  dimension_key VARCHAR(64) NOT NULL,
  label VARCHAR(160) NOT NULL,
  criterion TEXT NOT NULL,
  weight NUMERIC(5,2) NOT NULL CHECK (weight > 0 AND weight <= 100),
  anchors JSONB NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_rubric_dimension_key UNIQUE (rubric_version_id, dimension_key)
);
CREATE INDEX IF NOT EXISTS idx_rubric_custom_dimensions_version 
  ON llm.rubric_custom_dimensions(rubric_version_id);
