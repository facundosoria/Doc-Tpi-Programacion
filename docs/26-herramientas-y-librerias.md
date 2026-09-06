# 26 — Herramientas y librerías

> **Todo lo que se usa para construir, probar, operar y presentar el Tema 07, en una sola vista.**
> Para cada herramienta: qué es y de dónde viene (qué librería / binario / servicio), para qué la
> usamos, cómo, y en qué documento o ADR se decidió.

---

## Cómo leer este documento

Este es un **índice**, no la fuente de verdad de ninguna de las dos cosas que más cambian:

| Qué buscás | Dónde está de verdad |
|---|---|
| **Versión exacta** de una dependencia | El `pom.xml` de cada proyecto — [`codigo-ejemplo/ms-evaluacion-llm/pom.xml`](../codigo-ejemplo/ms-evaluacion-llm/pom.xml) para el servicio real |
| **El porqué** de cada elección | El ADR o la sección enlazada en la columna «Decidido en» |

**Columna «Estado»** en las tablas del servicio real:

| Marca | Significa |
|---|---|
| ✅ | Ya está en el `pom.xml` del esqueleto |
| 📋 | Decidido en un doc/ADR, todavía no agregado al esqueleto |
| 🧪 | Solo aparece en una demo, no en el servicio que se entrega |

> **El servicio de la cátedra es Java Spring Boot** (ADR-005 / [02 Parte 2](02-arquitectura-y-stack.md)).
> Casi todas las librerías de acá son del ecosistema Spring / Maven por esa decisión: la parte difícil
> del servicio es **integrarse** con Spring Cloud, no el machine learning.

---

> **Nomenclatura.** Las tablas de abajo nombran el esqueleto de ejemplo (`ms-evaluacion-llm`,
> rutas `/ai/*`, H2, puerto 8087, `trace_id`, `V4__llamadas_llm.sql`). La **identidad canónica**
> del servicio que se entrega es `llm-service` bajo `/api/llm/**`, PostgreSQL, correlación
> `traceparent` + `X-Request-Id` y esquema Flyway propio — ver
> [00 · §2–4](00-fuentes-de-verdad-y-convenciones.md) y
> [23 · §4](23-plan-construccion-producto-llm.md). Lo que no cambia con el renombre es **qué
> librería se usa y para qué**, que es lo que cataloga este documento.

# Parte 1 — El stack del servicio (esqueleto: `ms-evaluacion-llm` → producto: `llm-service`)

## 1. Framework y capa web

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Spring Boot 3.5** | `org.springframework.boot:spring-boot-starter-parent` | El framework del servicio | Parent POM; arranca el contenedor de aplicación, autoconfiguración, perfiles | ✅ | [ADR-005](08-decisiones-y-pendientes.md) |
| **Spring Web (MVC)** | `spring-boot-starter-web` | Los 6 endpoints REST `/ai/*` | `@RestController` en `controller/`; recibe del API Gateway, nunca del cliente directo | ✅ | [02 Parte 3](02-arquitectura-y-stack.md) |
| **Bean Validation** (Hibernate Validator) | `spring-boot-starter-validation` | Validar el cuerpo de cada request | `@NotNull` / `@Size` en los DTOs de `dto/request/` | ✅ | [02 Parte 2 §3](02-arquitectura-y-stack.md) |
| **Jackson** | Transitiva de `spring-boot-starter-web` | Serializar/deserializar JSON | Automático; DTOs planos (`record`) en `dto/` | ✅ | — |
| **Lombok** | `org.projectlombok:lombok` | Menos boilerplate (getters, builders) | `optional`, vía `annotationProcessorPaths` del compiler plugin | ✅ | — |
| **springdoc-openapi** | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | Swagger UI para probar a mano en la demo | Reemplaza el componente Angular durante las 4 semanas de demo | 📋 | [ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md) |

