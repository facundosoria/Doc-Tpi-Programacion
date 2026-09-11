package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway.Provider;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController @RequestMapping("/api/llm/admin")
public class ProviderCredentialController {
  private final ProviderCredentialRepository repository; private final EncryptedSecretService crypto; private final ProviderLlmGateway gateway; private final GoldenSetAuthorization authorization; private final EvaluatorModelEvents events;
  public ProviderCredentialController(ProviderCredentialRepository repository, EncryptedSecretService crypto, ProviderLlmGateway gateway, GoldenSetAuthorization authorization, EvaluatorModelEvents events) { this.repository=repository; this.crypto=crypto; this.gateway=gateway; this.authorization=authorization; this.events=events; }
  @GetMapping("/provider-credentials") public CredentialPage credentials(@RequestHeader HttpHeaders headers) { authorization.require(headers); return new CredentialPage(repository.list()); }
  @PostMapping("/provider-credentials") public ProviderCredentialRepository.CredentialSummary create(@Valid @RequestBody CreateCredential request, @RequestHeader HttpHeaders headers) { var actor=authorization.require(headers); String secret=request.apiKey().trim(); if (secret.length()<8) throw new IllegalArgumentException("La API key es inválida"); if (request.provider()==Provider.OPENAI_COMPATIBLE) gateway.validateBaseUrl(request.baseUrl()); var saved=repository.create(request.provider(), request.name().trim(), request.provider()==Provider.OPENAI_COMPATIBLE ? request.baseUrl().trim() : null, crypto.encrypt(secret), mask(secret), actor.delegatedUserId()); return new ProviderCredentialRepository.CredentialSummary(saved.id(),saved.provider(),saved.displayName(),saved.baseUrl(),saved.mask(),saved.state(),saved.createdAt()); }
  @DeleteMapping("/provider-credentials/{id}") public void disable(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); repository.disable(id); }
  @PostMapping("/provider-credentials/{id}/test") public ModelPage test(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); var credential=credential(id); return new ModelPage(gateway.listModels(credential.provider(),credential.baseUrl(),crypto.decrypt(credential.encryptedSecret(),credential.nonce()))); }
  @PostMapping("/provider-credentials/{id}/test-model") public ChatResponse testModel(@PathVariable UUID id,@Valid @RequestBody CreateDeployment request,@RequestHeader HttpHeaders headers) { authorization.require(headers); var credential=credential(id); var reply=gateway.chat(credential.provider(),credential.baseUrl(),crypto.decrypt(credential.encryptedSecret(),credential.nonce()),request.modelId().trim(),"Respondé únicamente: conexión operativa."); return new ChatResponse(reply.text(),reply.inputTokens(),reply.outputTokens()); }
  @PostMapping("/provider-credentials/{id}/deployments") public ProviderCredentialRepository.Deployment deployment(@PathVariable UUID id,@Valid @RequestBody CreateDeployment request,@RequestHeader HttpHeaders headers) { authorization.require(headers); credential(id); return repository.createCandidate(id,request.modelId().trim(),request.slot()); }
  @GetMapping("/evaluator-models") public DeploymentPage deployments(@RequestHeader HttpHeaders headers) { authorization.require(headers); return new DeploymentPage(repository.deployments()); }
  @GetMapping("/evaluator-models/active") public ProviderCredentialRepository.Deployment active(@RequestHeader HttpHeaders headers) { authorization.require(headers); return repository.active().orElseThrow(() -> new IllegalStateException("No hay modelo evaluador activo")); }
  @GetMapping("/evaluator-models/calibration-target") public ProviderCredentialRepository.Deployment calibrationTarget(@RequestHeader HttpHeaders headers) { authorization.require(headers); return repository.calibrationTarget().orElseThrow(() -> new IllegalStateException("No hay modelo candidato para calibrar")); }
  @GetMapping(value="/evaluator-models/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter events(@RequestHeader HttpHeaders headers) { authorization.require(headers); return events.subscribe(); }
  @PostMapping("/evaluator-models/{id}/chat") public ChatResponse chat(@PathVariable UUID id,@Valid @RequestBody ChatRequest request,@RequestHeader HttpHeaders headers) { authorization.require(headers); var deployment=repository.deployment(id).orElseThrow(() -> new IllegalStateException("El modelo no existe")); var credential=credential(deployment.credentialId()); var reply=gateway.chat(deployment.provider(),credential.baseUrl(),crypto.decrypt(credential.encryptedSecret(),credential.nonce()),deployment.modelId(),request.message().trim()); repository.recordUsage(id,"ADMIN_TEST",reply.inputTokens(),reply.outputTokens()); return new ChatResponse(reply.text(),reply.inputTokens(),reply.outputTokens()); }
  @PostMapping(value="/evaluator-models/{id}/chat/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter streamChat(@PathVariable UUID id,@Valid @RequestBody ChatRequest request,@RequestHeader HttpHeaders headers) { authorization.require(headers); var deployment=repository.deployment(id).orElseThrow(() -> new IllegalStateException("El modelo no existe")); var credential=credential(deployment.credentialId()); var emitter=new SseEmitter(120_000L); Thread.ofVirtual().start(() -> { try { var reply=gateway.streamChat(deployment.provider(),credential.baseUrl(),crypto.decrypt(credential.encryptedSecret(),credential.nonce()),deployment.modelId(),request.message().trim(),delta -> send(emitter,"delta",delta)); repository.recordUsage(id,"ADMIN_TEST",reply.inputTokens(),reply.outputTokens()); repository.markChatVerified(id); emitter.send(SseEmitter.event().name("done").data("")); emitter.complete(); } catch (Exception exception) { send(emitter,"error",chatFailure(exception)); emitter.complete(); } }); return emitter; }
  @PostMapping("/evaluator-models/{id}/activate") public ProviderCredentialRepository.Deployment activate(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); repository.activate(id); var active=repository.deployment(id).orElseThrow(); events.activeModelChanged(active); return active; }
  @DeleteMapping("/evaluator-models/{id}") public void archiveCandidate(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); repository.archiveCandidate(id); }
  @PostMapping("/evaluator-models/{id}/select-for-calibration") public ProviderCredentialRepository.Deployment selectForCalibration(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); repository.selectForCalibration(id); return repository.deployment(id).orElseThrow(); }
  @GetMapping("/evaluator-models/{id}/usage") public ProviderCredentialRepository.Usage usage(@PathVariable UUID id,@RequestHeader HttpHeaders headers) { authorization.require(headers); return repository.usage(id); }
  private ProviderCredentialRepository.Credential credential(UUID id) { return repository.get(id).filter(value -> "ACTIVE".equals(value.state())).orElseThrow(() -> new IllegalStateException("La credencial no está activa")); }
  private void send(SseEmitter emitter, String event, String data) { try { emitter.send(SseEmitter.event().name(event).data(data)); } catch (java.io.IOException exception) { throw new IllegalStateException("El cliente cerró el chat", exception); } }
  private String chatFailure(Exception exception) { String message=exception.getMessage()==null?"":exception.getMessage(); Throwable cause=exception.getCause(); while(cause!=null){message+=" "+(cause.getMessage()==null?"":cause.getMessage());cause=cause.getCause();} if(message.contains("HTTP 404")) return "El proveedor no encuentra este modelo o la ruta de chat (HTTP 404). Volvé a descubrir modelos y creá un candidato con uno disponible."; if(message.contains("HTTP 401")||message.contains("HTTP 403")) return "El proveedor rechazó la credencial para usar este modelo. Verificá sus permisos."; if(message.contains("HTTP 429")) return "El proveedor limitó las solicitudes. Esperá un momento y reintentá."; if(message.contains("HTTP 5")) return "El proveedor tiene un error temporal. Reintentá más tarde."; return "No se pudo completar la respuesta del modelo. Verificá la configuración del candidato."; }
  private String mask(String value) { return value.length() <= 7 ? "•••••••" : value.substring(0,3)+"…"+value.substring(value.length()-4); }
  public record CreateCredential(@NotNull Provider provider,@NotBlank String name,String baseUrl,@NotBlank String apiKey) {}
  public record CreateDeployment(@NotBlank String modelId, @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(3) Integer slot) { public CreateDeployment(String modelId) { this(modelId, 1); } }
  public record ChatRequest(@NotBlank String message) {}
  public record ChatResponse(String text,int inputTokens,int outputTokens) {}
  public record CredentialPage(List<ProviderCredentialRepository.CredentialSummary> items) {}
  public record ModelPage(List<String> items) {}
  public record DeploymentPage(List<ProviderCredentialRepository.Deployment> items) {}
}
