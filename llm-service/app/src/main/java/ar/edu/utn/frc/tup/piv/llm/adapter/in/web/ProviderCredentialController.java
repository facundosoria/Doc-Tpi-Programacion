package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderInvocationGateway;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderRegistry;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Provider-neutral administrative API. Its request shape is described by installed adapters.
 */
@RestController
@RequestMapping("${app.api.private-path}/admin")
public class ProviderCredentialController {
    private final ProviderCredentialRepository repository;
    private final EncryptedSecretService crypto;
    private final ProviderRegistry registry;
    private final ProviderInvocationGateway gateway;
    private final GoldenSetAuthorization authorization;
    private final EvaluatorModelEvents events;
    private final ObjectMapper json;
    private final GatewayExecutor executor;

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCredentialController(ProviderCredentialRepository repository, EncryptedSecretService crypto,
                                        ProviderRegistry registry, ProviderInvocationGateway gateway, GoldenSetAuthorization authorization,
                                        EvaluatorModelEvents events, ObjectMapper json, GatewayExecutor executor) {
        this.repository = repository;
        this.executor = executor;
        this.crypto = crypto;
        this.registry = registry;
        this.gateway = gateway;
        this.authorization = authorization;
        this.events = events;
        this.json = json;
    }

    /** Compatibilidad: sin gateway efectivo (presupuesto, breaker y registro de uso desactivados). */
    public ProviderCredentialController(ProviderCredentialRepository repository, EncryptedSecretService crypto,
                                        ProviderRegistry registry, ProviderInvocationGateway gateway, GoldenSetAuthorization authorization,
                                        EvaluatorModelEvents events, ObjectMapper json) {
        this(repository, crypto, registry, gateway, authorization, events, json, GatewayExecutor.disabled());
    }

    /**
     * Las pruebas de admin también pasan por el gateway (presupuesto, breaker, registro de uso) pero
     * sin reintentos: una credencial mal cargada debe fallar rápido y con su mensaje.
     */
    private ProviderReply viaGateway(String providerKey, String model, String message, int timeoutSeconds,
                                     Supplier<ProviderReply> call) {
        return executor.run(new GatewayExecutor.Spec<>(ModelFunction.EVALUATOR, providerKey, model,
                GatewayUsageLog.estimateTokens(message), Duration.ofSeconds(timeoutSeconds), call,
                reply -> new int[] {reply.inputTokens(), reply.outputTokens()}, reply -> { },
                "El proveedor no respondió a tiempo", "El proveedor no pudo responder la prueba",
                IllegalStateException::new, false));
    }

    @GetMapping("/providers")
    public ProviderPage providers(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return new ProviderPage(registry.descriptors());
    }

    @GetMapping("/provider-credentials")
    public CredentialPage credentials(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return new CredentialPage(repository.list());
    }

    @PostMapping("/provider-credentials")
    public ProviderCredentialRepository.CredentialSummary create(
            @Valid @RequestBody CreateCredential request, @RequestHeader HttpHeaders headers) {
        var actor = authorization.require(headers);
        var adapter = registry.required(request.providerKey());
        Map<String, String> configuration = request.configuration() == null ? Map.of() : Map.copyOf(request.configuration());
        Map<String, String> secrets = request.secrets() == null ? Map.of() : Map.copyOf(request.secrets());
        var material = new ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial(adapter.descriptor().key(), configuration, secrets);
        adapter.validate(material);
        String encrypted;
        try {
            encrypted = json.writeValueAsString(secrets);
        } catch (Exception exception) {
            throw new IllegalArgumentException("secrets inválidos", exception);
        }
        var saved = repository.create(adapter.descriptor().key(), request.displayName().trim(), configuration,
                crypto.encrypt(encrypted), "•••• configurada", actor.delegatedUserId());
        return new ProviderCredentialRepository.CredentialSummary(saved.id(), saved.providerKey(), saved.displayName(), saved.configuration(), saved.mask(), saved.state(), saved.createdAt());
    }

    @DeleteMapping("/provider-credentials/{id}")
    public void disable(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        repository.disable(id);
    }

    @PostMapping("/provider-credentials/{id}/discover-models")
    public ModelPage discover(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        var credential = credential(id);
        return new ModelPage(registry.required(credential.providerKey()).discoverModels(gateway.material(credential)));
    }

    @PostMapping("/provider-credentials/{id}/test-model")
    public ChatResponse testModel(@PathVariable UUID id, @Valid @RequestBody CreateDeployment request, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        var credential = credential(id);
        String prueba = "Respondé únicamente: conexión operativa.";
        var reply = viaGateway(credential.providerKey(), request.modelId().trim(), prueba, 30,
                () -> gateway.invoke(credential, request.modelId().trim(), prueba, defaults(), Duration.ofSeconds(30)));
        return new ChatResponse(reply.text(), reply.inputTokens(), reply.outputTokens());
    }

