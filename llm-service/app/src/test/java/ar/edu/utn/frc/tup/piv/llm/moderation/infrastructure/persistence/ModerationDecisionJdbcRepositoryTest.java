package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationDecisionJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private ModerationDecisionJdbcRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        repository = new ModerationDecisionJdbcRepository(jdbcTemplate);
    }

    @Test
    void saveWithAllowDecisionStrictlyPassesNullForMessageText() {
        ModerationDecision allowDecision = ModerationDecision.allow(
                "msg-allow-1",
                ModerationReasonCode.CLEAN,
                "deterministic",
                15L,
                "sha256-hash"
        );

        repository.save(allowDecision, "curso-42", "student", "Texto que no debe persistirse");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(
                anyString(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture()
        );

        var values = captor.getAllValues();
        assertThat(values.get(0)).isEqualTo("msg-allow-1");
        assertThat(values.get(1)).isEqualTo("curso-42");
        assertThat(values.get(2)).isEqualTo("student");
        assertThat(values.get(3)).isEqualTo("ALLOW");
        assertThat(values.get(4)).isEqualTo("CLEAN");
        assertThat(values.get(5)).isEqualTo("deterministic");
        assertThat(values.get(6)).isEqualTo(15L);
        assertThat(values.get(7)).isNull(); // incident_id is null for ALLOW
        assertThat(values.get(8)).isEqualTo("sha256-hash");
        assertThat(values.get(9)).isNull(); // message_text MUST BE NULL for ALLOW (minimización de datos)
        assertThat(values.get(10)).isNull(); // degradation_reason is null for normal ALLOW
    }

    @Test
    void saveWithBlockOrPendingDecisionPersistsMessageText() {
        UUID incidentId = UUID.randomUUID();
        ModerationDecision blockDecision = ModerationDecision.block(
                "msg-block-1",
                ModerationReasonCode.SPAM,
                "contextual",
                60L,
                "sha256-hash",
                incidentId
        );

        repository.save(blockDecision, "curso-42", "student", "Texto sospechoso o de spam");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(
                anyString(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture()
        );

        var values = captor.getAllValues();
        assertThat(values.get(0)).isEqualTo("msg-block-1");
        assertThat(values.get(3)).isEqualTo("BLOCK");
        assertThat(values.get(7)).isEqualTo(incidentId);
        assertThat(values.get(9)).isEqualTo("Texto sospechoso o de spam");
        assertThat(values.get(10)).isNull();
    }

    @Test
    void saveWithDegradationReasonPersistsColumn() {
        ModerationDecision pendingDecision = ModerationDecision.pendingReview(
                "msg-deg-1",
                ModerationReasonCode.NEEDS_REVIEW,
                "fallback",
                120L,
                "sha256-hash",
                ModerationReasonCode.CONTEXTUAL_UNAVAILABLE
        );

        repository.save(pendingDecision, "curso-42", "student", "Texto pendiente de revision");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(
                anyString(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture(), captor.capture(), captor.capture(), captor.capture(), captor.capture(),
                captor.capture()
        );

        var values = captor.getAllValues();
        assertThat(values.get(0)).isEqualTo("msg-deg-1");
        assertThat(values.get(3)).isEqualTo("PENDING_REVIEW");
        assertThat(values.get(9)).isEqualTo("Texto pendiente de revision");
        assertThat(values.get(10)).isEqualTo(ModerationReasonCode.CONTEXTUAL_UNAVAILABLE);
    }

    @Test
    void findByMessageIdReturnsEmptyWhenNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("msg-none")))
                .thenThrow(new EmptyResultDataAccessException(1));

        var result = repository.findByMessageId("msg-none");

        assertThat(result).isEmpty();
    }

    @Test
    void findByMessageIdReturnsMappedDecisionWhenFound() throws Exception {
        UUID incidentId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("msg-ok")))
                .thenAnswer(inv -> {
                    RowMapper<ModerationDecision> mapper = inv.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getString("message_id")).thenReturn("msg-ok");
                    when(rs.getString("decision")).thenReturn("BLOCK");
                    when(rs.getString("reason_code")).thenReturn("SPAM");
                    when(rs.getString("classifier_used")).thenReturn("contextual");
                    when(rs.getLong("latency_ms")).thenReturn(45L);
                    when(rs.getString("incident_id")).thenReturn(incidentId.toString());
                    when(rs.getString("content_hash")).thenReturn("hash-123");
                    when(rs.getString("degradation_reason")).thenReturn(null);
                    return mapper.mapRow(rs, 1);
                });

        var result = repository.findByMessageId("msg-ok");

        assertThat(result).isPresent();
        assertThat(result.get().getMessageId()).isEqualTo("msg-ok");
        assertThat(result.get().getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(result.get().getIncidentId()).isEqualTo(incidentId);
    }

    @Test
    void findByMessageIdReturnsAllowWithNullIncidentId() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("msg-allow")))
                .thenAnswer(inv -> {
                    RowMapper<ModerationDecision> mapper = inv.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getString("message_id")).thenReturn("msg-allow");
                    when(rs.getString("decision")).thenReturn("ALLOW");
                    when(rs.getString("reason_code")).thenReturn("CLEAN");
                    when(rs.getString("classifier_used")).thenReturn("deterministic");
                    when(rs.getLong("latency_ms")).thenReturn(10L);
                    when(rs.getString("incident_id")).thenReturn(null);
                    when(rs.getString("content_hash")).thenReturn("hash-clean");
                    return mapper.mapRow(rs, 1);
                });

        var result = repository.findByMessageId("msg-allow");

        assertThat(result).isPresent();
        assertThat(result.get().getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(result.get().getIncidentId()).isNull();
    }
}
