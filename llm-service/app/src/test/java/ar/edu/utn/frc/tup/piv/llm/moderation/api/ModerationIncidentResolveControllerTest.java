package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentResolveService;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationIncidentResolveControllerTest {

    private MockMvc mvc;
    private ModerationIncidentRepositoryPort incidentRepository;
    private ModerationResolutionRepositoryPort resolutionRepository;
    private ModerationAppealRepositoryPort appealRepository;
    private ModerationEventPublisherPort eventPublisher;
    private ModerationCourseAuthorization authorization;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(ModerationIncidentRepositoryPort.class);
        resolutionRepository = mock(ModerationResolutionRepositoryPort.class);
        appealRepository = mock(ModerationAppealRepositoryPort.class);
        eventPublisher = mock(ModerationEventPublisherPort.class);
        authorization = new ModerationCourseAuthorization();

        ModerationIncidentResolveService resolveService = new ModerationIncidentResolveService(
                incidentRepository, resolutionRepository, appealRepository, eventPublisher
        );

        ModerationIncidentResolveController controller = new ModerationIncidentResolveController(
                authorization, incidentRepository, resolveService
        );

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new GatewayIdentityFilter())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void test200OkResolveConfirmedWithCompleteAudit() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String reason = "El texto incluía lenguaje ofensivo explícito hacia otro compañero, no era contenido educativo.";
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "OFFENSIVE", "preview", OffsetDateTime.now()
        );

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(resolutionRepository.existsByIncidentId(incidentId)).thenReturn(false);
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resolutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
                .andExpect(jsonPath("$.resolved_by", is("prof-10")))
                .andExpect(jsonPath("$.resolution_reason", is(reason)))
                .andExpect(jsonPath("$.resolved_at", notNullValue()));

        verify(resolutionRepository).save(any(ModerationResolution.class));
    }

    @Test
    void test200OkResolveReversedWithReasonAlias() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase de algoritmos.";
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "CODE_OBFUSCATION", "preview", OffsetDateTime.now()
        );

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(resolutionRepository.existsByIncidentId(incidentId)).thenReturn(false);
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resolutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Prueba con el campo 'reason' como alias de 'resolution_reason'
        String requestJson = String.format("""
            {
              "resolution": "REVERSED",
              "reason": "%s"
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
                .andExpect(jsonPath("$.resolution", is("REVERSED")))
                .andExpect(jsonPath("$.resolved_by", is("prof-10")));

        verify(eventPublisher).publishMessageUnblocked(any());
    }

    @Test
    void test400BadRequestWhenReasonIsTooShortLessThan20Characters() throws Exception {
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "OFFENSIVE", "preview", OffsetDateTime.now()
        );
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        String requestJson = """
            {
              "resolution": "CONFIRMED",
              "resolution_reason": "Muy corto"
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
    }

    @Test
    void test400BadRequestWhenReasonIsBlank() throws Exception {
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "OFFENSIVE", "preview", OffsetDateTime.now()
        );
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

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
    }

    @Test
    void test403ForbiddenWhenTeacherDoesNotBelongToCourse() throws Exception {
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "OFFENSIVE", "preview", OffsetDateTime.now()
        );
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        String requestJson = """
            {
              "resolution": "CONFIRMED",
              "resolution_reason": "Motivo con más de veinte caracteres válidos para la prueba."
            }
        """;

        mvc.perform(post("/moderation/v1/incidents/" + incidentId + "/resolve")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-20")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.detail", is("No sos docente del curso curso-42")));
    }

    @Test
    void test409ConflictWhenAttemptingToResolveAlreadyResolvedIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        // Incidente ya confirmado
        ModerationIncident incident = new ModerationIncident(
                incidentId, "msg-001", "user-55", "curso-42", "CONFIRMED", "OFFENSIVE", "preview", OffsetDateTime.now()
        );
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(resolutionRepository.existsByIncidentId(incidentId)).thenReturn(true);

        String requestJson = """
            {
              "resolution": "REVERSED",
              "resolution_reason": "Intento de revertir un incidente que ya fue resuelto previamente."
            }
        """;

        mvc.perform(post("/moderation/v1/incidents/" + incidentId + "/resolve")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
    }
}
