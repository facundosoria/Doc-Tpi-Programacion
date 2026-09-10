package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.CalibrationActivationPreviewService;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationActivationService;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationMigrationConfirmation;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ChallengeCalibrationAssignmentRepository.Preview;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationActivationControllerTest {
  @Test void previewIssuesAConfirmationForOnlyMigrableChallenges() {
    var previews = mock(CalibrationActivationPreviewService.class); var confirmations = new CalibrationMigrationConfirmation();
    var activation = mock(CalibrationActivationService.class); var status = mock(CourseEvaluationStatusRepository.class);
    var identity = mock(GoldenSetAuthorization.class); var courses = mock(CourseAuthorization.class);
    var controller = new CalibrationActivationController(previews, confirmations, activation, status, identity, courses);
    UUID courseId = UUID.randomUUID(), runId = UUID.randomUUID(), migrable = UUID.randomUUID(), locked = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders(); CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    when(identity.require(headers)).thenReturn(actor); when(previews.preview(courseId, runId)).thenReturn(new Preview(List.of(migrable), List.of(locked)));

    var result = controller.preview(courseId, runId, headers);

    assertThat(result.migrableChallengeIds()).containsExactly(migrable);
    assertThat(result.lockedChallengeIds()).containsExactly(locked);
    verify(courses).requireTeacher(courseId, actor, headers);
    assertThat(confirmations.consume(result.previewToken(), courseId, runId, Set.of(migrable))).containsExactly(migrable);
  }
}
