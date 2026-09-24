package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.util.List;

/**
 * Envoltorio paginado agnóstico de frameworks para capas de aplicación y dominio (ADR-019).
 */
public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResult<>(
                content != null ? content : List.of(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
