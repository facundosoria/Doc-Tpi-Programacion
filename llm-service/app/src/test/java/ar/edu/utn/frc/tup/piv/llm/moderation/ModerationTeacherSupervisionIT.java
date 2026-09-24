package ar.edu.utn.frc.tup.piv.llm.moderation;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.ModerationCourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.ModerationIncidentQueryController;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.ModerationIncidentResolveController;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentQueryService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentResolveService;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas de integración E2E de supervisión y resolución docente (LLM-S12-H02 / T5).
 * Valida los 3 escenarios BDD de la ficha de historia de usuario oficial.
 */
class ModerationTeacherSupervisionIT {

    private MockMvc mvc;
    private InMemoryIncidentRepository incidentRepository;
    private InMemoryAppealRepository appealRepository;
    private InMemoryResolutionRepository resolutionRepository;
    private CapturingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        incidentRepository = new InMemoryIncidentRepository();
        appealRepository = new InMemoryAppealRepository();
        resolutionRepository = new InMemoryResolutionRepository();
        eventPublisher = new CapturingEventPublisher();

        ModerationCourseAuthorization authorization = new ModerationCourseAuthorization();
        ModerationIncidentQueryService queryService = new ModerationIncidentQueryService(incidentRepository, appealRepository);
        ModerationIncidentResolveService resolveService = new ModerationIncidentResolveService(
                incidentRepository, resolutionRepository, appealRepository, eventPublisher
        );

        ModerationIncidentQueryController queryController = new ModerationIncidentQueryController(authorization, queryService);
        ModerationIncidentResolveController resolveController = new ModerationIncidentResolveController(
                authorization, incidentRepository, resolveService
        );

