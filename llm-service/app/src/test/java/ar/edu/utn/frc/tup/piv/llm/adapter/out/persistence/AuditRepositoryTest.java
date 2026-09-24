package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditRepositoryTest {
  @Test void recordsTheEventWithTheFullDelegatedIdentity() {
    var jdbc = mock(JdbcTemplate.class);
    UUID resourceId = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-9", "trace-9");

    new AuditRepository(jdbc).record("calibration.queued", "calibration-run", resourceId, actor, "{}");

    verify(jdbc).update(anyString(), eq("calibration.queued"), eq(actor.serviceId()),
        eq(actor.delegatedUserId()), eq("calibration-run"), eq(resourceId), eq("req-9"),
        eq("trace-9"), eq("{}"));
  }
}