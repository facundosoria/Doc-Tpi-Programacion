package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persistence for provider-neutral credentials and deployments. Provider behavior belongs to SPI modules. */
@Repository
public class ProviderCredentialRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public ProviderCredentialRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

  public Credential create(String providerKey, String name, Map<String, String> configuration,
      EncryptedSecretService.EncryptedSecret encrypted, String mask, UUID actor) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into llm.provider_credentials(id,provider_key,display_name,public_configuration,encrypted_secrets,secret_nonce,secret_mask,created_by_user_id) values(?,?,?,cast(? as jsonb),?,?,?,?)",
        id, providerKey, name, write(configuration), encrypted.value(), encrypted.nonce(), mask, actor);
    return get(id).orElseThrow();
  }

  public List<CredentialSummary> list() {
    return jdbc.query("select id,provider_key,display_name,public_configuration::text,secret_mask,state::text,created_at from llm.provider_credentials order by created_at desc",
        (rs, row) -> new CredentialSummary(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
            map(rs.getString(4)), rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant()));
  }

  public Optional<Credential> get(UUID id) {
    return jdbc.query("select id,provider_key,display_name,public_configuration::text,encrypted_secrets,secret_nonce,secret_mask,state::text,created_at from llm.provider_credentials where id=?",
        (rs, row) -> new Credential(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), map(rs.getString(4)),
            rs.getBytes(5), rs.getBytes(6), rs.getString(7), rs.getString(8), rs.getTimestamp(9).toInstant()), id).stream().findFirst();
  }

  public void disable(UUID id) {
    if (jdbc.update("update llm.provider_credentials set state='DISABLED',replaced_at=now() where id=? and state='ACTIVE'", id) != 1)
      throw new IllegalStateException("La credencial no está activa");
  }

  @Transactional
  public Deployment createCandidate(UUID credentialId, ModelDescriptor model, int slot) {
    if (slot < 1 || slot > 3) throw new IllegalArgumentException("El candidato debe ocupar una de las tres tarjetas");
    Credential credential = get(credentialId).filter(value -> "ACTIVE".equals(value.state()))
        .orElseThrow(() -> new IllegalStateException("La credencial no está activa"));
    jdbc.update("update llm.model_deployments set candidate_archived_at=now(),candidate_slot=null,calibration_target=false where candidate_slot=? and candidate_archived_at is null", slot);
    UUID id = UUID.randomUUID();
    jdbc.update("insert into llm.model_deployments(id,credential_id,provider_key,adapter_version,model_id,model_version,capabilities,evaluator_state,candidate_slot) values(?,?,?,?,?,coalesce(?, 'provider-managed'),cast(? as jsonb),'CANDIDATE',?)",
        id, credentialId, credential.providerKey(), "1", model.modelId(), model.revision(), write(model.capabilities()), slot);
    return deployment(id).orElseThrow();
  }

  public List<Deployment> deployments() { return jdbc.query(deploymentSql() + " where d.candidate_slot is not null and d.candidate_archived_at is null order by d.candidate_slot", (rs,row) -> deploymentRow(rs)); }
  public Optional<Deployment> deployment(UUID id) { return jdbc.query(deploymentSql() + " where d.id=? and d.candidate_archived_at is null", (rs,row) -> deploymentRow(rs), id).stream().findFirst(); }
  public Optional<Deployment> active() { return jdbc.query(deploymentSql() + " where d.evaluator_state='ACTIVE'", (rs,row) -> deploymentRow(rs)).stream().findFirst(); }
  public Optional<Deployment> calibrationTarget() { return jdbc.query(deploymentSql() + " where d.calibration_target=true and d.candidate_archived_at is null", (rs,row) -> deploymentRow(rs)).stream().findFirst(); }
  public Optional<Deployment> forId(UUID id) { return deployment(id); }

  public void selectForCalibration(UUID id) { jdbc.update("update llm.model_deployments set calibration_target=false where calibration_target"); if (jdbc.update("update llm.model_deployments set calibration_target=true where id=? and evaluator_state <> 'DISABLED'", id) != 1) throw new IllegalStateException("El modelo candidato no existe o está deshabilitado"); }
  /** T-658: una corrida institucional fallida vuelve a dejar el deployment fuera del target global. */
  public void excludeFromCalibrationTarget(UUID id) { jdbc.update("update llm.model_deployments set calibration_target=false where id=? and candidate_archived_at is null", id); }
  public void activate(UUID id) { jdbc.update("update llm.model_deployments set evaluator_state='CANDIDATE',calibration_target=false where evaluator_state='ACTIVE' or calibration_target"); if (jdbc.update("update llm.model_deployments set evaluator_state='ACTIVE',calibration_target=true where id=? and evaluator_state='CANDIDATE' and candidate_slot is not null and candidate_archived_at is null and chat_verified_at is not null", id) != 1) throw new IllegalStateException("El candidato debe responder correctamente en el chat antes de activarse"); }
  public void markChatVerified(UUID id) { jdbc.update("update llm.model_deployments set chat_verified_at=now() where id=? and candidate_archived_at is null", id); }
  public void archiveCandidate(UUID id) { if (jdbc.update("update llm.model_deployments set candidate_archived_at=now(),candidate_slot=null,calibration_target=false,evaluator_state=case when evaluator_state='ACTIVE' then 'CANDIDATE'::llm.evaluator_deployment_state else evaluator_state end where id=? and candidate_archived_at is null", id) != 1) throw new IllegalStateException("El candidato no existe"); }
  public void recordUsage(UUID deploymentId, String kind, int input, int output) { jdbc.update("insert into llm.llm_usage_records(model_deployment_id,usage_kind,input_tokens,output_tokens) values(?,cast(? as llm.llm_usage_kind),?,?)", deploymentId, kind, input, output); }
  public Usage usage(UUID id) { return jdbc.query("select coalesce(sum(input_tokens),0),coalesce(sum(output_tokens),0),count(*) filter(where usage_kind='ADMIN_TEST'),count(*) filter(where usage_kind='EVALUATION') from llm.llm_usage_records where model_deployment_id=?", (rs,row) -> new Usage(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4)), id).stream().findFirst().orElse(new Usage(0,0,0,0)); }

  private String deploymentSql() { return "select d.id,d.credential_id,d.provider_key,c.display_name,d.model_id,d.evaluator_state::text,d.created_at,d.candidate_slot,d.chat_verified_at,d.capabilities::text from llm.model_deployments d join llm.provider_credentials c on c.id=d.credential_id"; }
  private Deployment deploymentRow(java.sql.ResultSet rs) throws java.sql.SQLException { return new Deployment(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7).toInstant(),rs.getObject(8,Integer.class),rs.getTimestamp(9)==null?null:rs.getTimestamp(9).toInstant(),map(rs.getString(10))); }
  private Map<String,String> map(String value) { try { return json.readValue(value, new TypeReference<Map<String,String>>() { }); } catch (Exception exception) { throw new IllegalStateException("Configuración de proveedor inválida", exception); } }
  private String write(Object value) { try { return json.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("No se pudo serializar la configuración", exception); } }

  public record Credential(UUID id, String providerKey, String displayName, Map<String,String> configuration, byte[] encryptedSecrets, byte[] nonce, String mask, String state, Instant createdAt) { }
  public record CredentialSummary(UUID id, String providerKey, String displayName, Map<String,String> configuration, String mask, String state, Instant createdAt) { }
  public record Deployment(UUID id, UUID credentialId, String providerKey, String credentialName, String modelId, String state, Instant createdAt, Integer candidateSlot, Instant chatVerifiedAt, Map<String,String> capabilities) { }
  public record Usage(long inputTokens, long outputTokens, long adminTests, long evaluations) { }
}
