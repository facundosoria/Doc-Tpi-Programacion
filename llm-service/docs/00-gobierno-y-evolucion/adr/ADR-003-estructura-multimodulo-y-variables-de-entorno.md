# ADR-003 — Estructura multi-módulo, fronteras de paquete y variables de entorno vigentes

- **Estado:** Aceptado el 2026-09-21 por Facundo Soria (revisión y aprobación directa en la sesión de trabajo, sin PR formal)
- **Fecha:** 2026-09-21
- **Autores:** equipo G03 (pareja P1)
- **Historia:** LLM-EP01-H01
- **Supersede parcialmente a:** [ADR-001](ADR-001-arquitectura-y-convenciones-llm-service.md) §1 (paquetes), §2 (regla de dependencias) y §4 (variables). Lo demás de ADR-001 (§3 identidad) sigue vigente.
- **Numeración:** este ID es local de la carpeta `adr/`; no es el `ADR-003` del
  [registro consolidado](../02-decisiones-y-pendientes.md).

## Contexto

ADR-001 (2026-09-18) describía el árbol plano `llm-service/src` con los paquetes `api`,
`application`, `domain`, `infrastructure`, `security` y `configuration`. El 2026-09-21 la
[integración `main` → `dev`](../../registro/2026-09-21-integracion-main-a-dev.md) adoptó el reactor Maven
multi-módulo de `main` con un SPI de proveedores. Esos seis paquetes ya no describen el código:
`api/` y `security/` no existen en la raíz y hay variables de entorno nuevas. Una ficha de H01 dice
que un ADR no se edita: se supersede.

## Decisión

### 1. Módulos Maven

`pom.xml` raíz es un reactor con cinco módulos: `app` (Spring Boot ejecutable), `provider-spi`
(puerto común de proveedores LLM) y `provider-openai-compatible`, `provider-anthropic`,
`provider-gemini` (un adaptador por proveedor). No hay código ejecutable en `llm-service/src`.
Un proveedor nuevo es un módulo `provider-*` que implementa el SPI; `app` no importa SDKs de
proveedor directamente.

### 2. Paquetes de `app` (raíz `ar.edu.utn.frc.tup.piv.llm`)

| Paquete | Responsabilidad |
|---|---|
| `adapter/in/web` | Controllers REST bajo `/api/llm/**`, `ApiExceptionHandler` (Problem Details RFC 7807) y `security/` (autorización por scopes desde la identidad del Gateway). |
| `adapter/out/{ai,http,persistence}` | Adaptadores de salida: proveedores de IA vía SPI, clientes HTTP (incluido courses-service) y repositorios JDBC. |
| `application` | Casos de uso (`service`), puertos (`port`), modelos y workers. |
| `domain` | Reglas de negocio puras. Sin framework. |
| `configuration` | Beans, `GatewayIdentityFilter`, `SecurityConfig`, propiedades. |
| `messaging/kafka` | Outbox transaccional, relay, consumidor idempotente y dead-letter (ver [ADR-002](ADR-002-mensajeria-kafka-outbox-y-dedup.md)). |
| `moderation/`, `shadow/` | Subsistemas verticales (EP-08 y shadow del evaluador) con sus propias capas internas. El código nuevo de cada uno va adentro de su vertical. |

`infrastructure/{agent,rag}` son 4 clases sueltas que la integración no reubicó: excepción, no patrón.

### 3. Regla de dependencias

`adapter/in → application → domain ← adapter/out`. `domain` **no** depende de `application`,
`adapter`, `infrastructure`, Spring, Kafka, JDBC/JPA ni SDKs de proveedores. Excepción documentada:
Jackson (`RealCaseAnonymizer`, `ModelResponseSchema`). Se verifica con ArchUnit en
`app/src/test/java/ar/edu/utn/frc/tup/piv/llm/architecture/ArchitectureTest.java`; una violación
rompe `mvn test` (CA4). `shadow` además no puede tocar `messaging`, Kafka ni los repositorios y
servicios de calibración.

