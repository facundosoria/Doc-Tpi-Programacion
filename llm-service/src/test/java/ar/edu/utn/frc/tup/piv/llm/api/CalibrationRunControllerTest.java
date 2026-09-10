package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.CalibrationRunService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
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

class CalibrationRunControllerTest {
  @Test
  void authorizesTeacherAndListsCalibrationRuns() {
    var service = mock(CalibrationRunService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new CalibrationRunController(service, auth, courses);

    UUID courseId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);

    UUID runId = UUID.randomUUID();
    when(service.list(courseId)).thenReturn(List.of(new CalibrationRunRepository.Run(runId, "PASSED", 100)));

    var result = controller.list(courseId, headers);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).id()).isEqualTo(runId);
    assertThat(result.items().get(0).state()).isEqualTo("PASSED");
    verify(courses).requireTeacher(courseId, actor, headers);
  }

  @Test
  void authorizesTeacherAndCreatesCalibrationRun() {
    var service = mock(CalibrationRunService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new CalibrationRunController(service, auth, courses);

    UUID courseId = UUID.randomUUID();
    UUID rubricId = UUID.randomUUID();
    UUID goldenId = UUID.randomUUID();
    UUID modelId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);

    UUID runId = UUID.randomUUID();
    when(service.enqueue(courseId, rubricId, goldenId, modelId, key, actor))
        .thenReturn(new CalibrationRunRepository.Run(runId, "QUEUED", 0));

    var response = controller.create(courseId, new CalibrationRunController.Request(rubricId, goldenId, modelId), key, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    assertThat(response.getBody().id()).isEqualTo(runId);
    verify(courses).requireTeacher(courseId, actor, headers);
  }
}
