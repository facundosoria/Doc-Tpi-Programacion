package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.EligibleInteractionsService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

class EligibleInteractionsControllerCoverageTest {
  private final HttpHeaders headers = new HttpHeaders();
  private final CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);

  @Test
  void includesTheServiceSummariesInThePage() {
    var service = mock(EligibleInteractionsService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new EligibleInteractionsController(service, auth, courses);
    UUID courseId = UUID.randomUUID();
    when(auth.require(headers)).thenReturn(actor);
    var summary = new EligibleInteractionsService.EligibleInteractionSummary(UUID.randomUUID(),
        "preview", "ch-x", "Tema");
    when(service.listEligible(courseId)).thenReturn(List.of(summary));

    var page = controller.list(courseId, headers);

    assertThat(page.items()).containsExactly(summary);
  }

  @Test
  void previewThrowsNotFoundWhenTheInteractionIsUnknown() {
    var service = mock(EligibleInteractionsService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new EligibleInteractionsController(service, auth, courses);
    UUID courseId = UUID.randomUUID();
    UUID interactionId = UUID.randomUUID();
    when(auth.require(headers)).thenReturn(actor);
    when(service.anonymizePreview(courseId, interactionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.preview(courseId, interactionId, headers))
        .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode().value()).isEqualTo(404));
  }
}