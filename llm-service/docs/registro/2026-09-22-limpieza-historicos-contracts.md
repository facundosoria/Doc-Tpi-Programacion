# Limpieza de históricos en `docs/contracts/` — 2026-09-22

## Decisión anterior

`docs/contracts/` conservaba tres piezas históricas: `90-mapa-de-integracion-historico.md`,
`91-contratos-inter-equipos-historicos.md` y la carpeta `historicos-y-contratos-v1/` (OpenAPI y
AsyncAPI v1/v2, adendas y el estándar Kafka anterior). Estaban citadas desde ~50 archivos del
repositorio, la mayoría como nota al pie ("más detalle en doc 17/18").

## Motivo

El usuario pidió evaluar si ese histórico cumplía alguna función para implementar hoy. Se
auditaron las citas una por una: casi todas eran notas al pie redundantes — el contenido
efectivamente accionable (los ítems de agenda I-04, I-05, I-08, I-09, I-14, I-06, I-15) ya estaba
descripto completo y vigente en
[`01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md`](../01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md)
y en [`07-planificacion-y-trabajo-equipo/11-equipos/README.md`](../07-planificacion-y-trabajo-equipo/11-equipos/README.md).
Los schemas OpenAPI/AsyncAPI v1/v2 tampoco aportaban nada que el schema ejecutable vigente no
tuviera ya, más completo.

**Excepción encontrada durante la limpieza:** `llm-service-v1-tutor-sse-adenda.md` no era
histórico — describía una propuesta de streaming SSE para el tutor todavía **abierta** (ítem
I-10, "propagar o revertir", sin cerrar). Se rescató antes de borrar la carpeta.

## Regla vigente

- `90-mapa-de-integracion-historico.md`, `91-contratos-inter-equipos-historicos.md` y toda
  `historicos-y-contratos-v1/` (salvo la excepción de abajo) quedaron **eliminados**.
- `llm-service-v1-tutor-sse-adenda.md` se renombró y movió a
  [`contracts/llm-service-tutor-interactions-stream-propuesta.md`](../contracts/llm-service-tutor-interactions-stream-propuesta.md),
  marcado explícitamente como **propuesto, no fusionado** (no histórico).
- Las ~55 citas a los archivos eliminados se resolvieron caso por caso:
  - Citas a `llm-service-v1.openapi.yaml` / `.asyncapi.yaml` / `v2-golden-set.openapi.yaml` →
    redirigidas al schema vigente (`llm-service.openapi.yaml` / `.asyncapi.yaml`).
  - Citas a `llm-service-v1-moderacion-borrador.yaml` → redirigidas a
    `llm-service-v1-moderacion.openapi.yaml` (el contrato de moderación vigente).
  - Citas puntuales a `90-mapa-de-integracion-historico.md` / `91-contratos-inter-equipos-historicos.md`
    → des-linkeadas (se conserva la mención "doc 17"/"doc 18" como texto, sin enlace roto).
  - Citas a la adenda de Golden Set de Sprint 1 (`llm-service-v1-s1-golden-set-adenda.md`,
    genuinamente histórica, sin equivalente vigente) → des-linkeadas.
  - Punteros genéricos a la carpeta (link con texto "contracts/" pero destino apuntando a
    `historicos-y-contratos-v1`) → corregidos para apuntar a `docs/contracts/` real.
- `docs/contracts/README.md`, `equipos/README.md`, `catalogo-documental.md` y el `README.md` raíz
  de `docs/` ya no listan ni ejemplifican con la carpeta eliminada.

## Documentos corregidos o agregados

**Eliminados:** `contracts/90-mapa-de-integracion-historico.md`,
`contracts/91-contratos-inter-equipos-historicos.md`, `contracts/historicos-y-contratos-v1/`
(8 archivos).

**Movido:** `contracts/historicos-y-contratos-v1/llm-service-v1-tutor-sse-adenda.md` →
`contracts/llm-service-tutor-interactions-stream-propuesta.md`.

