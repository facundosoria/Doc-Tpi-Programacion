# 40 - Control de consolidación documental y primera entrega

> **Estado:** documento de trabajo y control. No reemplaza ninguna fuente de verdad.  
> **Creado:** 2026-09-14.  
> **Propósito:** ordenar los puntos conversados para corregir la documentación sin borrar información, y verificar qué falta para poder afirmar que la primera entrega de Tema 07 está documentada al 100 %.

## 1. Resultado de la revisión

La documentación contiene información valiosa y no se debe eliminar de forma automática. Hay tres clases de superposición:

| Clase | Tratamiento | Ejemplos identificados |
|---|---|---|
| Misma información para audiencias distintas | Se conserva; se enlaza a la fuente canónica para evitar divergencia. | Diagramas de 17 y contratos de [`contracts/equipos/tema-05-desafios-practicos.md`](../contracts/equipos/tema-05-desafios-practicos.md) o [`contracts/equipos/tema-11-chat.md`](../contracts/equipos/tema-11-chat.md). |
| Planes sucesivos | Se conserva como historial; se marca claramente su vigencia y se deriva al plan actual. | [10](../01-vision-alcance-y-entrega/02-entregables-y-plan.md)*no** se resuelve agregando una adenda que deje vigente una regla errónea en el documento que explica el tema.

1. Se actualiza el documento temático canónico donde una persona buscaría esa regla.
2. Se conserva el antecedente histórico, identificado como tal; no se elimina información.
3. Se registra el cambio con el estado anterior, el motivo, el estado nuevo, la fecha y los documentos afectados.
4. Los contratos ejecutables se actualizan si el cambio modifica una ruta, evento, payload, estado o invariante observable por otro servicio.
5. Los documentos de planificación o presentación solo repiten el detalle cuando es necesario para su audiencia; en otro caso enlazan al documento canónico.

La jerarquía ya definida en [00](01-fuentes-de-verdad-y-convenciones.md) sigue aplicando:

1. PRD y adendas de producto expresamente aprobadas.
2. Propuesta de arquitectura de la cátedra.
3. `00`, contratos y ADR.
4. Resto de `docs/`.

## 2. Regla de evolución documental acordada

Cuando cambia una decisión, el cambio **no** se resuelve agregando una adenda que deje vigente una regla errónea en el documento que explica el tema.

1. Se actualiza el documento temático canónico donde una persona buscaría esa regla.
2. Se conserva el antecedente histórico, identificado como tal; no se elimina información.
3. Se registra el cambio con el estado anterior, el motivo, el estado nuevo, la fecha y los documentos afectados.
4. Los contratos ejecutables se actualizan si el cambio modifica una ruta, evento, payload, estado o invariante observable por otro servicio.
5. Los documentos de planificación o presentación solo repiten el detalle cuando es necesario para su audiencia; en otro caso enlazan al documento canónico.

La jerarquía ya definida en [00](01-fuentes-de-verdad-y-convenciones.md) sigue aplicando:

1. PRD y adendas de producto expresamente aprobadas.
2. Propuesta de arquitectura de la cátedra.
3. `00`, contratos y ADR.
4. Resto de `docs/`.

### 2.1 Registro inicial de cambios que hay que propagar

