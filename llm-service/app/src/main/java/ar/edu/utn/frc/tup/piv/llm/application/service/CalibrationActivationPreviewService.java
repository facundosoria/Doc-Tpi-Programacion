package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.CalibrationMigrationPreview;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ChallengeCalibrationAssignmentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CalibrationActivationPreviewService {
  private final ChallengeCalibrationAssignmentRepository r;

  public CalibrationActivationPreviewService(ChallengeCalibrationAssignmentRepository r) {
    this.r = r;
  }

  public CalibrationMigrationPreview preview(UUID course, UUID next) {
    return r.preview(course, next);
  }
}
