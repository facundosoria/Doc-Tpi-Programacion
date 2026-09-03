package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class InputGuard {

    private static final List<String> JAILBREAK_KEYWORDS = List.of(
            "ignora tus restricciones", "ignora tus instrucciones", "ignore previous instructions",
            "ignora", "olvida tus reglas", "bypass", "pretend", "act as", "actua como", "actúa como",
            "exploit", "vulnerabilidad", "haz mi tarea", "dame el codigo", "dame el código",
            "dame la solucion", "dame la solución", "resuelve por mi", "resuelve por mí",
            "hazme la tarea", "codigo resuelto", "código resuelto", "descuida tus reglas",
            "solucion completa", "solución completa", "escribe todo el codigo", "escribe todo el código"
    );

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public boolean esJailbreak(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }

        String textoNormalizado = normalizar(texto);

        return JAILBREAK_KEYWORDS.stream()
                .map(this::normalizar)
                .anyMatch(textoNormalizado::contains);
    }

    private String normalizar(String input) {
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        String sinAcentos = DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