**Actualizados (≈35 archivos):** todos los que citaban alguno de los documentos retirados —
incluye `contracts/README.md`, `contracts/equipos/README.md`, `contracts/KAFKA_EVENT_STANDARD.md`,
`contracts/llm-service.asyncapi.yaml` (comentarios), `catalogo-documental.md`, `README.md` (raíz de
`docs/`), `00-gobierno-y-evolucion/09-deteccion-de-repeticiones.md`,
`00-gobierno-y-evolucion/04-matriz-trazabilidad.md`, `01-vision-alcance-y-entrega/03-entregas/sesion-integracion-agenda.md`,
`07-planificacion-y-trabajo-equipo/01-backlog-y-sprints.md`, `07-planificacion-y-trabajo-equipo/11-equipos/README.md`
y sus `pendientes.md`, `contracts/equipos/tema-02/03/05/11/12*.md`, y seis archivos de
`08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/` (material histórico
importado que también citaba los archivos retirados).

## Segunda pasada — contenido histórico embebido dentro de archivos vigentes

El usuario pidió una segunda revisión: que todo lo que quede en `docs/contracts/` sea "su única y
última versión", no solo que no haya *carpetas* históricas. Se encontró contenido retirado todavía
mezclado **dentro** de archivos por lo demás vigentes:

- **`equipos/tema-03-motor-de-desafios.md`** tenía 140 de 163 líneas dedicadas al contrato directo
  anterior con Tema 03 (endpoints `/ai/**`, payloads `snake_case`), marcado "retirado" pero
  presente completo. Se recortó a solo la decisión vigente (sin contrato directo, todo pasa por
  Tema 05); el detalle del contrato viejo queda documentado acá, no en `equipos/`.
- **`equipos/tema-11-chat.md`** describía un borrador obsoleto (`POST /ai/moderador`,
  `RespuestaModeracion` con `severidad`/`categorias` en español) como si fuera el contrato,
  con un aviso arriba avisando que era obsoleto pero sin reemplazarlo. Se reescribió contra el
  schema ejecutable real (`llm-service-v1-moderacion.openapi.yaml`): endpoint
  `POST /moderation/v1/decisions`, campos reales (`decision`, `reason_code`, `classifier_used`,
  `degradation_reason`), timeout real de 800 ms (el documento viejo decía 300 ms), y el diagrama
  de secuencia actualizado a la terminología real.
- **`skillhub/pendiente-revision-llm-service-kafka-contract.md`** tenía la revisión v5 vigente
  seguida de un bloque completo "texto anterior (v4)" duplicado. Se recortó a solo el estado
  vigente.
- **`skillhub/pendiente-revision-llm-service-http-contract.md`** tenía un título y un "Estado"
  que decían "pendiente de aceptación" cuando en realidad ya estaba publicado (v4, sin cambios,
  según el propio `skillhub/README.md`). Se corrigió el encabezado.

Se corrigieron además 4 rutas rotas a `llm-service/docsV3/...` (el nombre de carpeta anterior al
rename a `docs/`) en `MOCK.md` y tres archivos de `skillhub/`, más dos referencias residuales al
mismo nombre viejo fuera de `contracts/`: el título de `docs/README.md` (decía "# docsV3 — guía de
entrada...") y un diagrama de árbol en
`02-arquitectura-y-plataforma/04-estructura-del-backend.md` (decía `docsV3/` y `demo/` en vez de
`docs/` y `lab/`).

## Evidencia de prueba

Barrido de los 2995 enlaces relativos de `docs/` antes y después: 0 enlaces rotos causados por
esta limpieza (verificado con un script propio que resuelve cada enlace Markdown contra el
filesystem). El único enlace roto preexistente y ajeno a esta tarea
(`06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-01/h08.md` → un
`CORRECCIONES-SUGERIDAS.md` de 13 ítems que ya no existe en ninguna rama consolidada) sigue
pendiente, señalado, no resuelto por falta de un destino inequívoco.
