package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.SyntheticGoldenSetProposalService;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyntheticGoldenSetControllerTest {
  @Test
  void authorizesTeacherAndReturnsAcceptedSyntheticProposalJob() {
    var service = mock(SyntheticGoldenSetProposalService.class);
    var auth = mock(GoldenSetAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var controller = new SyntheticGoldenSetController(service, auth, courses);

    UUID courseId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    CallerIdentity actor = new CallerIdentity("workbench", UUID.randomUUID(), null, null);
    when(auth.require(headers)).thenReturn(actor);

    var mapper = new ObjectMapper();
    var proposal = new SyntheticGoldenSetProposalService.SyntheticCaseProposal(
        UUID.randomUUID(), mapper.createArrayNode(), mapper.createObjectNode(), "Generador Asistido / LLM", "DRAFT"
    );
    var jobResult = new SyntheticGoldenSetProposalService.SyntheticProposalResult(UUID.randomUUID(), "COMPLETED", List.of(proposal));
    when(service.propose(courseId, 2)).thenReturn(jobResult);

    var response = controller.propose(courseId, new SyntheticGoldenSetController.ProposeRequest(2), headers);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    assertThat(response.getBody().items()).hasSize(1);
    assertThat(response.getBody().items().get(0).author()).isEqualTo("Generador Asistido / LLM");
    verify(courses).requireTeacher(courseId, actor, headers);
  }
}
