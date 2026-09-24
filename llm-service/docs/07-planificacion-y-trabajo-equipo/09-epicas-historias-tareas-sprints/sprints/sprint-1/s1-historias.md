# Sprint 1 — vista de historias (índice, tipo y demo)

> **Qué es este documento.** La vista **de sprint** de las nueve historias de S1: el
> objetivo, el índice, el tipo de cada una (HU de valor / habilitador) y la demo. Las
> **fichas completas** (Como/Quiero/Para, Notas, CA con negativos, BDD, Prototipo,
> Estimación, Dependencias) viven agrupadas por épica en
> [`../../historias/ep-01/`](../../historias/ep-01/README.md) y
> [`../../historias/ep-03/`](../../historias/ep-03/README.md). El **desglose en tareas SMART**, en
> [`../../tareas/`](../../tareas/README.md). La versión de las nueve historias contada **sin
> jerga técnica** está en [`s1-explicado.md`](s1-explicado.md). El catálogo y guía de carga para Taiga de todas las **historias agregadas** está en [`historias-agregadas-s1.md`](historias-agregadas-s1.md).
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato de acá no coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | [`35` · «S1»](../../../04-backlog-ejecutable.md) |
> | Tipo (HU de valor / habilitador) y demo | [30 · §5](../../../02-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../../../03-plan-de-construccion-del-producto.md) |
> | Fichas completas de HU | [`../../historias/`](../../historias/README.md) |
> | Tareas | [`../../tareas/`](../../tareas/README.md) |
> | Registro del sprint (capacidad, compromiso, cierre) | *(copia de [`../../plantillas/sprint-llm.md`](../../../10-plantillas/sprint-llm.md) en la Planning de S1)* |
> | Secuencia técnica de cada tarea | [playbook de construcción · §4](../../../06-playbook-de-construccion.md) |
>
> **Título en Taiga.** Cada ficha se carga con el título `GXX — TÍTULO` (`GXX` = número
> de grupo, aún sin asignar). El ID interno `LLM-S01-Hyy` es el del equipo
> ([23 · §9.2](../../../03-plan-de-construccion-del-producto.md)).
>
> **Realineado el 2026-09-13 (noche).** Esta vista tenía 17 historias/tareas; le faltaban
> 8 que [`README.md`](README.md) ya traía, más los Hilos F y G de la recalibración de esta
> sesión (EP-07 y extensión de EP-09). Se agregan abajo con la misma hora real de la ficha que
> usa [`s1.md`](s1.md) — ver esa tabla para el detalle de por qué varias no traen un número
> único.
>
> **Tipo.** En el backlog de Taiga, **H05–H07** se cargan como **HU** (rol real,
> puntos de valor) y **H01–H04, H08, H09** como **tareas** bajo EP-01: fallan la **V**
> de INVEST (el `COMO` es «el equipo» o «la plataforma», no un rol que percibe el
> resultado).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra la historia canónica (candidata:
> **LLM-S01-H06**). La columna *h* es la referencia de planificación del plan y **no se
> convierte** a puntos ([29 · §5–6](../../../08-guia-de-historias-de-usuario.md)).

---

## Objetivo de S1

Un **docente autorizado** carga y consulta casos de referencia (*golden set*) y **los
datos sobreviven al reinicio** del servicio.

