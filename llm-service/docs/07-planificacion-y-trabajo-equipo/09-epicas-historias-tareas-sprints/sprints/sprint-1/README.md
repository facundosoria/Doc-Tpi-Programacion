# Sprint 1 — Arranque en paralelo: EP-01, EP-02, EP-03, EP-04, EP-05, EP-09 completas + arranque de EP-07 y extensión de EP-09

> **Estado:** Listo para Planning.
> **Capacidad de referencia:** 581,6 h (10 devs — la cifra dev-only de [`capacidad-sprints.md`](../capacidad-sprints.md) §3; el total de 12 personas incluyendo soporte es 656,8 h). **Trabajo comprometido tras el realineado del 2026-09-13 (noche):** ~720 h + varios ítems de los Hilos D/E todavía sin estimar (~124% del techo). Sprint cargado intencionalmente por decisión explícita — ver [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 — para adelantar EP-07 y la extensión de EP-09 mientras P1/P2/P3/P5 todavía tienen hueco, en vez de dejarlo para más adelante.
>
> **Corrección del mismo realineado:** el Hilo F usaba ~208 h de referencia histórica (paquete
> `35`·S9 completo); las fichas reales de `LLM-S09-H01/H02/H03` ([`historias/ep-07/`](../../historias/ep-07/README.md))
> suman **86 h**, no 208 — se corrige acá y en [`s1.md`](s1.md)/[`s1-historias.md`](s1-historias.md).
> El Hilo G sigue en ~208 h porque no tiene ficha propia todavía; dado que EP-07, EP-08 y EP-10
> reales salieron en 26–41% de su referencia histórica equivalente, esa cifra probablemente
> también está sobreestimada — no se ajusta sin ficha propia, para no inventar un segundo número.
>
> **Segunda corrección, la misma sesión:** el Hilo A traía dos cifras sin conciliar — 146 h (suma
> de las 6 fichas originales, previa a auditar el código) y "~55 h" (una cifra suelta sin fuente
> clara en la tabla). El número real y auditado, con la ampliación ya aceptada el 2026-09-12, es
> **66 h** (52 h base + 14 h de ampliación — ver
> [`backlog-priorizado-cierre-s1.md` · Veredicto de capacidad](../../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md#veredicto-de-capacidad-actualizado-2026-09-12-catálogo-bajó-de-12h-a-8h-al-descartar-su-t2)
> y [`s1-cierre.md`](s1-cierre.md)). Se corrige acá y en [`s1.md`](s1.md)/[`s1-historias.md`](s1-historias.md).

- **Registro de planning:** [`s1.md`](s1.md)
- **Cierre de huecos auditados:** [`s1-cierre.md`](s1-cierre.md)
- **Vista de historias completa:** [`s1-historias.md`](s1-historias.md)
- **Historias agregadas y guía de carga en Taiga:** [`historias-agregadas-s1.md`](historias-agregadas-s1.md)
- **Historias explicadas sin jerga:** [`s1-explicado.md`](s1-explicado.md)
- **Backlog priorizado (orden de trabajo):** [`backlog-priorizado-s1.md`](backlog-priorizado-s1.md)

## Objetivo

**Seis épicas enteras** en un sprint: plataforma base, proveedor de IA real, golden set docente, calibración completa, tutor con guardarraíles y RAG con citas. Al final de S1 el servicio ya puede hacer todo excepto evaluar, moderar y personalizar desafíos. **Más:** arrancar la mitad de EP-07 (operación/cuotas) y la mitad de la extensión de EP-09 (ingesta visual) que antes esperaban a S3, porque P1, P2, P3 y P5 tienen hueco real en este sprint.

## HU comprometidas por hilo (581,6 h de referencia)

### Hilo A — Plataforma (P1) · ~66 h (real, auditado — ver nota de corrección arriba)

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP01-H01 | ADR de arquitectura y convenciones técnicas | Tarea | EP-01 | [h01.md](../../historias/ep-01/h01.md) |
| LLM-EP01-H02 | Entorno reproducible con un comando | Tarea | EP-01 | [h02.md](../../historias/ep-01/h02.md) |
| LLM-EP01-H03 | Esqueleto transversal del servicio | Tarea | EP-01 | [h03.md](../../historias/ep-01/h03.md) |
| LLM-EP01-H04 | Esquema inicial versionado con auditoría | Tarea | EP-01 | [h04.md](../../historias/ep-01/h04.md) |
| LLM-EP01-H05 | Contrato OpenAPI y mock del golden set publicados *(ex-H08)* | Tarea | EP-01 | [h05.md](../../historias/ep-01/h05.md) |
| LLM-EP01-H06 | Suite de pruebas y guía de demo de S1 *(ex-H09)* | Tarea | EP-01 | [h06.md](../../historias/ep-01/h06.md) |

### Hilo B — AI Gateway y proveedor real (P2) · ~52 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP02-H01 | Puerto del proveedor de modelos y fake *(ex-H10)* | Tarea | EP-02 | [h01.md](../../historias/ep-02/h01.md) |
| LLM-EP02-H02 | Conectar proveedor real (Groq/LangChain4j) *(ex-H11)* | Tarea | EP-02 | [h02.md](../../historias/ep-02/h02.md) |

### Hilo C — Tutor y RAG completos (P3) · ~100 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP05-H01 | Recibir una respuesta socrática del tutor, filtrada por guardarraíles | HU | EP-05 | [h01.md](../../historias/ep-05/h01.md) |
| LLM-EP05-H02 | Retomar una conversación anterior con el tutor | HU | EP-05 | [h02.md](../../historias/ep-05/h02.md) |
| LLM-EP05-H03 | Retomar el hilo sin perder el contexto | HU | EP-05 | [h03.md](../../historias/ep-05/h03.md) |
| LLM-EP09-H01 | Ingesta e indexado de un PDF como fuente de consulta *(ex-S14-H01)* | HU | EP-09 | [h01.md](../../historias/ep-09/h01.md) |
| LLM-EP09-H02 | Consulta al tutor con citas de fuente/página y abstención *(ex-S14-H02)* | HU | EP-09 | [h02.md](../../historias/ep-09/h02.md) |

### Hilo D — Calibración completa (P4) · ~90 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP04-H01 | Correr y activar una calibración de curso con métrica PAR-14 *(ex-S03-H01)* | HU | EP-04 | [h01.md](../../historias/ep-04/h01.md) |
| LLM-EP04-H02 | Correr una calibración de plataforma y ver su reporte *(ex-S04-H01)* | HU | EP-04 | [h02.md](../../historias/ep-04/h02.md) |
| LLM-EP04-H03 | Que una calibración vigente venza cuando cambia la referencia *(ex-S04-H02)* | HU | EP-04 | [h03.md](../../historias/ep-04/h03.md) |
| LLM-EP04-H04 | Enterarme de que mi curso tiene evaluaciones frenadas *(ex-S04-H03)* | HU | EP-04 | [h04.md](../../historias/ep-04/h04.md) |

### Hilo E — Golden set + inicio de evaluación (P5) · ~115 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP03-H04 | Rúbrica versionada por curso *(ex-S02-H01)* | HU | EP-03 | [h04.md](../../historias/ep-03/h04.md) |
| LLM-EP03-H05 | Golden set versionado por curso *(ex-S02-H02)* | HU | EP-03 | [h05.md](../../historias/ep-03/h05.md) |
| LLM-EP03-H01 | Alta de golden set y carga de entradas *(ex-S01-H05)* | HU | EP-03 | [h01.md](../../historias/ep-03/h01.md) |
| LLM-EP03-H02 | Consulta del golden set persistente tras reinicio *(canónica, ex-S01-H06)* | HU | EP-03 | [h02.md](../../historias/ep-03/h02.md) |
| LLM-EP03-H03 | Pantalla docente mínima del golden set *(ex-S01-H07)* | HU | EP-03 | [h03.md](../../historias/ep-03/h03.md) |
| LLM-EP06-H01 | Consumir el cierre de un intento y encolar su evaluación *(ex-S06-H01)* | Tarea | EP-06 | [h01.md](../../historias/ep-06/h01.md) |
| LLM-EP06-H02 | Recibir un puntaje explicado en las cinco dimensiones *(ex-S06-H02)* | HU | EP-06 | [h02.md](../../historias/ep-06/h02.md) |
| LLM-EP06-H03 | Que mi entrega se acepte igual si el evaluador está caído *(ex-S06-H03)* | HU | EP-06 | [h03.md](../../historias/ep-06/h03.md) |

### Hilo F — EP-07, primera mitad: costos, cuotas y salud (P1+P2) · 86 h (real, ficha por ficha: 36+28+22)

> Épica y fichas ya escritas — [`historias/ep-07/`](../../historias/ep-07/README.md). Paquete histórico de referencia: [`35` · S9](../../../04-backlog-ejecutable.md) (~208 h) — la ficha real de cada historia suma 86 h, no 208; se usa la cifra real.

| ID | Título | Tipo | Ficha |
|---|---|---|---|
| LLM-S09-H01 | Consultar el panel de costos, cuotas y fallas del servicio | HU de valor | [h01.md](../../historias/ep-07/h01.md) |
| LLM-S09-H02 | Configurar un límite de cuota versionado y auditado | HU de valor | [h02.md](../../historias/ep-07/h02.md) |
| LLM-S09-H03 | Que el sistema aplique 429 y Retry-After en todas las rutas con límite | Tarea (habilitador) | [h03.md](../../historias/ep-07/h03.md) |

> **Riesgo declarado:** la prueba de carga y el backup/restore reales (`LLM-S10-H03`) quedan para S2 junto con el resto de EP-07 — necesitan más del servicio ya desplegado de lo que S1 puede garantizar el día 1.

### Hilo G — EP-09, extensión primera mitad: ingesta visual (P3+P5) · ~208 h (referencia)

> **Sin ficha de historia individual todavía** — se redacta en la Planning de S1. Paquete histórico de referencia: [`35` · S15](../../../04-backlog-ejecutable.md). Cubre lo que `LLM-EP09-H01`/`H02` (Hilo C) no cubren: OCR, tablas y figuras con control de calidad, más el `EmbeddingPort` real en vez del fake ([`historias/ep-09/README.md`](../../historias/ep-09/README.md) · nota de alcance).

| Tarea | Descripción |
|---|---|
| Ingesta visual con control de calidad | OCR sobre escaneos, extracción de tablas/figuras, validación antes de indexar |
| Embeddings reales | Reemplazar el `EmbeddingPort` fake por un adaptador real (ONNX local u otro) |

## Resumen

| Hilo | HU/Tareas | h estimadas |
|---|---|---|
| A — Plataforma (P1) | 6 | 66 (real, ver nota) |
| B — AI Gateway (P2) | 2 | ~52 |
| C — Tutor y RAG (P3) | 5 | ~74–79 + 2 sin estimar individualmente |
| D — Calibración (P4) | 4 | ~73–81 + 1 a estimar (EP04-H02) |
| E — Golden Set + Eval base (P5) | 8 | ~101 + 3 a estimar (rúbrica, golden set versionado, EP06-H02) |
| F — EP-07 primera mitad (P1+P2) | 3 | 86 (real) |
| G — EP-09 extensión, ingesta visual (P3+P5) | 2 | ~208 (referencia histórica, sin ficha, probablemente sobreestimada) |
| **Total** | **30** | **~674–695 h + ~9 ítems a estimar/sin ficha, de 581,6 de referencia (~116–120%)** *(+14 h por la corrección de Hilo A)* |

> Las épicas **EP-01, EP-02, EP-03, EP-04, EP-05 y EP-09 (base)** quedan **completamente cerradas** al final de S1. EP-06 queda parcialmente abierta (solo la base). **EP-07 y la extensión de EP-09 arrancan en S1 y terminan en S2** — no cierran del todo en este sprint.
>
> **Concentración por pareja, no solo por sprint total:** P2 pasa a sumar Hilo B (~52 h) + su parte de F (86 h repartidos con P1) — es, junto con P5 (Hilo E ~101 h + 3 ítems sin estimar + su parte de G), la pareja más expuesta a superar su propia capacidad individual (~116,3 h/sprint). Se declara acá para que la Planning de S1 lo vea antes de comprometer, no para bloquear la carga.
>
> **Por qué el total sigue siendo un rango, no un número.** Cuatro fichas (Hilo C: EP09-H01/H02
> sin desglose individual; Hilo D: EP04-H02; Hilo E: EP03-H04, EP03-H05, EP06-H02) dicen
> explícitamente *"estimar en Refinamiento, no inventar el número acá"* — se respeta eso en vez
> de completar la tabla con una cifra inventada. El Hilo G tampoco tiene ficha. Detalle historia
> por historia en [`s1.md`](s1.md) § 4.

## Demo de S1

Un docente crea un golden set, lo calibra, carga un PDF de material, y consulta el tutor. El tutor responde con citas del PDF. Un intento de jailbreak se bloquea. Todo con el proveedor real de IA. **Más (si F y G avanzan):** el panel de operación muestra el costo y las cuotas del sprint, y un PDF escaneado (no solo texto) queda indexado con OCR.
