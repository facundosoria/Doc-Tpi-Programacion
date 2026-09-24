package ar.edu.utn.frc.tup.piv.llm.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalibrationExpirationWorker {
  private final CalibrationExpirationService expirations;

  public CalibrationExpirationWorker(CalibrationExpirationService expirations) {
    this.expirations = expirations;
  }

  @Scheduled(cron = "0 0 * * * *") // Run every hour
  public void evaluateExpirations() {
    expirations.expireByTimeLimit();
  }
}
