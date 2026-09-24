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

class CalibrationFlowIT extends AbstractIntegrationIT {
  static final String PERFECT = "{\\\"AUTONOMY\\\":85,\\\"CLARITY\\\":90,\\\"PROGRESSION\\\":88,\\\"COMPLIANCE\\\":92,\\\"EFFICIENCY\\\":85}";
  static final String BAD = "{\\\"AUTONOMY\\\":5,\\\"CLARITY\\\":5,\\\"PROGRESSION\\\":5,\\\"COMPLIANCE\\\":5,\\\"EFFICIENCY\\\":5}";

  @Autowired ProviderCredentialRepository models;
  @Autowired EncryptedSecretService crypto;
  @Autowired CalibrationRunWorker worker;
  @Autowired CalibrationRunRepository runs;
  HttpServer server;

  /** El claim es global: drena corridas en cola de otros ITs para reclamar solo las propias. */
  @BeforeEach
  void drainCalibrationQueue() {
    while (runs.claimNextQueued().isPresent()) {
      // se descartan (quedan RUNNING): no interesan a este test
    }
  }

  /** Cada enqueue crea un grupo de estabilidad de tres corridas (PAR-14): hay que procesarlas todas. */
  private void dispatchStabilityGroup() {
    for (int i = 0; i < 3; i++) {
      worker.dispatch();
    }
  }

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  /** Proveedor OpenAI-compatible local; status/content definen qué responde el "modelo". */
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
    var cred = models.create("openai-compatible", "local",
        java.util.Map.of("baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
        crypto.encrypt("{\"apiKey\":\"sk-local-1234567\"}"), "sk-4567", TEACHER);
    var dep = models.createCandidate(cred.id(), descriptorDeModelo("modelo-local"), 3);
    models.markChatVerified(dep.id());
    models.activate(dep.id());
  }

  private record Setup(UUID course, String goldenSetId, String rubricId) {}

