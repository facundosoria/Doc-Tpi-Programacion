# 06 — Evaluación diferida, verificación y degradación del evaluador

> **Decisiones:** D-86 a D-87, D-92 a D-94, D-100 a
> D-109, D-113 a D-114, D-117 a D-119 y D-162, D-163, D-166. D-114 conserva una referencia futura del libro
> de cuota (punto 2), fuera del sprint. D-162, D-163 y D-166 son propuestas
> técnicas pendientes de confirmación: no autorizan cambios de implementación,
> persistencia ni contratos publicados.

### D-86 — Recalibración por pérdida periódica de aprobación

**Decisión de Producto:** un cambio de nombre no exige recalibración. Las
calibraciones activas se verifican mediante un job mensual o programado contra
el proveedor; si una calibración deja de aprobar, por ejemplo porque el
proveedor ajustó parámetros del modelo, el sistema solicita una nueva
recalibración.

**Implicancia de diseño:** el resultado de cada verificación conserva proveedor,
deployment, configuración efectiva, fecha, métricas y causa de pérdida de
aprobación. La solicitud de recalibración es auditable y no reescribe la
calibración histórica que previamente había aprobado.

### D-87 — Pérdida de vigencia y diferimiento del evaluador

**Decisión de Producto:** si la calibración periódica falla, el modelo deja de
ser apto para evaluar desafíos del curso de inmediato. El evaluador no conmuta a
un proveedor de respaldo.

**Implicancia de diseño:** el estado de calibración pasa a no vigente para las
nuevas ejecuciones del evaluador y la consulta de estado lo refleja. Las
entregas se aceptan y su cálculo de IA queda diferido hasta que el único modelo
evaluador activo vuelva a ser utilizable y vigente; la calibración fallida y los
intentos históricos quedan preservados para auditoría.

### D-92 — Configuración y deshabilitación de verificaciones periódicas

**Decisión de Producto:** el administrador puede configurar la frecuencia o
deshabilitar las verificaciones periódicas de calibración.

**Implicancia de diseño:** el backend aplica la política vigente de forma
durable y auditable. Deshabilitar la verificación no elimina ni modifica
calibraciones existentes, sus resultados ni su historial; sólo evita nuevas
ejecuciones programadas hasta que la política vuelva a habilitarse.

### D-93 — Motivo obligatorio sin notificación al deshabilitar verificaciones

**Decisión de Producto:** deshabilitar verificaciones periódicas exige un motivo
administrativo obligatorio, pero no notifica a los docentes.

**Implicancia de diseño:** la auditoría conserva administrador, fecha, política
anterior, motivo y eventual reactivación. La decisión no genera eventos de
notificación a docentes.

### D-94 — Ejecución manual de verificación periódica

**Decisión de Producto:** el administrador puede iniciar manualmente una
verificación de calibración fuera del calendario configurado.

**Implicancia de diseño:** la corrida manual aplica las mismas reglas,
proveedores, métricas, auditoría y efectos de validez que una ejecución
programada; se registra que su disparador fue administrativo.

### D-100 — Filtro de recursos que requieren recalibración

**Decisión de Producto:** la lista de LLM incluye un filtro específico para
recursos cuya calibración existente requiere recalibración.

**Implicancia de diseño:** el filtro utiliza el estado de calibración ya
registrado por LLM, por ejemplo tras una verificación periódica fallida o un
cambio de seguridad. Sólo mejora la visualización y selección del docente; no
genera una solicitud ni decide bloqueos de publicación.

### D-101 — Desactivación del requisito excluye verificaciones periódicas

**Decisión de Producto:** cuando `requiresCalibration` se desactiva, dejan de
ejecutarse verificaciones periódicas sobre sus calibraciones.

**Implicancia de diseño:** LLM conserva el historial, pero excluye el recurso
inactivo de la planificación de verificaciones hasta que el dueño vuelva a
marcarlo como requerido.

### D-102 — Reactivación según historial de recalibración del modelo

**Decisión de Producto:** al reactivarse `requiresCalibration`, una calibración
histórica puede continuar siendo válida si el modelo usado no fue recalibrado
durante el período inactivo. Si ese modelo fue recalibrado en dicho intervalo,
LLM informa el recurso como sin calibración válida.

**Implicancia de diseño:** LLM cruza la fecha de desactivación/reactivación con
la línea de tiempo de recalibraciones del deployment que usó cada configuración.
La respuesta de estado muestra el motivo de invalidez sin generar una corrida ni
una compuerta automática; el docente puede seleccionarlo para recalibrar.