    @PostMapping("/provider-credentials/{id}/deployments")
    public ProviderCredentialRepository.Deployment deployment(@PathVariable UUID id, @Valid @RequestBody CreateDeployment request, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        var credential = credential(id);
        var adapter = registry.required(credential.providerKey());
        var model = new ModelDescriptor(request.modelId().trim(), request.modelId().trim(), null, adapter.descriptor().capabilities(), Map.of());
        return repository.createCandidate(id, model, request.slot());
    }

    @GetMapping("/evaluator-models")
    public DeploymentPage deployments(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return new DeploymentPage(repository.deployments());
    }

    @GetMapping("/evaluator-models/active")
    public ResponseEntity<ProviderCredentialRepository.Deployment> active(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return ResponseEntity.ok(repository.active().orElse(null));
    }

    @GetMapping("/evaluator-models/calibration-target")
    public ResponseEntity<ProviderCredentialRepository.Deployment> calibrationTarget(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return ResponseEntity.ok(repository.calibrationTarget().orElse(null));
    }

    @GetMapping(value = "/evaluator-models/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return events.subscribe();
    }

    @PostMapping("/evaluator-models/{id}/chat")
    public ChatResponse chat(@PathVariable UUID id, @Valid @RequestBody ChatRequest request, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        var deployment = repository.deployment(id).orElseThrow(() -> new ResourceNotFoundException("El modelo no existe"));
        var reply = viaGateway(deployment.providerKey(), deployment.modelId(), request.message().trim(), 90,
                () -> gateway.invoke(credential(deployment.credentialId()), deployment.modelId(), request.message().trim(), defaults(), Duration.ofSeconds(90)));
        repository.recordUsage(id, "ADMIN_TEST", reply.inputTokens(), reply.outputTokens());
        return new ChatResponse(reply.text(), reply.inputTokens(), reply.outputTokens());
    }

    @PostMapping(value = "/evaluator-models/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable UUID id, @Valid @RequestBody ChatRequest request, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        var deployment = repository.deployment(id).orElseThrow(() -> new ResourceNotFoundException("El modelo no existe"));
        var emitter = new SseEmitter(120_000L);
        Thread.ofVirtual().start(() -> {
            try {
                var reply = viaGateway(deployment.providerKey(), deployment.modelId(), request.message().trim(), 120,
                        () -> gateway.stream(credential(deployment.credentialId()), deployment.modelId(), request.message().trim(), defaults(), Duration.ofSeconds(90), delta -> send(emitter, "delta", delta)));
                repository.recordUsage(id, "ADMIN_TEST", reply.inputTokens(), reply.outputTokens());
                repository.markChatVerified(id);
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception exception) {
                send(emitter, "error", "No se pudo completar la respuesta del proveedor.");
                emitter.complete();
            }
        });
        return emitter;
    }

    @PostMapping("/evaluator-models/{id}/activate")
    public ProviderCredentialRepository.Deployment activate(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        repository.activate(id);
        var active = repository.deployment(id).orElseThrow();
        events.activeModelChanged(active);
        return active;
    }

    @DeleteMapping("/evaluator-models/{id}")
    public void archiveCandidate(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        repository.archiveCandidate(id);
    }

    @PostMapping("/evaluator-models/{id}/select-for-calibration")
    public ProviderCredentialRepository.Deployment selectForCalibration(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        repository.selectForCalibration(id);
        return repository.deployment(id).orElseThrow();
    }

    @GetMapping("/evaluator-models/{id}/usage")
    public ProviderCredentialRepository.Usage usage(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
        authorization.require(headers);
        return repository.usage(id);
    }

    private ProviderCredentialRepository.Credential credential(UUID id) {
        return repository.get(id).filter(value -> "ACTIVE".equals(value.state())).orElseThrow(() -> new IllegalStateException("La credencial no está activa"));
    }

    private InferenceSettings defaults() {
        return new InferenceSettings("admin-v2", null, null, null, null, false, 512);
    }

    private void send(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("El cliente cerró el chat", exception);
        }
    }

    public record CreateCredential(@NotBlank String providerKey, @NotBlank String displayName,
                                   Map<String, String> configuration, Map<String, String> secrets) {
    }

    public record CreateDeployment(@NotBlank String modelId,
                                   @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(3) Integer slot) {
        public CreateDeployment(String modelId) {
            this(modelId, 1);
        }
    }

    public record ChatRequest(@NotBlank String message) {
    }

    public record ChatResponse(String text, int inputTokens, int outputTokens) {
    }

    public record ProviderPage(List<ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor> items) {
    }

    public record CredentialPage(List<ProviderCredentialRepository.CredentialSummary> items) {
    }

    public record ModelPage(List<ModelDescriptor> items) {
    }

    public record DeploymentPage(List<ProviderCredentialRepository.Deployment> items) {
    }
}
