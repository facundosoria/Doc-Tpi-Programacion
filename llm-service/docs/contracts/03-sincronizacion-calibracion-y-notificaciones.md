# 03 — Sincronización de calibración y avisos de operación

> **Decisiones:** D-21 a D-26, D-78 a D-85, D-95 a
> D-99, D-110 a D-112, D-115 a D-116, D-164 y D-165. D-164 y D-165 son propuestas técnicas pendientes
> de confirmación: no modifican OpenAPI, AsyncAPI ni contratos de contrapartes.

### D-21 — Integración asíncrona de notificaciones

**Restricción de integración:** [01 — Inventario y brechas](01-inventario-y-brechas.md)
no contiene todavía el contrato técnico con `notification-service`. La
implementación exige definir canal, evento, destinatarios, severidad, payload,
reintentos y DLQ.

**Decisión de diseño:** `llm-service` persiste localmente el incidente de seguridad, la
suspensión/recalibración y el evento de auditoría. En la misma transacción guarda
un registro outbox. Un relay publica el aviso por Kafka de manera asíncrona;
`notification-service` es dueño de guardar, entregar y marcar leída/no leída la
notificación de cada destinatario. Kafka nunca es síncrono ni reemplaza la
respuesta HTTP inmediata de la carga.

**Reglas del contrato:** eventos versionados por topic, envelope con
`eventId`, `version`, `occurredAt`, `producer` y `data`; correlación sólo en los
headers `traceparent` y `X-Request-Id`; publicación at-least-once mediante
outbox y consumo idempotente con deduplicación por `eventId`.

### D-22 — Responsabilidades de alertas confirmadas

**Decisión de Producto:** se confirma la separación: `llm-service`
es dueño del incidente, la auditoría, la suspensión/recalibración y el outbox;
`notification-service` es dueño de persistir, entregar y administrar el estado
de lectura de las notificaciones por destinatario.

**Implicancia de diseño:** la integración se implementará sólo tras acordar el
contrato con el equipo de Notificaciones. La respuesta HTTP del flujo crítico no
depende de Kafka ni de la entrega del aviso.

### D-23 — Dueño del catálogo de desafíos publicados

**Decisión de Producto:** `challenges-service` es dueño del catálogo de desafíos
publicados y debe proveer la información necesaria para listarlos y
seleccionarlos durante la subcalibración.

**Implicancia de diseño:** `llm-service` no accede a la base de datos de
Desafíos. El contrato interequipos deberá acordar los campos de catálogo y la
validación de pertenencia a curso/estado publicado antes de implementar el
selector o permitir crear una subcalibración.

### D-24 — Sin proyección de catálogo ni ciclo de vida en `llm-service`

**Decisión de Producto:** `llm-service` no mantiene una proyección del catálogo,
estado ni vencimiento de desafíos. Sólo registra el `challengeId` y `courseId`
que se le asocian al crear, ejecutar o activar una subcalibración.

### D-25 — Elegibilidad de desafíos

**Decisión de Producto:** sólo los desafíos publicados y vigentes pueden
seleccionarse para crear o recalibrar una subcalibración.

**Implicancia de diseño:** el dueño del catálogo limita el selector a desafíos
publicados y vigentes. `llm-service` no valida el ciclo de vida externo; responde
únicamente por el estado de su calibración asociada.

### D-26 — Recursos publicados seleccionables por desafío

**Decisión de Producto:** una subcalibración puede seleccionar cualquier rúbrica
y Golden Set publicados que pertenezcan al curso, aunque no sean los usados por
la calibración global.

**Implicancia de diseño:** el backend valida publicación y pertenencia al curso
para ambos recursos. La interfaz mostrará los valores heredados y permitirá
reemplazarlos por cualquier versión elegible, dejando visible esa diferencia en
la vista previa.

### D-78 — Recordatorios de borradores próximos a vencer

**Decisión de Producto:** el docente recibe recordatorios sobre borradores de
subcalibración pendientes antes del vencimiento del desafío asociado.

### D-79 — Alcance de conocimiento de `llm-service`

**Decisión de Producto:** `llm-service` no conoce el catálogo completo de
cursos o desafíos ni sus vencimientos. Mantiene un registro acotado de cursos
activos y de desafíos marcados como `requiresCalibration`, junto con el estado
de las calibraciones que efectivamente existan: en ejecución, aprobada, activa,
fallida, requerida nuevamente o bloqueada por seguridad. No infiere un estado
pendiente si un recurso requerido aún no tiene corrida, ni persiste el resto del
catálogo.

