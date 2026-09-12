# Entrega 1 — propuesta de alcance y estado real

> **Qué es.** Define qué mostrar como primera entrega del TP y en qué estado está cada
> pieza, cruzando la reprogramación a 8 semanas ([`../sprints/README.md`](../sprints/README.md))
> con lo que [verificacion-v2-golden-set-calibracion.md](verificacion-v2-golden-set-calibracion.md)
> encontró en el código real (post `605f381`, ver [decision-605f381.md](decision-605f381.md)).
>
> **Por qué existe.** El código ya entrega más que "solo S1" — golden set y rúbrica
> versionados, y el esqueleto de calibración. Definir la Entrega 1 recortada a "S1 tal cual
> lo dice la receta original" desperdiciaría trabajo real ya hecho y probado.

---

## 1. Lo que se puede demostrar hoy (con evidencia)

| Capacidad | Historia / origen | Evidencia |
|---|---|---|
| Un docente crea, versiona y publica un golden set de su curso | H05/H06 (evolucionadas) | `CourseGoldenSetController` + tests, ownership real |
| Un docente crea, versiona y publica una rúbrica de 5 dimensiones con pesos que suman 100 | H04 (evolucionada) | `RubricController`/`RubricValidator` + tests |
| Los datos de alumno se anonimizan antes de guardarse | Restricción de EP-03 | `RealCaseAnonymizer`, aplicado en golden set manual e importado |
| Nada se pierde ni se edita tras publicar (append-only real) | Restricción de EP-01/EP-03 | Trigger de base `prevent_published_version_mutation` |
| Importación masiva de casos con validación fila por fila | Sin ficha | `GoldenSetImportController`/`Service` |
| Se puede encolar una calibración de forma idempotente, con estado y auditoría | Sin ficha (S3) | `CalibrationRunController`/`Service`, `CalibrationStateMachine` |

**Esto ya es más de lo que la demo original de S1 prometía** ("un docente carga y consulta un
golden set que sobrevive al reinicio"). Se puede mostrar el ciclo completo de golden set +
rúbrica versionados, no solo la carga simple.

## 2. Lo que NO se puede demostrar todavía (y por qué)

| Falta | Por qué | Lo resuelve |
|---|---|---|
| Una calibración real que termine en `PASSED`/`FAILED` | Nada invoca a un modelo (ni real ni fake) — el run queda en `RUNNING` para siempre | `LLM-S01-H10` (ya adelantada a S1) + un paso nuevo que la conecte al workflow |
| Un catálogo de modelos real y editable | `GET /model-adapters` está hardcodeado, con IDs inestables | Punch-list ítem 2 de la verificación |
| Consultar el detalle de un golden set/rúbrica/calibración por id (si no existe hoy) | No se encontró el endpoint en esta revisión | A confirmar con el dueño del código |
| Interacciones elegibles reales o generación sintética real | Ambas son datos de muestra hardcodeados | Depende de integrar `practice-service` (fuera de alcance de esta entrega) y de un modelo real |

## 3. Alcance recomendado para "Entrega 1"

**No** es "S1 tal cual la receta original de 208 h". Es: **golden set + rúbrica versionados
(ya construido) + el puerto de invocación con fake (H10) + el paso que conecta calibración
con ese puerto**, para poder mostrar un recorrido de calibración completo, aunque sea contra
un modelo simulado.

Esto coincide con lo que la reprogramación a 8 semanas ya venía proponiendo (Pista A: golden
set/calibración; Pista B: modelo/tutor arrancando en paralelo desde S1) — con la diferencia de
que **Pista A ya está mucho más avanzada de lo previsto**, y lo que falta para cerrar el
círculo es más chico de lo que parecía: no hay que construir calibración desde cero, hay que
**conectarla** con H10.

### Criterio de demo propuesto para Entrega 1

> Un docente autorizado publica una rúbrica y un golden set de su curso, encola una
> calibración contra el adaptador simulado (H10), y la calibración termina con un resultado
> (`PASSED`/`FAILED`) y su métrica (MAE, error máximo por dimensión) visibles — sin haber
> gastado un centavo en un proveedor real.

### Qué queda fuera de Entrega 1 (explícitamente, para no prometerlo)

- Un proveedor de modelo real (bloqueado por B-6/C-2, decisiones legales/de producto pendientes).
- Interacciones elegibles e ingesta de casos sintéticos reales (dependen de `practice-service`
  y de un modelo real respectivamente).
- El tutor y la salvaguarda anti-fuga (S5 en la reprogramación; no depende de nada de lo de
  arriba, puede seguir avanzando en paralelo).

## 4. Trabajo concreto para cerrar Entrega 1

Del punch-list de la verificación, en orden de bloqueo:

1. **H10** — puerto de invocación + fake ([ya con ficha](../historias/ep-02/h10.md), 32 h).
2. **Conectar calibración con H10** — nueva pieza (sin ficha todavía) que, al recibir un run
   `RUNNING`, llame al puerto por cada caso del golden set, arme `CaseScores`, corra
   `CalibrationMetrics.assess` y persista `calibration_case_results` + el cierre del run.
   Necesita ficha (tarea SMART) antes de estimarla.
3. **404 real para "no existe"** — evita que la demo muestre `409` donde las fichas prometen
   `404`.
4. **Catálogo de adaptadores real** — que lea `model_adapters` en vez de hardcodear
   "openai"/"anthropic".
5. **Marcar los placeholders como tales** en cualquier guion de demo (interacciones elegibles,
   casos sintéticos).

## 5. Documentación a escribir (para que la entrega no quede "sin ficha")

- Historia(s) de rúbrica versionada por curso (evolución de H04).
- Historia(s) de golden set versionado por curso (evolución de H05/H06).
- Historia de calibración (run + activación + métrica PAR-14) — hoy sin ficha, y es la pieza
  central de la demo propuesta.
- Actualizar terminología: "cohorte" → "curso" en toda la documentación de EP-03/EP-04, una
  vez confirmado con Franco qué representa `courseId` ([08 · B-7](../08-decisiones-y-pendientes.md)).

---

¿Sigo con la ficha de tarea SMART del punto 2 (conectar calibración con H10), o preferís que
primero escriba las historias faltantes de rúbrica/golden-set-v2/calibración para que todo lo
de arriba tenga ficha antes de tocar código?
