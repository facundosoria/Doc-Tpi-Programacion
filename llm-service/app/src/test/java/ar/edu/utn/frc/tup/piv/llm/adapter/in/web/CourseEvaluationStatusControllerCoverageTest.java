package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseEvaluationStatusService;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ChallengeAssignment;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.PendingEvaluation;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CourseEvaluationStatusControllerCoverageTest {
  private final HttpHeaders headers = new HttpHeaders();
  private final CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), "req", null);

  @Test
  void exposesTheChallengeCalibrationAssignments() {
    var status = mock(CourseEvaluationStatusService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new CourseEvaluationStatusController(status, identity, courses);
    UUID courseId = UUID.randomUUID();
    when(identity.require(headers)).thenReturn(actor);
    var assignment = new ChallengeAssignment(
        UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now());
    when(status.assignments(courseId)).thenReturn(List.of(assignment));

    var page = controller.assignments(courseId, headers);

    assertThat(page.items()).containsExactly(assignment);
  }

  @Test
  void exposesThePendingEvaluations() {
    var status = mock(CourseEvaluationStatusService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new CourseEvaluationStatusController(status, identity, courses);
    UUID courseId = UUID.randomUUID();
    when(identity.require(headers)).thenReturn(actor);
    var pending = new PendingEvaluation(UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "QUEUED", "sin calibración", OffsetDateTime.now());
    when(status.pendingEvaluations(courseId)).thenReturn(List.of(pending));

    var page = controller.pendingEvaluations(courseId, headers);

    assertThat(page.items()).containsExactly(pending);
  }

  @Test
  void activeCalibrationIsNullWhenThereIsNoCalibration() {
    var status = mock(CourseEvaluationStatusService.class);
    var identity = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new CourseEvaluationStatusController(status, identity, courses);
    UUID courseId = UUID.randomUUID();
    when(identity.require(headers)).thenReturn(actor);
    when(status.activeCalibration(courseId)).thenReturn(Optional.empty());

    assertThat(controller.activeCalibration(courseId, headers).getBody()).isNull();
  }
}