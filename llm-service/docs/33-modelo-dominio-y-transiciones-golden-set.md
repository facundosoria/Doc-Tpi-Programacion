# Modelo de dominio y transiciones — Golden Set

> Estado: propuesta de contrato v2; pendiente de revisión con cursos, desafíos y práctica.
> Fuente funcional: docs/31 y docs/32.

## Alcance

El modelo representa el evaluador del uso pedagógico de IA. No almacena soluciones esperadas ni
calcula aprobación académica. Los casos reales se anonimizaron antes de llegar a Golden Set.
Los recursos de curso se aíslan por courseId; ADMIN gestiona recursos de plataforma y modelos.

## Entidades

| Entidad | Campos esenciales | Regla |
|---|---|---|
| RubricFamily | id, scope PLATFORM o COURSE, courseId opcional, nombre | Una familia de curso pertenece a un curso. |
| RubricVersion | familyId, versión, state, revision, autor, fechas | Publicada o superada es inmutable. |
| RubricDimension | versión, key, criterio, anclas, prompt, weight | Cinco keys fijas; pesos suman exactamente 100. |
| GoldenSetFamily | id, scope, courseId opcional, nombre | La copia de base se vuelve independiente. |
| GoldenSetVersion | familyId, versión, state, baseVersionId opcional | Solo una versión publicada se calibra. |
| GoldenSetCase | versión, transcript, contexto, metadata segura, procedencia, referencias | Transcript no vacío; cinco enteros 0–100. |
| ImportBatch / ImportRow | lote, formato, estado, filas y errores | El commit es atómico y solo admite READY. |
| CalibrationRun | versiones, deployment, estado, progreso, métricas, artefactos | Solo PASSED puede activarse. |
| CalibrationCaseResult | corrida, caso, scores, finales, errores | Conserva los cinco errores y el máximo individual. |
| ActiveCalibration | courseId, calibrationRunId, activada por, fecha | Máximo una por curso. |
| ChallengeCalibrationAssignment | challengeId, courseId, corrida, lockedAt | Se bloquea con el primer intento. |
| PendingEvaluation | intento, asignación, motivo, estado, idempotencyKey | Reanuda una vez y conserva la asociación original. |
| ModelAdapter / ModelDeployment | provider, modelo, versión, capacidades, state | ADMIN administra; docente solo selecciona habilitados. |

## Relaciones

RubricFamily contiene RubricVersion y esta contiene cinco RubricDimension. GoldenSetFamily contiene
GoldenSetVersion y esta contiene GoldenSetCase. CalibrationRun referencia una versión publicada de
cada familia y un ModelDeployment, y produce CalibrationCaseResult. ActiveCalibration apunta a una
sola CalibrationRun PASSED por curso. ChallengeCalibrationAssignment y PendingEvaluation conservan
esa corrida histórica.

## Estados

| Recurso | Estados | Transiciones |
|---|---|---|
| Rúbrica / Golden Set | DRAFT, PUBLISHED, SUPERSEDED | DRAFT a PUBLISHED; publicar sucesora vuelve SUPERSEDED la previa; crear sucesora genera DRAFT nuevo. |
| Importación | DRAFT, VALIDATING, READY, COMMITTED, FAILED | Validar DRAFT; corregir FAILED hacia DRAFT; confirmar READY hacia COMMITTED. |
| Calibración | QUEUED, RUNNING, PASSED, FAILED, CANCELLED | Crear QUEUED; worker ejecuta; resultado terminal irreversible. |
| Evaluación real | QUEUED, RUNNING, COMPLETED, FAILED | La suspensión crea cola durable; worker procesa de forma idempotente. |

## Invariantes transaccionales

1. Publicar valida dimensiones, referencias y suma de pesos antes de cambiar de estado.
2. Toda escritura de transición usa Idempotency-Key; repetirla no duplica filas ni eventos.
3. Borradores usan revision e If-Match; una revisión vencida responde 409.
4. Activar valida PASSED, reemplaza la única activa, genera auditoría y migra solo desafíos confirmados sin intentos, todo en una transacción.
5. El primer intento hace una actualización condicional de la asociación y fija para siempre la corrida usada.
6. Una recalibración obligatoria fallida suspende evaluaciones nuevas, pero nunca modifica evaluaciones completadas.

## Dependencias externas

| Sistema | Aporte necesario |
|---|---|
| courses-service | Autorizar docente por curso y señalar archivo. |
| challenges-service | Exponer desafíos migrables y bloquear los que iniciaron intento. |
| practice-service | Emitir primer intento y cierre sin introducir PII en Golden Set. |
| ADMIN | Gestionar adapters, deployments y Golden Sets base. |

El contrato REST asociado es docs/contracts/llm-service-v2-golden-set.openapi.yaml.
