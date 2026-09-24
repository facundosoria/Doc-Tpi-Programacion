package ar.edu.utn.frc.tup.piv.llm.moderation;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.ApiExceptionHandler;
import ar.edu.utn.frc.tup.piv.llm.configuration.GatewayIdentityFilter;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.ModerationCourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.ModerationIncidentQueryController;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationDataPurgeJob;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationEvidencePurgeJob;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationIncidentQueryService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationMetricsService;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationPurgeService;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationDecisionJdbcRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationIncidentEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationRetentionPolicyEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationAppealRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationIncidentRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationResolutionRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationRetentionPolicyRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de integración para la verificación del ciclo de retención y purga (LLM-S13-H02 / T4).
 * Valida de punta a punta:
 * - Creación de incidentes viejos (vencidos) y recientes (vigentes).
 * - Ejecución del servicio / job de purga programada.
 * - Destrucción irreversible a NULL de los textos de incidentes viejos.
 * - Preservación intacta de los textos de incidentes recientes.
 * - Preservación idéntica de las métricas estadísticas agregadas.
 */
class ModerationDataPurgeIT {

    private MockMvc mvc;

    private InMemoryIncidentStore incidentStore;
    private InMemoryAppealStore appealStore;
    private InMemoryResolutionStore resolutionStore;
    private InMemoryDecisionStore decisionStore;

    private ModerationPurgeService purgeService;
    private ModerationDataPurgeJob dataPurgeJob;
    private ModerationMetricsService metricsService;
    private ModerationIncidentQueryService queryService;