### D-80 — `challenges-service` no gobierna la habilitación

**Decisión de Producto:** `challenges-service` no tiene la potestad de habilitar
ni deshabilitar un desafío o examen. La compuerta de aprobación/publicación debe
ubicarse en el servicio que realmente gobierna esa decisión.

### D-81 — Contrato agnóstico de estado de calibración

**Decisión de Producto:** `llm-service` expone un contrato HTTP de consulta que
cualquier servicio puede invocar mediante el API Gateway; ningún consumidor
concreto queda codificado en el contrato.

**Recursos acordados:**

```http
GET /api/llm/courses/{courseId}/calibration-status
GET /api/llm/courses/{courseId}/challenges/{challengeId}/calibration-status
```

La respuesta informa exclusivamente `registered`, `calibrationState` y, si
existe, el identificador de calibración efectiva. Si el recurso no fue marcado
como `requiresCalibration`, responde `registered: false` y no crea ni persiste
ningún registro por la consulta.

El contrato no decide elegibilidad, no publica ni habilita cursos o desafíos, y
no valida existencia, catálogo o vencimiento. Tampoco devuelve rúbricas, Golden
Set, skills ni contenido académico. La consulta protege el endpoint mediante el
JWT de servicio y propaga `traceparent` y `X-Request-Id`. Cada consumidor aplica
su propia política: por ejemplo, un curso requerido puede bloquearse sin estado
activo y un desafío opcional puede publicarse sin calibración.

### D-82 — Registro acotado de recursos que requieren calibración

**Decisión de Producto:** `courses-service` registra en `llm-service` los
cursos activos. Cuando quien crea un desafío indica que requiere calibración,
Cursos informa a LLM únicamente ese desafío y su relación con el curso. Los
recursos requeridos aparecen en la lista de recursos que requieren calibración
aun antes de que exista una corrida; el resto del catálogo de desafíos no se
almacena ni se lista en LLM. LLM no los clasifica ni gestiona como pendientes por
la ausencia de una calibración.

### D-83 — Nombre visible en el registro de calibración requerida

**Decisión de Producto:** al registrar un curso o desafío con
`requiresCalibration`, se guarda también un nombre visible como snapshot para
mostrarlo en las listas de LLM.

**Implicancia de diseño:** el snapshot se asocia al identificador externo y, en
el caso de un desafío, a su `courseId`. Es sólo información de presentación; no
convierte a `llm-service` en dueño del catálogo, del ciclo de vida ni del
vencimiento del recurso.

### D-84 — Versionado de cambios de nombre del snapshot

**Decisión de Producto:** un cambio de nombre de un curso o desafío registrado
crea una versión nueva del snapshot de presentación. Las versiones anteriores
se conservan para auditoría.

**Implicancia de diseño:** el historial de calibraciones mantiene el nombre que
era visible cuando se creó o ejecutó. Actualizar el snapshot no modifica
calibraciones, subcalibraciones ni evaluaciones históricas; sólo actualiza la
versión vigente mostrada en las listas futuras.

### D-85 — Sincronización por eventos de cursos activos y requisitos

**Decisión de Producto:** `courses-service` informa a `llm-service` por eventos
Kafka los cursos activos, sus cambios de nombre y los cambios de
`requiresCalibration` de cursos o desafíos.

**Implicancia de diseño:** los eventos entregan únicamente el tipo e
identificador del recurso, `courseId` cuando corresponda, el estado activo o de
requisito y la nueva versión del nombre visible. Se publican mediante outbox y
se consumen idempotentemente por `eventId`, con `traceparent` y
`X-Request-Id` en headers. `llm-service` actualiza su registro acotado sin
adquirir el catálogo, fechas de vencimiento ni la potestad de publicación.

### D-95 — Desactivación del requisito de calibración

**Decisión de Producto:** cuando el dueño informa
`requiresCalibration: false`, LLM conserva el registro como inactivo para
auditoría y lo oculta de la lista vigente de recursos requeridos.

**Implicancia de diseño:** el registro `requiresCalibration` existe sólo para
que LLM pueda obtener y mostrar los datos de cursos o desafíos requeridos. No
crea una tarea, pendiente ni proceso autónomo por falta de calibración; las
calibraciones ya existentes mantienen su historial independiente.

### D-96 — Agrupación de desafíos requeridos por curso

**Decisión de Producto:** la lista de LLM agrupa los desafíos que requieren
calibración debajo de su curso relacionado.

**Implicancia de diseño:** el registro acotado conserva la relación externa
`challengeId → courseId` y los snapshots vigentes de nombres para presentar la
jerarquía. No replica el catálogo completo ni el estado de publicación de los
desafíos.

