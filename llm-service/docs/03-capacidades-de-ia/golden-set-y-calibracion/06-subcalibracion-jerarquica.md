# Subcalibración por desafío — herencia, cascada y rebase

> **Estado:** vigente para diseño e implementación (directivas de Producto).
> **Decisiones:** D-07 a D-11, D-45 a D-51, D-56 a D-59 y D-73 a D-77. Amplía y en algunos puntos
> **reemplaza** la sección 11 ("Activación y asociación con desafíos") de
> [02 — Especificación funcional](02-especificacion-funcional.md), que describía una asociación
> simple de `ChallengeCalibrationAssignment` sin capas de herencia. Donde este documento contradice esa
> sección, gana este documento (ver nota en el archivo original).

## 1. Precondición: no hay subcalibración sin calibración global aprobada (D-07)

Un docente sólo puede **crear y activar** una calibración específica de desafío (subcalibración) si
el curso tiene, de antemano, una **calibración global activa y aprobada**.

- La interfaz **bloquea y explica** la subcalibración mientras esa condición no se cumple.
- El **backend valida esta condición de forma autoritativa**, tanto al crear como al activar una
  corrida de subcalibración por desafío — nunca depende únicamente del bloqueo visual.

## 2. Herencia editable al subcalibrar (D-08)

Al iniciar una subcalibración por desafío, el sistema **precarga**:

- la rúbrica de la calibración global activa,
- el Golden Set de la calibración global activa,
- las skills de la calibración global activa.

El docente puede **modificar cualquiera de esos valores** antes de iniciar la corrida. La vista
previa diferencia con claridad los valores **heredados** de los **reemplazados**.

Al confirmar, se crea una **configuración inmutable propia del desafío**: cambios posteriores de la
calibración global **no modifican** esa configuración ya confirmada ni su historial.

## 3. Una activa por alcance, con historial reactivable (D-09)

Cada **curso** tiene como máximo una calibración global **activa**; cada **desafío** tiene como
máximo una subcalibración **activa**. Recalibrar cualquiera de los dos niveles crea una **nueva
versión** y conserva todas las anteriores.

Una calibración anterior **aprobada** puede volver a habilitarse (reactivarse) para reemplazar la
activa actual. Las activaciones son cambios **explícitos, auditables y reversibles** hacia una
corrida histórica aprobada. Nunca se sobrescriben versiones, sus métricas ni las skills-snapshot
asociadas (ver D-140 y D-141 para las precondiciones exactas de reactivación, en
[08 — Máquina de estados](08-maquina-de-estados-corridas-y-activaciones.md)).

## 4. Recalibración global en cascada (D-10, D-11, D-47, D-48)

### 4.1 Regla general de cascada (D-10)

Al recalibrar un curso, deben recalibrarse también los **desafíos vinculados que no estén
terminados**.

> **Pendiente de Producto — desafío parcialmente realizado (D-10):** aún debe definirse qué ocurre
> con un desafío que ya fue iniciado por parte del curso cuando se impone una recalibración global
> por fuerza mayor. La resolución debe conservar la configuración histórica de los intentos ya
> comenzados y evitar cambios opacos o injustos para alumnos que estén cursando. **Este punto queda
> explícitamente abierto** y no debe resolverse por inferencia; D-11 fija la regla operativa mínima
> mientras tanto.

### 4.2 Corte por intento en la recalibración forzosa (D-11)

Ante una recalibración forzosa, la configuración se **congela por intento**:

- los intentos **iniciados** —incluidos los que están en curso— conservan la **versión anterior**;
- los intentos que **comiencen después** de activar la nueva configuración usan la **nueva versión**.

No se recalculan ni reinterpretan resultados históricos. Una recalibración global **crea las
subcalibraciones necesarias** para los desafíos no terminados; la configuración anterior sigue activa
hasta que la nueva **apruebe**. La interfaz muestra el alcance del cambio, la fecha de efectividad y
los intentos alcanzados por cada versión.

### 4.3 Inicio diferido de la cascada (D-47)

Las recalibraciones de desafíos vinculados se **inician sólo cuando la nueva calibración global
aprueba y el docente la activa**. Si la global falla o no se activa, **no se consumen recursos** en
subcalibraciones derivadas. `llm-service` marca como *requeridas para recalibración* sus
subcalibraciones asociadas; el servicio que gobierna el ciclo de vida del desafío determina cuáles
continúan vigentes y solicita las nuevas corridas correspondientes.

