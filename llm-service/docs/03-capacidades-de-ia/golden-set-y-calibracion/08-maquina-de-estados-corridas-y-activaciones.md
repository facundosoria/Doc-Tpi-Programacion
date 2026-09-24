# Máquina de estados de corridas, activaciones y verificaciones; concurrencia

> **Estado:** decisión de diseño confirmada (D-161), pieza central del modelo de dominio de
> calibración/subcalibración. Complementa, sin reemplazar por completo, la tabla de estados de
> [03 — Modelo de dominio y transiciones](03-modelo-de-dominio-y-transiciones.md) (que documenta el
> `CalibrationRun` simplificado previo): esta versión separa explícitamente tres agregados con
> máquinas de estado propias.
> **Decisiones:** D-137 a D-141, D-155 a D-158, D-159 y D-161. D-160 y D-162 en adelante viven en
> otros documentos de este mismo bloque; D-159 y D-161 se documentan acá de forma completa por ser la
> pieza central del dominio.

## 0. Modelo lógico propio y trazable de LLM para skills y calibraciones (D-159)

**Decisión técnica:** LLM persiste en su base exclusiva los agregados de skill, versión inmutable de
contenido, metadato de catálogo, favorito, linaje de clon, incidente de seguridad, perfil de
calibración, corrida, activación, verificación periódica, reserva de cuota, score diferido, auditoría
y outbox. Los cursos y desafíos se identifican sólo por sus IDs, snapshots autorizados y estado de
registro para calibración; no se replica su catálogo ni ciclo de vida.

**Implicancia de diseño:** el contenido Markdown, sus hashes y los perfiles efectivos quedan
inmutables una vez usados. Los metadatos modificables y los estados operativos se conservan separados
de esos snapshots. Las transiciones que deben sobrevivir a reintentos o producir eventos se registran
junto con su auditoría y outbox en la misma transacción, para que una falla no deje una calibración,
una reserva o una notificación en un estado contradictorio.

