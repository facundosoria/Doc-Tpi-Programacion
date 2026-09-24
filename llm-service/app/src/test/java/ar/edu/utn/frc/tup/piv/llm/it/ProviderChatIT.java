package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class ProviderChatIT extends AbstractIntegrationIT {
  static final String ADMIN = "/api/llm/admin";
  @Autowired ProviderCredentialRepository models;
  @Autowired EncryptedSecretService crypto;
  HttpServer server;
  UUID credentialId;
  UUID deploymentId;
  final UUID course = UUID.randomUUID();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/models", ex -> reply(ex, 200, "{\"data\":[{\"id\":\"m1\"},{\"id\":\"m2\"}]}"));
    server.createContext("/v1/chat/completions", ex -> {
      String req = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      if (req.contains("\"stream\":true")) {
        reply(ex, req.contains("caido") ? 404 : 200,
            "data: {\"choices\":[{\"delta\":{\"content\":\"ho\"}}]}\n\ndata: {\"choices\":[{\"delta\":{\"content\":\"la\"}}]}\n\ndata: [DONE]\n");
      } else {
        reply(ex, req.contains("caido") ? 404 : 200,
            "{\"choices\":[{\"message\":{\"content\":\"hola\"}}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":4}}");
      }
    });
    server.start();
    var cred = models.create("openai-compatible", "local", java.util.Map.of("baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
        crypto.encrypt("{\"apiKey\":\"sk-local-1234567\"}"), "sk-4567", TEACHER);
    credentialId = cred.id();
    deploymentId = models.createCandidate(cred.id(), descriptorDeModelo("modelo-local"), 2).id();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static void reply(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws java.io.IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.sendResponseHeaders(status, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.close();
  }

  private String streamed(String message) throws Exception {
    MvcResult started = mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + deploymentId + "/chat/stream"), course)
        .content("{\"message\":\"" + message + "\"}")).andReturn();
    for (int i = 0; i < 50 && !started.getRequest().isAsyncStarted(); i++) {
      Thread.sleep(50);
    }
    Thread.sleep(500);
    return started.getResponse().getContentAsString();
  }

  @Test
  void adminCanDiscoverModelsAndChatWithACandidate() throws Exception {
    var models = body(mvc.perform(asTeacher(post(ADMIN + "/provider-credentials/" + credentialId + "/discover-models"), course))
        .andExpect(status().isOk()));
    assertThat(models.path("items")).hasSize(2);

    var tested = body(mvc.perform(asTeacher(post(ADMIN + "/provider-credentials/" + credentialId + "/test-model"), course)
        .content("{\"modelId\":\"m1\"}")).andExpect(status().isOk()));
    assertThat(tested.path("text").asText()).isEqualTo("hola");

    var chat = body(mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + deploymentId + "/chat"), course)
        .content("{\"message\":\"hola\"}")).andExpect(status().isOk()));
    assertThat(chat.path("text").asText()).isEqualTo("hola");
    var usage = body(mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/" + deploymentId + "/usage"), course)).andExpect(status().isOk()));
    assertThat(usage.path("adminTests").asLong()).isGreaterThanOrEqualTo(1);

    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + deploymentId + "/chat"), course).content("{\"message\":\"\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(asTeacher(post(ADMIN + "/evaluator-models/" + deploymentId + "/chat"), course).content("{\"message\":\"caido\"}"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void streamingChatEmitsDeltasAndVerifiesTheCandidate() throws Exception {
    String sse = streamed("hola");
    assertThat(sse).contains("event:delta").contains("event:done");
    assertThat(models.deployment(deploymentId).orElseThrow().chatVerifiedAt()).isNotNull();
  }

  @Test
  void streamingChatReportsProviderFailuresAsErrorEvents() throws Exception {
    assertThat(streamed("caido")).contains("event:error");
  }

  @Test
  void adminCanSubscribeToEvaluatorModelEvents() throws Exception {
    mvc.perform(asTeacher(get(ADMIN + "/evaluator-models/events"), course)).andExpect(status().isOk());
  }

  /** Descriptor mínimo para registrar un candidato en los tests (el SPI pide el modelo completo). */
  private static ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor descriptorDeModelo(String modelId) {
    return new ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor(modelId, modelId, null,
        new ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities(
            true, true, true, true, true, true, false, false),
        java.util.Map.of());
  }
}
