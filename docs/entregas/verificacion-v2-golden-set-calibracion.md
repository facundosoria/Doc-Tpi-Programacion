# Verificación — Golden Set, rúbrica y calibración v2 (post `605f381`)

> **Qué es.** Reemplaza a [ep-03-s1-verificacion.md](ep-03-s1-verificacion.md) (marcada
> obsoleta) como la verificación vigente del código de `llm-service`/`llm-workbench` tras el
> merge de `605f381` (ver [decision-605f381.md](decision-605f381.md)). Contrasta lo que hay
> en el repo **hoy** contra las fichas escritas ([`../historias/`](../historias/README.md)) y
> los invariantes del producto ([23 · §4.2](../23-plan-construccion-producto-llm.md)).
>
> **Qué NO es.** No modifica código — `llm-service/` y `llm-workbench/` tienen dueño (Franco
> Brizzio). Los hallazgos se listan para transcribir a
> `CORRECCIONES-SUGERIDAS.md` en cada módulo, como marca [`no-tocar-codigo-ajeno`].
>
> **Profundidad de esta revisión.** Lectura completa de: migraciones (`V1`–`V12`), la
> autorización (`CourseAuthorization`, `GoldenSetAuthorization`), el manejador de errores, y
> el servicio + un test representativo de cada uno de los 9 subsistemas nuevos. **No** es
> cobertura línea por línea de las ~30 clases nuevas ni de la superficie completa del
> frontend — donde no se profundizó, se dice explícitamente.
>
> **Fecha.** 2026-09-12. Rama `facu` (commit `23ea20f`), working tree limpio en
> `llm-service/`/`llm-workbench/`.

---

## 1. Resumen ejecutivo

El código va **muy por delante** de lo que documenta el backlog. `605f381` no completó S1:
lo reemplazó por un flujo de golden set + rúbrica **versionado y por curso**, con
**calibración** (estado, métrica PAR-14) y un **catálogo de despliegues de modelo**
ya scaffoldeados — piezas que las recetas de sprint asignan a S1, S2, S3 y pedazos de S4/EP-06.

| Subsistema | Historia que le corresponde hoy | Estado |
|---|---|---|
| Golden set por curso (draft/publish/versión/copia) | H05, H06 (EP-03) — y S2 sin ficha | 🟢 Sólido, con 1 hueco (ver §7) |
| Rúbrica versionada por curso | H04 (EP-01) — y S2 sin ficha | 🟢 Sólido, mismo hueco de 404 |
| Calibración (run, activación, métrica PAR-14) | **Sin ficha** (S3/EP-04) | 🟡 Esqueleto correcto, **falta la pieza que invoca un modelo** |
| Catálogo de despliegues de modelo | **Sin ficha** (EP-02) | 🟡 Expone datos reales pero el endpoint de adaptadores está hardcodeado |
| Estado de evaluación por curso | **Sin ficha** (S4/EP-06) | ⚪ No auditado en profundidad |
| Interacciones elegibles | **Sin ficha** | 🔴 Datos totalmente hardcodeados — no es una función real todavía |
| Generación de casos sintéticos | **Sin ficha** | 🔴 Ídem — 3 plantillas fijas, sin LLM real pese al nombre del campo |
| Importación masiva de golden set | **Sin ficha** | 🟢 Sólido |
| Autorización por curso | H05·CA3, H06·CA4 | ✅ **Resuelto** — era el bloqueante más grave de la revisión anterior |

**El hallazgo más importante:** el esqueleto de calibración (estado, métricas, idempotencia)
está bien construido, pero **nada en el código invoca todavía a un modelo de lenguaje, ni
siquiera uno simulado**. Es exactamente el hueco que `LLM-S01-H10` (agregada en la
reprogramación a 8 semanas) está pensada para cerrar. Sin H10 (o equivalente), una
calibración se queda en `RUNNING` para siempre.

---

## 2. Lo que ya está resuelto (vs. la revisión anterior)

1. **Ownership por curso** (bloqueaba H05·CA3 y H06·CA4): `CourseAuthorization.requireTeacher`
   exige rol docente (`X-User-Roles`) y pertenencia al curso (`X-Teacher-Course-Ids`,
   propagados por el Gateway). Devuelve `403` si no corresponde. ✅
2. **Anonimización real de datos de alumno**: `RealCaseAnonymizer` quita campos como
   `student_id`/`email` y sanitiza el texto libre con `TranscriptSanitizer` antes de guardar
   cualquier caso (golden set manual, importado o de interacciones elegibles). ✅
3. **Inmutabilidad de versiones publicadas**: no es solo una regla de servicio, es un
   **trigger de base de datos** (`prevent_published_version_mutation`) sobre
   `rubric_version_v2` y `golden_set_versions` — no se puede saltear ni por un bug de
   aplicación. ✅ Cumple el invariante de [23 · §4.2](../23-plan-construccion-producto-llm.md):
   *"un cambio conserva evidencia y versiones de resultados anteriores"*.
