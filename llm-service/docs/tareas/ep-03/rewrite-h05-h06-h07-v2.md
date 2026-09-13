# Tareas SMART — Reescribir H05/H06/H07 contra el código v2 real

> **Aceptada como ampliación del sprint de cierre** (2026-09-12,
> [`backlog-priorizado-cierre-s1.md`](../../entregas/backlog-priorizado-cierre-s1.md)). Origen:
> [`checklist-cierre-s1.md` §3.1](../../entregas/checklist-cierre-s1.md) y
> [`estado-implementacion/ep-03/h05-h06-h07.md`](../../estado-implementacion/ep-03/h05-h06-h07.md)
> (las tres fichas actuales describen `POST /api/llm/golden-sets` y el término "cohorte" — **ni
> un endpoint de esos existe hoy**; el código real es `CourseGoldenSetController` sobre
> `/api/llm/courses/{courseId}/golden-sets/...`). Pareja **P1** (rol de documentación/
> integración; no toca código de P5). Formato del
> [template de Tarea de Taiga](../../plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)).
>
> **Por qué importa a nivel proyecto, no solo de prolijidad:** cualquiera que abra
> `docs/historias/ep-03/` hoy —incluida la cátedra— encuentra fichas describiendo endpoints
> borrados hace tiempo. El riesgo no es solo que estén desactualizadas: es que hacen dudar de
> **toda** la documentación del servicio.
>
> **Regla de no tocar código ajeno:** estas tareas son 100% de lectura de código +
> escritura de documentación. Ninguna toca `llm-service/` ni `llm-workbench/`.

### T1 — Reescribir la ficha de H05 contra `CourseGoldenSetController`

- **Específica:** reemplazar en [`historias/ep-03/h05.md`](../../historias/ep-03/h05.md) las
  rutas y el término "cohorte" por el flujo real: crear golden set (borrador vacío o copiado
  desde base publicada), agregar caso con anonimización, sobre `/api/llm/courses/{courseId}/
  golden-sets/...`. Mantener el formato Como/Quiero/Para + CA + BDD del template de Taiga.
- **Medible:** cada ruta y cada término de la ficha reescrita se puede verificar con un `curl`
  real contra el código de `CourseGoldenSetController`.
- **Alcanzable:** 4 h, una persona de P1 (consultando a P5 dueño del código si hay dudas de
  comportamiento, sin pedirle que escriba la ficha).
- **Relevante:** es la ficha más citada desde otros documentos (contratos, checklist, entrega-1).
- **Acotada:** 4 h · paso *documentación*. **Depende de:** — · **Traza:**
  [`estado-implementacion/ep-03/h05-h06-h07.md`](../../estado-implementacion/ep-03/h05-h06-h07.md).

### T2 — Reescribir la ficha de H06 contra la consulta real (con `s02-h02` como base)

- **Específica:** reemplazar en [`historias/ep-03/h06.md`](../../historias/ep-03/h06.md) la
  consulta v1 por listar/detalle reales del flujo por curso, incluyendo el `404` real que debería
  quedar cerrado por [`ep-01/h03.md` T9](../ep-01/h03.md) — sin ese fix, la ficha reescrita
  seguiría prometiendo algo que el código no cumple todavía.
- **Medible:** los escenarios BDD reescritos (camino feliz + negativos) se pueden ejecutar contra
  el código real una vez que T9 de H03 esté hecha.
- **Alcanzable:** 4 h, una persona de P1.
- **Relevante:** es la historia canónica original (candidata a Planning Poker); que describa algo
  inexistente es el peor lugar para que pase esto.
- **Acotada:** 4 h · paso *documentación*. **Depende de:** T1 (mismo patrón de reescritura) ·
  **Traza:** [`estado-implementacion/ep-03/h05-h06-h07.md`](../../estado-implementacion/ep-03/h05-h06-h07.md).

### T3 — Reescribir la ficha de H07 contra la pantalla real de `llm-workbench`

- **Específica:** reemplazar en [`historias/ep-03/h07.md`](../../historias/ep-03/h07.md) la
  descripción de pantalla, confirmando que `llm-workbench` ya usa rutas relativas al Gateway
  (`/api/llm/courses/...`) — el hallazgo original de H07 (llamaba al servicio directo) ya está
  cerrado según [`verificacion-v2-golden-set-calibracion.md` §2`](../../entregas/verificacion-v2-golden-set-calibracion.md).
- **Medible:** la ficha reescrita describe la pantalla que existe hoy en `llm-workbench`, no la
  del golden set v1.
- **Alcanzable:** 3 h, una persona de P1.
- **Relevante:** cierra la última de las tres fichas históricas.
- **Acotada:** 3 h · paso *documentación*. **Depende de:** T1, T2 · **Traza:**
  [`estado-implementacion/ep-03/h05-h06-h07.md`](../../estado-implementacion/ep-03/h05-h06-h07.md).

### T4 — Verificación nueva que reemplace `ep-03-s1-verificacion.md`

- **Específica:** escribir una verificación nueva, historia por historia (H05/H06/H07
  reescritas), contra el código v2 actual — reemplazando a
  [`entregas/ep-03-s1-verificacion.md`](../../entregas/ep-03-s1-verificacion.md), que
  [`decision-605f381.md`](../../entregas/decision-605f381.md) ya declaró obsoleta.
- **Medible:** existe el documento nuevo, y `ep-03-s1-verificacion.md` queda marcado en su
  cabecera como reemplazado, con el link al nuevo.
- **Alcanzable:** 3 h, una persona de P1.
- **Relevante:** sin esto, "Entrega 1" seguiría citando una verificación que audita código
  borrado.
- **Acotada:** 3 h · paso *documentación*. **Depende de:** T1–T3 · **Traza:**
  [`decision-605f381.md` · §5](../../entregas/decision-605f381.md).

> **Total:** 14 h. No incluye [checklist §3.2](../../entregas/checklist-cierre-s1.md)
> (unificar "cohorte"/"curso" en el *resto* de la documentación) ni §3.3 (fichar el código de
> EP-04/EP-06 construido de más) — ambas quedaron explícitamente para el siguiente ciclo, no
> para esta ampliación.
