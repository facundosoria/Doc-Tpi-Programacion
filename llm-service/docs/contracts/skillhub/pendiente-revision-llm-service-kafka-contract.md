# `llm-service-kafka-contract` en el Skill Hub — estado vigente (v5)

**Estado (verificado 2026-09-20): ENVIADA Y ACEPTADA.** Revisión **v5**; un admin la aceptó y
`get_skill` devuelve la v5 (adjunto verificado: mismo tamaño y `sha256` que el archivo del repo).
Adjunto: [`../llm-service.asyncapi.yaml`](../llm-service.asyncapi.yaml) v3.0.0, 14.799 bytes,
`sha256 65dc6ce348c1f012f9de5b3e1b60923a3189e83d3b45539a4885dcc6a5245e5d`.

Motivo de esta revisión: el estándar Kafka de la cátedra (`KAFKA.pdf`, ADR-020) reemplazó el
envelope que usaba la v4 publicada. Lo que cambió en `content`: envelope de cinco campos sin
`eventVersion`, `eventType` con guion bajo, bus `event-bus:29092`, tópicos provisorios, sin
`.dlt` (tabla `event_dead_letter`), consumidor `Event<?>` con `__TypeId__` y `timestamp` como
texto. También cambió `when_to_use`.

**`description`:** Canonical AsyncAPI contract for the Kafka events llm-service publishes and consumes, including the attempt evaluation flow with practice-service. Follows the platform Kafka standard (five-field envelope, no eventVersion).

**`when_to_use`:** Use when consuming llm-service scores, calibration outcomes or security incident events, or publishing ATTEMPT_CLOSED, over Kafka: practice-events, evaluation-events, consumer group, dead letter.

**`tags`:** asyncapi, events, kafka, llm-service, microservices, practice-service, evaluator, contracts

**`rationale`:** The platform Kafka standard (KAFKA.pdf, from the course staff) replaced the envelope this contract used. The envelope now has exactly five fields (`eventId`, `eventType`, `timestamp`, `producer`, `payload`) and no `eventVersion`; event types are `UPPER_SNAKE_CASE` (`ATTEMPT_CLOSED`, `SCORE_CALCULATED`, `SCORE_DEFERRED`, `MESSAGE_UNBLOCKED`); the bus is `event-bus:29092`; groups cannot create topics, so there is no `.dlt` topic and the topic names are provisional until the notifications group assigns them. The attached AsyncAPI is v3.0.0 of the repo file. Verified against an embedded Kafka broker (envelope with exactly five fields, no `eventVersion` header, malformed event kept in a dead-letter table without blocking the partition).

**`content`:** ver el texto completo en el hub (`get_skill llm-service-kafka-contract`, versión 5). Resumen de las
secciones enviadas: *Rule* (envelope de cinco campos), *Test bot first, real model later* (sin cambios salvo nombres de evento
y sin `eventVersion`), *Attempt evaluation (practice-service)* (bus y tópicos provisorios, envelope, payloads de
`ATTEMPT_CLOSED`/`SCORE_CALCULATED`/`SCORE_DEFERRED`, consumidores, `timestamp`, entradas inválidas, evolución) y *Reasoning*.