## 2. Datos y persistencia

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Spring Data JPA** | `spring-boot-starter-data-jpa` | Acceso a Postgres | Una interfaz `Repository` por entidad en `repository/`; queries complejas con `@Query` | ✅ | [ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md) |
| **PostgreSQL** (driver) | `org.postgresql:postgresql` | La base de datos de registro | Runtime scope; una base **propia y exclusiva** (regla de la cátedra) | ✅ | [12 §2](12-almacenamiento-e-ingesta.md) |
| **pgvector** | Extensión de Postgres (`CREATE EXTENSION vector;`) | Guardar y buscar embeddings del RAG en la misma base que su metadata | Columna `vector(1536)` en `chunks`; filtro por `curso_cohorte_id` + similitud en una sola consulta | 📋 | [ADR-004](08-decisiones-y-pendientes.md) · [12 §2](12-almacenamiento-e-ingesta.md) |
| **H2** | `com.h2database:h2` | Base en memoria para tests y para la demo | Runtime/test scope; **nunca en producción** | ✅ | [TESTING.md](../codigo-ejemplo/ms-evaluacion-llm/TESTING.md) |
| **Flyway** | `org.flywaydb:flyway-core` | Migraciones de esquema versionadas | Scripts `V1__…` … `V4__llamadas_llm.sql` en `resources/db/migration/`; nunca se editan una vez aplicados | 📋 | [ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md) |
| **HikariCP** | Transitiva de Spring Boot | Pool de conexiones | Configurado en `config/DataSourceConfig` | 📋 | — |

## 3. Cola interna, cuotas y caché

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Postgres** (`SKIP LOCKED`) | Ya está — la misma base del servicio | Cola de trabajos diferidos, sin componente nuevo | Una tabla `jobs` con prioridad; los workers hacen `SELECT … FOR UPDATE SKIP LOCKED` | 📋 | [06 "Con qué tecnología"](06-operacion-e-ingenieria.md) |
| **Spring Data Redis** | `spring-boot-starter-data-redis` | Cliente de Redis, si Postgres se queda corto | `RedisTemplate` en `config/RedisConfig` | 📋 | [12 §3](12-almacenamiento-e-ingesta.md) |
| **Redis** | Contenedor propio, con persistencia AOF | Cola con prioridades · contadores de cuota (RF-IA-22) · caché de retrieval | Una cola por tipo de job; contadores con TTL de 24 h | 📋 | [12 §3](12-almacenamiento-e-ingesta.md) · [06 Parte 1](06-operacion-e-ingenieria.md) |

> 🔄 **A 120 usuarios probablemente alcance con Postgres** — el análisis está en
> [06 Parte 2 §1](06-operacion-e-ingenieria.md). El bus de la plataforma es **Kafka** (Tema 11) y
> **no** se reusa para la cola interna: no tiene prioridades por mensaje ni DLQ nativa.

## 4. IA / LLM

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **langchain4j** | `dev.langchain4j:langchain4j` + un módulo por proveedor (`langchain4j-anthropic`, `langchain4j-open-ai`, `langchain4j-google-ai-gemini`) | Cliente de LLM: `ChatModel`, formato de mensajes, reintentos de transporte, tokens y motivo de corte | Vive **dentro** de cada adapter de `service/gateway/adapter/`, detrás de nuestra interfaz `LlmAdapter`. **No** se usan sus `AiServices`, tools ni memoria | 📋 | [ADR-016](08-decisiones-y-pendientes.md) · [02 Parte 2 §3](02-arquitectura-y-stack.md) |
| **Salida estructurada / JSON Schema** | langchain4j (`response format` / JSON Schema) + validación propia con Jackson | Que evaluador, corrector y generador devuelvan un objeto validable, no texto libre | El gateway valida la respuesta del modelo contra el schema antes de devolverla (paso 6 de 8) | 📋 | [02 Parte 1 §4](02-arquitectura-y-stack.md) · [ADR-016](08-decisiones-y-pendientes.md) |
| **Prompts en archivos** | `.txt` en `resources/prompts/<funcion>/` | Un solo criterio para todos los modelos (RF-IA-29) y evitar recompilar para ajustar un prompt | `system-v1.txt` + `user-v1.txt` por versión; la `prompt_version` se guarda con cada llamada | 📋 | [06 Parte 4 §2](06-operacion-e-ingenieria.md) |
| **Batch API** (del proveedor) | Anthropic / Google / OpenAI | −50 % de costo en las funciones asíncronas | Evaluador, corrector y generador encolan en modo Batch. Si langchain4j no expone el endpoint Batch de un proveedor, ese adapter usa `RestClient` directo (cláusula de revisión de ADR-016) | 📋 | [ADR-003](08-decisiones-y-pendientes.md) · [03 §1](03-modelos-costos-y-contexto.md) |
| **Prompt caching** (del proveedor) | Anthropic / Google / OpenAI | Cobrar barato el prefijo estable que se repite entre alumnos de una cohorte | Prefijo fijo (system prompt + rúbrica + contexto de curso) primero en el payload | 📋 | [03 §5](03-modelos-costos-y-contexto.md) |