        mvc = MockMvcBuilders.standaloneSetup(queryController, resolveController)
                .addFilters(new GatewayIdentityFilter())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * BDD Escenario 1 — Docente revisa y confirma un incidente de su curso (camino feliz)
     * Dado que el docente prof-10 tiene un JWT/headers con rol teacher en el curso curso-42,
     * y existe el incidente inc-001 en estado PENDING_REVIEW para ese curso con una apelación del alumno user-55
     * Cuando el docente llama a POST /moderation/v1/incidents/inc-001/resolve con resolution=CONFIRMED
     * Entonces responde 200 OK con el incidente actualizado y auditoría persistida.
     */
    @Test
    void bddScenario1TeacherReviewsAndConfirmsIncidentOfOwnCourse() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String reason = "El texto incluía lenguaje ofensivo explícito hacia otro compañero, no era contenido educativo.";

        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "OFFENSIVE", "preview del mensaje", OffsetDateTime.now()
        );
        incidentRepository.save(incident);

        ModerationAppeal appeal = ModerationAppeal.create(incidentId, "user-55", "Apelación del alumno que justifica su mensaje...");
        appealRepository.save(appeal);

        String requestJson = String.format("""
            {
              "resolution": "CONFIRMED",
              "resolution_reason": "%s"
            }
        """, reason);

        mvc.perform(post("/moderation/v1/incidents/" + incidentId + "/resolve")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id", is(incidentId.toString())))
                .andExpect(jsonPath("$.resolution", is("CONFIRMED")))
                .andExpect(jsonPath("$.resolved_by", is("prof-10")));

        // Verificar persistencia de auditoría inmutable
        assertThat(resolutionRepository.existsByIncidentId(incidentId)).isTrue();
        ModerationResolution savedResolution = resolutionRepository.findByIncidentId(incidentId).orElseThrow();
        assertThat(savedResolution.getResolvedBy()).isEqualTo("prof-10");
        assertThat(savedResolution.getResolution().name()).isEqualTo("CONFIRMED");
        assertThat(savedResolution.getResolutionReason()).isEqualTo(reason);

        // Verificar que el estado del incidente fue actualizado
        ModerationIncident updated = incidentRepository.findById(incidentId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CONFIRMED");
    }

    /**
     * BDD Escenario 2 — Docente de otro curso intenta acceder a incidentes (caso que debe fallar)
     * Dado que el docente prof-20 tiene rol teacher en curso-99, y el incidente inc-001 pertenece a curso-42
     * Cuando el docente prof-20 llama a GET /moderation/v1/incidents?course_id=curso-42
     * Entonces el sistema responde 403 Forbidden con error=forbidden y detail="No sos docente del curso curso-42"
     */
    @Test
    void bddScenario2TeacherOfAnotherCourseAttemptsToAccessIncidents() throws Exception {
        mvc.perform(get("/moderation/v1/incidents")
                        .param("course_id", "curso-42")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-20")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.detail", is("No sos docente del curso curso-42")));
    }

    /**
     * BDD Escenario 3 — Resolución sin motivo (caso que debe fallar)
     * Dado que el docente prof-10 tiene acceso al incidente inc-002 en estado PENDING_REVIEW de su curso
     * Cuando envía POST /moderation/v1/incidents/inc-002/resolve con resolution_reason vacía
     * Entonces el sistema responde 400 Bad Request con error="validation_error" y el incidente mantiene su estado
     */
    @Test
    void bddScenario3ResolutionWithoutReasonFails() throws Exception {
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-002", "user-55", "curso-42", "PENDING_REVIEW", "CODE_OBFUSCATION", "preview", OffsetDateTime.now()
        );
        incidentRepository.save(incident);

        String requestJson = """
            {
              "resolution": "REVERSED",
              "resolution_reason": ""
            }
        """;

        mvc.perform(post("/moderation/v1/incidents/" + incidentId + "/resolve")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_error")));

        // El incidente debe mantener su estado PENDING_REVIEW sin cambios
        ModerationIncident notUpdated = incidentRepository.findById(incidentId).orElseThrow();
        assertThat(notUpdated.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(resolutionRepository.existsByIncidentId(incidentId)).isFalse();
    }

    /**
     * Ciclo completo de reversión (Falso Positivo) y emisión de evento de dominio
     */
    @Test
    void testCompleteCycleReversedResolutionEmitsUnblockEvent() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase de algoritmos.";

        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-003", "user-55", "curso-42", "PENDING_REVIEW", "CODE_OBFUSCATION", "preview", OffsetDateTime.now()
        );
        incidentRepository.save(incident);

        String requestJson = String.format("""
            {
              "resolution": "REVERSED",
              "resolution_reason": "%s"
            }
        """, reason);

        mvc.perform(post("/moderation/v1/incidents/" + incidentId + "/resolve")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution", is("REVERSED")));

        assertThat(eventPublisher.events).hasSize(1);
        ModerationResolutionDomainEvent event = eventPublisher.events.get(0);
        assertThat(event.getResolution()).isEqualTo("REVERSED");
        assertThat(event.getMessageId()).isEqualTo("msg-003");
        assertThat(event.getResolvedBy()).isEqualTo("prof-10");
        assertThat(event.getCourseId()).isEqualTo("curso-42");
    }

    // Adaptadores en memoria para test de integración
    private static class InMemoryIncidentRepository implements ModerationIncidentRepositoryPort {
        private final ConcurrentHashMap<UUID, ModerationIncident> map = new ConcurrentHashMap<>();

        @Override
        public Optional<ModerationIncident> findById(UUID id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public ModerationIncident save(ModerationIncident incident) {
            map.put(incident.getId(), incident);
            return incident;
        }

        @Override
        public List<ModerationIncident> findByCourseIdAndStatus(String courseId, String status, int offset, int limit) {
            return map.values().stream()
                    .filter(i -> i.getCourseId().equals(courseId))
                    .filter(i -> "PENDING_REVIEW".equalsIgnoreCase(status) ? (i.isBlock() || "PENDING_REVIEW".equalsIgnoreCase(i.getStatus())) : i.getStatus().equalsIgnoreCase(status))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByCourseIdAndStatus(String courseId, String status) {
            return map.values().stream()
                    .filter(i -> i.getCourseId().equals(courseId))
                    .filter(i -> "PENDING_REVIEW".equalsIgnoreCase(status) ? (i.isBlock() || "PENDING_REVIEW".equalsIgnoreCase(i.getStatus())) : i.getStatus().equalsIgnoreCase(status))
                    .count();
        }
    }

    private static class InMemoryAppealRepository implements ModerationAppealRepositoryPort {
        private final ConcurrentHashMap<UUID, ModerationAppeal> map = new ConcurrentHashMap<>();

        @Override
        public ModerationAppeal save(ModerationAppeal appeal) {
            map.put(appeal.getId(), appeal);
            return appeal;
        }

        @Override
        public Optional<ModerationAppeal> findById(UUID id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public Optional<ModerationAppeal> findByIncidentId(UUID incidentId) {
            return map.values().stream().filter(a -> a.getIncidentId().equals(incidentId)).findFirst();
        }

        @Override
        public boolean existsByIncidentId(UUID incidentId) {
            return map.values().stream().anyMatch(a -> a.getIncidentId().equals(incidentId));
        }
    }

    private static class InMemoryResolutionRepository implements ModerationResolutionRepositoryPort {
        private final ConcurrentHashMap<UUID, ModerationResolution> map = new ConcurrentHashMap<>();

        @Override
        public ModerationResolution save(ModerationResolution resolution) {
            map.put(resolution.getIncidentId(), resolution);
            return resolution;
        }

        @Override
        public Optional<ModerationResolution> findByIncidentId(UUID incidentId) {
            return Optional.ofNullable(map.get(incidentId));
        }

        @Override
        public boolean existsByIncidentId(UUID incidentId) {
            return map.containsKey(incidentId);
        }
    }

    private static class CapturingEventPublisher implements ModerationEventPublisherPort {
        private final List<ModerationResolutionDomainEvent> events = new ArrayList<>();

        @Override
        public void publishMessageUnblocked(ModerationResolutionDomainEvent event) {
            events.add(event);
        }
    }
}
