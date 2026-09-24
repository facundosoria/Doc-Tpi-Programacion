# 04 — Seguridad y datos sensibles

## Identidad, autorización y trazabilidad

- HTTP usa JWT M2M con `aud=llm-service` y el scope específico de cada operación OpenAPI.
- El Gateway propaga la identidad delegada; Tema 07 valida que el docente pertenezca a la cohorte
  consultando al dueño de esa información.
- `traceparent` y `X-Request-Id` son headers obligatorios en HTTP y Kafka. No se ponen en los
  cuerpos de negocio.
- Toda escritura HTTP exige `Idempotency-Key`; los errores usan Problem Details RFC 7807.

## Datos que no se exponen

| Dato | Regla |
|---|---|
| Solución esperada | Solo llega M2M desde Practice y se usa en memoria por el guardarraíl. Nunca se incluye en prompt, log, auditoría, persistencia, respuesta o evento. |
| Clave de proveedor | `writeOnly`, cifrada en Tema 07, enmascarada al consultar. No pasa al frontend ni a Kafka. |
| Transcripción del tutor | Se minimiza al contexto necesario para evaluar; no contiene secretos. No ingresa a Golden Set sin anonimización y aprobación docente. |
| Identidad del alumno | Solo se usa donde el contrato la requiere para trazabilidad académica; no se entrega al proveedor si no es necesaria. |

## Respuesta segura del tutor

El tutor debe orientar, explicar y pedir razonamiento; no debe entregar una solución esperada ni
instrucciones para eludir las reglas. Un evento de incidente comunica únicamente metadatos de
seguridad definidos en AsyncAPI, nunca la solución ni el contenido sensible de la conversación.
