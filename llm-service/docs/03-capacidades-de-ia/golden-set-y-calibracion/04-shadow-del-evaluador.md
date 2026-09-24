# 04 — Shadow del evaluador (E-31)

> **Estado:** ✅ **Fase 1 implementada** (2026-09-19) · ⬜ **Fase 2 bloqueada** por el pipeline de evaluación real.
> Cierra el ítem **E-31** de [`decisiones y pendientes`](../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md)
> y el punto **A-2** de [`sincronización de arquitectura y despliegue`](../../02-arquitectura-y-plataforma/02-sincronizacion-arquitectura-y-despliegue.md).

## Qué es (y qué no es)

La unidad de despliegue nos dio *shadow deployment*: duplicar tráfico real hacia la versión nueva y **descartar
su respuesta**. Para `llm-service` se adoptó **para calibrar, no para desplegar**: validar una `rubric_version`
nueva contra transcripciones reales antes de tocar la nota de nadie. El riesgo que la unidad marca —duplicar
operaciones que escriben— no aplica porque **la evaluación en sombra no emite score**.

No confundir con el shadow de *infraestructura* (segunda instancia del servicio detrás de un proxy, en
`llm-service/scripts/shadow/` y `compose.shadow.yaml`): ese sirve para probar una *build* nueva, no una rúbrica.

## Las tres preguntas de E-31

| Pregunta | Decisión |
|---|---|
| **¿Dónde se activa?** | Como un **job aparte** (`shadow_run`), pedido por un docente del curso con `POST /courses/{courseId}/shadow-runs`. No es un flag en el camino de evaluación, así que no puede afectar una evaluación real (RF-IA-27). Lo ejecuta un worker del scheduler, de a una corrida. |
| **¿Dónde va la salida descartada?** | Solo a `shadow_runs` y `shadow_case_results` (migración `V37`). **Nunca** al outbox/Kafka, a `pending_evaluations` ni a `calibration_*`: lo hace cumplir `ArchitectureTest` (el paquete `shadow` no puede depender de ellos). No se guarda la transcripción: solo la referencia a la fuente y los puntajes. Retención 30 días (`llm.shadow.retention-days`). |
| **¿Cómo se compara?** | Cada transcripción se evalúa con la rúbrica **baseline** (la de la calibración activa del curso) y con la **candidata**, con la misma función `EVALUATOR` y el mismo modelo, así la diferencia es atribuible a la rúbrica. Ver métricas abajo. |

## Cómo funciona (Fase 1: replay)

Reproduce transcripciones **ya guardadas**; no depende de que exista el flujo de evaluación de intentos reales.

1. `POST /courses/{courseId}/shadow-runs` (`Idempotency-Key` obligatoria) con `candidateRubricVersionId`, `source`
   (`GOLDEN_SET` con `goldenSetVersionId`, o `TUTOR_CONVERSATIONS`), `sampleSize` y `divergenceThreshold` opcionales.
   Responde `202`. Exige que el curso tenga una calibración activa (esa rúbrica es la baseline), que la candidata sea
   otra distinta y que candidata y golden set sean visibles para el curso (de la plataforma ya publicados, o propios).
2. El worker toma la corrida (`QUEUED → RUNNING`). Por cada muestra **anonimiza** la transcripción
   (`RealCaseAnonymizer`), la evalúa con las dos rúbricas y guarda ambos puntajes.
3. Un caso que falla (respuesta no JSON, timeout, etc.) se guarda con `EVALUATION_FAILED` y **no tumba la corrida**.
4. Al final calcula el resumen y pasa a `COMPLETED` (o `FAILED` con `NO_SAMPLES`, `INVALID_RUBRIC` o
   `SHADOW_EXECUTION_FAILED`). Una corrida RUNNING que no avanza 15 minutos se reclama de nuevo.
5. `GET /courses/{courseId}/shadow-runs/{runId}` devuelve el estado y el resumen.

### Fuentes

