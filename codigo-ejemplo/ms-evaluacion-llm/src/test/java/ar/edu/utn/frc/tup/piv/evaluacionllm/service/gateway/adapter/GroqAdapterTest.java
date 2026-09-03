package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqAdapterTest {

    @Test
    void getModelName_debeRetornarModeloConfigurado() {
        GroqAdapter adapter = new GroqAdapter("https://api.groq.com/openai", "mock-key", "llama-3.3-70b-versatile");
        assertEquals("llama-3.3-70b-versatile", adapter.getModelName());
    }

    @Test
    void generar_cuandoApiKeyEsMock_debeRetornarRespuestaSimulada() {
        GroqAdapter adapter = new GroqAdapter("https://api.groq.com/openai", "mock-key", "llama-3.3-70b-versatile");
        String resultado = adapter.generar("System prompt", "User prompt");

        assertTrue(resultado.contains("Respuesta simulada en entorno de desarrollo"));
    }

    @Test
    void generar_cuandoApiKeyEsNulaOVacia_debeRetornarRespuestaSimulada() {
        GroqAdapter adapterNula = new GroqAdapter("https://api.groq.com/openai", null, "llama-3.3-70b-versatile");
        assertTrue(adapterNula.generar("Sys", "User").contains("Respuesta simulada"));

        GroqAdapter adapterVacia = new GroqAdapter("https://api.groq.com/openai", "   ", "llama-3.3-70b-versatile");
        assertTrue(adapterVacia.generar("Sys", "User").contains("Respuesta simulada"));
    }

    @Test
    void dtoEstructuras_gettersAndSetters() {
        GroqAdapter.ChatCompletionResponse response = new GroqAdapter.ChatCompletionResponse();
        GroqAdapter.ChatCompletionResponse.Message message = new GroqAdapter.ChatCompletionResponse.Message("assistant", "Hola");
        GroqAdapter.ChatCompletionResponse.Choice choice = new GroqAdapter.ChatCompletionResponse.Choice(message);
        response.setChoices(java.util.List.of(choice));

        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole());
        assertEquals("Hola", response.getChoices().get(0).getMessage().getContent());
    }
}