**Deuda declarada:** diez controllers de `adapter/in/web` todavía toman tipos de
`adapter/out/persistence` (records anidados de los repositorios). Están exentos por nombre en
`webAdapterDoesNotDependOnPersistenceAdapter`; la lista solo puede achicarse.

### 4. Variables de entorno admitidas

Toda variable nueva debe agregarse acá y a `.env.example` en el mismo PR; la revisión bloquea el PR si falta (CA5).
Reemplaza la tabla de ADR-001 §4. **Obligatoria** = sin default en `application.yml`.

| Variable | Default | Uso |
|---|---|---|
| `SERVER_PORT` / `MANAGEMENT_PORT` | 8086 / 8087 | Puerto de la API / del actuator |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | ver `.env.example` | PostgreSQL |
| `LLM_CREDENTIALS_MASTER_KEY` | — (obligatoria en compose) | Clave AES-256 base64 para credenciales de proveedores |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Registro en discovery |
| `GATEWAY_URL` | `http://api-gateway:8080` | Llamadas micro-a-micro |
| `APP_CLIENT_ID` / `APP_CLIENT_SECRET` | `llm-service` / vacío | Credenciales M2M |
| `JWKS_URL` / `JWKS_REFRESH_MS` | users-service / 300000 | Estado del JWKS |
| `TRUST_UNSIGNED_BEARER` | `false` | Solo dev/tests (ver ADR-001 §3) |
| `LLM_COURSES_BASE_URL` | **obligatoria** (`http://localhost:4300` solo con perfil `workbench`) | Base del courses-service, por Gateway |
| `LLM_COURSES_SERVICE_TOKEN` | **obligatoria** (`local-gateway-m2m-token` solo con `workbench`) | Token M2M para courses-service |
| `GROQ_API_KEY` / `GROQ_MODEL` / `GROQ_BASE_URL` | vacío / ver `.env.example` | Proveedor LLM de prueba |
| `LLM_GATEWAY_MAX_ATTEMPTS` / `_BACKOFF_MS` | 3 / 200 | Reintentos hacia proveedores |
| `LLM_GATEWAY_BREAKER_MIN_CALLS` / `_FAILURE_RATE` / `_OPEN_WAIT_S` | 5 / 50 / 30 | Circuit breaker hacia proveedores |
| `LLM_EVALUATION_RUBRIC_VERSION_ID` / `_TIMEOUT_MS` / `_RETRY_AFTER_MINUTES` | `10000000-0000-0000-0000-000000000002` / 8000 / 30 | Evaluación de intentos |
| `LLM_MODERATION_CONTEXTUAL_URL` / `_API_KEY` / `_MODEL` | — | Clasificador contextual de moderación |
| `NOTIFICATIONS_ENABLED` / `NOTIFICATIONS_MODERATION_EVENTS_PATH` | `true` / `/api/notifications/v1/moderation-events` | Aviso de decisiones de moderación |
| `KAFKA_BOOTSTRAP` / `LLM_KAFKA_ENABLED` / `LLM_KAFKA_OUTBOX_POLL_MS` | `event-bus:29092` / `false` / `2000` | Broker Kafka; relay y consumidores solo corren con `LLM_KAFKA_ENABLED=true` (ADR-002) |
| `MODERATION_RETENTION_ENABLED` / `_CRON` / `_DAYS` / `_REVERSED_DAYS` / `_UNRESOLVED_TIMEOUT_DAYS` | ver `application.yml` | Retención de moderación |

## Consecuencias

- El ADR vuelve a describir el código. ADR-001 conserva su texto, con un encabezado que lo declara parcialmente superado.
- Sin `LLM_COURSES_BASE_URL` y `LLM_COURSES_SERVICE_TOKEN` (fuera del perfil `workbench`) el servicio no arranca: hay que declararlas en cualquier despliegue.
- La entrada ADR-019 del registro consolidado quedó histórica; esta decisión la reemplaza para `llm-service`.
