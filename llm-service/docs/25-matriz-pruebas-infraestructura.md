# 25 — Matriz de pruebas de infraestructura

Esta matriz complementa las pruebas unitarias y de interfaz del Sprint 1. Cada suite verifica una integración real y conserva evidencia en la ejecución de CI.

| Componente | Prueba | Estado en S1 | Comando / evidencia |
|---|---|---|---|
| PostgreSQL | Flyway aplica el esquema, rechaza puntajes incompletos y bloquea mutaciones de tablas append-only. | Implementada con Testcontainers. | `mvn test -Dintegration=true` |
| Docker Compose | Postgres queda saludable, `llm-service` migra la base y Actuator responde `UP`. | Implementada. | `./scripts/smoke-compose.sh` desde `llm-service` |
| Gateway | Ruta `/api/llm/**`, propagación de identidad y correlación, rechazo de headers falsos y errores de contrato. | Pendiente: no existe un módulo Gateway ejecutable en este repositorio. | WireMock + Compose cuando el módulo se entregue. |
| Eureka | Registro de `LLM-SERVICE`, resolución desde Gateway, dos instancias y continuidad durante una caída del registry. | Pendiente: no existe un servidor Eureka ejecutable en este repositorio. | Compose de integración con dos instancias. |
| Kafka | Publicación desde outbox, consumo idempotente, reintento y recuperación tras reinicio. | Diferida a S6, cuando haya eventos, workers y outbox. | Testcontainers Kafka + prueba de recuperación. |

## Criterios para Gateway y Eureka

Cuando estén implementados, la CI deberá ejecutar una suite que levante Eureka, Gateway, dos instancias de `llm-service` y un consumidor HTTP de prueba. Debe verificar:

1. `GET /api/llm/golden-sets` pasa por Gateway y conserva `X-Request-Id`.
2. Un cliente no puede inyectar `X-Service-Id`, `X-Service-Scopes` ni `X-Delegated-User`; esos datos sólo los incorpora el borde desde el token validado.
3. Una ruta fuera de `/api/llm/**` devuelve `404`.
4. Las solicitudes se distribuyen entre dos instancias registradas y continúan hacia la sana cuando una se detiene.
5. Con Eureka temporalmente detenido, Gateway mantiene rutas ya descubiertas durante la ventana de caché configurada.

La suite se incorporará junto con los módulos reales, no como simulación local del servicio de negocio.
