---
slug: kafka-event-contract-rules
title: Kafka event contract rules
description: 'Fixed rules an async event contract must meet to travel the platform bus: past-tense versioned
  topic name, common envelope, at-least-once idempotent consumers, outbox publishing, event versioning.'
when_to_use: defining, publishing or consuming a Kafka event; writing the AsyncAPI; naming an event topic;
  envelope fields; idempotent consumer; outbox pattern; versioning an event payload
stack: shared
type: reference
owning_team: LLM
version: 3
tags:
- asyncapi
- contracts
- events
- idempotency
- kafka
- outbox
- versioning
---

## Rule

This is a **reference to consult before defining, publishing or consuming an event, or writing the AsyncAPI**, not a generator. An async event contract must be shaped so it can travel the platform bus. Six fixed rules; correlation is covered separately by [[request-correlation-across-http-and-kafka]].

1. **Topic name** `<event>.v<major>` — snake_case, past tense (a fact that already happened): `intento_cerrado.v1`, `score_de_ia_calculado.v1`. Not a command, not a verb in the imperative.
2. **Common envelope** on every event; your own content goes in `data`:
   `eventId` (uuid, doubles as the idempotency key), `version` (payload version, e.g. `"1.0"`), `occurredAt` (date-time), `producer` (emitting service), `data` (object).
3. **At-least-once, idempotent consumers** — the bus may redeliver. Every consumer dedups on `eventId` (or `(consumer, eventId)`) so processing twice has no double effect. No total order across topics; if order matters, same topic + same partition key.
4. **Publish via outbox** — write the event to an outbox table in the *same transaction* as the state change; a relay/Debezium publishes it. Never call `producer.send()` by hand inside business logic: rollback ⇒ no event, commit ⇒ event for sure.
5. **Event versioning** — compatible change (new optional `data` field) bumps `version`, same topic. Incompatible change (rename/remove/retype) ⇒ new topic `<event>.v2`; v1 and v2 coexist until every consumer migrates; v1 is never changed destructively.
6. **Cross-team fields** — a field another team needs is agreed before the contract freezes, documented with owner and date. Don't add fields "just in case"; don't modify another team's event contract.

Publishing an event is not a POST to another microservice: if you need a response it is HTTP, if you notify a fact that already happened it is an event. The platform messaging team owns topic naming, the envelope and delivery guarantees; the payload of *your* events (which fields, which enums) is yours.