> **El código nunca nombra un modelo.** Nombra una función (`evaluador`, `tutor`…), y una tabla en la
> base (`funcion_modelo_config`, editable por ADMIN) dice qué modelo le toca. Cambiar de modelo = editar
> una fila, sin deploy — [02 Parte 1 §4](02-arquitectura-y-stack.md), RF-IA-23/24.

### Proveedores de LLM (servicios externos)

| Proveedor | Modelo hoy (recomendación) | Para qué función | Decidido en |
|---|---|---|---|
| **Google Gemini** | 3.5 Flash-Lite | Tutor · Generador · descripción de imágenes en la ingesta | [03 §1](03-modelos-costos-y-contexto.md) |
| **Anthropic** | Claude Haiku 4.5 (+ Batch) | Evaluador · Corrector | [03 §1](03-modelos-costos-y-contexto.md) · [ADR-010](08-decisiones-y-pendientes.md) |
| **OpenAI** | `omni-moderation-latest` (gratis) | Residuo del moderador que la capa clásica no resuelve | [ADR-012](08-decisiones-y-pendientes.md) |
| **Groq** | `llama-3.3-70b-versatile` | Solo en las demos (API compatible con OpenAI) | [codigo-ejemplo/README](../codigo-ejemplo/README.md) |

## 5. RAG e ingesta de documentos (en Java)

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Apache Tika** | `org.apache.tika:tika-core` | Detectar el tipo real de archivo (no por extensión) | Primer paso de la ingesta | 📋 | [12 §9](12-almacenamiento-e-ingesta.md) |
| **Apache PDFBox** | `org.apache.pdfbox:pdfbox` | Extraer texto por página · contar caracteres (detectar escaneados) · renderizar página a imagen · extraer imágenes embebidas | Cubre **las cuatro** operaciones del pipeline de ingesta con una sola librería | 📋 (🧪 en la demo de Lara) | [12 §9](12-almacenamiento-e-ingesta.md) |
| **Apache POI** | `org.apache.poi:poi-ooxml` | Texto y estructura de DOCX / PPTX | Solo si aparece material que no sea PDF | 📋 | [12 §9](12-almacenamiento-e-ingesta.md) |
| **Tabula-java** | `technology.tabula:tabula` | Tablas con líneas en PDF | Para el resto de las tablas se usa un modelo multimodal | 📋 | [12 §9](12-almacenamiento-e-ingesta.md) |
| **Tess4J** (binding de Tesseract) | `net.sourceforge.tess4j:tess4j` | OCR de páginas escaneadas de puro texto | Requiere Tesseract instalado en el contenedor; casi siempre gana el multimodal | 📋 | [12 §9](12-almacenamiento-e-ingesta.md) |
| **Embeddings** | Interfaz `ProveedorEmbeddings` → API, o `DJL` local | Convertir cada chunk en un vector para buscar por significado | Se construye la **interfaz**, no el componente: hoy por API, DJL o un componente Python si algún día importa la soberanía | 📋 | [ADR-006](08-decisiones-y-pendientes.md) · [02 Parte 2 §6](02-arquitectura-y-stack.md) |
| **Comparador de código** | Interfaz `ComparadorDeCodigo` → `JavaParser` | Salvaguarda anti-fuga: comparar la respuesta del tutor contra la solución esperada por AST | `JavaParser` es mejor que `tree-sitter` **si los desafíos son en Java**; la interfaz deja la puerta abierta a un `HttpComparator` con `tree-sitter` | 📋 | [02 Parte 2 §3–6](02-arquitectura-y-stack.md) |

