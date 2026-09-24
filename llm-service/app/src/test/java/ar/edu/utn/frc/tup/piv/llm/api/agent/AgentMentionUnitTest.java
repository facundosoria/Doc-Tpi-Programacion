package ar.edu.utn.frc.tup.piv.llm.api.agent;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionRequest;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionResponse;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionController;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.MentionCitationDto;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.validation.MentionSenderValidator;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.AgentMentionService;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.QuotaExceededException;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.QuotaRegistry;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.AgentSelfMentionSanitizer;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.MentionLoopCircuitBreaker;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentMentionUnitTest {

  @Test
  @DisplayName("AgentMentionRequest cubre constructores auxiliares y safe UUID")
  void testAgentMentionRequestConstructorsAndUuid() {
    UUID cohortUuid = UUID.randomUUID();
    AgentMentionRequest req1 = new AgentMentionRequest(
        "msg-1", cohortUuid, "thread-req-1", "student", "std-1", "Hola"
    );
    assertThat(req1.cohortUuid()).isEqualTo(cohortUuid);

    AgentMentionRequest req2 = new AgentMentionRequest(
        "msg-2", (UUID) null, "thread-req-2", "student", "std-1", "Hola"
    );
    assertThat(req2.cohortUuid()).isNull();

    AgentMentionRequest req3 = new AgentMentionRequest(
        "msg-3", "not-a-valid-uuid", "thread-req-3", "student", "std-1", "Hola"
    );
    assertThat(req3.cohortUuid()).isNotNull();
    assertThat(req1.toString()).contains("msg-1");
  }

  @Test
  @DisplayName("MentionCitationDto y AgentMentionResponse cubren getters y records")
  void testDtos() {
    MentionCitationDto citation = new MentionCitationDto("doc.pdf", 12, "extracto");
    assertThat(citation.documentName()).isEqualTo("doc.pdf");
    assertThat(citation.pageNumber()).isEqualTo(12);
    assertThat(citation.excerpt()).isEqualTo("extracto");

    AgentMentionResponse resp = new AgentMentionResponse("Respuesta", List.of(citation), "OK");
    assertThat(resp.replyText()).isEqualTo("Respuesta");
    assertThat(resp.citations()).hasSize(1);
    assertThat(resp.status()).isEqualTo("OK");
  }

  @Test
  @DisplayName("QuotaRegistry soporta límites dinámicos, reset y rollovers")
  void testQuotaRegistryOperations() {
    QuotaRegistry registry = new QuotaRegistry();
    registry.setLimit("agent", "std-vip", 50);
    assertThat(registry.getRemaining("agent", "std-vip")).isEqualTo(50);

    registry.consume("agent", "std-vip");
    assertThat(registry.getRemaining("agent", "std-vip")).isEqualTo(49);

    registry.exhaustQuota("agent", "std-vip");
    assertThat(registry.getRemaining("agent", "std-vip")).isZero();

    assertThatThrownBy(() -> registry.consume("agent", "std-vip"))
        .isInstanceOf(QuotaExceededException.class);

    registry.resetAll();
    // Al reiniciar vuelve a la cuota base por defecto (20 para agente)
    assertThat(registry.getRemaining("agent", "std-vip")).isEqualTo(20);
  }

  @Test
  @DisplayName("QuotaExceededException expone function y retryAfterSeconds")
  void testQuotaExceededException() {
    QuotaExceededException ex = new QuotaExceededException("agent", 3600, "Cuota agotada");
    assertThat(ex.getFunctionKey()).isEqualTo("agent");
    assertThat(ex.getRetryAfterSeconds()).isEqualTo(3600);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(ex.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("3600");
  }

  @Test
  @DisplayName("AgentMentionController sobrecargas y fallback de servicio")
  void testControllerOverloadsAndFallbacks() {
    AgentMentionService serviceMock = mock(AgentMentionService.class);
    when(serviceMock.processMention(any(), any())).thenReturn(Optional.of(
        new AgentMentionService.Response("Respuesta mock", List.of(new AgentMentionService.Citation("doc.pdf", 1, "txt")), "OK")
    ));

    AgentMentionController controller = new AgentMentionController(serviceMock);

    // 1. Invocación directa sin headers
    AgentMentionRequest req1 = new AgentMentionRequest(
        "msg-1", UUID.randomUUID(), "thread-test-1", "student", "std-1", "Hola @agente"
    );
    ResponseEntity<?> response1 = controller.handleMention(req1);
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 2. Invocación con headers personalizados
    AgentMentionRequest req2 = new AgentMentionRequest(
        "msg-2", UUID.randomUUID(), "thread-test-2", "student", "std-1", "Hola @agente"
    );
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Caller-Service-Id", "custom-service");
    headers.add("X-User-Id", UUID.randomUUID().toString());
    headers.add("X-Request-Id", "req-123");
    headers.add("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    ResponseEntity<?> response2 = controller.handleMention(req2, headers);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 3. Fallback cuando service es null (modo H02)
    AgentMentionRequest req3 = new AgentMentionRequest(
        "msg-3", UUID.randomUUID(), "thread-test-3", "student", "std-1", "Hola @agente"
    );
    AgentMentionController fallbackController = new AgentMentionController(
        new MentionSenderValidator(), new MentionLoopCircuitBreaker(), new AgentSelfMentionSanitizer()
    );
    ResponseEntity<?> response3 = fallbackController.handleMention(req3, null);
    assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 4. Invocación con DTO legado H02
    var h02Req = new ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest(
        "msg-h02", UUID.randomUUID().toString(), "thread-test-4", "student", "std-1", "Hola @agente"
    );
    ResponseEntity<?> response4 = controller.handleMention(h02Req);
    assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 5. Invocación con DTO legado null o rol no autorizado
    ResponseEntity<?> response5 = controller.handleMention((ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest) null);
    assertThat(response5.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var h02BotReq = new ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest(
        "msg-bot", UUID.randomUUID().toString(), "thread-test-5", "bot", "std-1", "Hola"
    );
    ResponseEntity<?> response6 = controller.handleMention(h02BotReq);
    assertThat(response6.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // 6. Loop detectado en DTO legado
    MentionLoopCircuitBreaker loopBreaker = mock(MentionLoopCircuitBreaker.class);
    when(loopBreaker.evaluateMention("loop-thread")).thenReturn(
        new MentionLoopCircuitBreaker.EvaluationResult(false, true, true, 5, "loop-thread")
    );
    AgentMentionController loopController = new AgentMentionController(
        serviceMock, new MentionSenderValidator(), loopBreaker, new AgentSelfMentionSanitizer()
    );
    var h02LoopReq = new ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest(
        "msg-loop", UUID.randomUUID().toString(), "loop-thread", "student", "std-1", "Hola"
    );
    ResponseEntity<?> response7 = loopController.handleMention(h02LoopReq);
    assertThat(response7.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }
}
