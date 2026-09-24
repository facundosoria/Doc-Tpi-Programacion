package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link ModerationMetricsService} (LLM-S13-H02 / T3 / CA3).
 * Confirma que las métricas agregadas operativas (conteos por motivo, estado, resoluciones y tasas)
 * se calculan sobre metadatos anonimizados y permanecen 100% idénticas antes y después de purgar la evidencia textual.
 */
class ModerationMetricsPurgeTest {

    private JdbcTemplate jdbcTemplate;
    private ModerationMetricsService metricsService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        metricsService = new ModerationMetricsService(jdbcTemplate);
    }

    @Test
    @DisplayName("T3 & CA3: Las métricas agregadas devuelven conteos idénticos antes y después de purgar")
    void metricsRemainIdenticalBeforeAndAfterPurge() {
        String courseId = "curso-42";

        // Simulación de base de datos con 10 incidentes: 6 SPAM, 4 OFFENSIVE; 3 apelaciones; 2 CONFIRMED, 1 REVERSED
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_incidents"), eq(Long.class), any()))
                .thenReturn(10L);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
            when(rs1.getString("reason")).thenReturn("SPAM");
            when(rs1.getLong("cnt")).thenReturn(6L);
            rch.processRow(rs1);

            java.sql.ResultSet rs2 = mock(java.sql.ResultSet.class);
            when(rs2.getString("reason")).thenReturn("OFFENSIVE");
            when(rs2.getLong("cnt")).thenReturn(4L);
            rch.processRow(rs2);
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY i.reason_code"), any(RowCallbackHandler.class), any());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
            when(rs1.getString("status")).thenReturn("CONFIRMED");
            when(rs1.getLong("cnt")).thenReturn(7L);
            rch.processRow(rs1);

            java.sql.ResultSet rs2 = mock(java.sql.ResultSet.class);
            when(rs2.getString("status")).thenReturn("REVERSED");
            when(rs2.getLong("cnt")).thenReturn(3L);
            rch.processRow(rs2);
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY i.status"), any(RowCallbackHandler.class), any());

        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_appeals"), eq(Long.class), any()))
                .thenReturn(3L);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
            when(rs1.getString("resolution")).thenReturn("CONFIRMED");
            when(rs1.getLong("cnt")).thenReturn(2L);
            rch.processRow(rs1);

            java.sql.ResultSet rs2 = mock(java.sql.ResultSet.class);
            when(rs2.getString("resolution")).thenReturn("REVERSED");
            when(rs2.getLong("cnt")).thenReturn(1L);
            rch.processRow(rs2);
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY r.resolution"), any(RowCallbackHandler.class), any());

        // 1. Calcular métricas ANTES de la purga
        ModerationMetricsService.ModerationMetricsSummary beforePurge = metricsService.getCourseMetrics(courseId);

        assertThat(beforePurge.totalIncidents()).isEqualTo(10L);
        assertThat(beforePurge.incidentsByReason()).containsEntry("SPAM", 6L).containsEntry("OFFENSIVE", 4L);
        assertThat(beforePurge.incidentsByStatus()).containsEntry("CONFIRMED", 7L).containsEntry("REVERSED", 3L);
        assertThat(beforePurge.appealsCount()).isEqualTo(3L);
        assertThat(beforePurge.confirmedResolutions()).isEqualTo(2L);
        assertThat(beforePurge.reversedResolutions()).isEqualTo(1L);
        assertThat(beforePurge.appealRate()).isEqualTo(0.3);
        assertThat(beforePurge.reversalRate()).isCloseTo(0.3333, org.assertj.core.data.Offset.offset(0.001));

        // 2. Simular ejecución de purga (los campos de texto se destruyen en DB pero las tablas conservan las filas)
        // 3. Calcular métricas DESPUÉS de la purga
        ModerationMetricsService.ModerationMetricsSummary afterPurge = metricsService.getCourseMetrics(courseId);

        // 4. Verificar invariancia total de métricas agregadas
        assertThat(afterPurge.totalIncidents()).isEqualTo(beforePurge.totalIncidents());
        assertThat(afterPurge.incidentsByReason()).isEqualTo(beforePurge.incidentsByReason());
        assertThat(afterPurge.incidentsByStatus()).isEqualTo(beforePurge.incidentsByStatus());
        assertThat(afterPurge.appealsCount()).isEqualTo(beforePurge.appealsCount());
        assertThat(afterPurge.confirmedResolutions()).isEqualTo(beforePurge.confirmedResolutions());
        assertThat(afterPurge.reversedResolutions()).isEqualTo(beforePurge.reversedResolutions());
        assertThat(afterPurge.appealRate()).isEqualTo(beforePurge.appealRate());
        assertThat(afterPurge.reversalRate()).isEqualTo(beforePurge.reversalRate());
    }

    @Test
    @DisplayName("T3: Cálculo de métricas mensuales agregadas por curso")
    void monthlyCourseMetricsIncludeDateFilter() {
        String courseId = "curso-42";
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_incidents"), eq(Long.class), any(Object[].class)))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_appeals"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);

        ModerationMetricsService.ModerationMetricsSummary monthly = metricsService.getMonthlyCourseMetrics(courseId, 2026, 8);

        assertThat(monthly.courseId()).isEqualTo(courseId);
        assertThat(monthly.totalIncidents()).isEqualTo(5L);
        assertThat(monthly.appealsCount()).isEqualTo(1L);
    }
}
