CREATE TYPE golden_set_update_proposal_state AS ENUM ('PENDING', 'ACCEPTED', 'DISMISSED');

CREATE TABLE golden_set_update_proposals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id UUID NOT NULL,
  course_family_id UUID NOT NULL REFERENCES golden_set_families(id) ON DELETE RESTRICT,
  base_version_id UUID NOT NULL REFERENCES golden_set_versions(id) ON DELETE RESTRICT,
  state golden_set_update_proposal_state NOT NULL DEFAULT 'PENDING',
  detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ,
  CHECK ((state = 'PENDING' AND resolved_at IS NULL) OR (state IN ('ACCEPTED', 'DISMISSED') AND resolved_at IS NOT NULL)),
  UNIQUE (course_id, base_version_id)
);
CREATE INDEX golden_set_update_proposals_course_pending_idx ON golden_set_update_proposals (course_id, state, detected_at DESC);
