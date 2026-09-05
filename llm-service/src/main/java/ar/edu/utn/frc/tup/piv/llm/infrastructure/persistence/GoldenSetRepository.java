package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSet;
import ar.edu.utn.frc.tup.piv.llm.domain.GoldenSetEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GoldenSetRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  public GoldenSetRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
  public UUID rubricId(String version, String language) {
    List<UUID> result = jdbc.queryForList("select id from llm.rubric_versions where version = ? and language = ?", UUID.class, version, language);
    if (result.isEmpty()) throw new IllegalArgumentException("La versión de rúbrica no existe");
    return result.getFirst();
  }
  public GoldenSet create(UUID rubricId, String rubricVersion, String language, UUID actor, String service) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into llm.golden_sets (id, rubric_version_id, language, created_by_user_id, created_by_service) values (?, ?, ?, ?, ?)", id, rubricId, language, actor, service);
    return find(id).orElseThrow();
  }
  public UUID addEntry(UUID goldenSetId, JsonNode transcript, JsonNode scores, String hash, UUID actor) {
    UUID id = UUID.randomUUID();
    int inserted = jdbc.update("insert into llm.golden_set_entries (id, golden_set_id, transcript, reference_scores, content_hash, created_by_user_id) select ?, ?, ?::jsonb, ?::jsonb, ?, ? where exists (select 1 from llm.golden_sets where id = ?)", id, goldenSetId, transcript.toString(), scores.toString(), hash, actor, goldenSetId);
    if (inserted == 0) throw new IllegalArgumentException("El golden set no existe");
    return id;
  }
  public Optional<GoldenSet> find(UUID id) {
    List<GoldenSet> sets = jdbc.query("select g.id, g.version_no, r.version rubric_version, g.language, g.created_at from llm.golden_sets g join llm.rubric_versions r on r.id = g.rubric_version_id where g.id = ?", this::set, id);
    if (sets.isEmpty()) return Optional.empty();
    GoldenSet set = sets.getFirst();
    List<GoldenSetEntry> entries = jdbc.query("select id, transcript, reference_scores, created_at from llm.golden_set_entries where golden_set_id = ? order by created_at", this::entry, id);
    return Optional.of(new GoldenSet(set.id(), set.version(), set.rubricVersion(), set.language(), set.createdAt(), entries));
  }
  public List<GoldenSet> list(String rubricVersion, int limit, int offset) {
    String sql = "select g.id, g.version_no, r.version rubric_version, g.language, g.created_at from llm.golden_sets g join llm.rubric_versions r on r.id = g.rubric_version_id " + (rubricVersion == null ? "" : "where r.version = ? ") + "order by g.created_at desc limit ? offset ?";
    List<GoldenSet> sets = rubricVersion == null ? jdbc.query(sql, this::set, limit, offset) : jdbc.query(sql, this::set, rubricVersion, limit, offset);
    return sets.stream().map(set -> new GoldenSet(set.id(), set.version(), set.rubricVersion(), set.language(), set.createdAt(), List.of())).toList();
  }
  private GoldenSet set(ResultSet rs, int row) throws SQLException {
    return new GoldenSet(UUID.fromString(rs.getString("id")), rs.getLong("version_no"), rs.getString("rubric_version"), rs.getString("language"), rs.getObject("created_at", OffsetDateTime.class), List.of());
  }
  private GoldenSetEntry entry(ResultSet rs, int row) throws SQLException {
    try { return new GoldenSetEntry(UUID.fromString(rs.getString("id")), mapper.readTree(rs.getString("transcript")), mapper.readTree(rs.getString("reference_scores")), rs.getObject("created_at", OffsetDateTime.class)); }
    catch (Exception exception) { throw new SQLException("JSON almacenado inválido", exception); }
  }
}
