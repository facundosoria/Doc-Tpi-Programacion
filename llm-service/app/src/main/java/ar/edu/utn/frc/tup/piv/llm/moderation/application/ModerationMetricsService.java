package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de métricas estadísticas agregadas de moderación (LLM-S13-H02 / T3 / CA3).
 * Calcula indicadores operativos garantizando que los conteos no dependan del texto purgado
 * y permanezcan 100% idénticos antes y después de la ejecución de purgas GDPR.
 */
@Service
public class ModerationMetricsService {

    private final JdbcTemplate jdbcTemplate;

    public ModerationMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ModerationMetricsSummary getCourseMetrics(String courseId) {
        return calculateMetrics(courseId, null, null);
    }

    @Transactional(readOnly = true)
    public ModerationMetricsSummary getMonthlyCourseMetrics(String courseId, int year, int month) {
        return calculateMetrics(courseId, year, month);
    }

    @Transactional(readOnly = true)
    public ModerationMetricsSummary getGlobalMetrics() {
        return calculateMetrics(null, null, null);
    }

    private ModerationMetricsSummary calculateMetrics(String courseId, Integer year, Integer month) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (courseId != null && !courseId.isBlank()) {
            whereClause.append(" AND i.course_id = ? ");
            params.add(courseId.trim());
        }
        if (year != null && month != null) {
            whereClause.append(" AND EXTRACT(YEAR FROM i.created_at) = ? AND EXTRACT(MONTH FROM i.created_at) = ? ");
            params.add(year);
            params.add(month);
        }

        // 1. Total de incidentes
        String totalSql = "SELECT COUNT(*) FROM llm.moderation_incidents i " + whereClause;
        Long totalIncidentsLong = jdbcTemplate.queryForObject(totalSql, Long.class, params.toArray());
        long totalIncidents = totalIncidentsLong != null ? totalIncidentsLong : 0L;

        // 2. Desglose por motivo (reason_code)
        String reasonSql = "SELECT COALESCE(i.reason_code, 'UNKNOWN') AS reason, COUNT(*) AS cnt " +
                "FROM llm.moderation_incidents i " + whereClause + " GROUP BY i.reason_code";
        Map<String, Long> byReason = new HashMap<>();
        jdbcTemplate.query(reasonSql, (rs) -> {
            byReason.put(rs.getString("reason"), rs.getLong("cnt"));
        }, params.toArray());

        // 3. Desglose por estado (status)
        String statusSql = "SELECT i.status, COUNT(*) AS cnt " +
                "FROM llm.moderation_incidents i " + whereClause + " GROUP BY i.status";
        Map<String, Long> byStatus = new HashMap<>();
        jdbcTemplate.query(statusSql, (rs) -> {
            byStatus.put(rs.getString("status"), rs.getLong("cnt"));
        }, params.toArray());

        // 4. Apelaciones registradas sobre estos incidentes
        String appealsSql = "SELECT COUNT(*) FROM llm.moderation_appeals a " +
                "INNER JOIN llm.moderation_incidents i ON a.incident_id = i.id " + whereClause;
        Long appealsLong = jdbcTemplate.queryForObject(appealsSql, Long.class, params.toArray());
        long appealsCount = appealsLong != null ? appealsLong : 0L;

        // 5. Resoluciones docentes confirmadas vs revertidas
        String resolutionsSql = "SELECT r.resolution, COUNT(*) AS cnt " +
                "FROM llm.moderation_resolutions r " +
                "INNER JOIN llm.moderation_incidents i ON r.incident_id = i.id " +
                whereClause + " GROUP BY r.resolution";
        Map<String, Long> byResolution = new HashMap<>();
        jdbcTemplate.query(resolutionsSql, (rs) -> {
            byResolution.put(rs.getString("resolution"), rs.getLong("cnt"));
        }, params.toArray());

        long confirmed = byResolution.getOrDefault("CONFIRMED", 0L);
        long reversed = byResolution.getOrDefault("REVERSED", 0L);
        long totalResolved = confirmed + reversed;

        double appealRate = totalIncidents > 0 ? (double) appealsCount / totalIncidents : 0.0;
        double reversalRate = totalResolved > 0 ? (double) reversed / totalResolved : 0.0;

        return new ModerationMetricsSummary(
                courseId,
                totalIncidents,
                byReason,
                byStatus,
                appealsCount,
                confirmed,
                reversed,
                appealRate,
                reversalRate
        );
    }

    public record ModerationMetricsSummary(
            String courseId,
            long totalIncidents,
            Map<String, Long> incidentsByReason,
            Map<String, Long> incidentsByStatus,
            long appealsCount,
            long confirmedResolutions,
            long reversedResolutions,
            double appealRate,
            double reversalRate
    ) {
    }
}
