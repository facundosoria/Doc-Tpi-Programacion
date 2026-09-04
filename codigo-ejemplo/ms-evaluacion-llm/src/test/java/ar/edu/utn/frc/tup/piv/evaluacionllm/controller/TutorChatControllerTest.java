package ar.edu.utn.frc.tup.piv.evaluacionllm.controller;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.CrearConversacionRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.MensajeAlumnoRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ConversacionResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.MensajeResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TutorChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TutorService tutorService;

    @InjectMocks
    private TutorChatController chatController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void crearConversacion_debeRetornar201() throws Exception {
        UUID id = UUID.randomUUID();
        CrearConversacionRequest request = CrearConversacionRequest.builder()
                .cursoCohorteId("curso-2026-1")
                .usuarioRef("usr-1")
                .titulo("Sesión de Java")
                .build();

        ConversacionResponse response = ConversacionResponse.builder()
                .id(id)
                .cursoCohorteId("curso-2026-1")
                .usuarioRef("usr-1")
                .titulo("Sesión de Java")
                .fechaCreacion(LocalDateTime.now())
                .estado("ABIERTA")
                .build();

        when(tutorService.crearConversacion(any())).thenReturn(response);

        mockMvc.perform(post("/api/conversaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.titulo").value("Sesión de Java"));
    }

    @Test
    void listarConversaciones_debeRetornar200ConLista() throws Exception {
        when(tutorService.listarConversaciones(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/conversaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarMensajes_debeRetornar200ConLista() throws Exception {
        UUID convId = UUID.randomUUID();
        when(tutorService.listarMensajes(convId)).thenReturn(List.of(
                MensajeResponse.builder()
                        .id(UUID.randomUUID())
                        .rol("alumno")
                        .contenido("Hola")
                        .timestamp(LocalDateTime.now())
                        .build()
        ));

        mockMvc.perform(get("/api/conversaciones/" + convId + "/mensajes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("alumno"));
    }

    @Test
    void enviarMensaje_debeRetornar200() throws Exception {
        UUID convId = UUID.randomUUID();
        MensajeAlumnoRequest request = MensajeAlumnoRequest.builder()
                .contenido("¿Cómo funciona un HashMap?")
                .build();

        RespuestaTutorResponse response = RespuestaTutorResponse.builder()
                .respuesta("Un HashMap almacena pares clave-valor...")
                .estado("OK")
                .modelo("llama-3.3-70b-versatile")
                .conversacionId(convId)
                .build();

        when(tutorService.responderConversacionDirecta(eq(convId), any())).thenReturn(response);

        mockMvc.perform(post("/api/conversaciones/" + convId + "/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.respuesta").value("Un HashMap almacena pares clave-valor..."));
    }
}
