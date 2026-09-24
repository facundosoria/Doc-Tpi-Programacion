# Kafka — estándar de eventos de la plataforma

> **Fuente:** `KAFKA.pdf`, «Apache Kafka → Eventos que conectan servicios» (material de la cátedra, grupo de
> Notificaciones, ejemplos fechados 2026-09-19/20). El PDF no se versiona (`*.pdf` está en `.gitignore`); este
> documento transcribe lo que dice y separa **qué manda el PDF** de **qué conservamos nosotros**.
>
> **Reemplaza** al estándar anterior con `eventVersion` y `eventType` con guiones (retirado de
> `docs/contracts/` el 2026-09-22; ver [`registro/2026-09-20-estandar-kafka-del-pdf.md`](../registro/2026-09-20-estandar-kafka-del-pdf.md)
> para el motivo del cambio). Decisión registrada en
> [`../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md`](../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md).

Marcas usadas abajo: **[PDF]** lo exige el PDF · **[nuestro]** lo decidimos nosotros y el PDF no lo contradice ·
**[pendiente]** el PDF no lo define y no lo inventamos.

## 1. Vocabulario

| Concepto | Qué significa | En este proyecto |
|---|---|---|
| Emisor | El microservicio que genera un hecho y quiere comunicarlo | `llm-service` (publica `SCORE_CALCULATED`, `MESSAGE_UNBLOCKED`…) |
| Producer | El código que publica el evento en Kafka | `KafkaEventProducer` + `EventOutboxRelay` |
| Kafka | El intermediario que recibe, guarda y distribuye los eventos | el bus de la plataforma, `event-bus:29092` |
| Receptor | El microservicio que recibe un evento que le interesa | `llm-service` consume `ATTEMPT_CLOSED` |
| Consumer | El código que escucha y procesa los mensajes | `PracticeAttemptClosedListener` |

## 2. Contrato del evento (envelope) **[PDF]**

Todo lo emitido se publica **en inglés** y con este contrato. Los cinco campos son obligatorios:

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "SCORE_CALCULATED",
  "timestamp": "2026-09-19T23:53:00Z",
  "producer": "llm-service",
  "payload": {}
}
```

| Campo | Obligatorio | Función |
|---|---|---|
| `eventId` | Sí | Identifica unívocamente el evento (UUID) |
| `eventType` | Sí | Indica qué ocurrió |
| `timestamp` | Sí | Momento en que ocurrió (ISO-8601, UTC) |
| `producer` | Sí | Microservicio que produjo el evento |
| `payload` | Sí | Datos específicos del evento |

**No hay `eventVersion`.** El estándar anterior lo tenía; el del PDF no. Ver §11 para qué hacemos ante un cambio.

> ⚠️ **`timestamp`: número o texto [pendiente].** El ejemplo del PDF muestra `"2026-09-19T23:53:00Z"`, pero la
> clase `Event<T>` del propio PDF tiene `Instant timestamp` y `JsonSerializer` de Spring Kafka escribe un
> `Instant` **como número** (verificado con spring-kafka 3.3.6: `"timestamp":1789914600.000000000`). Quien siga el
> PDF al pie de la letra publica un número, no el texto ISO-8601. Para publicar el texto: `@JsonFormat(shape =
> JsonFormat.Shape.STRING)` sobre el campo, o crear el serializador con un `ObjectMapper` que tenga
> `WRITE_DATES_AS_TIMESTAMPS` desactivado. `llm-service` publica **texto ISO-8601 en UTC** y **acepta ambas
> formas** al consumir; se pregunta a Notificaciones cuál es la oficial (§12).

> ⚠️ **`producer`: valor a confirmar [pendiente].** El PDF usa `challenges-service` en un ejemplo y
> `tema-XX-service-name` en la plantilla. Hoy publicamos `llm-service` (el `spring.application.name`).

## 3. `eventType` **[PDF]**

- MAYÚSCULAS con guion bajo: `CHALLENGE_COMPLETED`, y en este proyecto `ATTEMPT_CLOSED`, `SCORE_CALCULATED`,
  `SCORE_DEFERRED`, `MESSAGE_UNBLOCKED`.
- Nombra un **hecho que ya ocurrió**, no una orden.
- Una vez publicado, se mantiene estable.

## 4. Tópicos

- El PDF agrupa los tópicos en cinco dominios: **Product Domains**, **Notifications and LLMs**, **Marketplace**,
  **Sandbox**, **Security and Audit**. Ejemplo de nombre: `challenges.results`. **[PDF]**
- **No se permite a los grupos crear tópicos.** Si necesitan uno, avisan al grupo de Notificaciones. **[PDF]**
- **[pendiente]** El PDF no trae la lista de nombres definitivos. Los que usa `llm-service` son **provisorios, a
  asignar por Notificaciones**:

| Nombre provisorio | Quién publica | Quién consume | Estado |
|---|---|---|---|
| `practice-events` | practice-service (Tema 05) | `llm-service` | 🔴 a asignar |
| `evaluation-events` | `llm-service` | practice-service (Tema 05) | 🔴 a asignar |
| `moderation-events` | `llm-service` | notifications-service / chat | 🔴 a asignar |
| `calibration-events`, `course-events`, `llm-config-events` | ver AsyncAPI | ver AsyncAPI | 🔴 a asignar |

- **Dead-letter:** como no podemos crear tópicos, ya **no** se publica a `<tópico>.dlt`. Los mensajes rechazados
  quedan en la tabla `event_dead_letter` de `llm-service` **[nuestro]**.

## 5. Conexión al bus y configuración **[PDF]**

Los dos microservicios (emisor y receptor) usan **Spring for Apache Kafka**:

```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

