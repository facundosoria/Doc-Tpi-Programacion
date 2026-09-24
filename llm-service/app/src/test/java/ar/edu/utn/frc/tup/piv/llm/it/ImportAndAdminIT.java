package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportAndAdminIT extends AbstractIntegrationIT {
  private static final String ROW = GoldenSetFlowIT.CASE.replaceAll("\\s+", " ");

  private String draft(UUID c) throws Exception {
    return body(mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/golden-sets"), c).content("{\"name\":\"Import\"}"))
        .andExpect(status().isCreated())).path("id").asText();
  }

  private String batch(UUID c, String goldenSetId, String rows) throws Exception {
    String req = "{\"goldenSetVersionId\":\"" + goldenSetId + "\",\"format\":\"JSON\",\"rows\":[" + rows + "]}";
    return body(mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/golden-set-imports"), c)
        .header("Idempotency-Key", UUID.randomUUID().toString()).content(req)).andExpect(status().isCreated())).path("id").asText();
  }

  @Test
  void validBatchIsValidatedAndCommittedIntoTheDraft() throws Exception {
    UUID c = UUID.randomUUID();
    String gid = draft(c);
    String bid = batch(c, gid, ROW + "," + ROW);
    String base = "/api/llm/courses/" + c + "/golden-set-imports/" + bid;

    var validated = body(mvc.perform(asTeacher(post(base + "/validate"), c)).andExpect(status().isOk()));
    assertThat(validated.path("state").asText()).isEqualTo("READY");
    mvc.perform(asTeacher(post(base + "/commit"), c)).andExpect(status().isCreated());

    var detail = body(mvc.perform(asTeacher(get("/api/llm/courses/" + c + "/golden-sets/" + gid), c)).andExpect(status().isOk()));
    assertThat(detail.path("cases")).hasSize(2);
    // Un lote ya confirmado no se puede volver a confirmar.
    mvc.perform(asTeacher(post(base + "/commit"), c)).andExpect(status().isConflict());
  }

  @Test
  void invalidRowsBlockTheCommit() throws Exception {
    UUID c = UUID.randomUUID();
    String bid = batch(c, draft(c), "{\"author\":\"sin puntajes\"}");
    String base = "/api/llm/courses/" + c + "/golden-set-imports/" + bid;
    assertThat(body(mvc.perform(asTeacher(post(base + "/validate"), c)).andExpect(status().isOk())).path("state").asText())
        .isEqualTo("FAILED");
    mvc.perform(asTeacher(post(base + "/commit"), c)).andExpect(status().isConflict());
  }

  @Test
  void unsupportedFormatIsRejected() throws Exception {
    UUID c = UUID.randomUUID();
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/golden-set-imports"), c)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .content("{\"goldenSetVersionId\":\"" + draft(c) + "\",\"format\":\"XML\",\"rows\":[]}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void modelAssignmentsAreReadAndReplaced() throws Exception {
    UUID c = UUID.randomUUID();
    // La V26 guarda la asignación como despliegue real: el @BeforeEach de la base siembra tutor/evaluator/embedding.
    var current = body(mvc.perform(asTeacher(get("/api/llm/model-assignments/TUTOR"), c)).andExpect(status().isOk()));
    assertThat(current.path("modelDeploymentId").asText()).isNotBlank();

    String target = fakeDeployment("evaluator").toString();
    mvc.perform(asTeacher(put("/api/llm/model-assignments/TUTOR"), c).header("Idempotency-Key", UUID.randomUUID().toString())
        .content("{\"modelDeploymentId\":\"" + target + "\"}")).andExpect(status().isOk());
    assertThat(body(mvc.perform(asTeacher(get("/api/llm/model-assignments/tutor"), c)).andExpect(status().isOk())).path("modelDeploymentId").asText())
        .isEqualTo(target);
    mvc.perform(asTeacher(get("/api/llm/model-assignments/NOPE"), c)).andExpect(status().isUnprocessableEntity());
    mvc.perform(asTeacher(put("/api/llm/model-assignments/TUTOR"), c).header("Idempotency-Key", UUID.randomUUID().toString())
        .content("{}")).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void institutionalCalibrationRequiresProfileAndTarget() throws Exception {
    UUID c = UUID.randomUUID();
    // El perfil institucional es global; se parte de un estado conocido para que el caso sea determinista.
    jdbc.update("delete from llm.institutional_calibration_profiles");
    // Las rutas institucionales exigen rol ADMIN + scope llm.institutional-calibration.manage:
    // la identidad correcta es asAdmin (con asTeacher nunca se llega a la regla de negocio).
    mvc.perform(asAdmin(get("/api/llm/admin/institutional-calibration/profile"))).andExpect(status().isConflict());
    mvc.perform(asAdmin(post("/api/llm/admin/institutional-calibration/profile"))
        .content("{\"goldenSetVersionId\":\"" + draft(c) + "\",\"rubricVersionId\":\"10000000-0000-0000-0000-000000000002\"}"))
        .andExpect(status().isConflict());
    mvc.perform(asAdmin(get("/api/llm/admin/institutional-calibration/runs"))).andExpect(status().isOk());
    mvc.perform(asAdmin(post("/api/llm/admin/institutional-calibration/runs"))
        .header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isConflict());
  }

  @Test
  void syntheticProposalsAndAnonymizedPreviews() throws Exception {
    UUID c = UUID.randomUUID();
    var proposal = body(mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/synthetic-golden-set-cases"), c)
        .content("{\"count\":2}")).andExpect(status().isAccepted()));
    assertThat(proposal.path("items")).isNotEmpty();
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/synthetic-golden-set-cases"), c)).andExpect(status().isAccepted());

    mvc.perform(asTeacher(get("/api/llm/courses/" + c + "/eligible-interactions"), c)).andExpect(status().isOk());
    var preview = mvc.perform(asTeacher(post("/api/llm/courses/" + c
        + "/eligible-interactions/e1111111-1111-1111-1111-111111111111/anonymize-preview"), c))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertThat(preview).contains("[REDACTED_EMAIL]").doesNotContain("l.gomez@alumno.frc.utn.edu.ar");
    mvc.perform(asTeacher(post("/api/llm/courses/" + c + "/eligible-interactions/" + UUID.randomUUID() + "/anonymize-preview"), c))
        .andExpect(status().isNotFound());
    mvc.perform(asTeacher(get("/api/llm/courses/" + c + "/golden-set-update-proposals"), c)).andExpect(status().isOk());
  }
}
