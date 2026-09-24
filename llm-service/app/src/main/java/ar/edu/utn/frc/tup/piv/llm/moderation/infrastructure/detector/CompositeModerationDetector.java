package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.MessageContent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.DeterministicModerationPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orquestador Composite con patrón Strategy y corte temprano (T5):
 * - Ejecuta detectores deterministas en secuencia: SpamDetector -> CodeObfuscationDetector -> ProfanityDetector.
 * - Corte temprano (early exit) en la primera falla grave: si un detector emite FAIL, detiene inmediatamente
 *   la evaluación y genera la decisión BLOCK correspondiente.
 * - Confidencialidad estricta de reglas (CA5 / CA_negativo_1): la decisión resultante nunca contiene expresiones
 *   regulares, pesos, ni detalles internos de reglas.
 * - Rendimiento objetivo: p99 < 50 ms en memoria sin dependencias de red externa.
 */
@Component
public class CompositeModerationDetector implements DeterministicModerationPort {

    private static final Logger log = LoggerFactory.getLogger(CompositeModerationDetector.class);
    private static final String CLASSIFIER_NAME = "deterministic";

    private final List<ModerationDetectorPort> detectors;

    @org.springframework.beans.factory.annotation.Autowired
    public CompositeModerationDetector(
            SpamDetector spamDetector,
            CodeObfuscationDetector obfuscationDetector,
            ProfanityDetector profanityDetector) {
        this.detectors = List.of(spamDetector, obfuscationDetector, profanityDetector);
    }

    public CompositeModerationDetector(List<ModerationDetectorPort> customDetectors) {
        this.detectors = List.copyOf(customDetectors);
    }

    @Override
    public ModerationDecision evaluate(String messageId, String text, String courseId) {
        long startTime = System.currentTimeMillis();
        MessageContent content = new MessageContent(text);
        String contentHash = content.getContentHash();

        for (ModerationDetectorPort detector : detectors) {
            DetectionResult result = detector.detect(text, courseId);

            if (result.isFailed()) {
                long totalLatency = Math.max(1, System.currentTimeMillis() - startTime);
                log.info("Mensaje '{}' bloqueado por detector '{}' con reason_code='{}' en {} ms",
                        messageId, detector.getName(), result.getReasonCode(), totalLatency);

                // Corte temprano: emitir BLOCK con reason_code genérico y sin detalles internos (CA5 / CA_negativo_1)
                return ModerationDecision.block(
                        messageId,
                        result.getReasonCode(),
                        CLASSIFIER_NAME,
                        totalLatency,
                        contentHash,
                        UUID.randomUUID()
                );
            }
        }

        long totalLatency = Math.max(1, System.currentTimeMillis() - startTime);
        return ModerationDecision.allow(
                messageId,
                ModerationReasonCode.CLEAN,
                CLASSIFIER_NAME,
                totalLatency,
                contentHash
        );
    }

    public List<ModerationDetectorPort> getDetectors() {
        return detectors;
    }
}
