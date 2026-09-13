# Tareas SMART — Catálogo de adaptadores de modelo real (sin ficha de historia)

> **Sin historia padre formal.** Igual que [`golden-set-update-proposal`](../ep-03/golden-set-update-proposal.md)
> o [`synthetic-golden-set`](../ep-03/synthetic-golden-set.md), esta pieza de código existe sin
> que ninguna HU la reclame. Nace del hueco auditado en
> [`estado-implementacion/ep-02/model-deployments.md`](../../estado-implementacion/ep-02/model-deployments.md)
> y es el ítem 4 de [`entregas/entrega-1.md` · §4](../../entregas/entrega-1.md). Pareja **P2**
> (Modelos y resiliencia, [23 · §3](../../23-plan-construccion-producto-llm.md)). Formato del
> [template de Tarea de Taiga](../../plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)).
>
> **Antes de escribir la historia formal (si se decide escribirla):** confirmar con quien
> mantiene el módulo si esto amerita una HU propia para Taiga o si queda como tarea técnica
> colgando de EP-02 — no se decide en este documento.

### T1 — `listAdapters` lee el catálogo real en vez de hardcodear

- **Específica:** reemplazar la lista literal (`"openai"`, `"anthropic"`) de
  `ModelDeploymentController.listAdapters` por una lectura de la tabla `model_adapters`
  (migración `V2`, ya existe), con un `id` estable entre llamadas (no `UUID.randomUUID()` por
  request).
- **Medible:** dos llamadas seguidas a `GET /api/llm/admin/model-adapters` devuelven los mismos
  `id` para el mismo adaptador.
- **Alcanzable:** 6 h, una persona de P2.
- **Relevante:** cierra la contradicción con RF-IA-11 ("editable por ADMIN, vía registro — nunca
  en el código").
- **Acotada:** 6 h · paso *adaptadores*. **Depende de:** — · **Traza:**
  [`model-deployments.md` · punto 1](../../estado-implementacion/ep-02/model-deployments.md).

### T2 — ~~Sembrar un `model_deployment` para el curso de demo~~ — descartada

> **Resuelto el 2026-09-12, leyendo el código** (ver
> [`s03-h01.md` · T7`](../ep-04/s03-h01.md)): `CalibrationRunWorker` invoca modelos vía
> `ModelInvocationService`/`function_model_config` — el mismo patrón que ya usa `TutorInteractionService`
> — y **no lee `model_deployments` en ningún punto del camino**. La fila `('evaluator', 'fake', ...)`
> que hace falta para que T7 funcione se siembra **dentro de T7** (en `function_model_config`, no
> acá). Esta tarea no aplica a la Entrega 1: **se retira sin ejecutar**, no cuenta en el total.

- **Alcanzable:** 0 h (descartada). El total de este archivo baja de 12 h a **8 h** para T1+T3.

### T3 — Documentar `GET .../model-deployments` en el contrato v2

- **Específica:** agregar `GET /api/llm/courses/{courseId}/model-deployments` a
  `llm-service-v2-golden-set.openapi.yaml`, que hoy no lo describe pese a que el endpoint existe
  y funciona.
- **Medible:** el contrato v2 valida y describe el endpoint con su forma de respuesta real.
- **Alcanzable:** 2 h, una persona de P2.
- **Relevante:** cierra uno de los "tres endpoints reales que no están en el contrato v2" que
  señala [`ep-01/h08.md`](../../estado-implementacion/ep-01/h08.md).
- **Acotada:** 2 h · paso *contrato*. **Depende de:** T1 · **Traza:**
  [`checklist-cierre-s1.md` §2.5](../../entregas/checklist-cierre-s1.md).

### T4 — Implementar `POST /api/llm/admin/model-adapters` (alta real de adaptador)

> **Aceptada como ampliación del sprint de cierre** (2026-09-12,
> [`backlog-priorizado-cierre-s1.md`](../../entregas/backlog-priorizado-cierre-s1.md)): P2 tiene
> margen de sobra en este ciclo (T1–T3 ocupan 12 h de ~116 h) y es la única de las 4 ampliaciones
> candidatas que repara una **capacidad real del producto**, no solo documentación o evidencia —
> hoy RF-IA-11 ("modelo por función, editable por ADMIN, nunca en el código") es falso mientras
> el alta de adaptadores no exista.

- **Específica:** implementar `POST /api/llm/admin/model-adapters` tal como lo describe
  `llm-service-v2-golden-set.openapi.yaml` (el contrato ya lo promete): recibe proveedor, modelo
  y metadata, inserta en `model_adapters` y devuelve el recurso creado con `id` estable.
- **Medible:** un `POST` válido crea una fila en `model_adapters` que `listAdapters` (T1) devuelve
  en la siguiente consulta con el mismo `id`; un `POST` con proveedor/modelo duplicado se rechaza
  sin crear una segunda fila.
- **Alcanzable:** 12 h, una persona de P2.
- **Relevante:** cierra el segundo de los "dos endpoints del contrato v2 no implementados" que
  señala [`ep-01/h08.md`](../../estado-implementacion/ep-01/h08.md); sin esto, ADMIN sigue sin
  poder agregar un modelo real sin que alguien de P2 lo hardcodee y redespliegue.
- **Acotada:** 12 h · paso *caso de uso + seguridad (rol ADMIN)*. **Depende de:** T1 · **Traza:**
  [`model-deployments.md` · punto 3](../../estado-implementacion/ep-02/model-deployments.md).
- **Estado:** 🟡 aceptada para el sprint de cierre — no es bloqueante de Entrega 1, es ampliación.

> **Total (referencia, actualizado 2026-09-12 al descartar T2):** T1+T3 = **8 h** (deuda de
> catálogo, ya no bloqueante de Entrega 1 — ver nota de T2) + **T4 = 12 h** (ampliación aceptada)
> = **20 h**.
