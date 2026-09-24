package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpamDetectorTest {

    private SpamDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SpamDetector(3, 200, 5);
    }

    @Test
    @DisplayName("T4 / CA3: Mensaje con alta densidad de URLs (>= 3 URLs distintas en < 200 caracteres) es FAIL con SPAM")
    void highUrlDensityDetectedAsSpam() {
        String spamMessage = "Ofertas: https://promo1.com y http://promo2.org o mira www.promo3.net aprovecha!";

        assertThat(spamMessage.length()).isLessThan(200);

        detector.detect(spamMessage); // calentamiento: la primera llamada compila regex y carga clases, y en CI superaba los 3 ms
        DetectionResult result = detector.detect(spamMessage);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(result.getDetectorName()).isEqualTo("spam");
        assertThat(result.getLatencyMs()).isLessThan(3L);
    }

    @Test
    @DisplayName("T4: Mensaje con 2 URLs en texto corto es tolerado (ALLOW)")
    void twoUrlsBelowThresholdNotSpam() {
        String text = "Consulta la doc en https://docs.spring.io y https://openjdk.org para dudas.";

        DetectionResult result = detector.detect(text);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T4: URLs repetidas (no distintas) no disparan falso positivo de densidad")
    void repeatedSameUrlDoesNotExceedDistinctThreshold() {
        String text = "Mira https://utn.edu.ar y otra vez https://utn.edu.ar en el sitio.";

        DetectionResult result = detector.detect(text);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T4: Repetición anormal de tokens (token flooding) es detectada como SPAM")
    void tokenFloodingDetectedAsSpam() {
        String spamMessage = "compre compre compre compre compre compre compre ahora mismo";

        DetectionResult result = detector.detect(spamMessage);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
    }

    @Test
    @DisplayName("T4: Inundación de caracteres repetidos (>= 15) es detectada como SPAM")
    void characterFloodingDetectedAsSpam() {
        String floodMessage = "Holaaaaaaaaaaaaaaaaaaaaa profe";

        DetectionResult result = detector.detect(floodMessage);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
    }

    @Test
    @DisplayName("T4: Mensaje legítimo de consulta académica pasa sin bloqueo")
    void cleanAcademicMessagePasses() {
        String message = "Buenas tardes profesor, quisiera consultar si la clase del viernes se graba y sube al aula virtual.";

        DetectionResult result = detector.detect(message);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
    }

    @Test
    @DisplayName("T4: Rendimiento objetivo < 3 ms")
    void performanceLessThan3Ms() {
        String spamMessage = "Ofertas: https://promo1.com y http://promo2.org o mira www.promo3.net!";

        // Warmup
        for (int i = 0; i < 20; i++) {
            detector.detect(spamMessage);
        }

        long start = System.currentTimeMillis();
        DetectionResult result = detector.detect(spamMessage);
        long latency = System.currentTimeMillis() - start;

        assertThat(latency).isLessThan(3L);
        assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DisplayName("T4: Manejo de nulo y cadenas vacías")
    void nullAndBlankHandling() {
        assertThat(detector.detect(null, "c1").isPassed()).isTrue();
        assertThat(detector.detect("   ", "c1").isPassed()).isTrue();
    }
}
