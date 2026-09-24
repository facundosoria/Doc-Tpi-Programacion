# Historias de usuario — EP-09 · RAG y consulta de material (F3)

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). El catálogo original
> ([`35` · «S14»](../../../04-backlog-ejecutable.md)) ubica EP-09 en **S14–S16**, después de EP-08
> (moderación) y de que exista corpus autorizado. Estas dos historias se **adelantan** a la fecha
> de escritura (2026-09-12) por el mismo motivo que `LLM-S01-H10` (EP-02) se adelantó a S1: hay
> pedido explícito de portar una implementación de referencia ya construida y probada
> (`demoLLMSpringAi`), documentada como spike en
> [`docs/31-spike-decodificacion-imagenes-y-rag-multifuente.md`](../../../../03-capacidades-de-ia/rag-e-ingesta/02-spike-rag-multifuente.md).
> No depende de S14/S15 (moderación, EP-08) — depende únicamente de que exista `pgvector` en la
> base (ya lo trae `V28__tutor_conversations_and_rag.sql`).
>
> **Alcance de esta pasada vs. el paquete completo de S14/S15.** El catálogo de S14 pide
> `pgvector` + ONNX local para embeddings — acá solo hay un adaptador **fake** (`EmbeddingPort`,
> mismo estado que el AI Gateway de texto en H10). El catálogo de S15 pide **no** guardar
> binarios en Postgres ("hashes y referencias autorizadas"); esta pasada sí guarda el PDF como
> `BYTEA` en `rag_documents.pdf_bytes` — una decisión explícita de simplificación para esta
> etapa (ver [`docs/estado-implementacion/ep-09/README.md`](../../../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-09/README.md)),
> no una implementación completa de S15.
>
> **Fuente de verdad.** ID, épica y horas: no asignadas todavía en `35` para historias
> individuales de EP-09 (solo el paquete agregado de S14). Los IDs `LLM-S14-H01`/`H02` acá son
> provisorios, a confirmar cuando se planifique el sprint real.

## Índice

| ID | Título | Tipo | Dep. |
|---|---|---|---|
| [LLM-EP09-H01](h01.md) | Ingesta e indexado de un PDF como fuente de consulta *(ex-S14-H01)* | HU de valor (rol: docente) | `LLM-EP02-H01` (AI Gateway, patrón de puerto reutilizado) |
| [LLM-EP09-H02](h02.md) | Consulta al tutor con citas de fuente/página y abstención *(ex-S14-H02)* | HU de valor (rol: alumno) | H01 |

> No forman parte del criterio de demo de ningún sprint todavía asignado — se documentan como
> adelanto, igual que se hizo con H10 en su momento.