> **Expansión a 5 Hilos en Paralelo (P1 a P5):**  
> Según lo normado en [38 · Parte 3](../../../05-plan-de-cinco-sprints.md) y [`capacidad-sprints.md`](../capacidad-sprints.md),
> el equipo dispone de **581,60 h** de capacidad comprometible entre sus 10 desarrolladores (~116,3 h por pareja).
> Para evitar la ociosidad de las parejas P2, P3 y P4, el Sprint 1 se expande desde el arranque en **cinco hilos paralelos**:
> 1. **Hilo A (P1):** Plataforma, contratos e infraestructura.
> 2. **Hilo B (P2):** AI Gateway, puerto de modelos y conexión al proveedor real (`LLM-S03-H11`).
> 3. **Hilo C (P3):** Tutor pedagógico y guardarraíles anti-fuga (`LLM-S05-H01`, `LLM-S05-H02`).
> 4. **Hilo D (P4):** Calibración a nivel plataforma y gobernanza (`LLM-S04-H01`, `H02`, `H03`).
> 5. **Hilo E (P5):** Golden set docente (`H05`–`H07`) e inicio de evaluación académica (`LLM-S06-H01`).
>
> Esta distribución sumaba **~327–368 h** en la versión original de este documento — muy por
> debajo del techo del equipo (581,6 h). **Tras el realineado del 2026-09-13 (noche)**, que
> agrega 8 historias que ya estaban en `README.md` más los Hilos F y G nuevos (EP-07 y extensión
> de EP-09), el total pasa a **~473–498 h + varios ítems todavía sin estimar** — ver la tabla de
> abajo y su nota. Ya no hay margen garantizado por pareja: P3 y P5 quedan cerca o por encima de
> su capacidad individual de 116,3 h incluso antes de sumar lo pendiente de estimar.

---

## Índice Consolidado por Hilos (Sprint 1 Expandido)

