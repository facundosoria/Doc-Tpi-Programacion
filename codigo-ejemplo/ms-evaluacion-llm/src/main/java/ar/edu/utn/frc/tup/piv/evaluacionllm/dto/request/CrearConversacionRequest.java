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
public class CrearConversacionRequest {

    @NotBlank(message = "curso_cohorte_id es obligatorio")
    @JsonProperty("curso_cohorte_id")
    private String cursoCohorteId;

    @NotBlank(message = "usuario_ref es obligatorio")
    @JsonProperty("usuario_ref")
    private String usuarioRef;

    @JsonProperty("desafio_id")
    private String desafioId;

    @JsonProperty("titulo")
    private String titulo;
}