## 6. Resiliencia y rate limiting

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Resilience4j** | `io.github.resilience4j:resilience4j-spring-boot3` | Circuit breaker + retry + rate limiter hacia cada proveedor de LLM | `config/Resilience4jConfig`; el circuit breaker abre ante 429/5xx y dispara la escalera de degradación (RF-IA-27); el `RateLimiter` (token bucket) frena el spam por usuario en el moderador | 📋 | [02 Parte 2 §3](02-arquitectura-y-stack.md) · [11 §Patrones](11-glosario-y-metadata.md) · [04](04-funciones-de-ia.md) |
| **Bucket4j** | `com.bucket4j:bucket4j-core` | Rate limit por IP/usuario en el borde (capa 1 de 3) | ~15 req/min por usuario autenticado; token bucket en memoria o sobre Redis | 📋 | [19 §2 y §4A](19-modernizacion-seguridad-y-ratelimit-llm.md) |
| **Cuota por desafío** | Lógica propia sobre Redis | Límite pedagógico (RF-IA-22): ~10 consultas al tutor por ejercicio | `service/gateway/quota/`; rechaza **antes** de gastar un token | 📋 | [19 §2](19-modernizacion-seguridad-y-ratelimit-llm.md) |

> Las **tres capas** de rate limiting (borde con Bucket4j → negocio por desafío → salida a proveedor
> con Resilience4j) están desarrolladas en [19](19-modernizacion-seguridad-y-ratelimit-llm.md).

## 7. Integración con la plataforma (Tema 01)

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Spring Cloud Netflix Eureka Client** | `spring-cloud-starter-netflix-eureka-client` | Registrarse en el Service Discovery de la cátedra | `config/EurekaConfig`; alta al levantar, heartbeat 30 s; el estado lo delega a Actuator | 📋 | [gateway-y-discovery/02](gateway-y-discovery/02-service-discovery-eureka.md) |
| **Spring for Apache Kafka** | `org.springframework.kafka:spring-kafka` | Publicar y consumir los eventos de dominio del bus del Tema 11 (Kafka) | `event/publisher/` publica `score_de_ia_calculado`, `calibracion_aprobada`, `incidente_de_jailbreak`…; `event/consumer/` consume `intento_cerrado`, `curso_archivado`, `modelo_llm_cambiado`. Con el `trace_id` en cada evento | 📋 | [02 §6](02-arquitectura-y-stack.md) · [18](18-contratos-inter-equipos.md) |
| **Spring Boot Actuator** | `spring-boot-starter-actuator` | Sondas `readiness` / `liveness` y estado para Eureka | El proveedor de LLM **no** entra en la sonda de readiness | 📋 | [ADR-014](08-decisiones-y-pendientes.md) · [gateway-y-discovery/06](gateway-y-discovery/06-resiliencia-observabilidad-y-operacion.md) |
| **Spring Security** (token interno) | `spring-boot-starter-security` | Autenticación servicio-a-servicio por token técnico; leer el `trace_id` y la identidad del header que propaga el API Gateway | `config/SecurityConfig` + `config/WebConfig`; el servicio no se expone a internet | 📋 | [02 Parte 3](02-arquitectura-y-stack.md) · [gateway-y-discovery/04](gateway-y-discovery/04-seguridad-y-pipeline-de-filtros.md) |

> El **API Gateway** (Spring Cloud Gateway + WebFlux, RS256/JWKS), **Eureka Server** y el **broker
> Kafka** del bus son infraestructura del **Tema 01 / Tema 11**, no la operamos nosotros. La
> referencia del gateway está en [`docs/gateway-y-discovery/`](gateway-y-discovery/README.md).

## 8. Observabilidad

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **Micrometer** | Transitiva de Actuator | Métricas: latencia p50/p95/p99 por función, tasa de fallback, profundidad de cola, % de aciertos de caché, costo por curso | El tablero de 8 métricas de operación | 📋 | [06 Parte 2 §7](06-operacion-e-ingenieria.md) |
| **`trace_id` propagado** | Header del API Gateway | Cruzar el log de una llamada entre dos microservicios | Se lee del header, se pasa como parámetro a todo el service, se guarda en `llamadas_llm` y se devuelve en toda response | 📋 | [02 Parte 3](02-arquitectura-y-stack.md) · [11 Parte B](11-glosario-y-metadata.md) |
| **Tabla `llamadas_llm`** | Postgres (append-only) | Auditoría LLMOps: `model_id`, `model_version`, `prompt_version`, `rubric_version`, tokens, costo, latencia, incidentes | La escribe el gateway en **cada** llamada, incluso si falla (RF-IA-02/25/33) | 📋 | [ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md) |

