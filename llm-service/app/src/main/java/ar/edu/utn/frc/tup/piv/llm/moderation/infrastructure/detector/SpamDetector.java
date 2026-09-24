package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.DetectionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationReasonCode;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationDetectorPort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Detector de spam por frecuencia y densidad de hipervínculos (T4):
 * - Evalúa densidad de URLs: clasifica como FAIL con SPAM si hay >= 3 URLs distintas en < 200 caracteres (CA3).
 * - Detecta repetición excesiva de tokens idénticos (token flooding) e inundación de caracteres repetidos (>= 15).
 * - Rendimiento objetivo: latencia < 3 ms en memoria.
 */
@Component
public class SpamDetector implements ModerationDetectorPort {

    private static final Logger log = LoggerFactory.getLogger(SpamDetector.class);
    private static final String DETECTOR_NAME = "spam";

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,!?;:\")\\]]+$");
    private static final Pattern CHAR_FLOODING_PATTERN = Pattern.compile("([^\\s])\\1{14,}");

    private final int maxDistinctUrlsShortText;
    private final int shortTextMaxChars;
    private final int maxTokenRepetition;

    public SpamDetector() {
        this(3, 200, 5);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SpamDetector(
            @Value("${llm.moderation.spam.max-distinct-urls-short-text:3}") int maxDistinctUrlsShortText,
            @Value("${llm.moderation.spam.short-text-max-chars:200}") int shortTextMaxChars,
            @Value("${llm.moderation.spam.max-token-repetition:5}") int maxTokenRepetition) {
        this.maxDistinctUrlsShortText = maxDistinctUrlsShortText;
        this.shortTextMaxChars = shortTextMaxChars;
        this.maxTokenRepetition = maxTokenRepetition;
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

        // 1. Detección de densidad de URLs (CA3)
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        Set<String> distinctUrls = new HashSet<>();
        while (urlMatcher.find()) {
            String rawUrl = urlMatcher.group();
            String cleanedUrl = TRAILING_PUNCTUATION.matcher(rawUrl).replaceAll("").toLowerCase(Locale.ROOT);
            distinctUrls.add(cleanedUrl);
        }

        if (distinctUrls.size() >= maxDistinctUrlsShortText && text.length() < shortTextMaxChars) {
            long latency = Math.max(1, System.currentTimeMillis() - start);
            log.info("Spam detectado por alta densidad de URLs ({} URLs en {} caracteres)", distinctUrls.size(), text.length());
            return DetectionResult.fail(DETECTOR_NAME, ModerationReasonCode.SPAM, 1.0, latency);
        }

        // 2. Detección de inundación de caracteres consecutivos (char flooding >= 15)
        if (CHAR_FLOODING_PATTERN.matcher(text).find()) {
            long latency = Math.max(1, System.currentTimeMillis() - start);
            log.info("Spam detectado por inundación de caracteres repetidos");
            return DetectionResult.fail(DETECTOR_NAME, ModerationReasonCode.SPAM, 1.0, latency);
        }

        // 3. Detección de repetición anormal de tokens (token flooding)
        String[] tokens = text.toLowerCase(Locale.ROOT).split("\\s+");
        if (tokens.length >= maxTokenRepetition) {
            Map<String, Integer> freq = new HashMap<>();
            int maxFreq = 0;
            for (String token : tokens) {
                if (token.length() >= 2) {
                    int count = freq.merge(token, 1, Integer::sum);
                    if (count > maxFreq) {
                        maxFreq = count;
                    }
                }
            }

            // Si un token repite >= maxTokenRepetition y representa más del 35% de los tokens totales, o repite >= 8 veces
            if ((maxFreq >= maxTokenRepetition && (double) maxFreq / tokens.length >= 0.35) || maxFreq >= 8) {
                long latency = Math.max(1, System.currentTimeMillis() - start);
                log.info("Spam detectado por repetición anormal de tokens (frecuencia={})", maxFreq);
                return DetectionResult.fail(DETECTOR_NAME, ModerationReasonCode.SPAM, 1.0, latency);
            }
        }

        long latency = Math.max(1, System.currentTimeMillis() - start);
        return DetectionResult.pass(DETECTOR_NAME, latency);
    }
}
