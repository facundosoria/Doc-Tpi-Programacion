package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.application.worker.CalibrationRunWorker;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlatformCalibrationIT extends AbstractIntegrationIT {
  static final String PERFECT = "{\\\"AUTONOMY\\\":85,\\\"CLARITY\\\":90,\\\"PROGRESSION\\\":88,\\\"COMPLIANCE\\\":92,\\\"EFFICIENCY\\\":85}";

  @Autowired ProviderCredentialRepository models;
  @Autowired EncryptedSecretService crypto;
  @Autowired CalibrationRunWorker worker;
  @Autowired CalibrationRunRepository runs;
  HttpServer server;

  @BeforeEach
  void drainCalibrationQueue() {
    while (runs.claimNextQueued().isPresent()) {}
  }

  @AfterEach
  void stop() {
    if (server != null) server.stop(0);
  }

  private void provider(int status, String content) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/chat/completions", ex -> {
      String payload = status == 200
          ? "{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5}}"
          : "{}";
      byte[] b = payload.getBytes(StandardCharsets.UTF_8);
      ex.sendResponseHeaders(status, b.length);
      ex.getResponseBody().write(b);
      ex.close();
    });
    server.start();
    var cred = models.create("openai-compatible", "local-platform",
        java.util.Map.of("baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
        crypto.encrypt("{\"apiKey\":\"sk-local-1234567\"}"), "sk-4567", TEACHER);
    var dep = models.createCandidate(cred.id(), descriptorDeModelo("modelo-local-platform"), 3);
    models.markChatVerified(dep.id());
    models.selectForCalibration(dep.id());
  }

  private static final String PLATFORM_TRANSCRIPT =
      "[{\"role\":\"STUDENT\",\"content\":\"¿Cómo calculo la complejidad de una búsqueda binaria?\"}]";
  private static final String PLATFORM_CONTEXT =
      "{\"statement\":\"Explicar complejidad logarítmica\",\"expectedKeyPoints\":[\"mitad\",\"O(log n)\"]}";
  private static final String PLATFORM_SCORES =
      "{\"AUTONOMY\":85,\"CLARITY\":90,\"PROGRESSION\":88,\"COMPLIANCE\":92,\"EFFICIENCY\":85}";

  /** Golden set PLATFORM publicado con casos, como lo dejaría el template admin que no llegó a existir. */
  private UUID seedPlatformGoldenSet() {
    UUID family = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    jdbc.update("""
        insert into llm.golden_set_families (id, scope, course_id, name, created_by_user_id)
        values (?, 'PLATFORM', null, 'Golden set institucional de prueba', ?)""", family, TEACHER);
    jdbc.update("""
        insert into llm.golden_set_versions (id, family_id, version_no, state, revision, created_by_user_id, published_at)
        values (?, ?, 1, 'PUBLISHED', 1, ?, now())""", version, family, TEACHER);
    for (int i = 0; i < 3; i++) {
      jdbc.update("""
          insert into llm.golden_set_cases (id, golden_set_version_id, case_order, review_state, transcript, challenge_context, author, reference_scores)
          values (?, ?, ?, 'REVIEWED', cast(? as jsonb), cast(? as jsonb), 'Docente', cast(? as jsonb))""",
          UUID.randomUUID(), version, i, PLATFORM_TRANSCRIPT, PLATFORM_CONTEXT, PLATFORM_SCORES);
    }
    return version;
  }

  @Test
  void platformCalibrationActivatesTarget() throws Exception {
    provider(200, PERFECT);
    
    // Seed platform rubric and golden set
    UUID rid = UUID.fromString("10000000-0000-0000-0000-000000000002"); // already seeded globally by migrations

    // No existe endpoint admin de templates de golden sets (nunca se implementó junto con el
    // controller institucional): la evidencia PLATFORM publicada se siembra directo, igual que la
    // suite siembra otros estados globales (assignments de calibración, profiles, deployments).
    UUID gid = seedPlatformGoldenSet();

    mvc.perform(asAdmin(post("/api/llm/admin/institutional-calibration/profile"))
        .content("{\"rubricVersionId\":\"" + rid + "\",\"goldenSetVersionId\":\"" + gid + "\"}"))
        .andExpect(status().isOk());

    UUID key = UUID.randomUUID();
    String runId = body(mvc.perform(asAdmin(post("/api/llm/admin/institutional-calibration/runs"))
        .header("Idempotency-Key", key.toString()))
        .andExpect(status().isAccepted())).path("id").asText();

    worker.dispatch();

    var detail = body(mvc.perform(asAdmin(get("/api/llm/admin/institutional-calibration/runs/" + runId))).andExpect(status().isOk()));
    assertThat(detail.path("run").path("state").asText()).isEqualTo("PASSED");
    assertThat(detail.path("dimensionErrors").isEmpty()).isFalse();

    var runsList = body(mvc.perform(asAdmin(get("/api/llm/admin/institutional-calibration/runs"))).andExpect(status().isOk()));
    // La lista institucional es global y CalibrationPersistenceIT deja su propia corrida PLATFORM en
    // la BD compartida: se verifica que la corrida recién creada esté en el listado y sea la más
    // reciente (order by created_at desc), sin asumir un inventario exacto de una.
    var platformIds = java.util.stream.StreamSupport.stream(runsList.path("items").spliterator(), false)
        .map(n -> n.path("id").asText()).toList();
    assertThat(platformIds).first().isEqualTo(runId);
    
    var activeOpt = models.deployments().stream().filter(d -> "ACTIVE".equals(d.state())).findFirst();
    assertThat(activeOpt).isPresent();
    assertThat(activeOpt.get().providerKey()).isEqualTo("openai-compatible");
  }

  private static ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor descriptorDeModelo(String modelId) {
    return new ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor(modelId, modelId, null,
        new ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities(true, true, true, true, true, true, false, false),
        java.util.Map.of());
  }
}
