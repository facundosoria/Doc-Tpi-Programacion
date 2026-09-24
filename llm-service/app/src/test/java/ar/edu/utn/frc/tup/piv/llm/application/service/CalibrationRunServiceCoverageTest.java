package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationReproducibilityRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationRunServiceCoverageTest {
  private final CalibrationRunRepository runs = mock(CalibrationRunRepository.class);
  private final CalibrationReproducibilityRepository artifacts = mock(CalibrationReproducibilityRepository.class);
  private final AuditRepository audit = mock(AuditRepository.class);
  private final CalibrationRunService service = new CalibrationRunService(runs, artifacts, audit);

  @Test void returnsTheFirstRunWhenEnqueueingAStabilityGroup() {
    UUID course = UUID.randomUUID(), rubric = UUID.randomUUID(), golden = UUID.randomUUID();
    UUID deployment = UUID.randomUUID(), key = UUID.randomUUID();
    var first = new CalibrationRunRepository.Run(UUID.randomUUID(), "QUEUED", 0);
    var second = new CalibrationRunRepository.Run(UUID.randomUUID(), "QUEUED", 0);
    when(runs.createStability(eq(course), eq(rubric), eq(golden), eq(deployment), eq(key),
        any(UUID.class), any())).thenReturn(List.of(first, second));
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-1", "trace-1");

    var result = service.enqueue(course, rubric, golden, deployment, key, actor);

    assertThat(result.id()).isEqualTo(first.id());
    verify(artifacts, times(2)).snapshot(any(), any(), eq(""));
    verify(audit).record(eq("calibration.queued"), eq("calibration-run"), eq(first.id()),
        eq(actor), anyString());
  }

  @Test void findsARunWithinACourse() {
    UUID course = UUID.randomUUID(), runId = UUID.randomUUID();
    when(runs.find(course, runId)).thenReturn(Optional.of(new CalibrationRunRepository.Run(runId, "PASSED", 100)));
    assertThat(service.get(course, runId).id()).isEqualTo(runId);
  }

  @Test void rejectsAnUnknownRun() {
    UUID course = UUID.randomUUID(), runId = UUID.randomUUID();
    when(runs.find(course, runId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(course, runId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("La calibración no existe en el curso");
  }

  @Test void listsRunsAndStabilityGroups() {
    UUID course = UUID.randomUUID(), runId = UUID.randomUUID(), groupId = UUID.randomUUID();
    when(runs.list(course)).thenReturn(List.of(new CalibrationRunRepository.Run(runId, "RUNNING", 30)));
    when(runs.stabilityGroups(course)).thenReturn(List.of(
        new CalibrationRunRepository.StabilityGroup(groupId, "RUNNING", null,
            java.time.Instant.now(), null, List.of())));
    assertThat(service.list(course)).hasSize(1);
    assertThat(service.stabilityGroups(course).get(0).id()).isEqualTo(groupId);
  }
}