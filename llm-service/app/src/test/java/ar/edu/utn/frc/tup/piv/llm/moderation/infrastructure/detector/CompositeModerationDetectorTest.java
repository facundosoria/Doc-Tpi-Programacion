package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompositeModerationDetectorTest {

    private TextNormalizationPipeline pipeline;
    private SpamDetector spamDetector;
    private CodeObfuscationDetector obfuscationDetector;
    private ProfanityDetector profanityDetector;
    private CompositeModerationDetector compositeDetector;
    private DeterministicModerationEngine engine;

    @BeforeEach
    void setUp() {
        pipeline = new TextNormalizationPipeline();
        spamDetector = new SpamDetector();
        obfuscationDetector = new CodeObfuscationDetector();
        profanityDetector = new ProfanityDetector(pipeline, 0.70);
        compositeDetector = new CompositeModerationDetector(spamDetector, obfuscationDetector, profanityDetector);
        engine = new DeterministicModerationEngine(compositeDetector);
    }

    @Test
    @DisplayName("T5 / Estrategia: Corte temprano — si el primer detector falla, los siguientes no se ejecutan")
    void earlyExitOnFirstFailure() {
        ModerationDetectorPort detector1 = mock(ModerationDetectorPort.class);
        ModerationDetectorPort detector2 = mock(ModerationDetectorPort.class);
        ModerationDetectorPort detector3 = mock(ModerationDetectorPort.class);

        when(detector1.getName()).thenReturn("mock1");
        when(detector1.detect(any(), any())).thenReturn(DetectionResult.fail("mock1", ModerationReasonCode.SPAM, 1.0, 1L));

        CompositeModerationDetector customComposite = new CompositeModerationDetector(List.of(detector1, detector2, detector3));

        ModerationDecision decision = customComposite.evaluate("msg-cut", "texto spam", "curso-1");

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);

        verify(detector1, times(1)).detect(any(), any());
        verify(detector2, never()).detect(any(), any());
        verify(detector3, never()).detect(any(), any());
    }

    @Test
    @DisplayName("T5: Mensaje limpio resulta en ALLOW con reason_code CLEAN y clasificador deterministic")
    void cleanMessageYieldsAllow() {
        String cleanMessage = "Hola profesor, tengo una consulta sobre el Trabajo Práctico de Base de Datos.";

        ModerationDecision decision = engine.evaluate("msg-clean", cleanMessage, "curso-1");

        assertThat(decision.getMessageId()).isEqualTo("msg-clean");
        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNull();
        assertThat(decision.getLatencyMs()).isLessThan(50L);
    }

    @Test
    @DisplayName("T5 / BDD Escenario 1: Código en Base64 bloqueado con BLOCK y CODE_OBFUSCATION")
    void obfuscatedCodeBlocked() {
        String base64Code = "dmFyIHggPSAxOyBjb25zb2xlLmxvZyh4KTs=";

        ModerationDecision decision = engine.evaluate("msg-007", base64Code, "curso-1");

        assertThat(decision.getMessageId()).isEqualTo("msg-007");
        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CODE_OBFUSCATION);
        assertThat(decision.getClassifierUsed()).isEqualTo("deterministic");
        assertThat(decision.getIncidentId()).isNotNull();
        assertThat(decision.getLatencyMs()).isLessThan(50L);
    }

    @Test
    @DisplayName("T5 / BDD Escenario 2 / CA_negativo_2: Texto educativo sobre Base64 no es bloqueado")
    void educationalTextAllowed() {
        String educational = "Base64 es una codificación que transforma datos binarios en texto ASCII; se usa en adjuntos de email";

        ModerationDecision decision = engine.evaluate("msg-edu", educational, "curso-1");

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.ALLOW);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
        assertThat(decision.getIncidentId()).isNull();
    }

    @Test
    @DisplayName("T5: Spam de URLs bloqueado con BLOCK y SPAM")
    void urlSpamBlocked() {
        String spam = "Link1 https://a.com link2 https://b.com link3 https://c.com oferta!";

        ModerationDecision decision = engine.evaluate("msg-spam", spam, "curso-1");

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.SPAM);
        assertThat(decision.getIncidentId()).isNotNull();
    }

    @Test
    @DisplayName("T5: Contenido ofensivo bloqueado con BLOCK y OFFENSIVE")
    void offensiveContentBlocked() {
        String offensive = "Sos un pelotudo";

        ModerationDecision decision = engine.evaluate("msg-off", offensive, "curso-1");

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        assertThat(decision.getReasonCode()).isEqualTo(ModerationReasonCode.OFFENSIVE);
        assertThat(decision.getIncidentId()).isNotNull();
    }

    @Test
    @DisplayName("T5 / CA5 / CA_negativo_1 / BDD Escenario 3: Confidencialidad total — nunca filtra regexes ni reglas")
    void rulesConfidentialityGuaranteed() {
        ModerationDecision decision = engine.evaluate("msg-sec", "https://site1.com https://site2.com https://site3.com spam", "curso-1");

        assertThat(decision.getDecision()).isEqualTo(ModerationDecisionEnum.BLOCK);
        String serializedString = decision.toString();

        // Verificar que no existen detalles de reglas ni regexes en la estructura
        assertThat(serializedString).doesNotContain("regex");
        assertThat(serializedString).doesNotContain("pattern");
        assertThat(serializedString).doesNotContain("rule_id");
        assertThat(serializedString).doesNotContain("threshold");
        assertThat(serializedString).doesNotContain("dictionary");
    }

    @Test
    @DisplayName("T5 / CA1: Certificación de rendimiento en memoria p99 < 50 ms")
    void performanceP99LessThan50Ms() {
        String[] sampleTexts = new String[]{
                "Hola profesor, una duda sobre la clase de hoy",
                "dmFyIHggPSAxOyBjb25zb2xlLmxvZyh4KTs=",
                "Mira https://u1.com y https://u2.com con https://u3.com",
                "Sos un p3l0tud0 tremendo",
                "El cálculo numérico de la derivada es exacto",
                "Base64 es una codificación que transforma datos binarios en texto ASCII; se usa en adjuntos de email"
        };

        // Warmup JIT
        for (int i = 0; i < 50; i++) {
            engine.evaluate("msg-warm", sampleTexts[i % sampleTexts.length], "curso-1");
        }

        List<Long> latencies = new ArrayList<>();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            String text = sampleTexts[i % sampleTexts.length];
            long start = System.nanoTime();
            engine.evaluate("msg-" + i, text, "curso-1");
            long elapsedNanos = System.nanoTime() - start;
            latencies.add(elapsedNanos / 1_000_000);
        }

        Collections.sort(latencies);
        int p99Index = (int) Math.ceil(99.0 / 100.0 * latencies.size()) - 1;
        long p99Latency = latencies.get(p99Index);

        assertThat(p99Latency)
                .withFailMessage("Latencia p99 (%d ms) excede el umbral de 50 ms", p99Latency)
                .isLessThan(50L);
    }

    @Test
    void gettersReturnConfiguredDetectors() {
        assertThat(engine.getCompositeDetector()).isSameAs(compositeDetector);
        assertThat(compositeDetector.getDetectors()).hasSize(3);
    }
}
