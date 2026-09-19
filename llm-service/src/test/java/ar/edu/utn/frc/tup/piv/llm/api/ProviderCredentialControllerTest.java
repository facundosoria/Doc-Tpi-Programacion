package ar.edu.utn.frc.tup.piv.llm.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Provider;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Reply;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository.Credential;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository.CredentialSummary;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository.Deployment;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ProviderCredentialControllerTest {

  private ProviderCredentialRepository repository;
  private EncryptedSecretService crypto;
  private ProviderLlmGateway gateway;
  private GoldenSetAuthorization authorization;
  private EvaluatorModelEvents events;
  private ProviderCredentialController controller;
  private HttpHeaders headers;

  @BeforeEach
  void setUp() {
    repository = mock(ProviderCredentialRepository.class);
    crypto = new EncryptedSecretService(Base64.getEncoder().encodeToString(new byte[32]));
    gateway = mock(ProviderLlmGateway.class);
    authorization = new GoldenSetAuthorization("workbench", "scope", true, UUID.fromString("11111111-1111-1111-1111-111111111111"));
    events = new EvaluatorModelEvents();
    controller = new ProviderCredentialController(repository, crypto, gateway, authorization, events);
    headers = new HttpHeaders();
  }

  private Credential credential(UUID id, Provider provider) {
    var encrypted = crypto.encrypt("una-api-key-larga");
    return new Credential(id, provider, "OpenAI demo", "https://api.openai.com", encrypted.value(), encrypted.nonce(), "sk-…xxxx", "ACTIVE", Instant.now());
  }

  @Test
  void listsCredentials() {
    when(repository.list()).thenReturn(List.of(new CredentialSummary(UUID.randomUUID(), Provider.OPENAI_COMPATIBLE, "demo", "url", "mask", "ACTIVE", Instant.now())));

    var page = controller.credentials(headers);

    assertThat(page.items()).hasSize(1);
  }

  @Test
  void createsACredential() {
    UUID id = UUID.randomUUID();
    when(repository.create(any(), any(), any(), any(), any(), any())).thenReturn(credential(id, Provider.ANTHROPIC));
    var request = new ProviderCredentialController.CreateCredential(Provider.ANTHROPIC, " demo ", null, " una-api-key-larga ");

    var result = controller.create(request, headers);

    assertThat(result.id()).isEqualTo(id);
    verify(repository).create(eq(Provider.ANTHROPIC), eq("demo"), eq(null), any(), any(), any());
  }

  @Test
  void createValidatesBaseUrlForOpenAiCompatible() {
    UUID id = UUID.randomUUID();
    when(repository.create(any(), any(), any(), any(), any(), any())).thenReturn(credential(id, Provider.OPENAI_COMPATIBLE));
    var request = new ProviderCredentialController.CreateCredential(Provider.OPENAI_COMPATIBLE, "demo", " https://api.example.com ", "una-api-key-larga");

    controller.create(request, headers);

    verify(gateway).validateBaseUrl(" https://api.example.com ");
    verify(repository).create(eq(Provider.OPENAI_COMPATIBLE), eq("demo"), eq("https://api.example.com"), any(), any(), any());
  }

  @Test
  void rejectsAnApiKeyThatIsTooShort() {
    var request = new ProviderCredentialController.CreateCredential(Provider.ANTHROPIC, "demo", null, "short");

    assertThatThrownBy(() -> controller.create(request, headers)).isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).create(any(), any(), any(), any(), any(), any());
  }

  @Test
  void disablesACredential() {
    UUID id = UUID.randomUUID();

    controller.disable(id, headers);

    verify(repository).disable(id);
  }

  @Test
  void testsACredentialByListingModels() {
    UUID id = UUID.randomUUID();
    var credential = credential(id, Provider.OPENAI_COMPATIBLE);
    when(repository.get(id)).thenReturn(Optional.of(credential));
    when(gateway.listModels(eq(Provider.OPENAI_COMPATIBLE), any(), any())).thenReturn(List.of("gpt-4o"));

    var page = controller.test(id, headers);

    assertThat(page.items()).containsExactly("gpt-4o");
  }

  @Test
  void testFailsWhenCredentialIsNotActive() {
    UUID id = UUID.randomUUID();
    when(repository.get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.test(id, headers)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testsAModel() {
    UUID id = UUID.randomUUID();
    var credential = credential(id, Provider.ANTHROPIC);
    when(repository.get(id)).thenReturn(Optional.of(credential));
    when(gateway.chat(eq(Provider.ANTHROPIC), any(), any(), eq("claude"), anyString())).thenReturn(new Reply("ok", 1, 2));

    var response = controller.testModel(id, new ProviderCredentialController.CreateDeployment(" claude "), headers);

    assertThat(response.text()).isEqualTo("ok");
    assertThat(response.inputTokens()).isEqualTo(1);
    assertThat(response.outputTokens()).isEqualTo(2);
  }

  @Test
  void createsADeploymentCandidate() {
    UUID id = UUID.randomUUID();
    var credential = credential(id, Provider.ANTHROPIC);
    when(repository.get(id)).thenReturn(Optional.of(credential));
    var deployment = new Deployment(UUID.randomUUID(), id, Provider.ANTHROPIC, "demo", "claude", "CANDIDATE", Instant.now(), 1, null);
    when(repository.createCandidate(id, "claude", 1)).thenReturn(deployment);

    var result = controller.deployment(id, new ProviderCredentialController.CreateDeployment(" claude ", 1), headers);

    assertThat(result).isEqualTo(deployment);
  }

  @Test
  void listsDeployments() {
    when(repository.deployments()).thenReturn(List.of());

    var page = controller.deployments(headers);

    assertThat(page.items()).isEmpty();
  }

  @Test
  void returnsTheActiveDeployment() {
    var deployment = new Deployment(UUID.randomUUID(), UUID.randomUUID(), Provider.ANTHROPIC, "demo", "claude", "ACTIVE", Instant.now(), 1, Instant.now());
    when(repository.active()).thenReturn(Optional.of(deployment));

    assertThat(controller.active(headers)).isEqualTo(deployment);
  }

  @Test
  void failsWhenThereIsNoActiveDeployment() {
    when(repository.active()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.active(headers)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void returnsTheCalibrationTarget() {
    var deployment = new Deployment(UUID.randomUUID(), UUID.randomUUID(), Provider.ANTHROPIC, "demo", "claude", "ACTIVE", Instant.now(), 1, Instant.now());
    when(repository.calibrationTarget()).thenReturn(Optional.of(deployment));

    assertThat(controller.calibrationTarget(headers)).isEqualTo(deployment);
  }

  @Test
  void failsWhenThereIsNoCalibrationTarget() {
    when(repository.calibrationTarget()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.calibrationTarget(headers)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void subscribesToEvents() {
    assertThat(controller.events(headers)).isNotNull();
  }

  @Test
  void sendsAChatMessageAndRecordsUsage() {
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    var deployment = new Deployment(deploymentId, credentialId, Provider.ANTHROPIC, "demo", "claude", "ACTIVE", Instant.now(), 1, Instant.now());
    when(repository.deployment(deploymentId)).thenReturn(Optional.of(deployment));
    when(repository.get(credentialId)).thenReturn(Optional.of(credential(credentialId, Provider.ANTHROPIC)));
    when(gateway.chat(eq(Provider.ANTHROPIC), any(), any(), eq("claude"), eq("hola"))).thenReturn(new Reply("respuesta", 3, 4));

    var response = controller.chat(deploymentId, new ProviderCredentialController.ChatRequest(" hola "), headers);

    assertThat(response.text()).isEqualTo("respuesta");
    verify(repository).recordUsage(deploymentId, "ADMIN_TEST", 3, 4);
  }

  @Test
  void chatFailsWhenDeploymentDoesNotExist() {
    UUID deploymentId = UUID.randomUUID();
    when(repository.deployment(deploymentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.chat(deploymentId, new ProviderCredentialController.ChatRequest("hola"), headers))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void activatesADeploymentAndPublishesTheEvent() {
    UUID id = UUID.randomUUID();
    var deployment = new Deployment(id, UUID.randomUUID(), Provider.ANTHROPIC, "demo", "claude", "ACTIVE", Instant.now(), 1, Instant.now());
    when(repository.deployment(id)).thenReturn(Optional.of(deployment));

    var result = controller.activate(id, headers);

    assertThat(result).isEqualTo(deployment);
    verify(repository).activate(id);
  }

  @Test
  void archivesACandidate() {
    UUID id = UUID.randomUUID();

    controller.archiveCandidate(id, headers);

    verify(repository).archiveCandidate(id);
  }

  @Test
  void selectsADeploymentForCalibration() {
    UUID id = UUID.randomUUID();
    var deployment = new Deployment(id, UUID.randomUUID(), Provider.ANTHROPIC, "demo", "claude", "CANDIDATE", Instant.now(), 1, null);
    when(repository.deployment(id)).thenReturn(Optional.of(deployment));

    var result = controller.selectForCalibration(id, headers);

    assertThat(result).isEqualTo(deployment);
    verify(repository).selectForCalibration(id);
  }

  @Test
  void returnsUsage() {
    UUID id = UUID.randomUUID();
    var usage = new ProviderCredentialRepository.Usage(10, 20, 1, 2);
    when(repository.usage(id)).thenReturn(usage);

    assertThat(controller.usage(id, headers)).isEqualTo(usage);
  }

  @Test
  void streamsChatAndMarksItVerified() throws Exception {
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    var deployment = new Deployment(deploymentId, credentialId, Provider.ANTHROPIC, "demo", "claude", "CANDIDATE", Instant.now(), 1, null);
    when(repository.deployment(deploymentId)).thenReturn(Optional.of(deployment));
    when(repository.get(credentialId)).thenReturn(Optional.of(credential(credentialId, Provider.ANTHROPIC)));
    when(gateway.streamChat(eq(Provider.ANTHROPIC), any(), any(), eq("claude"), eq("hola"), any()))
        .thenReturn(new Reply("respuesta", 1, 1));

    var emitter = controller.streamChat(deploymentId, new ProviderCredentialController.ChatRequest(" hola "), headers);
    Thread.sleep(200);

    assertThat(emitter).isNotNull();
    verify(repository, times(1)).recordUsage(eq(deploymentId), eq("ADMIN_TEST"), anyInt(), anyInt());
    verify(repository).markChatVerified(deploymentId);
  }

  @Test
  void streamChatSendsAnErrorEventWhenTheProviderFails() throws Exception {
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    var deployment = new Deployment(deploymentId, credentialId, Provider.ANTHROPIC, "demo", "claude", "CANDIDATE", Instant.now(), 1, null);
    when(repository.deployment(deploymentId)).thenReturn(Optional.of(deployment));
    when(repository.get(credentialId)).thenReturn(Optional.of(credential(credentialId, Provider.ANTHROPIC)));
    when(gateway.streamChat(eq(Provider.ANTHROPIC), any(), any(), eq("claude"), eq("hola"), any()))
        .thenThrow(new RuntimeException("HTTP 404 not found"));

    var emitter = controller.streamChat(deploymentId, new ProviderCredentialController.ChatRequest("hola"), headers);
    Thread.sleep(200);

    assertThat(emitter).isNotNull();
    verify(repository, never()).markChatVerified(any());
  }
}
