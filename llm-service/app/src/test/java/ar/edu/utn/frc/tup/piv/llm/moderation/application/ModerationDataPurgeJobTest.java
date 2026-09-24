package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationDecisionJdbcRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationIncidentEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationRetentionPolicyEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationAppealRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationIncidentRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationResolutionRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationRetentionPolicyRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link ModerationEvidencePurgeJob} y {@link ModerationPurgeService} (LLM-S13-H02 / T2).
 * Valida los criterios de aceptación CA1, CA2, CA_negativo_1 y la sanitización estricta de logs (GDPR).
 */
class ModerationDataPurgeJobTest {

    private SpringDataModerationIncidentRepository incidentRepository;
    private SpringDataModerationAppealRepository appealRepository;
    private SpringDataModerationResolutionRepository resolutionRepository;
    private ModerationDecisionJdbcRepository decisionJdbcRepository;
    private SpringDataModerationRetentionPolicyRepository policyRepository;

    private ModerationPurgeService purgeService;
    private ModerationEvidencePurgeJob purgeJob;
    private ModerationDataPurgeJob dataPurgeJob;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(SpringDataModerationIncidentRepository.class);
        appealRepository = mock(SpringDataModerationAppealRepository.class);
        resolutionRepository = mock(SpringDataModerationResolutionRepository.class);
        decisionJdbcRepository = mock(ModerationDecisionJdbcRepository.class);
        policyRepository = mock(SpringDataModerationRetentionPolicyRepository.class);

        purgeService = new ModerationPurgeService(
                incidentRepository,
                appealRepository,
                resolutionRepository,
                decisionJdbcRepository,
                policyRepository
        );

        purgeJob = new ModerationEvidencePurgeJob(purgeService);
        dataPurgeJob = new ModerationDataPurgeJob(purgeJob, purgeService);

        // Configurar appender para verificar sanitización de logs
        Logger serviceLogger = (Logger) LoggerFactory.getLogger(ModerationPurgeService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);