**Emisor** (`application.yml`):

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:event-bus:29092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Receptor**:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:event-bus:29092}
    consumer:
      group-id: notifications-service          # = nombre del propio servicio
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.*"
```

- El bus se llama `event-bus` y escucha en `29092` dentro de la red de Docker; la variable es `KAFKA_BOOTSTRAP`.
- El `group-id` es el nombre del servicio. En `llm-service` es `llm-service` (y `llm-service-shadow` en el modo shadow).
- Clase común del evento (del PDF): `Event<T>` con `eventId`, `eventType`, `timestamp` (`Instant`), `producer`,
  `payload` (`T`), constructor vacío y getters/setters. Cada `eventType` define su clase de payload
  (`ChallengeCompletedPayload`).

**Cómo lo hace `llm-service` [nuestro].** Serializa el envelope a JSON con Jackson y lo publica como `String`
(`StringSerializer`), leyéndolo del outbox. En el cable el resultado es el mismo JSON que produce `JsonSerializer`,
**salvo por un detalle**: `JsonSerializer` agrega el header `__TypeId__` con el nombre de la clase Java del
productor y nosotros **no lo emitimos** (acoplaría a los consumidores a nuestras clases). Consecuencia para los
consumidores que usan `JsonDeserializer`:

```yaml
spring.json.use.type.headers: false
spring.json.value.default.type: com.example.events.Event   # o su clase de evento
spring.json.trusted.packages: "com.example.*"
```

y un `ObjectMapper` con `JavaTimeModule` para que `timestamp` llegue como `Instant`.

## 6. Message Key **[nuestro]**

El PDF no habla de la key. La usamos para preservar el orden de los eventos de una misma entidad dentro de una
partición; ningún consumidor debe depender de ella:

| Tópico (provisorio) | Key |
|---|---|
| `evaluation-events` | `courseCohortId` |
| `calibration-events` | `rubricVersionId` |
| `moderation-events` | `courseId` |
| `practice-events` | la elige el productor (Tema 05); no dependemos de ella |

## 7. Un evento por hecho de negocio **[PDF]**

- No se publica un evento por consumidor si representan el mismo hecho.
- No se mete en el evento «todos los datos posibles por si algún microservicio los necesita».
- El evento lleva lo necesario para **describir el hecho** y que los consumidores reaccionen, no es un contenedor de
  datos.

## 8. Consumidores tipados **[PDF]**

El tipo del `@KafkaListener` depende de cómo se define el payload. Con un solo `eventType` por tópico:

```java
@KafkaListener(topics = "challenges.results", groupId = "ranking-service")
public void consume(Event<ChallengeCompletedPayload> event) { ... }
```

Cuando **un tópico mezcla varios `eventType` con payloads distintos** (es el caso de `evaluation-events`:
`SCORE_CALCULATED` y `SCORE_DEFERRED`), no se puede tipar con un único payload: se consume `Event<?>` (o
`JsonNode`) y se ramifica por `eventType`:

```java
if ("SCORE_CALCULATED".equals(event.getEventType())) { ... }
```

## 9. Correlación **[nuestro]**

`traceparent` y `X-Request-Id` viajan como **headers del mensaje Kafka**, nunca en el `payload`
([`request-correlation-across-http-and-kafka`](../../../.skill-hub/request-correlation-across-http-and-kafka.md)).
Además publicamos los headers `eventId` y `eventType` para filtrar sin deserializar. **No** publicamos
`eventVersion`.

## 10. Publicación y consumo confiables **[nuestro]**

- **Outbox transaccional:** el evento se guarda en `event_outbox` en la misma transacción que el cambio de estado y
  `EventOutboxRelay` lo publica. Nadie llama a `KafkaTemplate#send` desde lógica de negocio.
