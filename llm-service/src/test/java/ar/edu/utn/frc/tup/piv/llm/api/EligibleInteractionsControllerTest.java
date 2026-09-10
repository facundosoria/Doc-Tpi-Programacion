package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.EligibleInteractionsService;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EligibleInteractionsControllerTest {
  @Test void authorizesAndListsEligibleInteractions() {
    var service = mock(EligibleInteractionsService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new EligibleInteractionsController(service, auth, courses);
    UUID course = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);
    when(service.listEligible(course)).thenReturn(List.of());

    var result = controller.list(course, headers);

    assertThat(result.items()).isEmpty();
    verify(courses).requireTeacher(course, actor, headers);
  }

  @Test void authorizesAndPreviewsAnonymization() {
    var service = mock(EligibleInteractionsService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new EligibleInteractionsController(service, auth, courses);
    UUID course = UUID.randomUUID(), interaction = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);
    ObjectNode node = new ObjectMapper().createObjectNode().put("author", "Interacción anonimizada");
    when(service.anonymizePreview(course, interaction)).thenReturn(Optional.of(node));

    var response = controller.preview(course, interaction, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody().path("author").asText()).isEqualTo("Interacción anonimizada");
    verify(courses).requireTeacher(course, actor, headers);
  }
}