### D-103 — Validez al reactivar contra el deployment efectivo

**Decisión de Producto:** al reactivarse un requisito, la validez de la
calibración se comprueba contra el único deployment evaluador activo en ese
momento, aunque la calibración histórica haya utilizado otro deployment.

**Implicancia de diseño:** LLM resuelve el deployment evaluador activo y exige
una calibración vigente para esa configuración. Una calibración histórica de
otro deployment queda preservada para auditoría, pero no habilita el estado de
calibración vigente del recurso reactivado.

### D-104 — Estado por falta de proveedor elegible calibrado

**Decisión de Producto:** si no existe ningún proveedor o deployment elegible
con calibración vigente, LLM responde el estado específico
`NO_CALIBRATED_PROVIDER_UNAVAILABLE`.

**Implicancia de diseño:** la respuesta identifica de forma segura el
deployment o la calibración faltante y su causa, para que el docente y el
responsable externo puedan diferenciar este caso de una calibración inexistente
del curso o desafío. No expone secretos, prompts ni detalles internos del
proveedor.

### D-105 — Ausencia de verificación no infiere necesidad de recalibración

**Decisión de Producto:** LLM no exige una calibración ni una verificación
inmediata al reactivar un requisito. Si durante su inactividad no existe una
recalibración registrada del deployment relevante, LLM no puede inferir que la
calibración histórica necesite recalibrarse.

**Implicancia de diseño:** la respuesta de estado refleja exclusivamente hechos
registrados —calibraciones, recalibraciones, seguridad y disponibilidad del
deployment—. La ausencia de verificaciones periódicas no convierte por sí sola
una calibración en inválida ni crea una obligación para el docente.

### D-106 — Calibración aislada por configuración de modelo

**Decisión de Producto:** un cambio explícito de proveedor, modelo o versión
de modelo crea un deployment distinto. Sus calibraciones no se heredan desde el
deployment anterior, porque modelos diferentes pueden evaluar de manera
distinta.

**Implicancia de diseño:** cada calibración y verificación se asocia a un
identificador inmutable de deployment. Al cambiar esa identidad, el estado del
recurso sólo puede ser válido con una calibración propia del nuevo deployment;
el historial anterior se conserva exclusivamente para auditoría.

### D-107 — Detección de cambios silenciosos del proveedor

**Decisión de Producto:** cuando un proveedor cambia silenciosamente el
comportamiento de un modelo sin informar una nueva versión, la verificación
periódica de calibración es el mecanismo para detectar la pérdida de vigencia.

**Implicancia de diseño:** LLM no presume cambios ni invalida una calibración
por especulación. Ejecuta la verificación configurada y, si ésta falla, registra
la recalibración necesaria y aplica las reglas ya definidas de estado,
diferimiento, auditoría y notificación.

### D-108 — Fallo operativo de una verificación periódica

**Decisión de Producto:** si una verificación periódica del modelo evaluador no
puede ejecutarse por cuota agotada del proveedor o por un error interno de
backend, queda pendiente de verificación; no se verifica con un fallback.

**Implicancia de diseño:** el agotamiento de cuota y el error de backend no
invalidan por sí mismos el último resultado de calibración. Se registra la
causa, el estado `verificationPending` y se reintenta según la política
operativa, con trazabilidad y auditoría.

### D-109 — Indisponibilidad del modelo evaluador único

**Decisión de Producto:** si se agota la cuota o el deployment evaluador activo
no está disponible, LLM marca el evaluador como indisponible. No se utiliza
fallback para sustituirlo.

**Implicancia de diseño:** la indisponibilidad no altera ni borra el historial
de calibraciones aprobadas; expresa que no hay un modelo utilizable para una
nueva evaluación. La entrega del desafío no se bloquea ni se posterga: LLM
informa la indisponibilidad y deja el cálculo de IA diferido, mientras conserva
evidencia de la causa.

### D-113 — Resiliencia de entregas ante indisponibilidad de IA (RF-IA-27)

**Decisión de Producto:** una indisponibilidad, degradación o cuota agotada de
un modelo o proveedor nunca bloquea, invalida ni posterga la entrega de un
desafío. Si el tutor de IA no está disponible, el alumno resuelve y entrega sin
asistencia y su score de uso de IA es neutro, sin bonus ni penalidad. Si el
evaluador no está disponible, la entrega se acepta, se otorgan en ese momento
el XP base y las monedas, y el score de uso de IA queda pendiente de cálculo
diferido para aplicar su modificador al restablecerse el servicio.

