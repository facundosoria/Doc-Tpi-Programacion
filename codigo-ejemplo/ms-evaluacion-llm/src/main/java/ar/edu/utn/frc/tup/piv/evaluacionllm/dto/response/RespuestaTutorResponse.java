package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaTutorResponse {

    @JsonProperty("respuesta")
    private String respuesta;

    @JsonProperty("estado")
    private String estado; // "OK", "BLOCKED", "DEGRADED"

    @JsonProperty("modelo")
    private String modelo;

    @JsonProperty("conversacion_id")
    private UUID conversacionId;

    @JsonProperty("mensaje_alumno_id")
    private UUID mensajeAlumnoId;

    @JsonProperty("mensaje_tutor_id")
    private UUID mensajeTutorId;
}
