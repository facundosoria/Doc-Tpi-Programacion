package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationRetentionPolicyService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del endpoint de configuración de políticas de retención (LLM-S13-H02 / CA4).
 */
class ModerationRetentionPolicyControllerTest {

    private MockMvc mvc;
    private ModerationRetentionPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = mock(ModerationRetentionPolicyService.class);
        ModerationRetentionPolicyController controller = new ModerationRetentionPolicyController(policyService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new GatewayIdentityFilter())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CA4: Admin actualiza las políticas de retención exitosamente (200 OK)")
    void adminCanUpdateRetentionPolicies() throws Exception {
        Map<String, Integer> expected = Map.of(
                "BLOCK_CONFIRMED", 30,
                "BLOCK_REVERSED", 180,
                "PENDING_UNRESOLVED_TIMEOUT_DAYS", 90
        );

        when(policyService.updatePolicies(anyMap(), eq("admin-user"))).thenReturn(expected);

        String jsonPayload = """
            {
              "BLOCK_CONFIRMED": { "retentionDays": 30 },
              "BLOCK_REVERSED":  { "retentionDays": 180 },
              "PENDING_UNRESOLVED_TIMEOUT_DAYS": 90
            }
        """;

        mvc.perform(put("/api/v1/operations/retention-policy/moderation")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "admin-user")
                        .header(IdentityHeaders.USER_ROLES, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BLOCK_CONFIRMED", is(30)))
                .andExpect(jsonPath("$.BLOCK_REVERSED", is(180)))
                .andExpect(jsonPath("$.PENDING_UNRESOLVED_TIMEOUT_DAYS", is(90)));
    }

    @Test
    @DisplayName("CA4: Usuario sin rol ADMIN es rechazado con 403 Forbidden")
    void nonAdminIsForbiddenToUpdatePolicies() throws Exception {
        String jsonPayload = "{\"BLOCK_CONFIRMED\": 15}";

        mvc.perform(put("/api/v1/operations/retention-policy/moderation")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "teacher-user")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lectura de políticas de retención actuales (200 OK)")
    void canGetRetentionPolicies() throws Exception {
        when(policyService.getPolicies()).thenReturn(Map.of("BLOCK_CONFIRMED", 30, "BLOCK_REVERSED", 90));

        mvc.perform(get("/api/v1/operations/retention-policy/moderation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BLOCK_CONFIRMED", is(30)))
                .andExpect(jsonPath("$.BLOCK_REVERSED", is(90)));
    }
}
