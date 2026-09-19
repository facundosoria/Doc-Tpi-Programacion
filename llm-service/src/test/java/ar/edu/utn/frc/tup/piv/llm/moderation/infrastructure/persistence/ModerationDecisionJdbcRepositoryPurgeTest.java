package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationDecisionJdbcRepositoryPurgeTest {

    private JdbcTemplate jdbc;
    private ModerationDecisionJdbcRepository repository;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        repository = new ModerationDecisionJdbcRepository(jdbc);
    }

    @Test
    void purgeByIncidentIdsReturnsZeroForNullOrEmptyWithoutQuery() {
        assertThat(repository.purgeByIncidentIds(null, OffsetDateTime.now())).isZero();
        assertThat(repository.purgeByIncidentIds(List.of(), OffsetDateTime.now())).isZero();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void purgeByIncidentIdsBuildsPlaceholdersAndParams() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(2);

        int updated = repository.purgeByIncidentIds(List.of(a, b), now);

        assertThat(updated).isEqualTo(2);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(sql.capture(), params.capture());
        assertThat(sql.getValue()).contains("IN (?,?)").contains("message_text = NULL");
        assertThat(params.getValue()).containsExactly(now, a, b);
    }

    @Test
    void purgeStandaloneBeforePassesNowThenCutoff() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoff = now.minusDays(30);
        when(jdbc.update(anyString(), eq(now), eq(cutoff))).thenReturn(4);

        assertThat(repository.purgeStandaloneBefore(cutoff, now)).isEqualTo(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rowMapperToleratesMissingDegradationColumn() throws SQLException {
        ArgumentCaptor<RowMapper<ModerationDecision>> cap = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.queryForObject(anyString(), cap.capture(), eq("m1"))).thenReturn(null);
        repository.findByMessageId("m1");

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("message_id")).thenReturn("m1");
        when(rs.getString("decision")).thenReturn("BLOCK");
        when(rs.getString("reason_code")).thenReturn("PROFANITY");
        when(rs.getString("classifier_used")).thenReturn("det");
        when(rs.getLong("latency_ms")).thenReturn(5L);
        when(rs.getString("incident_id")).thenReturn(null);
        when(rs.getString("content_hash")).thenReturn("h");
        when(rs.getString("degradation_reason")).thenThrow(new SQLException("no column"));

        ModerationDecision d = cap.getValue().mapRow(rs, 0);

        assertThat(d.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(d.getDegradationReason()).isNull();
        assertThat(d.getMessageId()).isEqualTo("m1");
    }
}
