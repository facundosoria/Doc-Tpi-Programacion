package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService.EncryptedSecret;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Credential;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Deployment;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Usage;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ProviderCredentialRepositoryTest {

  @FunctionalInterface
  private interface ResultSetConfigurer {
    void configure(ResultSet rs) throws SQLException;
  }

  private static ResultSet rs(ResultSetConfigurer config) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    config.configure(rs);
    return rs;
  }

  @SafeVarargs
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Answer<List<?>> rowsOf(ResultSetConfigurer... configs) {
    return invocation -> {
      RowMapper mapper = invocation.getArgument(1);
      List<Object> result = new ArrayList<>();
      for (int index = 0; index < configs.length; index++) result.add(mapper.mapRow(rs(configs[index]), index));
      return result;
    };
  }

  private static final ProviderCapabilities CAPS =
      new ProviderCapabilities(true, true, true, false, false, false, false, false);

  private static void stubCredentialQuery(JdbcTemplate jdbc, ResultSetConfigurer config) {
    when(jdbc.query(contains("encrypted_secrets"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(config));
  }

  private static void stubDeploymentQuery(JdbcTemplate jdbc, ResultSetConfigurer config) {
    when(jdbc.query(contains("from llm.model_deployments d"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(config));
  }

  @Test
  void createPersistsAndLoadsBackTheCredential() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID actor = UUID.randomUUID();
    EncryptedSecret encrypted = new EncryptedSecret("enc".getBytes(), new byte[] {9});
    stubCredentialQuery(jdbc, r -> {
      when(r.getObject(1, UUID.class)).thenReturn(UUID.randomUUID());
      when(r.getString(2)).thenReturn("openai");
      when(r.getString(3)).thenReturn("API OpenAI");
      when(r.getString(4)).thenReturn("{\"baseUrl\":\"x\"}");
      when(r.getBytes(5)).thenReturn("enc".getBytes());
      when(r.getBytes(6)).thenReturn(new byte[] {9});
      when(r.getString(7)).thenReturn("•••• total");
      when(r.getString(8)).thenReturn("ACTIVE");
      when(r.getTimestamp(9)).thenReturn(Timestamp.from(Instant.now()));
    });

    var saved = repository.create("openai", "API OpenAI", Map.of("baseUrl", "x"), encrypted, "•••• total", actor);

    assertThat(saved.providerKey()).isEqualTo("openai");
    assertThat(saved.state()).isEqualTo("ACTIVE");
    verify(jdbc).update(contains("insert into llm.provider_credentials"), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void createThrowsWhenTheInsertedRowCannotBeLoaded() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("encrypted_secrets"), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());

    assertThatThrownBy(() -> repository.create("openai", "x", Map.of(), new EncryptedSecret(new byte[0], new byte[0]), "m", UUID.randomUUID()))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void listBuildsCredentialSummaries() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("secret_mask"), any(RowMapper.class))).thenAnswer(rowsOf(r -> {
      when(r.getObject(1, UUID.class)).thenReturn(id);
      when(r.getString(2)).thenReturn("openai");
      when(r.getString(3)).thenReturn("API");
      when(r.getString(4)).thenReturn("{\"baseUrl\":\"x\"}");
      when(r.getString(5)).thenReturn("••••");
      when(r.getString(6)).thenReturn("ACTIVE");
      when(r.getTimestamp(7)).thenReturn(Timestamp.from(Instant.now()));
    }));

    var items = repository.list();

    assertThat(items).hasSize(1);
    assertThat(items.get(0).id()).isEqualTo(id);
    assertThat(items.get(0).configuration()).containsEntry("baseUrl", "x");
  }

  @Test
  void getRejectsInvalidPublicConfigurationJson() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    when(jdbc.query(contains("encrypted_secrets"), any(RowMapper.class), any(UUID.class))).thenAnswer(rowsOf(r -> {
      when(r.getString(4)).thenReturn("not-json");
    }));

    assertThatThrownBy(() -> repository.get(UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("Configuración de proveedor inválida");
  }

  @Test
  void disableRequiresAnActiveCredential() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.update(contains("state='DISABLED'"), any(UUID.class))).thenReturn(1);
    repository.disable(id);
    verify(jdbc).update(contains("state='DISABLED'"), eq(id));

    when(jdbc.update(contains("state='DISABLED'"), any(UUID.class))).thenReturn(0);
    assertThatThrownBy(() -> repository.disable(id)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no está activa");
  }

  @Test
  void createCandidateRejectsSlotsOutsideOneToThree() {
    var repository = new ProviderCredentialRepository(mock(JdbcTemplate.class), new ObjectMapper());

    assertThatThrownBy(() -> repository.createCandidate(UUID.randomUUID(), model(), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> repository.createCandidate(UUID.randomUUID(), model(), 4))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createCandidateRequiresAnActiveCredential() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID credentialId = UUID.randomUUID();
    stubCredentialQuery(jdbc, r -> {
      when(r.getString(4)).thenReturn("{}");
      when(r.getString(8)).thenReturn("DISABLED");
      when(r.getTimestamp(9)).thenReturn(Timestamp.from(Instant.now()));
    });

    assertThatThrownBy(() -> repository.createCandidate(credentialId, model(), 1))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no está activa");
  }

  @Test
  void createCandidatePersistsAndLoadsTheDeployment() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID credentialId = UUID.randomUUID();
    UUID deploymentId = UUID.randomUUID();
    stubCredentialQuery(jdbc, r -> {
      when(r.getObject(1, UUID.class)).thenReturn(credentialId);
      when(r.getString(2)).thenReturn("openai");
      when(r.getString(4)).thenReturn("{}");
      when(r.getString(8)).thenReturn("ACTIVE");
      when(r.getTimestamp(9)).thenReturn(Timestamp.from(Instant.now()));
    });
    stubDeploymentQuery(jdbc, r -> {
      when(r.getObject(1, UUID.class)).thenReturn(deploymentId);
      when(r.getObject(2, UUID.class)).thenReturn(credentialId);
      when(r.getString(3)).thenReturn("openai");
      when(r.getString(4)).thenReturn("API");
      when(r.getString(5)).thenReturn("gpt-4o-mini");
      when(r.getString(6)).thenReturn("CANDIDATE");
      when(r.getTimestamp(7)).thenReturn(Timestamp.from(Instant.now()));
      when(r.getObject(8, Integer.class)).thenReturn(1);
      when(r.getTimestamp(9)).thenReturn(null);
      when(r.getString(10)).thenReturn("{}");
    });

    var deployment = repository.createCandidate(credentialId, model(), 1);

    assertThat(deployment.modelId()).isEqualTo("gpt-4o-mini");
    assertThat(deployment.candidateSlot()).isEqualTo(1);
    verify(jdbc).update(contains("insert into llm.model_deployments"), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void deploymentQueriesBuildDeploymentRows() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    ResultSetConfigurer config = r -> {
      when(r.getObject(1, UUID.class)).thenReturn(id);
      when(r.getObject(2, UUID.class)).thenReturn(UUID.randomUUID());
      when(r.getString(3)).thenReturn("openai");
      when(r.getString(4)).thenReturn("API");
      when(r.getString(5)).thenReturn("gpt-4o-mini");
      when(r.getString(6)).thenReturn("ACTIVE");
      when(r.getTimestamp(7)).thenReturn(Timestamp.from(Instant.now()));
      when(r.getObject(8, Integer.class)).thenReturn(2);
      when(r.getTimestamp(9)).thenReturn(null);
      when(r.getString(10)).thenReturn("{}");
    };
    when(jdbc.query(contains("d.candidate_slot is not null"), any(RowMapper.class))).thenAnswer(rowsOf(config));
    when(jdbc.query(contains("d.evaluator_state='ACTIVE'"), any(RowMapper.class))).thenAnswer(rowsOf(config));
    when(jdbc.query(contains("d.calibration_target=true"), any(RowMapper.class))).thenAnswer(rowsOf(config));

    assertThat(repository.deployments()).hasSize(1);
    assertThat(repository.active()).isPresent();
    assertThat(repository.calibrationTarget()).isPresent();

    stubDeploymentQuery(jdbc, config);
    assertThat(repository.deployment(id)).isPresent();
    assertThat(repository.forId(id)).isPresent();
  }

  @Test
  void selectForCalibrationUpdatesAndFailsWhenCandidateCannotBeSelected() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.update(contains("calibration_target=true where id=?"), any(UUID.class))).thenReturn(1);
    repository.selectForCalibration(id);

    when(jdbc.update(contains("calibration_target=true where id=?"), any(UUID.class))).thenReturn(0);
    assertThatThrownBy(() -> repository.selectForCalibration(id)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no existe o está deshabilitado");
  }

  @Test
  void activateRequiresAChatVerifiedCandidate() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.update(contains("evaluator_state='ACTIVE',calibration_target=true where id=?"), any(UUID.class))).thenReturn(1);
    repository.activate(id);

    when(jdbc.update(contains("evaluator_state='ACTIVE',calibration_target=true where id=?"), any(UUID.class))).thenReturn(0);
    assertThatThrownBy(() -> repository.activate(id)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("responder correctamente");
  }

  @Test
  void markChatVerifiedAndRecordUsagePersist() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();

    repository.markChatVerified(id);
    repository.recordUsage(id, "ADMIN_TEST", 3, 4);

    verify(jdbc).update(contains("chat_verified_at=now()"), eq(id));
    verify(jdbc).update(contains("insert into llm.llm_usage_records"), eq(id), eq("ADMIN_TEST"), eq(3), eq(4));
  }

  @Test
  void archiveCandidateFailsWhenTheCandidateDoesNotExist() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.update(contains("candidate_archived_at=now(),candidate_slot=null"), any(UUID.class))).thenReturn(1);
    repository.archiveCandidate(id);

    when(jdbc.update(contains("candidate_archived_at=now(),candidate_slot=null"), any(UUID.class))).thenReturn(0);
    assertThatThrownBy(() -> repository.archiveCandidate(id)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no existe");
  }

  @Test
  void usageAggregatesAndFallsBackToZerosWhenEmpty() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new ProviderCredentialRepository(jdbc, new ObjectMapper());
    UUID id = UUID.randomUUID();
    when(jdbc.query(contains("coalesce(sum(input_tokens),0)"), any(RowMapper.class), any(UUID.class)))
        .thenAnswer(rowsOf(r -> {
          when(r.getLong(1)).thenReturn(11L);
          when(r.getLong(2)).thenReturn(22L);
          when(r.getLong(3)).thenReturn(33L);
          when(r.getLong(4)).thenReturn(44L);
        }));

    assertThat(repository.usage(id)).isEqualTo(new Usage(11, 22, 33, 44));

    when(jdbc.query(contains("coalesce(sum(input_tokens),0)"), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());
    assertThat(repository.usage(id)).isEqualTo(new Usage(0, 0, 0, 0));
  }

  @Test
  void createRejectsUnserializableConfiguration() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    ObjectMapper failing = mock(ObjectMapper.class);
    when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
    var repository = new ProviderCredentialRepository(jdbc, failing);

    assertThatThrownBy(() -> repository.create("openai", "x", Map.of("k", "v"),
        new EncryptedSecret(new byte[0], new byte[0]), "m", UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo serializar");
  }

  @Test
  void createCandidateRejectsUnserializableCapabilities() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    ObjectMapper failing = mock(ObjectMapper.class);
    when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
    var repository = new ProviderCredentialRepository(jdbc, failing);
    UUID credentialId = UUID.randomUUID();
    stubCredentialQuery(jdbc, r -> {
      when(r.getString(4)).thenReturn("{}");
      when(r.getString(8)).thenReturn("ACTIVE");
      when(r.getTimestamp(9)).thenReturn(Timestamp.from(Instant.now()));
    });

    assertThatThrownBy(() -> repository.createCandidate(credentialId, model(), 1))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No se pudo serializar");
  }

  private static ModelDescriptor model() {
    return new ModelDescriptor("gpt-4o-mini", "GPT-4o Mini", "rev-1", CAPS, Map.of());
  }
}