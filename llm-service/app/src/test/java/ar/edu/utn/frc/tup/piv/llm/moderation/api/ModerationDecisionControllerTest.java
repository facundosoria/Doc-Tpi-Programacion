package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationDecisionService;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.MessageContent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDecisionRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationDecisionControllerTest {

    private MockMvc mvc;
    private ModerationDecisionRepositoryPort repository;
    private ModerationDecisionService service;
    private ModerationGatewayAuthorization authorization;
    private ModerationDecisionController controller;
    private GatewayIdentityFilter identityFilter;

    @BeforeEach
    void setUp() {
        repository = mock(ModerationDecisionRepositoryPort.class);
        service = new ModerationDecisionService(repository, 800L);
        authorization = new ModerationGatewayAuthorization("chat-service", "moderation:decide", false);
        controller = new ModerationDecisionController(authorization, service);
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

    private String createJwt(String sub, String scope) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + sub + "\",\"scope\":\"" + scope + "\"}").getBytes(StandardCharsets.UTF_8)
        );
        return header + "." + payload + ".mock-sig";
    }

    @Test
    void test200OkValidPayloadWithJwtAndScopeReturnsAllow() throws Exception {
        when(repository.findByMessageId("msg-001")).thenReturn(Optional.empty());

        String jwt = createJwt("chat-service", "moderation:decide");
        String requestJson = """
            {
              "message_id": "msg-001",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Hola, ¿cómo resuelvo el ejercicio?",
              "context_flags": {
                "thread_id": "thread-99",
                "is_reply": true
              }
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message_id", is("msg-001")))
                .andExpect(jsonPath("$.decision", is("ALLOW")))
                .andExpect(jsonPath("$.reason_code", is("CLEAN")))
                .andExpect(jsonPath("$.classifier_used", is("deterministic")))
                .andExpect(jsonPath("$.latency_ms", lessThan(800)))
                .andExpect(jsonPath("$.incident_id", nullValue()));

        verify(repository).save(any(), eq("curso-42"), eq("student"), eq("Hola, ¿cómo resuelvo el ejercicio?"));
    }

    @Test
    void test200OkWithGatewayIdentityHeaders() throws Exception {
        when(repository.findByMessageId("msg-gw-1")).thenReturn(Optional.empty());

        String requestJson = """
            {
              "message_id": "msg-gw-1",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Mensaje ruteado desde gateway"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "service")
                        .header(IdentityHeaders.SERVICE_ID, "chat-service")
                        .header(IdentityHeaders.SERVICE_SCOPES, "moderation:decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message_id", is("msg-gw-1")))
                .andExpect(jsonPath("$.decision", is("ALLOW")));
    }

    @Test
    void test401UnauthorizedWithoutToken() throws Exception {
        String requestJson = """
            {
              "message_id": "msg-002",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Mensaje sin credenciales"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test401UnauthorizedTokenMissingScope() throws Exception {
        String jwtWithoutScope = createJwt("chat-service", "other:scope");
        String requestJson = """
            {
              "message_id": "msg-003",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Mensaje con token sin scope requerido"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwtWithoutScope)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test400BadRequestMissingMessageId() throws Exception {
        String jwt = createJwt("chat-service", "moderation:decide");
        String requestJson = """
            {
              "message_id": "",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Texto válido"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test400BadRequestMissingText() throws Exception {
        String jwt = createJwt("chat-service", "moderation:decide");
        String requestJson = """
            {
              "message_id": "msg-004",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test400BadRequestTextExceedsMaxLength() throws Exception {
        String jwt = createJwt("chat-service", "moderation:decide");
        String tooLongText = "x".repeat(4097);
        String requestJson = String.format("""
            {
              "message_id": "msg-005",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "%s"
            }
        """, tooLongText);

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIdempotencyReturnsSameDecisionWithoutDuplicateAudit() throws Exception {
        String jwt = createJwt("chat-service", "moderation:decide");
        String requestJson = """
            {
              "message_id": "msg-006",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Mensaje para probar idempotencia"
            }
        """;

        AtomicReference<ModerationDecision> storedDecision = new AtomicReference<>();
        when(repository.findByMessageId("msg-006")).thenAnswer(inv -> Optional.ofNullable(storedDecision.get()));

        // Primera invocación: procesa y persiste
        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("ALLOW")))
                .andExpect(jsonPath("$.message_id", is("msg-006")));

        ArgumentCaptor<ModerationDecision> captor = ArgumentCaptor.forClass(ModerationDecision.class);
        verify(repository, times(1)).save(captor.capture(), eq("curso-42"), eq("student"), eq("Mensaje para probar idempotencia"));
        storedDecision.set(captor.getValue());

        // Segunda invocación con idéntico message_id: debe devolver la misma decisión sin llamar a save nuevamente
        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("ALLOW")))
                .andExpect(jsonPath("$.message_id", is("msg-006")));

        // Verifica que save NO se volvió a invocar
        verify(repository, times(1)).save(any(), any(), any(), any());
    }

    @Test
    void testDiscrepancySameMessageIdDifferentTextReturnsOriginalDecision() throws Exception {
        String jwt = createJwt("chat-service", "moderation:decide");
        MessageContent originalContent = new MessageContent("Texto original");
        ModerationDecision original = ModerationDecision.allow(
                "msg-disc", "CLEAN", "deterministic", 10L, originalContent.getContentHash()
        );

        when(repository.findByMessageId("msg-disc")).thenReturn(Optional.of(original));

        String differentPayload = """
            {
              "message_id": "msg-disc",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Texto alterado fraudulentamente"
            }
        """;

        mvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(differentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message_id", is("msg-disc")))
                .andExpect(jsonPath("$.decision", is("ALLOW")));

        verify(repository, never()).save(any(), any(), any(), any());
    }

    @Test
    void testProcessingTimeoutExceeding800MsReturnsPending() throws Exception {
        // Creamos un servicio con motor simulando retraso excesivo
        ModerationDecisionService timeoutService = new ModerationDecisionService(
                repository,
                100L, // Timeout de 100 ms para acelerar test
                cmd -> {
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ModerationDecision.allow(cmd.messageId(), "CLEAN", "deterministic", 300L, "hash");
                }
        );
        ModerationDecisionController timeoutController = new ModerationDecisionController(authorization, timeoutService);
        MockMvc timeoutMvc = MockMvcBuilders.standaloneSetup(timeoutController)
                .addFilters(identityFilter)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        when(repository.findByMessageId("msg-timeout")).thenReturn(Optional.empty());

        String jwt = createJwt("chat-service", "moderation:decide");
        String requestJson = """
            {
              "message_id": "msg-timeout",
              "course_id": "curso-42",
              "sender_id": "user-1",
              "sender_role": "student",
              "text": "Mensaje que provoca timeout"
            }
        """;

        timeoutMvc.perform(post("/moderation/v1/decisions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message_id", is("msg-timeout")))
                .andExpect(jsonPath("$.decision", is("PENDING")))
                .andExpect(jsonPath("$.reason_code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.classifier_used", is("fallback")))
                .andExpect(jsonPath("$.incident_id", notNullValue()));

        verify(repository).save(any(), eq("curso-42"), eq("student"), eq("Mensaje que provoca timeout"));
    }

    @Test
    void testDataMinimizationAllowNeverPersistsText() {
        ModerationDecision decision = ModerationDecision.allow(
                "msg-min", "CLEAN", "deterministic", 10L, "hash"
        );

        // Verificamos el comportamiento de minimización del agregado
        assertThat(decision.shouldPersistText()).isFalse();
    }
}
