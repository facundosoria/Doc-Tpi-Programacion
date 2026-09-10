# EP-03 / Sprint 1 — Verificación contra historias y DoD

> **Qué es.** Revisión del estado actual de la épica **EP-03 · Golden set y referencia humana**
> para el **Sprint 1** (historias `LLM-S01-H05`, `H06`, `H07`), contrastando lo implementado en
> `llm-service/` y `llm-workbench/` con:
> - los criterios de aceptación y escenarios BDD de [`docs/historias/ep-03.md`](../historias/ep-03.md);
> - la ficha de épica [`docs/epicas/ep-03.md`](../epicas/ep-03.md);
> - la **DoD por historia** y **por incremento** de [23 · §9.2](../23-plan-construccion-producto-llm.md);
> - la [adenda S1 del contrato](../contracts/llm-service-v1-s1-golden-set-adenda.md).
>
> **Qué NO es.** No modifica código. `llm-service/` y `llm-workbench/` se importaron de otra rama
> y tienen dueño (ver commits `d6df8d7` y, en otra rama, `605f381`). Los hallazgos de código de
> esta revisión deben transcribirse a un `CORRECCIONES-SUGERIDAS.md` dentro de cada módulo por
> quien lo mantiene; acá quedan consolidados para la conversación de Sprint 1.
>
> **Fecha de la revisión.** 2026-09-10. Rama `facu`, working tree.

---

## 1. Resumen

| Historia | Estado | Bloqueante para la demo de S1 |
|---|---|---|
| **H05** — Alta y carga de entradas | **Parcial** | CA3 (ownership de cohorte) y CA4 (400 por puntaje inválido) no se cumplen |
| **H06** — Consulta que sobrevive al reinicio | **Parcial** | CA4 (aislamiento de cohorte) y CA6 (`size` fuera de rango → 400) no se cumplen; persistencia sin volumen nombrado |
| **H07** — Pantalla docente mínima | **Parcial** | CA2 (todas las llamadas por el Gateway) no se cumple: el front llama al servicio directo |

