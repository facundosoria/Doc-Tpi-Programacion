package ar.edu.utn.frc.tup.piv.llm.shadow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowRunService;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class ShadowRunControllerTest {
  private final ShadowRunService service = mock(ShadowRunService.class);
  private final GoldenSetAuthorization auth = mock(GoldenSetAuthorization.class);
  private final CourseAuthorization courses = mock(CourseAuthorization.class);
  private final ShadowRunController controller = new ShadowRunController(service, auth, courses);
  private final UUID course = UUID.randomUUID();
  private final HttpHeaders headers = new HttpHeaders();
  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), null, null);

  private ShadowRun run(ShadowRun.State state) {
    return new ShadowRun(UUID.randomUUID(), course, UUID.randomUUID(), UUID.randomUUID(), ShadowRun.Source.TUTOR_CONVERSATIONS,
        null, 10, 10, state, 0, null, null, actor.delegatedUserId(), OffsetDateTime.now(), null, null);
  }

  @Test
  void createRequiresATeacherOfTheCourseAndAnswersAccepted() {
    when(auth.require(headers)).thenReturn(actor);
    var candidate = UUID.randomUUID();
    var key = UUID.randomUUID();
    var queued = run(ShadowRun.State.QUEUED);
    when(service.enqueue(eq(course), any(), eq(key), eq(actor))).thenReturn(queued);

    var response = controller.create(course, new ShadowRunController.Request(candidate, ShadowRun.Source.TUTOR_CONVERSATIONS,
        null, 10, null), key, headers);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isEqualTo(queued);
    verify(courses).requireTeacher(course, actor, headers);
  }

  @Test
  void listAndGetAlsoRequireATeacherOfTheCourse() {
    when(auth.require(headers)).thenReturn(actor);
    var r = run(ShadowRun.State.COMPLETED);
    when(service.list(course)).thenReturn(List.of(r));
    when(service.get(course, r.id())).thenReturn(r);

    assertThat(controller.list(course, headers).items()).containsExactly(r);
    assertThat(controller.get(course, r.id(), headers)).isEqualTo(r);
    verify(courses, org.mockito.Mockito.times(2)).requireTeacher(course, actor, headers);
  }
}
