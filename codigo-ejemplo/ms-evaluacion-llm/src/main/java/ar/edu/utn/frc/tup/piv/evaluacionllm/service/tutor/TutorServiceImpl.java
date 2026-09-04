package ar.edu.utn.frc.tup.piv.evaluacionllm.service.tutor;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.ContextoDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.CrearConversacionRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.PayloadTutorDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ConversacionResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.MensajeResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.RespuestaTutorResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.entity.ConversacionEntity;
import ar.edu.utn.frc.tup.piv.evaluacionllm.entity.MensajeEntity;
import ar.edu.utn.frc.tup.piv.evaluacionllm.exception.RecursoNoEncontradoException;
import ar.edu.utn.frc.tup.piv.evaluacionllm.repository.ConversacionRepository;
import ar.edu.utn.frc.tup.piv.evaluacionllm.repository.MensajeRepository;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.LlmGateway;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard.InputGuard;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.gateway.guard.OutputAntiLeakGuard;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    public static final String RESPUESTA_BLOQUEADA =
            "No puedo procesar esa consulta de esa forma. " +
            "¿Hay algo específico del tema que no entiendas? " +
            "Cuéntame qué es lo que se te complica y podemos avanzar juntos.";

    private final LlmGateway llmGateway;
    private final InputGuard inputGuard;
    private final OutputAntiLeakGuard outputAntiLeakGuard;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final ResourceLoader resourceLoader;

    private String systemPromptCache;
    private String userPromptTemplateCache;

    @PostConstruct
    public void cargarPrompts() {
        this.systemPromptCache = leerRecurso("classpath:prompts/tutor/system-v1.txt");
        this.userPromptTemplateCache = leerRecurso("classpath:prompts/tutor/user-v1.txt");
    }

    private String leerRecurso(String ruta) {
        try {
            Resource resource = resourceLoader.getResource(ruta);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar prompt desde {}: {}", ruta, e.getMessage());
        }
        return "";
    }

    @Override
    @Transactional
    public RespuestaTutorResponse responder(ContextoDto contexto, PayloadTutorDto payload) {
        ConversacionEntity conversacion;
        if (payload.getConversacionId() != null) {
            conversacion = conversacionRepository.findById(payload.getConversacionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada: " + payload.getConversacionId()));
        } else {
            conversacion = ConversacionEntity.builder()
                    .cursoCohorteId(contexto.getCursoCohorteId())
                    .usuarioRef(contexto.getUsuarioRef())
                    .desafioId(contexto.getDesafioId())
                    .titulo(payload.getTitulo() != null && !payload.getTitulo().isBlank()
                            ? payload.getTitulo().trim()
                            : "Tutoría Desafío " + (contexto.getDesafioId() != null ? contexto.getDesafioId() : "General"))
                    .fechaCreacion(LocalDateTime.now())
                    .estado("ABIERTA")
                    .build();
            conversacion = conversacionRepository.save(conversacion);
        }

        return procesarMensaje(conversacion, payload.getMensaje());
    }

    @Override
    @Transactional
    public RespuestaTutorResponse responderConversacionDirecta(UUID conversacionId, String contenidoAlumno) {
        ConversacionEntity conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada: " + conversacionId));
        return procesarMensaje(conversacion, contenidoAlumno);
    }

    private RespuestaTutorResponse procesarMensaje(ConversacionEntity conversacion, String contenidoAlumno) {
        String modeloActivo = llmGateway.obtenerModeloActivo("tutor");

        // 1. Guardar mensaje del alumno
        MensajeEntity mensajeAlumno = MensajeEntity.builder()
                .conversacion(conversacion)
                .rol("alumno")
                .contenido(contenidoAlumno)
                .timestamp(LocalDateTime.now())
                .build();
        mensajeAlumno = mensajeRepository.save(mensajeAlumno);

        // 2. Validar con Guardarraíl de entrada (Jailbreak)
        if (inputGuard.esJailbreak(contenidoAlumno)) {
            MensajeEntity mensajeBloqueado = MensajeEntity.builder()
                    .conversacion(conversacion)
                    .rol("tutor")
                    .contenido(RESPUESTA_BLOQUEADA)
                    .timestamp(LocalDateTime.now())
                    .build();
            mensajeBloqueado = mensajeRepository.save(mensajeBloqueado);

            return RespuestaTutorResponse.builder()
                    .respuesta(RESPUESTA_BLOQUEADA)
                    .estado("BLOCKED")
                    .modelo(modeloActivo)
                    .conversacionId(conversacion.getId())
                    .mensajeAlumnoId(mensajeAlumno.getId())
                    .mensajeTutorId(mensajeBloqueado.getId())
                    .build();
        }

        // 3. Obtener histórico y armar prompt
        List<MensajeEntity> historico = mensajeRepository.findByConversacionIdOrderByTimestampAsc(conversacion.getId());
        String historicoTexto = historico.stream()
                .map(m -> String.format("%s: %s", m.getRol(), m.getContenido()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String userPrompt = construirUserPrompt(conversacion.getTitulo(), historicoTexto, contenidoAlumno);
        String systemPrompt = this.systemPromptCache != null && !this.systemPromptCache.isBlank()
                ? this.systemPromptCache
                : "Eres un tutor socrático. Guía al alumno sin dar la solución de código.";

        // 4. Invocar LlmGateway
        String respuestaTexto = llmGateway.llamar("tutor", systemPrompt, userPrompt);

        // 5. Guardarraíl de salida (Anti-fuga)
        if (outputAntiLeakGuard != null && outputAntiLeakGuard.contieneFuga(respuestaTexto, null)) {
            respuestaTexto = "¿Podrías intentar explicar cómo estructurarías el algoritmo con tus propias palabras antes de que revisemos más detalles?";
        }

        // 6. Guardar respuesta del tutor
        MensajeEntity mensajeTutor = MensajeEntity.builder()
                .conversacion(conversacion)
                .rol("tutor")
                .contenido(respuestaTexto)
                .timestamp(LocalDateTime.now())
                .build();
        mensajeTutor = mensajeRepository.save(mensajeTutor);

        return RespuestaTutorResponse.builder()
                .respuesta(respuestaTexto)
                .estado("OK")
                .modelo(modeloActivo)
                .conversacionId(conversacion.getId())
                .mensajeAlumnoId(mensajeAlumno.getId())
                .mensajeTutorId(mensajeTutor.getId())
                .build();
    }

    private String construirUserPrompt(String tema, String historico, String pregunta) {
        if (this.userPromptTemplateCache != null && !this.userPromptTemplateCache.isBlank()) {
            return this.userPromptTemplateCache
                    .replace("{tema}", tema != null ? tema : "Consulta general")
                    .replace("{historico}", historico != null ? historico : "")
                    .replace("{pregunta}", pregunta != null ? pregunta : "");
        }
        return String.format("TEMA: %s\nHISTÓRICO:\n%s\nPREGUNTA ALUMNO: %s\nResponde socráticamente:", tema, historico, pregunta);
    }

    @Override
    @Transactional
    public ConversacionResponse crearConversacion(CrearConversacionRequest request) {
        ConversacionEntity conversacion = ConversacionEntity.builder()
                .cursoCohorteId(request.getCursoCohorteId())
                .usuarioRef(request.getUsuarioRef())
                .desafioId(request.getDesafioId())
                .titulo(request.getTitulo() != null && !request.getTitulo().isBlank()
                        ? request.getTitulo().trim()
                        : "Nueva conversación")
                .fechaCreacion(LocalDateTime.now())
                .estado("ABIERTA")
                .build();

        ConversacionEntity guardada = conversacionRepository.save(conversacion);
        return mapToConversacionResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversacionResponse> listarConversaciones(String cursoCohorteId, String usuarioRef) {
        List<ConversacionEntity> lista;
        if (cursoCohorteId != null && usuarioRef != null) {
            lista = conversacionRepository.findByCursoCohorteIdAndUsuarioRef(cursoCohorteId, usuarioRef);
        } else if (cursoCohorteId != null) {
            lista = conversacionRepository.findByCursoCohorteId(cursoCohorteId);
        } else {
            lista = conversacionRepository.findAll();
        }
        return lista.stream().map(this::mapToConversacionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeResponse> listarMensajes(UUID conversacionId) {
        conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada: " + conversacionId));
        return mensajeRepository.findByConversacionIdOrderByTimestampAsc(conversacionId).stream()
                .map(m -> MensajeResponse.builder()
                        .id(m.getId())
                        .rol(m.getRol())
                        .contenido(m.getContenido())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private ConversacionResponse mapToConversacionResponse(ConversacionEntity c) {
        return ConversacionResponse.builder()
                .id(c.getId())
                .cursoCohorteId(c.getCursoCohorteId())
                .usuarioRef(c.getUsuarioRef())
                .desafioId(c.getDesafioId())
                .titulo(c.getTitulo())
                .fechaCreacion(c.getFechaCreacion())
                .estado(c.getEstado())
                .build();
    }
}
