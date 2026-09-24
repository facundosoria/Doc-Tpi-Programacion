# Sprint 5 — EP-10 completa, streaming del tutor, demo grabada y defensa

> **Estado:** Por planificar — el registro se crea en la Planning de S5.
> **Referencia:** [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 · Sprint 5
> **Recalibrado el 2026-09-13 (noche):** deja de ser "sin obligación de llenarlo". Se carga EP-10
> (personalización y agente) completa más streaming/SSE del tutor encima de la demo y defensa
> originales.
> **Corregido el mismo día (2ª vuelta):** la primera versión de esta nota usaba ~416 h — la
> referencia histórica de `35` (2 paquetes × ~208 h). Las 4 fichas reales de EP-10
> ([`historias/ep-10/`](../../historias/ep-10/README.md)) suman **116 h**, no 416. El total del
> sprint queda muy por debajo del techo (~35–38%) — con más margen real del que se pensaba para
> absorber lo que EP-10 herede recortado si Sprint 4 no cerró EP-08 a tiempo, más el ensayo real
> de la demo.

## Objetivo

Demo grabada, deck de defensa ante la cátedra, margen final de calendario, **y cerrar EP-10: desafío personalizado desde el material del curso y agente por mención en el chat, con moderación y citas.**

## HU / Tareas comprometidas

### EP-10 — Personalización y agente completa (P3+P4+P5) · 116 h (real, ficha por ficha)

> Épica y fichas ya escritas — [`historias/ep-10/`](../../historias/ep-10/README.md). Receta histórica de referencia: [`35` · S17–S18](../../../04-backlog-ejecutable.md) (~416 h) — la ficha real de cada historia suma 116 h.
> **Prerequisito real, no de horas:** EP-09 con datos reales (✅ ya cubierto), EP-08 con moderación disponible (Sprint 4), y contrato acordado con `challenges-service` y `chat-service` ([`epicas/ep-10.md`](../../epicas/ep-10.md) · Suposiciones). Si Sprint 4 no cerró EP-08, EP-10 se recorta en la Planning de S5 — no se adelanta igual.

| ID | Título | Tipo | Pareja | h (real) | Ficha HU | Tareas SMART |
|---|---|---|---|---:|---|---|
| LLM-S17-H01 | Solicitar un desafío personalizado generado a partir del material de mi curso | HU de valor | P5+P4 | 38 | [h01.md](../../historias/ep-10/h01.md) | [7 tareas (38 h)](../../tareas/ep-10/h01.md) |
| LLM-S17-H02 | Que la generación del desafío sea durable y la entrega al motor no se duplique | Tarea (habilitador) | P4 | 32 | [h02.md](../../historias/ep-10/h02.md) | [6 tareas (32 h)](../../tareas/ep-10/h02.md) |
| LLM-S18-H01 | Mencionar a @agente en el chat y recibir una respuesta citada y moderada | HU de valor | P3+P5 | 30 | [h03.md](../../historias/ep-10/h03.md) | [6 tareas (30 h)](../../tareas/ep-10/h03.md) |
| LLM-S18-H02 | Que el agente solo responda a menciones válidas de personas reales y nunca a otros bots | Tarea (habilitador) | P3 | 16 | [h04.md](../../historias/ep-10/h04.md) | [4 tareas (16 h)](../../tareas/ep-10/h04.md) |


### Streaming/SSE del tutor (P2) · ~50 h

> Buffer Interceptor — declarado fuera del contrato ejecutable hasta que se fusione ([`38`](../../../05-plan-de-cinco-sprints.md) · Parte 4). **Sin ficha todavía** — se redacta en la Planning de S5 antes de comprometer horas exactas; ~50 h es referencia gruesa, no estimación fina.

### Preparación de la entrega final (todas las parejas)

| Tarea | Descripción |
|---|---|
| Demo grabada | Recorrido completo E2E grabado: golden set → calibración → tutor → cierre → evaluación → apelación → resolución docente |
| Deck de defensa | Presentación para la cátedra: arquitectura, decisiones, evidencia de los 6 ítems obligatorios |
| Checklist de entrega final | Todos los ítems de la DoD del proyecto verificados y con evidencia |
| Runbooks | Cómo operar cada pieza, qué hacer si algo falla en la demo |
| Wiki de Taiga al día | Épicas, HU y tareas cargadas con sus enlaces permanentes |
| Backlog posterior | Lista de lo que no entró y podría continuarse en otro cuatrimestre |

### Margen final

| Tarea | Descripción |
|---|---|
| Corrección de última hora | Lo que aparezca en el ensayo de la demo |
| Documentación técnica | OpenAPI, AsyncAPI y ADRs actualizados y publicados |

## Los 6 ítems obligatorios — verificación final

| # | Ítem | HU que lo cubre |
|---|---|---|
| 1 | Rúbrica versionada | EP-03 · H01 |
| 2 | Golden set versionado | EP-03 · H02, H05, H06 |
| 3 | Invocación real del modelo | EP-02 · H11 |
| 4 | Calibración | EP-04 · H01 |
| 5 | Bloqueo de activación sin calibración | EP-01 · H03 + `courses-service` |
| 6 | Salvaguarda anti-fuga | EP-05 · H01 |

## Resumen

| Bloque | h estimadas |
|---|---|
| EP-10 completa | 116 (real) |
| Streaming/SSE del tutor | ~50 |
| Demo grabada + deck | ~15–20 |
| Documentación final | ~10–15 |
| Margen de corrección | ~10–20 |
| **Total** | **~201–221 h** |

> **Techo de referencia del sprint: 581,6 h (~35–38% con el número real).** La primera versión de esta nota decía ~501–521 h con EP-10 a su referencia histórica de ~416 h; con las fichas reales (116 h) el sprint queda con bastante más margen del pensado — igual sirve para absorber lo que EP-10 herede recortado de S4 y el ensayo de la demo final. Ver [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 · Sprint 5.

## Demo de S5

La demo grabada del proyecto completo, lista para la defensa ante la cátedra. **Más:** un alumno pide un desafío personalizado generado desde el material de su curso, y menciona a `@agente` en el chat para recibir una respuesta citada y moderada (EP-10).
