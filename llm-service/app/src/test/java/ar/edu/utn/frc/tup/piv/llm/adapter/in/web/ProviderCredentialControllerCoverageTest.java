package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ProviderCredentialController.ChatRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ProviderCredentialController.CreateCredential;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ProviderCredentialController.CreateDeployment;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService.EncryptedSecret;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderInvocationGateway;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderRegistry;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Credential;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Deployment;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository.Usage;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ProviderCredentialControllerCoverageTest {

  private record Harness(ProviderCredentialController controller, ProviderCredentialRepository repository,
      EncryptedSecretService crypto, ProviderRegistry registry, ProviderInvocationGateway gateway,
      GoldenSetAuthorization auth, EvaluatorModelEvents events, ObjectMapper json, HttpHeaders headers) {
    static Harness build() { return build(new ObjectMapper()); }
    static Harness build(ObjectMapper json) {
      ProviderCredentialRepository repository = mock(ProviderCredentialRepository.class);
      EncryptedSecretService crypto = mock(EncryptedSecretService.class);
      ProviderRegistry registry = mock(ProviderRegistry.class);
      ProviderInvocationGateway gateway = mock(ProviderInvocationGateway.class);
      GoldenSetAuthorization auth = mock(GoldenSetAuthorization.class);
      EvaluatorModelEvents events = mock(EvaluatorModelEvents.class);
      HttpHeaders headers = new HttpHeaders();
      when(auth.require(headers)).thenReturn(new CallerIdentity("admin-service", UUID.randomUUID(), "req-1", null));
      var controller = new ProviderCredentialController(repository, crypto, registry, gateway, auth, events, json);
      return new Harness(controller, repository, crypto, registry, gateway, auth, events, json, headers);
    }
  }

  private static ProviderCapabilities capabilities() {
    return new ProviderCapabilities(true, true, true, false, false, false, false, false);
  }

  private static AiProviderAdapter adapter(String key) {
    AiProviderAdapter adapter = mock(AiProviderAdapter.class);
    when(adapter.descriptor()).thenReturn(new ProviderDescriptor(key, key, "1.0", List.of(), capabilities()));
    return adapter;
  }

  private static Credential activeCredential() {
    return new Credential(UUID.randomUUID(), "openai", "API OpenAI", Map.of("baseUrl", "x"),
        new byte[] {1}, new byte[] {2}, "•••• total", "ACTIVE", Instant.now());
  }

  private static Deployment activeDeployment(UUID credentialId) {
    return new Deployment(UUID.randomUUID(), credentialId, "openai", "API OpenAI", "gpt-4o-mini",
        "ACTIVE", Instant.now(), 1, Instant.now(), Map.of("x", "y"));
  }

  @Test
  void providersReturnsRegistryDescriptors() {
    var h = Harness.build();
    when(h.registry().descriptors()).thenReturn(List.of(new ProviderDescriptor("openai", "OpenAI", "1.0", List.of(), capabilities())));

    var page = h.controller().providers(h.headers());

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).key()).isEqualTo("openai");
  }

  @Test
  void credentialsReturnsPersistedSummaries() {
    var h = Harness.build();
    var summary = new ProviderCredentialRepository.CredentialSummary(UUID.randomUUID(), "openai",
        "API OpenAI", Map.of("baseUrl", "x"), "•••• total", "ACTIVE", Instant.now());
    when(h.repository().list()).thenReturn(List.of(summary));

    var page = h.controller().credentials(h.headers());

    assertThat(page.items()).containsExactly(summary);
  }

  @Test
  void createPersistsAnEncryptedCredential() {
    var h = Harness.build();
    AiProviderAdapter provider = adapter("openai");
    Credential saved = activeCredential();
    when(h.registry().required("openai")).thenReturn(provider);
    when(h.crypto().encrypt(anyString())).thenReturn(new EncryptedSecret("enc".getBytes(), new byte[] {7}));
    when(h.repository().create(eq("openai"), eq("API OpenAI"), eq(Map.of("baseUrl", "x")), any(), any(), any()))
        .thenReturn(saved);

    var result = h.controller().create(
        new CreateCredential("openai", "API OpenAI", Map.of("baseUrl", "x"), Map.of("apiKey", "k")), h.headers());

    assertThat(result.providerKey()).isEqualTo("openai");
    assertThat(result.state()).isEqualTo("ACTIVE");
    assertThat(result.mask()).isEqualTo("•••• total");
    verify(h.repository()).create(eq("openai"), eq("API OpenAI"), eq(Map.of("baseUrl", "x")), any(), eq("•••• configurada"), any());
  }

  @Test
  void createAcceptsNullConfigurationAndSecrets() {
    var h = Harness.build();
    AiProviderAdapter provider = adapter("openai");
    when(h.registry().required("openai")).thenReturn(provider);
    when(h.crypto().encrypt(anyString())).thenReturn(new EncryptedSecret("".getBytes(), new byte[] {}));
    when(h.repository().create(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
      String displayName = invocation.getArgument(1);
      return new ProviderCredentialRepository.Credential(UUID.randomUUID(), "openai", displayName, Map.of(),
          new byte[] {1}, new byte[] {2}, "•••• total", "ACTIVE", Instant.now());
    });

    var result = h.controller().create(new CreateCredential("openai", "solo key ", null, null), h.headers());

    assertThat(result.displayName()).isEqualTo("solo key");
  }

  @Test
  void createRejectsInvalidSecretsJson() throws Exception {
    ObjectMapper json = mock(ObjectMapper.class);
    var h = Harness.build(json);
    var aiAdapter = adapter("openai"); when(h.registry().required("openai")).thenReturn(aiAdapter);
    when(json.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

    assertThatThrownBy(() -> h.controller().create(
        new CreateCredential("openai", "API", Map.of(), Map.of("k", "v")), h.headers()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("secrets inválidos");
  }

  @Test
  void disableArchivesTheCredential() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();

    h.controller().disable(id, h.headers());

    verify(h.repository()).disable(id);
  }

  @Test
  void discoverListsProviderModels() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    AiProviderAdapter provider = adapter("openai");
    when(h.repository().get(id)).thenReturn(Optional.of(credential));
    when(h.registry().required("openai")).thenReturn(provider);
    when(h.gateway().material(credential)).thenReturn(new ProviderCredentialMaterial("openai", Map.of(), Map.of()));
    when(provider.discoverModels(any())).thenReturn(List.of(
        new ModelDescriptor("m1", "M1", "r1", capabilities(), Map.of())));

    var page = h.controller().discover(id, h.headers());

    assertThat(page.items()).extracting(ModelDescriptor::modelId).containsExactly("m1");
  }

  @Test
  void discoverRequiresAnActiveCredential() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    when(h.repository().get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> h.controller().discover(id, h.headers()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no está activa");

    when(h.repository().get(id)).thenReturn(Optional.of(new Credential(id, "openai", "API", Map.of(),
        new byte[] {}, new byte[] {}, "mask", "DISABLED", Instant.now())));

    assertThatThrownBy(() -> h.controller().discover(id, h.headers()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no está activa");
  }

  @Test
  void testModelInvokesTheProviderAndMapsTheReply() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    when(h.repository().get(id)).thenReturn(Optional.of(credential));
    when(h.gateway().invoke(any(), any(), any(), any(), any())).thenReturn(new ProviderReply("resp", 7, 3, "fp"));

    var response = h.controller().testModel(id, new CreateDeployment("model-1", 1), h.headers());

    assertThat(response.text()).isEqualTo("resp");
    assertThat(response.inputTokens()).isEqualTo(7);
    assertThat(response.outputTokens()).isEqualTo(3);
  }

  @Test
  void deploymentCreatesACandidate() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    when(h.repository().get(id)).thenReturn(Optional.of(credential));
    var aiAdapter = adapter("openai"); when(h.registry().required("openai")).thenReturn(aiAdapter);
    Deployment created = activeDeployment(credential.id());
    when(h.repository().createCandidate(any(), any(), eq(2))).thenReturn(created);

    var result = h.controller().deployment(id, new CreateDeployment("model-1", 2), h.headers());

    assertThat(result.modelId()).isEqualTo("gpt-4o-mini");
    verify(h.repository()).createCandidate(eq(id), any(ModelDescriptor.class), eq(2));
  }

  @Test
  void deploymentsListsCandidates() {
    var h = Harness.build();
    Deployment deployment = activeDeployment(UUID.randomUUID());
    when(h.repository().deployments()).thenReturn(List.of(deployment));

    var page = h.controller().deployments(h.headers());

    assertThat(page.items()).containsExactly(deployment);
  }

  @Test
  void activeAndCalibrationTargetAllowAbsentValues() {
    var h = Harness.build();
    when(h.repository().active()).thenReturn(Optional.empty());
    when(h.repository().calibrationTarget()).thenReturn(Optional.empty());

    assertThat(h.controller().active(h.headers()).getBody()).isNull();
    assertThat(h.controller().calibrationTarget(h.headers()).getBody()).isNull();

    Deployment deployment = activeDeployment(UUID.randomUUID());
    when(h.repository().active()).thenReturn(Optional.of(deployment));
    when(h.repository().calibrationTarget()).thenReturn(Optional.of(deployment));

    assertThat(h.controller().active(h.headers()).getBody()).isEqualTo(deployment);
    assertThat(h.controller().calibrationTarget(h.headers()).getBody()).isEqualTo(deployment);
  }

  @Test
  void eventsSubscribesThroughTheEmitterHub() {
    var h = Harness.build();
    SseEmitter emitter = new SseEmitter(0L);
    when(h.events().subscribe()).thenReturn(emitter);

    assertThat(h.controller().events(h.headers())).isSameAs(emitter);
  }

  @Test
  void chatInvokesDeploymentAndRecordsUsage() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    Deployment deployment = activeDeployment(credential.id());
    when(h.repository().deployment(id)).thenReturn(Optional.of(deployment));
    when(h.repository().get(credential.id())).thenReturn(Optional.of(credential));
    when(h.gateway().invoke(any(), eq("gpt-4o-mini"), any(), any(), any())).thenReturn(new ProviderReply("resp", 10, 20, "fp"));

    var response = h.controller().chat(id, new ChatRequest("hola"), h.headers());

    assertThat(response.text()).isEqualTo("resp");
    verify(h.repository()).recordUsage(id, "ADMIN_TEST", 10, 20);
  }

  @Test
  void chatThrowsWhenTheDeploymentDoesNotExist() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    when(h.repository().deployment(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> h.controller().chat(id, new ChatRequest("hola"), h.headers()))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("El modelo no existe");
  }

  @Test
  void streamChatStreamsDeltasAndCompletes() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    Deployment deployment = activeDeployment(credential.id());
    when(h.repository().deployment(id)).thenReturn(Optional.of(deployment));
    when(h.repository().get(credential.id())).thenReturn(Optional.of(credential));
    doAnswer(invocation -> {
      Consumer<String> onDelta = invocation.getArgument(5);
      onDelta.accept("tick");
      return new ProviderReply("resp", 10, 20, "fp");
    }).when(h.gateway()).stream(any(), any(), any(), any(), any(), any());

    SseEmitter emitter = h.controller().streamChat(id, new ChatRequest("hola"), h.headers());

    assertThat(emitter).isNotNull();
    verify(h.gateway(), timeout(2000)).stream(any(), anyString(), anyString(), any(), any(), any());
    verify(h.repository(), timeout(2000)).recordUsage(eq(id), eq("ADMIN_TEST"), eq(10), eq(20));
    verify(h.repository(), timeout(2000)).markChatVerified(id);
  }

  @Test
  void streamChatSendsTheErrorEventWhenTheProviderFails() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Credential credential = activeCredential();
    Deployment deployment = activeDeployment(credential.id());
    when(h.repository().deployment(id)).thenReturn(Optional.of(deployment));
    when(h.repository().get(credential.id())).thenReturn(Optional.of(credential));
    doThrow(new RuntimeException("boom")).when(h.gateway()).stream(any(), any(), any(), any(), any(), any());

    SseEmitter emitter = h.controller().streamChat(id, new ChatRequest("hola"), h.headers());

    assertThat(emitter).isNotNull();
    verify(h.gateway(), timeout(2000)).stream(any(), anyString(), anyString(), any(), any(), any());
    verify(h.repository(), never()).markChatVerified(eq(id));
    verify(h.repository(), never()).recordUsage(eq(id), eq("ADMIN_TEST"), eq(10), eq(20));
  }

  @Test
  void activateDemotesThenPromotesAndNotifies() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Deployment deployment = activeDeployment(UUID.randomUUID());
    when(h.repository().deployment(id)).thenReturn(Optional.of(deployment));

    var result = h.controller().activate(id, h.headers());

    assertThat(result).isEqualTo(deployment);
    verify(h.repository()).activate(id);
    verify(h.events()).activeModelChanged(deployment);
  }

  @Test
  void activateThrowsWhenTheDeploymentGoesMissing() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    when(h.repository().deployment(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> h.controller().activate(id, h.headers()))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void archiveCandidateDelegatesToRepository() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();

    h.controller().archiveCandidate(id, h.headers());

    verify(h.repository()).archiveCandidate(id);
  }

  @Test
  void selectForCalibrationReturnsTheUpdatedDeployment() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    Deployment deployment = activeDeployment(UUID.randomUUID());
    when(h.repository().deployment(id)).thenReturn(Optional.of(deployment));

    var result = h.controller().selectForCalibration(id, h.headers());

    assertThat(result).isEqualTo(deployment);
    verify(h.repository()).selectForCalibration(id);
  }

  @Test
  void selectForCalibrationThrowsWhenTheDeploymentGoesMissing() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    when(h.repository().deployment(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> h.controller().selectForCalibration(id, h.headers()))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void usageReturnsAggregatedTokens() {
    var h = Harness.build();
    UUID id = UUID.randomUUID();
    when(h.repository().usage(id)).thenReturn(new Usage(11, 22, 33, 44));

    var result = h.controller().usage(id, h.headers());

    assertThat(result.inputTokens()).isEqualTo(11);
    assertThat(result.evaluations()).isEqualTo(44);
  }
}