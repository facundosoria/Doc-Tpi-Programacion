package ar.edu.utn.frc.tup.piv.llm.application.port.out;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.UUID;

/**
 * Boundary owned by courses-service. Tema 07 never derives course membership from browser
 * headers or local demo data.
 */
public interface CourseMembershipPort {
  Membership membership(UUID courseCohortId, UUID userId, CallerIdentity caller);

  record Membership(String role, String status) {
    public boolean isActiveTeacher() {
      return status != null && ("ACTIVE".equalsIgnoreCase(status) || "ACTIVO".equalsIgnoreCase(status))
          && role != null && ("TEACHER".equalsIgnoreCase(role) || "DOCENTE".equalsIgnoreCase(role));
    }
  }
}