| ID                                                               | Título                                                                 | Tipo                | Épica | Pareja | Dep.                     |                                                                   h | Ficha · Tareas                                                                                        |     |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------------- | ----- | ------ | ------------------------ | ------------------------------------------------------------------: | ----------------------------------------------------------------------------------------------------- | --- |
| **Hilo A — Plataforma**                                          |                                                                        |                     |       |        |                          |                                                             **~66** |                                                                                                       |     |
| LLM-EP01-H01 *(S01-H01)*                                         | ADR de arquitectura y convenciones técnicas                            | Tarea (habilitador) | EP-01 | P1     | —                        |                                                                  16 | [ficha](../../historias/ep-01/h01.md) · [tareas](../../tareas/ep-01/h01.md)                           |     |
| LLM-EP01-H02 *(S01-H02)*                                         | Entorno reproducible con un comando                                    | Tarea (habilitador) | EP-01 | P1     | H01                      |                                                                  30 | [ficha](../../historias/ep-01/h02.md) · [tareas](../../tareas/ep-01/h02.md)                           |     |
| LLM-EP01-H03 *(S01-H03)*                                         | Esqueleto transversal del servicio                                     | Tarea (habilitador) | EP-01 | P1     | H02                      |                                                                  34 | [ficha](../../historias/ep-01/h03.md) · [tareas](../../tareas/ep-01/h03.md)                           |     |
| LLM-EP01-H04 *(S01-H04)*                                         | Esquema inicial versionado con auditoría                               | Tarea (habilitador) | EP-01 | P1     | H03                      |                                                                  38 | [ficha](../../historias/ep-01/h04.md) · [tareas](../../tareas/ep-01/h04.md)                           |     |
| LLM-EP01-H05 *(S01-H08)*                                         | Contrato OpenAPI y mock del golden set publicados                      | Tarea (habilitador) | EP-01 | P1     | H03                      |                                                                  10 | [ficha](../../historias/ep-01/h05.md) · [tareas](../../tareas/ep-01/h05.md)                           |     |
| LLM-EP01-H06 *(S01-H09)*                                         | Suite de pruebas y guía de demo de S1                                  | Tarea (habilitador) | EP-01 | todos  | H04, EP-03               |                                                                  18 | [ficha](../../historias/ep-01/h06.md) · [tareas](../../tareas/ep-01/h06.md)                           |     |
| **Hilo B — AI Gateway**                                          |                                                                        |                     |       |        |                          |                                                              **52** |                                                                                                       |     |
| LLM-EP02-H01 *(S01-H10)*                                         | Puerto del proveedor de modelos y fake                                 | Tarea (habilitador) | EP-02 | P2     | EP-01·H01                |                                                                  32 | [ficha](../../historias/ep-02/h01.md) · [tareas](../../tareas/ep-02/h01.md)                           |     |
| LLM-EP02-H02 *(S03-H11)*                                         | Proveedor real (Groq/LangChain4j) y fallback                           | Tarea (habilitador) | EP-02 | P2     | H01                      |                                                                  20 | [ficha](../../historias/ep-02/h02.md)                                                                 |     |
| **Hilo C — Tutor y RAG**                                         |                                                                        |                     |       |        |                          |                                                         **~94–105** |                                                                                                       |     |
| LLM-EP05-H01 *(S05-H01)*                                         | Interacción socrática y tutor pedagógico                               | **HU**              | EP-05 | P3     | EP-02·H01                |                                                                  28 | [ficha](../../historias/ep-05/h01.md)                                                                 |     |
| LLM-EP05-H02 *(S05-H02)*                                         | Guardarraíles anti-jailbreak y anti-fuga                               | **HU**              | EP-05 | P3     | H01                      |                                                                  26 | [ficha](../../historias/ep-05/h02.md)                                                                 |     |
| LLM-EP05-H03 *(nueva)*                                           | Retomar el hilo sin perder el contexto                                 | **HU**              | EP-05 | P3     | H02                      |                                                               20–25 | [ficha](../../historias/ep-05/h03.md)                                                                 |     |
| LLM-EP09-H01 *(nueva, ex-S14-H01)*                               | Ingesta e indexado de un PDF como fuente de consulta                   | **HU**              | EP-09 | P3     | EP-02·H01                |                                     *(sin asignar individualmente)* | [ficha](../../historias/ep-09/h01.md)                                                                 |     |
| LLM-EP09-H02 *(nueva, ex-S14-H02)*                               | Consulta al tutor con citas de fuente/página y abstención              | **HU**              | EP-09 | P3     | H01                      |                     *(~20–26 entre H01 y H02, referencia conjunta)* | [ficha](../../historias/ep-09/h02.md)                                                                 |     |
| **Hilo D — Calibración**                                         |                                                                        |                     |       |        |                          |                                            **~73–81 + 1 a estimar** |                                                                                                       |     |
| LLM-EP04-H01 *(nueva, ex-S03-H01)*                               | Correr y activar una calibración de curso (PAR-14)                     | **HU**              | EP-04 | P4     | EP-02·H01, EP-03·H04/H05 |                                                                  46 | [ficha](../../historias/ep-04/h01.md)                                                                 |     |
| LLM-EP04-H02 *(S04-H01)*                                         | Calibración a nivel plataforma y activación                            | **HU**              | EP-04 | P4     | EP-02·H01                | *(a estimar en Refinamiento — la ficha pide no inventar el número)* | [ficha](../../historias/ep-04/h02.md)                                                                 |     |
| LLM-EP04-H03 *(S04-H02)*                                         | Vencimiento y ciclo de vida de calibración                             | **HU**              | EP-04 | P4     | H02                      |                                                               12–15 | [ficha](../../historias/ep-04/h03.md)                                                                 |     |
| LLM-EP04-H04 *(S04-H03)*                                         | Panel docente/admin de calibración                                     | **HU**              | EP-04 | P4     | H02                      |                                                               15–20 | [ficha](../../historias/ep-04/h04.md)                                                                 |     |
| **Hilo E — Golden Set y Eval**                                   |                                                                        |                     |       |        |                          |                                               **101 + 3 a estimar** |                                                                                                       |     |
| LLM-EP03-H04 *(nueva, ex-S02-H01)*                               | Rúbrica versionada por curso                                           | **HU**              | EP-03 | P5     | EP-01·H04                |                                    *(a estimar — código ya existe)* | [ficha](../../historias/ep-03/h04.md)                                                                 |     |
| LLM-EP03-H05 *(nueva, ex-S02-H02)*                               | Golden set versionado por curso                                        | **HU**              | EP-03 | P5     | EP-01·H04, H04           |                                    *(a estimar — código ya existe)* | [ficha](../../historias/ep-03/h05.md)                                                                 |     |
| LLM-EP03-H01 *(S01-H05)*                                         | Alta de golden set y carga de entradas                                 | **HU**              | EP-03 | P5     | EP-01·H04                |                                                                  24 | [ficha](../../historias/ep-03/h01.md) · [tareas](../../tareas/ep-03/h01.md)                           |     |
| LLM-EP03-H02 *(S01-H06)*                                         | Consulta del golden set persistente tras reinicio                      | **HU** *(canónica)* | EP-03 | P5     | EP-01·H04                |                                                                  14 | [ficha](../../historias/ep-03/h02.md) · [tareas](../../tareas/ep-03/h02.md)                           |     |
| LLM-EP03-H03 *(S01-H07)*                                         | Pantalla docente mínima del golden set                                 | **HU**              | EP-03 | P5     | H01, H02                 |                                                                  24 | [ficha](../../historias/ep-03/h03.md) · [tareas](../../tareas/ep-03/h03.md)                           |     |
| LLM-EP06-H01 *(S06-H01)*                                         | Consumo asíncrono de eventos de entrega                                | Tarea (habilitador) | EP-06 | P5     | EP-01·H01/H04            |                                                                  24 | [ficha](../../historias/ep-06/h01.md)                                                                 |     |
| LLM-EP06-H02 *(nueva, ex-S06-H02)*                               | Recibir un puntaje explicado en las cinco dimensiones                  | **HU**              | EP-06 | P5     | H01, EP-03·H04           |                *(a estimar — sin paquete propio en ninguna receta)* | [ficha](../../historias/ep-06/h02.md)                                                                 |     |
| LLM-EP06-H03 *(nueva, ex-S06-H03)*                               | Que mi entrega se acepte igual si el evaluador está caído              | **HU**              | EP-06 | P5     | H02                      |                                                                  15 | [ficha](../../historias/ep-06/h03.md)                                                                 |     |
| **Hilo F — EP-07, primera mitad** *(nuevo)*                      |                                                                        |                     |       |        |                          |                                                              **86** |                                                                                                       |     |
| LLM-S09-H01                                                      | Consultar el panel de costos, cuotas y fallas del servicio             | **HU**              | EP-07 | P1+P2  | EP-01·H03                |                                                                  36 | [ficha](../../historias/ep-07/h01.md)                                                                 |     |
| LLM-S09-H02                                                      | Configurar un límite de cuota versionado y auditado                    | **HU**              | EP-07 | P2     | H01                      |                                                                  28 | [ficha](../../historias/ep-07/h02.md)                                                                 |     |
| LLM-S09-H03                                                      | Que el sistema aplique 429 y Retry-After en todas las rutas con límite | Tarea (habilitador) | EP-07 | P2     | H02                      |                                                                  22 | [ficha](../../historias/ep-07/h03.md)                                                                 |     |
| **Hilo G — extensión EP-09, primera mitad** *(nuevo, sin ficha)* |                                                                        |                     |       |        |                          |                       **~208 (referencia gruesa, sin comprometer)** |                                                                                                       |     |
| *(a redactar en Planning)*                                       | Ingesta visual con control de calidad                                  | —                   | EP-09 | P3+P5  | EP-09·H01/H02            |   *(histórica de `35`·S15; probablemente sobreestimada — ver nota)* | —                                                                                                     |     |
| **Hilo H — EP-08, Moderación integrada (F2)** *(incorporada)*    |                                                                        |                     |       |        |                          |                                                             **160** |                                                                                                       |     |
| LLM-S11-H01                                                      | Que un mensaje del chat se permita o bloquee antes de entregarse       | Tarea (contrato)    | EP-08 | P3+P2+P1 | —                      |                                                                  34 | [ficha](../../historias/ep-08/h01.md) · [tareas](../../tareas/ep-08/h01.md)                           |     |
| LLM-S11-H02                                                      | Detectar spam, contenido ofensivo e intentos de ocultar código         | Tarea (motor)       | EP-08 | P3     | H01                      |                                                                  28 | [ficha](../../historias/ep-08/h02.md) · [tareas](../../tareas/ep-08/h02.md)                           |     |
| LLM-S12-H01                                                      | Apelar un mensaje que bloqueó la moderación                            | **HU**              | EP-08 | P3     | H01                      |                                                                  26 | [ficha](../../historias/ep-08/h03.md) · [tareas](../../tareas/ep-08/h03.md)                           |     |
| LLM-S12-H02                                                      | Que el docente revise un incidente de moderación y lo resuelva         | **HU**              | EP-08 | P3     | H03                      |                                                                  30 | [ficha](../../historias/ep-08/h04.md) · [tareas](../../tareas/ep-08/h04.md)                           |     |
| LLM-S13-H01                                                      | Que la moderación funcione degradada si el clasificador falla          | Tarea (resiliencia) | EP-08 | P3+P2  | H01, H02                 |                                                                  24 | [ficha](../../historias/ep-08/h05.md) · [tareas](../../tareas/ep-08/h05.md)                           |     |
| LLM-S13-H02                                                      | Que la evidencia de moderación se retenga y luego se purgue            | Tarea (retención)   | EP-08 | P1     | H01                      |                                                                  18 | [ficha](../../historias/ep-08/h06.md) · [tareas](../../tareas/ep-08/h06.md)                           |     |
|                                                                  |                                                                        |                     |       |        |                          |                                                  **Total Estimado** | **~647–672 h + 9 ítems a estimar/sin ficha** *(incluye 160 h de EP-08 incorporada; ver historias-agregadas-s1.md)* |     |

