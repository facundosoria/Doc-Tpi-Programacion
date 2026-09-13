-- Course Golden Sets may contain valid V2 keys in upper case, while archived inputs
-- use lower case. Version cloning copies the immutable references verbatim, so the
-- integrity rule must accept either representation without rejecting the draft.
CREATE OR REPLACE FUNCTION llm.valid_reference_scores(value JSONB) RETURNS BOOLEAN LANGUAGE sql IMMUTABLE AS $$
  SELECT jsonb_typeof(value) = 'object'
    AND (SELECT array_agg(lower(key) ORDER BY lower(key)) FROM jsonb_object_keys(value) key)
      = ARRAY['autonomy','clarity','compliance','efficiency','progression']
    AND NOT EXISTS (
      SELECT 1 FROM jsonb_each(value) score
      WHERE jsonb_typeof(score.value) <> 'number'
         OR (score.value #>> '{}') !~ '^(0|[1-9][0-9]?|100)$'
    );
$$;
