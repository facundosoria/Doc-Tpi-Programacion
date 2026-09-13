# Correcciones sugeridas — `llm-service`

El código de este servicio llegó por el commit `605f381` ("feat(s01): implementar flujo de
calibración Golden Set v2", autor Franco Brizzio / `Coleman`) y **se importó sin tocar una sola
línea**: ver [[no-tocar-codigo-ajeno]]. Lo que sigue son correcciones que conviene aplicar,
anotadas acá en vez de aplicadas, para que las haga quien es dueño del código.

---

## 🟡 1. Siete controllers saltan a `infrastructure/persistence` directo, sin pasar por `application`

**Detectado al mapear el árbol real de carpetas contra la regla de
[36 §4](docs/36-playbook-de-construccion.md), documentado en
[37 §6](docs/37-estructura-carpetas-backend.md#6-hueco-conocido--no-corregido-acá).**

El flujo que fija el doc 36 es:

```
Consumidor -> API Gateway -> /api/llm/** -> controller -> aplicación/dominio
```

Es decir, un controller de `api/` debería depender de un service de `application/`, no de un
`*Repository` de `infrastructure/persistence/`. Hoy **7 de los 12 controllers** inyectan el
repository directo y lo usan como colaborador — a veces devolviendo el tipo anidado del repository
tal cual como respuesta HTTP, sin ningún DTO intermedio:

| Archivo | Qué inyecta directo |
|---|---|
| `api/ModelDeploymentController.java` | `ModelDeploymentRepository` |
| `api/CourseEvaluationStatusController.java` | `CourseEvaluationStatusRepository` |
| `api/CourseGoldenSetController.java` | `CourseGoldenSetRepository` (y sus tipos anidados como respuesta) |
| `api/GoldenSetUpdateProposalController.java` | `GoldenSetUpdateProposalRepository` |
| `api/CalibrationActivationController.java` | `CourseEvaluationStatusRepository` |
| `api/CalibrationRunController.java` | `CalibrationRunRepository` |
| `api/GoldenSetImportController.java` | `GoldenSetImportRepository` |

Ejemplo (`ModelDeploymentController.java`):

```java
public ModelDeploymentController(ModelDeploymentRepository repository,
    GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
  this.repository = repository;
  // ...
}

@GetMapping("/api/llm/courses/{courseId}/model-deployments")
public ModelDeploymentPage listForCourse(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
  var actor = authorization.require(headers);
  courseAuthorization.requireTeacher(courseId, actor, headers);
  return new ModelDeploymentPage(repository.listEnabledDeployments());
}
```

**Por qué importa:** no es sólo estilo. `domain/` está verificado limpio (cero imports de Spring),
pero la frontera que protege esa limpieza es justamente que `api/` no toque `infrastructure/`
directo. Si el patrón se sigue copiando —y es fácil copiarlo, porque 7 de 12 controllers ya lo
hacen—, la capa `application/` deja de ser el lugar donde vive la lógica de casos de uso y pasa a
ser sólo donde viven los que alguien no simplificó todavía. También acopla la respuesta HTTP al
shape interno de la tabla: cambiar una columna en `infrastructure/persistence/` rompe el contrato
de `api/` sin que ningún test de capa lo avise.

**Matiz:** es defendible como CQRS liviano para lecturas puras (una consulta de sólo lectura sin
regla de negocio no necesita una capa `application/` que sólo reenvíe la llamada). Pero hoy no está
declarado como decisión — es un atajo que se fue repitiendo — y el propio diagrama de doc 36 dice lo
contrario.

**Sugerencia (no aplicada):**

1. Decidir explícitamente si se adopta CQRS liviano para lecturas (y documentarlo en 36) o si se
   exige pasar siempre por `application/`.
2. Si se exige `application/`: los 7 controllers necesitan un service intermedio, aunque sea un
   wrapper delgado sobre el repository, para no romper la regla de dependencia.
3. Ninguno de los dos casos debería devolver el tipo anidado del repository (`CourseGoldenSetView`,
   `ModelDeploymentSummary`, etc.) directo como respuesta HTTP — conviene un DTO propio de `api/`
   aunque sea estructuralmente idéntico, para que un cambio de columna no rompa el contrato sin
   aviso.

---

> **Los ítems 2 a 10 vienen de
> [`docs/entregas/verificacion-v2-golden-set-calibracion.md`](docs/entregas/verificacion-v2-golden-set-calibracion.md)**
> (revisión del código v2 post-`605f381`, 2026-09-12), que pedía explícitamente transcribirlos
> acá y no se había hecho todavía.

## 🟡 2. La calibración nunca invoca un modelo — todo run queda en `RUNNING` para siempre

> **🟢 Parcialmente resuelto el 2026-09-12.** El puerto de invocación + fake (`LLM-S01-H10`) ya
> existe (`domain/ai/ModelInvocationPort`, `infrastructure/ai/FakeModelAdapter`,
> `application/ModelInvocationService`) — ver [`docs/estado-implementacion/ep-02/h10.md`](docs/estado-implementacion/ep-02/h10.md).
> **Sigue sin conectarse a la calibración**: el párrafo de abajo describe el estado tal como
> estaba antes de H10 y el paso que falta sigue exactamente igual — solo que ahora existe algo
> concreto para enchufar.

`CalibrationRunController.create` → `CalibrationRunService.enqueue` crea el registro y audita.
`CalibrationRunWorker.dispatch` (`@Scheduled`, cada 1s) sólo hace la transición `QUEUED → RUNNING`.
**Ningún código llama a `CalibrationMetrics.assess`** con puntajes reales, ni llena
`calibration_case_results`, ni transiciona a `PASSED`/`FAILED`. El estado, la métrica y el modelo
de datos están bien probados (`CalibrationMetricsTest`, `CalibrationStateMachineTest`) — falta la
pieza que los conecta con un modelo real o simulado.

**Falta un paso sin ficha todavía**: algo (¿una extensión de `CalibrationWorkflowService` o un
`CalibrationRunner` nuevo?) que tome `ModelInvocationService` (ya existe), corra los casos del
golden set contra el fake, llene `calibration_case_results` y cierre el run con
`CalibrationMetrics.assess`.

## 🔴 3. El catálogo de modelos no lee su propia tabla

`ModelDeploymentController.listAdapters` (`GET /api/llm/admin/model-adapters`) devuelve
`"openai"`/`"anthropic"` **hardcodeados en el código**, con `id = UUID.randomUUID()` (distinto en
cada pedido — no sirve como identificador). Contradice RF-IA-11 ("modelo por función, editable
por ADMIN, vía registro — no en el código"). La tabla real (`model_adapters`, migración `V2`)
existe pero no se lee acá. (El otro endpoint, `GET .../model-deployments`, sí lee la tabla real y
hoy devuelve vacío porque no hay filas sembradas — eso es correcto, no un bug.)

## 🟡 4. `404` vs `409` — recurso inexistente responde mal

`ApiExceptionHandler` no mapea nada a `404`. Los `get()` que no encuentran el recurso lanzan
`IllegalStateException`, que hoy resuelve **`409 Conflict`**, no **`404 Not Found`**. Contradice
directamente H06·CA5 ("`goldenSetId` inexistente → `404`, nunca `500`" — hoy sería `409`) y el
mismo patrón aplicaría a rúbricas y calibraciones. Sugerencia: una excepción dedicada
(`ResourceNotFoundException`) → `404`, reservando `409` para conflictos de estado reales (p. ej.
publicar una versión que ya no es borrador).

## 🟡 5. Placeholders que se pueden confundir con funciones reales en una demo

- **`EligibleInteractionsService`** — dos interacciones fijas (UUIDs constantes, texto a mano). Sin
  integración con `practice-service` ni historial de chat real.
- **`SyntheticGoldenSetProposalService`** — tres plantillas fijas rotadas por índice (`i % 3`), con
  el campo `author` seteado a `"Generador Asistido / LLM"` **sin que ningún LLM las genere**. Ese
  nombre de campo es el riesgo concreto.

No son errores — son placeholders razonables mientras no existen las integraciones reales. Pero
hay que decirlo explícito en cualquier demo o entrega: si se muestran, aclarar que son datos de
ejemplo.

## 🟡 6. Falta confirmar si existe un `GET` de detalle por id

No se encontró (en la pasada de la verificación v2) un `GET` de un golden set, rúbrica o
calibración **individual** por id — sólo `list`. Si falta de verdad, decidir si se agrega o si las
fichas (H06 en particular) se actualizan para reflejar que la consulta es sólo por listado.

## 🟢 7. Documentación pendiente — código sin ficha

Rúbrica versionada por curso, calibración (run + activación + métrica) y catálogo de despliegues
de modelo ya están construidos pero **no tienen historia escrita**: hoy es imposible auditarlos
contra un CA acordado. Hace falta escribir esas fichas retroactivamente.

## 🟢 8. Terminología: "cohorte" (fichas) vs "curso" (código)

Las historias dicen "cohorte" de punta a punta; el código dice "curso". Adoptar "curso" en la
documentación — confirmando antes con Franco Brizzio si `courseId` es el curso-plantilla o la
cohorte/oferta concreta ([08 §B-7](docs/08-decisiones-y-pendientes.md)).

## 🟡 9. Confirmar el invariante de pesos de la rúbrica

[23 §4.2](docs/23-plan-construccion-producto-llm.md) fija 30/25/20/15/10 como invariante de
producto; `RubricValidator` sólo exige que las cinco dimensiones sumen 100, sin fijar esos valores
concretos. Decidir si cada curso puede variar sus pesos (y el invariante de 23 está desactualizado)
o si falta una validación en el código. Detalle en
[`LLM-S02-H01`](docs/historias/ep-03/s02-h01.md).

## 🟡 10. Confirmar el alcance de calibración: plataforma vs. curso

EP-04 describe calibración de plataforma **y** de curso; el código sólo implementa la de curso.
Detalle en [`LLM-S03-H01`](docs/historias/ep-04/s03-h01.md).

---

> **Los ítems 11 a 15 salen de cruzar
> [`docs/contracts/llm-service-v2-golden-set.openapi.yaml`](docs/contracts/llm-service-v2-golden-set.openapi.yaml)
> y [`docs/contracts/llm-service-v1.openapi.yaml`](docs/contracts/llm-service-v1.openapi.yaml)
> contra los `@*Mapping` reales de los 12 controllers, endpoint por endpoint (2026-09-12).**

## 🟡 11. Tres endpoints reales no están en el contrato v2

| Endpoint real | Controller |
|---|---|
| `POST /api/llm/courses/{courseId}/golden-sets/{versionId}/next-version` | `CourseGoldenSetController.createNextVersion` |
| `GET /api/llm/courses` | `CourseContextController.list` |
| `GET /api/llm/courses/{courseId}/model-deployments` | `ModelDeploymentController.listForCourse` |

El contrato v2 versiona rúbricas con `next-version` (`/rubrics/{id}/next-version`) pero no golden
sets, aunque el código sí lo implementa igual para los dos. `GET /courses` y
`GET .../model-deployments` no aparecen en ningún path del contrato — quien integre contra el
contrato v2 tal como está hoy no se entera de que existen.

## 🟡 12. Dos endpoints del contrato v2 no están implementados

| Endpoint documentado | Qué falta |
|---|---|
| `GET /api/llm/admin/base-golden-sets` (`listBaseGoldenSets`) | Ningún controller expone `base-golden-sets`. `CourseGoldenSetController.copyFromBase` necesita *conocer* un golden set base publicado (`baseVersionId`), pero no hay ningún endpoint para listarlos y obtener ese id |
| `POST /api/llm/admin/model-adapters` (`createModelAdapter`) | `ModelDeploymentController` sólo tiene los dos `GET` (`listAdapters`, `listForCourse`); no existe alta de adapter |

El primero es más grave que el segundo: sin `GET /admin/base-golden-sets`, el flujo documentado
"copiar desde la base publicada" (`copy-from-base/{baseVersionId}`) no tiene forma de descubrir
qué `baseVersionId` usar salvo conociéndolo de antemano.

## 🔴 13. El contrato v1 sigue documentando rutas que el código prohíbe activamente reintroducir

`docs/contracts/llm-service-v1.openapi.yaml` (y su adenda
[`llm-service-v1-s1-golden-set-adenda.md`](docs/contracts/llm-service-v1-s1-golden-set-adenda.md))
describen `POST /golden-sets`, `GET /golden-sets`, `POST /golden-sets/{goldenSetId}/entries`,
`POST /calibrations`, `GET /course-cohorts/{courseCohortId}/calibration` y
`GET /course-cohorts/{courseCohortId}/pending-evaluations` — el vocabulario y las rutas exactas
del `GoldenSetController` v1 que `605f381` **borró por completo**.

Lo llamativo: el propio commit agregó `scripts/check-no-v1.sh` +
`LegacyGoldenSetRouteAbsentTest` para garantizar que esas rutas **nunca vuelvan** — pero nadie
actualizó el archivo de contrato que sigue prometiéndolas. Alguien integrando contra
`llm-service-v1.openapi.yaml` hoy escribiría un cliente para endpoints que el propio servicio se
asegura de rechazar.

[`decision-605f381.md`](docs/entregas/decision-605f381.md) ya identificó la deuda de
terminología ("cohorte" vs "curso") en las historias, pero no menciona que el contrato v1 tiene el
mismo problema **en un archivo que se publica a otros equipos** ([18](docs/18-contratos-inter-equipos.md)).
No se edita acá — el contrato es un artefacto compartido y cambiarlo sin aviso es exactamente lo
que [02](docs/02-arquitectura-y-stack.md) prohíbe ("un cambio incompatible sin aviso rompe al
otro equipo en medio de su sprint"). Queda anotado para decidir en la sesión de integración: ¿se
marca `v1.openapi.yaml` como histórico/reemplazado, o se fusiona con v2 como la adenda original
pedía?

## 🟢 14. `/tutor/interactions` y `/evaluations/**` del contrato v1 no tienen código — esperado, no es un hueco

El resto del contrato v1 (tutor, evaluaciones, apelaciones, overrides, `/jobs/{jobId}`,
`/model-assignments/{function}`) no tiene ningún controller real. **No es una inconsistencia**:
son EP-05/EP-06/EP-02, que todavía no empezaron (ver [37 §7](docs/37-estructura-carpetas-backend.md)).
Se anota sólo para que no se lea como un hueco al lado de los ítems 11-13, que sí lo son.

## 🟢 15. `GoldenSetUpdateProposal` (contrato) trae campos que el repositorio no expone completos

El schema `GoldenSetUpdateProposal` del contrato v2 exige `baseVersion` y `baseCaseCount`.
`GoldenSetUpdateProposalController.pending` devuelve
`GoldenSetUpdateProposalRepository.GoldenSetUpdateProposal` directo (mismo patrón del ítem 1) —
no se verificó en esta pasada si ese registro trae esos dos campos poblados o el repository los
omite. Queda para confirmar junto con el ítem 1 si se decide envolver esta respuesta en un DTO.

---

> **Los ítems 16 a 22 salen de cruzar las historias y tareas SMART de
> [`docs/historias/ep-01/`](docs/historias/ep-01/README.md),
> [`docs/historias/ep-02/h10.md`](docs/historias/ep-02/h10.md) y
> [`docs/historias/ep-03/`](docs/historias/ep-03/README.md) — con sus tareas espejo en
> `docs/tareas/` — contra el código y la configuración reales (2026-09-12).**

## 🔴 16. H03·T3 no cumplida: el servicio no se registra en Eureka

La tarea T3 de H03 pide "registrar el servicio en Eureka como `llm-service`" (5 h, traza CA1).
Verificado: **cero** menciones de Eureka en todo `llm-service/` — ni dependencia
`spring-cloud-starter-netflix-eureka-client` en `pom.xml`, ni bloque `eureka:` en
`application.yml`, ni ninguna clase de configuración. El servicio arranca y responde en local,
pero no hay forma de que el API Gateway lo descubra. **H03·CA1 no se cumple.**

No es necesariamente un bug urgente — el resto de la plataforma (Gateway/Eureka reales) tampoco
está integrado todavía ([24](docs/24-convenciones-cobertura.md): *"Gateway y Eureka se
ejecutan en CI cuando existan sus módulos reales"*) — pero la tarea está marcada como parte de S1
y hoy tiene 0 horas de las 5 aplicadas.

## 🔴 17. H03·T4 no cumplida: el servicio nunca devuelve `401`, todo es `403`

La tarea T4 de H03 pide "validar `aud=llm-service` y el scope requerido; un token sin scope
devuelve `401`". Verificado con `grep -rn "ResponseStatusException" src/main/java`: las **únicas**
respuestas de autorización que existen en todo el servicio son `HttpStatus.FORBIDDEN` (403), en
`GoldenSetAuthorization.require` (falta de servicio confiable, scope o usuario delegado) y en
`CourseAuthorization.requireTeacher` (falta de rol docente u ownership del curso). **No hay un
solo `401` en el código.** Esto también contradice el escenario 2 de H03 ("token sin el scope
requerido... recibe `401`") y la traza de H09·T3 ("pruebas de contrato... que ejercitan headers,
`401`/`403`").

**Matiz:** puede ser una decisión deliberada no documentada (colapsar 401 en 403 para no revelar
si el problema es de autenticación o de autorización) — pero si es así, H03/H09 deberían
actualizarse para dejar de prometer un `401` que nunca va a llegar.

## 🟡 18. H09·T6 no cumplida: sin JaCoCo no hay gate de cobertura backend posible

La tarea T6 de H09 pide "activar el gate de cobertura de [24](docs/24-convenciones-cobertura.md)
en CI" (umbral 95% backend). Verificado: `pom.xml` **no tiene el plugin de JaCoCo configurado**
(`grep jacoco pom.xml` → vacío). Sin JaCoCo no hay `target/site/jacoco/index.html` ni `jacoco.xml`
que un gate de CI pueda leer — el umbral de doc 24 no puede estar activo hoy, sin importar cuántos
tests haya.

## 🟢 19. H02: `.env.example` y `down` — resuelto el 2026-09-12

> Ya no es un hueco: existe [`.env.example`](.env.example) con las variables del ADR, y
> [`README.md`](README.md) documenta `up` (aislado y con workbench), `docker compose down` y cómo
> verificar salud desde afuera del contenedor. Se conserva la redacción original como registro de
> lo que faltaba antes de la consolidación de `docs/`/`llm-workbench/` dentro de `llm-service/`.

H02 pide como dato obligatorio un "archivo `.env.example` con las variables del ADR" y que el
README documente `up`, la comprobación de salud y `down`. Verificado en su momento: no existía
ningún archivo `.env*` en el repo, y `llm-service/README.md` sólo documentaba los dos comandos `up`
(aislado y con workbench) — no mencionaba `docker compose down` ni cómo verificar salud desde
afuera del contenedor.

## 🔴 20. H05, H06 y H07 describen endpoints que ya no existen — quedaron obsoletas y nadie las reescribió

Las tres fichas ([h05](docs/historias/ep-03/h05.md), [h06](docs/historias/ep-03/h06.md),
[h07](docs/historias/ep-03/h07.md)) siguen documentando literalmente
`POST /api/llm/golden-sets`, `GET /api/llm/golden-sets/{goldenSetId}`, "cohorte" como término y
`403` (nunca `404`) para un golden set inexistente. **Ninguno de esos endpoints existe hoy**: el
código real es `/api/llm/courses/{courseId}/golden-sets/...` (`CourseGoldenSetController`), tal
como ya identificó [decision-605f381.md](docs/entregas/decision-605f381.md) a nivel de
subsistema. Este ítem lo hace explícito a nivel de **ficha completa**: H05/H06/H07, tal como están
escritas hoy, no se pueden ejecutar ni verificar contra el servicio real — ni un solo `curl` de
sus secciones "Endpoints" funciona. Es el mismo problema que ya tuvo
[ep-03-s1-verificacion.md](docs/entregas/ep-03-s1-verificacion.md) (marcada obsoleta), pero acá
nadie marcó ni reescribió las historias mismas.

## 🟢 21. H10 — confirmado 0 de 6 tareas hechas, no sólo "falta la pieza que invoca"

> **🟢 Resuelto el 2026-09-12.** Las 6 tareas están hechas — ver
> [`docs/estado-implementacion/ep-02/h10.md`](docs/estado-implementacion/ep-02/h10.md). Se
> conserva la tabla original como registro de lo que faltaba antes de portar
> `LlmGateway`/`GroqAdapter` de `codigo-ejemplo/ms-evaluacion-llm` (carpeta ya eliminada).

Yendo tarea por tarea de [`tareas/ep-02/h10.md`](docs/tareas/ep-02/h10.md) contra el código
(complementa el ítem 2, que ya reportaba el síntoma):

| Tarea | Pide | Estado |
|---|---|---|
| T1 | Interfaz de puerto de invocación en `domain`/`application` | ❌ No existe ninguna clase `LlmPort`/`ModelInvocationPort` ni equivalente |
| T2 | Schema estricto de salida + validador | ❌ No existe |
| T3 | Adaptador fake (WireMock) | ❌ No existe — ninguna dependencia de WireMock en `pom.xml` |
| T4 | Timeout configurable + error controlado | ❌ No existe |
| T5 | Migración de tabla `función → proveedor + modelo` | ❌ No existe. `model_adapters`/`model_deployments` (migración `V2`) son catálogos de adapter/deployment por curso — no hay ninguna tabla que mapee una función (`tutor`, `evaluador`, `moderador`) a un modelo |
| T6 | Pruebas de los 3 escenarios BDD | ❌ No existe (no hay nada que probar) |

**H10 está en 0%, no parcialmente construida.** Es la causa raíz exacta del ítem 2: sin el puerto
de T1, no hay dónde enchufar ni un fake ni un modelo real, así que la calibración no tiene forma
de dejar de quedarse en `RUNNING`.

## 🟢 22. H04 sí se cumple — y esto resuelve a favor del código una divergencia que 28/11 daban por abierta

Verificado en `V1__schema_and_rubric.sql`: la migración `V1` sí crea `rubric_versions` con la
versión `1.0` y `rubric_dimensions` con los cinco pesos exactos que pide H04 y que
[23 §4.2](docs/23-plan-construccion-producto-llm.md) fija como invariante — autonomía 30,
claridad 25, progresión 20, cumplimiento 15, eficiencia 10. **H04·CA1 se cumple.**

Dato adicional no pedido por H04 pero relevante para otro documento: los nombres de tabla y columna
son **en inglés** (`rubric_versions`, `golden_sets`, `autonomy`, `clarity`...), en las migraciones
`V1` y `V2` por igual. [28 §6](docs/28-normativa-catedra-plataforma.md) y
[11 Parte B](docs/11-glosario-y-metadata.md) documentan esto como una divergencia **abierta**
frente a la cátedra, con una propuesta de nombres en español marcada `E-15` todavía sin decidir.
El código ya decidió: fue directo al inglés, sin pasar por la propuesta en español. No lo cambio
acá (no es un bug, es información para quien mantenga 28/11), pero alguien debería actualizar esos
dos documentos para que no sigan listando como "abierta" una divergencia que el código ya cerró.

---