**Hueco transversal más grave:** **no existe aislamiento por cohorte**. Ni el esquema
(`golden_sets` no tiene columna de cohorte/curso), ni la autorización
(`GoldenSetAuthorization` sólo valida servicio + scope + usuario delegado presente), ni la
integración con `courses-service` están implementados. Tres CA negativos de H05/H06 dependen de
esto y hoy fallan; además es una **restricción de la épica** ("un docente no consulta, lista ni
modifica golden sets de cohortes ajenas → `403`/`404`").

**Segundo hueco:** el manejo de errores no cubre tres casos que las historias piden como `400`
(*Problem Details*). Hoy devuelven **500** porque `ApiExceptionHandler` sólo maneja
`GoldenSetNotFoundException`, `IllegalArgumentException` (→422) e `IllegalStateException` (→409).

---

## 2. H05 — Alta de golden set y carga de entradas

| CA | Esperado | Estado | Nota |
|---|---|---|---|
| CA1 | Alta y carga → `201` + `Location` | ✅ | `GoldenSetController.create/addEntry` devuelven `ResponseEntity.created(...)` |
| CA2 | Reintento con misma `Idempotency-Key` → mismo resultado, sin duplicar | ✅ | `IdempotencyRepository.replay` con `UNIQUE(operation, caller_service, delegated_user_id, idempotency_key)` + `request_hash`; devuelve el `response_body` guardado |
| CA3 | Docente que no es dueño de la cohorte → `403`, nada persiste | ❌ | **No hay chequeo de ownership.** No hay cohorte en el modelo ni llamada a `courses-service` |
| CA4 | `referenceScores` incompleto o fuera de rango → `400` *Problem Details* | ❌ | La validación vive sólo en el `CHECK valid_reference_scores` de la DB → `DataIntegrityViolationException` **no manejada** → `500`. `CreateGoldenSetEntryRequest` sólo tiene `@NotNull JsonNode` |
| CA5 | Request sin identidad delegada (no pasa por el Gateway) → `403` | ✅ | `GoldenSetAuthorization.require` lanza `ResponseStatusException(FORBIDDEN)` |

### BDD

- **Escenario 1 (camino feliz)** — ✅ cubierto por `GoldenSetServiceTest` y `GoldenSetControllerTest`.
- **Escenario 2 (idempotencia)** — ✅ `returnsStoredResponseWithoutCreatingOrAuditingWhenKeyIsReplayed`.
- **Escenario 3 (otra cohorte → 403)** — ❌ no implementado, sin test.
- **Escenario 4 (puntaje incompleto → 400)** — ❌ hoy `500`, sin test de la ruta HTTP completa.

### Otros hallazgos H05

1. **`transcript` sin validación en el borde.** Se pide "arreglo JSON con al menos un mensaje".
   Sólo lo valida el `CHECK` de la DB → un `transcript` vacío o no-array también da `500`, no `400`.
2. **`Location` de la entrada apunta a un recurso inexistente.** `addEntry` arma
   `/api/llm/golden-sets/{id}/entries/{entryId}` pero no hay `GET` de una entrada individual.
3. **`400` vs `422`.** La historia y la adenda dicen `400`; el handler mapea
   `IllegalArgumentException` → `422`. Hay que unificar (elegir el código y alinear historia +
   adenda + handler + escenarios BDD).
4. **Perfil `workbench` desactiva toda la autorización.** `GoldenSetAuthorization` con
   `llm.workbench.enabled=true` devuelve un usuario fijo sin mirar headers. `compose.yaml` corre
   el servicio con `SPRING_PROFILES_ACTIVE: workbench` → **la demo containerizada no tiene
   permisos reales**. Aceptable para correr local, pero incompatible con la DoD por incremento
   ("permisos reales", "ambiente integrado").

---

## 3. H06 — Consulta del golden set que sobrevive al reinicio

| CA | Esperado | Estado | Nota |
|---|---|---|---|
| CA1 | Golden set con entrada se consulta y la devuelve | ✅ | `GoldenSetRepository.find` carga `entries[]` |
| CA2 | Tras `docker compose restart`, misma consulta = mismos datos | ⚠️ | `restart` conserva el contenedor, así que *literalmente* pasa. Pero `compose.yaml` **no define un volumen nombrado** para PostgreSQL: `down`+`up` pierde los datos. H02·T1 pedía "volumen persistente". Frágil |
| CA3 | Listado respeta `page`/`size` y orden por creación descendente | ✅ | `list()` → `order by created_at desc limit ? offset ?`, `offset = page*size` |
| CA4 | Golden set de otra cohorte no aparece en el listado; su detalle → `404` | ❌ | `list()` devuelve **todos** los golden sets sin filtrar por caller. `get()` devuelve cualquiera. Fuga de confidencialidad |
| CA5 | `goldenSetId` con formato válido pero inexistente → `404`, nunca `500` | ✅ | `GoldenSetNotFoundException` → `404` |
| CA6 | `size=500` → `400` | ❌ | `@Max(100)` + `@Validated` lanzan `HandlerMethodValidationException` / `ConstraintViolationException`, **no manejada** → `500` |

### BDD

- **Escenario 1 (sobrevive al reinicio)** — ⚠️ sin test de integración de reinicio (`FlywaySchemaTest`
  sólo verifica migración + reglas append-only, y está detrás de `-Dintegration=true`).
- **Escenario 2 (aislamiento entre cohortes)** — ❌ no implementado.
- **Escenario 3 (inexistente → 404)** — ✅ `failsWhenRequestedGoldenSetDoesNotExist`.
- **Escenario 4 (`size=500` → 400)** — ❌ hoy `500`, sin test.

### Otros hallazgos H06

1. **El listado incluye `entries: []`.** La adenda define para el listado sólo
   `{ id, version, rubricVersion, language, createdAt }`. `GoldenSet` siempre trae `entries` y
   `default-property-inclusion: non_null` no descarta una lista vacía. Desvío menor de contrato.
2. **Filtro por cohorte como parámetro, no como invariante.** La historia lo dice explícito
   ("no es un parámetro opcional"); al implementarlo, tiene que salir de la identidad, no del query.

---

## 4. H07 — Pantalla docente mínima del golden set

| CA | Esperado | Estado | Nota |
|---|---|---|---|
| CA1 | Crear, cargar y ver detalle sin Swagger | ✅ | `WorkbenchComponent` cubre el recorrido |
| CA2 | Todas las llamadas salen al Gateway (`/api/llm/**`), no al servicio directo | ❌ | `GoldenSetApiService.baseUrl = 'http://localhost:8080/api/llm/golden-sets'` → **servicio directo**. No hay baseUrl por entorno |
| CA3 | Estados visibles de carga y de error en cada operación | ✅ | signals `loading` / `saving` / `error`; template los muestra |
| CA4 | Backend caído → aviso claro y pantalla navegable | ✅ | `fail()` + `.notice[role=alert]`; cubierto por spec |
| CA5 | Transcripción que no es JSON válido → mensaje, no se envía | ✅ | `addEntry()` hace `JSON.parse` en try/catch; cubierto por spec |
| CA6 | Respuesta `403` → estado «no autorizado» explícito | ⚠️ | `fail()` muestra `error.error.detail` genérico; no hay un estado «no autorizado» distinto del error genérico |

### Accesibilidad (WCAG 2.1 AA — exigida por la historia y la épica)

- ✅ `<label for>` en cada campo; `<fieldset><legend>` para los puntajes; errores con `role="alert"`.
- ⚠️ `aria-live` sólo en el "Cargando golden sets…" del listado. El "Cargando detalle…" no lo tiene.
- ⚠️ Sin manejo de foco al cambiar de modo/ruta ni al aparecer un error.
- ❌ Sin test automático de accesibilidad.

### Otros hallazgos H07

1. **`crypto.randomUUID()` como `Idempotency-Key` nueva en cada intento.** Si el usuario reintenta
   un alta que había fallado por timeout, genera un recurso nuevo en vez de recuperar el anterior:
   anula el beneficio de la idempotencia del backend desde la UI.
2. La demo interactiva `Demos/Golden Set/` que citan las fichas está **borrada en el working tree**.
   Hay que decidir si vuelve, si se reemplaza por `llm-workbench/`, o si se actualizan las fichas.

---

## 5. DoD §9.2 — por historia

| # | Ítem | Estado |
|---|---|---|
| 1 | PR revisado, sin secretos, migración versionada | ✅ `V1__schema_and_rubric.sql`. El UUID de usuario workbench por defecto no es secreto |
| 2 | Autorización por audiencia, scope/rol **y ownership**; se valida el recurso, no los IDs del body | ❌ falta ownership (cohorte). La existencia del golden set en `addEntry` sí se valida (`where exists`) |
| 3 | Bean Validation + RFC 7807 con `X-Request-Id` propagado | ⚠️ el `ProblemDetail` propaga `requestId`, pero no se manejan `MethodArgumentNotValidException`, `ConstraintViolationException` ni `DataIntegrityViolationException` |
| 4 | Idempotencia: misma clave no duplica; el reintento recupera el resultado | ✅ sólido (incluye detección de payload distinto y de solicitud en curso) |
| 5 | `traceparent` + `X-Request-Id` en HTTP; logs sin prompt/solución/token | ✅ `CallerIdentity` los transporta y `audit_events` los persiste. Logs no auditados en detalle |
| 6 | Pruebas: unitarias, integración de persistencia/migración, **contrato con WireMock**, autorización, BDD con evidencia | ⚠️ unitarias ✅, migración ✅ (gated), **contrato/WireMock ❌ ausente**, BDD negativos ❌ (los caminos rotos no tienen test) |
| 7 | Métrica, health/readiness, runbook | ⚠️ `health,info` + probes ✅. Sin métricas de negocio (la épica pide "golden sets activos por cohorte", "entradas cargadas"). Sin runbook |

## 6. DoD §9.2 — por incremento de sprint

| # | Ítem | Estado |
|---|---|---|
| 1 | Recorrido con interfaz, persistencia y **permisos reales** | ⚠️ interfaz ✅, persistencia ⚠️ (sin volumen), permisos ❌ (perfil workbench + front directo) |
| 2 | Los servicios consumidores del compromiso participan de verdad (Gateway, `admin-service`, `courses-service`) | ❌ ninguno integrado; la adenda sigue "por aprobar con `admin-service`" |
| 3 | Pruebas de contrato, fallas, idempotencia, seguridad, regresión | ⚠️ parcial (sin contrato) |
| 5 | Código, migraciones y contratos coinciden con lo desplegado | ⚠️ la adenda no está fusionada en `llm-service-v1.openapi.yaml` |
| 6 | Versión identificable, evidencia de demo, procedimiento de recuperación | ⚠️ `README` tiene pasos de arranque; falta la guía de demo de S1 (H09) y el runbook |
| 7 | Página de Wiki `G03 - GOLDEN SET` al día (formato [27](../27-guia-wiki-taiga.md), HU por permalink, diagramas de la secuencia) | ❌ no verificable en el repo (vive en Taiga); pendiente |
| 8 | La demo pasa en ambiente integrado, no en una máquina aislada | ❌ hoy sólo local |

---

## 7. Inconsistencias de contrato / especificación

1. **`400` vs `422`** para entrada inválida: historia/adenda dicen `400`; el handler usa `422`.
2. **`Location` de la entrada** apunta a un `GET` que no existe.
3. **Listado devuelve `entries: []`**, fuera de los campos de la adenda.
4. **Detalle de cohorte ajena** debería ser `404` (historia); hoy es `200` con datos.
5. **Vocabulario**: las historias y la épica dicen "cohorte"; la rama `605f381` usa
   "course"/`courseId`. Unificar antes de integrar.

---

## 8. Nota de coordinación — rama `605f381`

El commit `605f381` ("feat(s01): implementar flujo de calibración Golden Set v2", autor Coleman)
**en otra rama** borra por completo esta implementación de S1 (`GoldenSetController`,
`GoldenSetService`, …) y la reemplaza por un flujo de calibración con endpoints
`/api/llm/courses/{courseId}/…`, un `CourseGoldenSetController` y un
`CourseAuthorization.requireTeacher(courseId, …)` — que es justamente el chequeo de ownership por
curso que falta acá.

Antes de invertir en tapar los huecos de esta versión hay que decidir con el/los dueños del
código: **(a)** S1 se rebasa sobre la v2, **(b)** se reconcilian, o **(c)** la v2 se pospone a
S2/EP-04 (calibración) y S1 sigue con esta base más el ownership agregado. Es una decisión de
quien mantiene el módulo, no un arreglo de esta revisión.

---

## 9. Punch-list sugerida para cerrar EP-03 / S1

> Para transcribir a `llm-service/CORRECCIONES-SUGERIDAS.md` y
> `llm-workbench/CORRECCIONES-SUGERIDAS.md` según corresponda, y priorizar en la Planning de S1.

**Bloqueantes de la demo:**

1. Modelo + autorización de **cohorte/curso**: columna en `golden_sets`, filtro por identidad en
   `list`/`get`, chequeo de ownership en `create`/`addEntry` (contra `courses-service` o el criterio
   provisional congelado en el contrato). Cubre H05·CA3, H06·CA4, restricción de la épica.
2. `ApiExceptionHandler`: manejar `DataIntegrityViolationException`, `ConstraintViolationException` /
   `HandlerMethodValidationException` y `MethodArgumentNotValidException` → `400`/`422` *Problem
   Details* con `X-Request-Id`. Cubre H05·CA4 y H06·CA6.
3. Validación de `transcript` y `referenceScores` en el borde (Bean Validation / validador propio),
   no sólo en el `CHECK` de la DB.
4. `llm-workbench`: `baseUrl` por entorno apuntando al **Gateway**; no al servicio directo. H07·CA2.
5. `compose.yaml`: volumen nombrado para PostgreSQL. H06·CA2 / H02·T1.

**Para la DoD:**

6. Test de contrato del Gateway (WireMock) y test HTTP end-to-end (`@SpringBootTest` + MockMvc) que
   ejerza los caminos negativos (403 ownership, 400 validación, 404 inexistente).
7. Test de integración del reinicio (persistencia real tras recrear el contenedor).
8. Unificar `400`/`422` en historia + adenda + handler; agregar `GET` de entrada individual o
   corregir el `Location`.
9. Fusionar la adenda S1 en `llm-service-v1.openapi.yaml` con `admin-service`.
10. Guía de demo de S1 (H09) + runbook + página de Wiki `G03 - GOLDEN SET`.
11. `llm-workbench`: reusar la `Idempotency-Key` en los reintentos de una misma operación;
    `aria-live` en el estado de carga del detalle y manejo de foco.

**Decisión previa:** resolver la coordinación con la rama `605f381` (§8).
