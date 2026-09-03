package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OutputAntiLeakGuard {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");

    public boolean contieneFuga(String respuesta, String solucionEsperada) {
        if (respuesta == null || respuesta.isBlank()) {
            return false;
        }

        // Si la respuesta incluye bloques de codigo extensos cuando no deberia
        var matcher = CODE_BLOCK_PATTERN.matcher(respuesta);
        while (matcher.find()) {
            String bloque = matcher.group();
            if (bloque.lines().count() > 8) {
                // Bloque de codigo sospechosamente largo para ser un tutor socratico
                return true;
            }
        }

        if (solucionEsperada != null && !solucionEsperada.isBlank()) {
            return respuesta
                    .toLowerCase()
                    .contains(solucionEsperada.toLowerCase().trim());
        }

        return false;
    }
}
