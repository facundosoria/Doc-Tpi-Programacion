package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import java.util.Optional;

/**
 * Puerto de salida de dominio para consulta por message_id y persistencia de auditoría inmutable.
 */
public interface ModerationDecisionRepositoryPort {

    /**
     * Busca una decisión previa por message_id para control de idempotencia.
     */
    Optional<ModerationDecision> findByMessageId(String messageId);

    /**
     * Persiste el registro de decisión de moderación aplicando la regla de minimización de datos.
     * Si la decisión es ALLOW, messageText debe ser null en el almacén de persistencia.
     */
    void save(ModerationDecision decision, String courseId, String senderRole, String messageText);
}
