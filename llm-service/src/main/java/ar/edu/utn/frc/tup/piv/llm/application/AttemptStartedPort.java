package ar.edu.utn.frc.tup.piv.llm.application;
import java.util.UUID;
/** Temporary adapter boundary for the future attempts-service event. */
public interface AttemptStartedPort {void firstAttemptStarted(UUID challengeId, UUID attemptId);}
