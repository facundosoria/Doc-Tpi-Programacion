package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoldenSetFlowIT extends AbstractIntegrationIT {

  static final String CASE = """
      {"transcript":[{"role":"STUDENT","content":"¿Cómo calculo la complejidad de una búsqueda binaria?"}],
       "challengeContext":{"statement":"Explicar complejidad logarítmica","expectedKeyPoints":["mitad","O(log n)"]},
       "author":"Docente","referenceScores":{"AUTONOMY":85,"CLARITY":90,"PROGRESSION":88,"COMPLIANCE":92,"EFFICIENCY":85},
       "scoreJustifications":{"AUTONOMY":"resolvió solo"}}""";

  private String draftWithCases(UUID course, int cases) throws Exception {
    String base = "/api/llm/courses/" + course + "/golden-sets";
    String id = body(mvc.perform(asTeacher(post(base), course).content("{\"name\":\"Banco\"}")).andExpect(status().isCreated())).path("id").asText();
    for (int i = 0; i < cases; i++) {
      mvc.perform(asTeacher(post(base + "/" + id + "/cases"), course).content(CASE)).andExpect(status().isCreated());
    }
    return id;
  }

  @Test
  void draftLifecycleFromCreationToNextVersion() throws Exception {
    UUID course = UUID.randomUUID();
    String base = "/api/llm/courses/" + course + "/golden-sets";
    String id = draftWithCases(course, 1);

    var detail = body(mvc.perform(asTeacher(get(base + "/" + id), course)).andExpect(status().isOk()));
    assertThat(detail.path("cases")).hasSize(1);
    String caseId = detail.path("cases").get(0).path("id").asText();

    // Con un solo caso no se puede publicar (exige entre 3 y 5).
    mvc.perform(asTeacher(post(base + "/" + id + "/publish"), course)).andExpect(status().isConflict());

    mvc.perform(asTeacher(patch(base + "/" + id + "/cases/" + caseId), course).content(CASE)).andExpect(status().isOk());
    for (int i = 0; i < 2; i++) {
      mvc.perform(asTeacher(post(base + "/" + id + "/cases"), course).content(CASE)).andExpect(status().isCreated());
    }
    mvc.perform(asTeacher(post(base + "/" + id + "/publish"), course)).andExpect(status().isOk());

    var list = body(mvc.perform(asTeacher(get(base), course)).andExpect(status().isOk()));
    assertThat(list.path("items")).hasSize(1);

    // Una versión publicada es inmutable: agregar casos falla y la única vía es una versión nueva.
    mvc.perform(asTeacher(post(base + "/" + id + "/cases"), course).content(CASE)).andExpect(status().is4xxClientError());
    var next = body(mvc.perform(asTeacher(post(base + "/" + id + "/next-version"), course)).andExpect(status().isCreated()));
    assertThat(next.path("version").asInt()).isEqualTo(2);
    mvc.perform(asTeacher(delete(base + "/" + next.path("id").asText()), course)).andExpect(status().isNoContent());
  }

  @Test
  void copyFromBaseRequiresAnInstitutionalPublishedBase() throws Exception {
    UUID source = UUID.randomUUID();
    String published = draftWithCases(source, 3);
    mvc.perform(asTeacher(post("/api/llm/courses/" + source + "/golden-sets/" + published + "/publish"), source)).andExpect(status().isOk());

    UUID target = UUID.randomUUID();
    mvc.perform(asTeacher(post("/api/llm/courses/" + target + "/golden-sets/copy-from-base/" + published), target))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsMissingScopeOtherCourseAndUnknownVersion() throws Exception {
    UUID course = UUID.randomUUID();
    String base = "/api/llm/courses/" + course + "/golden-sets";
    mvc.perform(get(base).header("X-Principal-Type", "service").header("X-Service-Id", "admin-service")
        .header("X-Service-Scopes", "otro.scope").header("X-Delegated-User", TEACHER.toString())).andExpect(status().isUnauthorized());
    mvc.perform(get(base)).andExpect(status().isUnauthorized());
    mvc.perform(asTeacher(get(base), UUID.randomUUID())).andExpect(status().isForbidden());
    mvc.perform(asTeacher(get(base + "/" + UUID.randomUUID()), course)).andExpect(status().isNotFound());
    mvc.perform(post(base).header("X-Principal-Type", "service").header("X-Service-Id", "admin-service")
        .header("X-Service-Scopes", "llm.golden-set.manage").header("X-Delegated-User", "no-uuid").contentType("application/json").content("{}")).andExpect(status().isForbidden());
    mvc.perform(post(base).header("X-Principal-Type", "service").header("X-Service-Id", "admin-service")
        .header("X-Service-Scopes", "llm.golden-set.manage").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  }
}
