# Convenciones de eventos (Kafka) — estilo de la casa

Material fijo. Reglas que aplican a **todo** evento asíncrono de la plataforma. Las
secciones marcadas **[plataforma]** las define el equipo de mensajería/eventos, no el
equipo del servicio: acá va el resumen, la fuente autoritativa es su documentación. Las
marcadas **[casa]** son estilo propio para el payload de tus eventos.

---

## 1. Los dos canales **[plataforma]**

| Canal | Transporte | Contrato | Garantías |
|---|---|---|---|
| Sincrónico | HTTP por el API Gateway | OpenAPI v1 | request/response, `200/202/4xx/5xx` |
| Asincrónico | Kafka | AsyncAPI v1 | **publicar un evento no es un POST a otro micro** |

Publicar un evento y llamar a un endpoint son cosas distintas con garantías distintas. Si
necesitás una respuesta, es HTTP. Si notificás un hecho que ya pasó, es un evento.

## 2. Nombre de topic **[plataforma]**

`<evento>.v<major>` — snake_case, en pasado (es un hecho consumado):

- `intento_cerrado.v1`
- `score_de_ia_calculado.v1`
- `calibracion_aprobada.v1`

El sufijo `.v<major>` es la versión mayor del contrato del evento (ver §7).

## 3. Envelope común **[plataforma]**

Todo evento lleva la misma envoltura; el contenido propio va en `data`:

```yaml
eventId:    string (uuid)      # identidad del evento — sirve de idempotencia
version:    string             # "1.0" — versión del payload
occurredAt: string (date-time) # cuándo pasó el hecho
producer:   string             # qué servicio lo emitió
data:       object             # el contenido propio del evento
```

## 4. Correlación **[plataforma]**

- `traceparent` y `X-Request-Id` viajan en los **headers del mensaje Kafka**, **nunca**
  en el `data`.
- Un servicio que **consume** un evento y a raíz de eso **publica** otro, **propaga** los
  mismos headers de correlación.
- Los logs del consumidor incluyen `X-Request-Id` como campo, para seguir un flujo de
  punta a punta a través de HTTP y eventos.

## 5. Entrega at-least-once y consumidores idempotentes **[plataforma]**

- El bus garantiza **at-least-once**: un evento puede llegar más de una vez.
- Por eso **cada consumidor es idempotente**: procesar dos veces el mismo `eventId` no
  duplica efectos. Se implementa con un **índice de deduplicación** por `eventId` (o por
  `(consumidor, eventId)`).
- No se asume orden total entre topics distintos. Si el orden importa, va en el mismo
  topic con la misma clave de partición.

## 6. Publicación: patrón outbox **[casa / plataforma]**

- El evento se escribe en una tabla **outbox** en la **misma transacción** que el cambio
  de estado que lo origina.
- Un **relay** (o Debezium / connector) lee la outbox y publica en Kafka.
- **No** se llama `producer.send()` a mano en medio de la lógica de negocio: si la
  transacción hace rollback, el evento no debe existir; si commitea, el evento se publica
  sí o sí.

## 7. Versionado de eventos **[plataforma]**

- Cambio **compatible** (agregar un campo opcional a `data`): sube `version` (`1.0` →
  `1.1`), mismo topic.
- Cambio **incompatible** (renombrar / quitar / cambiar el tipo de un campo): topic
  nuevo `<evento>.v2`. Los dos topics **conviven** hasta que todos los consumidores
  migran. El `.v1` no se toca en destructivo.
- Un campo nuevo que otro equipo necesita se **acuerda antes** de publicarlo — no se
  agrega "por las dudas".

## 8. Estructura mínima de un evento que consumimos **[casa]**

Por cada evento ajeno que consumís, documentá en el doc inter-equipos:

- el `data` mínimo que necesitás y **por qué** (qué dispara en tu servicio),
- qué campos son **obligatorios** para vos,
- qué pasa si el evento llega sin esos campos (rechazo, dead-letter, alerta).

> Pedí estos campos **antes** de que el equipo dueño cierre su contrato de eventos.
> Después es renegociar con varios equipos a la vez.

## 9. Qué NO hacer

- No usar Kafka como si fuera HTTP (esperar una "respuesta" a un evento).
- No poner `trace_id` / `traceId` en el `data`: la correlación es de headers.
- No publicar sin outbox desde dentro de una transacción de negocio.
- No asumir exactly-once ni orden entre topics.
- No consumir sin deduplicación por `eventId`.
- No modificar el contrato de eventos de otro equipo: documentá lo que necesitás, con
  dueño y fecha.
- No inventar eventos que no existen ni están acordados.
