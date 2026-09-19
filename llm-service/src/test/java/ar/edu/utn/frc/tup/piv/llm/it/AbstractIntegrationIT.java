package ar.edu.utn.frc.tup.piv.llm.it;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de los tests de integración: app completa contra un PostgreSQL (pgvector) real con todas las
 * migraciones Flyway. El contenedor es único por JVM y se comparte entre clases (arranca una vez).
 * Requiere Docker; el gate de cobertura de `mvn verify` cuenta con estos tests.
 */
@SpringBootTest(properties = {
    "llm.credentials.master-key=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "app.jwks-refresh-ms=3600000",
    "spring.task.scheduling.enabled=false",
    // spring.task.scheduling.enabled no apaga @Scheduled: espaciamos los workers para que no le
    // roben corridas/evaluaciones en cola a los tests que las manejan a mano.
    "llm.calibrations.dispatch-delay-ms=3600000",
    "llm.evaluations.resume-delay-ms=3600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationIT {

  protected static final java.util.UUID TEACHER = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");

  @org.springframework.beans.factory.annotation.Autowired protected org.springframework.test.web.servlet.MockMvc mvc;
  @org.springframework.beans.factory.annotation.Autowired protected com.fasterxml.jackson.databind.ObjectMapper json;

  /** Identidad de docente de un curso, tal como la propaga el API Gateway (admin-service delegando). */
  protected static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asTeacher(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, java.util.UUID courseId) {
    return request
        .header("X-Principal-Type", "service")
        .header("X-Service-Id", "admin-service")
        .header("X-Service-Scopes", "llm.golden-set.manage llm.rubric-template.manage llm.tutor.interact llm.rag.query")
        .header("X-Delegated-User", TEACHER.toString())
        .header("X-Actor-Id", TEACHER.toString())
        .header("X-User-Roles", "TEACHER")
        .header("X-Teacher-Course-Ids", courseId.toString())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
  }

  protected com.fasterxml.jackson.databind.JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
      DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
