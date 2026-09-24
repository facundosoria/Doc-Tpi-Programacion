-- LLM-S01-H10 (puerto de invocación de modelos + fake) y EP-05 (tutor).
-- Tabla función→proveedor+modelo que pide H10·T5 / `GET-PUT /api/llm/model-assignments/{function}`.
-- Cambiar una fila no exige recompilar ni re-desplegar código (H10·CA3).

CREATE TABLE function_model_config (
  function VARCHAR(32) PRIMARY KEY CHECK (function IN ('tutor', 'evaluator', 'moderator', 'generator')),
  provider VARCHAR(120) NOT NULL,
  model_id VARCHAR(120) NOT NULL,
  model_version VARCHAR(60),
  enabled BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by_user_id UUID
);

INSERT INTO function_model_config (function, provider, model_id, model_version, enabled)
VALUES ('tutor', 'fake', 'fake-socratic-v1', '1', true);
