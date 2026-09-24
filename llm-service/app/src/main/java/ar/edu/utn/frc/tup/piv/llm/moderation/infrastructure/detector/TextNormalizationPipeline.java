package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.detector;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.TextNormalizerPort;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Pipeline de normalización de texto y neutralización de evasiones (T1):
 * - Normalización Unicode NFKD y eliminación de tildes/signos diacríticos (\p{M}).
 * - Lowercase canónico.
 * - Sustitución de leet speak ('0'->'o', '3'->'e', '1'/'!'/'|'->'i', '4'/'@'->'a', '5'/'$'->'s', '7'->'t')
 *   y homoglifos cirílicos comunes.
 * - Colapso de caracteres repetidos (ej. 'holaaaa' -> 'hola').
 * - Unión de letras separadas por puntuación o espacios (ej. 'p-e-l-o' -> 'pelo', 'p.e.l.o.t.u.d.o' -> 'pelotudo').
 */
@Component
public class TextNormalizationPipeline implements TextNormalizerPort {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern REPEATING_CHARS_PATTERN = Pattern.compile("([a-z])\\1{2,}");
    private static final Pattern REPEATING_VOWELS_PATTERN = Pattern.compile("([aiu])\\1+");
    private static final Pattern PUNCT_SEPARATED_LETTERS = Pattern.compile("\\b([a-z0-9](?:[.\\-_*~/\\\\#|]+[a-z0-9])+)\\b");
    private static final Pattern SPACE_SEPARATED_LETTERS = Pattern.compile("(?:^|(?<=\\s))([a-z0-9](?:\\s+[a-z0-9]){3,})(?=\\s|$)");
    private static final Pattern PUNCT_STRIP = Pattern.compile("[.\\-_*~/\\\\#|]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    @Override
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. Normalización Unicode NFKD y eliminación de marcas diacríticas
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");

        // 2. Minúsculas estándar
        String lower = withoutDiacritics.toLowerCase(Locale.ROOT);

        // 3. Transformación de leet speak y homoglifos
        String leetTransformed = transformLeetAndHomoglyphs(lower);

        // 4. Colapso de caracteres repetidos (ej. holaaaa -> hola)
        String collapsed = collapseRepeatingCharacters(leetTransformed);

        // 5. Unión de letras con separadores (ej. p-e-l-o -> pelo, p.e.l.o.t.u.d.o -> pelotudo)
        return collapseSeparators(collapsed);
    }

    private String transformLeetAndHomoglyphs(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sb.append(mapChar(c));
        }
        return sb.toString();
    }

    private char mapChar(char c) {
        return switch (c) {
            case '0' -> 'o';
            case '1', '!', '|' -> 'i';
            case '3' -> 'e';
            case '4', '@' -> 'a';
            case '5', '$' -> 's';
            case '7' -> 't';
            // Homoglifos cirílicos comunes
            case '\u0430' -> 'a'; // а
            case '\u0435' -> 'e'; // е
            case '\u043E' -> 'o'; // о
            case '\u0440' -> 'p'; // р
            case '\u0441' -> 'c'; // с
            case '\u0443' -> 'y'; // у
            case '\u0445' -> 'x'; // х
            case '\u0456' -> 'i'; // і
            default -> c;
        };
    }

    private String collapseRepeatingCharacters(String input) {
        // Reducir cualquier carácter que repita 3 o más veces a 1 (ej. 'holaaaa' -> 'hola')
        String res = REPEATING_CHARS_PATTERN.matcher(input).replaceAll("$1");
        // Reducir vocales que no suelen duplicarse en español (a, i, u)
        return REPEATING_VOWELS_PATTERN.matcher(res).replaceAll("$1");
    }

    private String collapseSeparators(String input) {
        // Unir secuencias de caracteres separados por signos de puntuación (ej. 'p-e-l-o' -> 'pelo')
        Matcher punctMatcher = PUNCT_SEPARATED_LETTERS.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (punctMatcher.find()) {
            String matched = punctMatcher.group(0);
            String joined = PUNCT_STRIP.matcher(matched).replaceAll("");
            punctMatcher.appendReplacement(sb, Matcher.quoteReplacement(joined));
        }
        punctMatcher.appendTail(sb);
        String intermediate = sb.toString();

        // Unir secuencias de 4 o más caracteres individuales separados por espacios (ej. 'p e l o t u d o' -> 'pelotudo')
        Matcher spaceMatcher = SPACE_SEPARATED_LETTERS.matcher(intermediate);
        StringBuilder sbSpace = new StringBuilder();
        while (spaceMatcher.find()) {
            String matched = spaceMatcher.group(1);
            String joined = MULTI_SPACE.matcher(matched).replaceAll("");
            spaceMatcher.appendReplacement(sbSpace, Matcher.quoteReplacement(joined));
        }
        spaceMatcher.appendTail(sbSpace);

        return sbSpace.toString();
    }
}
