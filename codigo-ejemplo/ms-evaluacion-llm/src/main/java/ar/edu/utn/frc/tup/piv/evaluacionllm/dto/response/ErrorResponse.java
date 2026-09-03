package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("detalles")
    private List<String> detalles;

    @JsonProperty("trace_id")
    private String traceId;

    @Builder.Default
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
}
