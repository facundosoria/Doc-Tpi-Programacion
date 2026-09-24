package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.TextNormalizerPort;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.ahocorasick.trie.PayloadEmit;
import org.ahocorasick.trie.PayloadTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Detector determinista de términos ofensivos (T2):
 * - Utiliza el autómata Aho-Corasick en memoria (org.ahocorasick:ahocorasick) con .onlyWholeWords().
 * - Carga diccionarios calibrados con pesos (com.modernmt.text:profanity-filter) para 'es' y 'en'.
 * - Normaliza el texto previamente con TextNormalizerPort para desbaratar evasiones (leet, tildes, etc.).
 * - Evalúa umbrales configurables por curso o por defecto.
 * - Evita falsos positivos por Scunthorpe y términos polisémicos calibrados (ej. 'concha' score 0.062 vs 'la concha de tu madre' score 0.795).
 * - Cumple con CA5: no expone palabras clave ni reglas internas.
 */
@Component
public class ProfanityDetector implements ModerationDetectorPort {

    private static final Logger log = LoggerFactory.getLogger(ProfanityDetector.class);
    private static final String DETECTOR_NAME = "offensive";

    private final TextNormalizerPort normalizer;
    private final double defaultThreshold;
    private final Map<String, Double> courseThresholds = new ConcurrentHashMap<>();
    private final PayloadTrie<Double> trie;

    public ProfanityDetector(TextNormalizerPort normalizer) {
        this(normalizer, 0.70, Map.of(), Map.of());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProfanityDetector(
            TextNormalizerPort normalizer,
            @Value("${llm.moderation.offensive.default-threshold:0.70}") double defaultThreshold) {
        this(normalizer, defaultThreshold, Map.of(), Map.of());
    }

    public ProfanityDetector(
            TextNormalizerPort normalizer,
            double defaultThreshold,
            Map<String, Double> initialCourseThresholds,
            Map<String, Double> customDictionary) {
        this.normalizer = normalizer;
        this.defaultThreshold = defaultThreshold;
        if (initialCourseThresholds != null) {
            this.courseThresholds.putAll(initialCourseThresholds);
        }
        this.trie = buildTrie(normalizer, customDictionary);
    }

    public void setCourseThreshold(String courseId, double threshold) {
        if (courseId != null && !courseId.isBlank()) {
            this.courseThresholds.put(courseId, threshold);
        }
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

        double threshold = (courseId != null && courseThresholds.containsKey(courseId))
                ? courseThresholds.get(courseId)
                : defaultThreshold;

        // 1. Normalización para neutralizar leet speak, diacríticos y separadores
        String normalized = normalizer.normalize(text);

        // 2. Búsqueda determinista multilínea en una sola pasada vía Aho-Corasick
        Collection<PayloadEmit<Double>> emits = trie.parseText(normalized);

        double maxScore = 0.0;
        boolean offensiveFound = false;

        for (PayloadEmit<Double> emit : emits) {
            Double score = emit.getPayload();
            if (score != null) {
                if (score > maxScore) {
                    maxScore = score;
                }
                if (score >= threshold) {
                    offensiveFound = true;
                }
            }
        }

        long latency = Math.max(1, System.currentTimeMillis() - start);

        if (offensiveFound) {
            // Se reporta FAIL sin filtrar la palabra clave ni el patrón interno (CA5 / CA_negativo_1)
            return DetectionResult.fail(DETECTOR_NAME, ModerationReasonCode.OFFENSIVE, maxScore, latency);
        }

        return DetectionResult.pass(DETECTOR_NAME, latency);
    }

    private static PayloadTrie<Double> buildTrie(TextNormalizerPort normalizer, Map<String, Double> customDictionary) {
        PayloadTrie.PayloadTrieBuilder<Double> builder = PayloadTrie.<Double>builder()
                .onlyWholeWords()
                .ignoreCase();

        Map<String, Double> terms = new HashMap<>();

        // Cargar diccionario en español de modernmt
        loadClasspathDictionary("com/modernmt/text/profanity/dictionary.es", terms, normalizer);
        // Cargar diccionario en inglés de modernmt
        loadClasspathDictionary("com/modernmt/text/profanity/dictionary.en", terms, normalizer);

        // Incorporar diccionario custom si existe
        if (customDictionary != null) {
            for (Map.Entry<String, Double> entry : customDictionary.entrySet()) {
                String normKey = normalizer.normalize(entry.getKey());
                if (!normKey.isBlank()) {
                    terms.put(normKey, entry.getValue());
                }
            }
        }

        // Agregar términos al trie
        for (Map.Entry<String, Double> entry : terms.entrySet()) {
            builder.addKeyword(entry.getKey(), entry.getValue());
        }

        log.info("Autómata Aho-Corasick de ofensividad inicializado con {} términos en memoria.", terms.size());
        return builder.build();
    }

    private static void loadClasspathDictionary(String resourcePath, Map<String, Double> target, TextNormalizerPort normalizer) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ProfanityDetector.class.getClassLoader();
        }
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Recurso de diccionario de profanidad no encontrado: {}", resourcePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\t");
                    if (parts.length >= 2) {
                        String term = normalizer.normalize(parts[0].trim());
                        try {
                            double score = Double.parseDouble(parts[1].trim());
                            if (!term.isBlank()) {
                                target.merge(term, score, Math::max);
                            }
                        } catch (NumberFormatException ignored) {
                            // Ignorar línea con formato numérico inválido
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cargando diccionario de profanidad desde {}", resourcePath, e);
        }
    }
}
