package ar.edu.utn.frc.tup.piv.evaluacionllm.controller;

import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.AiRequest;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request.PayloadTutorDto;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.AiResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response.RespuestaTutorResponse;
import ar.edu.utn.frc.tup.piv.evaluacionllm.service.tutor.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final TutorService tutorService;

    @PostMapping("/tutor")
    public ResponseEntity<AiResponse<RespuestaTutorResponse>> interactuarTutor(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody AiRequest<PayloadTutorDto> request) {

        String effectiveTraceId = traceId != null && !traceId.isBlank() ? traceId : UUID.randomUUID().toString();

        RespuestaTutorResponse respuestaTutor = tutorService.responder(request.getContexto(), request.getPayload());

        AiResponse<RespuestaTutorResponse> response = AiResponse.<RespuestaTutorResponse>builder()
                .resultado(respuestaTutor)
                .traceId(effectiveTraceId)
                .metadata(Map.of(
                        "funcion", "tutor",
                        "modo", request.getModo(),
                        "estado", respuestaTutor.getEstado()
                ))
                .build();

        return ResponseEntity.ok(response);
    }
}
