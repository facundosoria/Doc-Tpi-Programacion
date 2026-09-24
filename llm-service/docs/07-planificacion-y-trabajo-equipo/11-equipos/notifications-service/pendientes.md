# Notifications-service — pendientes (EP-08 moderación)

> Contexto: [`moderacion-pendientes-chat-service.md`](../../../contracts/moderacion-pendientes-chat-service.md) §4 ·
> historia [LLM-S12-H02 (h04)](../../09-epicas-historias-tareas-sprints/historias/ep-08/h04.md), CA5.

`llm-service` ya notifica cuando un docente resuelve un incidente como `REVERSED`, con dos canales reales:
`POST` por el Gateway a `/api/notifications/v1/moderation-events` (ruta **supuesta**, configurable con
`NOTIFICATIONS_MODERATION_EVENTS_PATH`) y evento Kafka `MESSAGE_UNBLOCKED` (topic `moderation-events`, provisorio; key
`courseId`). Con `NOTIFICATIONS_ENABLED=false` corre en modo mock (solo log).

## 🟡 Acuerdo cruzado — a confirmar con el equipo de notificaciones

| # | Pregunta |
|---|---|
| N1 | ¿La ruta y el payload actuales (`eventId`, `messageId`, `incidentId`, `courseId`, `userId`, `resolvedBy`, `resolution`, `resolutionReason`) son los que esperan, o prefieren solo Kafka? |
| N2 | ¿Qué texto/plantilla reciben los alumnos para `REVERSED` y `CONFIRMED`? Hoy solo se notifica `REVERSED`. |
| N3 | Política ante fallo: hoy es *best effort* (se loguea y no se revierte la resolución). ¿Alcanza? |

## 🔴 Bus Kafka — a definir con el grupo de Notificaciones (dueño del bus)

Según el PDF `KAFKA.pdf` (transcrito en [`KAFKA_EVENT_STANDARD.md`](../../../contracts/KAFKA_EVENT_STANDARD.md),
[ADR-020](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md)), los grupos **no crean tópicos**: los pide
el grupo de Notificaciones. Hay que resolver:

| # | Pregunta |
|---|---|
| K1 | **Nombres definitivos de tópico** para `llm-service`. Hoy usamos, provisoriamente: `practice-events` (entrada, Tema 05), `evaluation-events` (salida), `moderation-events` (salida), `calibration-events` (salida). Ver el [AsyncAPI](../../../contracts/llm-service.asyncapi.yaml). |
| K2 | **Dead-letter.** Ya no publicamos a `<tópico>.dlt` (no podemos crearlo): los mensajes rechazados quedan en la tabla `event_dead_letter` de `llm-service`. ¿Habrá un tópico de dead-letter común, o alcanza con eso? |
| K3 | **Versionado de eventos.** El envelope del PDF no tiene `eventVersion`. ¿Cómo se avisa y se coordina un cambio incompatible en un `payload`? Mientras tanto: solo cambios compatibles y aviso previo. |
| K4 | **Valor de `producer`.** Hoy publicamos `llm-service`; el PDF usa `challenges-service` en un ejemplo y `tema-XX-service-name` en la plantilla. |
| K5 | **Forma de `timestamp`.** El ejemplo del PDF es texto ISO-8601, pero `JsonSerializer` con un `Instant` escribe un número. ¿Cuál es la oficial? Nosotros publicamos texto ISO-8601 y aceptamos ambos al consumir. |
| K6 | **Ambientes y seguridad.** Cuál es el de pruebas de Tema 05 y cuál el de producción; si el bus pide TLS/SASL (el PDF no lo menciona). |
