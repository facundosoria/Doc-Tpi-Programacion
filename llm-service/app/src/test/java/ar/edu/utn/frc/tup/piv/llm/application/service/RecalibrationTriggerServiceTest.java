package ar.edu.utn.frc.tup.piv.llm.application.service;

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

class RecalibrationTriggerServiceTest {

  @Test void enqueuesMonthlyDueRuns() {
    var jdbc = mock(JdbcTemplate.class);
    new RecalibrationTriggerService(jdbc).enqueueMonthlyDue();
    verify(jdbc).update(anyString());
  }

  @Test void enqueuesRunsAfterAModelChange() {
    var jdbc = mock(JdbcTemplate.class);
    UUID deployment = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any())).thenReturn(1);
    new RecalibrationTriggerService(jdbc).enqueueForModelChange(deployment);
    verify(jdbc).update(anyString(), eq(deployment), eq(deployment));
  }

  @Test void handlesAnEmptyModelChangeScan() {
    var jdbc = mock(JdbcTemplate.class);
    UUID deployment = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any())).thenReturn(0);
    new RecalibrationTriggerService(jdbc).enqueueForModelChange(deployment);
    verify(jdbc).update(anyString(), eq(deployment), eq(deployment));
  }
}