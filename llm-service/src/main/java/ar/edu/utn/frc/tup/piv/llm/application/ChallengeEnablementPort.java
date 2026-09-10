package ar.edu.utn.frc.tup.piv.llm.application;
import java.util.UUID;
/** Temporary adapter boundary for the future challenges-service integration. */
public interface ChallengeEnablementPort {void enable(UUID challengeId,UUID courseId);}
