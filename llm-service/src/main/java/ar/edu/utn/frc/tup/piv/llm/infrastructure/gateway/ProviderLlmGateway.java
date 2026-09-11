package ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Internal AI Gateway. No web controller or application service talks to a provider directly. */
@Component
public class ProviderLlmGateway {
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper json;
  public ProviderLlmGateway(ObjectMapper json) { this.json = json; }

  public List<String> listModels(Provider provider, String baseUrl, String secret) {
    var request = HttpRequest.newBuilder(modelsUri(provider, baseUrl)).timeout(Duration.ofSeconds(20))
        .headers(authHeader(provider, secret)).GET().build();
    var body = send(request);
    JsonNode root = read(body);
    List<String> models = new ArrayList<>();
    JsonNode values = provider == Provider.GEMINI ? root.path("models") : root.path("data");
    values.forEach(item -> models.add(provider == Provider.GEMINI
        ? item.path("name").asText().replaceFirst("^models/", "") : item.path("id").asText()));
    return models.stream().filter(value -> !value.isBlank()).sorted().toList();
  }

  public Reply chat(Provider provider, String baseUrl, String secret, String model, String message) {
    try {
      URI uri; String payload; HttpRequest.Builder builder;
      switch (provider) {
        case OPENAI_COMPATIBLE -> { uri = URI.create(normalize(baseUrl) + "/chat/completions"); payload = json.writeValueAsString(java.util.Map.of("model", model, "messages", List.of(java.util.Map.of("role", "user", "content", message)))); builder = HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + secret); }
        case ANTHROPIC -> { uri = URI.create("https://api.anthropic.com/v1/messages"); payload = json.writeValueAsString(java.util.Map.of("model", model, "max_tokens", 512, "messages", List.of(java.util.Map.of("role", "user", "content", message)))); builder = HttpRequest.newBuilder(uri).headers("x-api-key", secret, "anthropic-version", "2023-06-01"); }
        case GEMINI -> { uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + java.net.URLEncoder.encode(secret, java.nio.charset.StandardCharsets.UTF_8)); payload = json.writeValueAsString(java.util.Map.of("contents", List.of(java.util.Map.of("parts", List.of(java.util.Map.of("text", message)))))); builder = HttpRequest.newBuilder(uri); }
        default -> throw new IllegalStateException("Proveedor no soportado");
      }
      JsonNode root = read(send(builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build()));
      String text = switch (provider) {
        case OPENAI_COMPATIBLE -> root.path("choices").path(0).path("message").path("content").asText();
        case ANTHROPIC -> root.path("content").path(0).path("text").asText();
        case GEMINI -> root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
      };
      int input = provider == Provider.GEMINI ? root.path("usageMetadata").path("promptTokenCount").asInt() : root.path("usage").path(provider == Provider.ANTHROPIC ? "input_tokens" : "prompt_tokens").asInt();
      int output = provider == Provider.GEMINI ? root.path("usageMetadata").path("candidatesTokenCount").asInt() : root.path("usage").path(provider == Provider.ANTHROPIC ? "output_tokens" : "completion_tokens").asInt();
      return new Reply(text, input, output);
    } catch (Exception exception) { throw new IllegalStateException("El proveedor no pudo responder la prueba", exception); }
  }

  public Reply streamChat(Provider provider, String baseUrl, String secret, String model, String message, Consumer<String> onDelta) {
    if (provider != Provider.OPENAI_COMPATIBLE) { Reply reply = chat(provider, baseUrl, secret, model, message); onDelta.accept(reply.text()); return reply; }
    try {
      String payload = json.writeValueAsString(java.util.Map.of("model", model, "stream", true, "messages", List.of(java.util.Map.of("role", "user", "content", message))));
      HttpRequest request = HttpRequest.newBuilder(URI.create(normalize(baseUrl) + "/chat/completions")).timeout(Duration.ofSeconds(90)).header("Authorization", "Bearer " + secret).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
      HttpResponse<java.util.stream.Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
      if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("El proveedor respondió HTTP " + response.statusCode());
      StringBuilder complete = new StringBuilder();
      try (var lines = response.body()) { lines.filter(line -> line.startsWith("data: ") && !"data: [DONE]".equals(line)).forEach(line -> { JsonNode delta = read(line.substring(6)).path("choices").path(0).path("delta").path("content"); if (!delta.isMissingNode() && !delta.isNull()) { String text = delta.asText(); complete.append(text); onDelta.accept(text); } }); }
      return new Reply(complete.toString(), 0, 0);
    } catch (Exception exception) { throw new IllegalStateException("El proveedor no pudo transmitir la prueba", exception); }
  }

  public void validateBaseUrl(String value) {
    try {
      URI uri = URI.create(value); if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException("La URL base debe ser HTTPS válida");
      for (InetAddress address : InetAddress.getAllByName(uri.getHost())) if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) throw new IllegalArgumentException("La URL base no puede apuntar a una red privada");
    } catch (IllegalArgumentException exception) { throw exception; } catch (Exception exception) { throw new IllegalArgumentException("No se pudo validar la URL base"); }
  }
  private URI modelsUri(Provider provider, String baseUrl) { return switch (provider) { case OPENAI_COMPATIBLE -> URI.create(normalize(baseUrl) + "/models"); case ANTHROPIC -> URI.create("https://api.anthropic.com/v1/models"); case GEMINI -> URI.create("https://generativelanguage.googleapis.com/v1beta/models"); }; }
  private String[] authHeader(Provider provider, String secret) { return switch (provider) { case OPENAI_COMPATIBLE -> new String[] {"Authorization", "Bearer " + secret}; case ANTHROPIC -> new String[] {"x-api-key", secret, "anthropic-version", "2023-06-01"}; case GEMINI -> new String[] {"x-goog-api-key", secret}; }; }
  private String send(HttpRequest request) { try { HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("El proveedor respondió HTTP " + response.statusCode()); return response.body(); } catch (Exception exception) { throw new IllegalStateException("No se pudo conectar con el proveedor", exception); } }
  private JsonNode read(String body) { try { return json.readTree(body); } catch (Exception exception) { throw new IllegalStateException("Respuesta de proveedor inválida", exception); } }
  private String normalize(String baseUrl) { return baseUrl.replaceFirst("/+$", "") + "/v1".replace(baseUrl.endsWith("/v1") ? "/v1" : "", ""); }
  public enum Provider { OPENAI_COMPATIBLE, ANTHROPIC, GEMINI }
  public record Reply(String text, int inputTokens, int outputTokens) {}
}
