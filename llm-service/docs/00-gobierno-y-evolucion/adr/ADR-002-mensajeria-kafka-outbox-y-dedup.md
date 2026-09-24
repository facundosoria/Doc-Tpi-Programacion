# ADR-002 — Mensajería Kafka: outbox transaccional y deduplicación por `eventId`

- **Estado:** Aceptado el 2026-09-19 por Facundo Soria (revisión y aprobación directa en la sesión de trabajo, sin PR formal)
- **Fecha:** 2026-09-18
- **Historia:** LLM-EP01-H07
- **Contrato de plataforma:** [`KAFKA_EVENT_STANDARD.md`](../../contracts/KAFKA_EVENT_STANDARD.md); eventos del servicio en [`llm-service-v1.asyncapi.yaml`](../../contracts/llm-service.asyncapi.yaml) (v2.0.0).

## Contexto

El servicio debe publicar y consumir eventos sin perderlos ni duplicar sus efectos. Guardar en la base
y publicar en Kafka son dos escrituras que no pueden ser atómicas entre sí.

## Decisión

1. **Envelope y topics** según el estándar de plataforma: `{eventId, eventType, eventVersion, timestamp, producer, payload}`, un topic por dominio (`moderation-events`, `practice-events`), `eventType` en MAYÚSCULAS-CON-GUIONES. Message Key de `moderation-events`: `courseId`.
2. **Productor con outbox transaccional.** La lógica de negocio llama a `KafkaEventProducer.enqueue`, que inserta en `event_outbox` dentro de su misma transacción; `EventOutboxRelay` publica de forma asíncrona y marca `published_at`. Nadie llama a `KafkaTemplate.send` desde negocio. `traceparent` y `X-Request-Id` viajan como headers del mensaje, nunca en el payload.
3. **Consumidor idempotente.** Reserva el `eventId` en `kafka_consumed_events` (append-only, PK) antes de procesar; un duplicado se ignora. Un mensaje sin `eventId` o mal formado va a `<topic>.dlt` y no bloquea la partición.
4. **Apagado por defecto** (`LLM_KAFKA_ENABLED=false`): el outbox siempre escribe, pero el relay y los `@KafkaListener` solo corren con el flag prendido. `docker compose` lo enciende.

## Consecuencias

- Entrega *at-least-once*; la deduplicación es responsabilidad del consumidor.
- Las filas del outbox no se purgan todavía (retención pendiente).
- Sin definir aún (el estándar los deja abiertos): reintentos, particiones, replication factor, consumer groups y política ante versiones desconocidas.
- El contrato real de `practice-events` sigue en fixture provisorio hasta que el Tema 05 lo congele.