  private Setup publishedGoldenSetAndRubric() throws Exception {
    UUID c = UUID.randomUUID();
    String gbase = "/api/llm/courses/" + c + "/golden-sets";
    String gid = body(mvc.perform(asTeacher(post(gbase), c).content("{\"name\":\"B\"}"))
        .andExpect(status().isCreated())).path("id").asText();
    for (int i = 0; i < 3; i++) {
      mvc.perform(asTeacher(post(gbase + "/" + gid + "/cases"), c).content(GoldenSetFlowIT.CASE))
          .andExpect(status().isCreated());
    }
    mvc.perform(asTeacher(post(gbase + "/" + gid + "/publish"), c)).andExpect(status().isOk());
    String rid = body(mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/rubrics"), c)
        .content("{\"templateVersionId\":\"10000000-0000-0000-0000-000000000002\",\"name\":\"R\"}"))
        .andExpect(status().isCreated())).path("id").asText();
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/rubrics/" + rid + "/publish"), c))
        .andExpect(status().isNoContent());
    return new Setup(c, gid, rid);
  }

  private String enqueue(Setup s, UUID key) throws Exception {
    String body = "{\"rubricVersionId\":\"" + s.rubricId() + "\",\"goldenSetVersionId\":\"" + s.goldenSetId() + "\"}";
    return body(mvc.perform(asTeacher(post("/api/llm/courses/" + s.course() + "/calibrations"), s.course())
        .header("Idempotency-Key", key.toString()).content(body)).andExpect(status().isAccepted())).path("id").asText();
  }

  private com.fasterxml.jackson.databind.JsonNode run(Setup s, String runId) throws Exception {
    return body(mvc.perform(asTeacher(get("/api/llm/courses/" + s.course() + "/calibrations/" + runId), s.course()))
        .andExpect(status().isOk()));
  }

  @Test
  void passingRunIsActivatedForTheCourse() throws Exception {
    provider(200, PERFECT);
    var s = publishedGoldenSetAndRubric();
    UUID key = UUID.randomUUID();
    String runId = enqueue(s, key);
    assertThat(enqueue(s, key)).as("misma Idempotency-Key devuelve la misma corrida").isEqualTo(runId);
    assertThat(run(s, runId).path("state").asText()).isEqualTo("QUEUED");

    dispatchStabilityGroup();
    assertThat(run(s, runId).path("state").asText()).isEqualTo("PASSED");
    String base = "/api/llm/courses/" + s.course();
    assertThat(body(mvc.perform(asTeacher(get(base + "/calibrations"), s.course()))
        .andExpect(status().isOk())).path("items")).hasSize(3);

    var preview = body(mvc.perform(asTeacher(post(base + "/calibrations/" + runId + "/activate-preview"), s.course()))
        .andExpect(status().isOk()));
    mvc.perform(asTeacher(post(base + "/calibrations/" + runId + "/activate"), s.course())
        .content("{\"previewToken\":\"" + preview.path("previewToken").asText() + "\",\"challengeIds\":[]}"))
        .andExpect(status().isOk());
    mvc.perform(asTeacher(get(base + "/active-calibration"), s.course())).andExpect(status().isOk());
    mvc.perform(asTeacher(get(base + "/challenge-calibration-assignments"), s.course())).andExpect(status().isOk());
    mvc.perform(asTeacher(get(base + "/pending-evaluations"), s.course())).andExpect(status().isOk());
  }

  @Test
  void activationWithUnknownTokenIsRejected() throws Exception {
    provider(200, PERFECT);
    var s = publishedGoldenSetAndRubric();
    String runId = enqueue(s, UUID.randomUUID());
    dispatchStabilityGroup();
    mvc.perform(asTeacher(post("/api/llm/courses/" + s.course() + "/calibrations/" + runId + "/activate"), s.course())
        .content("{\"previewToken\":\"token-inventado\",\"challengeIds\":[]}")).andExpect(status().is4xxClientError());
  }

  @Test
  void runWithLargeErrorFailsTheThreshold() throws Exception {
    provider(200, BAD);
    var s = publishedGoldenSetAndRubric();
    String runId = enqueue(s, UUID.randomUUID());
    dispatchStabilityGroup();
    assertThat(run(s, runId).path("state").asText()).isEqualTo("FAILED");
  }

  @Test
  void malformedModelAnswerIsDiagnosed() throws Exception {
    provider(200, "no soy json");
    var s = publishedGoldenSetAndRubric();
    String runId = enqueue(s, UUID.randomUUID());
    dispatchStabilityGroup();
    var run = run(s, runId);
    assertThat(run.path("state").asText()).isEqualTo("FAILED");
  }

  @Test
  void providerErrorsAreClassified() throws Exception {
    provider(503, "");
    var s = publishedGoldenSetAndRubric();
    String runId = enqueue(s, UUID.randomUUID());
    dispatchStabilityGroup();
    assertThat(run(s, runId).path("state").asText()).isEqualTo("FAILED");
  }

  @Test
  void calibrationRequestsAreValidated() throws Exception {
    UUID c = UUID.randomUUID();
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/calibrations"), c).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(asTeacher(get("/api/llm/courses/" + c + "/calibrations/" + UUID.randomUUID()), c))
        .andExpect(status().isNotFound());
  }

  @Autowired ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService workflow;

  @Test
  void incompleteGoldenSetIsRejected() throws Exception {
    provider(200, PERFECT);
    UUID c = UUID.randomUUID();
    String gbase = "/api/llm/courses/" + c + "/golden-sets";
    String gid = body(mvc.perform(asTeacher(post(gbase), c).content("{\"name\":\"B\"}")).andExpect(status().isCreated())).path("id").asText();
    mvc.perform(asTeacher(post(gbase + "/" + gid + "/cases"), c).content(GoldenSetFlowIT.CASE)).andExpect(status().isCreated());
    
    String rid = body(mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/rubrics"), c)
        .content("{\"templateVersionId\":\"10000000-0000-0000-0000-000000000002\",\"name\":\"R\"}"))
        .andExpect(status().isCreated())).path("id").asText();
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/rubrics/" + rid + "/publish"), c)).andExpect(status().isNoContent());
    
    String reqBody = "{\"rubricVersionId\":\"" + rid + "\",\"goldenSetVersionId\":\"" + gid + "\"}";
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/calibrations"), c)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .content(reqBody))
        .andExpect(status().isBadRequest())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").value("validation_error"));
  }

  @Test
  void expirationByNewRubricKeepsHistoryAndRejectsEvaluations() throws Exception {
    provider(200, PERFECT);
    var s = publishedGoldenSetAndRubric();
    String runId = enqueue(s, UUID.randomUUID());
    dispatchStabilityGroup();
    String base = "/api/llm/courses/" + s.course();
    var preview = body(mvc.perform(asTeacher(post(base + "/calibrations/" + runId + "/activate-preview"), s.course())).andExpect(status().isOk()));
    mvc.perform(asTeacher(post(base + "/calibrations/" + runId + "/activate"), s.course())
        .content("{\"previewToken\":\"" + preview.path("previewToken").asText() + "\",\"challengeIds\":[]}")).andExpect(status().isOk());
    
    UUID challenge = UUID.randomUUID();
    jdbc.update("insert into llm.challenge_calibration_assignments(challenge_id, course_id, calibration_run_id) values (?, ?, ?)", challenge, s.course(), UUID.fromString(runId));
    
    workflow.queue(UUID.randomUUID(), challenge, UUID.randomUUID());
    int pending1 = jdbc.queryForObject("select count(*) from llm.pending_evaluations where assignment_challenge_id=?", Integer.class, challenge);
    assertThat(pending1).isZero();
    
    String newRid = body(mvc.perform(asTeacher(post(base + "/rubrics/" + s.rubricId() + "/next-version"), s.course())).andExpect(status().isCreated())).path("id").asText();
    mvc.perform(asTeacher(post(base + "/rubrics/" + newRid + "/publish"), s.course())).andExpect(status().isNoContent());
    
    var run = run(s, runId);
    assertThat(run.path("state").asText()).isEqualTo("EXPIRED");
    assertThat(run.path("expirationReason").asText()).isEqualTo("NEW_RUBRIC_VERSION");
    assertThat(run.path("maeFinal").isNull()).isFalse();
    
    workflow.queue(UUID.randomUUID(), challenge, UUID.randomUUID());
    int pending2 = jdbc.queryForObject("select count(*) from llm.pending_evaluations where assignment_challenge_id=?", Integer.class, challenge);
    assertThat(pending2).isOne();
  }

  /** Descriptor mínimo para registrar un candidato en los tests (el SPI pide el modelo completo). */
  private static ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor descriptorDeModelo(String modelId) {
    return new ar.edu.utn.frc.tup.piv.llm.provider.spi.ModelDescriptor(modelId, modelId, null,
        new ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities(
            true, true, true, true, true, true, false, false),
        java.util.Map.of());
  }
}
