package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ActiveCalibration;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ChallengeAssignment;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.PendingEvaluation;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseEvaluationStatusRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseEvaluationStatusService {

  private final CourseEvaluationStatusRepository repository;

  public CourseEvaluationStatusService(CourseEvaluationStatusRepository repository) {
    this.repository = repository;
  }

  public Optional<ActiveCalibration> activeCalibration(UUID courseId) {
    return repository.activeCalibration(courseId);
  }

  public List<ChallengeAssignment> assignments(UUID courseId) {
    return repository.assignments(courseId);
  }

  public List<PendingEvaluation> pendingEvaluations(UUID courseId) {
    return repository.pendingEvaluations(courseId);
  }
}