**Implicancia de diseño:** la aceptación de la entrega, el XP base y las
monedas pertenecen a sus servicios dueños; LLM expone o comunica que el cálculo
de IA está diferido, sin gobernar la entrega. La indisponibilidad del evaluador
se registra, notifica y se resuelve posteriormente
sin penalizar al alumno por la dependencia externa.

### D-114 — Reserva preventiva de cuota del evaluador

**Decisión de Producto:** antes de iniciar una evaluación, LLM reserva de forma
preventiva el presupuesto máximo estimado de tokens más un margen de seguridad.
Si la cuota disponible no cubre esa reserva, no inicia una nueva evaluación y la
deja en cálculo diferido hasta recuperar capacidad.

**Implicancia de diseño:** esta admisión protege la cuota y evita cortar una
evaluación en ejecución. Si aun con la reserva el proveedor responde por cuota
agotada durante una llamada, LLM no confirma un score parcial ni cambia de
modelo: descarta el resultado incompleto, reencola el cálculo íntegro y lo
ejecuta nuevamente con el mismo modelo evaluador activo cuando haya capacidad.
La entrega, el XP base y las monedas no se alteran.

### D-117 — Límite configurable de reintentos diferidos

**Decisión de Producto:** los cálculos de IA diferidos tienen un límite de
reintentos configurable, con cinco intentos como valor predeterminado. Al
agotarlo, el sistema emite un aviso.

**Implicancia de diseño:** cada trabajo conserva el número de intento, las
causas y los momentos de error. La política de reintentos se configura y audita
sin reescribir trabajos históricos; al llegar al límite, LLM detiene los
reintentos automáticos y genera la notificación correspondiente, manteniendo el
trabajo y su estado para trazabilidad.

### D-118 — Reanudación administrativa tras agotar reintentos

**Decisión de Producto:** tras alcanzar el límite de reintentos, sólo un
administrador puede reanudar manualmente el cálculo diferido una vez resuelta
la causa.

**Implicancia de diseño:** la reanudación crea una transición explícita y
auditable con administrador, motivo y momento. Conserva el historial de
intentos previo y vuelve a encolar el trabajo sin habilitar a docentes ni a
otros servicios a omitir el pendiente o forzar el cierre del curso.

### D-119 — Destinatarios del aviso por reintentos agotados

**Decisión de Producto:** al agotarse los reintentos de un cálculo diferido, se
notifica al administrador y a los docentes afectados.

**Implicancia de diseño:** la notificación identifica el recurso y que la
intervención administrativa es necesaria, sin revelar la entrega del alumno ni
detalles sensibles del proveedor. Se vincula al trabajo pendiente y al incidente
para que el seguimiento y la posterior reanudación sean auditables.

### D-162 — Propuesta: cola durable de evaluaciones diferidas

**Alcance:** punto 3. Esta propuesta define la cola y su idempotencia; no
define el libro de cuota, que permanece fuera de alcance.

**Propuesta de modelo:** una `DeferredEvaluation` representa la obligación de
calcular una única vez el score de uso de IA de un `attemptId`. Conserva la
asignación y el perfil efectivo que correspondían al intento, su razón de
diferimiento, número de intentos, próximo intento y el historial de fallos. No
almacena ni modifica el resultado académico, XP o monedas.

```text
DEFERRED -> QUEUED -> RUNNING -> COMPLETED
    ^          ^         |
    |          |         +-> RETRY_WAIT -> QUEUED
    |          |                              |
    |          +------------------------------+
    |
    +-- condición aún no apta

RETRY_WAIT -> RETRY_EXHAUSTED
RETRY_EXHAUSTED --reanudación administrativa--> QUEUED
```

- `DEFERRED` significa que no puede comenzar todavía por una causa tipada:
  `CALIBRATION_UNAVAILABLE`, `EVALUATOR_UNAVAILABLE`,
  `CALIBRATION_RECALIBRATION_REQUIRED` o `PROFILE_SECURITY_SUSPENDED`. Sólo
  una condición apta la mueve a `QUEUED`.
- `QUEUED` es apta para ser tomada por un worker; `RUNNING` posee un lease de
  ejecución. `RETRY_WAIT` conserva un `nextAttemptAt` calculado por la política
  vigente. `RETRY_EXHAUSTED` no se considera resuelta: sigue contando como
  score pendiente y bloquea el cierre del curso.
- `COMPLETED` es el único estado que quita la obligación pendiente. No existe
  una transición de omisión, vaciado, cancelación ni cierre forzado, incluso
  para Administración.
