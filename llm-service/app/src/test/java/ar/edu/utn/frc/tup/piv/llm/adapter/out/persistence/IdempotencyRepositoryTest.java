package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyRepositoryTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test void returnsEmptyWhenTheInsertSucceeds() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

    var result = repository.replay("create-calibration", actor(), UUID.randomUUID(), "hash");

    assertThat(result).isEmpty();
  }

  @Test void replaysTheStoredResponseForADuplicateRequest() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    CallerIdentity actor = actor();
    UUID key = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("dup"));
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getString("request_hash")).thenReturn("hash");
    when(rs.getString("response_body")).thenReturn("{\"id\":\"abc\"}");
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(), any(), any(), any()))
        .thenAnswer(invocation -> invocation.<ResultSetExtractor<?>>getArgument(1).extractData(rs));

    Optional<JsonNode> result = repository.replay("create-calibration", actor, key, "hash");

    assertThat(result).isPresent();
    assertThat(result.get().path("id").asText()).isEqualTo("abc");
  }

  @Test void rejectsADuplicateRequestWithADifferentPayload() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    when(jdbc.update(anyString(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("dup"));
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getString("request_hash")).thenReturn("other-hash");
    when(rs.getString("response_body")).thenReturn("{\"id\":\"abc\"}");
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(), any(), any(), any()))
        .thenAnswer(invocation -> invocation.<ResultSetExtractor<?>>getArgument(1).extractData(rs));

    assertThatThrownBy(() -> repository.replay("create-calibration", actor(), UUID.randomUUID(), "hash"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsADuplicateWhileTheOriginalRequestIsStillInFlight() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    when(jdbc.update(anyString(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("dup"));
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getString("request_hash")).thenReturn("hash");
    when(rs.getString("response_body")).thenReturn(null);
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(), any(), any(), any()))
        .thenAnswer(invocation -> invocation.<ResultSetExtractor<?>>getArgument(1).extractData(rs));

    assertThatThrownBy(() -> repository.replay("create-calibration", actor(), UUID.randomUUID(), "hash"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("La solicitud original sigue en curso");
  }

  @Test void rejectsAnUnreadableStoredResponse() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    when(jdbc.update(anyString(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("dup"));
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getString("request_hash")).thenReturn("hash");
    when(rs.getString("response_body")).thenReturn("not-json");
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(), any(), any(), any()))
        .thenAnswer(invocation -> invocation.<ResultSetExtractor<?>>getArgument(1).extractData(rs));

    assertThatThrownBy(() -> repository.replay("create-calibration", actor(), UUID.randomUUID(), "hash"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void rethrowsTheDuplicateWhenNoRowIsReturned() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    when(jdbc.update(anyString(), any(), any(), any(), any(), any()))
        .thenThrow(new DuplicateKeyException("dup"));
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(false);
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(), any(), any(), any()))
        .thenAnswer(invocation -> invocation.<ResultSetExtractor<?>>getArgument(1).extractData(rs));

    assertThatThrownBy(() -> repository.replay("create-calibration", actor(), UUID.randomUUID(), "hash"))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test void completesTheRequestWithTheResourceAndResponse() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new IdempotencyRepository(jdbc, mapper);
    CallerIdentity actor = actor();
    UUID key = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    JsonNode response = mapper.createObjectNode().put("id", "abc");

    repository.complete("create-calibration", actor, key, resourceId, response);

    verify(jdbc).update(anyString(), eq(resourceId), eq(response.toString()), eq("create-calibration"),
        eq(actor.serviceId()), eq(actor.delegatedUserId()), eq(key));
  }

  private CallerIdentity actor() {
    return new CallerIdentity("courses-service", UUID.randomUUID(), "req-x", "trace-x");
  }
}