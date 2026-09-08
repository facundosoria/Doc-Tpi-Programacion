package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseEvaluationStatusControllerTest {
  @Test void authorizesCourseBeforeReturningItsActiveCalibration() {
    var status = mock(CourseEvaluationStatusRepository.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CourseEvaluationStatusController(status, identity, courses); UUID courseId = UUID.randomUUID(); HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), "request", null);
    var active = new CourseEvaluationStatusRepository.ActiveCalibration(courseId, UUID.randomUUID(), OffsetDateTime.now());
    when(identity.require(headers)).thenReturn(actor); when(status.activeCalibration(courseId)).thenReturn(Optional.of(active));
    var response = controller.activeCalibration(courseId, headers);
    assertThat(response.getStatusCode().value()).isEqualTo(200); assertThat(response.getBody()).isEqualTo(active);
    var order = inOrder(identity, courses, status); order.verify(identity).require(headers); order.verify(courses).requireTeacher(courseId, actor, headers); order.verify(status).activeCalibration(courseId);
  }
}
