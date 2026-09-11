package ar.edu.utn.frc.tup.piv.llm.application;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class CalibrationRunWorker { private final CalibrationRunRepository runs; private final RealCalibrationExecutor executor;
  public CalibrationRunWorker(CalibrationRunRepository runs, RealCalibrationExecutor executor) { this.runs=runs;this.executor=executor; }
  @Scheduled(fixedDelayString="${llm.calibrations.dispatch-delay-ms:1000}") public void dispatch() { runs.claimNextQueued().ifPresent(run -> executor.execute(run.id())); }
}
