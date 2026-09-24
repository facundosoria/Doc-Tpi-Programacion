# Sprint 2 — Cerrar EP-06, EP-07 y la extensión de EP-09; volver todo real

> **Estado:** Por planificar — el registro se crea en la Planning de S2.
> **Referencia:** [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 · Sprint 2
> **Recalibrado el 2026-09-13 (noche):** además de cerrar EP-06, cierra lo que Sprint 1 arrancó de EP-07 (Hilo F) y de la extensión de EP-09 (Hilo G) — P1, P2, P3 y P5 tienen capacidad disponible (~70-90 h de ~581,6 h) para absorberlo sin forzar nada.

## Objetivo

Cerrar EP-06 completa (apelación, override docente, bloqueo de cierre de curso), cerrar EP-07 completa (lo que S1 no llegó a terminar: recuperación de trabajos, salud sin proveedor, prueba de carga y backup/restore) y la segunda mitad de la extensión de EP-09 (versionado y retiro documental), y conectar todo lo construido en Sprint 1 contra el proveedor real de IA.

## HU comprometidas

### EP-07 — segunda mitad: recuperación, salud y hardening (P1+P2) · 70 h (real: 30+14+26)

> Cierra lo que el Hilo F de Sprint 1 dejó pendiente. Épica y fichas: [`historias/ep-07/`](../../historias/ep-07/README.md). Paquete histórico de referencia: [`35` · S10](../../../04-backlog-ejecutable.md) (~208 h) — la ficha real de cada historia suma 70 h.

| ID | Título | Tipo | Ficha |
|---|---|---|---|
| LLM-S10-H01 | Ver, reintentar y recuperar trabajos detenidos sin duplicar resultados | HU de valor | [h04.md](../../historias/ep-07/h04.md) |
| LLM-S10-H02 | Que el estado de salud del servicio sea útil aunque el proveedor esté caído | Tarea (habilitador) | [h05.md](../../historias/ep-07/h05.md) |
| LLM-S10-H03 | Prueba de carga y backup/restore probados con evidencia real | Tarea (habilitador) | [h06.md](../../historias/ep-07/h06.md) |

> **Riesgo declarado, no de horas:** `LLM-S10-H03` necesita ventana de pruebas de carga con proveedor real y presupuesto acordado ([`epicas/ep-07.md`](../../epicas/ep-07.md) · Suposiciones). Si no hay ventana ni presupuesto para S2, se recorta en la Planning.

### EP-09 — extensión, segunda mitad: versionado y retiro documental (P3+P5) · ~208 h (referencia)

> Cierra lo que el Hilo G de Sprint 1 dejó pendiente. **Sin ficha de historia individual todavía** — se redacta en la Planning de S2. Paquete histórico de referencia: [`35` · S16](../../../04-backlog-ejecutable.md).

| Tarea | Descripción |
|---|---|
| Versionado documental | Activar una versión nueva de un material no mezcla resultados con la anterior |
| Retiro documental | Retirar un material deja de usarse en búsquedas pero conserva el historial de auditoría |

### Cola de EP-06 — Apelación y auditoría (P5) · ~55–70 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-S06-H02 | Recibir un puntaje explicado en las cinco dimensiones | HU de valor | EP-06 | [h02.md](../../historias/ep-06/h02.md) |
| LLM-S06-H03 | Que mi entrega se acepte igual si el evaluador está caído | HU de valor | EP-06 | [h03.md](../../historias/ep-06/h03.md) |
| LLM-S07-H01 | Impugnar la nota que me dio la IA en una entrega | HU de valor | EP-06 | [h04.md](../../historias/ep-06/h04.md) |
| LLM-S07-H02 | Que el docente pueda corregir la nota de la IA sin borrar lo que ya había | HU de valor | EP-06 | [h05.md](../../historias/ep-06/h05.md) |
| LLM-S07-H03 | Consultar si un curso puede cerrarse o tiene evaluaciones pendientes | Tarea habilitadora (M2M) | EP-06 | [h06.md](../../historias/ep-06/h06.md) |

### Enchufar el proveedor real en todo lo construido (P2) · ~15–20 h

| ID | Título | Tipo | Épica | Ficha |
|---|---|---|---|---|
| LLM-EP02-H02 | Proveedor real conectado en tutor, calibración y RAG *(ex-S03-H11)* | Tarea habilitadora | EP-02 | [h02.md](../../historias/ep-02/h02.md) |

### Bloqueo real de punta a punta con `courses-service` (P1) · variable

> Depende de que `courses-service` haya respondido durante Sprint 1. Si no respondió, se declara como riesgo y no bloquea el sprint.

| ID | Título | Tipo | Épica |
|---|---|---|---|
| LLM-S01-H03 | Bloqueo de activación de curso sin calibración (integración real con `courses-service`) | Tarea habilitadora | EP-01 |

## Resumen

| Bloque | HU / Tareas | h estimadas |
|---|---|---|
| EP-06 completa (P5) | 5 | ~55–70 |
| EP-07 segunda mitad — completa la épica (P1+P2) | 3 | 70 (real) |
| EP-09 extensión, segunda mitad — completa la extensión (P3+P5) | 2 | ~208 (referencia histórica, sin ficha) |
| Proveedor real integrado (P2) | 1 | ~15–20 |
| Bloqueo courses-service (P1) | 1 | variable |
| **Total** | **12** | **~348–368 h + variable** |

> **Techo de referencia: 581,6 h (~60–63% con estos números).** S2 queda con margen cómodo —
> incluso con la extensión de EP-09 todavía a su referencia gruesa de ~208 h (probablemente
> sobreestimada: EP-07 real salió en 34% de su referencia histórica equivalente). Realineado
> 2026-09-13 (noche): EP-07 pasó de ~208 h de referencia a 70 h reales (fichas
> [`historias/ep-07/`](../../historias/ep-07/README.md)).
>
> Con Sprint 2 cerrado, **EP-01 a EP-07, EP-09 (base + extensión completa)** quedan cubiertas — no solo las 6 épicas identificadas originalmente, también EP-07 completa.

## Demo de S2

Un alumno cierra un intento, recibe su puntaje desglosado con el proveedor real de IA, puede apelar la corrección, y un docente puede revisarla y corregirla. El historial original nunca se borra. **Más:** el panel de operación recupera un trabajo detenido sin duplicar resultados, y un material con una versión nueva no mezcla resultados con la anterior.
