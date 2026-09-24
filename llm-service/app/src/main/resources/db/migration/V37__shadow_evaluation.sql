-- E-31 (Fase 1): shadow del evaluador por replay. Valida una rubric_version candidata contra la
-- activa del curso sobre transcripciones ya guardadas, descartando la salida: estas tablas son el
-- ÚNICO destino. No hay FK ni escritura hacia calibration_*, pending_evaluations ni event_outbox,
-- así que una evaluación en sombra no puede emitir ni alterar un score real.
-- No se guarda la transcripción: solo la referencia a la fuente y los puntajes por dimensión.

CREATE TABLE shadow_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id UUID NOT NULL,
  baseline_rubric_version_id UUID NOT NULL REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  candidate_rubric_version_id UUID NOT NULL REFERENCES rubric_version_v2(id) ON DELETE RESTRICT,
  source VARCHAR(24) NOT NULL CHECK (source IN ('GOLDEN_SET', 'TUTOR_CONVERSATIONS')),
  golden_set_version_id UUID REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  sample_size INTEGER NOT NULL CHECK (sample_size BETWEEN 1 AND 500),
  divergence_threshold NUMERIC(5,2) NOT NULL CHECK (divergence_threshold > 0),
  state VARCHAR(12) NOT NULL DEFAULT 'QUEUED' CHECK (state IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
  progress SMALLINT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  summary JSONB,
  failure_code VARCHAR(40),
  idempotency_key UUID NOT NULL UNIQUE,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  CHECK ((source = 'GOLDEN_SET') = (golden_set_version_id IS NOT NULL)),
  CHECK (baseline_rubric_version_id <> candidate_rubric_version_id),
  CHECK ((state IN ('COMPLETED', 'FAILED')) = (finished_at IS NOT NULL))
);
CREATE INDEX shadow_runs_course_created_idx ON shadow_runs (course_id, created_at DESC);
CREATE INDEX shadow_runs_state_created_idx ON shadow_runs (state, created_at);

CREATE TABLE shadow_case_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  shadow_run_id UUID NOT NULL REFERENCES shadow_runs(id) ON DELETE CASCADE,
  source_ref VARCHAR(80) NOT NULL,
  baseline_scores JSONB,
  candidate_scores JSONB,
  human_scores JSONB,
  error_code VARCHAR(40),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (shadow_run_id, source_ref),
  CHECK ((error_code IS NULL) = (baseline_scores IS NOT NULL AND candidate_scores IS NOT NULL))
);
