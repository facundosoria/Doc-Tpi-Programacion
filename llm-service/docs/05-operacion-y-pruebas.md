# 05 — Operación y pruebas

## Confiabilidad asíncrona

- Los eventos salientes se persisten con outbox y se publican al menos una vez.
- Cada consumidor deduplica por `eventId`; las escrituras e inicio de corridas usan
  `Idempotency-Key`.
- Un mensaje inválido o no procesable se identifica con `eventId` y `X-Request-Id` y sigue la
  política de reintento/DLQ de plataforma. Los valores concretos de retención y reintento deben
  acordarse con los dueños de Kafka.
- Un fallo temporal de proveedor genera una evaluación diferida, no una nota inventada ni una
  pérdida silenciosa del intento.

## Señales operativas mínimas

Se deben poder correlacionar: petición de tutor, hecho de cierre, evaluación, evento publicado y
llamada a proveedor. Las métricas mínimas son latencia/error por proveedor, evaluaciones pendientes,
fallos de calibración, rechazos anti-fuga y mensajes enviados a DLQ.

## Pruebas que deben existir antes de promover a Implementado

1. Validación sintáctica y de compatibilidad de OpenAPI/AsyncAPI.
2. Prueba de contrato productor-consumidor para cada evento recibido y publicado.
3. Reintento del mismo evento y de la misma escritura HTTP sin duplicar efecto.
4. Rúbrica editable en borrador, rechazo de edición publicada y pesos que suman 100 %.
5. No fuga de `expectedSolution` en respuesta, log, evento, auditoría o persistencia.
6. Diferimiento por proveedor no disponible y posterior resolución trazable.
