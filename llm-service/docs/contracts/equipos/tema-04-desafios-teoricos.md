# Tema 04 — Desafío Teórico ("corregir") — contratos

> Fuente: no hay contrato técnico acordado — a diferencia del resto de las carpetas de `equipos/`,
> acá no hay endpoint, evento ni scope M2M. Lo único que existe es una recomendación técnica
> nuestra, sin integración: [`docs/entregas/recomendacion-correccion-respuestas-cortas.md`](../../01-vision-alcance-y-entrega/03-entregas/recomendacion-correccion-respuestas-cortas.md).

## Qué nos llama

Nada. No hay ninguna llamada de Tema 04 hacia `llm-service` ni al revés.

## Qué le damos

Nada por API. Le dimos una recomendación técnica en texto (normalización + distancia de edición
para respuestas cortas con clave única) que **Tema 04 implementa en su propio motor**, sin
necesidad de llamarnos.

## Por qué no hay más que esto

Porque lo que sí requeriría integración — un corrector semántico basado en LLM, con RAG, para
respuestas parciales o parafraseadas — está **fuera de alcance** por decisión ya resuelta del
Product Owner:
[P-01](../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md#-p-01--corrector-de-respuestas-abiertas-fuera-de-alcance)
(2026-09-06). Mientras esa decisión no se reabra, no hay contrato que escribir acá más allá de
esta nota. Ver [`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/tema-04-desafios-teoricos/pendientes.md) para lo que sigue abierto.

## Deslinde de alcance ya acordado

- **Corrección de opción múltiple, V/F, ordenar, emparejar, algoritmos con tests y respuesta corta
  con clave única**: es de Tema 04, se resuelve con código propio, no con IA
  ([`04-funciones-de-ia.md` §1c](../../03-capacidades-de-ia/02-funciones-de-ia.md#1c-en-la-corrección-tres-de-cada-cuatro-tipos-no-necesitan-llm)).
- **Corrección de respuesta abierta / desarrollo**: revisión docente, fuera del evaluador de uso de
  IA y fuera de `llm-service` (P-01).