Este es el inventario de agregados que persiste `llm-service` para todo el dominio de skills y
calibración documentado en esta carpeta y en
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md).
Sirve de base para la separación de agregados que introduce D-161 a continuación: `CalibrationRun`,
`CalibrationActivation` y `CalibrationVerification` (§1) son tres de esos agregados con máquina de
estado propia; skill, versión inmutable, metadato de catálogo, favorito y linaje de clon están
detallados en
[05 — Skills: catálogo y formato](05-skills-catalogo-y-formato.md); incidente de seguridad en
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md);
perfil de calibración (`CourseBaseline`/`ChallengeOverlay`/`EffectiveEvaluationProfile`) en
[06 — Subcalibración jerárquica §6](06-subcalibracion-jerarquica.md#6-modelo-híbrido-coursebaseline--challengeoverlay-d-49);
reserva de cuota en §7 de este documento; score diferido es propuesta D-162 (fuera de este rango,
responsabilidad del otro agente).

La regla transaccional de la última oración ("una falla no deje una calibración, una reserva o una
notificación en un estado contradictorio") es la que sostiene las reglas de concurrencia e
idempotencia del §2 de este documento y el uso de `Idempotency-Key`, lease y revisión condicional que
describe D-161 §4.

## 1. Tres agregados, tres máquinas de estado (D-161)

**Decisión de diseño confirmada:** la corrida de calibración, la activación de una configuración y su
verificación periódica son **agregados con máquinas de estado distintas**. Una corrida aprobada no
cambia de estado al activarse ni se reescribe si posteriormente pierde vigencia. La activación es un
**puntero versionado por alcance**; la verificación comprueba su **aptitud operativa**.

### 1.1 Corrida de calibración (`CalibrationRun`)

La corrida guarda su **snapshot inmutable** de perfil efectivo, recursos y deployment antes de entrar
a la cola.

```text
QUEUED  -> RUNNING
RUNNING -> PASSED | FAILED
QUEUED  -> FAILED   (control automático antes de invocar)
RUNNING -> FAILED   (control automático durante la ejecución)
```

- `PASSED` y `FAILED` son **terminales**. `PASSED` requiere cumplir las reglas de aprobación vigentes
  para el Golden Set (PAR-14, y su extensión por subcriterio — ver
  [07 — Rúbrica ponderada y subcriterios](07-rubrica-ponderada-y-subcriterios.md#42-subcriterios-referenciados-integran-la-aprobación-de-calibración-d-66)).
  `FAILED` conserva un **código de causa controlado**: por ejemplo, resultado fuera de tolerancia,
  error de proveedor, respuesta inválida o política de seguridad.
- **No existe cancelación manual** de una corrida iniciada, ni para docente ni para Administración
  (ver D-155, §3.1). Ante un error transitorio **tampoco hay reintento automático** (ver D-156, §3.2):
  el docente inicia otra corrida, con nuevo `runId` y nuevo snapshot.
- Si un control externo de seguridad invalida una skill antes o durante la ejecución, la corrida
  termina en `FAILED` con causa `SECURITY_POLICY`. **Nunca se completa ni se vuelve a ejecutar** con
  esa versión afectada (coherente con D-36, fuera de este rango, y con
  [04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md)).
- El estado histórico `CANCELLED` que pudiera aparecer en documentos o contratos anteriores (incluido
  el `CalibrationRun` previo, ver
  [03 — Modelo de dominio](03-modelo-de-dominio-y-transiciones.md#estados)) **no tiene transición
  habilitada** en este flujo. Se deprecará al congelar el contrato del bloque 6; **no debe
  implementarse una acción de cancelación** para producirlo.

Sólo una corrida `PASSED` puede ser elegida como **origen de una activación**. Reintentar, recalibrar
o sustituir recursos **siempre crea una corrida nueva**; nunca revive ni modifica una corrida
terminal.

### 1.2 Activación por alcance (`CalibrationActivation`)

Una activación referencia una corrida `PASSED` y se identifica por su **alcance**: `COURSE(courseId)`
para la base global o `CHALLENGE(challengeId)` para el perfil específico. Hay **como máximo una
activación vigente por cada clave de alcance**.

```text
ACTIVE -> SUPERSEDED
ACTIVE -> SUSPENDED_RECALIBRATION_REQUIRED
ACTIVE -> SUSPENDED_SECURITY
SUSPENDED_RECALIBRATION_REQUIRED -> SUPERSEDED
SUSPENDED_SECURITY                -> SUPERSEDED
```

- Activar una corrida aprobada crea una **nueva activación** y pasa la activación anterior del mismo
  alcance a `SUPERSEDED`. Reactivar una corrida histórica aprobada crea **otra** activación; nunca
  altera la activación ni la corrida anteriores (ver D-140, §2.2).
- `SUSPENDED_RECALIBRATION_REQUIRED` representa que una verificación terminó **fuera de tolerancia**.
  Desde ese momento la configuración ya no es apta para nuevos scores; se necesita una nueva corrida
  `PASSED` y una activación explícita para recuperarla.
- `SUSPENDED_SECURITY` representa una **suspensión preventiva** disparada por un control de seguridad
  externo. La causa y su pipeline se resuelven en el bloque pendiente de sanitización (bloque 4, fuera
  de alcance del sprint — ver
  [04 — Seguridad y gobierno de skills §6](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md));
  esta máquina sólo **consume su resultado** y bloquea futuros usos de la configuración afectada.
- La **recuperación de disponibilidad del proveedor no rehabilita por sí sola** una activación
  suspendida por tolerancia o seguridad. Tampoco hay sustitución automática de modelo, de skill ni de
  perfil.
- La activación global nueva **conserva las activaciones específicas existentes** hasta que cada
  desafío tenga su recalibración correspondiente. Las nuevas subcalibraciones deben partir de la base
  global vigente; si su base cambió, deben pasar por rebase y revisión antes de ejecutarse o activarse
  (ver [06 — Subcalibración jerárquica §10](06-subcalibracion-jerarquica.md#10-rebase-de-personalizaciones-ante-una-nueva-base-global-d-73-d-74-d-76)).

### 1.3 Verificación periódica (`CalibrationVerification`)

La verificación es una **ejecución separada** sobre el snapshot de la activación y el deployment que
ésta referencia. **No crea** una calibración activable ni reemplaza su corrida original.

```text
QUEUED  -> RUNNING
RUNNING -> PASSED | FAILED_TOLERANCE | FAILED_OPERATIONAL
QUEUED/RUNNING -> CANCELLED  (la activación fue sustituida antes de terminar)
```

- La planificación crea **como máximo una verificación abierta** por activación y ventana programada.
  Una petición manual administrativa usa la misma máquina, pero conserva su origen manual.
- `PASSED` mantiene la activación en `ACTIVE` y actualiza su última verificación satisfactoria.
- `FAILED_TOLERANCE` mueve la activación a `SUSPENDED_RECALIBRATION_REQUIRED`; no se puntúan nuevas
  evaluaciones con ella.
- `FAILED_OPERATIONAL` —por indisponibilidad del proveedor, cuota agotada o error interno— **no
  invalida por sí mismo** la última calibración aprobada. La verificación queda pendiente para
  reintento según la política operativa; no se usa un modelo de fallback para el evaluador.
- `CANCELLED` sólo evita aplicar el resultado de una verificación cuya activación **ya fue
  reemplazada**. No cancela, revierte ni modifica ninguna corrida de calibración.

### 1.4 Estado efectivo compuesto

El estado consultable se compone internamente a partir de **dos dimensiones**, sin mezclar la validez
de la calibración con la disponibilidad momentánea del deployment:

```text
calibrationValidity  = NO_ACTIVE | VALID | VERIFICATION_PENDING
                     | RECALIBRATION_REQUIRED | SECURITY_SUSPENDED

evaluatorAvailability = AVAILABLE | UNAVAILABLE
```

Los nombres, campos y códigos HTTP definitivos quedan **fuera de esta decisión** y se congelarán
exclusivamente en el bloque 6 de contratos.

### 1.5 Alcance explícito de esta decisión

D-161 define únicamente el comportamiento de **dominio y concurrencia** de este punto (bloque técnico
1 del plan). No adelanta el libro de cuota (bloque 2), el pipeline de sanitización (bloque 4) ni la
observabilidad, auditoría, métricas o pruebas (bloque 9): esos tres bloques siguen **fuera de alcance
del sprint actual** conforme al corte declarado al inicio del plan.

## 2. Reglas de concurrencia sobre toda operación mutante (D-161 §4)

- Una misma `Idempotency-Key`, actor, operación y payload devuelve el resultado original. Reutilizarla
  con un payload diferente se rechaza como **conflicto de idempotencia**.
- Las transiciones condicionan el estado y la **revisión actual** del agregado. Si otro comando ya lo
  cambió, el comando tardío falla por **estado obsoleto**; no se fuerza ni se repite una transición
  terminal.
- El **preview de activación** fija la corrida, las revisiones de las activaciones involucradas y las
  asociaciones migrables. La confirmación debe comprobar ese token completo: si cambió cualquier
  elemento, rechaza **toda** la operación como preview obsoleto, sin migraciones parciales.
- Un **worker** toma una corrida o verificación mediante un **lease**. Sólo el mismo lease puede
  completar la transición `RUNNING`; si vence, otro worker puede retomarla sin que ambos apliquen
  resultados.
- El primer intento mantiene el **bloqueo condicional** ya definido sobre la asociación
  desafío-calibración. Una asociación bloqueada nunca se migra ni cambia por una activación posterior.

## 3. Reglas operativas complementarias sobre corridas (D-155, D-156)

### 3.1 Corridas de calibración sin cancelación manual (D-155)

Un docente **no puede cancelar** una corrida de calibración después de iniciarla. Una corrida iniciada
termina con su resultado o por los controles automáticos de seguridad y fallos ya definidos. La
interfaz no ofrece cancelación manual: evita estados parciales ambiguos y conserva la evidencia y
trazabilidad completas de toda corrida iniciada.

### 3.2 Falla transitoria sin reintento automático (D-156)

Si una corrida de calibración falla por cuota agotada o un error temporal de proveedor, **finaliza e
informa la causa** al docente; **no se reintenta automáticamente**. La corrida queda en un estado
terminal explicable y auditable, sin activar resultados parciales ni consumir cuota adicional sin
decisión docente. El docente puede corregir la configuración, esperar capacidad o iniciar una nueva
calibración cuando corresponda.

## 4. Indicadores de scores diferidos por curso (D-157)

La consulta de pendientes por curso expone:

- el **total** de scores diferidos,
- la **antigüedad del más antiguo**,
- si **alguno agotó sus reintentos**.

El servicio dueño del cierre de curso dispone así de una precondición verificable, y Administración
puede distinguir una acumulación normal de una cola que requiere intervención. Los indicadores no
exponen entregas ni datos del alumno; `llm-service` conserva el detalle interno para reintento y
auditoría (relacionado con D-115/D-116, fuera de este rango, sobre el bloqueo de cierre de curso).

## 5. Concurrencia entre docentes al activar (D-137, D-138)

### 5.1 Concurrencia en la activación de calibración global (D-137)

Si dos docentes autorizados recalibran el mismo curso, quien intente activar una configuración basada
en una versión global **ya superada** debe **revisarla y rebasarla** sobre la versión activa actual
antes de activarla.

La activación aplica **control de concurrencia optimista** sobre la versión base y rechaza una
activación desactualizada con una explicación de conflicto. Las corridas, borradores y evidencias de
ambos docentes se conservan; el rebase explícito produce una nueva configuración auditable y evita
sobrescribir una calibración activa en silencio.

### 5.2 Concurrencia en la activación de subcalibración (D-138)

La misma regla de control de concurrencia y rebase se aplica cuando **dos docentes editan o
recalibran simultáneamente** un mismo desafío. Una subcalibración sólo se activa si su baseline de
curso y la versión activa del desafío siguen siendo las esperadas. Ante cambios concurrentes, el
backend conserva el borrador, muestra el conflicto y exige una resolución explícita antes de generar
un nuevo perfil efectivo y activarlo (mismo mecanismo de rebase que D-73/D-74, ver
[06 — Subcalibración jerárquica](06-subcalibracion-jerarquica.md#10-rebase-de-personalizaciones-ante-una-nueva-base-global-d-73-d-74-d-76)).

## 6. Lectura compartida e historial reactivable (D-139 a D-141)

### 6.1 Lectura compartida del historial por curso autorizado (D-139)

Todo docente autorizado en un curso puede consultar el **historial completo** de sus calibraciones
globales y subcalibraciones, aunque otra persona autorizada haya creado una versión. La lectura se
autoriza por **pertenencia vigente al curso**, no por autoría de la corrida. El historial muestra
creador, fechas, versiones, resultados y auditoría, sin ampliar el acceso a cursos ajenos ni a
contenido privado no asociado al curso.

### 6.2 Reactivación de versiones históricas aprobadas (D-140)

Cualquier docente actualmente autorizado en el curso puede **reactivar** una versión histórica que
haya aprobado la calibración y no haya sido descartada por un fallo de calibración. La reactivación
valida el estado aprobado y vigente de la versión antes de cambiar la referencia activa. Versiones
fallidas o descartadas permanecen consultables para auditoría, pero **no son activables**; la
transición conserva actor, motivo, momento y configuración efectiva.

### 6.3 Precondiciones de seguridad y vigencia al reactivar (D-141)

**No puede reactivarse** una versión histórica aprobada si usa una skill deshabilitada por seguridad o
un modelo cuya calibración perdió vigencia. La reactivación verifica las versiones de skills y el
estado actual del deployment evaluador antes de aplicarse. Si alguna precondición falla, el sistema
explica la causa y exige reemplazo seguro y/o recalibración; el historial aprobado queda intacto, pero
no se reutiliza de forma insegura.

## 7. Concurrencia de calibraciones con reserva agregada de cuota (D-158)

Docentes distintos pueden ejecutar calibraciones en paralelo; **cada docente ejecuta una sola corrida
a la vez**. Antes de cada inicio, `llm-service` valida que el saldo de cuota alcance también para las
reservas ya tomadas por otras corridas concurrentes.

La admisión **reserva de manera atómica** el presupuesto estimado de toda corrida antes de invocar al
proveedor, evitando que dos docentes sobreasignen la misma cuota. Si no hay saldo suficiente, la nueva
corrida no inicia y el docente recibe una causa clara; al finalizar o fallar, la reserva se **concilia
con el uso real** y libera el remanente.

> **Nota de alcance:** esta decisión describe la regla de concurrencia y admisión que corresponde a
> este bloque de dominio. El **libro de cuota** en sí —reserva persistente, vencimiento, conciliación
> contra uso real y saldo no expuesto por el proveedor— es el bloque técnico 2 del plan, declarado
> **fuera de alcance del sprint actual**: no se diseña ni implementa aquí su almacenamiento,
> contratos ni persistencia. D-158 sólo fija la regla de negocio que ese libro de cuota deberá cumplir
> cuando se aborde.

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-159 | Modelo lógico propio y trazable de LLM: inventario de agregados persistidos y separación snapshot/metadato operativo. |
| D-137 | Concurrencia en activación de calibración global (control optimista, rebase). |
| D-138 | Concurrencia en activación de subcalibración. |
| D-139 | Lectura compartida del historial por curso autorizado. |
| D-140 | Reactivación autorizada de versiones históricas aprobadas. |
| D-141 | Precondiciones de seguridad y vigencia al reactivar. |
| D-155 | Corridas de calibración sin cancelación manual. |
| D-156 | Falla transitoria de calibración sin reintento automático. |
| D-157 | Indicadores de scores diferidos por curso. |
| D-158 | Concurrencia de calibraciones con reserva agregada de cuota (regla; libro de cuota fuera de alcance). |
| D-161 | Máquina de estados de `CalibrationRun`, `CalibrationActivation`, `CalibrationVerification` y reglas de concurrencia. |
