# Sprint 1 — vista de historias (índice, tipo y demo)

> **Qué es este documento.** La vista **de sprint** de las nueve historias de S1: el
> objetivo, el índice, el tipo de cada una (HU de valor / habilitador) y la demo. Las
> **fichas completas** (Como/Quiero/Para, Notas, CA con negativos, BDD, Prototipo,
> Estimación, Dependencias) viven agrupadas por épica en
> [`../historias/ep-01/`](../historias/ep-01/README.md) y
> [`../historias/ep-03/`](../historias/ep-03/README.md). El **desglose en tareas SMART**, en
> [`../tareas/`](../tareas/README.md). La versión de las nueve historias contada **sin
> jerga técnica** está en [`s1-explicado.md`](s1-explicado.md).
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato de acá no coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | [`35` · «S1»](../35-backlog-ejecutable.md) |
> | Tipo (HU de valor / habilitador) y demo | [30 · §5](../30-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../23-plan-construccion-producto-llm.md) |
> | Fichas completas de HU | [`../historias/`](../historias/README.md) |
> | Tareas | [`../tareas/`](../tareas/README.md) |
> | Registro del sprint (capacidad, compromiso, cierre) | *(copia de [`../plantillas/sprint-llm.md`](../plantillas/sprint-llm.md) en la Planning de S1)* |
> | Secuencia técnica de cada tarea | [playbook de construcción · §4](../36-playbook-de-construccion.md) |
>
> **Título en Taiga.** Cada ficha se carga con el título `GXX — TÍTULO` (`GXX` = número
> de grupo, aún sin asignar). El ID interno `LLM-S01-Hyy` es el del equipo
> ([23 · §9.2](../23-plan-construccion-producto-llm.md)).
>
> **Tipo.** En el backlog de Taiga, **H05–H07** se cargan como **HU** (rol real,
> puntos de valor) y **H01–H04, H08, H09** como **tareas** bajo EP-01: fallan la **V**
> de INVEST (el `COMO` es «el equipo» o «la plataforma», no un rol que percibe el
> resultado).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra la historia canónica (candidata:
> **LLM-S01-H06**). La columna *h* es la referencia de planificación del plan y **no se
> convierte** a puntos ([29 · §5–6](../29-guia-catedra-historias-de-usuario.md)).

---

## Objetivo de S1

Un **docente autorizado** carga y consulta casos de referencia (*golden set*) y **los
datos sobreviven al reinicio** del servicio. Trabajo estimado **~240 h** de paquetes
(piso, no tope) sobre una capacidad de referencia de ≈ 571 h
([sprint-0.md · §4](sprint-0.md)). Incluye **H10** (EP-02), adelantada desde S3 en la
reprogramación a 8 semanas ([`README.md`](README.md)); no cambia el objetivo ni la demo.

S1 es un sprint de arranque: **casi todo es habilitador técnico**. Solo **H05–H07** son
**HU de valor** (un docente percibe el resultado).

---

## Índice

| ID | Título | Tipo | Épica | Pareja | Dep. | h | Ficha · Tareas |
| --- | --- | --- | --- | --- | --- | --: | --- |
| LLM-S01-H01 | ADR de arquitectura y convenciones técnicas | Tarea (habilitador) | EP-01 | P1 | — | 16 | [ficha](../historias/ep-01/h01.md) · [tareas](../tareas/ep-01/h01.md) |
| LLM-S01-H02 | Entorno reproducible con un comando | Tarea (habilitador) | EP-01 | P1 | H01 | 30 | [ficha](../historias/ep-01/h02.md) · [tareas](../tareas/ep-01/h02.md) |
| LLM-S01-H03 | Esqueleto transversal del servicio | Tarea (habilitador) | EP-01 | P1 | H02 | 34 | [ficha](../historias/ep-01/h03.md) · [tareas](../tareas/ep-01/h03.md) |
| LLM-S01-H04 | Esquema inicial versionado con auditoría | Tarea (habilitador) | EP-01 | P1 | H03 | 38 | [ficha](../historias/ep-01/h04.md) · [tareas](../tareas/ep-01/h04.md) |
| LLM-S01-H05 | Alta de golden set y carga de entradas | **HU** | EP-03 | P5 | H04 | 24 | [ficha](../historias/ep-03/h05.md) · [tareas](../tareas/ep-03/h05.md) |
| LLM-S01-H06 | Consulta del golden set que sobrevive al reinicio | **HU** *(canónica)* | EP-03 | P5 | H04 | 14 | [ficha](../historias/ep-03/h06.md) · [tareas](../tareas/ep-03/h06.md) |
| LLM-S01-H07 | Pantalla docente mínima del golden set | **HU** | EP-03 | P5 | H05, H06 | 24 | [ficha](../historias/ep-03/h07.md) · [tareas](../tareas/ep-03/h07.md) |
| LLM-S01-H08 | Contrato OpenAPI y mock del golden set publicados | Tarea (habilitador) | EP-01 | P1 | H03 | 10 | [ficha](../historias/ep-01/h08.md) · [tareas](../tareas/ep-01/h08.md) |
| LLM-S01-H09 | Suite de pruebas y guía de demo de S1 | Tarea (habilitador) | EP-01 | todos | H04–H07 | 18 | [ficha](../historias/ep-01/h09.md) · [tareas](../tareas/ep-01/h09.md) |
| LLM-S01-H10 | Puerto del proveedor de modelos (AI Gateway) y fake para pruebas | Tarea (habilitador) | EP-02 | P2 | H01 | 32 | [ficha](../historias/ep-02/h10.md) · [tareas](../tareas/ep-02/h10.md) |
| | | | | | **Total** | **240** | |

> **H10 es una incorporación de la reprogramación a 8 semanas.** Se adelanta desde S3
> porque no depende del golden set publicado, sólo del ADR (H01), y corre en paralelo con
> el resto sin cambiar el criterio de demo. Detalle de por qué y cómo se reparte el resto
> del plan comprimido en [`README.md`](README.md).

**Demo de S1:** un docente autorizado crea un golden set, carga una entrada y la consulta
después de reiniciar el servicio (`docker compose restart`), obteniendo exactamente lo que
cargó. La demo corre en un **ambiente integrado**, no en una máquina local aislada
([sprint-0.md · §9](sprint-0.md)). **H10 no forma parte de este criterio de demo** — es
preparación de EP-02 que corre en paralelo.

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
