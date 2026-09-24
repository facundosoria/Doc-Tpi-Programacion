# Skill Hub — estado de lo subido (contratos con Tema 05)

Registro de lo que se hizo contra el Skill Hub para que los agentes de Tema 05 encuentren el contrato de
`llm-service`. Las entradas están en **inglés** (la guía del hub lo pide y el índice de búsqueda es en inglés).

> ⚠️ **2026-09-20 — el estándar Kafka cambió.** El PDF `KAFKA.pdf` (ADR-020) reemplazó el envelope con `eventVersion`
> y los `eventType` con guiones. **Las dos revisiones de `llm-service` para Kafka se enviaron y un admin las aceptó**
> (verificado con `get_skill` el 2026-09-20). El HTTP no cambia.

| Qué | Estado en el hub (verificado el 2026-09-20) | Archivo |
|---|---|---|
| Contrato `llm-service-http-contract` | **Publicado v4.** OpenAPI adjunto: 37.919 bytes, `sha256 e306008d…`, idéntico al del repo. **Sin cambios** (el PDF no toca HTTP) | [`pendiente-revision-llm-service-http-contract.md`](pendiente-revision-llm-service-http-contract.md) |
| Contrato `llm-service-kafka-contract` | **Publicado v5**, con el AsyncAPI v3.0.0 adjunto: 14.799 bytes, `sha256 65dc6ce3…` (**coincide con el repo**) | [`pendiente-revision-llm-service-kafka-contract.md`](pendiente-revision-llm-service-kafka-contract.md) |
| Skill `building-the-practice-service-tutor-client-and-score-consumer` | **Publicado v4**, con la guía v2 adjunta: 52.121 bytes, `sha256 25a18b11…` (**coincide con el repo**) | [`building-the-practice-service-tutor-client-and-score-consumer.md`](building-the-practice-service-tutor-client-and-score-consumer.md) |
| Regla `kafka-event-contract-rules` | **Ya no existe en el hub** (`get_skill` responde «No skill exists», 2026-09-20). No hay nada que revisar. El texto queda preparado por si se quiere proponerla de nuevo | [`revision-kafka-event-contract-rules.md`](revision-kafka-event-contract-rules.md) |
| Convención `request-correlation-across-http-and-kafka` | **Ya no existe en el hub** (no figura en `list_skills`). El espejo local de `.skill-hub/` quedó viejo | — |

**Hub al día.** Un agente de Tema 05 que use solo el hub ya ve el envelope de cinco campos. Si se edita el AsyncAPI o la guía del
repo, hay que enviar una revisión nueva y volver a comparar `sha256` y `size_bytes` con `get_skill`. **Queda pendiente** resincronizar
el espejo local `.skill-hub/` con `get_skill` (no editarlo a mano): sus copias de `llm-service-kafka-contract` y del skill de Tema 05
son las versiones viejas, y `kafka-event-contract-rules.md` y `request-correlation-across-http-and-kafka.md` son copias de entradas
que ya no existen en el hub.

Las entradas de plataforma que citan Kafka (`backend-service-integration`, `owning_team: platform`) solo remiten al skill
`contratos-kafka`, que tampoco está en el hub; no chocan con el estándar del PDF y no son nuestras.

**Nota:** los `pendiente-revision-*.md` conservan el texto de la primera revisión de cada contrato; el contenido
vigente es el que muestra `get_skill` en el hub.

## Cómo se llegó a enviar las revisiones

Al principio el hub rechazó `propose_revision` porque las dos entradas eran propuestas provisionales ("There is
nothing published to revise yet"). Después de que un admin las aceptó, las revisiones se enviaron y quedaron
como **versión 2 pendiente**: `get_skill` sigue devolviendo la v1 con `pending_revision: true` hasta que un
admin acepte o descarte la revisión.

Lo que hay hoy en esas entradas está desactualizado o no se pudo comprobar:

- **Kafka:** el YAML adjunto es una copia exacta del repo en el commit `aff07ba2`. Los scores tienen un
  `Envelope` genérico, sin los campos de `SCORE-CALCULATED` ni de `SCORE-DEFERRED`.
- **HTTP:** el YAML adjunto pesa 42.714 bytes y no coincide con ninguna versión del repo (29 a 38 KB). No se
  puede descargar sin la API key del MCP, así que no se sabe qué dice del tutor.

Por eso el skill que se subió lleva adentro el comportamiento esencial y le indica al agente que, si difieren,
manda la guía `../equipos/llm-service-contrato-para-desafios-practicos.md`.

## Cómo seguir

1. Un admin del hub acepta (o descarta) las tres revisiones de texto pendientes.
2. **Comprobar la integridad del adjunto**: el hub no devuelve el hash de una revisión pendiente. Una vez aceptada,
   `get_skill` muestra el `sha256` y el `size_bytes` del archivo publicado; tienen que ser iguales a los locales:
   `llm-service.openapi.yaml` 37.907 bytes, `sha256 9cc9eda98318570914040b341ee2aaa6e5366c1eecd7c2c0b05572589c9fc341`;
   `llm-service.asyncapi.yaml` 10.375 bytes, `sha256 1d6b1a6847be483d73e5dfd2fee8070a65b90229d2b1b196424c4d6f8a2c662d`.
   Los dos adjuntos se copiaron a mano al pedido; si no coinciden, hay que enviar una revisión nueva.
3. Si cambian los YAML del repo, hay que volver a revisar las entradas.

No se hizo, a propósito: proponer una entrada `contract` nueva con la guía completa adjunta. Duplicaría las dos
existentes (el hub rechaza lo parecido) y no se había acordado.

## Otras cosas que aparecieron

- El hub tiene un tipo **`contract`** propio, con archivo adjunto. Es la "parte contratos"; el tag `contracts`
  solo agrupa entradas de otros tipos.
- La entrada `kafka-event-contract-rules` (v3) describía topics por evento (`<event>.v<major>`) y un envelope con
  `version`, `occurredAt` y `data`. Ya hay una revisión preparada ([`revision-kafka-event-contract-rules.md`](revision-kafka-event-contract-rules.md)) con el
  estándar del PDF `KAFKA.pdf` (`../KAFKA_EVENT_STANDARD.md`, ADR-020).
- El detector de duplicados del hub compara el **título** con el texto de las entradas existentes. Un título
  como "Integrating practice-service with llm-service" repite casi palabra por palabra el `when_to_use` de
  `llm-service-http-contract`, y por MCP se rechaza sin permitir justificar. Por eso el skill se llama
  "Building the practice-service tutor client and score consumer".
