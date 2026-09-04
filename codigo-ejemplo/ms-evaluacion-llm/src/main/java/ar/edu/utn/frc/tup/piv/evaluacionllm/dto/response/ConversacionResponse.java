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
public class ConversacionResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("curso_cohorte_id")
    private String cursoCohorteId;

    @JsonProperty("usuario_ref")
    private String usuarioRef;

    @JsonProperty("desafio_id")
    private String desafioId;

    @JsonProperty("titulo")
    private String titulo;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("estado")
    private String estado;
}