### D-97 — Cursos activos provistos por `courses-service`

**Decisión de Producto:** los cursos son inmutables bajo responsabilidad de
`courses-service`, que provee a LLM la lista de cursos activos. Cuando un curso
deja de estar activo, LLM oculta de su lista vigente el curso y sus desafíos
requeridos, conservando su historial.

**Implicancia de diseño:** LLM no decide ni modifica el ciclo de vida del curso.
Usa la información de actividad provista por Cursos para filtrar su registro
acotado de requisitos y snapshots de presentación.

### D-98 — Sincronización inicial e incremental de cursos activos

**Decisión de Producto:** `courses-service` ofrece una lista inicial de cursos
activos para sincronizar LLM y luego emite eventos Kafka para sus altas o
cambios.

**Implicancia de diseño:** LLM puede reconstruir su registro acotado después de
una caída o desincronización y aplicar los cambios incrementales de manera
idempotente. La lista inicial no transfiere la propiedad del catálogo ni del
ciclo de vida de Cursos.

### D-99 — Lista persistente y filtro por existencia de calibración

**Decisión de Producto:** un curso o desafío requerido permanece en la lista de
LLM después de calibrarse, para poder seleccionarlo y recalibrarlo. La lista
ofrece un filtro explícito entre recursos con calibración existente y recursos
sin calibración.

**Implicancia de diseño:** LLM conoce la existencia de sus propias
calibraciones y usa ese dato sólo para filtrar y presentar la lista. El filtro
no crea una pendiente, no solicita una calibración ni aplica una compuerta de
publicación.

### D-110 — Alerta administrativa por falta total de modelo

**Decisión de Producto:** la indisponibilidad del modelo evaluador único genera
una alerta inmediata al administrador.

**Implicancia de diseño:** LLM registra el incidente y publica la solicitud de
notificación de manera confiable mediante el mecanismo asíncrono ya definido,
con correlación y causa. La alerta permite intervenir sobre la disponibilidad de
proveedores sin que LLM asuma el rol de gobernar cursos, desafíos o bloqueos.

### D-111 — Aviso a docentes ante indisponibilidad de evaluación

**Decisión de Producto:** ante falta total de un modelo utilizable, también se
notifica a los docentes cuyos cursos o desafíos puedan resultar afectados.

**Implicancia de diseño:** LLM determina los destinatarios a partir de las
asociaciones de calibración y la autorización de cursos que recibe de sus
dueños, sin replicar su catálogo completo. La notificación identifica el
recurso afectado y la indisponibilidad, pero no divulga información sensible de
proveedores ni de otros cursos.

### D-112 — Notificación de recuperación operativa

**Decisión de Producto:** cuando vuelve a existir un modelo utilizable, se
notifica la recuperación al administrador y a los docentes afectados por la
indisponibilidad.

**Implicancia de diseño:** el incidente conserva una transición explícita de
abierto a recuperado y enlaza ambos avisos para evitar ambigüedad. La
recuperación informa el restablecimiento de capacidad; no modifica evaluaciones
históricas ni fuerza recalibraciones fuera de las reglas ya establecidas.

### D-115 — Cierre de curso condicionado a scores de IA pendientes

**Decisión de Producto:** un curso no puede cerrarse mientras tenga scores de
uso de IA pendientes de cálculo diferido.

**Implicancia de diseño:** LLM conserva una cola persistente e idempotente de
evaluaciones diferidas y expone el contador de pendientes por curso. El servicio
dueño del cierre consulta ese estado y aplica el bloqueo de RF-IA-34; LLM no
cierra cursos ni modifica la economía del alumno. La cola debe monitorearse por
antigüedad además de cantidad para permitir intervención operativa antes de un
cierre.

### D-116 — Sin excepción administrativa al cierre con pendientes

**Decisión de Producto:** ningún rol, incluido Administración, puede forzar el
cierre de un curso que tenga scores de IA pendientes.

**Implicancia de diseño:** la consulta de pendientes es una precondición dura
del cierre en el servicio que lo gobierna. LLM mantiene el conteo verificable y
la trazabilidad de cada trabajo pendiente, sin exponer una operación de omisión,
vaciado manual ni cierre forzado.

### D-164 — Propuesta: superficie HTTP y congelamiento de campos

