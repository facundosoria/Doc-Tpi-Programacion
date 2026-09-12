# Catálogo de despliegues de modelo — sin ficha de historia

- **Estado:** 🔴 Hueco (parte hardcodeada), 🟢 parte real y correcta
- **Épica tentativa:** EP-02 (tabla de
  [`verificacion-v2-golden-set-calibracion.md` · §1](../../entregas/verificacion-v2-golden-set-calibracion.md))
- **Código:** `ModelDeploymentController.java`
- **Evidencia:** [`CORRECCIONES-SUGERIDAS.md` ítem 3](../../../CORRECCIONES-SUGERIDAS.md)

## Qué hay en el código

- `GET /api/llm/admin/model-adapters` (`listAdapters`) devuelve una lista **hardcodeada en el
  código** (`"openai"`, `"anthropic"`, literal en Java) con `id = UUID.randomUUID()` — un id
  **distinto en cada pedido**, que no sirve como identificador real de nada. Contradice
  RF-IA-11 ("modelo por función, editable por ADMIN, vía registro — no en el código"). La tabla
  real `model_adapters` (migración `V2`) existe pero este endpoint no la lee.
- `GET /api/llm/courses/{courseId}/model-deployments` (`listForCourse`) **sí lee la tabla real**
  (`repository.listEnabledDeployments()`) y hoy devuelve una lista vacía porque no hay filas
  sembradas — eso es correcto, no un bug: todavía no hay proveedor real habilitado
  ([08 · B-6/C-2](../../08-decisiones-y-pendientes.md)).
- Ambos endpoints inyectan el `*Repository` directo en el controller, sin capa `application/`
  intermedia (patrón general de 7 de los 12 controllers, ver
  [`CORRECCIONES-SUGERIDAS.md` ítem 1](../../../CORRECCIONES-SUGERIDAS.md)).
- No está en ningún contrato OpenAPI publicado (`llm-service-v2-golden-set.openapi.yaml` no lo
  documenta) — ver [`ep-01/h08.md`](../ep-01/h08.md) ítem 11.
- El contrato v2 sí documenta `POST /api/llm/admin/model-adapters` (alta de adapter), pero **no
  existe ningún endpoint que lo implemente** — ver
  [`CORRECCIONES-SUGERIDAS.md` ítem 12](../../../CORRECCIONES-SUGERIDAS.md).

## Qué falta para cerrar el hueco

1. `listAdapters` debe leer `model_adapters` en vez de hardcodear proveedores, con un `id`
   estable entre llamadas.
2. Documentar `GET .../model-deployments` en el contrato v2.
3. Implementar el `POST /api/llm/admin/model-adapters` que el contrato v2 ya promete.
