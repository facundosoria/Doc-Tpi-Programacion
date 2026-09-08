# Adenda — Tutor SSE (Buffer Interceptor)

Esta adenda completa el contrato v1 con la variante en streaming del tutor: la respuesta se
emite token a token en vez de un bloque único. Implementa el **Buffer Interceptor** de la guía
didáctica ([14 §C-2 / A-1](../14-sincronizacion-guia-didactica.md)), que revisa a
[ADR-009](../08-decisiones-y-pendientes.md).

**Debe fusionarse en `llm-service-v1.openapi.yaml` cuando se cierre [I-10](../17-mapa-de-integracion.md)
(streaming: propagar o revertir) y `practice-service` (Tema 05) acuerde consumir SSE.** Hasta
entonces el contrato ejecutable sigue siendo solo `POST /api/llm/tutor/interactions` con respuesta
completa. Al fusionar, `info.version` pasa a `1.1.0` (cambio aditivo, retrocompatible).

## Endpoint

`POST /api/llm/tutor/interactions/stream`

- Mismo `requestBody` que `POST /tutor/interactions` (`TutorInteractionRequest`).
- Mismo `Idempotency-Key` obligatorio. Reintento con la misma clave: el servidor no rebobina el
  stream — reemite el resultado final como un único `segment` + `done` con el estado almacenado.
- El cliente debe enviar `Accept: text/event-stream`.
- Correlación en headers de respuesta: `traceparent` + `X-Request-Id`.
- Errores **antes** de abrir el stream: `403`, `429`, `503` como Problem Details RFC 7807, igual
  que el endpoint síncrono.
- Errores **a mitad** de stream (p. ej. caída del proveedor): un evento `error` con cuerpo
  Problem Details y cierre del stream.

## Modelo de eventos

Máquina de estados `OUTSIDE_CODE` / `INSIDE_CODE`. El campo `event:` de SSE lleva el nombre; el
`data:` lleva el JSON de `TutorStreamEvent` (que repite el nombre en `type` para el discriminador).

| `event:` | Cuándo | `data:` |
|---|---|---|
| `token` | Prosa en vivo, fuera de bloque de código | `{ "type": "token", "text": "…" }` |
| `hold` | Se abrió un bloque de código: se congela la emisión y el tramo se acumula en RAM mientras corre el guardarraíl anti-fuga | `{ "type": "hold" }` |
| `segment` | Se libera un tramo ya validado: un bloque de código que pasó PAR-11, o la prosa que reanuda | `{ "type": "segment", "text": "…", "kind": "code" \| "prose" }` |
| `blocked` | Un bloque de código superó el umbral de similitud (PAR-11, default 70 %): se descarta y se regenera en silencio | `{ "type": "blocked", "regenerated": true }` |
| `done` | Evento terminal: estado final + metadata LLMOps (RF-IA-02/25) | `{ "type": "done", "state": "completed" \| "blocked" \| "unavailable", "metadata": { … } }` |
| `error` | Falla a mitad de stream; el stream se cierra después | Problem Details RFC 7807 con `code` estable |

### Nivel de riesgo del desafío

- `riskLevel` `high` / `medium`: el servidor **siempre** aplica el Buffer Interceptor — ningún
  bloque de código sale sin pasar por `hold` → validación → `segment` o `blocked`.
- `riskLevel` `low` (hackathon, code review — RF-IA-19): no hay una única solución esperada contra
  la cual comparar; el servidor puede emitir el código como `token` sin retención.
- `done.state = blocked` cuando la respuesta final tuvo que suprimirse por completo tras agotar la
  regeneración.

## Fragmento OpenAPI para fusionar

```yaml
paths:
  /tutor/interactions/stream:
    post:
      summary: Stream socrático del tutor con Buffer Interceptor (SSE)
      operationId: streamTutorInteraction
      description: >
        Variante SSE de POST /tutor/interactions. La prosa se emite token a token; al abrir un
        bloque de código la emisión se congela y el tramo se acumula hasta que el guardarraíl
        anti-fuga (RF-IA-20, PAR-11) lo valida o lo descarta. Requiere Accept: text/event-stream.
      parameters:
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/TutorInteractionRequest' }
      responses:
        '200':
          description: >
            Flujo SSE. Eventos: token, hold, segment, blocked, done, error. El data de cada
            evento sigue TutorStreamEvent.
          content:
            text/event-stream:
              schema: { $ref: '#/components/schemas/TutorStreamEvent' }
        '403': { $ref: '#/components/responses/Problem' }
        '429': { $ref: '#/components/responses/Problem' }
        '503': { $ref: '#/components/responses/Problem' }
components:
  schemas:
    TutorStreamEvent:
      oneOf:
        - $ref: '#/components/schemas/TutorStreamToken'
        - $ref: '#/components/schemas/TutorStreamHold'
        - $ref: '#/components/schemas/TutorStreamSegment'
        - $ref: '#/components/schemas/TutorStreamBlocked'
        - $ref: '#/components/schemas/TutorStreamDone'
        - $ref: '#/components/schemas/TutorStreamError'
      discriminator: { propertyName: type }
    TutorStreamToken:
      type: object
      required: [type, text]
      properties: { type: { type: string, const: token }, text: { type: string } }
    TutorStreamHold:
      type: object
      required: [type]
      properties: { type: { type: string, const: hold } }
    TutorStreamSegment:
      type: object
      required: [type, text, kind]
      properties:
        type: { type: string, const: segment }
        text: { type: string }
        kind: { type: string, enum: [prose, code] }
    TutorStreamBlocked:
      type: object
      required: [type, regenerated]
      properties: { type: { type: string, const: blocked }, regenerated: { type: boolean } }
    TutorStreamDone:
      type: object
      required: [type, state]
      properties:
        type: { type: string, const: done }
        state: { type: string, enum: [completed, blocked, unavailable] }
        metadata: { $ref: '#/components/schemas/TutorStreamMetadata' }
    TutorStreamError:
      type: object
      required: [type, status, code, title]
      properties:
        type: { type: string, const: error }
        status: { type: integer }
        code: { type: string }
        title: { type: string }
        detail: { type: string }
    TutorStreamMetadata:
      type: object
      properties:
        inputTokens: { type: integer, minimum: 0 }
        outputTokens: { type: integer, minimum: 0 }
        latencyMs: { type: integer, minimum: 0 }
        antifugaBlocks: { type: integer, minimum: 0 }
```

## Ejemplo de flujo

```
event: token
data: {"type":"token","text":"Pensá qué estructura te conviene para "}

event: token
data: {"type":"token","text":"contar frecuencias.\n\n"}

event: hold
data: {"type":"hold"}

event: blocked
data: {"type":"blocked","regenerated":true}

event: segment
data: {"type":"segment","kind":"code","text":"// pista: un Map<String,Integer> alcanza"}

event: done
data: {"type":"done","state":"completed","metadata":{"inputTokens":2980,"outputTokens":210,"latencyMs":1740,"antifugaBlocks":1}}
```