**Alcance:** punto 6. Esta es una propuesta de contrato para revisión previa;
no modifica `llm-service.openapi.yaml` ni acuerda aún consumidores. Todas las
rutas públicas quedan bajo `/api/llm/**`, pasan por API Gateway, requieren JWT
M2M con `aud=llm-service`, scopes mínimos por operación y propagan
`traceparent`, `X-Request-Id` e `Idempotency-Key` en cada escritura. Los errores
se expresarán como RFC 7807 con un `codigo` estable.

#### Consulta de estado de calibración

Se propone conservar los recursos ya acordados:

```http
GET /api/llm/courses/{courseId}/calibration-status
GET /api/llm/courses/{courseId}/challenges/{challengeId}/calibration-status
```

Respuesta propuesta, sin rúbricas, Golden Set, skills, prompts ni contenido
académico:

```json
{
  "registered": true,
  "calibrationValidity": "VALID",
  "evaluatorAvailability": "AVAILABLE",
  "effectiveCalibrationRunId": "uuid | null"
}
```

- `registered=false` indica que LLM no posee un registro de requisito para el
  recurso y no crea ninguno por leerlo.
- `calibrationValidity` propone los valores `NO_ACTIVE`, `VALID`,
  `VERIFICATION_PENDING`, `RECALIBRATION_REQUIRED` y `SECURITY_SUSPENDED`.
- `evaluatorAvailability` propone `AVAILABLE` o `UNAVAILABLE`, separado de la
  validez para distinguir una calibración vigente de un proveedor temporalmente
  indisponible.
- `effectiveCalibrationRunId` es nulo cuando no existe configuración efectiva.
  No se expone una causa técnica del proveedor, secretos ni estados de otros
  cursos.

Esta propuesta ajusta D-81: allí `calibrationState` era un único campo. Se
recomienda sustituirlo por las dos dimensiones anteriores, ya que un solo enum
no puede expresar sin ambigüedad validez y disponibilidad. Esta modificación
queda pendiente de confirmación antes de cualquier OpenAPI.

#### Indicadores de scores diferidos

Se propone un recurso de sólo lectura para el servicio que gobierna el cierre y
para las vistas autorizadas:

```http
GET /api/llm/courses/{courseId}/deferred-evaluation-summary
```

Respuesta propuesta:

```json
{
  "pendingCount": 12,
  "oldestPendingAt": "2026-09-21T18:20:00Z | null",
  "retryExhaustedCount": 1,
  "hasRetryExhausted": true,
  "updatedAt": "2026-09-21T18:25:00Z"
}
```

El recurso no devuelve `attemptId`, alumno, transcripciones ni detalle de una
entrega. `pendingCount` incluye `DEFERRED`, `QUEUED`, `RUNNING`, `RETRY_WAIT` y
`RETRY_EXHAUSTED`; sólo `COMPLETED` queda fuera. `oldestPendingAt` es nulo con
contador cero. El dueño del cierre usa el contador, no una operación de
excepción de LLM.

#### Comandos de calibración propuestos

Para evitar que la API replique todo el perfil en cada inicio, la corrida debe
partir de un borrador persistente y privado. Se proponen estos recursos:

| Operación propuesta | Cuerpo final propuesto | Resultado |
|---|---|---|
| `POST /api/llm/courses/{courseId}/calibration-drafts/{draftId}/runs` | Sin body; el servidor materializa el snapshot del borrador. | `202`, corrida y `Location` de consulta. |
| `POST /api/llm/courses/{courseId}/calibration-runs/{runId}/activation-previews` | Sin body. | `201`, `previewId`, `expiresAt`, corridas/asignaciones esperadas y desafíos migrables o bloqueados. |
| `POST /api/llm/courses/{courseId}/calibration-activations` | `calibrationRunId`, `previewId`, `migrateChallengeIds` y `reason` opcional sólo para reactivación histórica. | `201`, activación creada. |
| `POST /api/llm/admin/calibration-activations/{activationId}/verifications` | `reason` obligatorio para origen manual. | `202`, verificación y `Location`. |

El `draftId` identifica de manera suficiente el objetivo (`COURSE` o un único
`CHALLENGE`), recursos seleccionados y baseline esperado; por eso no se aceptan
IDs redundantes ni identidad de curso/desafío en el body. Todas las escrituras
requieren `Idempotency-Key`; el servidor rechaza `draftId` ajeno al actor,
`runId` fuera de curso, preview vencido u obsoleto, baseline cambiado y una
lista de migración que contenga desafíos bloqueados.

**Decisiones que deben congelarse antes de editar OpenAPI:** nombres definitivos
de recursos, scopes por operación, las dos dimensiones del estado, enum de
validez, forma exacta del preview, semántica de `reason`, paginación del futuro
detalle administrativo y consumidores autorizados del resumen de pendientes.