- La reanudación desde `RETRY_EXHAUSTED` requiere rol administrativo y motivo.
  Conserva todos los intentos previos, incrementa una época de reanudación y
  vuelve a `QUEUED`; no crea otra obligación para el mismo intento.
- El límite de reintentos se lee de la política vigente al programar el
  siguiente intento. El valor de inicio propuesto es cinco, conforme a D-117;
  un cambio de política no reescribe intentos ya registrados.

**Idempotencia de negocio propuesta:** existe una única obligación abierta o
completada por `(attemptId, evaluationKind=AI_USAGE)`. El intento conserva el
`evaluationProfileSnapshotId` que le correspondía al inicio. Al completar, la
misma transacción registra el resultado inmutable, marca la obligación
`COMPLETED` y prepara la comunicación posterior. Un índice único sobre esa
identidad impide que una repetición, un lease vencido o una respuesta tardía
creen dos scores.

El AI Gateway recibe además una clave estable derivada del `evaluationId` para
las invocaciones que el proveedor pueda hacer idempotentes. Si el proveedor no
confirma el resultado de una llamada que expiró, la cola puede reintentar la
invocación, pero nunca publica ni persiste un segundo score: sólo la primera
transacción que adquiere la unicidad de `evaluationId` puede completar.

**Reanudación y perfil histórico:** la cola nunca sustituye silenciosamente el
perfil del intento por la calibración activa posterior. Si el perfil histórico
está suspendido por seguridad o ya no es apto, la obligación queda diferida con
la causa correspondiente. La regla de negocio para habilitar una evaluación
histórica contra un perfil nuevo debe resolverse expresamente antes de
implementarse; mientras no exista esa decisión, no se remapea ni se puntúa el
intento con otra configuración.

**Concurrencia propuesta:** el worker reclama trabajos vencidos mediante lease
y transición condicional `QUEUED -> RUNNING`. Sólo quien conserva el mismo
`leaseId` puede terminar o programar el reintento. La recepción repetida de un
cierre de intento devuelve la misma obligación por su clave de negocio; no
duplica filas ni invocaciones simultáneas.

**Pendientes para confirmar:** el conjunto final de causas de diferimiento y
la política específica para un perfil histórico que queda suspendido de forma
permanente. Ninguno de esos pendientes habilita una excepción al bloqueo de
cierre.

### D-163 — Propuesta: ejecución de verificaciones periódicas y degradación

**Alcance:** punto 5. Complementa la máquina de estados D-161 y define cómo se
planifica y coordina una verificación; no diseña la reserva ni conciliación de
cuota del punto 2.

**Propuesta de planificación:** una `VerificationPolicy` administrada contiene
`enabled`, frecuencia, próxima ejecución y fecha de última modificación. Al
deshabilitarla se exige motivo administrativo y se detienen únicamente nuevas
ejecuciones programadas; no se modifica ninguna calibración ni resultado
histórico. Una solicitud administrativa manual crea una verificación con
origen `MANUAL`, pero aplica exactamente las mismas métricas y efectos que una
programada.

El planificador considera sólo activaciones de recursos que continúan
registrados con `requiresCalibration=true`. Por cada `(activationId,
verificationWindow)` crea como máximo una verificación. Una restricción única
evita que dos instancias del scheduler creen la misma ventana; si ya existe una
en `QUEUED` o `RUNNING`, un disparo posterior la reutiliza en lugar de abrir
otra.

**Propuesta de concurrencia:** las verificaciones se ejecutan con un pool
separado y con límite de concurrencia por deployment evaluador. Un lease evita
dos workers sobre la misma verificación. Las verificaciones manuales no
preemptan ni duplican una corrida abierta: se asocian a ella o se programan a
continuación según la política de prioridad que se confirme.

**Resultado y degradación:**

| Resultado de la verificación | Efecto sobre activación | Efecto sobre nuevas evaluaciones |
|---|---|---|
| `PASSED` | Permanece `ACTIVE`; actualiza última comprobación válida. | Pueden ejecutarse si el deployment está disponible. |
| `FAILED_TOLERANCE` | Pasa a `SUSPENDED_RECALIBRATION_REQUIRED`. | Se difieren hasta nueva corrida aprobada y activada. |
| `FAILED_OPERATIONAL` | Mantiene su última validez; marca `VERIFICATION_PENDING`. | Sólo se difieren si el evaluador también está `UNAVAILABLE`. |
| Activación sustituida durante la corrida | La verificación queda `CANCELLED`; no aplica su resultado. | No altera la nueva activación. |

