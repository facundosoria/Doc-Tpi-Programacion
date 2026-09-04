package ar.edu.utn.frc.tup.piv.evaluacionllm.service.tutor;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.ContextoDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.CrearConversacionRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.PayloadTutorDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.ConversacionResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.MensajeResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.RespuestaTutorResponse;

import java.util.List;
import java.util.UUID;

public interface TutorService {

    RespuestaTutorResponse responder(ContextoDto contexto, PayloadTutorDto payload);

    RespuestaTutorResponse responderConversacionDirecta(UUID conversacionId, String contenidoAlumno);

    ConversacionResponse crearConversacion(CrearConversacionRequest request);

    List<ConversacionResponse> listarConversaciones(String cursoCohorteId, String usuarioRef);

    List<MensajeResponse> listarMensajes(UUID conversacionId);
}