    @BeforeEach
    void setUp() {
        incidentStore = new InMemoryIncidentStore();
        appealStore = new InMemoryAppealStore();
        resolutionStore = new InMemoryResolutionStore();
        decisionStore = new InMemoryDecisionStore();

        SpringDataModerationRetentionPolicyRepository policyRepository = mock(SpringDataModerationRetentionPolicyRepository.class);
        when(policyRepository.findByIncidentType("BLOCK_CONFIRMED"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "BLOCK_CONFIRMED", "DEFAULT", 30, OffsetDateTime.now(), "SYSTEM", 1L)));
        when(policyRepository.findByIncidentType("BLOCK_REVERSED"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "BLOCK_REVERSED", "DEFAULT", 90, OffsetDateTime.now(), "SYSTEM", 1L)));
        when(policyRepository.findByIncidentType("PENDING_REVIEW"))
                .thenReturn(Optional.of(new ModerationRetentionPolicyEntity(UUID.randomUUID(), "PENDING_REVIEW", "DEFAULT", 90, OffsetDateTime.now(), "SYSTEM", 1L)));

        purgeService = new ModerationPurgeService(
                incidentStore.springDataRepository(),
                appealStore.springDataRepository(),
                resolutionStore.springDataRepository(),
                decisionStore.decisionJdbcRepository(),
                policyRepository
        );

        ModerationEvidencePurgeJob purgeJob = new ModerationEvidencePurgeJob(purgeService);
        dataPurgeJob = new ModerationDataPurgeJob(purgeJob, purgeService);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        wireMetricsMock(jdbcTemplate);
        metricsService = new ModerationMetricsService(jdbcTemplate);

        queryService = new ModerationIncidentQueryService(
                incidentStore.domainRepository(),
                appealStore.domainRepository(),
                resolutionStore.domainRepository()
        );

        ModerationCourseAuthorization authorization = new ModerationCourseAuthorization();
        ModerationIncidentQueryController controller = new ModerationIncidentQueryController(authorization, queryService);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new GatewayIdentityFilter())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T4 & GDPR: Ciclo completo de retención, purga destructiva y preservación de métricas")
    void fullRetentionAndPurgeLifecycle() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Crear incidente VIEJO (>30 días) confirmado
        UUID oldIncidentId = UUID.randomUUID();
        OffsetDateTime oldCreatedAt = now.minusDays(40);
        String oldSensitiveText = "Mensaje spam ofensivo viejo: comprar viagra en http://spam.xyz";
        ModerationIncident oldIncident = new ModerationIncident(
                oldIncidentId, "msg-old-1", "user-100", "curso-42", "CONFIRMED", "SPAM", oldSensitiveText, oldCreatedAt, null
        );
        incidentStore.save(oldIncident);
        appealStore.save(new ModerationAppeal(UUID.randomUUID(), oldIncidentId, "user-100", "Motivo de apelación libre con datos personales...", ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus.CONFIRMED, oldCreatedAt, oldCreatedAt));
        resolutionStore.save(new ModerationResolution(UUID.randomUUID(), oldIncidentId, "prof-1", ModerationResolutionType.CONFIRMED, "Resolución docente confirmando el bloqueo por spam", oldCreatedAt));
        decisionStore.save("msg-old-1", oldIncidentId, oldSensitiveText, oldCreatedAt);

        // 2. Crear incidente RECIENTE (<30 días)
        UUID recentIncidentId = UUID.randomUUID();
        OffsetDateTime recentCreatedAt = now.minusDays(5);
        String recentSensitiveText = "Mensaje reciente bloqueado por lenguaje ofensivo";
        ModerationIncident recentIncident = new ModerationIncident(
                recentIncidentId, "msg-rec-1", "user-101", "curso-42", "CONFIRMED", "OFFENSIVE", recentSensitiveText, recentCreatedAt, null
        );
        incidentStore.save(recentIncident);
        decisionStore.save("msg-rec-1", recentIncidentId, recentSensitiveText, recentCreatedAt);

        // 3. Verificar estado PRE-PURGA: ambos tienen sus textos legibles
        ModerationIncident beforeOld = incidentStore.findById(oldIncidentId).orElseThrow();
        assertThat(beforeOld.getMessagePreview()).isEqualTo(oldSensitiveText);
        assertThat(beforeOld.isPurged()).isFalse();

        ModerationIncident beforeRecent = incidentStore.findById(recentIncidentId).orElseThrow();
        assertThat(beforeRecent.getMessagePreview()).isEqualTo(recentSensitiveText);
        assertThat(beforeRecent.isPurged()).isFalse();

        ModerationMetricsService.ModerationMetricsSummary metricsBefore = metricsService.getCourseMetrics("curso-42");
        assertThat(metricsBefore.totalIncidents()).isEqualTo(2L);

        // 4. Ejecutar el job de purga
        dataPurgeJob.run();

        // 5. Verificar estado POST-PURGA:
        // Registro viejo destruido de forma irreversible
        ModerationIncident afterOld = incidentStore.findById(oldIncidentId).orElseThrow();
        assertThat(afterOld.getMessagePreview()).isNull();
        assertThat(afterOld.isPurged()).isTrue();
        assertThat(afterOld.getPurgedAt()).isNotNull();

        assertThat(appealStore.getAppealReason(oldIncidentId)).isNull();
        assertThat(resolutionStore.getResolutionReason(oldIncidentId)).isNull();
        assertThat(decisionStore.getMessageText(oldIncidentId)).isNull();

        // Registro reciente permanece completamente INTACTO
        ModerationIncident afterRecent = incidentStore.findById(recentIncidentId).orElseThrow();
        assertThat(afterRecent.getMessagePreview()).isEqualTo(recentSensitiveText);
        assertThat(afterRecent.isPurged()).isFalse();
        assertThat(afterRecent.getPurgedAt()).isNull();
        assertThat(decisionStore.getMessageText(recentIncidentId)).isEqualTo(recentSensitiveText);

        // 6. Verificar consulta de API: el incidente viejo devuelve explícitamente content='PURGED'
        mvc.perform(get("/moderation/v1/incidents/" + oldIncidentId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-1")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id", is(oldIncidentId.toString())))
                .andExpect(jsonPath("$.content", is("PURGED")))
                .andExpect(jsonPath("$.purged_at", notNullValue()))
                .andExpect(jsonPath("$.reason_code", is("SPAM")))
                .andExpect(jsonPath("$.resolution", is("CONFIRMED")));

        // El incidente reciente devuelve el texto intacto
        mvc.perform(get("/moderation/v1/incidents/" + recentIncidentId)
                        .header(IdentityHeaders.PRINCIPAL_TYPE, "user")
                        .header(IdentityHeaders.USER_ID, "prof-1")
                        .header(IdentityHeaders.USER_ROLES, "TEACHER")
                        .header("X-Teacher-Course-Ids", "curso-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id", is(recentIncidentId.toString())))
                .andExpect(jsonPath("$.content", is(recentSensitiveText)))
                .andExpect(jsonPath("$.purged_at", nullValue()));

        // 7. Verificar métricas agregadas post-purga: 100% idénticas
        ModerationMetricsService.ModerationMetricsSummary metricsAfter = metricsService.getCourseMetrics("curso-42");
        assertThat(metricsAfter.totalIncidents()).isEqualTo(metricsBefore.totalIncidents());
        assertThat(metricsAfter.incidentsByReason()).isEqualTo(metricsBefore.incidentsByReason());
        assertThat(metricsAfter.confirmedResolutions()).isEqualTo(metricsBefore.confirmedResolutions());
    }

    private void wireMetricsMock(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_incidents"), eq(Long.class), any(Object[].class)))
                .thenAnswer(inv -> (long) incidentStore.count());

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            for (var entry : incidentStore.countsByReason().entrySet()) {
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("reason")).thenReturn(entry.getKey());
                when(rs.getLong("cnt")).thenReturn(entry.getValue());
                rch.processRow(rs);
            }
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY i.reason_code"), any(RowCallbackHandler.class), any(Object[].class));

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            for (var entry : incidentStore.countsByStatus().entrySet()) {
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("status")).thenReturn(entry.getKey());
                when(rs.getLong("cnt")).thenReturn(entry.getValue());
                rch.processRow(rs);
            }
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY i.status"), any(RowCallbackHandler.class), any(Object[].class));

        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM llm.moderation_appeals"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
            when(rs.getString("resolution")).thenReturn("CONFIRMED");
            when(rs.getLong("cnt")).thenReturn(1L);
            rch.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(contains("GROUP BY r.resolution"), any(RowCallbackHandler.class), any(Object[].class));
    }

    // Adaptadores en memoria fieles a los contratos de repositorio para test de integración
    private static class InMemoryIncidentStore {
        private final ConcurrentHashMap<UUID, ModerationIncidentEntity> map = new ConcurrentHashMap<>();

        public void save(ModerationIncident inc) {
            map.put(inc.getId(), new ModerationIncidentEntity(
                    inc.getId(), inc.getMessageId(), inc.getUserId(), inc.getCourseId(),
                    inc.getStatus(), inc.getReasonCode(), inc.getMessagePreview(),
                    inc.getCreatedAt(), inc.getPurgedAt()
            ));
        }

        public Optional<ModerationIncident> findById(UUID id) {
            ModerationIncidentEntity e = map.get(id);
            if (e == null) return Optional.empty();
            return Optional.of(new ModerationIncident(
                    e.getId(), e.getMessageId(), e.getUserId(), e.getCourseId(),
                    e.getStatus(), e.getReasonCode(), e.getMessagePreview(),
                    e.getCreatedAt(), e.getPurgedAt()
            ));
        }

        public int count() {
            return map.size();
        }

        public java.util.Map<String, Long> countsByReason() {
            java.util.Map<String, Long> res = new java.util.HashMap<>();
            map.values().forEach(e -> res.merge(e.getReasonCode(), 1L, Long::sum));
            return res;
        }

        public java.util.Map<String, Long> countsByStatus() {
            java.util.Map<String, Long> res = new java.util.HashMap<>();
            map.values().forEach(e -> res.merge(e.getStatus(), 1L, Long::sum));
            return res;
        }

        public SpringDataModerationIncidentRepository springDataRepository() {
            SpringDataModerationIncidentRepository repo = mock(SpringDataModerationIncidentRepository.class);
            when(repo.findUnpurgedBefore(any(OffsetDateTime.class), any())).thenAnswer(inv -> {
                OffsetDateTime cutoff = inv.getArgument(0);
                String status = inv.getArgument(1);
                return map.values().stream()
                        .filter(e -> e.getPurgedAt() == null)
                        .filter(e -> e.getCreatedAt().isBefore(cutoff) || e.getCreatedAt().isEqual(cutoff))
                        .filter(e -> status == null || e.getStatus().equalsIgnoreCase(status))
                        .toList();
            });

            when(repo.purgeIncidentsByIds(any(), any())).thenAnswer(inv -> {
                Collection<UUID> ids = inv.getArgument(0);
                OffsetDateTime purgedAt = inv.getArgument(1);
                int count = 0;
                for (UUID id : ids) {
                    ModerationIncidentEntity e = map.get(id);
                    if (e != null) {
                        e.setMessagePreview(null);
                        e.setPurgedAt(purgedAt);
                        count++;
                    }
                }
                return count;
            });

            return repo;
        }

        public ModerationIncidentRepositoryPort domainRepository() {
            return new ModerationIncidentRepositoryPort() {
                @Override
                public Optional<ModerationIncident> findById(UUID id) {
                    return InMemoryIncidentStore.this.findById(id);
                }

                @Override
                public ModerationIncident save(ModerationIncident incident) {
                    InMemoryIncidentStore.this.save(incident);
                    return incident;
                }

                @Override
                public List<ModerationIncident> findByCourseIdAndStatus(String courseId, String status, int offset, int limit) {
                    return map.values().stream()
                            .filter(e -> e.getCourseId().equals(courseId))
                            .map(e -> new ModerationIncident(e.getId(), e.getMessageId(), e.getUserId(), e.getCourseId(), e.getStatus(), e.getReasonCode(), e.getMessagePreview(), e.getCreatedAt(), e.getPurgedAt()))
                            .toList();
                }

                @Override
                public long countByCourseIdAndStatus(String courseId, String status) {
                    return map.values().stream().filter(e -> e.getCourseId().equals(courseId)).count();
                }
            };
        }
    }

    private static class InMemoryAppealStore {
        private final java.util.Map<UUID, String> reasonsByIncident = java.util.Collections.synchronizedMap(new java.util.HashMap<>());
        private final java.util.Map<UUID, ModerationAppeal> appeals = java.util.Collections.synchronizedMap(new java.util.HashMap<>());

        public void save(ModerationAppeal appeal) {
            appeals.put(appeal.getIncidentId(), appeal);
            reasonsByIncident.put(appeal.getIncidentId(), appeal.getAppealReason());
        }

        public String getAppealReason(UUID incidentId) {
            return reasonsByIncident.get(incidentId);
        }

        public SpringDataModerationAppealRepository springDataRepository() {
            SpringDataModerationAppealRepository repo = mock(SpringDataModerationAppealRepository.class);
            when(repo.purgeAppealsByIncidentIds(any(), any())).thenAnswer(inv -> {
                Collection<UUID> ids = inv.getArgument(0);
                int count = 0;
                for (UUID id : ids) {
                    if (reasonsByIncident.containsKey(id)) {
                        reasonsByIncident.put(id, null);
                        count++;
                    }
                }
                return count;
            });
            return repo;
        }

        public ModerationAppealRepositoryPort domainRepository() {
            return new ModerationAppealRepositoryPort() {
                @Override
                public ModerationAppeal save(ModerationAppeal appeal) {
                    InMemoryAppealStore.this.save(appeal);
                    return appeal;
                }

                @Override
                public Optional<ModerationAppeal> findById(UUID id) {
                    return appeals.values().stream().filter(a -> a.getId().equals(id)).findFirst();
                }

                @Override
                public Optional<ModerationAppeal> findByIncidentId(UUID incidentId) {
                    ModerationAppeal app = appeals.get(incidentId);
                    if (app == null) return Optional.empty();
                    String reason = reasonsByIncident.get(incidentId);
                    return Optional.of(new ModerationAppeal(app.getId(), app.getIncidentId(), app.getUserId(), reason, app.getStatus(), app.getCreatedAt(), app.getUpdatedAt()));
                }

                @Override
                public boolean existsByIncidentId(UUID incidentId) {
                    return appeals.containsKey(incidentId);
                }
            };
        }
    }

    private static class InMemoryResolutionStore {
        private final java.util.Map<UUID, String> reasonsByIncident = java.util.Collections.synchronizedMap(new java.util.HashMap<>());
        private final java.util.Map<UUID, ModerationResolution> resolutions = java.util.Collections.synchronizedMap(new java.util.HashMap<>());

        public void save(ModerationResolution resolution) {
            resolutions.put(resolution.getIncidentId(), resolution);
            reasonsByIncident.put(resolution.getIncidentId(), resolution.getResolutionReason());
        }

        public String getResolutionReason(UUID incidentId) {
            return reasonsByIncident.get(incidentId);
        }

        public SpringDataModerationResolutionRepository springDataRepository() {
            SpringDataModerationResolutionRepository repo = mock(SpringDataModerationResolutionRepository.class);
            when(repo.purgeResolutionsByIncidentIds(any())).thenAnswer(inv -> {
                Collection<UUID> ids = inv.getArgument(0);
                int count = 0;
                for (UUID id : ids) {
                    if (reasonsByIncident.containsKey(id)) {
                        reasonsByIncident.put(id, null);
                        count++;
                    }
                }
                return count;
            });
            return repo;
        }

        public ModerationResolutionRepositoryPort domainRepository() {
            return new ModerationResolutionRepositoryPort() {
                @Override
                public ModerationResolution save(ModerationResolution resolution) {
                    InMemoryResolutionStore.this.save(resolution);
                    return resolution;
                }

                @Override
                public Optional<ModerationResolution> findByIncidentId(UUID incidentId) {
                    ModerationResolution res = resolutions.get(incidentId);
                    if (res == null) return Optional.empty();
                    String reason = reasonsByIncident.get(incidentId);
                    return Optional.of(new ModerationResolution(res.getId(), res.getIncidentId(), res.getResolvedBy(), res.getResolution(), reason, res.getResolvedAt()));
                }

                @Override
                public boolean existsByIncidentId(UUID incidentId) {
                    return resolutions.containsKey(incidentId);
                }
            };
        }
    }

    private static class InMemoryDecisionStore {
        private final java.util.Map<UUID, String> textsByIncident = java.util.Collections.synchronizedMap(new java.util.HashMap<>());

        public void save(String messageId, UUID incidentId, String text, OffsetDateTime createdAt) {
            if (incidentId != null) {
                textsByIncident.put(incidentId, text);
            }
        }

        public String getMessageText(UUID incidentId) {
            return textsByIncident.get(incidentId);
        }

        public ModerationDecisionJdbcRepository decisionJdbcRepository() {
            ModerationDecisionJdbcRepository repo = mock(ModerationDecisionJdbcRepository.class);
            when(repo.purgeByIncidentIds(any(), any())).thenAnswer(inv -> {
                Collection<UUID> ids = inv.getArgument(0);
                int count = 0;
                for (UUID id : ids) {
                    if (textsByIncident.containsKey(id)) {
                        textsByIncident.put(id, null);
                        count++;
                    }
                }
                return count;
            });
            when(repo.purgeStandaloneBefore(any(), any())).thenReturn(0);
            return repo;
        }
    }
}
