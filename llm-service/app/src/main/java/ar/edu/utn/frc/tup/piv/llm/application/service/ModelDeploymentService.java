package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Servicio de aplicación para consultar despliegues de modelos habilitados. */
@Service
public class ModelDeploymentService {
  private final ModelDeploymentRepository repository;

  public ModelDeploymentService(ModelDeploymentRepository repository) {
    this.repository = repository;
  }

  public List<ModelDeploymentSummary> listEnabledDeployments() {
    return repository.listEnabledDeployments();
  }
}
