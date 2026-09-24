package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Motor determinista de moderación (T5 - Evidencia de tarea SMART):
 * Implementación principal del puerto de dominio DeterministicModerationPort delegando
 * en CompositeModerationDetector con política de corte temprano y seguridad anti-filtrado de reglas.
 */
@Component
@Primary
public class DeterministicModerationEngine implements DeterministicModerationPort {

    private final CompositeModerationDetector compositeDetector;

    public DeterministicModerationEngine(CompositeModerationDetector compositeDetector) {
        this.compositeDetector = compositeDetector;
    }

    @Override
    public ModerationDecision evaluate(String messageId, String text, String courseId) {
        return compositeDetector.evaluate(messageId, text, courseId);
    }

    public CompositeModerationDetector getCompositeDetector() {
        return compositeDetector;
    }
}
