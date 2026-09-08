package ar.edu.utn.frc.tup.piv.llm.application;
import java.util.UUID; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
/** Scheduling boundary; the runner enqueues a new run idempotently for each due course. */
@Component public class RecalibrationScheduler {private final RecalibrationTrigger trigger;public RecalibrationScheduler(RecalibrationTrigger t){trigger=t;}@Scheduled(cron="${llm.calibrations.monthly-cron:0 0 3 * * *}")public void monthly(){trigger.enqueueMonthlyDue();}public void modelDeploymentChanged(UUID deploymentId){trigger.enqueueForModelChange(deploymentId);}public interface RecalibrationTrigger {void enqueueMonthlyDue();void enqueueForModelChange(UUID deploymentId);}}
