package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeAlumnoRequest {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @JsonProperty("contenido")
    private String contenido;
}
