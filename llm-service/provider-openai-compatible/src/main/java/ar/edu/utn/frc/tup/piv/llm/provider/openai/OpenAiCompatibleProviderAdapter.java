package ar.edu.utn.frc.tup.piv.llm.provider.openai;

import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.CredentialField;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCredentialMaterial;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderException;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderInvocation;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Adapter for OpenAI, Groq and any endpoint with the OpenAI chat-completions contract. */
final class OpenAiCompatibleProviderAdapter implements AiProviderAdapter {
  private static final ProviderCapabilities CAPABILITIES = new ProviderCapabilities(true, true, true, true, true, true, false, true);
  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  OpenAiCompatibleProviderAdapter(ObjectMapper json) { this.json = json; }

  @Override public ProviderDescriptor descriptor() {
    return new ProviderDescriptor("openai-compatible", "OpenAI compatible", "1",
        List.of(new CredentialField("baseUrl", "URL base", false, true, "https://api.openai.com"),
            new CredentialField("apiKey", "API key", true, true, "Nunca se devuelve al cliente")), CAPABILITIES);
  }

  @Override public void validate(ProviderCredentialMaterial credential) {
    String baseUrl = credential.configuration("baseUrl");
    if (baseUrl == null || baseUrl.isBlank() || !esUrlSegura(baseUrl))
      throw new ProviderException("INVALID_CONFIGURATION", "baseUrl debe ser una URL HTTPS válida");
    if (credential.secret("apiKey") == null || credential.secret("apiKey").isBlank())
      throw new ProviderException("INVALID_CREDENTIAL", "apiKey es obligatoria");
  }

  /** HTTPS siempre; HTTP se admite solo contra loopback (servidores OpenAI-compatible locales). */
  private boolean esUrlSegura(String baseUrl) {
    URI uri = URI.create(baseUrl);
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
    if ("https".equals(scheme)) return true;
    return "http".equals(scheme) && esLoopback(uri.getHost());
  }

  private boolean esLoopback(String host) {
    if (host == null) return false;
    if ("localhost".equalsIgnoreCase(host)) return true;
    try {
      return java.util.Arrays.stream(java.net.InetAddress.getAllByName(host))
          .anyMatch(java.net.InetAddress::isLoopbackAddress);
    } catch (java.net.UnknownHostException exception) {
      return false;
    }
  }

  @Override public List<ModelDescriptor> discoverModels(ProviderCredentialMaterial credential) {
    validate(credential);
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(credential) + "/models"))
          .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + credential.secret("apiKey")).GET().build();
      JsonNode values = json.readTree(send(request)).path("data");
      return values.findValuesAsText("id").stream().filter(value -> !value.isBlank()).sorted()
          .map(value -> new ModelDescriptor(value, value, null, CAPABILITIES, Map.of())).toList();
    } catch (ProviderException exception) { throw exception; }
      catch (Exception exception) { throw new ProviderException("MODEL_DISCOVERY_FAILED", "No se pudieron descubrir modelos", exception); }
  }

  @Override public ProviderReply invoke(ProviderCredentialMaterial credential, ProviderInvocation invocation) {
    validate(credential);
    try {
      InferenceSettings settings = invocation.settings();
      var builder = OpenAiChatModel.builder().baseUrl(baseUrl(credential)).apiKey(credential.secret("apiKey"))
          .modelName(invocation.modelId()).timeout(invocation.timeout()).maxCompletionTokens(settings.maxOutputTokens())
          .logRequests(false).logResponses(false);
      if (settings.temperature() != null) builder.temperature(settings.temperature());
      if (settings.topP() != null) builder.topP(settings.topP());
      if (settings.seed() != null) builder.seed(Math.toIntExact(settings.seed()));
      return new ProviderReply(builder.build().chat(invocation.prompt()), 0, 0, null);
    } catch (Exception exception) { throw new ProviderException("PROVIDER_INVOCATION_FAILED", "El proveedor OpenAI compatible no pudo responder", exception); }
  }

  private String baseUrl(ProviderCredentialMaterial credential) {
    String value = credential.configuration("baseUrl").replaceFirst("/+$", "");
    return value.endsWith("/v1") ? value : value + "/v1";
  }
  private String send(HttpRequest request) {
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new ProviderException("PROVIDER_HTTP_" + response.statusCode(), "El proveedor respondió HTTP " + response.statusCode());
      return response.body();
    } catch (ProviderException exception) { throw exception; }
      catch (Exception exception) { throw new ProviderException("PROVIDER_CONNECTION_FAILED", "No se pudo conectar con el proveedor", exception); }
  }
}