> **Nota sobre la carga de P1 (corregida).** La fila de Hilo A traía dos números sin conciliar:
> 146 h era la suma de los 6 ítems tal como quedaron listados abajo (estimación **original**,
> de antes de auditar el código), y una nota aparte decía "~55 h" como cifra "real" sin que la
> tabla se actualizara. El [`backlog-priorizado-cierre-s1.md` · Veredicto de
> capacidad](../../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md#veredicto-de-capacidad-actualizado-2026-09-12-catálogo-bajó-de-12h-a-8h-al-descartar-su-t2)
> ya tiene el número correcto y auditado: **52 h de base** (Eureka, 401, 404, CI, ADR, `.env` —
> el detalle está en [`s1-cierre.md`](s1-cierre.md)) **+ 14 h de ampliación ya aceptada** el
> 2026-09-12 = **66 h reales**. Se usa **66 h** de acá en más para Hilo A; los 16/30/34/38/10/18
> de las filas de abajo quedan como referencia histórica de cada ficha, no como lo que falta
> construir hoy.
>
> **Nota sobre el Hilo G y las horas "a estimar".** El detalle completo de por qué varias
> historias no traen un número único, y por qué el Hilo G no se compromete con su referencia
> gruesa de ~208 h, está en [`s1.md`](s1.md) § 4 y en [`README.md`](README.md) § Hilo G.

**Demo de S1:** un docente autorizado crea un golden set, carga una entrada y la consulta
después de reiniciar el servicio (`docker compose restart`), obteniendo exactamente lo que
cargó. La demo corre en un **ambiente integrado**, no en una máquina local aislada
([sprint-0.md · §9](../sprint-0/sprint-0.md)). **Los hilos B, C, D, E, F y G corren en paralelo**
sin bloquear ni alterar el criterio central de la demo docente de S1.

---

## En resumen

Al final de S1 no se corrige nada con IA todavía, pero sí:

- un ADR con las convenciones y fronteras del servicio (H01),
- un entorno que se levanta con un comando (H02),
- el esqueleto transversal con su seguridad de borde (H03),
- un esquema académico versionado, *append-only* y auditado (H04),
- un docente que crea su golden set, le carga entradas y las vuelve a consultar tras
  reiniciar (H05, H06),
- una pantalla docente mínima para hacerlo (H07),
- un contrato y un mock para que `admin-service` avance en paralelo (H08),
- una suite de pruebas + guía de demo que lo respaldan con evidencia (H09),
- y, en paralelo y sin bloquear nada de lo anterior, el puerto de invocación de modelos
  con su adaptador fake, listo para que S3 no arranque de cero (H10).
