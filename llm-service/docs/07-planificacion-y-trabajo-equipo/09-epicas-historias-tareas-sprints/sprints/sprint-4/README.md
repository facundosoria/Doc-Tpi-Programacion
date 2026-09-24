# Sprint 4 — EP-08 completa e integración en ambiente compartido

> **Estado:** Por planificar — el registro se crea en la Planning de S4.
> **Referencia:** [`38`](../../../05-plan-de-cinco-sprints.md) · Parte 3 · Sprint 4
> **Recalibrado el 2026-09-13 (noche):** se carga EP-08 (moderación integrada) completa encima
> de la integración E2E original.
> **Corregido el mismo día (2ª vuelta):** la primera versión de esta nota usaba ~624 h — la
> referencia histórica de `35` (3 paquetes × ~208 h). Las 6 fichas reales de EP-08
> ([`historias/ep-08/`](../../historias/ep-08/README.md)) suman **160 h**, no 624. Con el
> número real, S4 **no** necesita superar el techo de 581,6 h — queda muy por debajo (~35–40%).

## Objetivo

Integración en el ambiente compartido de la plataforma (no local), corrección de lo que la integración real destape, verificación de punta a punta con los otros equipos, **y cerrar EP-08 completa: moderación previa a entrega, apelación y resolución docente, degradación y retención de evidencia.**

## HU / Tareas comprometidas

### EP-08 — Moderación integrada completa (P1+P2+P3) · 160 h (real, ficha por ficha)

> Épica y fichas ya escritas — [`historias/ep-08/`](../../historias/ep-08/README.md). Receta histórica de referencia: [`35` · S11–S13](../../../04-backlog-ejecutable.md) (~624 h) — la ficha real de cada historia suma 160 h.
> **Prerequisito real, no de horas:** contrato de moderación acordado con `chat-service` y política aprobada por producto/seguridad ([`epicas/ep-08.md`](../../epicas/ep-08.md) · Suposiciones). Si el contrato no cerró para el arranque de S4, el trabajo propio se construye contra un stub y queda declarado como no integrado hasta que `chat-service` entregue — no se retrasa el sprint por eso.

| ID | Título | Tipo | Pareja | h (real) | Ficha |
|---|---|---|---|---:|---|
| LLM-S11-H01 | Que un mensaje del chat se permita o bloquee antes de entregarse | Tarea (habilitador — contrato con chat) | P3+P2+P1 | 34 | [h01.md](../../historias/ep-08/h01.md) |
| LLM-S11-H02 | Detectar spam, contenido ofensivo e intentos de ocultar código | Tarea (habilitador) | P3 | 28 | [h02.md](../../historias/ep-08/h02.md) |
| LLM-S12-H01 | Apelar un mensaje que bloqueó la moderación | HU de valor | P3 | 26 | [h03.md](../../historias/ep-08/h03.md) |
| LLM-S12-H02 | Que el docente revise un incidente de moderación y lo resuelva | HU de valor | P3 | 30 | [h04.md](../../historias/ep-08/h04.md) |
| LLM-S13-H01 | Que la moderación siga funcionando (en modo degradado) si el clasificador contextual falla | Tarea (habilitador) | P3+P2 | 24 | [h05.md](../../historias/ep-08/h05.md) |
| LLM-S13-H02 | Que la evidencia de moderación se retenga el tiempo necesario y luego se purgue | Tarea (habilitador) | P1 | 18 | [h06.md](../../historias/ep-08/h06.md) |

### Integración real con la plataforma

| Tarea | Descripción |
|---|---|
| Despliegue en ambiente compartido | Eureka/Gateway real, no compose local |
| Integración E2E con `courses-service` | Golden set → calibración → tutor → cierre → evaluación → apelación |
| Integración E2E con `practice-service` | Consumo real de `ATTEMPT_CLOSED` desde el bus de eventos real (desde el 2026-09-13; antes `challenges-service`) |
| Corrección de errores de integración | Lo que aparece siempre al pasar del fake al real en ambiente compartido |

### Hardening de seguridad

| Tarea | Descripción |
|---|---|
| Corpus de jailbreak actualizado | Pruebas contra el proveedor real con el corpus completo |
| Permisos y scopes en ambiente real | Verificar M2M, tokens y scopes contra el gateway real |
| Retención y purga de datos | Verificar que logs no guardan contenido de alumnos ni respuestas del modelo |

## Resumen

| Bloque | h estimadas |
|---|---|
| EP-08 completa | 160 (real) |
| Despliegue + integración E2E | ~20–30 |
| Corrección de errores de integración | ~15–25 |
| Hardening seguridad | ~10–15 |
| **Total** | **~205–230 h** |

> **Techo de referencia del sprint: 581,6 h (~35–40% con el número real).** La primera versión de esta nota decía que S4 superaba el techo en ~15–19% usando la referencia histórica de ~624 h para EP-08; con las fichas reales (160 h) **no hace falta superar el techo**. Si el contrato con `chat-service` no cerró, EP-08 se declara "construida, no integrada" en el cierre del sprint en vez de retrasar el resto — ese riesgo sigue igual, cambió solo la cuenta de horas.

## Demo de S4

El flujo completo de punta a punta en el ambiente compartido: docente crea golden set → calibración activa → alumno cierra intento → evaluación automática → alumno apela → docente resuelve. Todo sin intervención manual y sin datos del fake. **Más:** un mensaje de chat ofensivo se bloquea antes de entregarse, el alumno lo apela y el docente lo resuelve (EP-08) — contra el `chat-service` real si el contrato cerró, contra un stub si no.
