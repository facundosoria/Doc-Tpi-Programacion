package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta paginada para el listado de incidentes de moderación (LLM-S12-H02 / CA1).
 */
public record ModerationIncidentPageResponse(
        @JsonProperty("content")
        List<ModerationIncidentResponse> content,

        @JsonProperty("page")
        int page,

        @JsonProperty("size")
        int size,

        @JsonProperty("total_elements")
        long totalElements,

        @JsonProperty("total_pages")
        int totalPages
) {
}
