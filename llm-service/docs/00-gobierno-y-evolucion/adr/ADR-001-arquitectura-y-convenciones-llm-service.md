# ADR-001 — Arquitectura y convenciones técnicas de `llm-service`

- **Estado:** Aceptado el 2026-09-19 por Facundo Soria (revisión y aprobación directa en la sesión de trabajo, sin PR formal). **Parcialmente superado el 2026-09-21 por [ADR-003](ADR-003-estructura-multimodulo-y-variables-de-entorno.md)** en §1, §2 y §4 (paquetes, regla de dependencias y variables tras la integración multi-módulo); §3 sigue vigente. El texto original no se edita.
- **Fecha:** 2026-09-18
- **Autores:** equipo G03 (pareja P1)
- **Historia:** LLM-EP01-H01

## Contexto

`llm-service` es un microservicio de la plataforma. Las parejas del equipo lo construyen en
paralelo, así que necesitan una estructura de paquetes, una regla de dependencias y una lista de
configuración compartidas y verificables.

## Decisión

### 1. Paquetes (raíz `ar.edu.utn.frc.tup.piv.llm`)

| Paquete | Responsabilidad |
|---|---|
| `api` | Controllers REST, DTOs, `ApiExceptionHandler` (Problem Details RFC 7807). |
| `application` | Casos de uso / servicios; orquestan dominio y puertos. |
| `domain` | Reglas de negocio puras y puertos (interfaces). Sin framework. |
| `infrastructure` | Adaptadores: persistencia JDBC, proveedores de IA, gateway, RAG. |
| `security` | Autorización por scopes/roles a partir de la identidad del Gateway. |
| `configuration` | Beans, filtros (`GatewayIdentityFilter`), `SecurityConfig`, propiedades. |
| `messaging.kafka` | Esqueleto de mensajería transversal (outbox, relay, consumidor idempotente); ver [ADR-002](ADR-002-mensajeria-kafka-outbox-y-dedup.md). |

El módulo `moderation` replica el mismo esquema (`api/application/domain/infrastructure`) dentro de su propio paquete.

### 2. Regla de dependencias

`api → application → domain ← infrastructure`. `domain` **no** depende de `application`, `api`,
`infrastructure`, Spring, Kafka, JDBC/JPA ni SDKs de proveedores de IA. Se verifica con ArchUnit en
`src/test/java/.../architecture/ArchitectureTest.java`: un PR que viole la regla rompe `mvn test`
y no se puede mergear (CA4).

### 3. Identidad

El JWT lo valida el API Gateway. El servicio construye la autenticación **solo** desde los headers
`X-*` del Gateway (`GatewayIdentityFilter`). El puerto del servicio no se publica. El Bearer sin
firma solo se lee si `app.security.trust-unsigned-bearer=true` (desarrollo/tests; por defecto
`false`).

### 4. Variables de entorno admitidas

Toda variable nueva debe agregarse acá y a `.env.example` en el mismo PR; la revisión bloquea el PR si falta (CA5).

| Variable | Default | Uso |
|---|---|---|
| `SERVER_PORT` / `MANAGEMENT_PORT` | 8086 / 8087 | Puerto de la API / del actuator |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | ver `.env.example` | PostgreSQL |
| `LLM_CREDENTIALS_MASTER_KEY` | — (obligatoria en compose) | Clave AES-256 base64 para credenciales de proveedores |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Registro en discovery |
| `GATEWAY_URL` | `http://api-gateway:8080` | Llamadas micro-a-micro |
| `APP_CLIENT_ID` / `APP_CLIENT_SECRET` | `llm-service` / vacío | Credenciales M2M |
| `JWKS_URL` / `JWKS_REFRESH_MS` | users-service / 300000 | Estado del JWKS |
| `TRUST_UNSIGNED_BEARER` | `false` | Solo dev/tests (ver §3) |
| `GROQ_API_KEY` / `GROQ_MODEL` / `GROQ_BASE_URL` | vacío / ver `.env.example` | Proveedor LLM |
| `LLM_MODERATION_CONTEXTUAL_URL` / `_API_KEY` / `_MODEL` | — | Clasificador contextual de moderación |
| `KAFKA_BOOTSTRAP_SERVERS` / `LLM_KAFKA_ENABLED` / `LLM_KAFKA_OUTBOX_POLL_MS` | `localhost:9092` / `false` / `2000` | Broker Kafka; el relay y los consumidores solo corren con `LLM_KAFKA_ENABLED=true` (ver ADR-002) |
| `MODERATION_RETENTION_ENABLED` / `_CRON` / `_DAYS` / `_REVERSED_DAYS` / `_UNRESOLVED_TIMEOUT_DAYS` | ver `application.yml` | Retención de moderación |

## Consecuencias

- Las parejas arrancan con la misma estructura; las desviaciones se detectan en revisión.
- La independencia del dominio queda protegida por un test, no por convención.
- Kafka (H07) entró al stack el 2026-09-18 y se documenta en [ADR-002](ADR-002-mensajeria-kafka-outbox-y-dedup.md).
