package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentQueryService;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationIncidentQueryControllerTest {

    private MockMvc mvc;
    private ModerationIncidentRepositoryPort incidentRepository;
    private ModerationAppealRepositoryPort appealRepository;
    private ModerationCourseAuthorization authorization;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(ModerationIncidentRepositoryPort.class);
        appealRepository = mock(ModerationAppealRepositoryPort.class);
        authorization = new ModerationCourseAuthorization();

        ModerationIncidentQueryService queryService = new ModerationIncidentQueryService(
                incidentRepository, appealRepository
        );

        ModerationIncidentQueryController controller = new ModerationIncidentQueryController(
                authorization, queryService
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
    void test200OkListIncidentsWithTruncatedPreviewToMax200Chars() throws Exception {
        UUID inc1Id = UUID.randomUUID();
        // Mensaje excesivamente largo (350 caracteres) para probar minimización GDPR
        String veryLongText = "Este es un texto excesivamente largo que sobrepasa con creces el límite de minimización de datos fijado en doscientos caracteres para garantizar que el docente solo vea un preview acotado y seguro del contenido del alumno sin comprometer la privacidad ni exponer el historial completo del chat bajo ningún concepto. ".repeat(2);
        assertThat(veryLongText.length()).isGreaterThan(200);

        ModerationIncident inc1 = new ModerationIncident(
                inc1Id, "msg-001", "user-55", "curso-42", "BLOCK", "CODE_OBFUSCATION", veryLongText, OffsetDateTime.now()
        );

        when(incidentRepository.findByCourseIdAndStatus(eq("curso-42"), eq("PENDING_REVIEW"), anyInt(), anyInt()))
                .thenReturn(List.of(inc1));
        when(incidentRepository.countByCourseIdAndStatus(eq("curso-42"), eq("PENDING_REVIEW")))
                .thenReturn(1L);
        when(appealRepository.findByIncidentId(inc1Id)).thenReturn(Optional.empty());

        mvc.perform(get("/moderation/v1/incidents")
                        .param("course_id", "curso-42")
                        .param("status", "PENDING_REVIEW")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].incident_id", is(inc1Id.toString())))
                .andExpect(jsonPath("$.content[0].reason_code", is("CODE_OBFUSCATION")))
                .andExpect(jsonPath("$.content[0].has_appeal", is(false)))
                .andExpect(jsonPath("$.content[0].appeal_reason", nullValue()))
                .andExpect(jsonPath("$.content[0].message_preview").value(org.hamcrest.Matchers.hasLength(200)));
    }

    @Test
    void test200OkListIncidentsIncludesAppealDetailsIfPresent() throws Exception {
        UUID inc1Id = UUID.randomUUID();
        String appealText = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";

        ModerationIncident inc1 = new ModerationIncident(
                inc1Id, "msg-001", "user-55", "curso-42", "PENDING_REVIEW", "CODE_OBFUSCATION", "preview...", OffsetDateTime.now()
        );
        ModerationAppeal appeal = new ModerationAppeal(
                UUID.randomUUID(), inc1Id, "user-55", appealText, ModerationAppealStatus.PENDING_REVIEW, OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(incidentRepository.findByCourseIdAndStatus(eq("curso-42"), eq("PENDING_REVIEW"), anyInt(), anyInt()))
                .thenReturn(List.of(inc1));
        when(incidentRepository.countByCourseIdAndStatus(eq("curso-42"), eq("PENDING_REVIEW")))
                .thenReturn(1L);
        when(appealRepository.findByIncidentId(inc1Id)).thenReturn(Optional.of(appeal));

        mvc.perform(get("/moderation/v1/incidents")
                        .param("course_id", "curso-42")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-10")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].has_appeal", is(true)))
                .andExpect(jsonPath("$.content[0].appeal_reason", is(appealText)));
    }

    @Test
    void test403ForbiddenWhenTeacherDoesNotBelongToCourse() throws Exception {
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
}
