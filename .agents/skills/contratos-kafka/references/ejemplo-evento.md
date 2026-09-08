# Ejemplo resuelto — contrato de eventos (Kafka)

> Ilustración. El skill genera la misma **estructura** para cualquier servicio.

## Contexto de entrada que aportó el equipo (resumido)

- **Servicio:** `llm-service`.
- **Publica:** `score_de_ia_calculado.v1` (el resultado de evaluar un intento) — lo
  consume el motor de desafíos para aplicar XP.
- **Consume:** `intento_cerrado.v1` (lo publica el motor de desafíos) — dispara la
  evaluación.
- **Regla de dominio:** `llm-service` **nunca** asigna XP; solo devuelve el score.

## Salida A — fragmento de AsyncAPI

```yaml
channels:
  intento_cerrado.v1:
    address: intento_cerrado.v1
    messages:
      intentoCerrado: { $ref: '#/components/messages/IntentoCerrado' }
  score_de_ia_calculado.v1:
    address: score_de_ia_calculado.v1
    messages:
      scoreCalculado: { $ref: '#/components/messages/ScoreCalculado' }
operations:
  consumeIntentoCerrado: { action: receive, channel: { $ref: '#/channels/intento_cerrado.v1' } }
  publishScoreCalculado: { action: send,    channel: { $ref: '#/channels/score_de_ia_calculado.v1' } }
components:
  schemas:
    Envelope:
      type: object
      required: [eventId, version, occurredAt, producer, data]
      properties:
        eventId:    { type: string, format: uuid }
        version:    { type: string, const: '1.0' }
        occurredAt: { type: string, format: date-time }
        producer:   { type: string }
        data:       { type: object }
    ScoreCalculado:
      allOf:
        - $ref: '#/components/schemas/Envelope'
        - type: object
          properties:
            data:
              type: object
              required: [attemptId, courseCohortId, learnerId, aggregateScore, dimensions, rubricVersion]
              properties:
                attemptId:      { type: string, format: uuid }
                courseCohortId: { type: string, format: uuid }
                learnerId:      { type: string, format: uuid }
                aggregateScore: { type: integer, minimum: 0, maximum: 100 }
                dimensions:     { $ref: '#/components/schemas/Dimensions' }
                confidence:     { type: number, minimum: 0, maximum: 1 }
                rubricVersion:  { type: string }
    IntentoCerrado:
      allOf:
        - $ref: '#/components/schemas/Envelope'
        - type: object
          properties:
            data:
              type: object
              required: [attemptId, courseCohortId, learnerId, transcript, rubricVersion]
              properties:
                attemptId:      { type: string, format: uuid }
                courseCohortId: { type: string, format: uuid }
                learnerId:      { type: string, format: uuid }
                transcript:     { type: array, items: { type: object } }
                rubricVersion:  { type: string }
```

## Salida B — sección del doc inter-equipos

**Publicamos `score_de_ia_calculado.v1`** — consumidor: motor de desafíos.
- `data`: `aggregateScore` (0–100), `dimensions`, `confidence`, `rubricVersion`. **Nunca
  un valor de XP.**
- El motor traduce el score a modificador y aplica XP + monedas en **una** transacción.

**Consumimos `intento_cerrado.v1`** — lo publica el motor de desafíos.
- Estructura mínima que necesitamos: `attemptId`, `courseCohortId`, `learnerId`,
  `transcript`, `rubricVersion` (**obligatorio** para elegir la calibración).
- 🔴 Pedir estos campos antes de que el equipo dueño cierre el contrato de eventos de la
  plataforma.

---

## Por qué queda así

- **Topic `<evento>.v1`** en pasado: `intento_cerrado`, `score_de_ia_calculado` — son
  hechos consumados, no comandos.
- **Envelope común** en los dos: `eventId` sirve para que el consumidor deduplique
  (at-least-once).
- La **correlación** (`traceparent`, `X-Request-Id`) va en headers de Kafka, no en
  `data`. Al consumir `intento_cerrado` y publicar `score_de_ia_calculado`, se propagan
  los mismos headers.
- El **consumidor de `intento_cerrado` es idempotente**: si el evento llega dos veces, se
  encola una sola evaluación (índice por `eventId`).
- `score_de_ia_calculado` se publica por **outbox**: se escribe en la misma transacción
  que guarda el resultado de la evaluación.
- El `data` de `score_de_ia_calculado` **no** tiene XP — es la frontera de dominio: el
  `llm-service` da el número, el motor de desafíos aplica la economía.
- `rubricVersion` es **obligatorio** en el evento que consumimos porque sin él no se sabe
  contra qué calibración evaluar — se pide antes de que el contrato se congele.