## 9. Tests

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Estado | Decidido en |
|---|---|---|---|---|---|
| **JUnit 5 · Mockito · AssertJ** | `spring-boot-starter-test` | Tests unitarios de la lógica determinística (~80 % del servicio) | El LLM **siempre** detrás de una interfaz que se mockea con un JSON fijo | ✅ | [TESTING.md](../codigo-ejemplo/ms-evaluacion-llm/TESTING.md) · [06 Parte 4 §1](06-operacion-e-ingenieria.md) |
| **Testcontainers** | `org.testcontainers:*` | Tests de integración que levantan Postgres (y Kafka / Redis) reales | `@Tag("integracion")`; fuera del build diario, dentro de `-Pcompleto` | 📋 | [02 Parte 2 §3](02-arquitectura-y-stack.md) |
| **WireMock** | `org.wiremock:wiremock` | Simular la API HTTP del proveedor sin gastar plata | En los tests de los adapters | 📋 | [02 Parte 2 §3](02-arquitectura-y-stack.md) |
| **Fixtures** (`src/test/resources/fixtures/`) | Archivos JSON versionados | Respuestas fijas del modelo para probar el parseo y reproducir bugs | Un JSON por caso | 📋 | [ESTRUCTURA.md](../codigo-ejemplo/ms-evaluacion-llm/ESTRUCTURA.md) |
| Etiqueta **`@Tag("modelo-real")`** | JUnit 5 | Aislar los tests que **sí** pegan a la API real y gastan | Excluidos en los dos perfiles; se corren a mano: `./mvnw test -Dgroups=modelo-real -DexcludedGroups=` | ✅ | [TESTING.md](../codigo-ejemplo/ms-evaluacion-llm/TESTING.md) |

---

# Parte 2 — Build y análisis del código Java

Todo vía **Maven** (`./mvnw`, wrapper 3.9.x). Plugins declarados en el `pom.xml` del esqueleto —
[`ms-evaluacion-llm/pom.xml`](../codigo-ejemplo/ms-evaluacion-llm/pom.xml) — y corren con `./mvnw`:

| Herramienta | De dónde viene | Versión | Para qué | Cómo la usamos |
|---|---|---|---|---|
| **Spotless** + `palantir-java-format` | `com.diffplug.spotless:spotless-maven-plugin` | 2.44 | Formato uniforme del Java | `spotless:apply` corrige los archivos; `spotless:check` solo avisa |
| **PMD** | `org.apache.maven.plugins:maven-pmd-plugin` (7.x) | plugin 3.26 | Análisis estático: código muerto, variables sin usar, `catch` vacíos, comparar objetos con `==` | `failOnViolation=false`: reporta, no corta; reglas en [`pmd-ruleset.xml`](../codigo-ejemplo/ms-evaluacion-llm/pmd-ruleset.xml) |
| **CPD** | Viene dentro de PMD | 7.x | Bloques de código duplicados entre clases | `minimumTokens=60` (el default de 100 solo caza copias enormes) |
| **JaCoCo** | `org.jacoco:jacoco-maven-plugin` | 0.8.12 | Instrumentar y medir cobertura | Produce el reporte XML/HTML en `target/`; el umbral **no** se fija global (un % sobre todo el módulo premia testear getters) |
| **Surefire** | `maven-surefire-plugin` | (Spring Boot BOM) | Correr la suite | `testFailureIgnore=true` para que escriba los XML de reporte; `excludedGroups=integracion,modelo-real` por defecto |
| **spring-boot-maven-plugin** | `org.springframework.boot` | (BOM) | Empaquetar el jar ejecutable y `spring-boot:run` | — |

**Perfil `completo`** (`-Pcompleto`): suma los tests `@Tag("integracion")`; **mantiene** excluidos los
`modelo-real`.

---

# Parte 3 — Infraestructura y entorno de ejecución

