ALTER TABLE llm.rubric_version_v2
  ADD COLUMN template_origin_version_id UUID REFERENCES llm.rubric_version_v2(id) ON DELETE RESTRICT;

UPDATE llm.rubric_dimension_v2
SET anchors = jsonb_build_object(
  'low', jsonb_build_object(
    'behavior', coalesce(anchors ->> 'low', ''),
    'referenceScore', 25,
    'example', 'Ejemplo pedagógico pendiente de aprobación funcional.'),
  'medium', jsonb_build_object(
    'behavior', coalesce(anchors ->> 'medium', ''),
    'referenceScore', 60,
    'example', 'Ejemplo pedagógico pendiente de aprobación funcional.'),
  'high', jsonb_build_object(
    'behavior', coalesce(anchors ->> 'high', ''),
    'referenceScore', 90,
    'example', 'Ejemplo pedagógico pendiente de aprobación funcional.'));

ALTER TABLE llm.rubric_dimension_v2
  DROP COLUMN evaluator_prompt;