2. **Ruteo del frontend por el Gateway** (bloqueaba H07·CA2): `llm-workbench` ya usa rutas
   relativas (`/api/llm/courses/...`) en vez de `http://localhost:8080` hardcodeado. ✅ — a
   confirmar que el proxy/Gateway efectivamente resuelve esas rutas en el ambiente integrado.
3. **`DataIntegrityViolationException` manejada** con mensaje explicable (no `500` genérico).
   ✅ — pero ver §4 sobre el resto de los códigos de error.

---

## 3. El hueco crítico: calibración sin invocación de modelo

Recorrido del código:

- `CalibrationRunController.create` → `CalibrationRunService.enqueue`: crea el registro en
  `calibration_runs` con estado `QUEUED`, guarda un snapshot vacío y audita. **No llama a
  nada.**
- `CalibrationRunWorker.dispatch` (`@Scheduled`, cada 1 s por defecto): reclama el próximo
  `QUEUED` y llama a `CalibrationWorkflowService.start`, que solo hace la transición de
  estado `QUEUED → RUNNING`.
- Ahí termina. **No hay ningún código que llame a `CalibrationMetrics.assess`** (la función
  que sí implementa PAR-14 correctamente) con puntajes reales, ni que llene
  `calibration_case_results`, ni que transicione a `PASSED`/`FAILED`.

Consecuencia: **hoy, toda calibración que se encola queda en `RUNNING` para siempre.** El
estado, la métrica y el modelo de datos están listos y bien probados (`CalibrationMetricsTest`,
`CalibrationStateMachineTest`), pero les falta la pieza que los conecta con un modelo real o
simulado.

**Esto es exactamente `LLM-S01-H10`** (puerto de invocación + fake), que la reprogramación a
8 semanas ya adelantó a S1 ([h10.md](../historias/ep-02/h10.md)). Falta, además de H10, el
paso que hoy no tiene ficha: que algo (probablemente una extensión de
`CalibrationWorkflowService` o un nuevo `CalibrationRunner`) tome el puerto de H10, corra los
casos del golden set contra el fake, llene `calibration_case_results` y cierre el run con
`CalibrationMetrics.assess`.

---

## 4. Manejo de errores: 404 vs 409 — inconsistencia real

`ApiExceptionHandler` mapea hoy:

| Excepción | Código HTTP |
|---|---|
| `IllegalArgumentException` | 422 |
| `IllegalStateException` | 409 |
| `OptimisticLockException` | 409 |
| `DataIntegrityViolationException` | 422 |

**No hay ningún mapeo a 404.** Y los `get()` de los servicios auditados (`CourseGoldenSetService`
no tiene un `get` por versión; `RubricDraftService.get`, `CalibrationRunService.get`) lanzan
`IllegalStateException` cuando el recurso no existe — que hoy responde **409 Conflict**, no
**404 Not Found**.

Esto contradice directamente lo que piden las fichas:
- H06·CA5: *"`goldenSetId` con formato UUID pero inexistente → `404` Problem Details, nunca `500`"* — hoy sería `409`.
- El mismo patrón aplica a rúbricas y calibraciones, que ni ficha tienen todavía pero deberían
  seguir la misma convención una vez que se escriba.

**Recomendación para el punch-list:** una excepción dedicada (p. ej. `ResourceNotFoundException`)
mapeada a `404`, usada en los `get()` en vez de `IllegalStateException` cuando el motivo es
"no existe" (reservando `409` para conflictos de estado reales, como publicar una versión que
ya no es borrador).

---

## 5. Placeholders que parecen funciones reales — riesgo de confusión en una demo

Dos piezas devuelven **datos totalmente hardcodeados** y podrían presentarse por error como
funciones de IA ya construidas:

1. **`EligibleInteractionsService`** — dos interacciones fijas (UUIDs constantes, texto
   escrito a mano). No hay integración con `practice-service` ni con ningún historial de chat
   real. El *preview* de anonimización sí anonimiza de verdad esos datos de muestra.
2. **`SyntheticGoldenSetProposalService`** — tres plantillas fijas rotadas por índice
   (`i % 3`), con el campo `author` seteado a `"Generador Asistido / LLM"` **sin que ningún
   LLM las genere**. Ese nombre de campo es el riesgo concreto: alguien que lo lea puede creer
   que ya hay un modelo generando casos.

**No son errores** — son placeholders razonables mientras las integraciones reales
(`practice-service`, un modelo real) no existen. Pero hay que **decirlo explícitamente** en
cualquier entrega o demo: si se muestran, aclarar que son datos de ejemplo, no una función de
IA operando.

---

