package ar.edu.utn.frc.tup.piv.evaluacionllm.controller;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.AiRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.ContextoDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.PayloadTutorDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.RespuestaTutorResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.exception.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.tutor.TutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TutorService tutorService;

    @InjectMocks
    private AiController aiController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void interactuarTutor_debeRetornar200ConAiResponseValido() throws Exception {
        UUID conversacionId = UUID.randomUUID();
        ContextoDto contexto = ContextoDto.builder()
                .cursoCohorteId("curso-2026-1")
                .usuarioRef("usr-alumno-01")
                .desafioId("desafio-05")
                .build();

        PayloadTutorDto payload = PayloadTutorDto.builder()
                .conversacionId(conversacionId)
                .mensaje("¿Qué es la complejidad algorítmica?")
                .build();

        AiRequest<PayloadTutorDto> request = AiRequest.<PayloadTutorDto>builder()
                .contexto(contexto)
                .payload(payload)
                .modo("sync")
                .build();

        RespuestaTutorResponse tutorResponse = RespuestaTutorResponse.builder()
                .respuesta("Es una medida de cómo escala el tiempo de cómputo...")
                .estado("OK")
                .modelo("llama-3.3-70b-versatile")
                .conversacionId(conversacionId)
                .build();

        when(tutorService.responder(any(), any())).thenReturn(tutorResponse);

        mockMvc.perform(post("/ai/tutor")
                        .header("X-Trace-Id", "trace-xyz-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trace_id").value("trace-xyz-123"))
                .andExpect(jsonPath("$.resultado.respuesta").value("Es una medida de cómo escala el tiempo de cómputo..."))
                .andExpect(jsonPath("$.resultado.estado").value("OK"))
                .andExpect(jsonPath("$.metadata.funcion").value("tutor"));
    }

    @Test
    void interactuarTutor_cuandoFaltanCamposObligatorios_debeRetornar400() throws Exception {
        AiRequest<PayloadTutorDto> requestInvalido = new AiRequest<>();

        mockMvc.perform(post("/ai/tutor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validacion_fallida"));
    }
}
