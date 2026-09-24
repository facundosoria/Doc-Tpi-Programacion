package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Detector de código camuflado y ofuscación por Base64 con entropía de Shannon (T3):
 * - Extrae secuencias candidatas en Base64 de longitud mínima (>= 16 caracteres).
 * - Calcula la entropía de Shannon de la secuencia.
 * - Si la entropía supera 4.5, intenta decodificar y busca palabras clave de código de programación.
 * - Emite FAIL con reason_code: CODE_OBFUSCATION si contiene código fuente camuflado.
 * - No bloquea texto educativo legítimo ("Base64 es una codificación...") ni Base64 de texto plano no ejecutable.
 */
@Component
public class CodeObfuscationDetector implements ModerationDetectorPort {

    private static final Logger log = LoggerFactory.getLogger(CodeObfuscationDetector.class);
    private static final String DETECTOR_NAME = "code_obfuscation";

    // Detecta secuencias continuas en Base64 de al menos 16 caracteres
    private static final Pattern BASE64_CANDIDATE_PATTERN = Pattern.compile("[A-Za-z0-9+/]{16,}={0,2}");

    private static final List<String> CODE_KEYWORDS = List.of(
            "class ", "class\n", "class{",
            "import ", "function ", "function(", "def ", "return ",
            "var ", "let ", "const ",
            "public ", "private ", "protected ",
            "void ", "int ", "boolean ",
            "console.log", "system.out.", "eval(",
            "<script", "#!/bin/", "echo ", "print("
    );

    private final double entropyThreshold;

    public CodeObfuscationDetector() {
        this(4.5);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CodeObfuscationDetector(
            @Value("${llm.moderation.obfuscation.entropy-threshold:4.5}") double entropyThreshold) {
        this.entropyThreshold = entropyThreshold;
    }

    @Override
    public String getName() {
        return DETECTOR_NAME;
    }

    @Override
    public DetectionResult detect(String text, String courseId) {
        long start = System.currentTimeMillis();
        if (text == null || text.isBlank()) {
            return DetectionResult.pass(DETECTOR_NAME, 0);
        }

        Matcher matcher = BASE64_CANDIDATE_PATTERN.matcher(text);

        while (matcher.find()) {
            String candidate = matcher.group();

            // Longitud mínima de comprobación (>= 16)
            if (candidate.length() < 16) {
                continue;
            }

            double entropy = computeShannonEntropy(candidate);

            if (entropy > entropyThreshold) {
                String decodedText = tryDecodeBase64(candidate);
                if (decodedText != null && containsCodeKeywords(decodedText)) {
                    long latency = Math.max(1, System.currentTimeMillis() - start);
                    log.info("Ofuscación de código detectada en Base64 (entropía={}): clasificado como FAIL", entropy);
                    return DetectionResult.fail(
                            DETECTOR_NAME,
                            ModerationReasonCode.CODE_OBFUSCATION,
                            Math.min(1.0, entropy / 6.0),
                            latency
                    );
                }
            }
        }

        long latency = Math.max(1, System.currentTimeMillis() - start);
        return DetectionResult.pass(DETECTOR_NAME, latency);
    }

    /**
     * Calcula la entropía de Shannon: H(X) = -sum(p_i * log2(p_i)).
     */
    public static double computeShannonEntropy(String str) {
        if (str == null || str.isEmpty()) {
            return 0.0;
        }
        Map<Character, Integer> frequencies = new HashMap<>();
        for (int i = 0; i < str.length(); i++) {
            frequencies.merge(str.charAt(i), 1, Integer::sum);
        }

        double entropy = 0.0;
        double len = str.length();
        for (int count : frequencies.values()) {
            double p = count / len;
            entropy -= p * (Math.log(p) / Math.log(2.0));
        }
        return entropy;
    }

    private static String tryDecodeBase64(String candidate) {
        try {
            // Asegurar padding si es necesario
            String padded = candidate;
            int mod = padded.length() % 4;
            if (mod != 0) {
                padded = padded + "=".repeat(4 - mod);
            }
            byte[] decoded = Base64.getDecoder().decode(padded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // No es Base64 válido
            return null;
        }
    }

    private static boolean containsCodeKeywords(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : CODE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
