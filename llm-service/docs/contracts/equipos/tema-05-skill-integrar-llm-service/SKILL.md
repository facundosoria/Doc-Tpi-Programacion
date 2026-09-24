---
name: integrar-llm-service
description: Integra practice-service (Tema 05, Desafíos Prácticos) con llm-service. Arma el cliente del tutor de IA que se llama por el API Gateway con un token de servicio, y la publicación y el consumo de los eventos Kafka ATTEMPT_CLOSED, SCORE_CALCULATED y SCORE_DEFERRED del evaluador. Usar cuando se pida conectar, probar o depurar la integración con llm-service, el tutor o el evaluador.
---

# Integrar practice-service con llm-service

`llm-service` le ofrece a este servicio dos cosas: un **tutor** de IA (HTTP, por el API Gateway) y un
**evaluador** (Kafka: se publica el cierre de cada intento y vuelve un puntaje). Al principio responde un
**bot de prueba** y después un modelo real; **el contrato es el mismo en las dos fases**.

## Antes de empezar

1. **Leé completa la guía `llm-service-contrato-para-desafios-practicos.md`**, que tiene que estar en el mismo
   directorio que este skill. Es la única fuente de verdad: campos, códigos, eventos y los contratos
   ejecutables (Anexo A: OpenAPI del tutor; Anexo B: AsyncAPI de los eventos).
2. **No inventes** campos, rutas, topics ni códigos. Si algo no está en la guía, preguntá.
3. **No decidas solo los puntos abiertos** (sección 10 de la guía y las notas "a confirmar"): dejalos como
   propiedades configurables y avisá al final cuáles quedaron así.
4. La guía asume **Java con Spring** (`RestClient`, Spring Kafka). Si este repo usa otro stack, traducí lo
   mismo respetando el contrato.

## Qué construir

| Pieza | Qué hace | Dónde está en la guía |
|---|---|---|
| Configuración | `gateway-url`, `client-id`, `client-secret` (variable de entorno, nunca en el repo), nombres de topic **provisorios** (los asigna Notificaciones; no crear tópicos), `bootstrap-servers: ${KAFKA_BOOTSTRAP:event-bus:29092}`, `group-id` = nombre del servicio | Secciones 2 y 6 |
| Cliente de token | Pide un token `client_credentials` con `audience: llm-service` y scope `llm.tutor.interact`; lo reutiliza hasta que venza | Sección 2, paso 2 |
| Cliente del tutor | `POST /api/llm/tutor/interactions` por el Gateway; una `Idempotency-Key` **nueva por cada mensaje**; timeout de cliente de 30 s | Secciones 2 y 5, Anexo A |
| Manejo de respuestas | `completed`: mostrar el mensaje. `unavailable`: pantalla de "tutor no disponible", y para reintentar usar una clave nueva. `blocked`: hoy no llega, pero hay que manejarlo (por ejemplo, tratarlo como no disponible hasta que se defina otra cosa) | Sección 5 |
| Manejo de errores | `401` scope o servicio incorrecto; `403` falta la identidad delegada; `409` misma clave con la primera solicitud aún en curso; `422` cuerpo inválido o clave reutilizada con otro cuerpo. Cualquier otro `4xx`/`5xx`, tratarlo como fallo | Sección 5 |
| Publicador de `ATTEMPT_CLOSED` | Envelope de **cinco campos** (`eventId`, `eventType`, `timestamp`, `producer`, `payload`), **sin `eventVersion`**; `timestamp` como texto ISO-8601 (ojo: `JsonSerializer` escribe un `Instant` como número); `eventId` único y **el mismo si se reintenta el envío**; `transcript` completo con `{role, content}`; conviene guardarlo primero en la base y publicarlo después (*outbox*) | Sección 6, Anexo B |
| Consumidor de `evaluation-events` | `group-id` propio y estable; el tópico mezcla dos payloads, así que consumir `Event<?>`/`JsonNode` y **ramificar por `eventType`** (`SCORE_CALCULATED`, `SCORE_DEFERRED`); si usan `JsonDeserializer`, `spring.json.use.type.headers: false` y tipo por defecto (no mandamos `__TypeId__`); deduplicar por `eventId`; si hay varios para un `attemptId`, quedarse con el más reciente; **reenviar el score a Tema 03** por el mecanismo que acuerden | Sección 6 ("Cómo funciona y cómo conectarse") |

## Reglas que no se negocian

- **Siempre por el Gateway**, nunca al servicio directo (solo en pruebas locales, sección 11 de la guía).
- **No mandes ni confíes en headers `X-*`**: los agrega el Gateway.
- **No loguees** tokens, `clientSecret` ni `expectedSolution`. `expectedSolution` (la solución esperada del
  desafío, opcional) no se persiste en ningún lado.
- **Ignorá los campos desconocidos** en respuestas y eventos: pueden agregarse campos sin romper.
- **El score no se espera en la misma llamada**: llega después por Kafka.

## Cómo verificar (contra el bot de prueba)

Escribí una prueba por cada caso. El texto del bot es de plantilla y no tiene relación con el mensaje del
alumno: se valida la **forma**, no el contenido.

| Caso | Resultado esperado |
|---|---|
| Mensaje normal | `200`, `state: completed`, `conversacionId` presente |
| Misma `Idempotency-Key` y mismo cuerpo | `200`, respuesta idéntica |
| Misma clave con otro cuerpo | `422` |
| Sin el scope correcto | `401` |
| Sin identidad delegada | `403` |
| `riskLevel` inválido o campo obligatorio ausente | `422` |
| `state: unavailable` | El bot no lo produce: pedirle al equipo de `llm-service` que fuerce el fallo |
| `ATTEMPT_CLOSED` válido | Llega un `SCORE_CALCULATED` en `evaluation-events`, con la cohorte como key |
| El mismo `eventId` publicado dos veces | Un solo score |
| `ATTEMPT_CLOSED` sin `attemptId` o con `transcript` que no es un array | No genera score; queda en la tabla `event_dead_letter` de `llm-service` (no hay tópico `.dlt`) |

## Al terminar, informá

- Qué se implementó, qué pruebas pasan y contra qué ambiente (Gateway o local).
- Qué puntos abiertos de la guía quedaron como propiedades configurables y con qué valor por defecto.
- Los riesgos conocidos: si llega `403` "Identidad delegada ausente" con un token correcto (pendiente de
  confirmar con el equipo del Gateway), la ruta del pedido del token (confirmarla con `users-service`) y
  que los nombres de tópico son provisorios hasta que Notificaciones los asigne.
