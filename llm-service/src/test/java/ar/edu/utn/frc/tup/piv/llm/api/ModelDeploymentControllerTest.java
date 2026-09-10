package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelDeploymentControllerTest {
  @Test
  void authorizesTeacherAndListsDeploymentsForCourse() {
    var repository = mock(ModelDeploymentRepository.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new ModelDeploymentController(repository, auth, courses);

    UUID courseId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);

    when(repository.listEnabledDeployments()).thenReturn(List.of(
        new ModelDeploymentRepository.ModelDeploymentSummary(UUID.randomUUID(), "openai", "gpt-4o-mini", "2024-07-18", "ENABLED")
    ));

    var page = controller.listForCourse(courseId, headers);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).modelId()).isEqualTo("gpt-4o-mini");
    verify(courses).requireTeacher(courseId, actor, headers);
  }
}
