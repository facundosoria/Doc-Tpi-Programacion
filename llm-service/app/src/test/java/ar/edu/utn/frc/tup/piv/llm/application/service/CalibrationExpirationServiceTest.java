package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationExpirationServiceTest {

  @Test void expiresPassedRunsWhenARubricFamilyChanges() {
    var jdbc = mock(JdbcTemplate.class);
    var runs = mock(CalibrationRunRepository.class);
    UUID familyId = UUID.randomUUID(), runId = UUID.randomUUID();
    when(jdbc.queryForList(anyString(), eq(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of(runId));

    new CalibrationExpirationService(jdbc, runs).expireByRubric(familyId);

    verify(runs).expire(runId, "NEW_RUBRIC_VERSION");
  }

  @Test void expiresPassedRunsWhenAGoldenSetFamilyChanges() {
    var jdbc = mock(JdbcTemplate.class);
    var runs = mock(CalibrationRunRepository.class);
    UUID familyId = UUID.randomUUID(), runId = UUID.randomUUID();
    when(jdbc.queryForList(anyString(), eq(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(List.of(runId));

    new CalibrationExpirationService(jdbc, runs).expireByGoldenSet(familyId);

    verify(runs).expire(runId, "NEW_GOLDEN_SET_VERSION");
  }

  @Test void expiresPassedRunsBeyondTheTimeLimit() {
    var jdbc = mock(JdbcTemplate.class);
    var runs = mock(CalibrationRunRepository.class);
    UUID runId = UUID.randomUUID();
    when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(runId));

    new CalibrationExpirationService(jdbc, runs).expireByTimeLimit();

    verify(runs).expire(runId, "TIME_LIMIT_EXCEEDED");
  }

  @Test void ignoresEmptyExpirationScans() {
    var jdbc = mock(JdbcTemplate.class);
    var runs = mock(CalibrationRunRepository.class);
    when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of());

    new CalibrationExpirationService(jdbc, runs).expireByTimeLimit();

    verify(runs, never()).expire(any(), any());
  }
}