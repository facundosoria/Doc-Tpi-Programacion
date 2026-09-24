package ar.edu.utn.frc.tup.piv.llm.domain;

import java.util.EnumSet;

public final class CalibrationStateMachine {
  private CalibrationStateMachine() {}
  public static CalibrationState transition(CalibrationState current, CalibrationState target) {
    if (!allowed(current).contains(target)) throw new IllegalStateException("Invalid calibration transition: " + current + " -> " + target);
    return target;
  }
  public static boolean canActivate(CalibrationState state) { return state == CalibrationState.PASSED; }
  public static boolean mustQueue(EvaluationState state, boolean validCalibration) { return state == EvaluationState.QUEUED && !validCalibration; }
  public static EvaluationState resume(EvaluationState state, boolean validCalibration) {
    if (!validCalibration || state != EvaluationState.QUEUED) throw new IllegalStateException("A queued evaluation requires a valid calibration to resume");
    return EvaluationState.RUNNING;
  }
  private static EnumSet<CalibrationState> allowed(CalibrationState state) {
    return switch (state) {
      case QUEUED -> EnumSet.of(CalibrationState.RUNNING, CalibrationState.CANCELLED);
      case RUNNING -> EnumSet.of(CalibrationState.PASSED, CalibrationState.FAILED, CalibrationState.CANCELLED);
      case PASSED, FAILED, CANCELLED -> EnumSet.noneOf(CalibrationState.class);
    };
  }
  public enum CalibrationState { QUEUED, RUNNING, PASSED, FAILED, CANCELLED }
  public enum EvaluationState { QUEUED, RUNNING, COMPLETED, FAILED }
}
