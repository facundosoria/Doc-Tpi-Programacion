# Estándar Kafka del PDF `KAFKA.pdf` — 2026-09-20

## Decisión anterior

Los eventos de `llm-service` seguían `KAFKA_EVENT_STANDARD.md` (v2 del AsyncAPI): envelope de seis campos con
`eventVersion`, `eventType` con guiones (`ATTEMPT-CLOSED`), un tópico por dominio propuesto por nosotros
(`practice-events`, `evaluation-events`, `moderation-events`), broker `kafka:9092` y dead-letter en `<tópico>.dlt`.
En el Skill Hub quedaba, además, el estándar v1 (`<evento>.v<major>`, `version/occurredAt/data`).

## Motivo

La cátedra (grupo de Notificaciones) publicó el PDF «Apache Kafka → Eventos que conectan servicios» con el contrato
común de todos los microservicios. Sostener otro formato obliga a Tema 05 y a Notificaciones a adaptarse a un formato
que nadie más usa.

## Regla vigente

[`contracts/KAFKA_EVENT_STANDARD.md`](../contracts/KAFKA_EVENT_STANDARD.md) y [ADR-020](../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md):

- Envelope `{eventId, eventType, timestamp, producer, payload}`, sin `eventVersion`, todo en inglés.
- `eventType` en `MAYÚSCULAS_CON_GUION_BAJO`.
- Bus `event-bus:29092` (`KAFKA_BOOTSTRAP`); `group-id` = nombre del servicio.
- Los grupos no crean tópicos: los nombres de `llm-service` son **provisorios** hasta que Notificaciones los asigne.
- Sin tópico `.dlt`: los mensajes rechazados quedan en la tabla `event_dead_letter` (migración `V38` tras la [integración del 2026-09-21](2026-09-21-integracion-main-a-dev.md); era la `V32` cuando se escribió este registro).
- Se conservan (el PDF no los contradice): Message Key por dominio, outbox, dedup por `eventId`, headers `traceparent` y
  `X-Request-Id`.

**Abierto con Notificaciones** ([pendientes](../07-planificacion-y-trabajo-equipo/11-equipos/notifications-service/pendientes.md)):
nombres de tópico, dead-letter, versionado sin `eventVersion`, valor de `producer` y forma de `timestamp` (el
`JsonSerializer` del PDF escribe un `Instant` como número, no como el texto de su ejemplo).

## Fuentes

`KAFKA.pdf` (material de la cátedra; no se versiona por `.gitignore`).

## Documentos V2 corregidos

`contracts/KAFKA_EVENT_STANDARD.md` (reescrito), `contracts/llm-service.asyncapi.yaml` (v3.0.0), la guía y el skill de
Tema 05, los contratos por equipo, `requisitos-a-otros-micros.md`, el mapa de integración, los pendientes de
Notificaciones y Tema 05, las historias y tareas que nombraban los eventos, `00-gobierno-y-evolucion` (ADR-020) y
`skillhub/` (revisiones preparadas, no enviadas). Skills locales: `.agents/skills/contratos-kafka/` y la plantilla de
`contratos-api-gateway`.

## Documentos históricos relacionados

`contracts/historicos-y-contratos-v1/KAFKA_EVENT_STANDARD-v2-con-eventVersion.md` y `llm-service-v2.asyncapi.yaml`
se preservaron con aviso de reemplazo en su momento. **Actualización 2026-09-22:** toda la carpeta
`historicos-y-contratos-v1/`, junto con `90-mapa-de-integracion-historico.md` y
`91-contratos-inter-equipos-historicos.md`, se retiró de `docs/contracts/` — ver
[`2026-09-22-limpieza-historicos-contracts.md`](2026-09-22-limpieza-historicos-contracts.md). Esta
entrada documenta la decisión del envelope Kafka tal como se tomó ese día; no afecta esa decisión.

## Responsable

Equipo `llm-service` (Tema 07).

## Evidencia de prueba

`EventOutboxRelayEnvelopeTest` (exactamente cinco campos), `PracticeAttemptClosedListenerTest`,
`AttemptEvaluationServiceTest`, `ModerationResolutionEventTest`, `ArchitectureTest` y `EventOutboxKafkaFlowIT`
(broker embebido + Postgres con Flyway hasta V32): envelope sin `eventVersion` ni header, y un mensaje inválido queda en
`event_dead_letter` sin bloquear el consumo.
