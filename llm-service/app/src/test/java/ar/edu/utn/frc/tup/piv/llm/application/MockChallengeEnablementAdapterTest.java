package ar.edu.utn.frc.tup.piv.llm.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ChallengeCalibrationAssignmentRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockChallengeEnablementAdapterTest {

  @Test
  void enablesTheChallengeWhenAnActiveCalibrationExists() {
    var assignments = mock(ChallengeCalibrationAssignmentRepository.class);
    var adapter = new MockChallengeEnablementAdapter(assignments);
    UUID challenge = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    when(assignments.assignActive(challenge, course)).thenReturn(true);

    adapter.enable(challenge, course);

    verify(assignments).assignActive(challenge, course);
  }

  @Test
  void rejectsTheEnablementWithoutAnActiveCalibration() {
    var assignments = mock(ChallengeCalibrationAssignmentRepository.class);
    var adapter = new MockChallengeEnablementAdapter(assignments);
    UUID challenge = UUID.randomUUID();
    UUID course = UUID.randomUUID();
    when(assignments.assignActive(challenge, course)).thenReturn(false);

    assertThatThrownBy(() -> adapter.enable(challenge, course))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("No existe calibración activa válida");
  }
}