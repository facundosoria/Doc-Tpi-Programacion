# Tema 05 — Desafíos Prácticos — contratos

> Fuente completa: [18 §4.3](../../18-contratos-inter-equipos.md#43-tema-05--desafíos-prácticos),
> [docs/entregas/alcance-y-contrato-para-desafios-practicos.md](../../entregas/alcance-y-contrato-para-desafios-practicos.md)
> (carta dirigida a `practice-service`).

## Qué nos llama

- `POST /ai/tutor` (asistencia sincrónica, presupuesto **< 2 s**) → hoy es
  `POST /api/llm/tutor/interactions` en el contrato v1 vigente
  ([`llm-service-v1.openapi.yaml`](../../contracts/llm-service-v1.openapi.yaml)).

### Cuerpo de la solicitud ✅ (schema real: `TutorInteractionRequest`)

```json
{
  "attemptId": "b1e2c3d4-0001-4a00-8000-000000000001",
  "challengeId": "b1e2c3d4-0002-4a00-8000-000000000002",
  "courseCohortId": "b1e2c3d4-0003-4a00-8000-000000000003",
  "learnerId": "b1e2c3d4-0004-4a00-8000-000000000004",
  "message": "No entiendo por qué mi recursión no corta en el caso base",
  "riskLevel": "medium"
}
```

Header obligatorio: `Idempotency-Key` (UUID). `riskLevel` es el único campo que hoy decide el
comportamiento del guardarraíl (`high | medium | low`) — lo fija Tema 05 según el tipo de
desafío, nosotros no lo inferimos.

## Qué le damos

- Respuesta completa del tutor (200), **síncrona, sin streaming todavía** — ver
  [`pendientes.md`](pendientes.md).
- El guardarraíl anti-fuga (RF-IA-20) corre de nuestro lado antes de devolver la respuesta:
  nunca se expone la solución ni los tests ocultos (ADR-008).

### Cuerpo de la respuesta ✅ (schema real: `TutorInteractionResponse`)

```json
{
  "message": "¿Qué pasa con `n` en cada llamada recursiva? Fijate qué valor tiene justo antes de que se cumpla la condición de corte.",
  "state": "completed"
}
```

`state` es el único enum publicado hoy: `completed | blocked | unavailable`. Es más angosto que
lo que muestran los diagramas de doc 17 (que hablan de streaming y de estados intermedios) —
mientras no se fusione la adenda SSE, esto es todo lo que el contrato ejecutable promete.

## Qué pasa si esto falla

Técnica común (Resilience4j, escalera de degradación) en
[transversales del README](../README.md#resiliencia-y-manejo-de-errores-técnica-común-a-todos-los-endpoints).
Caso puntual del tutor, vía el campo `state` de la respuesta:

| `state` | Cuándo pasa | Qué ve Tema 05 |
|---|---|---|
| `completed` | El modelo respondió y pasó el guardarraíl de salida | Respuesta normal |
| `blocked` | El guardarraíl anti-fuga (ADR-008) detectó que la respuesta se acercaba a la solución esperada | Mensaje regenerado o bloqueado — **hoy no se produce en el código real** (`docs/estado-implementacion/ep-05/interactions.md`): solo hay guardarraíl de entrada implementado, el de salida está pendiente |
| `unavailable` | Se agotó la escalera de degradación (Nivel 1-3 fallaron: modelo primario, otro proveedor, modelo local) | El tutor no puede responder — presupuesto de 2 s ya se gastó en los reintentos, así que no hay margen para más de un fallback |

Si el `503`/`unavailable` se sostiene, Tema 05 tiene que decidir qué mostrarle al alumno — no
hay hoy un acuerdo escrito de UX para ese caso (relacionado con la pantalla 1 de
[`frontend-angular/pendientes.md`](../frontend-angular/pendientes.md)).

## Deslinde de alcance ya acordado

- **"Originalidad entre alumnos"** (comparar una entrega contra otra, o contra ediciones
  anteriores del mismo alumno) **es responsabilidad de Tema 05, no nuestra**
  ([`02-arquitectura-y-stack.md`](../../02-arquitectura-y-stack.md) línea 303). Lo nuestro es
  el perímetro anti-fuga de un único intento contra su propia solución esperada.
- La carta completa de alcance ([`docs/entregas/alcance-y-contrato-para-desafios-practicos.md`](../../entregas/alcance-y-contrato-para-desafios-practicos.md))
  ya detalla esta frontera para que Tema 05 la lea sin ambigüedad.
