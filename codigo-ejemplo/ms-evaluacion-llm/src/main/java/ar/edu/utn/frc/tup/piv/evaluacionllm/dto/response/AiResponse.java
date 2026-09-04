package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse<T> {

    @JsonProperty("resultado")
    private T resultado;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
