package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadTutorDto {

    @JsonProperty("conversacion_id")
    private UUID conversacionId;

    @NotBlank(message = "El mensaje del alumno es obligatorio")
    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("titulo")
    private String titulo;
}
