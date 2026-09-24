# Mensaje para Tema 05 — integración con `llm-service` (2026-09-20)

> Texto listo para pegar en el canal de Tema 05 (Desafíos Prácticos). Todo lo que necesitan está en el Skill Hub
> (skill `building-the-practice-service-tutor-client-and-score-consumer` v4 con la guía adjunta, y los contratos
> `llm-service-http-contract` y `llm-service-kafka-contract` v5); si difieren, manda la guía
> [`llm-service-contrato-para-desafios-practicos.md`](llm-service-contrato-para-desafios-practicos.md). Estado de las
> entradas: [`../skillhub/README.md`](../skillhub/README.md).

---

Hola equipo de Desafíos Prácticos. Todo lo que necesitan para integrarse con llm-service (tutor de IA y evaluador de intentos) está en el Skill Hub, no hace falta que les pasemos archivos.

**Cómo encontrarlo.** Busquen llm-service (las búsquedas funcionan mejor en inglés). Empiecen por el skill `building-the-practice-service-tutor-client-and-score-consumer`: trae el orden de armado, los casos de prueba y adjunta la guía completa (`llm-service-contrato-para-desafios-practicos.md`, en español), con el plan, la receta del token, el contrato y los puntos abiertos. Después miren los dos contratos: `llm-service-http-contract` (OpenAPI del tutor) y `llm-service-kafka-contract` (AsyncAPI de los eventos, **v5**). Si algo difiere, manda la guía.

**El plan.** Primero responde un bot de prueba, sin modelo real, para que verifiquen la conexión, los errores y los eventos. Después pasamos al modelo real y el contrato no cambia.

**Los eventos siguen el estándar Kafka de la cátedra (`KAFKA.pdf`).** El HTTP del tutor no cambia y los eventos se rigen por estas reglas:
- El envelope tiene **5 campos**: `eventId`, `eventType`, `timestamp`, `producer`, `payload`. **Ya no hay `eventVersion`.**
- Los tipos son `ATTEMPT_CLOSED`, `SCORE_CALCULATED` y `SCORE_DEFERRED` (con guion bajo).
- El bus es `event-bus:29092` (`KAFKA_BOOTSTRAP`) y el `group-id` es el nombre de su servicio.
- **Los tópicos no se crean, los asigna Notificaciones.** Usen `practice-events` y `evaluation-events` como nombres provisorios y configurables. Ya no hay tópico dead-letter.
- Ojo con tres cosas: `evaluation-events` mezcla dos payloads, así que ramifiquen por `eventType`. Nosotros no mandamos el header `__TypeId__`, así que con `JsonDeserializer` de Spring usen `spring.json.use.type.headers: false`. Y `JsonSerializer` escribe un `Instant` como número: usen `@JsonFormat(shape = STRING)` para publicar el texto ISO-8601.

**Lo que hoy bloquea las pruebas, y no depende de nosotros:** el alta de `practice-service` en la plataforma (scope `llm.tutor.interact`, allowlist del Gateway) y que Notificaciones asigne los tópicos definitivos.

**Lo que necesitamos de ustedes:** confirmar los puntos de la sección 10 de la guía. Son los nombres de tópico provisorios, qué `producer` usan y si su `timestamp` sale como texto o número, el payload del score, cómo arman el transcript, si mandan la solución esperada en `expectedSolution`, quién genera el evento del IDE y qué muestran cuando el tutor no está disponible.

**Dos cosas a confirmar con la plataforma:** la ruta del pedido del token y si el Gateway agrega la identidad delegada con un token de servicio.
