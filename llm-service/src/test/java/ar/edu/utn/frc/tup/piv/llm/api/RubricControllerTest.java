package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.RubricPublicationService;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RubricControllerTest {
  private final RubricPublicationService publication = mock(RubricPublicationService.class);
  private final RubricDraftService drafts = mock(RubricDraftService.class);
  private final GoldenSetAuthorization identityAuthorization = mock(GoldenSetAuthorization.class);
  private final CourseAuthorization courseAuthorization = mock(CourseAuthorization.class);
  private final RubricController controller = new RubricController(publication, drafts, identityAuthorization, courseAuthorization);

  @Test void publishValidatesCourseBeforeCallingTheService() {
    UUID courseId = UUID.randomUUID(); UUID versionId = UUID.randomUUID(); HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "request", null);
    when(identityAuthorization.require(headers)).thenReturn(actor);

    controller.publish(courseId, versionId, headers);

    var order = org.mockito.Mockito.inOrder(identityAuthorization, courseAuthorization, publication);
    order.verify(identityAuthorization).require(headers);
    order.verify(courseAuthorization).requireTeacher(courseId, actor, headers);
    order.verify(publication).publish(courseId, versionId, actor);
  }
}
