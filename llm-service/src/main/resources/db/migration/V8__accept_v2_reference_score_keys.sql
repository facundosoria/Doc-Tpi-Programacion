-- V1 stored lower-case dimensions; V2 uses the domain's upper-case keys.
-- Keep both forms valid because V1 is archived, while all new V2 cases use upper case.
CREATE OR REPLACE FUNCTION llm.valid_reference_scores(value JSONB) RETURNS BOOLEAN LANGUAGE sql IMMUTABLE AS $$
  SELECT jsonb_typeof(value) = 'object'
    AND (SELECT array_agg(key ORDER BY key) FROM jsonb_object_keys(value) key) IN (
      ARRAY['autonomy','clarity','compliance','efficiency','progression'],
      ARRAY['AUTONOMY','CLARITY','COMPLIANCE','EFFICIENCY','PROGRESSION']
    )
    AND NOT EXISTS (
      SELECT 1 FROM jsonb_each(value) score
      WHERE jsonb_typeof(score.value) <> 'number'
         OR (score.value #>> '{}') !~ '^(0|[1-9][0-9]?|100)$'
    );
$$;
