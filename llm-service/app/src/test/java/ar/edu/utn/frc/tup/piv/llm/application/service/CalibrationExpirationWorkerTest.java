package ar.edu.utn.frc.tup.piv.llm.application.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalibrationExpirationWorkerTest {
  @Test void evaluatesTimeLimitExpirationsOnEachTick() {
    var expirations = mock(CalibrationExpirationService.class);
    new CalibrationExpirationWorker(expirations).evaluateExpirations();
    verify(expirations).expireByTimeLimit();
  }
}