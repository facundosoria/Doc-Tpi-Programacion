package com.example.demo.controller;

import com.example.demo.dto.ConversacionResponse;
import com.example.demo.dto.CrearConversacionRequest;
import com.example.demo.dto.MensajeAlumnoRequest;
import com.example.demo.dto.MensajeResponse;
import com.example.demo.dto.RespuestaTutorResponse;
import com.example.demo.model.Conversacion;
import com.example.demo.model.Mensaje;
import com.example.demo.repository.ConversacionRepository;
import com.example.demo.repository.MensajeRepository;
import com.example.demo.service.TutorSocraticoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversaciones")
@Tag(name = "Chatbot Tutor Socrático", description = "Endpoints del chat de tutoría con IA (Groq)")
public class ChatController {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final TutorSocraticoService tutorService;

    public ChatController(ConversacionRepository conversacionRepository,
                          MensajeRepository mensajeRepository,
                          TutorSocraticoService tutorService) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.tutorService = tutorService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva conversación de tutoría")
    public ResponseEntity<ConversacionResponse> crearConversacion(
            @RequestBody CrearConversacionRequest request) {
        Conversacion conversacion = new Conversacion();
        conversacion.setTitulo(
                request.titulo() == null || request.titulo().isBlank()
                        ? "Nueva conversación"
                        : request.titulo().trim());
        Conversacion guardada = conversacionRepository.save(conversacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(guardada));
    }

    @GetMapping
    @Operation(summary = "Listar todas las conversaciones")
    public List<ConversacionResponse> listarConversaciones() {
        return conversacionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/mensajes")
    @Operation(summary = "Ver el histórico de mensajes de una conversación")
    public List<MensajeResponse> listarMensajes(@PathVariable UUID id) {
        conversacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada: " + id));
        return mensajeRepository.findByConversacionIdOrderByTimestampAsc(id).stream()
                .map(this::toMensajeResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/mensajes")
    @Operation(summary = "Enviar una pregunta del alumno y recibir la respuesta del tutor",
               description = "Envía el mensaje del alumno. El tutor recibe todo el histórico de la conversación como contexto.")
    public ResponseEntity<RespuestaTutorResponse> enviarMensaje(
            @PathVariable UUID id,
            @RequestBody MensajeAlumnoRequest request) {
        RespuestaTutorResponse respuesta = tutorService.responder(id, request.contenido());
        return ResponseEntity.ok(respuesta);
    }

    private ConversacionResponse toResponse(Conversacion c) {
        return new ConversacionResponse(c.getId(), c.getTitulo(), c.getFechaCreacion(), c.getEstado());
    }

    private MensajeResponse toMensajeResponse(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getRol(), m.getContenido(), m.getTimestamp());
    }
}