| Herramienta | De dónde viene | Para qué | Cómo la usamos | Decidido en |
|---|---|---|---|---|
| **Docker** + **Docker Compose** | Docker Engine | Levantar el servicio y sus dependencias con un comando | `docker compose up`; escalar el pico es `--scale worker=6` (misma imagen, distinto comando) | [02 §10](02-arquitectura-y-stack.md) |
| **Build multietapa** (Dockerfile) | Docker | Compilar en una etapa, copiar solo el jar a la final | Pendiente: el Dockerfile del servicio todavía no existe | [11 §Infraestructura](11-glosario-y-metadata.md) |
| **nginx** | Contenedor, en el borde | Servir el Angular compilado y hacer de reverse proxy de `/api`; termina TLS | Está **antes** del API Gateway, no lo reemplaza | [ADR-015](08-decisiones-y-pendientes.md) · [15](15-sincronizacion-arquitectura-y-despliegue.md) |
| **PostgreSQL + pgvector** | Contenedor propio | La única base de datos del servicio — y la cola de trabajos (`SKIP LOCKED`) | Ver Parte 1 §2 y §3 | [12](12-almacenamiento-e-ingesta.md) |
| **Redis** | Contenedor propio (AOF) | Cuotas y caché; cola solo si Postgres se queda corto | Ver Parte 1 §3 | [12 §3](12-almacenamiento-e-ingesta.md) |
| **Kafka** (broker) | Infraestructura del Tema 11 — **no lo operamos** | El bus de eventos entre microservicios | `event/` publica y consume con `spring-kafka`; ver Parte 1 §7 | [02 §6](02-arquitectura-y-stack.md) |
| **MinIO** | Infraestructura compartida (compatible S3) | Guardar los PDF originales y las imágenes extraídas | **Solo si nadie más los tiene**; guardamos la referencia (bucket + key + hash), no el archivo. En la demo: carpeta local | [12 §4](12-almacenamiento-e-ingesta.md) |
| **Rolling update** | Estrategia de despliegue | Reemplazar instancias de a una; las dos versiones conviven | — | [ADR-013](08-decisiones-y-pendientes.md) |

---

# Parte 4 — Las demos y las presentaciones

## `demo/` — la suite de demostración del equipo

| Herramienta | De dónde viene | Para qué |
|---|---|---|
| **Docker Compose** | `demo/docker-compose.yml` | Levanta front + back con un comando (`run-demo.sh` / `run-demo.bat`) |
| **nginx:alpine** | imagen | Sirve la UI (`index.html` + `app.js` + `styles.css`), puerto 3000 |
| **`ms-evaluacion-llm`** (Spring Boot 3.5) | el esqueleto | Backend REST, puerto 8087, con `InputGuard` + `AntiLeakGuard` reales |
| **H2** | en memoria | Conversaciones y mensajes de la demo |
| **GroqAdapter** | mock / `llama-3.3-70b-versatile` | Responde sin key (modo simulación) o con `GROQ_API_KEY` real |

## `codigo-ejemplo/lara-heredia-demo-llm-spring-ai/` — prueba de concepto (importada, no se edita)

| Herramienta | De dónde viene | Para qué |
|---|---|---|
| **Spring Boot** | `spring-boot-starter-parent` 4.1.1 | Base de la demo |
| **Spring AI** | `org.springframework.ai:spring-ai-starter-model-openai` (BOM 2.0.1) | `ChatClient`, prompt de sistema, historial — apuntando a **Groq** por su API compatible con OpenAI |
| **springdoc-openapi** | `springdoc-openapi-starter-webmvc-ui` 3.1.0 | Swagger para probar a mano |
| **Apache PDFBox** | `org.apache.pdfbox:pdfbox` 3.0.1 | Extracción de texto de PDF |
| **H2** | en memoria | Conversaciones |

> 🔴 Trae una **API key de Groq hardcodeada** en `application.properties`. Se importó sin tocar; el
> arreglo está anotado en [`codigo-ejemplo/CORRECCIONES-SUGERIDAS.md`](../codigo-ejemplo/CORRECCIONES-SUGERIDAS.md).
> **Hay que rotarla en Groq**, no solo borrarla.

## `presentaciones/` — decks y wikis HTML

Se abren sin internet. Usan **Tailwind (Play CDN)**, **marked.js** (Markdown → HTML) y **Lucide**
(íconos) — vendorizados o por CDN según la corrección de [`presentaciones/CORRECCIONES-SUGERIDAS.md`](../presentaciones/CORRECCIONES-SUGERIDAS.md).
No son parte del build del servicio.

