package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeObfuscationDetectorTest {

    private CodeObfuscationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CodeObfuscationDetector(4.5);
    }

    @Test
    @DisplayName("T3 / BDD Escenario 1: Código JavaScript en Base64 detectado y bloqueado en < 50 ms")
    void bddScenario1JavaScriptInBase64Detected() {
        // "var x = 1; console.log(x);" codificado en Base64
        String payload = "dmFyIHggPSAxOyBjb25zb2xlLmxvZyh4KTs=";
        String message = "Mira este snippet: " + payload;

        DetectionResult result = detector.detect(message);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.CODE_OBFUSCATION);
        assertThat(result.getDetectorName()).isEqualTo("code_obfuscation");
        assertThat(result.getLatencyMs()).isLessThan(50L);
    }

    @Test
    @DisplayName("T3: Código Python camuflado en Base64 es bloqueado con CODE_OBFUSCATION")
    void pythonCodeInBase64Detected() {
        String pythonCode = "def calculate(values):\n    return sum(values) / len(values)";
        String base64Payload = Base64.getEncoder().encodeToString(pythonCode.getBytes(StandardCharsets.UTF_8));

        DetectionResult result = detector.detect("Revisa esta solucion: " + base64Payload);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.CODE_OBFUSCATION);
    }

    @Test
    @DisplayName("T3: Código Java camuflado en Base64 es bloqueado con CODE_OBFUSCATION")
    void javaCodeInBase64Detected() {
        String javaCode = "public class Solution { public void run() { System.out.println(1); } }";
        String base64Payload = Base64.getEncoder().encodeToString(javaCode.getBytes(StandardCharsets.UTF_8));

        DetectionResult result = detector.detect("Aca va la clase: " + base64Payload);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.CODE_OBFUSCATION);
    }

    @Test
    @DisplayName("T3 / BDD Escenario 2 / CA_negativo_2: Texto educativo legítimo sobre Base64 no es bloqueado")
    void bddScenario2EducationalTextNotBlocked() {
        String educationalText = "Base64 es una codificación que transforma datos binarios en texto ASCII; se usa en adjuntos de email";

        DetectionResult result = detector.detect(educationalText);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo(ModerationReasonCode.CLEAN);
    }

    @Test
    @DisplayName("T3: Cadena Base64 de texto en lenguaje natural sin código fuente no es bloqueada")
    void plainTextBase64WithoutCodeNotBlocked() {
        String naturalText = "Hola queridos estudiantes, bienvenidos al modulo de programacion del segundo cuatrimestre";
        String base64Payload = Base64.getEncoder().encodeToString(naturalText.getBytes(StandardCharsets.UTF_8));

        DetectionResult result = detector.detect("Mensaje cifrado: " + base64Payload);

        // Al decodificar no contiene palabras clave de código ejecutable
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("T3: Verificación de cálculo de entropía de Shannon")
    void shannonEntropyCalculation() {
        // Cadena homogénea: entropía 0
        assertThat(CodeObfuscationDetector.computeShannonEntropy("aaaaaa")).isEqualTo(0.0);

        // String Base64 de código JS tiene entropía > 4.5
        double entropy = CodeObfuscationDetector.computeShannonEntropy("dmFyIHggPSAxOyBjb25zb2xlLmxvZyh4KTs=");
        assertThat(entropy).isGreaterThan(4.5);
    }

    @Test
    @DisplayName("T3: Rendimiento objetivo < 5 ms")
    void performanceLessThan5Ms() {
        String payload = "dmFyIHggPSAxOyBjb25zb2xlLmxvZyh4KTs=";

        // Warmup
        for (int i = 0; i < 20; i++) {
            detector.detect(payload);
        }

        long start = System.currentTimeMillis();
        DetectionResult result = detector.detect(payload);
        long latency = System.currentTimeMillis() - start;

        assertThat(latency).isLessThan(5L);
        assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DisplayName("T3: Constructor por defecto y manejo de nulo y vacío")
    void defaultConstructorAndNullHandling() {
        CodeObfuscationDetector defaultDet = new CodeObfuscationDetector();
        assertThat(defaultDet.detect(null, "c1").isPassed()).isTrue();
        assertThat(defaultDet.detect("   ", "c1").isPassed()).isTrue();
        assertThat(CodeObfuscationDetector.computeShannonEntropy(null)).isEqualTo(0.0);
        assertThat(CodeObfuscationDetector.computeShannonEntropy("")).isEqualTo(0.0);
    }
}
