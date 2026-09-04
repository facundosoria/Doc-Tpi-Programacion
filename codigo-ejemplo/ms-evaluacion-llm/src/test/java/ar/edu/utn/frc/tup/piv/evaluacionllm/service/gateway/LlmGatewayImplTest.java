package ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway;

import ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.adapter.GroqAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmGatewayImplTest {

    @Mock
    private GroqAdapter groqAdapter;

    private LlmGatewayImpl llmGateway;

    @BeforeEach
    void setUp() {
        llmGateway = new LlmGatewayImpl(groqAdapter);
    }

    @Test
    void debeDelegarLlamadaAlAdapter() {
        when(groqAdapter.generar("sys prompt", "user prompt")).thenReturn("Respuesta del modelo");

        String respuesta = llmGateway.llamar("tutor", "sys prompt", "user prompt");

        assertEquals("Respuesta del modelo", respuesta);
        verify(groqAdapter, times(1)).generar("sys prompt", "user prompt");
    }

    @Test
    void debeObtenerModeloActivo() {
        when(groqAdapter.getModelName()).thenReturn("llama-3.3-70b-versatile");

        String modelo = llmGateway.obtenerModeloActivo("tutor");

        assertEquals("llama-3.3-70b-versatile", modelo);
        verify(groqAdapter, times(1)).getModelName();
    }
}
