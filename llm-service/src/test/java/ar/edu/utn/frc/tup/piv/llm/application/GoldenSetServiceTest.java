package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSet;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoldenSetServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UUID actorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private final UUID setId = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private final CallerIdentity actor = new CallerIdentity("admin-service", actorId, "req-1", "trace-1");
  @Mock private GoldenSetRepository goldenSets;
  @Mock private IdempotencyRepository idempotency;
  @Mock private AuditRepository audit;
  @Captor private ArgumentCaptor<String> hash;
  private GoldenSetService service;

  @BeforeEach void setUp() { service = new GoldenSetService(goldenSets, idempotency, audit, mapper); }

  @Test void createsGoldenSetCompletesIdempotencyAndAuditsActor() {
    UUID key = UUID.randomUUID();
    GoldenSet set = new GoldenSet(setId, 1, "1.0", "es", OffsetDateTime.parse("2026-01-02T03:04:05Z"), List.of());
    when(idempotency.replay(eq("create-golden-set"), eq(actor), eq(key), hash.capture())).thenReturn(Optional.empty());
    when(goldenSets.rubricId("1.0", "es")).thenReturn(UUID.randomUUID());
    when(goldenSets.create(any(), eq("1.0"), eq("es"), eq(actorId), eq("admin-service"))).thenReturn(set);

    var response = service.create("1.0", "es", key, actor);

    assertThat(response.path("id").asText()).isEqualTo(setId.toString());
    assertThat(response.path("version").asLong()).isEqualTo(1);
    assertThat(hash.getValue()).hasSize(64);
    verify(idempotency).complete(eq("create-golden-set"), eq(actor), eq(key), eq(setId), eq(response));
    verify(audit).record(eq("golden-set.created"), eq("golden-set"), eq(setId), eq(actor), eq("{\"version\":1}"));
  }

  @Test void returnsStoredResponseWithoutCreatingOrAuditingWhenKeyIsReplayed() throws Exception {
    UUID key = UUID.randomUUID();
    var stored = mapper.readTree("{\"id\":\"22222222-2222-2222-2222-222222222222\",\"version\":1}");
    when(idempotency.replay(eq("create-golden-set"), eq(actor), eq(key), anyString())).thenReturn(Optional.of(stored));

    assertThat(service.create("1.0", "es", key, actor)).isSameAs(stored);

    verify(goldenSets, never()).rubricId(anyString(), anyString());
    verify(goldenSets, never()).create(any(), anyString(), anyString(), any(), anyString());
    verify(idempotency, never()).complete(anyString(), any(), any(), any(), any());
    verify(audit, never()).record(anyString(), anyString(), any(), any(), anyString());
  }

  @Test void addsEntryWithAContentHashAndAuditsIt() throws Exception {
    UUID key = UUID.randomUUID(); UUID entryId = UUID.randomUUID();
    var transcript = mapper.readTree("[{\"role\":\"learner\",\"text\":\"hola\"}]");
    var scores = mapper.readTree("{\"autonomy\":80}");
    when(idempotency.replay(eq("create-golden-set-entry"), eq(actor), eq(key), anyString())).thenReturn(Optional.empty());
    when(goldenSets.addEntry(eq(setId), eq(transcript), eq(scores), hash.capture(), eq(actorId))).thenReturn(entryId);

    var response = service.addEntry(setId, transcript, scores, key, actor);

    assertThat(response.path("id").asText()).isEqualTo(entryId.toString());
    assertThat(hash.getValue()).hasSize(64);
    verify(idempotency).complete(eq("create-golden-set-entry"), eq(actor), eq(key), eq(entryId), eq(response));
    verify(audit).record(eq("golden-set.entry-created"), eq("golden-set-entry"), eq(entryId), eq(actor), eq("{\"goldenSetId\":\"" + setId + "\"}"));
  }

  @Test void failsWhenRequestedGoldenSetDoesNotExist() {
    when(goldenSets.find(setId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(setId)).isInstanceOf(GoldenSetNotFoundException.class);
  }

  @Test void passesTheRequestedPageAsDatabaseOffset() {
    service.list("1.0", 2, 20);
    verify(goldenSets).list("1.0", 20, 40);
  }
}
