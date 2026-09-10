package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetUpdateProposalService;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoldenSetUpdateProposalControllerTest {
  @Test void authorizesCourseBeforeListingPendingProposals() {
    var service = mock(GoldenSetUpdateProposalService.class); var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new GoldenSetUpdateProposalController(service, identity, courses); UUID course = UUID.randomUUID(); HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(identity.require(headers)).thenReturn(actor); when(service.pendingForCourse(course)).thenReturn(List.of());
    assertThat(controller.pending(course, headers)).isEmpty();
    var order = org.mockito.Mockito.inOrder(identity, courses, service);
    order.verify(identity).require(headers); order.verify(courses).requireTeacher(course, actor, headers); order.verify(service).pendingForCourse(course);
  }
}