### 4.4 Alcance de la cascada por vigencia del desafío (D-48)

Al recalibrar un curso se recalibran únicamente los desafíos vinculados que **continúan vigentes**.
Los desafíos **cerrados o expirados no se recalibran** y conservan su historial. La verificación de
vigencia ocurre **fuera de `llm-service`**; este servicio conserva el historial y el estado de
recalibración de las asociaciones que ya conoce, sin consultar ni almacenar fechas de desafío.

Cada subcalibración nueva creada en cascada **copia la nueva configuración global** como punto de
partida. El docente puede modificarla antes de utilizarla — no queda atado a los recursos copiados
(ver D-50 para el flujo de revisión docente previo a la ejecución real).

## 5. Revisión docente antes de ejecutar la cascada (D-50)

Cuando una nueva calibración global activa alcanza a desafíos vigentes, el sistema crea una
**subcalibración derivada en estado de borrador pendiente de revisión**. Copia la nueva configuración
global, pero **no la ejecuta automáticamente**: el docente puede ajustarla y luego inicia su
recalibración de forma explícita.

La activación global crea **tareas durables de revisión por desafío**, vinculadas a la versión global
originadora, con trazabilidad de los cambios realizados antes de la ejecución. Mientras tanto, la
subcalibración previamente activa continúa gobernando las evaluaciones futuras, salvo una suspensión
de seguridad u otra excepción ya definida (ver
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md)).

## 6. Modelo híbrido: `CourseBaseline` + `ChallengeOverlay` (D-49)

### 6.1 Decisión de Producto

La subcalibración es **híbrida**: conserva una base de curso de mayor peso y puede acoplar una
rúbrica, Golden Set y skills específicas del desafío.

### 6.2 Decisión de diseño — dos capas inmutables

La relación **no concatena dos calibraciones completas en un prompt**. Se modela en capas explícitas:

| Capa | Contenido |
|---|---|
| `CourseBaseline` | Referencia a la versión activa de la calibración global y a su snapshot inmutable. |
| `ChallengeOverlay` | Referencias explícitas a los recursos elegidos para el desafío y a sus reemplazos/exclusiones respecto de la base. |
| `EffectiveEvaluationProfile` | Resultado determinista de resolver ambas capas antes de invocar al proveedor, con hash, versiones y orden de skills auditables. |

Reglas de composición:

