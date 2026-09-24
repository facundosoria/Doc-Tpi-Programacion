# Sprint 3 — Regresión, cobertura y hardening

> **Estado:** Por planificar — el registro se crea en la Planning de S3.
> **Referencia:** [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 · Sprint 3
> **Recalibrado el 2026-09-13 (noche, 2ª vuelta):** EP-07 completa y la extensión de EP-09
> (S15/S16) **se adelantaron a S1+S2** — repartidas en las mitades que ese hueco ya tenía
> disponible ([`sprint-1/README.md`](../sprint-1/README.md) · Hilos F/G, [`sprint-2/README.md`](../sprint-2/README.md))
> en vez de esperar a este sprint. S3 vuelve a ser lo que era antes de la primera recalibración:
> regresión y cobertura de lo ya construido, sin funcionalidad nueva.

## Objetivo

Regresión completa de todo lo construido en S1 y S2, cobertura de integración al 95% que exige `AGENTS.md` en cada pieza nueva, y colchón real para lo que `courses-service` haya tardado en responder.

## HU / Tareas comprometidas

> Sprint 3 no agrega funcionalidad nueva — consolida y verifica lo ya construido, incluyendo EP-07 y la extensión de EP-09 que ahora cierran en S1/S2. El contenido exacto se define en la Planning de S3 según qué quedó pendiente de S2.

### Regresión y cobertura (todas las parejas)

| Tarea | Descripción |
|---|---|
| Regresión EP-01 a EP-06 | Correr todas las suites contra el proveedor real, no solo el fake |
| Regresión EP-07 y extensión de EP-09 | Verificar lo que S1/S2 cerraron de operación/cuotas e ingesta visual/versionado documental |
| Cobertura EP-05 | `ConversationRepository` y `MessageRepository` — integración con Testcontainers/Postgres real (bloqueada por Docker en sesiones anteriores) |
| Cobertura EP-09 | 4 clases con 0% de cobertura de integración real (RAG) |
| Cobertura EP-06 | Tests de integración de evaluación, apelación y override |
| Bloqueo `courses-service` | Si no cerró en S2, es la última oportunidad antes del ensamble |

### Deuda técnica identificada

| Área | Qué falta |
|---|---|
| EP-02 | Probar `LLM-S03-H11` contra Groq real en CI (no solo WireMock) |
| EP-04 | UI docente/admin de calibración si no entró en S1 |
| EP-07 | Si `LLM-S10-H03` (prueba de carga) se recortó en S1/S2 por falta de ventana/presupuesto, es la última oportunidad antes de S4 |
| EP-09 | pgvector + ONNX local si la extensión no terminó de reemplazar el fake en S1/S2 |

## Resumen

| Bloque | h estimadas |
|---|---|
| Regresión + cobertura | ~30–45 |
| Deuda técnica pendiente | ~10–20 |
| Colchón `courses-service` | variable |
| **Total** | **~40–65 h** |

> **Techo de referencia del sprint: 581,6 h.** Este total queda muy por debajo — a diferencia de S1 y S2 (recalibrados para absorber EP-07 y la extensión de EP-09), S3 no tiene backlog nuevo con ficha vigente pendiente de asignar. Si aparece algo que no cerró en S1/S2, se recorta hacia acá antes que inventar trabajo nuevo.

## Demo de S3

Todas las suites de todas las épicas (EP-01 a EP-07 + EP-09 completa) en verde contra el proveedor real. Cobertura ≥ 95% en todos los módulos nuevos.
