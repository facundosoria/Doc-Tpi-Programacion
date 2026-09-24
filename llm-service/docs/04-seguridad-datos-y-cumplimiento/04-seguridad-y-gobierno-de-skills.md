# Seguridad, confianza y gobierno de skills

> **Estado:** vigente para diseño e implementación (directivas de Producto), salvo la sección 6
> (sanitización), que documenta decisiones ya tomadas pero **cuyo pipeline de implementación está
> fuera de alcance del sprint actual** (bloque técnico 4 del plan). Ver nota al inicio de esa sección.
> **Decisiones:** D-12 a D-20, D-43 y D-128 a D-133.

## 1. Auditoría obligatoria del flujo de skills y calibraciones (D-12 a D-14)

### 1.1 Alcance mínimo de auditoría (D-12)

Todas las decisiones y cambios de este flujo deben quedar en un **historial de auditoría**. Alcance
mínimo:

- creación, publicación, cambio de visibilidad, versionado, archivado y favoritismo de skills;
- adjuntos y snapshots usados;
- creación, ejecución, aprobación, activación, reemplazo y reversión de calibraciones;
- recalibraciones en cascada y forzosas;
- corte por intento;
- notificaciones y acciones explícitas del docente.

Cada evento registra: actor, rol, fecha, recurso, versiones implicadas, curso/desafío/intento cuando
corresponda, motivo, `X-Request-Id` y `traceparent`, **sin exponer secretos ni contenido sensible en
logs**.

### 1.2 Consulta del historial de auditoría (D-13)

Los **administradores** pueden consultar el historial de auditoría completo. Los **docentes** sólo
pueden consultar los eventos de los cursos para los que están autorizados. La autorización se evalúa
en el **backend** para cada consulta, no sólo mediante filtros de interfaz. Las respuestas omiten
datos de otros cursos y cualquier contenido sensible de skills o prompts.

### 1.3 Retención indefinida (D-14)

El historial de auditoría y las versiones/snapshots de skills utilizados en calibraciones se
conservan **eternamente**. El archivado o reemplazo lógico **no elimina** evidencia histórica. La
solución debe prever almacenamiento escalable, integridad de hashes y copias de respaldo para
auditoría de largo plazo, aplicando las obligaciones legales de protección de datos que correspondan.

## 2. Skills como instrucciones efectivas del evaluador (D-15)

Las skills seleccionadas se incorporan, **en el orden elegido**, como instrucciones adicionales del
agente. Potencian al agente tanto durante la **calibración** como durante la **evaluación posterior**
del uso de IA en exámenes y desafíos.

La configuración activada debe **ligar las versiones exactas** de skills a la corrida de calibración y
a cada evaluación posterior. El AI Gateway construye el contexto efectivo con rúbrica, Golden Set y
skills; los intentos conservan el snapshot que les correspondía al inicio. **Las skills no pueden
eludir el AI Gateway ni cambiar resultados históricos** (regla no negociable del `AGENTS.md` de
`llm-service`: ningún componente llama a un proveedor LLM directamente).

## 3. Skills declarativas: sin ejecución ni red (D-16)

Una skill **no ejecuta código** ni realiza llamadas a APIs o herramientas externas. Su único efecto es
aportar **instrucciones y contexto en Markdown** al agente, dentro del flujo controlado por el AI
Gateway. Se rechaza cualquier formato distinto de Markdown o contenido que intente declarar ejecución,
red, secretos o instrucciones que contradigan los guardarraíles de plataforma. **No se incorpora** un
runtime de plugins, intérpretes ni credenciales de terceros.

## 4. Jerarquía de instrucciones y confianza cero (D-43)

