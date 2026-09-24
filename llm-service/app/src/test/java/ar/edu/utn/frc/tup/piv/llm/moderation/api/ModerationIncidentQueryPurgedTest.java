package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentDetail;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentSummary;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.PagedResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationIncidentQueryUseCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de API para la consulta de incidentes purgados (LLM-S13-H02 / CA_negativo_2 / Escenario 3 BDD).
 */
class ModerationIncidentQueryPurgedTest {

    private MockMvc mvc;
    private ModerationIncidentQueryUseCase queryUseCase;
    private ModerationCourseAuthorization authorization;

    @BeforeEach
    void setUp() {
        queryUseCase = mock(ModerationIncidentQueryUseCase.class);
        authorization = mock(ModerationCourseAuthorization.class);

        ModerationIncidentQueryController controller = new ModerationIncidentQueryController(authorization, queryUseCase);

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
    @DisplayName("CA_negativo_2 & Escenario 3 BDD: Consulta de incidente purgado devuelve explícitamente content='PURGED' y purged_at")
    void queryPurgedIncidentReturnsExplicitPurgedIndication() throws Exception {
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ModerationIncidentDetail detail = new ModerationIncidentDetail(
                incidentId,
                "msg-123",
                "user-99",
                "curso-42",
                "CONFIRMED",
                "SPAM",
                "PURGED",
                now.minusDays(40),
                now.minusDays(10),
                "CONFIRMED",
                false,
                null
        );

        when(queryUseCase.getIncidentById(incidentId)).thenReturn(Optional.of(detail));

        mvc.perform(get("/moderation/v1/incidents/" + incidentId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-1")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id", is(incidentId.toString())))
                .andExpect(jsonPath("$.content", is("PURGED")))
                .andExpect(jsonPath("$.purged_at", notNullValue()))
                .andExpect(jsonPath("$.reason_code", is("SPAM")))
                .andExpect(jsonPath("$.resolution", is("CONFIRMED")));
    }

    @Test
    @DisplayName("Consulta de incidente vigente devuelve contenido intacto y purged_at null")
    void queryUnpurgedIncidentReturnsOriginalContent() throws Exception {
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ModerationIncidentDetail detail = new ModerationIncidentDetail(
                incidentId,
                "msg-456",
                "user-99",
                "curso-42",
                "PENDING_REVIEW",
                "OFFENSIVE",
                "Este es el texto original vigente",
                now.minusDays(5),
                null,
                "PENDING_REVIEW",
                false,
                null
        );

        when(queryUseCase.getIncidentById(incidentId)).thenReturn(Optional.of(detail));

        mvc.perform(get("/moderation/v1/incidents/" + incidentId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-1")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id", is(incidentId.toString())))
                .andExpect(jsonPath("$.content", is("Este es el texto original vigente")))
                .andExpect(jsonPath("$.purged_at", nullValue()));
    }

    @Test
    @DisplayName("Consulta de incidente inexistente devuelve 404 Not Found")
    void queryNonExistentIncidentReturns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(queryUseCase.getIncidentById(nonExistentId)).thenReturn(Optional.empty());

        mvc.perform(get("/moderation/v1/incidents/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Lista de incidentes muestra message_preview='PURGED' para registros purgados")
    void listIncidentsDisplaysPurgedPreview() throws Exception {
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ModerationIncidentSummary summary = new ModerationIncidentSummary(
                incidentId,
                "PURGED",
                "SPAM",
                "CONFIRMED",
                now.minusDays(35),
                false,
                null,
                "curso-42",
                "CONFIRMED"
        );

        PagedResult<ModerationIncidentSummary> paged = PagedResult.of(List.of(summary), 0, 20, 1);
        when(queryUseCase.getIncidents(eq("curso-42"), any(), anyInt(), anyInt())).thenReturn(paged);

        mvc.perform(get("/moderation/v1/incidents")
                        .param("course_id", "curso-42")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-1")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message_preview", is("PURGED")));
    }
}
