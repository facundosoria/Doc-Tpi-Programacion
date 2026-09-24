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
    "llm.evaluations.resume-delay-ms=3600000",
    // El shadow (E-31) también lo maneja a mano su IT.
    "llm.shadow.dispatch-delay-ms=3600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationIT {

  protected static final java.util.UUID TEACHER = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");

  @org.springframework.beans.factory.annotation.Autowired protected org.springframework.test.web.servlet.MockMvc mvc;
  @org.springframework.beans.factory.annotation.Autowired protected com.fasterxml.jackson.databind.ObjectMapper json;
  @org.springframework.beans.factory.annotation.Autowired protected org.springframework.jdbc.core.JdbcTemplate jdbc;

  private static final java.util.UUID FAKE_CREDENTIAL =
      java.util.UUID.nameUUIDFromBytes("it-fake-credential".getBytes(java.nio.charset.StandardCharsets.UTF_8));

  /**
   * Desde la V26 `function_model_config` referencia un despliegue real y las semillas 'fake' de
   * tutor/evaluator/embedding se retiraron. Los ITs que invocan modelos (tutor, evaluación de
   * intentos, RAG) necesitan esa asignación: acá se crea una credencial y un despliegue por función
   * con proveedor `fake` — que mapea al {@code FakeModelAdapter} del classpath de test — y se
   * asignan por función. El upsert es idempotente para todos los métodos.
   */
  @org.junit.jupiter.api.BeforeEach
  void seedFakeFunctionAssignments() {
    jdbc.update("insert into llm.provider_credentials (id, provider_key, display_name, public_configuration, encrypted_secrets, secret_nonce, secret_mask, created_by_user_id, state) values (?, 'fake', 'fake', '{}'::jsonb, ?, ?, 'fake', ?, 'ACTIVE') on conflict (id) do nothing",
        FAKE_CREDENTIAL, new byte[] {0}, new byte[] {0}, TEACHER);
    for (String function : java.util.List.of("tutor", "evaluator", "embedding")) {
      java.util.UUID deployment = fakeDeployment(function);
      jdbc.update("insert into llm.model_deployments (id, credential_id, provider_key, adapter_version, model_id, model_version, capabilities, evaluator_state) values (?, ?, 'fake', '1', ?, 'v1', '{}'::jsonb, 'CANDIDATE') on conflict (id) do nothing",
          deployment, FAKE_CREDENTIAL, "fake-" + function);
      jdbc.update("insert into llm.function_model_config (function, model_deployment_id, enabled, updated_at) values (?, ?, true, now()) on conflict (function) do update set model_deployment_id=excluded.model_deployment_id, enabled=true, updated_at=now()",
          function, deployment);
    }
  }

  /** Despliegue fake estable por función, para reasignarlo desde un IT sin inventar un FK. */
  protected static java.util.UUID fakeDeployment(String function) {
    return java.util.UUID.nameUUIDFromBytes(("it-fake-deployment-" + function).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Identidad de docente de un curso, tal como la propaga el API Gateway (admin-service delegando). */
  protected static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asTeacher(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, java.util.UUID courseId) {
    // Reemplaza, no acumula: `asTeacher(request, X)` declara la matrícula de ESE request, igual que
    // hacía el header `X-Teacher-Course-Ids`. Así los casos de "otro curso" siguen dando 403.
    CURSOS_DEL_DOCENTE.clear();
    CURSOS_DEL_DOCENTE.add(courseId);
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

  /** Identidad de administrador institucional. */
  protected static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asAdmin(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
    return request
        .header("X-Principal-Type", "service")
        .header("X-Service-Id", "admin-service")
        .header("X-Service-Scopes", "llm.institutional-calibration.manage")
        .header("X-Delegated-User", TEACHER.toString())
        .header("X-Actor-Id", TEACHER.toString())
        .header("X-User-Roles", "ADMIN")
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

  /**
   * Cohortes en las que el docente de prueba está matriculado. `asTeacher(request, courseId)` la
   * completa: el propio test declara a qué curso pertenece, igual que antes lo hacía con el header
   * `X-Teacher-Course-Ids`.
   */
  static final java.util.Set<java.util.UUID> CURSOS_DEL_DOCENTE =
      java.util.concurrent.ConcurrentHashMap.newKeySet();

  /**
   * Stub de courses-service. Desde la integración main↔dev la autorización de curso ya no se
   * resuelve con headers: `CourseAuthorization` consulta a Courses por el Gateway
   * ({@code GatewayCoursesMembershipClient}). Sin este stub todos los ITs de curso responden 503.
   * Devuelve matrícula solo en las cohortes que el test declaró,
   * para que los casos de "otro curso" sigan dando 403.
   */
  static final com.sun.net.httpserver.HttpServer COURSES_STUB;

  static {
    try {
      COURSES_STUB = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
      COURSES_STUB.createContext("/api/courses", exchange -> {
        String[] partes = exchange.getRequestURI().getPath().split("/");
        // /api/courses/{courseCohortId}/members/{userId}
        boolean matriculado = false;
        if (partes.length >= 6 && "members".equals(partes[4])) {
          try {
            java.util.UUID.fromString(partes[5]); // valida que sea un userId
            matriculado = CURSOS_DEL_DOCENTE.contains(java.util.UUID.fromString(partes[3]));
          } catch (IllegalArgumentException noEsUuid) {
            matriculado = false;
          }
        }
        byte[] cuerpo = matriculado
            ? "{\"role\":\"TEACHER\",\"status\":\"ACTIVE\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)
            : "{\"error\":\"not a member\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(matriculado ? 200 : 404, cuerpo.length);
        exchange.getResponseBody().write(cuerpo);
        exchange.close();
      });
      COURSES_STUB.start();
    } catch (java.io.IOException error) {
      throw new IllegalStateException("No se pudo levantar el stub de courses-service", error);
    }
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("llm.courses.base-url", () -> "http://localhost:" + COURSES_STUB.getAddress().getPort());
  }
}
