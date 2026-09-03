package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class OutputAntiLeakGuard {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");

    public boolean contieneFuga(String respuesta, String solucionEsperada) {
        if (respuesta == null || respuesta.isBlank()) {
            return false;
        }

        // Si la respuesta incluye bloques de código extensos cuando no debería
        var matcher = CODE_BLOCK_PATTERN.matcher(respuesta);
        int codeBlockCount = 0;
        while (matcher.find()) {
            codeBlockCount++;
            String bloque = matcher.group();
            if (bloque.lines().count() > 8) {
                // Bloque de código sospechosamente largo para ser un tutor socrático
                return true;
            }
        }

        if (solucionEsperada != null && !solucionEsperada.isBlank()) {
            return respuesta.toLowerCase().contains(solucionEsperada.toLowerCase().trim());
        }

        return false;
    }
}
