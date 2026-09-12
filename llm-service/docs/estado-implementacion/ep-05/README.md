# EP-05 · Tutor seguro y guardarraíles — estado

> **Primera vez que EP-05 tiene código en `llm-service`** (2026-09-12). No existe todavía
> `docs/historias/ep-05/` — esta ficha de estado no reemplaza esa historia pendiente; documenta
> lo que ya se construyó contra el contrato ya acordado
> ([`docs/contracts/llm-service-v1.openapi.yaml`](../../contracts/llm-service-v1.openapi.yaml) +
> [adenda SSE](../../contracts/llm-service-v1-tutor-sse-adenda.md)), portado de
> [`codigo-ejemplo/ms-evaluacion-llm`](../codigo-ejemplo/ms-evaluacion-llm.md) (carpeta ya
> eliminada).

## Índice

| Endpoint | Código | Estado | Nota |
|---|---|---|---|
| [interactions](interactions.md) | `TutorInteractionController`, `TutorInteractionService` | 🟡 | Camino síncrono completo con guardarraíles; streaming/SSE fuera de esta pasada |

## Pendiente (no se hizo en esta pasada, dejarlo dicho en vez de inventarlo)

- **Historia de usuario formal** para EP-05 (`docs/historias/ep-05/`) — este código se adelantó
  sin ficha, igual que pasó con golden set/calibración tras `605f381`.
- **Streaming SSE** (`POST /tutor/interactions/stream`, Buffer Interceptor PAR-11) — la adenda
  dice explícitamente que hasta que se fusione, el contrato ejecutable es solo el síncrono.
- **Adaptador real a un proveedor** (langchain4j, ADR-016) — hoy `FakeModelAdapter`
  ([`ep-02/h10.md`](../ep-02/h10.md)) es el único adaptador; ningún proveedor real está conectado.
- **Moderador** (`EP-08`) — el contrato de referencia se preservó en
  [`docs/contracts/llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml)
  pero no tiene código.
