package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.exception.AppealAlreadyExistsException;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationAppealUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationAppealControllerTest {

    private MockMvc mvc;
    private ModerationAppealUseCase useCase;
    private ModerationAppealController controller;
    private GatewayIdentityFilter identityFilter;

    @BeforeEach
    void setUp() {
        useCase = mock(ModerationAppealUseCase.class);
        controller = new ModerationAppealController(useCase);
        identityFilter = new GatewayIdentityFilter();
        identityFilter.setTrustUnsignedBearer(true);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(identityFilter)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String createJwt(String sub) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + sub + "\",\"roles\":[\"STUDENT\"]}").getBytes(StandardCharsets.UTF_8)
        );
        return header + "." + payload + ".mock-sig";
    }

    @Test
    void test201CreatedValidAppealOfOwnIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase, no intentaba ocultar código.";
        OffsetDateTime now = OffsetDateTime.now();

        ModerationAppeal appeal = new ModerationAppeal(
                appealId, incidentId, userId, reason, ModerationAppealStatus.PENDING_REVIEW, now, now
        );

        when(useCase.createAppeal(any(CreateModerationAppealCommand.class))).thenReturn(appeal);

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "%s"
            }
        """, incidentId, reason);

        mvc.perform(post("/moderation/v1/appeals")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appeal_id", is(appealId.toString())))
                .andExpect(jsonPath("$.incident_id", is(incidentId.toString())))
                .andExpect(jsonPath("$.status", is("PENDING_REVIEW")))
                .andExpect(jsonPath("$.user_id", is(userId)))
                .andExpect(jsonPath("$.appeal_reason", is(reason)));

        verify(useCase).createAppeal(new CreateModerationAppealCommand(incidentId, userId, reason));
    }

    @Test
    void test201CreatedValidAppealWithJwtToken() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        String userId = "user-55";
        String jwt = createJwt(userId);
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase, no intentaba ocultar código.";
        OffsetDateTime now = OffsetDateTime.now();

        ModerationAppeal appeal = new ModerationAppeal(
                appealId, incidentId, userId, reason, ModerationAppealStatus.PENDING_REVIEW, now, now
        );

        when(useCase.createAppeal(any(CreateModerationAppealCommand.class))).thenReturn(appeal);

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "%s"
            }
        """, incidentId, reason);

        mvc.perform(post("/moderation/v1/appeals")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appeal_id", is(appealId.toString())))
                .andExpect(jsonPath("$.status", is("PENDING_REVIEW")));
    }

    @Test
    void test403ForbiddenWhenAuthenticatedUserAttemptsToAppealIncidentOfAnotherUser() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String authenticatedUserId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase, no intentaba ocultar código.";

        when(useCase.createAppeal(any(CreateModerationAppealCommand.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "El incidente no pertenece al usuario autenticado"));

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "%s"
            }
        """, incidentId, reason);

        mvc.perform(post("/moderation/v1/appeals")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, authenticatedUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void test409ConflictWhenDuplicatingAppealOnSameIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID existingAppealId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase, no intentaba ocultar código.";

        when(useCase.createAppeal(any(CreateModerationAppealCommand.class)))
                .thenThrow(new AppealAlreadyExistsException(existingAppealId));

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "%s"
            }
        """, incidentId, reason);

        mvc.perform(post("/moderation/v1/appeals")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("appeal_already_exists")))
                .andExpect(jsonPath("$.appeal_id", is(existingAppealId.toString())));
    }

    @Test
    void test400BadRequestWhenReasonIsTooShortLessThan20Characters() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String shortReason = "Demasiado corto"; // < 20 caracteres

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "%s"
            }
        """, incidentId, shortReason);

        mvc.perform(post("/moderation/v1/appeals")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test400BadRequestWhenReasonIsBlank() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";

        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": ""
            }
        """, incidentId);

        mvc.perform(post("/moderation/v1/appeals")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test401UnauthorizedWithoutAuthenticationHeadersOrToken() throws Exception {
        UUID incidentId = UUID.randomUUID();
        String requestJson = String.format("""
            {
              "incident_id": "%s",
              "appeal_reason": "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase."
            }
        """, incidentId);

        mvc.perform(post("/moderation/v1/appeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test200OkGetAppealStatus() throws Exception {
        UUID appealId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";
        OffsetDateTime now = OffsetDateTime.now();

        ModerationAppeal appeal = new ModerationAppeal(
                appealId, incidentId, userId, reason, ModerationAppealStatus.PENDING_REVIEW, now, now
        );

        when(useCase.getAppeal(appealId, userId)).thenReturn(appeal);

        mvc.perform(get("/moderation/v1/appeals/" + appealId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeal_id", is(appealId.toString())))
                .andExpect(jsonPath("$.incident_id", is(incidentId.toString())))
                .andExpect(jsonPath("$.status", is("PENDING_REVIEW")))
                .andExpect(jsonPath("$.user_id", is(userId)));
    }

    @Test
    void test403ForbiddenGetAppealOfAnotherUser() throws Exception {
        UUID appealId = UUID.randomUUID();
        String userId = "user-55";

        when(useCase.getAppeal(eq(appealId), eq(userId)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "La apelación no pertenece al usuario autenticado"));

        mvc.perform(get("/moderation/v1/appeals/" + appealId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void test404NotFoundGetNonExistentAppeal() throws Exception {
        UUID appealId = UUID.randomUUID();
        String userId = "user-55";

        when(useCase.getAppeal(eq(appealId), eq(userId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Apelación no encontrada"));

        mvc.perform(get("/moderation/v1/appeals/" + appealId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, userId))
                .andExpect(status().isNotFound());
    }
}
