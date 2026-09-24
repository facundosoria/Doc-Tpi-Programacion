package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.EvaluatorSkillsController.EvaluatorSkillResponse;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.EvaluatorSkillsController.UnknownSkillKeyException;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.EvaluatorSkillsController.UpdateCourseSkillsRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.CourseSkillView;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.EvaluatorSkill;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EvaluatorSkillsControllerTest {

  private EvaluatorSkillRepository repository;
  private GoldenSetAuthorization authorization;
  private CourseAuthorization courseAuthorization;
  private EvaluatorSkillsController controller;

  private final HttpHeaders headers = new HttpHeaders();
  private final CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);

  @BeforeEach
  void setUp() {
    repository = mock(EvaluatorSkillRepository.class);
    authorization = mock(GoldenSetAuthorization.class);
    courseAuthorization = mock(CourseAuthorization.class);
    controller = new EvaluatorSkillsController(repository, authorization, courseAuthorization);
  }

  @Test
  void listCatalog_requiresAuthAndReturnsSkills() {
    when(authorization.require(headers)).thenReturn(actor);
    when(repository.listCatalog()).thenReturn(List.of(
        new EvaluatorSkill("rubric-alignment", "Alineación", "Desc", "PROMPT", "Inst", true),
        new EvaluatorSkill("code-syntax", "Sintaxis", "Desc2", "TOOL", "Inst2", false)
    ));

    List<EvaluatorSkillResponse> catalog = controller.listCatalog(headers);

    assertThat(catalog).hasSize(2);
    assertThat(catalog.get(0).skillKey()).isEqualTo("rubric-alignment");
    assertThat(catalog.get(0).isActive()).isTrue();
    assertThat(catalog.get(1).skillKey()).isEqualTo("code-syntax");
    assertThat(catalog.get(1).isActive()).isFalse();
    verify(authorization).require(headers);
  }

  @Test
  void listCourseSkills_authorizesTeacherAndReturnsSkillsWithStatus() {
    UUID courseId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(actor);
    when(repository.findCourseSkills(courseId)).thenReturn(List.of(
        new CourseSkillView("rubric-alignment", "Alineación", "Desc", "PROMPT", "Inst", true),
        new CourseSkillView("code-syntax", "Sintaxis", "Desc2", "TOOL", "Inst2", false)
    ));

    List<EvaluatorSkillResponse> skills = controller.listCourseSkills(courseId, headers);

    assertThat(skills).hasSize(2);
    assertThat(skills.get(0).skillKey()).isEqualTo("rubric-alignment");
    assertThat(skills.get(0).isActive()).isTrue();
    assertThat(skills.get(1).skillKey()).isEqualTo("code-syntax");
    assertThat(skills.get(1).isActive()).isFalse();
    verify(authorization).require(headers);
    verify(courseAuthorization).requireTeacher(courseId, actor, headers);
  }

  @Test
  void updateCourseSkills_authorizesTeacherAndUpdatesSkills() {
    UUID courseId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(actor);
    List<String> activeKeys = List.of("rubric-alignment");
    when(repository.allKeysExist(activeKeys)).thenReturn(true);
    when(repository.findCourseSkills(courseId)).thenReturn(List.of(
        new CourseSkillView("rubric-alignment", "Alineación", "Desc", "PROMPT", "Inst", true)
    ));

    UpdateCourseSkillsRequest request = new UpdateCourseSkillsRequest(activeKeys);
    List<EvaluatorSkillResponse> response = controller.updateCourseSkills(courseId, request, headers);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).skillKey()).isEqualTo("rubric-alignment");
    assertThat(response.get(0).isActive()).isTrue();
    verify(repository).updateCourseSkills(courseId, activeKeys);
    verify(courseAuthorization).requireTeacher(courseId, actor, headers);
  }

  @Test
  void updateCourseSkills_throwsUnknownSkillKeyExceptionWhenKeyDoesNotExist() {
    UUID courseId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(actor);
    List<String> activeKeys = List.of("invalid-skill-key");
    when(repository.allKeysExist(activeKeys)).thenReturn(false);

    UpdateCourseSkillsRequest request = new UpdateCourseSkillsRequest(activeKeys);

    assertThatThrownBy(() -> controller.updateCourseSkills(courseId, request, headers))
        .isInstanceOf(UnknownSkillKeyException.class)
        .hasMessageContaining("UNKNOWN_SKILL_KEY");
  }

  @Test
  void accessDenied_throwsForbiddenWhenNotAuthorized() {
    UUID courseId = UUID.randomUUID();
    when(authorization.require(headers))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado"));

    assertThatThrownBy(() -> controller.listCatalog(headers))
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.FORBIDDEN);

    assertThatThrownBy(() -> controller.listCourseSkills(courseId, headers))
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  void accessDenied_throwsForbiddenWhenNotTeacher() {
    UUID courseId = UUID.randomUUID();
    when(authorization.require(headers)).thenReturn(actor);
    doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No es docente"))
        .when(courseAuthorization).requireTeacher(courseId, actor, headers);

    assertThatThrownBy(() -> controller.listCourseSkills(courseId, headers))
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.FORBIDDEN);
  }
}
