package ar.edu.utn.frc.tup.piv.llm.provider.anthropic;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class AnthropicProviderAdapter implements AiProviderAdapter {
  private static final ProviderCapabilities CAPABILITIES = new ProviderCapabilities(true, true, false, false, true, true, true, false);
  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  AnthropicProviderAdapter(ObjectMapper json) { this.json = json; }
  @Override public ProviderDescriptor descriptor() { return new ProviderDescriptor("anthropic", "Anthropic", "1", List.of(new CredentialField("apiKey", "API key", true, true, "Nunca se devuelve al cliente")), CAPABILITIES); }
  @Override public void validate(ProviderCredentialMaterial credential) { if (credential.secret("apiKey") == null || credential.secret("apiKey").isBlank()) throw new ProviderException("INVALID_CREDENTIAL", "apiKey es obligatoria"); }
  @Override public List<ModelDescriptor> discoverModels(ProviderCredentialMaterial credential) {
    validate(credential);
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/models")).timeout(Duration.ofSeconds(20)).headers("x-api-key", credential.secret("apiKey"), "anthropic-version", "2023-06-01").GET().build();
      JsonNode values = json.readTree(send(request)).path("data");
      return values.findValuesAsText("id").stream().filter(value -> !value.isBlank()).sorted().map(value -> new ModelDescriptor(value, value, null, CAPABILITIES, Map.of())).toList();
    } catch (ProviderException exception) { throw exception; } catch (Exception exception) { throw new ProviderException("MODEL_DISCOVERY_FAILED", "No se pudieron descubrir modelos", exception); }
  }
  @Override public ProviderReply invoke(ProviderCredentialMaterial credential, ProviderInvocation invocation) {
    validate(credential);
    try {
      InferenceSettings settings = invocation.settings();
      var builder = AnthropicChatModel.builder().apiKey(credential.secret("apiKey")).modelName(invocation.modelId()).maxTokens(settings.maxOutputTokens()).timeout(invocation.timeout()).logRequests(false).logResponses(false);
      if (settings.temperature() != null) builder.temperature(settings.temperature());
      if (settings.topP() != null) builder.topP(settings.topP());
      if (settings.topK() != null) builder.topK(settings.topK());
      return new ProviderReply(builder.build().chat(invocation.prompt()), 0, 0, null);
    } catch (Exception exception) { throw new ProviderException("PROVIDER_INVOCATION_FAILED", "Anthropic no pudo responder", exception); }
  }
  private String send(HttpRequest request) { try { HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString()); if (response.statusCode() < 200 || response.statusCode() >= 300) throw new ProviderException("PROVIDER_HTTP_" + response.statusCode(), "El proveedor respondió HTTP " + response.statusCode()); return response.body(); } catch (ProviderException exception) { throw exception; } catch (Exception exception) { throw new ProviderException("PROVIDER_CONNECTION_FAILED", "No se pudo conectar con Anthropic", exception); } }
}
