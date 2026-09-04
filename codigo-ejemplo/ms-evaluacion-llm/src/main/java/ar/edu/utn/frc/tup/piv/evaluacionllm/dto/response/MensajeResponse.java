package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("rol")
    private String rol;

    @JsonProperty("contenido")
    private String contenido;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
