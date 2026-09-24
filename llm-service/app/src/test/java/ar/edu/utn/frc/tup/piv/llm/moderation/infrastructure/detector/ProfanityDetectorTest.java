package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityDetectorTest {

    private ProfanityDetector detector;
    private TextNormalizationPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new TextNormalizationPipeline();
        detector = new ProfanityDetector(pipeline, 0.70);
    }

    @Test
    @DisplayName("T2: Término ofensivo directo es detectado con FAIL y reason_code OFFENSIVE")
    void directProfanityDetected() {
        DetectionResult result = detector.detect("Sos un pelotudo importante");

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
        assertThat(result.getDetectorName()).isEqualTo("offensive");
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0.70);
        assertThat(result.getLatencyMs()).isLessThan(10L);
    }

    @Test
    @DisplayName("T2: Frase compuesta de alta gravedad es detectada con FAIL")
    void compoundOffensivePhraseDetected() {
        DetectionResult result = detector.detect("Andate a la concha de tu madre");

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0.70);
    }

    @Test
    @DisplayName("T2: Evasión mediante leet speak y tildes es detectada gracias al pipeline")
    void leetSpeakEvasionDetected() {
        DetectionResult resultLeet = detector.detect("Sos un p3l0tud0 total");
        assertThat(resultLeet.isFailed()).isTrue();
        assertThat(resultLeet.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);

        DetectionResult resultDotted = detector.detect("Sos un p.e.l.o.t.u.d.o");
        assertThat(resultDotted.isFailed()).isTrue();
        assertThat(resultDotted.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
    }

    @Test
    @DisplayName("T2: Problema de Scunthorpe evitado — palabras con subcadenas no disparan falso positivo")
    void scunthorpeProblemAvoided() {
        // 'cálculo' contiene 'culo', 'putativo' contiene 'puta'
        DetectionResult resultCalculo = detector.detect("El cálculo del límite da infinito");
        assertThat(resultCalculo.isPassed()).isTrue();

        DetectionResult resultPutativo = detector.detect("El padre putativo compareció ante el tribunal");
        assertThat(resultPutativo.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T2: Término común con score calibrado bajo no dispara solo (ej. 'concha' score 0.062)")
    void lowScorePolysemicWordDoesNotTriggerAlone() {
        DetectionResult result = detector.detect("Encontramos una concha en la orilla de la playa");
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T2: Umbral configurable por curso permite ajustar tolerancia")
    void courseConfigurableThreshold() {
        // 'boludo' tiene score 0.417 en ModernMT. Con umbral por defecto 0.70 pasa:
        DetectionResult defaultCourse = detector.detect("Che boludo, ¿vamos a almorzar?", "curso-standard");
        assertThat(defaultCourse.isPassed()).isTrue();

        // Si un curso configura política estricta con umbral 0.40:
        detector.setCourseThreshold("curso-estricto", 0.40);
        DetectionResult strictCourse = detector.detect("Che boludo, ¿vamos a almorzar?", "curso-estricto");
        assertThat(strictCourse.isFailed()).isTrue();
        assertThat(strictCourse.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
    }

    @Test
    @DisplayName("T2: Confidencialidad de reglas — no se filtra la palabra clave encontrada (CA5)")
    void rulesConfidentialityNoKeywordLeakage() {
        DetectionResult result = detector.detect("Sos un pelotudo");

        assertThat(result.toString()).doesNotContain("pelotudo");
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
    }

    @Test
    @DisplayName("T2: Rendimiento objetivo < 10 ms")
    void performanceLessThan10Ms() {
        String message = "Este es un mensaje académico normal sobre la estructura de datos en Java.";

        // Warmup
        for (int i = 0; i < 20; i++) {
            detector.detect(message);
        }

        long start = System.currentTimeMillis();
        DetectionResult result = detector.detect(message);
        long latency = System.currentTimeMillis() - start;

        assertThat(latency).isLessThan(10L);
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T2: Manejo de nulo, blanco y constructores/diccionarios custom")
    void nullBlankAndEdgeCases() {
        assertThat(detector.detect(null).isPassed()).isTrue();
        assertThat(detector.detect("   ").isPassed()).isTrue();
        detector.setCourseThreshold(null, 0.5);
        detector.setCourseThreshold("", 0.5);

        ProfanityDetector defaultDet = new ProfanityDetector(pipeline);
        assertThat(defaultDet.detect("hola").isPassed()).isTrue();

        ProfanityDetector customDet = new ProfanityDetector(pipeline, 0.6, Map.of("c1", 0.4), Map.of("insultocustom", 0.9));
        assertThat(customDet.detect("insultocustom").isFailed()).isTrue();
    }
}
