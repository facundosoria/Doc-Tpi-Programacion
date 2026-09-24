package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent;

/**
 * Cita de documento y página extraída de fragmentos RAG (HU LLM-S18-H01 · T1).
 */
public record MentionCitationDto(
    String documentName,
    int pageNumber,
    String excerpt
) {}
