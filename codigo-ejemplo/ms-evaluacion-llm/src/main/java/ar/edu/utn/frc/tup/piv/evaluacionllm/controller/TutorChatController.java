package ar.edu.utn.frc.tup.piv.evaluacionllm.controller;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.CrearConversacionRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.MensajeAlumnoRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ConversacionResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.MensajeResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.RespuestaTutorResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.tutor.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversaciones")
@RequiredArgsConstructor
public class TutorChatController {

    private final TutorService tutorService;

    @PostMapping
    public ResponseEntity<ConversacionResponse> crearConversacion(@Valid @RequestBody CrearConversacionRequest request) {
        ConversacionResponse response = tutorService.crearConversacion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversacionResponse>> listarConversaciones(
            @RequestParam(value = "curso_cohorte_id", required = false) String cursoCohorteId,
            @RequestParam(value = "usuario_ref", required = false) String usuarioRef) {
        List<ConversacionResponse> response = tutorService.listarConversaciones(cursoCohorteId, usuarioRef);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<MensajeResponse>> listarMensajes(@PathVariable("id") UUID id) {
        List<MensajeResponse> response = tutorService.listarMensajes(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<RespuestaTutorResponse> enviarMensaje(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MensajeAlumnoRequest request) {
        RespuestaTutorResponse response = tutorService.responderConversacionDirecta(id, request.getContenido());
        return ResponseEntity.ok(response);
    }
}
