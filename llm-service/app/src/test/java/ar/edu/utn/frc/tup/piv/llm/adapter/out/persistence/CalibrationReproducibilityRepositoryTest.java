package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalibrationReproducibilityRepositoryTest {
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final CalibrationReproducibilityRepository repository = new CalibrationReproducibilityRepository(jdbc);
  private final ObjectMapper json = new ObjectMapper();

  @Test void snapshotsTheExecutionParametersAndANonConflictiveArtifact() {
    UUID run = UUID.randomUUID();
    JsonNode parameters = json.createObjectNode().put("seed", 7);
    repository.snapshot(run, parameters, "prompt");

    verify(jdbc).update(anyString(), eq(parameters.toString()), eq("prompt"), eq(run));
    verify(jdbc).update(anyString(), anyString(), eq(run));
  }

  @Test void storesAFreeFormArtifactForARun() {
    UUID run = UUID.randomUUID();
    JsonNode content = json.createObjectNode().put("kind", "diagnosis");
    repository.artifact(run, "DIAGNOSIS", content);
    verify(jdbc).update(anyString(), eq(run), eq("DIAGNOSIS"), eq(content.toString()), anyString());
  }
}