---

# Parte 5 — Lo que decidimos NO usar

| Tentación | Por qué no | Dónde está el fundamento |
|---|---|---|
| **Kubernetes / autoescalado / CDN** | 120 sesiones es poquísimo tráfico web; un contenedor de Spring Boot y uno de Postgres aguantan | [06 Parte 2 §1](06-operacion-e-ingenieria.md) |
| **Base vectorial dedicada** (Pinecone, Qdrant, Weaviate) | El corpus son miles de chunks, no millones; pgvector alcanza y filtra por curso + similitud en una consulta | [ADR-004](08-decisiones-y-pendientes.md) · [12 §6](12-almacenamiento-e-ingesta.md) |
| **MongoDB** para transcripciones | Se consultan de forma muy relacional y son producción académica: querés transacciones | [12 §6](12-almacenamiento-e-ingesta.md) |
| **Elasticsearch** | La búsqueda es semántica, no de texto completo | [12 §6](12-almacenamiento-e-ingesta.md) |
| **La capa de agentes de langchain4j** (`AiServices`, tools, memoria) o un framework tipo LangChain como base | Encapsula justo lo que hay que controlar y versionar: el prompt exacto. De langchain4j usamos **solo** el `ChatModel` y la salida estructurada (ADR-016), nada de agentes | [06 Parte 1 §6](06-operacion-e-ingenieria.md) · [ADR-002](08-decisiones-y-pendientes.md) |
| **Orquestador / router basado en LLM** | La ruta la sabe la UI; un router agrega latencia, costo, un punto de falla y una superficie de injection | [ADR-002](08-decisiones-y-pendientes.md) · [06 Parte 1](06-operacion-e-ingenieria.md) |
| **Python para el servicio** | La parte difícil es integrarse con Spring Cloud (no negociable de la cátedra); ser el único servicio Python entre doce se paga cada semana | [ADR-005](08-decisiones-y-pendientes.md) · [02 Parte 2](02-arquitectura-y-stack.md) |
| **Streaming token a token** en desafíos prácticos | No se puede bloquear una respuesta que el alumno ya está leyendo (RF-IA-20) | [ADR-009](08-decisiones-y-pendientes.md) |
| **Modelo local para el evaluador** | RF-IA-25: un único modelo activo, sin pool ni enrutamiento | [ADR-011](08-decisiones-y-pendientes.md) |

---

# Parte 6 — De dónde viene cada cosa, de un vistazo

| Canal | Herramientas |
|---|---|
| **Maven Central** (dependencias del servicio) | Spring Boot y starters (web, validation, data-jpa, data-redis, security, actuator), Spring Cloud Eureka Client, Spring for Apache Kafka, Resilience4j, Bucket4j, Jackson, Lombok, Flyway, driver de PostgreSQL, H2, langchain4j (+ módulo por proveedor), Tika, PDFBox, POI, Tabula-java, Tess4J, JavaParser, JUnit 5, Mockito, AssertJ, Testcontainers, WireMock, springdoc-openapi, Spring AI (solo demo de Lara) |
| **Plugins Maven** (build del esqueleto) | spotless-maven-plugin, maven-pmd-plugin (+ CPD), jacoco-maven-plugin, maven-surefire-plugin, spring-boot-maven-plugin |
| **Binarios del sistema** (en el contenedor del servicio) | Tesseract, para `Tess4J` (OCR de la ingesta) |
| **Imágenes de contenedor** | `postgres` (+ pgvector), `redis`, `nginx` / `nginx:alpine`, `minio/minio` |
| **Extensión de Postgres** | pgvector |
| **Infraestructura de plataforma** (Tema 01/11, no la operamos) | API Gateway (Spring Cloud Gateway), Eureka Server, broker **Kafka** del bus de eventos |
| **Servicios externos (SaaS)** | Anthropic API, Google Gemini API, OpenAI API (moderación), Groq API (demos) |

---

*Índice de herramientas. La verdad de las versiones vive en cada `pom.xml`; la del porqué, en los
ADR de [08](08-decisiones-y-pendientes.md) y los documentos enlazados.*