| ID | Fecha conocida | Antes | Por qué cambió | Ahora | Fuente aprobatoria | Documentos que deben quedar alineados | Estado |
|---|---|---|---|---|---|---|---|
| DOC-001 | 2026-09-08 (ADR) | Cinco dimensiones y pesos `30/25/20/15/10` inmutables por curso. | El Product Owner pidió y aprobó adaptar la rúbrica a cada curso sin perder las cinco dimensiones, trazabilidad ni resultados históricos. | Cinco dimensiones obligatorias; en borrador se editan criterios, anclas, prompts y pesos. Los pesos suman 100 %. Al publicar, la versión es inmutable; todo cambio crea una versión nueva. | [ADR-017](02-decisiones-y-pendientes.md#adr-017--rúbricas-editables-y-versionadas-por-curso). Es una adenda limitada al PRD. | Canónicos: [13](../03-capacidades-de-ia/03-rubricas-y-prompts.md)nstruccion-del-producto.md) | Plan de construcción de 19 sprints. | Histórico; su horizonte fue retirado. |
| [35](../07-planificacion-y-trabajo-equipo/04-backlog-ejecutable.md) | Recetas y estimaciones detalladas; algunas siguen siendo antecedentes útiles. | Mixto; no es el calendario rector. |
| [38](../07-planificacion-y-trabajo-equipo/05-plan-de-cinco-sprints.md) | Calendario rector vigente de máximo cinco sprints. | Vigente. |
| [30](../07-planificacion-y-trabajo-equipo/02-arranque-agil-y-sprint-0.md) | Vista de entrega y arranque; reúne enlaces de trabajo. | No es fuente de verdad por su propia declaración. |
| [31](../03-capacidades-de-ia/golden-set-y-calibracion/01-plan-de-revision.md)íncronas a través del Gateway.
- **AsyncAPI** describe eventos asíncronos: topic, quién publica, quién consume y el payload. En esta plataforma se aplica a Kafka.

Los contratos existentes son [OpenAPI v1](../contracts/llm-service.openapi.yaml), [OpenAPI v2 Golden Set/calibración](../contracts/llm-service.openapi.yaml)y [AsyncAPI v1](../contracts/llm-service.asyncapi.yaml)

## 3. Cronología y función de los documentos superpuestos

Las fechas de creación que muestra Git quedaron afectadas por una consolidación de rutas del 2026-09-12. Por eso no deben usarse solas para decidir si un archivo es duplicado. La cronología útil se determina por contenido, historial y rótulos de vigencia.

| Grupo | Papel que conserva | Estado de vigencia |
|---|---|---|
| [10](../01-vision-alcance-y-entrega/02-entregables-y-plan.md) | Plan inicial de entregables. | Histórico; remite a [38]. |
| [20](../07-planificacion-y-trabajo-equipo/01-backlog-y-sprints.md) | Backlog y sprints de una planificación posterior. | Histórico; remite a [38]. |
| [23](../07-planificacion-y-trabajo-equipo/03-plan-de-construccion-del-producto.md) | Plan de construcción de 19 sprints. | Histórico; su horizonte fue retirado. |
| [35](../07-planificacion-y-trabajo-equipo/04-backlog-ejecutable.md) | Recetas y estimaciones detalladas; algunas siguen siendo antecedentes útiles. | Mixto; no es el calendario rector. |
| [38](../07-planificacion-y-trabajo-equipo/05-plan-de-cinco-sprints.md) | Calendario rector vigente de máximo cinco sprints. | Vigente. |
| [30](../07-planificacion-y-trabajo-equipo/02-arranque-agil-y-sprint-0.md) | Vista de entrega y arranque; reúne enlaces de trabajo. | No es fuente de verdad por su propia declaración. |
| [31](../03-capacidades-de-ia/golden-set-y-calibracion/01-plan-de-revision.md), [32](../03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md), [33](../03-capacidades-de-ia/golden-set-y-calibracion/03-modelo-de-dominio-y-transiciones.md) | Plan de revisión, especificación funcional y modelo de dominio, respectivamente. | Complementarios; no son copias entre sí. |

## 4. Contratos entre microservicios: estado actual

### 4.1 Qué es cada contrato

- **OpenAPI** describe una API HTTP: rutas, verbo, seguridad, headers, request, response y errores. Es el acuerdo para llamadas síncronas a través del Gateway.
- **AsyncAPI** describe eventos asíncronos: topic, quién publica, quién consume y el payload. En esta plataforma se aplica a Kafka.

Los contratos existentes son [OpenAPI v1](../contracts/llm-service.openapi.yaml), [OpenAPI v2 Golden Set/calibración](../contracts/llm-service.openapi.yaml) y [AsyncAPI v1](../contracts/llm-service.asyncapi.yaml).

### 4.2 Cobertura por par

| Par | Intercambio esperado | Evidencia actual | Brecha que impide decir “contrato cerrado” |
|---|---|---|---|
| Tema 05 / `practice-service` | Invocar tutor; publicar `intento_cerrado`; consumir score de IA. | Ruta `POST /api/llm/tutor/interactions` en OpenAPI v1 y contrato de Tema 05. `ATTEMPT_CLOSED` y `SCORE_CALCULATED` existen en AsyncAPI. | La solución esperada necesaria para anti-fuga no tiene endpoint, verbo ni payload acordado. Los payloads publicados de score están vacíos/genéricos en AsyncAPI. |
| Tema 02 / `courses-service` | Consultar calibración activa y evaluaciones pendientes antes de activar/cerrar un curso. | Rutas de curso en OpenAPI v2 y documentación de Tema 02. | OpenAPI v1, que [00](01-fuentes-de-verdad-y-convenciones.md)
| Tema 03 / `challenges-service` | Recibir indirectamente de Tema 05 el score para aplicar XP. | [00] y contratos Tema 03/05 aclaran que no existe llamada directa LLM ↔ Tema 03. | El relay Tema 05 → Tema 03 queda fuera del contrato directo de Tema 07, pero el schema final del score debe definirse para que Tema 05 pueda reenviarlo sin reinterpretarlo. |
| Tema 12 / `admin-service` | Gestionar modelos, Golden Set base, calibraciones y operaciones docentes delegadas. | OpenAPI v2 modela rúbricas, Golden Set y calibraciones; documentación de Tema 12. | Hay diferencias entre rutas declaradas, v2 marcada `draft` y controladores existentes. Falta consolidación y reglas de fallo de calibración/operación. |
| Tema 11 / chat | Moderación de mensajes en Fase 2. | Borrador `llm-service-v1-moderacion-borrador.yaml`. | No forma parte de la primera entrega; su contrato es explícitamente borrador/no implementado. |

### 4.3 Brechas transversales de contrato

1. **No hay un contrato Kafka completo para los eventos publicados.** `SCORE_CALCULATED`, `SCORE_DEFERRED`, los resultados de calibración y el incidente de jailbreak solo declaran el envelope y un `data` genérico. Deben definir campos obligatorios, tipos, semántica, versionado, idempotencia y errores de consumo.
2. **La solución esperada para RF-IA-20 no está contratada.** [Tema 05 pendientes](../07-planificacion-y-trabajo-equipo/11-equipos/tema-05-desafios-practicos/pendientes.md) declara que sin ella no se puede comparar la salida del tutor contra la solución y, por lo tanto, implementar anti-fuga.
3. **OpenAPI v1 y v2 no tienen un estado único explícito.** V1 es referida como vigente desde [00], pero v2 contiene las rutas de rúbricas, Golden Set y calibración que necesita la primera entrega y se presenta como `2.0.0-draft`. Hay que elegir y publicar una fuente ejecutable única o declarar formalmente la partición por alcance.
4. **Hay rutas documentadas sin correspondencia inequívoca en los controladores y controladores más nuevos sin representación completa en v1.** La verificación debe ser ruta por ruta, no solo por título de documento.
5. **Los esquemas de consulta para cursos** (calibración activa, asignación a desafíos y evaluaciones pendientes) deben incluir respuestas y códigos de error que permitan a Tema 02 bloquear sin inventar reglas.

## 5. Primera entrega según la propuesta de arquitectura

La propuesta `TUP_PIV_BE_PROPUESTA_ARQ.pdf`, página 7, no usa la etiqueta “Fase 1”. Para Tema 07 llama **“Pedido para empezar”** a estas seis capacidades. Esta es la base de control solicitada.

| # | Entregable de la propuesta | Interpretación vigente para este proyecto | Documentación existente | Estado documental | Falta para 100 % |
|---:|---|---|---|---|---|
| 1 | Rúbricas con pesos fijos `30/25/20/15/10` | La adenda aprobada ADR-017 reemplaza solo la rigidez de criterios, anclas, prompts y pesos: las cinco dimensiones continúan obligatorias, los pesos suman 100 % y las versiones publicadas son inmutables. | [00], ADR-017, [31], [32], [33], OpenAPI v2. | **Parcial.** La regla nueva está definida, pero convive con textos que dicen “pesos fijos”. | Propagar DOC-001 a [04], [13](../03-capacidades-de-ia/03-rubricas-y-prompts.md)
| 2 | Invocación del modelo | Adapter de modelo detrás de `LlmAdapter`; llamada del tutor con contexto validado, guardarraíles y trazabilidad. | [02](../02-arquitectura-y-plataforma/01-arquitectura-y-stack.md), ADR-016, [03], [04](../03-capacidades-de-ia/02-funciones-de-ia.md)
| 3 | Golden Set base | ADMIN publica el Golden Set base; el curso trabaja con una copia independiente y versionada. Es contenido docente, no un entregable que pueda producir el equipo de desarrollo por sí solo. | [31], [32](../03-capacidades-de-ia/golden-set-y-calibracion/02-especificacion-funcional.md)trazabilidad. | [31](../03-capacidades-de-ia/golden-set-y-calibracion/01-plan-de-revision.md)der, se compara la salida del tutor contra la solución esperada del desafío; la solución no entra al Golden Set ni se devuelve al alumno. | [05](../04-seguridad-datos-y-cumplimiento/01-seguridad-y-guardarrailes.md), 17 DOC-001 conserve contradicciones en documentos temáticos canónicos;
- no exista una fuente OpenAPI vigente y coherente para Golden Set, rúbricas, calibración y consultas de curso;
- AsyncAPI no especifique los datos de cada evento publicado/consumido;
- no esté firmado el contrato de solución esperada de Tema 05 para anti-fuga;
- no se verifique cada contrato contra los controladores y pruebas de integración.

### 5.1 Dictamen de cumplimiento documental

**La documentación no cumple todavía 100 % de la primera entrega.** No es por ausencia de explicación funcional: los seis puntos están identificados y, salvo anti-fuga, cuentan con diseño sustantivo. No puede declararse completa mientras:

- DOC-001 conserve contradicciones en documentos temáticos canónicos;
- no exista una fuente OpenAPI vigente y coherente para Golden Set, rúbricas, calibración y consultas de curso;
- AsyncAPI no especifique los datos de cada evento publicado/consumido;
- no esté firmado el contrato de solución esperada de Tema 05 para anti-fuga;
- no se verifique cada contrato contra los controladores y pruebas de integración.

## 6. Orden propuesto para resolverlo punto por punto

| Orden | Acción verificable | Resultado que habilita |
|---:|---|---|
| 1 | Confirmar DOC-001 con Product Owner como adenda identificada y propagarlo a los documentos canónicos. | Ya no quedan reglas contradictorias sobre edición de rúbricas. |
| 2 | Definir con Tema 05 el contrato mínimo y seguro para obtener/usar la solución esperada. | Se puede implementar y probar RF-IA-20. |
| 3 | Completar schemas AsyncAPI de intento cerrado, score calculado/diferido, resultados de calibración e incidente. | Tema 05, Tema 02 y Tema 03 integran eventos sin adivinar datos. |
| 4 | Decidir el estado de OpenAPI v1 y v2 y consolidar rutas, responses y errores de primera entrega en una fuente ejecutable. | Los equipos tienen un único contrato HTTP de referencia. |
| 5 | Contrastar cada operación del contrato final contra controller, autorización, tests de contrato e integración vía Gateway. | Se puede afirmar “documentado e implementable”, no solo “diseñado”. |
| 6 | Registrar los resultados, fecha, responsables y evidencia de prueba en este control o en un registro de cambios enlazado desde [00]. | Trazabilidad incremental sin sustituir ni borrar antecedentes. |

## 7. Criterio de cierre

Podrá marcarse la primera entrega como **documentada al 100 %** solo cuando cada fila de la sección 5 tenga:

- una regla funcional vigente sin contradicciones;
- fuente canónica enlazada desde [00];
- contrato HTTP o Kafka completo cuando cruce límites de servicio;
- responsable externo y dependencia explícita cuando corresponda;
- correspondencia verificada con controller/consumidor/productor y prueba de integración;
- evidencia de la prueba o una marca explícita de que la capacidad sigue fuera de implementación.

Mientras tanto, este documento es un tablero de control; no cambia el alcance ni reemplaza al PRD, a la propuesta de arquitectura ni a los ADR aprobados.
