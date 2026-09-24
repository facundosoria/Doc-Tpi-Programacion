# 01 — Integración y contratos entre microservicios

## Audiencia y objetivo

**Audiencia:** desarrolladores de `practice-service`, `courses-service`, `admin-service` y
consumidores indirectos del score.

**Objetivo:** identificar qué interfaz deben consumir o proveer, qué información es obligatoria y
qué puntos aún requieren un acuerdo antes de programar.

Los schemas exactos vivirán en `contracts/`. Este documento no los duplica.

## Reglas comunes

- HTTP pasa por API Gateway, con JWT M2M `aud=llm-service`, scope mínimo, identidad delegada y
  `traceparent` + `X-Request-Id`.
- Cada escritura HTTP requiere `Idempotency-Key` UUID y responde errores RFC 7807.
- Kafka sigue el estándar del PDF [`KAFKA_EVENT_STANDARD.md`](KAFKA_EVENT_STANDARD.md): envelope `eventId`,
  `eventType` (`MAYÚSCULAS_CON_GUION_BAJO`), `timestamp`, `producer` y `payload` (sin `eventVersion`), bus
  `event-bus:29092`, tópicos asignados por Notificaciones; correlación en headers; entrega at-least-once,
  deduplicación por `eventId` y outbox.
- Los datos de un servicio pertenecen a su dueño. Tema 07 no escribe en bases ajenas ni inventa
  respuestas para reemplazar otro microservicio.

## Matriz de integración

| Par | Dirección | Canal | Necesidad de Tema 07 | Estado |
|---|---|---|---|---|
| `practice-service` | hacia Tema 07 | Kafka | Informar que una práctica fue publicada para asignarle la calibración activa de su cohorte. | Solicitado: ver INT-000. |
| `practice-service` | hacia Tema 07 | Kafka | Informar el inicio del primer intento para inmovilizar la calibración asignada. | Solicitado: ver INT-001. |
| `practice-service` | hacia Tema 07 | HTTP | Invocar tutor con contexto pedagógico validado. | Propuesto: schema y tratamiento seguro en OpenAPI; requiere aprobación de Practice. |
| `practice-service` | hacia Tema 07 | Kafka | Publicar el cierre de un intento para disparar su evaluación. | Bloqueado: falta schema completo de `ATTEMPT_CLOSED`. |
| Tema 07 | hacia `practice-service` | Kafka | Publicar score calculado o evaluación diferida; Practice reenvía el resultado a Challenges. | Propuesto: schema en AsyncAPI; requiere aprobación de Practice. |
| `courses-service` | hacia Tema 07 | HTTP | Consultar calibración activa y evaluaciones pendientes antes de activar/cerrar una cohorte. | Propuesto: schema en OpenAPI; requiere aprobación de Courses. |
| Tema 07 | hacia `courses-service` | HTTP | Validar pertenencia docente activa para operaciones de cohorte. Ruta propuesta: `GET /api/courses/{courseCohortId}/members/{userId}`. | Pendiente de aprobación del dueño de Courses. |
| `admin-service` | hacia Tema 07 | HTTP | Gestionar modelos, Golden Set y calibraciones con identidad docente delegada. | Propuesto: schema en OpenAPI; requiere aprobación de Admin. |
| `challenges-service` | indirecta | Kafka vía `practice-service` | Aplicar XP según el score que Practice reenvía. | Sin llamada directa Tema 07 ↔ Challenges. |

## Acuerdos que faltan antes de promover contratos a Acordado

### INT-000 — Publicación de práctica

**Dueño:** `practice-service`.

Tema 07 necesita conocer que una práctica quedó publicada y disponible para alumnos. Ese hecho
permite asociar a su `challengeId` la calibración activa vigente en ese instante. Se solicita el
evento propuesto `PRACTICE_PUBLISHED`; el nombre final lo confirma el dueño. Ver el detalle en
[requisitos a otros micros](requisitos-a-otros-micros.md).

### INT-001 — Cierre de intento

**Dueño:** `practice-service`.

Antes del cierre, Tema 07 debe recibir `ATTEMPT_STARTED` para inmovilizar la calibración en el
primer intento. Luego `ATTEMPT_CLOSED` debe declarar como mínimo `attemptId`, `challengeId`,
`courseCohortId`, `learnerId` y la información necesaria para evaluar el uso del tutor. Si falta
un campo obligatorio, el consumidor rechaza el evento y debe alertar/direccionarlo a DLQ según la
política de plataforma.

### INT-002 — Resultado de evaluación de IA

**Dueños:** Tema 07 y `practice-service`.

Se debe acordar el payload de `SCORE_CALCULATED`: identificación del intento y cohorte,
estado, score agregado, cinco dimensiones, versiones de rúbrica y calibración aplicadas, y motivo
tipado en caso de fallo o diferimiento. Tema 07 no emite XP ni nota académica.

### INT-003 — Solución esperada para anti-fuga

**Dueño:** `practice-service` / Tema 05.

La solución esperada viaja solo dentro del llamado M2M al tutor propuesto en OpenAPI. Se usa en
memoria para comparar la salida, no se incluye en prompts, auditoría, persistencia, respuesta HTTP
ni eventos. Practice debe aprobar que puede aportarla de forma segura en esa invocación.

### INT-004 — Pertenencia docente de cohorte

**Dueño:** `courses-service`.

Falta aprobar la ruta, autenticación y respuesta de pertenencia docente propuesta. Tema 07 necesita
como mínimo el rol y el estado de la matrícula para autorizar la administración de rúbricas,
Golden Set y calibraciones de una cohorte.
