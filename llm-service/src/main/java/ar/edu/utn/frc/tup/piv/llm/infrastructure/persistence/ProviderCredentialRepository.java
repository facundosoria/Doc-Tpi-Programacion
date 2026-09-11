package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Provider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProviderCredentialRepository {
  private final JdbcTemplate jdbc;
  public ProviderCredentialRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Credential create(Provider provider, String name, String baseUrl, EncryptedSecretService.EncryptedSecret encrypted, String mask, UUID actor) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into llm.provider_credentials(id,provider,display_name,base_url,encrypted_secret,secret_nonce,secret_mask,created_by_user_id) values(?,?,?,?,?,?,?,?)", id, provider.name(), name, baseUrl, encrypted.value(), encrypted.nonce(), mask, actor);
    return get(id).orElseThrow();
  }
  public List<CredentialSummary> list() { return jdbc.query("select id,provider,display_name,base_url,secret_mask,state::text,created_at from llm.provider_credentials order by created_at desc", (rs, row) -> new CredentialSummary(rs.getObject(1, UUID.class), Provider.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant())); }
  public Optional<Credential> get(UUID id) { return jdbc.query("select id,provider,display_name,base_url,encrypted_secret,secret_nonce,secret_mask,state::text,created_at from llm.provider_credentials where id=?", (rs, row) -> new Credential(rs.getObject(1, UUID.class), Provider.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), rs.getBytes(5), rs.getBytes(6), rs.getString(7), rs.getString(8), rs.getTimestamp(9).toInstant()), id).stream().findFirst(); }
  public void disable(UUID id) { if (jdbc.update("update llm.provider_credentials set state='DISABLED',replaced_at=now() where id=? and state='ACTIVE'", id) != 1) throw new IllegalStateException("La credencial no está activa"); }
  public Deployment createDeployment(UUID credentialId, String modelId) { return createCandidate(credentialId, modelId, nextSlot()); }
  @Transactional
  public Deployment createCandidate(UUID credentialId, String modelId, int slot) {
    if (slot < 1 || slot > 3) throw new IllegalArgumentException("El candidato debe ocupar una de las tres tarjetas");
    Credential credential = get(credentialId).filter(value -> "ACTIVE".equals(value.state())).orElseThrow(() -> new IllegalStateException("La credencial no está activa"));
    UUID adapter = jdbc.query("select id from llm.model_adapters where provider=?", (rs, row) -> rs.getObject(1, UUID.class), credential.provider().name().toLowerCase()).stream().findFirst().orElseGet(() -> { UUID id = UUID.randomUUID(); jdbc.update("insert into llm.model_adapters(id,provider,created_by_user_id) values(?,?,?)", id, credential.provider().name().toLowerCase(), UUID.fromString("11111111-1111-1111-1111-111111111111")); return id; });
    jdbc.update("update llm.model_deployments set candidate_archived_at=now(),candidate_slot=null,calibration_target=false where candidate_slot=? and candidate_archived_at is null", slot);
    Optional<UUID> existing = jdbc.query("select id from llm.model_deployments where adapter_id=? and model_id=? and model_version=?", (rs, row) -> rs.getObject(1, UUID.class), adapter, modelId, "provider-managed").stream().findFirst();
    if (existing.isPresent()) {
      UUID id = existing.get();
      jdbc.update("update llm.model_deployments set credential_id=?,candidate_slot=?,candidate_archived_at=null,evaluator_state=case when evaluator_state='ACTIVE' then evaluator_state else 'CANDIDATE'::llm.evaluator_deployment_state end,calibration_target=case when evaluator_state='ACTIVE' then true else false end,chat_verified_at=case when evaluator_state='ACTIVE' then chat_verified_at else null end where id=?", credentialId, slot, id);
      return deployment(id).orElseThrow();
    }
    UUID id = UUID.randomUUID();
    jdbc.update("insert into llm.model_deployments(id,adapter_id,credential_id,model_id,model_version,evaluator_state,candidate_slot) values(?,?,?,?,?,'CANDIDATE',?)", id, adapter, credentialId, modelId, "provider-managed", slot);
    return deployment(id).orElseThrow();
  }
  public List<Deployment> deployments() { return jdbc.query("select d.id,d.credential_id,c.provider,c.display_name,d.model_id,d.evaluator_state::text,d.created_at,d.candidate_slot,d.chat_verified_at from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id where d.candidate_slot is not null and d.candidate_archived_at is null order by d.candidate_slot", (rs,row) -> deploymentRow(rs)); }
  public Optional<Deployment> deployment(UUID id) { return jdbc.query("select d.id,d.credential_id,c.provider,c.display_name,d.model_id,d.evaluator_state::text,d.created_at,d.candidate_slot,d.chat_verified_at from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id where d.id=? and d.candidate_archived_at is null", (rs,row) -> deploymentRow(rs), id).stream().findFirst(); }
  public Optional<Deployment> active() { return jdbc.query("select d.id,d.credential_id,c.provider,c.display_name,d.model_id,d.evaluator_state::text,d.created_at,d.candidate_slot,d.chat_verified_at from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id where d.evaluator_state='ACTIVE'", (rs,row) -> deploymentRow(rs)).stream().findFirst(); }
  public Optional<Deployment> calibrationTarget() { return active(); }
  public void selectForCalibration(UUID id) { jdbc.update("update llm.model_deployments set calibration_target=false where calibration_target"); if (jdbc.update("update llm.model_deployments set calibration_target=true where id=? and evaluator_state <> 'DISABLED'", id) != 1) throw new IllegalStateException("El modelo candidato no existe o está deshabilitado"); }
  public void activate(UUID id) { jdbc.update("update llm.model_deployments set evaluator_state='CANDIDATE',calibration_target=false where evaluator_state='ACTIVE' or calibration_target"); if (jdbc.update("update llm.model_deployments set evaluator_state='ACTIVE',calibration_target=true where id=? and evaluator_state='CANDIDATE' and candidate_slot is not null and candidate_archived_at is null and chat_verified_at is not null", id) != 1) throw new IllegalStateException("El candidato debe responder correctamente en el chat antes de activarse"); }
  public void markChatVerified(UUID id) { jdbc.update("update llm.model_deployments set chat_verified_at=now() where id=? and candidate_archived_at is null", id); }
  public void archiveCandidate(UUID id) { if (jdbc.update("update llm.model_deployments set candidate_archived_at=now(),candidate_slot=null,calibration_target=false,evaluator_state=case when evaluator_state='ACTIVE' then 'CANDIDATE'::llm.evaluator_deployment_state else evaluator_state end where id=? and candidate_archived_at is null", id) != 1) throw new IllegalStateException("El candidato no existe"); }
  public void recordUsage(UUID deploymentId, String kind, int input, int output) { jdbc.update("insert into llm.llm_usage_records(model_deployment_id,usage_kind,input_tokens,output_tokens) values(?,cast(? as llm.llm_usage_kind),?,?)", deploymentId, kind, input, output); }
  public Usage usage(UUID id) { return jdbc.query("select coalesce(sum(input_tokens),0),coalesce(sum(output_tokens),0),count(*) filter(where usage_kind='ADMIN_TEST'),count(*) filter(where usage_kind='EVALUATION') from llm.llm_usage_records where model_deployment_id=?", (rs,row) -> new Usage(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4)), id).stream().findFirst().orElse(new Usage(0,0,0,0)); }
  private int nextSlot() { for (int slot=1;slot<=3;slot++) if (jdbc.queryForObject("select not exists(select 1 from llm.model_deployments where candidate_slot=? and candidate_archived_at is null)",Boolean.class,slot)) return slot; throw new IllegalStateException("Ya hay tres candidatos; reemplace una tarjeta existente"); }
  private Deployment deploymentRow(java.sql.ResultSet rs) throws java.sql.SQLException { return new Deployment(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),Provider.valueOf(rs.getString(3)),rs.getString(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7).toInstant(),rs.getObject(8,Integer.class),rs.getTimestamp(9)==null?null:rs.getTimestamp(9).toInstant()); }
  public record Credential(UUID id, Provider provider, String displayName, String baseUrl, byte[] encryptedSecret, byte[] nonce, String mask, String state, Instant createdAt) {}
  public record CredentialSummary(UUID id, Provider provider, String displayName, String baseUrl, String mask, String state, Instant createdAt) {}
  public record Deployment(UUID id, UUID credentialId, Provider provider, String credentialName, String modelId, String state, Instant createdAt, Integer candidateSlot, Instant chatVerifiedAt) {}
  public record Usage(long inputTokens, long outputTokens, long adminTests, long evaluations) {}
}