- **Rúbrica:** la rúbrica global conserva las dimensiones y pesos canónicos del curso; la rúbrica del
  desafío aporta criterios, evidencias y restricciones situadas sobre esas mismas dimensiones. **El
  modelo (LLM) nunca decide qué regla prevalece ni fusiona pesos contradictorios** — eso lo resuelve
  el compilador determinista descripto en D-160 (ver
  [08 — Máquina de estados](08-maquina-de-estados-corridas-y-activaciones.md#compilador-determinista-del-perfil-efectivo-d-160)).
- **Golden Set:** el Golden Set global acredita la base del curso; el Golden Set del desafío valida el
  perfil híbrido para ese desafío específico. **Nunca se envían ambos conjuntos completos** como
  contexto de una misma llamada — se resuelve uno solo (ver D-60 y D-160).
- **Skills:** las skills de curso y de desafío se componen **por capas y orden explícito**,
  descartando duplicados por hash de contenido. Las instrucciones de sistema y seguridad siempre
  quedan **por encima** de ambas capas.
- **Presupuesto de contexto:** el compilador de contexto calcula de forma determinista el presupuesto
  total (instrucciones fijas, rúbrica efectiva, skills, evidencia del alumno y reserva de salida). Si
  excede el máximo certificado del modelo, **falla antes de llamar al proveedor** y explica al docente
  qué componente debe reducir o reemplazar (ver también D-153 en
  [09 — Flujos de UI](09-flujos-ui-calibracion-y-gestion-skills.md#estimación-visible-de-presupuesto-de-contexto-d-153)).
- **Salida:** se exige esquema estructurado y se valida contra el perfil efectivo, para reducir
  ambigüedad y alucinación.

## 7. Modificación de skills heredadas en la subcalibración (D-51)

El docente puede **quitar o reemplazar** skills heredadas de la calibración global al editar la
subcalibración de un desafío, además de **agregar** skills propias de ese desafío.

El `ChallengeOverlay` registra, por cada skill heredada, si se **conserva, excluye o reemplaza**, más
las skills agregadas y su orden. El perfil efectivo conserva el vínculo a la base global y
materializa el resultado final para auditoría, recalibración y evaluación posterior. La interfaz debe
distinguir con claridad las skills **heredadas, excluidas, reemplazadas y agregadas** para que el
cambio no sea accidental.

## 8. Exámenes como tipo de desafío (D-45, D-46)

### 8.1 `EXAM` es un tipo de desafío, no un servicio nuevo (D-45)

Los exámenes son desafíos de tipo `EXAM` publicados por `challenges-service`. **No se integra un
microservicio adicional.** La subcalibración aplica las **mismas reglas** de seguridad, versiones,
skills, activación y congelamiento por intento tanto a desafíos regulares como a exámenes.
`llm-service` sólo conserva su asociación de calibración — no el catálogo ni el tipo del desafío.

### 8.2 Filtro de tipo de desafío en el selector (D-46)

El selector de subcalibración permite **filtrar** entre desafíos regulares y exámenes, mostrando el
tipo de manera visible. El selector obtiene el tipo desde el dueño del catálogo
(`challenges-service`). El filtro es sólo una ayuda de interfaz: no cambia las reglas de elegibilidad
ni el flujo de calibración de `llm-service`.

## 9. Herencia de configuración y extensión de dimensiones (D-56 a D-59)

### 9.1 Herencia como composición, no herencia de clases (D-56)

La subcalibración adopta el **concepto** de herencia: parte de la calibración base del curso y,
dentro de ese marco, agrega criterios o modifica los existentes para el desafío. Técnicamente se
implementa mediante **composición de configuraciones inmutables**, no herencia de clases de
programación. Cada subcalibración guarda la referencia y el snapshot de su `CourseBaseline`, más un
`ChallengeOverlay` de diferencias explícitas. El resolvedor construye un único perfil efectivo: si una
propiedad no fue modificada, hereda el valor de la base. Esto permite comparar, auditar y recalibrar
sin duplicar ni mezclar prompts completos.

### 9.2 Extensión de dimensiones en una subcalibración (D-57)

> ⚠️ **Evolución material de dominio — ver [ADR-021](../../00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md).**

Además de heredar y modificar dimensiones, una subcalibración puede **agregar dimensiones nuevas**
(análogo a una subclase que incorpora una función propia). Ejemplo: heredar cuatro dimensiones del
curso y agregar una quinta para un examen concreto.

**Esta decisión reemplaza la restricción previa (correspondiente a D-53, ver
[07 — Rúbrica ponderada y subcriterios](07-rubrica-ponderada-y-subcriterios.md#dimensiones-heredadas-y-porcentajes-ajustables-por-desafío-d-53))
que exigía conservar exactamente el mismo conjunto de dimensiones entre curso y desafío.**

Reglas de una dimensión agregada:

- identificador inmutable, nombre, descripción, criterios;
- peso mayor o igual a cero;
- origen `CHALLENGE` (para distinguirla de las heredadas `COURSE`).

La rúbrica efectiva contiene las dimensiones heredadas, modificadas y agregadas, con **suma total
exactamente 100 %**. Las dimensiones heredadas no eliminadas permanecen trazables; un peso de 0 %
expresa su no participación (nunca se retiran del snapshot).

**Impacto técnico a planificar:** la implementación actual asume un conjunto fijo de cinco
dimensiones (ver `RubricDimension` en
[03 — Modelo de dominio](03-modelo-de-dominio-y-transiciones.md)). Esta capacidad exige evolucionar el
modelo, sus validadores, contratos y persistencia mediante **cambios aprobados explícitamente** antes
de desarrollar migraciones o APIs — ver ADR-021.

### 9.3 Máximo administrable de dimensiones adicionales (D-58)

El administrador puede configurar la **cantidad máxima** de dimensiones nuevas que una subcalibración
puede agregar a su base heredada. La creación y edición validan ese máximo antes de guardar o
calibrar. Este límite **complementa, pero no reemplaza**, la validación del presupuesto real de
contexto y tokens del proveedor (D-49, D-153, D-160). El mensaje de error debe indicar explícitamente
si se excedió la política administrativa o el límite técnico de la invocación.

### 9.4 Promoción opcional de una dimensión de desafío al curso (D-59)

Una dimensión creada para un desafío podrá, **a futuro**, promoverse a la rúbrica base del curso
mediante una nueva calibración global. **No es una capacidad bloqueante para la primera entrega.**

La primera versión conserva el origen `CHALLENGE` y la trazabilidad de la dimensión sin requerir
promoción. El backlog evolutivo incorporará una acción explícita de promoción que cree una nueva
versión de la rúbrica/calibración global y aplique las reglas ya acordadas de borradores de
subcalibración, revisión docente e historial (D-50, D-73).

## 10. Rebase de personalizaciones ante una nueva base global (D-73, D-74, D-76)

### 10.1 Intento de conservar personalizaciones (D-73)

Al activarse una nueva calibración global, cada **borrador derivado** de desafío intenta conservar
sus personalizaciones previas sobre la **nueva base**. El sistema debe **señalar los conflictos** para
revisión, sin descartar cambios ni aplicar decisiones silenciosas.

El rebase compara `CourseBaseline` anterior, overlay anterior y nueva base, y genera diferencias
clasificadas:

- **aplicadas sin conflicto**,
- **heredadas nuevamente**,
- **inválidas**,
- **en conflicto**.

El docente recibe una vista de comparación de dimensiones, subcriterios, pesos, Golden Set y skills;
su resolución queda versionada y auditada antes de iniciar la recalibración.

### 10.2 Conflictos bloquean la nueva corrida (D-74)

Un borrador de subcalibración con **conflictos de rebase sin resolver no puede iniciar una
recalibración**. El estado `CONFLICTED` comunica el motivo y las acciones pendientes, sin invocar al
proveedor ni consumir cuota. La subcalibración activa anterior sigue rigiendo las evaluaciones futuras
hasta que el docente resuelva los conflictos, complete el borrador, lo calibre y lo active; se
mantienen las excepciones de seguridad ya acordadas (suspensión preventiva, ver
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md)).

### 10.3 Resolución adoptando la nueva base (D-76)

Un docente puede resolver un conflicto de rebase **descartando explícitamente** las personalizaciones
del desafío y **adoptando la nueva configuración base global**. La acción sólo modifica el borrador
actual y requiere confirmación con una vista de los cambios que se perderán. El overlay previo, la
subcalibración activa y todas las versiones históricas se conservan intactas. La auditoría registra
actor, momento, conflicto y decisión de volver a la base.

## 11. Vencimiento durante la revisión de un borrador derivado (D-77)

Si un desafío **expira** mientras su borrador derivado espera revisión, el sistema lo **cancela** y no
ejecuta su recalibración. El **servicio dueño del vencimiento** determina la cancelación y evita
solicitar una nueva corrida — `llm-service` no ejecuta controles de calendario propios; conserva el
borrador y el historial cuando recibe la cancelación por el mecanismo de integración que se acuerde
con `challenges-service`.

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-07 | Precondición: calibración global activa y aprobada antes de subcalibrar. |
| D-08 | Herencia editable al iniciar la subcalibración. |
| D-09 | Una activa por alcance; historial reactivable. |
| D-10 | Cascada de recalibración; pendiente de Producto sobre desafíos parcialmente realizados. |
| D-11 | Corte por intento en recalibración forzosa. |
| D-45 | Exámenes como desafíos de tipo `EXAM`. |
| D-46 | Filtro de tipo de desafío en el selector. |
| D-47 | Inicio diferido de la cascada hasta aprobar y activar la global. |
| D-48 | Alcance de la cascada limitado a desafíos vigentes. |
| D-49 | Modelo híbrido `CourseBaseline` + `ChallengeOverlay` + `EffectiveEvaluationProfile`. |
| D-50 | Borrador de revisión docente antes de ejecutar la cascada. |
| D-51 | Modificación de skills heredadas (conservar/excluir/reemplazar/agregar). |
| D-56 | Herencia como composición de configuraciones inmutables. |
| D-57 | Extensión de dimensiones en subcalibración — requiere ADR-021. |
| D-58 | Máximo administrable de dimensiones adicionales. |
| D-59 | Promoción opcional de dimensión de desafío al curso (no bloqueante). |
| D-73 | Rebase de personalizaciones ante nueva base global. |
| D-74 | Conflictos de rebase bloquean la nueva corrida. |
| D-76 | Resolución de conflicto adoptando la nueva base. |
| D-77 | Vencimiento durante revisión de borrador derivado. |
