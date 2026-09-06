package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSet;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoldenSetService {
  private final GoldenSetRepository goldenSets;
  private final IdempotencyRepository idempotency;
  private final AuditRepository audit;
  private final ObjectMapper mapper;
  public GoldenSetService(GoldenSetRepository goldenSets, IdempotencyRepository idempotency, AuditRepository audit, ObjectMapper mapper) {
    this.goldenSets = goldenSets; this.idempotency = idempotency; this.audit = audit; this.mapper = mapper;
  }
  @Transactional
  public JsonNode create(String rubricVersion, String language, UUID key, CallerIdentity actor) {
    var replay = idempotency.replay("create-golden-set", actor, key, hash(rubricVersion + ":" + language));
    if (replay.isPresent()) return replay.get();
    GoldenSet set = goldenSets.create(goldenSets.rubricId(rubricVersion, language), rubricVersion, language, actor.delegatedUserId(), actor.serviceId());
    JsonNode response = mapper.valueToTree(new CreatedGoldenSet(set.id(), set.version(), set.rubricVersion(), set.language(), set.createdAt().toString()));
    idempotency.complete("create-golden-set", actor, key, set.id(), response);
    audit.record("golden-set.created", "golden-set", set.id(), actor, "{\"version\":" + set.version() + "}");
    return response;
  }
  @Transactional
  public JsonNode addEntry(UUID goldenSetId, JsonNode transcript, JsonNode scores, UUID key, CallerIdentity actor) {
    var replay = idempotency.replay("create-golden-set-entry", actor, key, hash(goldenSetId + ":" + transcript + ":" + scores));
    if (replay.isPresent()) return replay.get();
    UUID entryId = goldenSets.addEntry(goldenSetId, transcript, scores, hash(transcript.toString()), actor.delegatedUserId());
    JsonNode response = mapper.valueToTree(new CreatedEntry(entryId, goldenSetId));
    idempotency.complete("create-golden-set-entry", actor, key, entryId, response);
    audit.record("golden-set.entry-created", "golden-set-entry", entryId, actor, "{\"goldenSetId\":\"" + goldenSetId + "\"}");
    return response;
  }
  public GoldenSet get(UUID id) { return goldenSets.find(id).orElseThrow(() -> new GoldenSetNotFoundException(id)); }
  public List<GoldenSet> list(String rubricVersion, int page, int size) { return goldenSets.list(rubricVersion, size, page * size); }
  private String hash(String value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception exception) { throw new IllegalStateException(exception); }
  }
  public record CreatedGoldenSet(UUID id, long version, String rubricVersion, String language, String createdAt) {}
  public record CreatedEntry(UUID id, UUID goldenSetId) {}
}
