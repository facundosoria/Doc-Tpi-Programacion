package ar.edu.utn.frc.tup.piv.llm.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piv.llm.application.port.out.CourseMembershipPort;
import ar.edu.utn.frc.tup.piv.llm.application.worker.CalibrationRunWorker;
import ar.edu.utn.frc.tup.piv.llm.application.worker.PendingEvaluationWorker;
import ar.edu.utn.frc.tup.piv.llm.application.worker.RecalibrationScheduler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * E2E del flujo de golden sets sobre infraestructura real (Postgres + Kafka vía Testcontainers).
 * Se salta automáticamente cuando no hay Docker disponible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CalibrationFlowE2ETest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

  @Container
  @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CourseMembershipPort courseMembershipPort;

  @MockitoBean
  private CalibrationRunWorker calibrationRunWorker;

  @MockitoBean
  private PendingEvaluationWorker pendingEvaluationWorker;

  @MockitoBean
  private RecalibrationScheduler recalibrationScheduler;

  @Test
  void completeGoldenSetDraftFlow_shouldSucceed() throws Exception {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseMembershipPort.membership(any(), any(), any()))
        .thenReturn(new CourseMembershipPort.Membership("TEACHER", "ACTIVE"));

    MvcResult result = mockMvc.perform(post("/api/llm/courses/" + courseId + "/golden-sets")
            .header("X-Principal-Type", "service")
            .header("X-Service-Id", "admin-service")
            .header("X-Service-Scopes", "llm.golden-set.manage")
            .header("X-Delegated-User", teacherId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "name": "E2E Golden Set Test" }
                """))
        .andExpect(status().isCreated())
        .andReturn();

    String location = result.getResponse().getHeader("Location");
    assertThat(location).isNotNull().contains("/api/llm/courses/" + courseId + "/golden-sets/");
  }
}