## 6. Catálogo de modelos: no lee su propia tabla

`ModelDeploymentController.listAdapters` (`GET /api/llm/admin/model-adapters`) arma la
respuesta así:

```java
List.of(
  new ModelAdapterSummary(UUID.randomUUID(), "openai", ...),
  new ModelAdapterSummary(UUID.randomUUID(), "anthropic", ...)
)
```

Dos problemas concretos:
1. **Los nombres de proveedor están hardcodeados en el código**, no salen de la tabla
   `model_adapters` que la migración `V2` sí crea. Contradice RF-IA-11 ("modelo por función,
   editable por ADMIN, vía registro — no en el código").
2. **El `id` de cada adaptador es un `UUID.randomUUID()` distinto en cada pedido** — no es
   estable entre llamadas, así que no sirve como identificador real de nada.

`GET /api/llm/courses/{courseId}/model-deployments` sí lee de la tabla real
(`repository.listEnabledDeployments()`), y como no hay ninguna fila sembrada (correcto:
todavía no hay proveedor real habilitado, ver B-6/C-2 de
[08](../08-decisiones-y-pendientes.md)), hoy devuelve una lista vacía.

---

## 7. Lo que quedó sin auditar en profundidad esta vez

- `CourseEvaluationStatusController`/`Repository` — la forma de los tres endpoints es
  coherente con el esquema (`active_calibrations`, `challenge_calibration_assignments`,
  `pending_evaluations`), pero no se revisaron sus queries línea por línea.
- El resto de la superficie de `llm-workbench` (estados de carga/error, accesibilidad de las
  páginas nuevas `summary-page`, `calibrations-page`, `assignments-page`).
- Si existe o no un `GET` de un golden set / rúbrica / calibración **individual** por id (más
  allá del `list`) — no se encontró en `CourseGoldenSetController` ni `RubricController` en
  esta pasada; si no existe, varias fichas (H06 en particular) necesitan revisarse contra el
  endpoint real.
- Ejecución real de la suite de tests (Testcontainers/Flyway) — no se corrió en esta revisión;
  se recomienda correrla en CI antes de dar por cerrado cualquier punto de este documento.

---

## 8. Punch-list para `CORRECCIONES-SUGERIDAS.md`

**Bloqueantes para una demo real de calibración (S3):**

1. Conectar `CalibrationRunWorker`/`CalibrationWorkflowService` con un invocador de modelo
   (H10 + un paso nuevo que corra `CalibrationMetrics.assess` sobre los resultados) para que
   un run pase de `RUNNING` a `PASSED`/`FAILED`.
2. `ModelDeploymentController.listAdapters`: leer proveedores de `model_adapters` en vez de
   hardcodear "openai"/"anthropic"; devolver un `id` estable.

**Para que las fichas existentes (H05, H06) sean verificables tal como están escritas:**

3. Mapear "no existe" a `404` (no `409`) — nueva excepción dedicada en `ApiExceptionHandler`,
   usada donde el motivo real es "recurso inexistente".
4. Confirmar si existe (o falta) un `GET` de detalle por id de golden set/rúbrica/calibración;
   si falta, decidir si se agrega o si las fichas se actualizan para reflejar que la
   consulta es solo por listado.

**Para que una demo no confunda placeholder con función real:**

5. Marcar explícitamente en la UI (o en cualquier guion de demo) que
   `EligibleInteractionsService` y `SyntheticGoldenSetProposalService` devuelven datos de
   muestra, no una integración ni una generación real.

**Documentación pendiente (no es código):**

6. Escribir las historias que faltan para lo que el código ya construyó: rúbrica versionada,
   calibración (run + activación + métrica), catálogo de despliegues de modelo — hoy son
   funcionalidad sin ficha, lo que hace imposible auditarlas contra un CA acordado.
7. Unificar terminología: las historias dicen "cohorte", el código dice "curso" de punta a
   punta. Adoptar "curso" en la documentación, confirmando antes con Franco si `courseId` es
   el curso-plantilla o la cohorte/oferta concreta ([08 · B-7](../08-decisiones-y-pendientes.md)).
8. **Confirmar el invariante de pesos de la rúbrica.** [23 · §4.2](../23-plan-construccion-producto-llm.md)
   fija 30/25/20/15/10 como invariante de producto; `RubricValidator` solo exige que las cinco
   dimensiones sumen 100, sin fijar esos valores concretos. Decidir si eso es correcto (cada
   curso puede variar sus pesos) o si falta una validación. Detalle en
   [`LLM-S02-H01`](../historias/ep-03/s02-h01.md).
9. **Confirmar el alcance de calibración de plataforma vs. de curso.** La épica EP-04 describe
   ambos niveles; el código solo implementa el de curso. Detalle en
   [`LLM-S03-H01`](../historias/ep-04/s03-h01.md).
