package ar.edu.utn.frc.tup.piv.llm.application;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
/** Idempotently resumes only evaluations whose challenge recovered a valid active calibration. */
@Component public class PendingEvaluationWorker {private final CalibrationWorkflowService workflow;public PendingEvaluationWorker(CalibrationWorkflowService w){workflow=w;}@Scheduled(fixedDelayString="${llm.evaluations.resume-delay-ms:5000}")public void resume(){while(workflow.resumeNext()){} }}
