package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseRubricOverlayControllerTest {
  private final ChallengeRubricOverlayService overlay = mock(ChallengeRubricOverlayService.class);
  private final GoldenSetAuthorization identityAuthorization = mock(GoldenSetAuthorization.class);
  private final CourseAuthorization courseAuthorization = mock(CourseAuthorization.class);
  private final CourseRubricOverlayController controller =
      new CourseRubricOverlayController(overlay, identityAuthorization, courseAuthorization);

  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "request", null);

  @Test void listAuthorizesAndDelegates() {
    UUID course = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    when(identityAuthorization.require(headers)).thenReturn(actor);
    var item = new ChallengeRubricOverlayService.CourseOverlayItem(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "Overlay A", "PUBLISHED", 2,
        "MODULAR_CUSTOM", UUID.randomUUID());
    when(overlay.listAllByCourse(course)).thenReturn(List.of(item));

    var page = controller.list(course, headers);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).name()).isEqualTo("Overlay A");
    verify(courseAuthorization).requireTeacher(course, actor, headers);
  }
}