- **Entrega al menos una vez:** el bus puede reentregar. Todo consumidor deduplica por `eventId`
  (`kafka_consumed_events`).
- **Mensaje inválido** (JSON roto, sin `eventId`, payload incompleto): no bloquea la partición; queda en
  `event_dead_letter` con el motivo.
- Acks `all`; confirmar el offset después de procesar.

## 11. Versionado y evolución **[pendiente]**

Sin `eventVersion`, el PDF no define qué hacer ante un cambio incompatible. Hasta que el grupo de Notificaciones
lo defina:

- Solo cambios **compatibles** (agregar campos opcionales al `payload`); los consumidores **ignoran campos
  desconocidos**.
- Un cambio incompatible (renombrar, quitar, cambiar el tipo de un campo) se **avisa antes por escrito** a los
  consumidores y se coordina; no se hace unilateralmente.
- No se agregan ni se renombran campos del envelope.

## 12. Puntos abiertos con Notificaciones

Están en [`../07-planificacion-y-trabajo-equipo/11-equipos/notifications-service/pendientes.md`](../07-planificacion-y-trabajo-equipo/11-equipos/notifications-service/pendientes.md):

1. Nombres definitivos de tópico para `llm-service`.
2. Si habrá un tópico de dead-letter y cómo se llama.
3. Política de versionado de eventos (§11).
4. Valor de `producer` (§2).
5. Forma oficial de `timestamp`: texto ISO-8601 (como el ejemplo del PDF) o número (lo que produce `JsonSerializer` con
   un `Instant`).

## 13. Reglas para asistentes y agentes de IA

**Deben:** usar exactamente los cinco campos del envelope; escribir `eventType` en `MAYÚSCULAS_CON_GUION_BAJO`;
publicar todo en inglés; leer el bus de `KAFKA_BOOTSTRAP` con default `event-bus:29092`; usar el nombre del servicio
como `group-id`.

**No deben:** inventar tópicos ni nombres definitivos (los marcados 🔴 son provisorios); agregar `eventVersion`;
crear tópicos (ni `.dlt`); meter datos «por si acaso» en un evento; poner ids de traza en el `payload`; inventar
contratos marcados como pendientes: se pide la decisión al equipo.
