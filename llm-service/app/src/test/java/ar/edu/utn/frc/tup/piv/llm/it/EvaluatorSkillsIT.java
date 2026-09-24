package ar.edu.utn.frc.tup.piv.llm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * IT E2E de los endpoints de evaluator-skills (US G03 / #1450-1453):
 * catálogo, lectura y actualización de skills por curso, y su consumo por el
 * ejecutor de calibración modular vía {@link EvaluatorSkillRepository#findActiveByCourse}.
 *
 * <p>La semilla se define en {@code V42__evaluator_skills_schema.sql}:
 * {@code code_quality} y {@code test_runner} tienen {@code enabled_by_default=true};
 * {@code student_autonomy} y {@code architecture_linter} {@code false}.</p>
 */
class EvaluatorSkillsIT extends AbstractIntegrationIT {

  @Autowired EvaluatorSkillRepository repository;

  @Test
  void catalogListsSeededSkillsWithEnabledByDefaultState() throws Exception {
    mvc.perform(asTeacher(get("/api/llm/evaluator-skills"), UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        // order by skill_key (alfabético): architecture_linter, code_quality, student_autonomy, test_runner
        .andExpect(jsonPath("$[0].skillKey").value("architecture_linter"))
        .andExpect(jsonPath("$[0].isActive").value(false))
        .andExpect(jsonPath("$[1].skillKey").value("code_quality"))
        .andExpect(jsonPath("$[1].isActive").value(true))
        .andExpect(jsonPath("$[2].skillKey").value("student_autonomy"))
        .andExpect(jsonPath("$[2].isActive").value(false))
        .andExpect(jsonPath("$[3].skillKey").value("test_runner"))
        .andExpect(jsonPath("$[3].isActive").value(true));
  }

  @Test
  void courseSkillsDefaultToEnabledByDefaultThenUpdateReplacesActiveSet() throws Exception {
    UUID course = UUID.randomUUID();

    mvc.perform(asTeacher(get("/api/llm/courses/" + course + "/evaluator-skills"), course))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[1].skillKey").value("code_quality"))
        .andExpect(jsonPath("$[1].isActive").value(true))
        .andExpect(jsonPath("$[3].skillKey").value("test_runner"))
        .andExpect(jsonPath("$[3].isActive").value(true));

    mvc.perform(asTeacher(put("/api/llm/courses/" + course + "/evaluator-skills"), course)
            .content("{\"activeSkillKeys\":[\"student_autonomy\",\"architecture_linter\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].skillKey").value("architecture_linter"))
        .andExpect(jsonPath("$[0].isActive").value(true))
        .andExpect(jsonPath("$[1].skillKey").value("code_quality"))
        .andExpect(jsonPath("$[1].isActive").value(false))
        .andExpect(jsonPath("$[2].skillKey").value("student_autonomy"))
        .andExpect(jsonPath("$[2].isActive").value(true))
        .andExpect(jsonPath("$[3].skillKey").value("test_runner"))
        .andExpect(jsonPath("$[3].isActive").value(false));

    // Persistencia: el mismo set activo que consume el ejecutor modular (CA3).
    assertThat(repository.findActiveSkillKeysByCourse(course))
        .containsExactly("architecture_linter", "student_autonomy");
  }

  @Test
  void unknownSkillKeyIsRejectedWith422() throws Exception {
    UUID course = UUID.randomUUID();
    mvc.perform(asTeacher(put("/api/llm/courses/" + course + "/evaluator-skills"), course)
            .content("{\"activeSkillKeys\":[\"no-such-skill\"]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("UNKNOWN_SKILL_KEY"));
  }

  @Test
  void teacherCannotReadOrUpdateSkillsOfAnotherCourse() throws Exception {
    UUID mine = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    String url = "/api/llm/courses/" + other + "/evaluator-skills";

    mvc.perform(asTeacher(get(url), mine))
        .andExpect(status().isForbidden());

    mvc.perform(asTeacher(put(url), mine)
            .content("{\"activeSkillKeys\":[\"code_quality\"]}"))
        .andExpect(status().isForbidden());
  }
}