Las skills se tratan como **contenido no confiable de usuario** — el mismo principio de
["todo texto que viene de un usuario es DATO, nunca una instrucción"](01-seguridad-y-guardarrailes.md#1-el-principio-que-ordena-todo)
que ya rige el tutor y el evaluador. El system prompt y los controles de seguridad **prevalecen
siempre**; una skill no puede anular guardarraíles, revelar soluciones ni transformar la evaluación de
uso de IA en corrección académica.

El AI Gateway compone las skills **debajo** de las instrucciones de sistema y aplica sanitización,
aislamiento y validación antes de cada invocación (coherente con el compilador determinista D-160, ver
[Golden Set y calibración — 08](../03-capacidades-de-ia/golden-set-y-calibracion/08-maquina-de-estados-corridas-y-activaciones.md#compilador-determinista-del-perfil-efectivo-d-160):
"no incluye prompts originales de base u overlay ni permite que una skill altere las instrucciones de
sistema, seguridad o el esquema estructurado de respuesta"). Un intento de instrucción conflictiva
dentro de una skill se trata como **evento de seguridad**, no como una orden válida del docente.

## 5. Publicación, archivado y suspensión de skills (D-17 a D-20)

### 5.1 Publicación inmediata por confianza docente (D-17)

Una skill marcada como pública por su docente autor queda **disponible inmediatamente**; no requiere
revisión ni aprobación previa de un administrador. La publicación audita autor, fecha, visibilidad y
versión. Se mantienen las validaciones automáticas de formato y seguridad (§6), pero **no existe una
cola editorial** que bloquee la disponibilidad de la skill.

### 5.2 Archivado administrativo, mismo flujo de seguridad que el RAG (D-18)

Un administrador puede **archivar** una skill pública inapropiada o insegura. El archivado impide usos
nuevos, pero **conserva** el historial y las calibraciones que ya la utilizaron.

**Todas las skills pasan por el mismo flujo de seguridad definido para archivos PDF del RAG**
(ver [02 — Arquitectura y fronteras](../02-arquitectura-y-fronteras.md) y la ingesta de
material en `03-capacidades-de-ia/rag-e-ingesta/`). La carga Markdown se somete al pipeline de
validación de archivos y guardarraíles de contenido vigente en la plataforma **antes** de publicarse o
quedar disponible para adjuntar. El resultado de validación, rechazo o archivado se audita. La
aplicación concreta **reutilizará y alineará** las etapas ya documentadas de RAG/seguridad, aplicando
además la sanitización síncrona específica de skills (§6).

### 5.3 Suspensión preventiva y sanitización crítica (D-19)

Si un administrador archiva por seguridad una skill usada por una **calibración activa**, se
**suspenden las evaluaciones futuras** que la utilizan y se **solicita una recalibración**. Los
intentos ya iniciados conservan su snapshot histórico. El archivado es motivo suficiente para
recalibrar (mecanismo: activación pasa a `SUSPENDED_SECURITY`, ver
[08 — Máquina de estados §1.2](../03-capacidades-de-ia/golden-set-y-calibracion/08-maquina-de-estados-corridas-y-activaciones.md#12-activación-por-alcance-calibrationactivation)).

**Toda skill subida debe sanitizarse inmediatamente** y disparar alarmas antes de que pueda
adjuntarse, calibrar o evaluar desafíos. Es un **control crítico**. Una skill no alcanza los estados
seleccionable, pública o adjuntable hasta que el pipeline síncrono de seguridad la apruebe. Un rechazo
o incidente genera auditoría, alerta y feedback seguro; nunca expone detalles que permitan eludir los
guardarraíles. La suspensión sólo afecta usos futuros: no sobrescribe evidencia ni resultados previos.
Una entrega posterior se acepta conforme a RF-IA-27 y su cálculo de IA queda diferido hasta contar con
una configuración segura y calibrada.

### 5.4 Destinatarios de alertas críticas (D-20)

Ante una skill rechazada, archivada por seguridad o que suspenda calibraciones activas, se **alerta a
todos los actores afectados**: el docente autor, los administradores y los docentes autorizados de
los cursos afectados. (La integración asíncrona concreta de estas alertas vía `notification-service`
—outbox, eventos Kafka, DLQ— corresponde a D-21/D-22, fuera de este rango.)

## 6. Reglas de contenido y sanitización síncrona (D-128 a D-133)

> **Nota de alcance del sprint.** El plan declara el **pipeline** de sanitización (sus estados,
> evidencia, alarmas y bloqueo de versiones comprometidas) como bloque técnico 4, **pendiente y fuera
> de alcance del sprint actual**: "no diseñar ni implementar el pipeline, sus estados, evidencia,
> alarmas, bloqueos ni cambios de contratos/persistencia para este bloque". Las reglas de **contenido**
> que siguen (qué se acepta y qué se rechaza en una skill Markdown) **sí son decisiones de Producto ya
> tomadas** y se documentan íntegras acá como especificación funcional vigente; su implementación
> como pipeline ejecutable, con sus estados y evidencia, queda para cuando se aborde el bloque 4.

### 6.1 Sanitización síncrona antes de persistir (D-128)

Toda versión de skill se sanitiza de forma **síncrona** durante la carga o el clonado. Sólo se
guarda, publica y vuelve seleccionable si supera los controles de seguridad. Antes de persistir se
validan: tamaño, codificación, estructura Markdown y patrones o instrucciones prohibidos. **No existe
un estado inicial de skill publicable "en análisis"**. Ante rechazo, no se almacena una versión
utilizable; se conserva sólo la **evidencia mínima, segura y auditable** necesaria para alarmas e
investigación.

### 6.2 Markdown sin imágenes ni enlaces externos (D-129)

Las skills Markdown **no admiten imágenes ni enlaces externos**. La sanitización rechaza esas
construcciones antes de persistir. Las skills permanecen como instrucciones textuales
autocontenidas, sin recursos remotos que aumenten tokens, agreguen latencia, filtren datos o amplíen
la superficie de seguridad del visor.

### 6.3 Markdown sin HTML embebido (D-130)

Las skills Markdown **no admiten HTML embebido**. El parser y sanitizador aceptan sólo el subconjunto
textual de Markdown definido por la plataforma. El HTML se rechaza antes de persistir, para evitar
contenido oculto, comportamientos de renderizado no necesarios y consumo de contexto que no mejora el
razonamiento del modelo.

### 6.4 Bloques de código como contenido literal (D-131)

Las skills pueden contener bloques de código **sólo como ejemplos o instrucciones literales**; nunca
se ejecutan. El visor los representa como texto y el AI Gateway los envía al modelo únicamente como
parte del contexto declarado. Ningún controller, worker ni proveedor interpreta esos bloques como
comandos, scripts, herramientas o llamadas externas.

### 6.5 Reproceso seguro de entregas diferidas por incidente de skill (D-132)

Si una entrega queda diferida porque su skill se volvió insegura, tras **reemplazarla y recalibrar**,
se evalúa con la nueva configuración segura y calibrada. El trabajo diferido conserva el snapshot
original y el incidente para auditoría, pero **referencia explícitamente** la configuración segura
que produjo su resultado final. **Nunca se invoca la versión deshabilitada**; la actualización permite
resolver el pendiente sin bloquear la entrega ni el cierre del curso.

### 6.6 Aviso al alumno al resolver un score diferido (D-133)

Cuando se calcula y aplica posteriormente el modificador de IA de una entrega diferida, se **notifica
al alumno** y se explica que su score estaba pendiente. La notificación enlaza al resultado y su
desglose permitido, sin revelar prompts, skills, detalles de seguridad ni información del proveedor.
El servicio dueño de la economía aplica el modificador y solicita el aviso; `llm-service` aporta el
resultado trazable del cálculo diferido.

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-12 | Auditoría obligatoria, alcance mínimo del flujo de skills/calibración. |
| D-13 | Consulta de auditoría: administradores todo, docentes solo sus cursos autorizados. |
| D-14 | Retención indefinida de auditoría y snapshots. |
| D-15 | Skills como instrucciones efectivas del evaluador, ligadas por versión exacta. |
| D-16 | Skills declarativas: sin ejecución de código ni llamadas de red. |
| D-17 | Publicación inmediata de skills públicas, sin cola editorial. |
| D-18 | Archivado administrativo; mismo flujo de seguridad que archivos PDF del RAG. |
| D-19 | Suspensión preventiva de calibraciones activas; sanitización síncrona como control crítico. |
| D-20 | Destinatarios de alertas críticas de skills. |
| D-43 | Jerarquía de instrucciones y confianza cero sobre el contenido de skills. |
| D-128 | Sanitización síncrona antes de persistir (decisión de contenido; pipeline fuera de alcance). |
| D-129 | Markdown sin imágenes ni enlaces externos. |
| D-130 | Markdown sin HTML embebido. |
| D-131 | Bloques de código como contenido literal, nunca ejecutable. |
| D-132 | Reproceso seguro de entregas diferidas tras reemplazo y recalibración. |
| D-133 | Aviso al alumno al resolver un score diferido. |