### D-165 — Propuesta: eventos propios, outbox y mensajes a Notificaciones

**Alcance:** punto 7. Esta propuesta no modifica el AsyncAPI actual ni el
contrato de `notification-service`; enumera eventos que requieren acuerdo de
consumidores antes de publicarse.

**Reglas propuestas de publicación:** todo hecho se inserta en una outbox local
en la misma transacción que su cambio de estado. Un relay lo publica a Kafka de
forma at-least-once. Cada mensaje usa el envelope de plataforma:

```text
eventId, version, occurredAt, producer="llm-service", data
```

`traceparent` y `X-Request-Id` viajan exclusivamente en headers Kafka. El
relay no reconstruye ni altera la correlación original. Un consumidor deduplica
por `eventId`; LLM aplica la misma regla al consumir eventos ajenos. No se
asume orden entre topics: cada evento se particiona por el identificador de su
agregado (`courseId`, `challengeId`, `attemptId` o `incidentId` según el caso).

**Eventos de LLM propuestos para acuerdo:**

| Topic propuesto | Hecho | `data` mínima | Consumidor previsto |
|---|---|---|---|
| `calibracion_activada.v1` | Una activación fue creada o reemplazó la previa. | `activationId`, `scope`, `courseId`, `challengeId` opcional, `calibrationRunId`, `occurredAt`. | Servicios que consultan o proyectan disponibilidad; por confirmar. |
| `calibracion_suspendida.v1` | Una activación deja de ser apta. | `activationId`, `scope`, `courseId`, `challengeId` opcional, `reasonCode`, `occurredAt`. | Consumidores de estado y Notificaciones; por confirmar. |
| `score_de_ia_calculado.v1` | Se completó el único score de un intento. | Se conserva la propuesta existente: `evaluationId`, `attemptId`, `challengeId`, `courseId`, score agregado, scores por dimensión, versión de rúbrica/perfil y fecha. | `practice-service`. |
| `score_pendiente_diferido.v1` | Un score pasa a diferido o cambia su causa. | `evaluationId`, `attemptId`, `challengeId`, `courseId`, `reasonCode`, `deferredAt`, `nextRetryAt` opcional. | `practice-service` y quien controla el cierre; por confirmar. |
| `score_diferido_reintentos_agotados.v1` | Un pendiente alcanza el límite de reintentos. | `evaluationId`, `courseId`, `challengeId`, `attemptId`, `attemptCount`, `occurredAt`. | Notificaciones y operación; por confirmar. |
| `evaluador_indisponible.v1` / `evaluador_recuperado.v1` | Se abre o recupera una indisponibilidad total del evaluador. | `incidentId`, `deploymentId`, `reasonCode`, `affectedCourseIds` sólo si se acuerda, `occurredAt`. | Notificaciones; por confirmar. |

Los eventos existentes `calibracion_aprobada.v1` y
`calibracion_fuera_de_tolerancia.v1` se revisarán junto a esta tabla para evitar
duplicar semánticas. Una aprobación de corrida no equivale a una activación y
una suspensión no equivale a una falla técnica de corrida; los topics deben
mantener esa diferencia.

**Propuesta de mensaje para `notification-service`:** LLM publica una solicitud
de notificación, no persiste estados de lectura ni intenta entregar mensajes.
El topic candidato es `notificacion_solicitada.v1`; requiere aprobación del
dueño de Notificaciones. Su `data` no incluye Markdown de skills,
transcripciones, prompts, secretos ni resultados de alumnos y propone:

```json
{
  "notificationRequestId": "uuid",
  "incidentId": "uuid | null",
  "type": "CALIBRATION_SUSPENDED",
  "severity": "HIGH",
  "recipientScope": {
    "kind": "ADMIN_ROLE | COURSE_AUTHORIZED_TEACHERS | USER",
    "courseId": "uuid | null",
    "userId": "uuid | null"
  },
  "resource": {
    "courseId": "uuid | null",
    "challengeId": "uuid | null"
  },
  "reasonCode": "string",
  "occurredAt": "date-time"
}
```

La deduplicación técnica del consumidor usa `eventId`. Para evitar avisos de
negocio repetidos, LLM propone una unicidad de outbox por `(incidentId, type,
recipientScope, phase)`; `phase` distingue `OPENED`, `RETRY_EXHAUSTED` y
`RECOVERED`. Notificaciones conserva la autoridad sobre destinatarios finales,
entrega y leído/no leído. Falta acordar con ese servicio el topic, scopes de
destinatarios, severidades y tratamiento de DLQ.

