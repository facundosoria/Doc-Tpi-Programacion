package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway;

import ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.adapter.GroqAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmGatewayImpl implements LlmGateway {

    private final GroqAdapter groqAdapter;

    @Override
    public String llamar(String funcion, String systemPrompt, String userPrompt) {
        // En el M1 completo, aquí se consultaría la tabla funcion_modelo_config
        // y se verificaría la cuota en Redis. Por ahora el proveedor default es Groq.
        return groqAdapter.generar(systemPrompt, userPrompt);
    }

    @Override
    public String obtenerModeloActivo(String funcion) {
        return groqAdapter.getModelName();
    }
}
