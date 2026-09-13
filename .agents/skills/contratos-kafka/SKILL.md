---
name: contratos-kafka
description: >-
  Referencia para escribir los contratos de eventos asíncronos de un
  microservicio de forma que cumplan con el bus de mensajería de la plataforma
  (Kafka): nombre de topic <evento>.v<major>, el envelope común (eventId,
  version, occurredAt, producer, data), la correlación en headers de Kafka
  (traceparent + X-Request-Id, nunca en el payload), entrega at-least-once con
  consumidores idempotentes (deduplicación por eventId), el patrón outbox para
  publicar dentro de la transacción, y el versionado de eventos. Consultá esto
  ANTES de definir, publicar o consumir un evento, o de escribir el AsyncAPI.
  Las reglas del bus son del equipo de mensajería/eventos de la plataforma:
  acá va el resumen, la fuente autoritativa es su documentación.
---

# Contratos de eventos (Kafka) — referencia

**Esto es una referencia, no un generador.** Te dice **las reglas fijas** que un contrato
de evento tiene que cumplir para viajar por el bus de la plataforma, y te da el
**esqueleto AsyncAPI** a rellenar. Qué eventos publica o consume cada servicio lo define
su equipo; esta referencia dice *con qué forma*.

> **De quién es cada cosa.** El **contrato de eventos de la plataforma** (nombres de
> topic, envelope, garantías de entrega, headers de correlación) lo define el **equipo de
> mensajería/eventos**, no el equipo del servicio. Acá va el resumen; la fuente
> autoritativa es su documentación. No se modifica desde acá. El estilo del payload de
> *tus* eventos (qué campos, qué enums) es tuyo.
>
> Es el par asíncrono de [`contratos-api-gateway`](../contratos-api-gateway/SKILL.md)
> (sincrónico). **Publicar un evento no es hacer un POST a otro microservicio:** son
> canales distintos, con garantías distintas.

## Cuándo consultarla

Antes de:

- definir un **evento nuevo** que tu servicio publica,
- **consumir** un evento de otro equipo,
- escribir o cambiar el **AsyncAPI** del servicio,
- decidir cómo se propaga la correlación en un evento, o cómo se hace idempotente un
  consumidor.

## Qué hay acá

| Archivo | Para qué |
|---|---|
| [`references/convenciones-kafka.md`](references/convenciones-kafka.md) | Las reglas: nombre de topic, envelope común, correlación en headers, at-least-once + consumidor idempotente, outbox, versionado de eventos, qué NO hacer. Marca cuáles son de plataforma. |
| [`references/plantilla-asyncapi.yaml`](references/plantilla-asyncapi.yaml) | Esqueleto AsyncAPI 3.0 con el envelope común. Rellenar con los eventos del servicio. |
| [`references/ejemplo-evento.md`](references/ejemplo-evento.md) | Un evento publicado y uno consumido, resueltos, con notas de por qué quedan así. |

## Cómo se usa

1. **Leé `references/convenciones-kafka.md`** entero la primera vez. Es corto.
2. Para el **AsyncAPI**: copiá `plantilla-asyncapi.yaml`, reemplazá `<n>` por el nombre
   del servicio. **Un canal por evento**, address `= <evento>.v<major>`
   (`intento_cerrado.v1`). `operations` con `action: send` (publica) / `action: receive`
   (consume).
3. **Envelope común** en todos: `eventId` (UUID — sirve de idempotencia), `version`,
   `occurredAt`, `producer`, `data`. El contenido propio del evento va en `data`.
4. **La correlación va en headers de Kafka** (`traceparent`, `X-Request-Id`), nunca en el
   payload. Un servicio que consume un evento y publica otro **propaga** los mismos
   headers.
5. Para **consumir**: el consumidor es **idempotente** — procesar dos veces el mismo
   `eventId` no duplica efectos (índice de deduplicación por `eventId`).
6. Para **publicar**: usá el patrón **outbox** — el evento se escribe en la misma
   transacción que el cambio de estado que lo origina, y un relay lo publica después. No
   se hace `publish()` a mano en medio de la lógica.
7. El **doc inter-equipos** (quién publica qué, quién consume qué, con qué estructura
   mínima) vive en [`contratos-api-gateway/references/plantilla-contratos-inter-equipos.md`](../contratos-api-gateway/references/plantilla-contratos-inter-equipos.md)
   — cubre los dos canales; completá la sección de eventos ahí.

## Reglas de oro (resumen de la referencia)

- **Publicar un evento ≠ POST a otro servicio.** Kafka es el bus; el Gateway es para HTTP.
- **Un canal/topic por evento**, con sufijo de versión mayor: `intento_cerrado.v1`.
- **Envelope común** con `eventId` (idempotencia), `version`, `occurredAt`, `producer`,
  `data`.
- **Correlación en headers de Kafka**, no en el payload. Se propaga aguas abajo.
- **At-least-once:** el consumidor es idempotente; dedup por `eventId`.
- **Outbox:** publicar en la misma transacción que el cambio de estado.
- **Versionado:** un cambio incompatible del payload → `<evento>.v2`, con los dos topics
  conviviendo hasta que todos los consumidores migren. El `.v1` no se cambia en
  destructivo.
- Pedí los campos que necesitás de un evento ajeno **antes** de que el equipo dueño cierre
  su contrato. Después es renegociar con varios equipos.
- **No modifiques** el contrato de eventos de otros equipos: documentá lo que necesitás
  en el doc inter-equipos, con dueño y fecha.

## Checklist de un contrato de eventos terminado

- [ ] AsyncAPI: un canal por evento con sufijo de versión; `operations` send/receive;
      envelope común; **valida** contra su esquema (linter en CI); sin `<placeholders>`.
- [ ] Correlación en headers de Kafka, no en el payload.
- [ ] Cada consumidor documentado como idempotente (dedup por `eventId`).
- [ ] Publicación por outbox, no `publish()` suelto.
- [ ] Estructura mínima de cada evento consumido, listada en el doc inter-equipos, con
      los campos obligatorios y por qué.
- [ ] Eventos ajenos pendientes de acordar, marcados 🔴 con dueño y fecha.
- [ ] Nada inventado: los eventos vienen del alcance del servicio.

## Relación con el resto del bundle

- `generar-vision-y-alcance` da las **funciones** → de ahí salen los eventos que se
  publican/consumen.
- `contratos-api-gateway` es el par sincrónico.
- `generar-historias-usuario` cita los eventos en *Dependencias*.
- `generar-backlog-y-recetas` marca en cada receta qué evento se implementa.

Para el skill hub, esto iría como `type: reference`, owned por el equipo de mensajería.
