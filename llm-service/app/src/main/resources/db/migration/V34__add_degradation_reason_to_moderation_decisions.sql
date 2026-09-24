-- LLM-S13-H01 · EP-08: Modo degradado y resiliencia en moderación de chat.
-- Agrega columna degradation_reason para auditar si la degradación se debió a CONTEXTUAL_UNAVAILABLE o FULL_ENGINE_UNAVAILABLE.

ALTER TABLE IF EXISTS llm.moderation_decisions
  ADD COLUMN IF NOT EXISTS degradation_reason VARCHAR(100);
