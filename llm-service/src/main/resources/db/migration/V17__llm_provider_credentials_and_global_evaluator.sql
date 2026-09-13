CREATE TYPE provider_credential_state AS ENUM ('ACTIVE', 'DISABLED');
CREATE TYPE evaluator_deployment_state AS ENUM ('CANDIDATE', 'ACTIVE', 'DISABLED');
CREATE TYPE llm_usage_kind AS ENUM ('ADMIN_TEST', 'EVALUATION');

CREATE TABLE provider_credentials (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider VARCHAR(32) NOT NULL CHECK (provider IN ('OPENAI_COMPATIBLE', 'ANTHROPIC', 'GEMINI')),
  display_name VARCHAR(120) NOT NULL,
  base_url VARCHAR(500),
  encrypted_secret BYTEA NOT NULL,
  secret_nonce BYTEA NOT NULL,
  key_version SMALLINT NOT NULL DEFAULT 1,
  secret_mask VARCHAR(32) NOT NULL,
  state provider_credential_state NOT NULL DEFAULT 'ACTIVE',
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  replaced_at TIMESTAMPTZ,
  CHECK ((provider = 'OPENAI_COMPATIBLE' AND base_url IS NOT NULL) OR provider <> 'OPENAI_COMPATIBLE')
);

ALTER TABLE model_deployments ADD COLUMN credential_id UUID REFERENCES provider_credentials(id) ON DELETE RESTRICT;
ALTER TABLE model_deployments ADD COLUMN evaluator_state evaluator_deployment_state NOT NULL DEFAULT 'CANDIDATE';
CREATE UNIQUE INDEX model_deployments_one_global_evaluator_active
  ON model_deployments ((evaluator_state)) WHERE evaluator_state = 'ACTIVE';

CREATE TABLE llm_usage_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  model_deployment_id UUID NOT NULL REFERENCES model_deployments(id) ON DELETE RESTRICT,
  usage_kind llm_usage_kind NOT NULL,
  input_tokens INTEGER NOT NULL DEFAULT 0 CHECK (input_tokens >= 0),
  output_tokens INTEGER NOT NULL DEFAULT 0 CHECK (output_tokens >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX llm_usage_records_model_kind_created_idx ON llm_usage_records(model_deployment_id, usage_kind, created_at DESC);
