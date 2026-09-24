-- V1 remains readable only in this private schema.  No application repository maps it.
CREATE SCHEMA IF NOT EXISTS legacy_v1;

ALTER TABLE llm.golden_set_entries SET SCHEMA legacy_v1;
ALTER TABLE llm.golden_sets SET SCHEMA legacy_v1;
ALTER TABLE llm.rubric_dimensions SET SCHEMA legacy_v1;
ALTER TABLE llm.rubric_versions SET SCHEMA legacy_v1;
ALTER SEQUENCE llm.golden_set_version_seq SET SCHEMA legacy_v1;

CREATE TABLE llm.legacy_v1_migration_inventory (
  original_golden_set_id UUID PRIMARY KEY,
  original_version_no BIGINT NOT NULL,
  source_checksum CHAR(64) NOT NULL,
  archived_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  entry_count INTEGER NOT NULL,
  converted_entry_count INTEGER NOT NULL,
  conversion_result VARCHAR(24) NOT NULL CHECK (conversion_result IN ('QUARANTINED', 'PARTIAL', 'NOT_CONVERTIBLE')),
  quarantine_golden_set_version_id UUID REFERENCES llm.golden_set_versions(id) ON DELETE RESTRICT
);

WITH source_sets AS (
  SELECT gs.id, gs.version_no, gs.created_by_user_id,
         encode(digest(coalesce(string_agg(e.content_hash, ',' ORDER BY e.id), gs.id::text), 'sha256'), 'hex') AS checksum,
         count(e.id)::integer AS entries,
         count(e.id) FILTER (WHERE jsonb_typeof(e.transcript) = 'array' AND jsonb_array_length(e.transcript) > 0
           AND llm.valid_reference_scores(e.reference_scores))::integer AS convertible
  FROM legacy_v1.golden_sets gs
  LEFT JOIN legacy_v1.golden_set_entries e ON e.golden_set_id = gs.id
  GROUP BY gs.id, gs.version_no, gs.created_by_user_id
), families AS (
  INSERT INTO llm.golden_set_families (scope, name, created_by_user_id)
  SELECT 'PLATFORM', 'Cuarentena legado V1 ' || version_no, created_by_user_id FROM source_sets
  RETURNING id, name
), numbered_families AS (
  SELECT id, row_number() OVER (ORDER BY id) AS ordinal FROM families
), numbered_sources AS (
  SELECT *, row_number() OVER (ORDER BY id) AS ordinal FROM source_sets
), versions AS (
  INSERT INTO llm.golden_set_versions (family_id, version_no, state, created_by_user_id)
  SELECT f.id, 1, 'DRAFT', s.created_by_user_id
  FROM numbered_sources s JOIN numbered_families f ON f.ordinal = s.ordinal
  WHERE s.convertible > 0
  RETURNING id, family_id
), numbered_versions AS (
  SELECT id, row_number() OVER (ORDER BY id) AS ordinal FROM versions
), converted AS (
  INSERT INTO llm.golden_set_cases (golden_set_version_id, case_order, review_state, transcript, challenge_context, safe_metadata, author, reference_scores)
  SELECT v.id, row_number() OVER (PARTITION BY s.id ORDER BY e.id) - 1, 'DRAFT', e.transcript,
    jsonb_build_object('statement', 'El contexto original V1 no está disponible; requiere revisión obligatoria.', 'legacyContextUnavailable', true),
    jsonb_build_object('legacyV1', jsonb_build_object('goldenSetId', s.id, 'entryId', e.id, 'checksum', e.content_hash, 'requiresReview', true)),
    'legado-v1',
    jsonb_build_object('AUTONOMY', (e.reference_scores->>'autonomy')::int, 'CLARITY', (e.reference_scores->>'clarity')::int,
      'PROGRESSION', (e.reference_scores->>'progression')::int, 'COMPLIANCE', (e.reference_scores->>'compliance')::int,
      'EFFICIENCY', (e.reference_scores->>'efficiency')::int)
  FROM numbered_sources s JOIN numbered_versions v ON v.ordinal = s.ordinal
  JOIN legacy_v1.golden_set_entries e ON e.golden_set_id = s.id
  WHERE jsonb_typeof(e.transcript) = 'array' AND jsonb_array_length(e.transcript) > 0 AND llm.valid_reference_scores(e.reference_scores)
  RETURNING golden_set_version_id
)
INSERT INTO llm.legacy_v1_migration_inventory (original_golden_set_id, original_version_no, source_checksum, entry_count, converted_entry_count, conversion_result, quarantine_golden_set_version_id)
SELECT s.id, s.version_no, s.checksum, s.entries, s.convertible,
  CASE WHEN s.convertible = 0 THEN 'NOT_CONVERTIBLE' WHEN s.convertible = s.entries THEN 'QUARANTINED' ELSE 'PARTIAL' END,
  v.id
FROM numbered_sources s LEFT JOIN numbered_versions v ON v.ordinal = s.ordinal;
