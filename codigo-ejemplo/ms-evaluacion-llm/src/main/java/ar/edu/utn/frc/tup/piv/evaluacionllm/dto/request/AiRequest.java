package ar.edu.utn.frc.tup.piv.evaluacionllm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest<T> {

    @Valid
    @NotNull(message = "El contexto es obligatorio")
    @JsonProperty("contexto")
    private ContextoDto contexto;

    @Valid
    @NotNull(message = "El payload es obligatorio")
    @JsonProperty("payload")
    private T payload;

    @Builder.Default
    @JsonProperty("modo")
    private String modo = "sync";

    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}
