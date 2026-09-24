# Moderación (EP-08) — contrato y pendientes con `chat-service`

> Documento para pasar al equipo de `chat-service` (y, en la sección 4, al de `notifications-service`).
> Contrato de referencia: [`llm-service-v1-moderacion.openapi.yaml`](llm-service-v1-moderacion.openapi.yaml) v1.1.0.
> Fecha: 2026-09-19 · Dueño del lado `llm-service`: EP-08.

## 1. Qué hace `llm-service` por vos

Antes de publicar un mensaje, `chat-service` llama de forma **síncrona** a
`POST /moderation/v1/decisions` (por el Gateway: `/api/llm/moderation/v1/decisions`) y publica **solo**
si la decisión es `ALLOW`.

| `decision` | Qué debe hacer chat |
|---|---|
| `ALLOW` | Publicar el mensaje. |
| `BLOCK` | No publicar. Mostrar al alumno que fue bloqueado; puede apelar (`incident_id`). |
| `PENDING` | No publicar (timeout de 800 ms o motor caído). Nunca se asume `ALLOW`. |
| `PENDING_REVIEW` | No publicar. Modo degradado: queda en bandeja del docente. |

Es idempotente por `message_id`: reenviarlo devuelve la decisión original aunque cambie el texto.

## 2. Lo que `chat-service` nos tiene que enviar (request)

| Campo | Obligatorio | Nota |
|---|---|---|
| `message_id` | sí | Único por mensaje; clave de idempotencia. |
| `course_id` | sí | |
| **`sender_id`** | **sí (breaking v1.1.0)** | Id del usuario que escribió el mensaje; es el dueño del incidente y el único que puede apelar. **Hoy no lo mandan** y nosotros probamos con un valor mockeado. |
| `sender_role` | sí | `student` / `teacher` / `system`. |
| `text` | sí | Máx. 4096 caracteres. Solo el mensaje, nunca el historial. |
| `context_flags` | no | `thread_id`, `is_reply`. |

Autenticación: principal de **servicio** con scope `moderation:decide` (headers de Gateway).

## 3. Decisiones que tiene que cerrar el equipo de chat

| # | Pregunta | Por qué nos importa |
|---|---|---|
| C1 | ¿Cuándo empiezan a enviar `sender_id` real? | Hoy es mock; sin esto los incidentes no tienen dueño real. |
| C2 | ¿Cómo manejan `PENDING` / `PENDING_REVIEW` en la UI del alumno (spinner, "en revisión", reintento)? | Definir si reintentan con el mismo `message_id` (recomendado, es idempotente). |
| C3 | ¿Qué hace chat si el timeout de 800 ms vence del lado de ellos (sin respuesta nuestra)? | Debe tratarlo como `PENDING`, no publicar. Confirmar. |
| C4 | ¿Quién reintenta y cuántas veces si `llm-service` responde 5xx? | Evitar duplicados: siempre reusar `message_id`. |
| C5 | ¿Escuchan el evento `MESSAGE_UNBLOCKED` (topic `moderation-events`, key `courseId`) para publicar el mensaje tras una reversión docente? | Al revertir, chat es quien debe publicar/mostrar el mensaje. Definir si consumen Kafka o prefieren callback HTTP. |
| C6 | ¿Retirar un mensaje ya publicado? | Un `ALLOW` no se puede retirar sin revisión explícita. Si chat necesita retirar mensajes, debe pasar por moderación: `DELETE /moderation/v1/decisions/{message_id}` responde `409 PROTOCOL_VIOLATION` para un `ALLOW` o sin revisión previa. |

## 4. Pendiente con `notifications-service` (otro equipo)

`llm-service` ya publica hacia notificaciones cuando el docente resuelve `REVERSED`, con dos canales reales:
POST HTTP por el Gateway a `/api/notifications/v1/moderation-events` (ruta **supuesta**, configurable con
`NOTIFICATIONS_MODERATION_EVENTS_PATH`) y evento Kafka `MESSAGE_UNBLOCKED`. Falta que ese equipo confirme:

| # | Pregunta |
|---|---|
| N1 | ¿La ruta y el payload actuales (`eventId`, `messageId`, `incidentId`, `courseId`, `userId`, `resolvedBy`, `resolution`, `resolutionReason`) son los que esperan, o prefieren solo Kafka? |
| N2 | ¿Qué texto/plantilla reciben los alumnos para `REVERSED` y `CONFIRMED`? Hoy solo se notifica `REVERSED`. |
| N3 | Política ante fallo: hoy es *best effort* (se loguea y no se revierte la resolución). ¿Alcanza? |

Mientras no esté acordado, se puede correr en modo mock con `NOTIFICATIONS_ENABLED=false` (solo log).

## 5. Fuera de chat (para que no se mezcle)

- Períodos de retención 30/90 días: decisión de producto/seguridad, no de chat.
- Umbrales de los detectores: calibración con datos reales, interna de EP-08.