| `source` | De dónde salen las transcripciones | Nota humana |
|---|---|---|
| `GOLDEN_SET` | Los casos de una versión del golden set | Sí: se mide también el error de cada rúbrica contra la nota humana |
| `TUTOR_CONVERSATIONS` | Conversaciones reales del tutor del curso (`conversations`/`messages`, más recientes primero; se omiten las de menos de 2 mensajes) | No |

### Métricas

El puntaje final de cada versión usa **los pesos de su propia rúbrica** (cambiar pesos es justamente uno de los cambios
que se quiere detectar). Divergencia de un caso = `candidata − baseline`.

| Métrica | Significado |
|---|---|
| `mae` | Promedio de la divergencia absoluta |
| `bias` | Promedio de la divergencia con signo: `> 0` la candidata puntúa más alto, `< 0` más bajo |
| `maxDivergence`, `shareOverThreshold` | Peor caso y proporción de casos que superan `divergenceThreshold` (10 por defecto) |
| `baselineMaeVsHuman`, `candidateMaeVsHuman` | Solo con `GOLDEN_SET`: qué tan cerca de la nota humana queda cada rúbrica |
| `mostDivergent` | Los 5 casos con más divergencia, para revisarlos a mano |
| `recommendation` | `PROCEED_TO_CALIBRATION`, `REVIEW` o `INSUFFICIENT_DATA` |

> ⚠️ **Los umbrales de la recomendación son propuestos, no acordados.** `PROCEED_TO_CALIBRATION` exige
> `mae ≤ 5`, `|bias| ≤ 3` y `≤ 20 %` de casos fallidos (`llm.shadow.max-mae`, `max-abs-bias`, `max-failed-share`).
> Los tomé del límite de MAE de PAR-14 y de criterio propio; el equipo tiene que confirmarlos. Son un consejo para el
> docente: **el shadow no activa ninguna rúbrica**. Si recomienda avanzar, lo que sigue es la calibración PAR-14 formal.

## Garantías

- **No emite scores**: escribe solo en sus tablas (`ArchitectureTest.shadowCannotEmitEventsNorTouchRealCalibrationOrEvaluationState`;
  la IT verifica que `event_outbox`, `calibration_case_results` y `pending_evaluations` no cambian).
- **Privacidad**: anonimiza antes de salir al proveedor; no persiste transcripciones; retención corta con purga diaria.
- **Aislado por curso**: candidata y golden set deben ser visibles para el curso; solo docentes del curso.
- **Kill switch**: `llm.shadow.enabled=false` apaga el worker.
- **Acotado**: `llm.shadow.max-sample-size` (100) limita las llamadas; cada muestra cuesta **dos** llamadas al evaluador.

## Fase 2 — sobre tráfico real (bloqueada)

Duplicar cada evaluación de un intento real (encolar una copia con la candidata, con muestreo) **no se puede enganchar
todavía**: en `llm-service` la evaluación de intentos reales no está implementada. `PracticeAttemptClosedListener` solo
deduplica y loguea el evento, y no hay tabla de evaluaciones ni `GET /evaluations/{id}`. Se retoma cuando exista ese
pipeline (dependencia del contrato de cierre de intento con Tema 05). El diseño de la Fase 1 ya deja la comparación y las
tablas listas: la Fase 2 solo agrega una **fuente** más (`ShadowSampleSource`) y el muestreo.

## Limitaciones conocidas

- Solo compara **`rubric_version`**. El `prompt_version` no está versionado (el prompt del evaluador se arma en código,
  en `EvaluatorPrompt`); comparar prompts exige primero versionarlos.
- En `TUTOR_CONVERSATIONS` se toma `course_cohort_id` como el curso de la corrida, y el contexto del desafío se reduce
  al id (el servicio no guarda el enunciado).
- Las corridas se ejecutan de a una, en un solo hilo del scheduler.

## Dónde está en el código

`llm-service/src/main/java/ar/edu/utn/frc/tup/piv/llm/shadow/` (`domain`, `application`, `infrastructure`, `api`),
migración `V37__shadow_evaluation.sql`, contrato en [`llm-service.openapi.yaml`](../../contracts/llm-service.openapi.yaml)
(`/shadow-runs`). Tests: `shadow/**` (unitarios) y `it/ShadowPersistenceIT` (Postgres real).