No existe fallback para el evaluador. La recuperación operativa del deployment
restaura `evaluatorAvailability=AVAILABLE`, pero no recupera una activación que
falló por tolerancia o fue suspendida por seguridad: para eso se requiere una
nueva corrida `PASSED` y activación explícita, como fija D-161.

**Reintento propuesto:** sólo `FAILED_OPERATIONAL` se reprograma. La próxima
ejecución se decide por una política de backoff separada de los reintentos de
scores diferidos. El fallo operativo no se convierte en fallo de tolerancia ni
induce una recalibración por especulación.

**Pendientes para confirmar:** representación exacta de la frecuencia
(intervalo o calendario), límite de concurrencia por deployment y prioridad
entre una solicitud manual y una ventana programada ya abierta.

### D-166 — Propuesta: validación de respuesta estructurada del modelo

**Alcance:** punto 8. Esta propuesta regula la aceptación de una respuesta del
modelo y la evidencia mínima de una corrida; no crea el pipeline de
sanitización de skills ni una estrategia general de observabilidad.

**Propuesta de salida canónica:** el AI Gateway solicita modo de respuesta
estructurada estricto. Un deployment que no soporte un esquema JSON estricto no
es elegible para el evaluador. La respuesta transporta puntajes de unidades de
evaluación, no el puntaje final ponderado:

```json
{
  "schemaVersion": "1.0",
  "profileHash": "sha256-hex",
  "scores": [
    { "unitId": "AUTONOMY", "score": 74 },
    { "unitId": "AUTONOMY/PLANIFICATION", "score": 70 }
  ]
}
```

`unitId` identifica cada dimensión o subcriterio activo definido por el
`EffectiveEvaluationProfile` materializado. El servidor calcula el puntaje de
dimensión y el final exclusivamente a partir de esas puntuaciones y pesos; no
acepta que el modelo imponga un total, pesos, aprobación académica ni reglas de
composición.

**Pipeline de aceptación propuesto:**

1. El AI Gateway verifica transporte, tamaño máximo y que el proveedor haya
   declarado salida JSON estructurada.
2. Un parser estricto valida JSON, `schemaVersion`, propiedades permitidas y
   tipos. No repara JSON, extrae números desde texto libre ni ignora campos
   inesperados.
3. El validador compara `profileHash` con el snapshot de la invocación y exige
   que el conjunto de `unitId` sea exactamente el conjunto de unidades activas:
   sin faltantes, duplicados ni unidades desconocidas.
4. Cada `score` debe ser entero entre 0 y 100. Sólo entonces el servidor
   calcula agregados, desvíos de calibración y decisión PAR-14.
5. El resultado se persiste o se usa para completar una evaluación sólo si todo
   el pipeline aprobó. No existe score parcial, redondeo implícito ni publicación
   de un evento de score ante un fallo de validación.

Los códigos de fallo propuestos son `MODEL_RESPONSE_MALFORMED`,
`MODEL_RESPONSE_SCHEMA_MISMATCH`, `MODEL_RESPONSE_PROFILE_MISMATCH`,
`MODEL_RESPONSE_SCORE_OUT_OF_RANGE`, `MODEL_RESPONSE_SCORE_MISSING`,
`MODEL_RESPONSE_SCORE_DUPLICATED` y `MODEL_RESPONSE_UNIT_UNKNOWN`. Una corrida
de calibración termina con el código correspondiente, sin reintento automático
según D-156. Una evaluación diferida aplica su política de reintentos de D-162,
sin confirmar un resultado parcial.

**Evidencia mínima propuesta por invocación:** se conserva una referencia
inmutable con `invocationId`, `evaluationId` o `calibrationRunId`, `caseId`
cuando aplique, `profileHash`, versión de esquema, deployment, marcas de tiempo,
hash de request, respuesta normalizada, resultado de validación, código de
fallo y metadatos de proveedor disponibles. La respuesta cruda, si se conserva
para diagnóstico, queda en almacenamiento protegido y acotado; no se escribe en
logs ni se expone por contratos docentes. La transcript o prompt completo no se
duplica en esta evidencia: se referencia el snapshot que ya le corresponde.

**Pendientes para confirmar:** versión inicial exacta del esquema, si la
respuesta debe incluir justificación visible al docente, límite de tamaño de
salida y política de retención/acceso de la respuesta cruda. Hasta cerrarlos no
se modifica el contrato del AI Gateway ni se implementa un parser de producción.

