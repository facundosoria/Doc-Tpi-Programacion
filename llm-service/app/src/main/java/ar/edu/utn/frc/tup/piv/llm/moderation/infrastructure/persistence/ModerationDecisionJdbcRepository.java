package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDecisionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia JDBC para auditoría inmutable e idempotencia de decisiones de moderación.
 */
@Repository
public class ModerationDecisionJdbcRepository implements ModerationDecisionRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ModerationDecision> rowMapper = (rs, rowNum) -> {
        String messageId = rs.getString("message_id");
        ModerationDecisionEnum decision = ModerationDecisionEnum.valueOf(rs.getString("decision"));
        String reasonCode = rs.getString("reason_code");
        String classifierUsed = rs.getString("classifier_used");
        long latencyMs = rs.getLong("latency_ms");
        String incidentStr = rs.getString("incident_id");
        UUID incidentId = incidentStr != null ? UUID.fromString(incidentStr) : null;
        String contentHash = rs.getString("content_hash");
        String degradationReason = null;
        try {
            degradationReason = rs.getString("degradation_reason");
        } catch (Exception ignored) {
            // Columna ausente en esquemas antiguos o proyecciones parciales
        }

        return new ModerationDecision(
                messageId,
                decision,
                reasonCode,
                classifierUsed,
                latencyMs,
                incidentId,
                contentHash,
                degradationReason
        );
    };

    public ModerationDecisionJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ModerationDecision> findByMessageId(String messageId) {
        String sql = """
            SELECT message_id, decision, reason_code, classifier_used, latency_ms, incident_id, content_hash, degradation_reason
            FROM llm.moderation_decisions
            WHERE message_id = ?
        """;
        try {
            ModerationDecision decision = jdbcTemplate.queryForObject(sql, rowMapper, messageId);
            return Optional.ofNullable(decision);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(ModerationDecision decision, String courseId, String senderRole, String messageText) {
        // Minimización de datos estricta: si la decisión es ALLOW, el texto del mensaje NUNCA se persiste
        String textToPersist = decision.shouldPersistText() ? messageText : null;

        String sql = """
            INSERT INTO llm.moderation_decisions (
                message_id, course_id, sender_role, decision, reason_code,
                classifier_used, latency_ms, incident_id, content_hash, message_text, degradation_reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (message_id) DO NOTHING
        """;

        jdbcTemplate.update(
                sql,
                decision.getMessageId(),
                courseId,
                senderRole,
                decision.getDecision().name(),
                decision.getReasonCode(),
                decision.getClassifierUsed(),
                decision.getLatencyMs(),
                decision.getIncidentId(),
                decision.getContentHash(),
                textToPersist,
                decision.getDegradationReason()
        );
    }

    public int purgeByIncidentIds(java.util.Collection<UUID> incidentIds, java.time.OffsetDateTime now) {
        if (incidentIds == null || incidentIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(incidentIds.size(), "?"));
        String sql = "UPDATE llm.moderation_decisions SET message_text = NULL, purged_at = ? WHERE incident_id IN (" + placeholders + ")";
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(now);
        params.addAll(incidentIds);
        return jdbcTemplate.update(sql, params.toArray());
    }

    public int purgeStandaloneBefore(java.time.OffsetDateTime cutoff, java.time.OffsetDateTime now) {
        String sql = """
            UPDATE llm.moderation_decisions
            SET message_text = NULL, purged_at = ?
            WHERE created_at <= ? AND message_text IS NOT NULL
        """;
        return jdbcTemplate.update(sql, now, cutoff);
    }
}
