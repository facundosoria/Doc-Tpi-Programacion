-- Coordinated breaking migration: provider credentials and all provider-dependent evidence are recreated.
-- Golden sets, rubrics and course data are deliberately preserved.
TRUNCATE TABLE llm.calibration_run_artifacts, llm.calibration_case_results, llm.calibration_runs,
  llm.calibration_stability_groups, llm.llm_usage_records, llm.function_model_config,
  llm.model_deployments, llm.model_adapters, llm.provider_credentials RESTART IDENTITY CASCADE;

ALTER TABLE llm.provider_credentials DROP CONSTRAINT IF EXISTS provider_credentials_provider_check;
ALTER TABLE llm.provider_credentials RENAME COLUMN provider TO provider_key;
ALTER TABLE llm.provider_credentials RENAME COLUMN encrypted_secret TO encrypted_secrets;
ALTER TABLE llm.provider_credentials DROP COLUMN IF EXISTS base_url;
ALTER TABLE llm.provider_credentials ADD COLUMN public_configuration JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE llm.provider_credentials ADD COLUMN schema_version VARCHAR(32) NOT NULL DEFAULT '1';
ALTER TABLE llm.provider_credentials ADD CONSTRAINT provider_credentials_provider_key_check
  CHECK (provider_key ~ '^[a-z][a-z0-9-]{1,63}$');

ALTER TABLE llm.model_deployments ALTER COLUMN adapter_id DROP NOT NULL;
ALTER TABLE llm.model_deployments ADD COLUMN provider_key VARCHAR(64);
ALTER TABLE llm.model_deployments ADD COLUMN adapter_version VARCHAR(64) NOT NULL DEFAULT '1';
-- `capabilities` already belongs to the original deployment schema (V2); V26 only
-- preserves it as the provider-neutral capability snapshot.
ALTER TABLE llm.model_deployments ADD COLUMN IF NOT EXISTS capabilities JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE llm.model_deployments ALTER COLUMN provider_key SET NOT NULL;
ALTER TABLE llm.model_deployments ADD CONSTRAINT model_deployments_provider_key_check
  CHECK (provider_key ~ '^[a-z][a-z0-9-]{1,63}$');

ALTER TABLE llm.function_model_config ADD COLUMN model_deployment_id UUID;
ALTER TABLE llm.function_model_config DROP COLUMN provider;
ALTER TABLE llm.function_model_config DROP COLUMN model_id;
ALTER TABLE llm.function_model_config DROP COLUMN model_version;
ALTER TABLE llm.function_model_config ALTER COLUMN model_deployment_id SET NOT NULL;
ALTER TABLE llm.function_model_config ADD CONSTRAINT function_model_config_deployment_fk
  FOREIGN KEY (model_deployment_id) REFERENCES llm.model_deployments(id) ON DELETE RESTRICT;
