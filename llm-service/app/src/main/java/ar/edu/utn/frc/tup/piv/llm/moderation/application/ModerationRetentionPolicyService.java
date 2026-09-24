package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationRetentionPolicy;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationRetentionPolicyEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationRetentionPolicyRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación para consultar y actualizar políticas de retención versionadas (LLM-S13-H02 / T1 / CA4).
 */
@Service
public class ModerationRetentionPolicyService {

    private final SpringDataModerationRetentionPolicyRepository repository;

    public ModerationRetentionPolicyService(SpringDataModerationRetentionPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getPolicies() {
        List<ModerationRetentionPolicyEntity> all = repository.findAll();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ModerationRetentionPolicyEntity entity : all) {
            result.put(entity.getIncidentType(), entity.getRetentionDays());
        }
        return result;
    }

    @Transactional
    public Map<String, Integer> updatePolicies(Map<String, Object> payload, String updatedBy) {
        if (payload == null || payload.isEmpty()) {
            return getPolicies();
        }

        String actor = (updatedBy != null && !updatedBy.isBlank()) ? updatedBy.trim() : "ADMIN";
        OffsetDateTime now = OffsetDateTime.now();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String incidentType = entry.getKey().trim();
            int days = extractDays(entry.getValue());
            if (days < 1) {
                continue;
            }

            var existingOpt = repository.findByIncidentTypeAndSeverity(incidentType, "DEFAULT");
            if (existingOpt.isPresent()) {
                ModerationRetentionPolicyEntity entity = existingOpt.get();
                entity.setRetentionDays(days);
                entity.setUpdatedAt(now);
                entity.setUpdatedBy(actor);
                repository.save(entity);
            } else {
                ModerationRetentionPolicyEntity newEntity = new ModerationRetentionPolicyEntity(
                        UUID.randomUUID(),
                        incidentType,
                        "DEFAULT",
                        days,
                        now,
                        actor,
                        1L
                );
                repository.save(newEntity);
            }
        }

        return getPolicies();
    }

    private int extractDays(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof Map<?, ?> map) {
            Object daysVal = map.get("retentionDays");
            if (daysVal == null) {
                daysVal = map.get("retention_days");
            }
            if (daysVal == null) {
                daysVal = map.get("days");
            }
            if (daysVal instanceof Number num) {
                return num.intValue();
            }
            if (daysVal != null) {
                try {
                    return Integer.parseInt(daysVal.toString().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }
}