        // Mock default de políticas
        when(policyRepository.findByIncidentType("BLOCK_CONFIRMED"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "BLOCK_CONFIRMED", "DEFAULT", 30, OffsetDateTime.now(), "SYSTEM", 1L)));
        when(policyRepository.findByIncidentType("BLOCK_REVERSED"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "BLOCK_REVERSED", "DEFAULT", 90, OffsetDateTime.now(), "SYSTEM", 1L)));
        when(policyRepository.findByIncidentType("PENDING_REVIEW"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "PENDING_REVIEW", "DEFAULT", 90, OffsetDateTime.now(), "SYSTEM", 1L)));
    }

    @Test
    @DisplayName("CA1 & Escenario 1 BDD: Purga destructiva irreversible de incidente BLOCK confirmado vencido")
    void purgeExpiredBlockIncidentDestroysEvidence() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID expiredId = UUID.randomUUID();
        ModerationIncidentEntity expiredIncident = new ModerationIncidentEntity(
                expiredId, "msg-001", "user-10", "curso-42", "CONFIRMED", "SPAM", "Texto spam sensible", now.minusDays(35)
        );

        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("CONFIRMED")))
                .thenReturn(List.of(expiredIncident));
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("BLOCK")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("REVERSED")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("PENDING_REVIEW")))
                .thenReturn(Collections.emptyList());

        when(incidentRepository.purgeIncidentsByIds(any(), any())).thenReturn(1);
        when(appealRepository.purgeAppealsByIncidentIds(any(), any())).thenReturn(1);
        when(resolutionRepository.purgeResolutionsByIncidentIds(any())).thenReturn(1);
        when(decisionJdbcRepository.purgeByIncidentIds(any(), any())).thenReturn(1);
        when(decisionJdbcRepository.purgeStandaloneBefore(any(), any())).thenReturn(0);

        ModerationPurgeService.PurgeExecutionSummary summary = purgeService.purgeExpiredEvidence(now);

        assertThat(summary.incidentsPurged()).isEqualTo(1);
        assertThat(summary.appealsPurged()).isEqualTo(1);
        assertThat(summary.resolutionsPurged()).isEqualTo(1);
        assertThat(summary.decisionsPurged()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(incidentRepository).purgeIncidentsByIds(captor.capture(), eq(now));
        assertThat(captor.getValue()).containsExactly(expiredId);

        verify(appealRepository).purgeAppealsByIncidentIds(captor.capture(), eq(now));
        verify(resolutionRepository).purgeResolutionsByIncidentIds(captor.capture());
        verify(decisionJdbcRepository).purgeByIncidentIds(captor.capture(), eq(now));
    }

    @Test
    @DisplayName("CA2 & Escenario 2 BDD: Incidente REVERSED con retención de 90 días no se purga a los 35 días pero sí a los 95")
    void reversedIncidentRespectsExtendedRetentionPeriod() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID reversedAt35Days = UUID.randomUUID();
        ModerationIncidentEntity reversed35 = new ModerationIncidentEntity(
                reversedAt35Days, "msg-002", "user-11", "curso-42", "REVERSED", "CODE_OBFUSCATION", "codigo falso positivo", now.minusDays(35)
        );

        // A los 35 días el cutoff para REVERSED es now.minusDays(90), por lo que NO debe ser retornado
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("REVERSED")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("CONFIRMED")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("BLOCK")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("PENDING_REVIEW")))
                .thenReturn(Collections.emptyList());

        ModerationPurgeService.PurgeExecutionSummary summary35 = purgeService.purgeExpiredEvidence(now);
        assertThat(summary35.incidentsPurged()).isZero();
        verify(incidentRepository, never()).purgeIncidentsByIds(any(), any());

        // A los 95 días sí vence
        UUID reversedAt95Days = UUID.randomUUID();
        ModerationIncidentEntity reversed95 = new ModerationIncidentEntity(
                reversedAt95Days, "msg-003", "user-12", "curso-42", "REVERSED", "CODE_OBFUSCATION", "codigo", now.minusDays(95)
        );
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("REVERSED")))
                .thenReturn(List.of(reversed95));
        when(incidentRepository.purgeIncidentsByIds(any(), any())).thenReturn(1);

        ModerationPurgeService.PurgeExecutionSummary summary95 = purgeService.purgeExpiredEvidence(now);
        assertThat(summary95.incidentsPurged()).isEqualTo(1);
        verify(incidentRepository, times(1)).purgeIncidentsByIds(any(), any());
    }

    @Test
    @DisplayName("CA_negativo_1: Incidentes vigentes (no vencidos) no son purgados aunque el job corra")
    void unexpiredIncidentsAreNotPurged() {
        OffsetDateTime now = OffsetDateTime.now();

        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), any()))
                .thenReturn(Collections.emptyList());
        when(decisionJdbcRepository.purgeStandaloneBefore(any(), any())).thenReturn(0);

        ModerationPurgeService.PurgeExecutionSummary summary = purgeService.purgeExpiredEvidence(now);

        assertThat(summary.incidentsPurged()).isZero();
        verify(incidentRepository, never()).purgeIncidentsByIds(any(), any());
        verify(appealRepository, never()).purgeAppealsByIncidentIds(any(), any());
        verify(resolutionRepository, never()).purgeResolutionsByIncidentIds(any());
        verify(decisionJdbcRepository, never()).purgeByIncidentIds(any(), any());
    }

    @Test
    @DisplayName("Sanitización de Logs: Ningún logger imprime texto de mensajes ni motivos purgados")
    void logSanitizationEnsuresNoTextLeaked() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID expiredId = UUID.randomUUID();
        String sensitiveText = "TEXTO_MUY_CONFIDENCIAL_Y_SENSIBLE_DEL_ALUMNO";
        ModerationIncidentEntity expiredIncident = new ModerationIncidentEntity(
                expiredId, "msg-004", "user-10", "curso-42", "CONFIRMED", "OFFENSIVE", sensitiveText, now.minusDays(40)
        );

        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("CONFIRMED")))
                .thenReturn(List.of(expiredIncident));
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("BLOCK")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("REVERSED")))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), eq("PENDING_REVIEW")))
                .thenReturn(Collections.emptyList());

        when(incidentRepository.purgeIncidentsByIds(any(), any())).thenReturn(1);

        purgeService.purgeExpiredEvidence(now);

        // Verificar logs generados
        List<ILoggingEvent> logsList = logAppender.list;
        assertThat(logsList).isNotEmpty();
        for (ILoggingEvent event : logsList) {
            assertThat(event.getFormattedMessage()).doesNotContain(sensitiveText);
            assertThat(event.getFormattedMessage()).contains("Moderation evidence purge completed");
        }
    }

    @Test
    @DisplayName("El delegado ModerationDataPurgeJob ejecuta correctamente")
    void moderationDataPurgeJobExecutesViaDelegate() {
        when(incidentRepository.findUnpurgedBefore(any(OffsetDateTime.class), any()))
                .thenReturn(Collections.emptyList());
        when(decisionJdbcRepository.purgeStandaloneBefore(any(), any())).thenReturn(0);

        dataPurgeJob.run();
        ModerationPurgeService.PurgeExecutionSummary summary = dataPurgeJob.purgeNow();

        assertThat(summary).isNotNull();
    }
}
