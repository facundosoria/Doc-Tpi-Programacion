CREATE TABLE IF NOT EXISTS llm.evaluator_skills (
  skill_key VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  tool_type VARCHAR(32) NOT NULL,
  system_instruction TEXT NOT NULL,
  enabled_by_default BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS llm.course_evaluator_skills (
  course_id UUID NOT NULL,
  skill_key VARCHAR(64) NOT NULL REFERENCES llm.evaluator_skills(skill_key) ON DELETE RESTRICT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (course_id, skill_key)
);

-- Inserción de semillas base
INSERT INTO llm.evaluator_skills (skill_key, name, description, tool_type, system_instruction, enabled_by_default) VALUES
('code_quality', 'Revisor de Calidad de Código', 'Inspecciona legibilidad, modularidad y convenciones de estilo.', 'STATIC_ANALYSIS', 'Analiza la claridad de nombres, descomposición de métodos y ausencia de duplicaciones.', true),
('test_runner', 'Verificador de Pruebas Automatizadas', 'Evalúa la cobertura de casos de prueba y tratamiento de situaciones límite.', 'TEST_RUNNER', 'Verifica si la solución incluye casos de prueba unitarios, límites y verificación de excepciones.', true),
('student_autonomy', 'Detector de Autonomía Pedagógica', 'Examina los intentos previos y la formulación de hipótesis antes de pedir ayuda.', 'TELEMETRY', 'Evalúa si el estudiante intentó resolver el problema antes de consultar al tutor o si delegó la resolución completa.', false),
('architecture_linter', 'Inspector de Arquitectura', 'Valida separación en capas y desacoplamiento de dependencias.', 'CODE_METRICS', 'Inspecciona que el código respete la arquitectura en capas y no existan dependencias circulares.', false)
ON CONFLICT (skill_key) DO NOTHING;
