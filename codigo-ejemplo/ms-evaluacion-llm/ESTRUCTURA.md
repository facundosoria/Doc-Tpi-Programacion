# Estructura de carpetas — ms-evaluacion-llm

> Organización **por capas** del microservicio Java Spring Boot (Tema 07).
>
> Referencia de arquitectura: [02-arquitectura-y-stack.md](../../docs/02-arquitectura-y-stack.md)
> Referencia de integración: [17-mapa-de-integracion.md](../../docs/17-mapa-de-integracion.md)

---

## Por qué por capas y no por módulos

Somos **un solo microservicio**. Un paquete por capa (controller → service → repository) es la
convención de Spring Boot y lo que el equipo va a reconocer sin fricción.

Los ocho módulos del documento de arquitectura (M1–M8) no son ocho microservicios ni ocho paquetes
raíz: son **responsabilidades dentro de la capa de servicio**. Estar en paquetes separados dentro de
`service/` es suficiente para mantener su interfaz explícita sin romper la convención de capas.

```
controller   ←  recibe HTTP · valida entrada · delega al service · serializa respuesta
    ↓
service      ←  lógica de negocio · orquesta gateway, RAG, cola · nunca toca HTTP
    ↓
repository   ←  único punto de acceso a Postgres · Spring Data JPA
    ↑
entity       ←  las tablas como objetos Java · solo se usan en repository y service
```

`queue`, `event` y `config` son capas de soporte: no tienen acceso entre sí ni a `controller`.

---

## Árbol completo

```
src/
├── main/
│   ├── java/ar/edu/utn/frc/tup/piv/evaluacionllm/
│   │   │
│   │   ├── Application.java
│   │   │
│   │   ├── controller/                       ← CAPA 1 · Presentación
│   │   │
│   │   ├── dto/
│   │   │   ├── request/                      ← DTOs de entrada
│   │   │   └── response/                     ← DTOs de salida
│   │   │
│   │   ├── exception/                        ← Manejo global de errores
│   │   │
│   │   ├── service/                          ← CAPA 2 · Negocio (M1–M7)
│   │   │   ├── gateway/                      ← M1 · AI Gateway
│   │   │   │   ├── adapter/                  │   Adapters por proveedor LLM
│   │   │   │   ├── quota/                    │   Contadores de cuota
│   │   │   │   └── guard/                    │   Guardarraíles entrada/salida
│   │   │   ├── rag/                          ← M2 · RAG
│   │   │   ├── evaluacion/                   ← M3 · Evaluador
│   │   │   ├── calibracion/                  ← M4 · Calibración
│   │   │   ├── moderacion/                   ← M5 · Moderación de chat
│   │   │   ├── tutor/                        ← M6 · Tutor
│   │   │   ├── generacion/                   ← M7a · Generador de desafíos
│   │   │   └── correccion/                   ← M7b · Corrector
│   │   │
│   │   ├── repository/                       ← CAPA 3 · Acceso a datos
│   │   │
│   │   ├── entity/                           ← CAPA 4 · Entidades JPA
│   │   │
│   │   ├── queue/                            ← CAPA 5 · Cola interna Redis (M8)
│   │   │   ├── producer/
│   │   │   └── worker/
│   │   │
│   │   ├── event/                            ← CAPA 6 · Bus de eventos (M8)
│   │   │   ├── publisher/
│   │   │   └── consumer/
│   │   │
│   │   └── config/                           ← CAPA 7 · Configuración Spring (M8)
│   │
│   └── resources/
│       ├── application.yml
│       ├── contracts/                        ← OpenAPI por función
│       ├── prompts/                          ← Prompts versionados fuera del código
│       │   ├── evaluacion/
│       │   ├── correccion/
│       │   ├── generacion/
│       │   ├── tutor/
│       │   └── moderacion/
│       └── db/
│           └── migration/                    ← Scripts Flyway
│
└── test/
    └── java/ar/edu/utn/frc/tup/piv/evaluacionllm/
        ├── controller/
        ├── service/
        │   ├── gateway/
        │   ├── rag/
        │   ├── evaluacion/
        │   ├── calibracion/
        │   ├── moderacion/
        │   ├── tutor/
        │   ├── generacion/
        │   └── correccion/
        ├── repository/
        ├── queue/
        └── event/
```

---

## Detalle capa por capa

---

### `controller/` — Capa de presentación

**Qué va acá:** Las clases `@RestController` que exponen los 6 endpoints del contrato
([02](../../docs/02-arquitectura-y-stack.md) Parte 3). Nada más.

**Qué NO va acá:** lógica de negocio, llamadas a repositorios, acceso a Redis, nada de LLM.

**Comunicación:** Solo habla hacia abajo con `service/`. Recibe HTTP del API Gateway de la cátedra.
El cliente nunca llega directo (ADR-015): el flujo es `nginx → API Gateway → este controller`.

**Clases esperadas:**

| Clase | Endpoints que expone | Quién la llama |
|---|---|---|
| `AiController` | `POST /ai/{funcion}` · `GET /ai/jobs/{job_id}` | T03, T05, T11 (moderador), T12 |
| `IngestaController` | `POST /ai/ingesta` | Backoffice (T12) o proceso interno |
| `CalibracionController` | `POST /ai/calibracion` · `GET /ai/calibracion/{curso_cohorte_id}` | **T02 bloquea en este GET** |
| `PendientesController` | `GET /ai/pendientes/{curso_cohorte_id}` | T02 al cerrar un curso |

> **El `GET /ai/calibracion/{curso_cohorte_id}` es el endpoint más crítico para el resto de la plataforma.**
> Si no existe, el Tema 02 no puede activar ningún curso. Entregarlo como mock lo antes posible.

**Regla:** Los controllers reciben y devuelven **DTOs** (`dto/request/` y `dto/response/`),
nunca entidades JPA. La conversión la hace el service o un mapper.

---

### `dto/` — Objetos de transferencia

**Qué va acá:** Clases Java planas (`record` o POJO) que representan el cuerpo de cada
request y response del contrato OpenAPI. Se anotan con Bean Validation (`@NotNull`, `@Size`, etc.).

**Por qué están separados de `entity/`:** Las entidades JPA tienen anotaciones de Hibernate y
reflejan el esquema de base de datos. Los DTOs reflejan el contrato HTTP. Son cosas distintas y
cambiar una no debería forzar cambiar la otra.

**`dto/request/`** — lo que entra:

| Clase | Para qué |
|---|---|
| `AiRequest` | El sobre común: `contexto`, `payload`, `modo`, `idempotency_key` |
| `ContextoDto` | `curso_cohorte_id`, `intento_id`, `desafio_id`, `usuario_ref` |
| `IngestaRequest` | `curso_template_id`, `documento_ref`, `hash` |
| `CalibracionRequest` | `curso_cohorte_id`, `golden_set_version`, `model_id` |
| `PayloadEvaluacionDto` | Texto del alumno + referencia al desafío |
| `PayloadModeracionDto` | `mensaje`, `canal_ref`, `autor_ref`, `desafio_activo` |
| `PayloadTutorDto` | Pregunta del alumno + historial reciente |

**`dto/response/`** — lo que sale:

| Clase | Para qué |
|---|---|
| `AiResponse` | `resultado`, `trace_id`, `metadata` (sync) |
| `JobAceptadoResponse` | `job_id`, `estado: "pendiente"` (async 202) |
| `JobEstadoResponse` | `estado`, `resultado?`, `error?`, `posicion_en_cola?` |
| `CalibracionEstadoResponse` | `aprobada`, `desviacion_promedio`, `model_id`, `rubric_version`, etc. |
| `PendientesResponse` | `cantidad`, `mas_antiguo_desde` |
| `ErrorCuotaResponse` | `error: "cuota_agotada"`, `limite`, `reinicia_en` |
| `ErrorProveedorResponse` | `error: "proveedor_no_disponible"`, `degradacion` |

> **`trace_id` va en toda response.** Es lo único que sirve para depurar cuando algo falla
> entre dos microservicios. Lo propaga el API Gateway; el controller lo lee del header y lo
> reenvía al service como parámetro.

---

### `exception/` — Manejo global de errores

**Qué va acá:** Un `@RestControllerAdvice` que intercepta excepciones de servicio y las
traduce a respuestas HTTP con los códigos de error tipados del contrato.

**Por qué:** Los códigos de error tienen que ser estables (`cuota_agotada`,
`proveedor_no_disponible`, `calibracion_pendiente`). Si cada controller maneja sus propias
excepciones, termina habiendo seis formatos de error distintos.

**Clases esperadas:**

| Clase | Qué hace |
|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice` — mapea excepciones a HTTP |
| `CuotaAgotadaException` | Se lanza en `service/gateway/quota/` → devuelve 429 |
| `ProveedorNoDisponibleException` | Se lanza en `service/gateway/adapter/` → devuelve 503 |
| `JobNotFoundException` | Se lanza en `service/` al buscar un `job_id` inexistente → 404 |
| `IdempotencyConflictException` | La misma `idempotency_key` ya existe → 409 |

---

### `service/` — Capa de negocio (M1 a M7)

**Qué va acá:** Toda la lógica del negocio. Los services son interfaces con su implementación.
Nadie de afuera de `service/` sabe qué hay adentro: el controller llama a la interfaz.

**Comunicación:** Recibe calls de `controller/`, llama a `repository/`, `queue/producer/` y
`service/gateway/`. No toca HTTP ni serializa JSON.

#### `service/gateway/` — M1 · AI Gateway (la pieza más importante)

**Por qué es un sub-paquete y no una capa propia:** El gateway no es una capa de acceso a datos
ni de presentación. Es **infraestructura del canal de IA** que vive dentro de la capa de servicio.
Todo service que necesite un LLM llama a `LlmGateway` y no al proveedor directamente.

> **Regla absoluta:** ningún service (ni `evaluacion/`, ni `tutor/`, ni ninguno) llama a un
> proveedor de LLM por su cuenta. Todo pasa por `LlmGateway`. Es lo que hace posible
> RF-IA-23/24 (tabla función→modelo editable por ADMIN) y RF-IA-27 (degradación).

**`service/gateway/adapter/`** — Adapters por proveedor

| Clase | Qué hace |
|---|---|
| `LlmGateway` | Interfaz central: `llamar(funcion, prompt) → LlmRespuesta` |
| `LlmGatewayImpl` | Orquesta los 8 pasos (resolver función → cuota → guardarraíl → prompt → adapter → validar schema → anti-fuga → registrar) |
| `AnthropicAdapter` | Traduce `LlmRequest` al formato de Anthropic y la respuesta al formato interno |
| `OpenAiAdapter` | Ídem para OpenAI |
| `GroqAdapter` | Ídem para Groq |
| `GoogleAdapter` | Ídem para Google Gemini |
| `LlmRequest` / `LlmResponse` | DTOs internos del gateway (no son los del contrato HTTP) |
| `FuncionModeloConfig` | Lee la tabla `funcion → proveedor + modelo + versión` de la BD (RF-IA-23/24) |

**`service/gateway/quota/`** — Contadores (RF-IA-22)

| Clase | Qué hace |
|---|---|
| `QuotaService` | Verifica y decrementa el contador por `usuario_ref/día` y por `usuario_ref/desafio_id`. Lanza `CuotaAgotadaException` antes de gastar un token |
| `QuotaStore` | Escribe/lee los contadores en Redis (TTL de 24 hs) |

**`service/gateway/guard/`** — Guardarraíles (RF-IA-05/06/07/20)

| Clase | Qué hace |
|---|---|
| `InputGuard` | Guardarraíl de **entrada**: injection, perímetro temático, lenguaje ofensivo. Corre ANTES del adapter |
| `OutputAntiLeakGuard` | Guardarraíl de **salida**: similitud del output del tutor contra la solución esperada (PAR-11, RF-IA-20). Corre DESPUÉS del adapter. Si detecta fuga, pide regeneración |

> **`InputGuard` y `OutputAntiLeakGuard` son distintos** del `ModeracionService`: los guardarraíles
> del gateway son para las 5 funciones de IA; el moderador es específico del chat de Tema 11.

---

#### `service/rag/` — M2 · RAG

**Qué va acá:** Todo lo relacionado con el material del curso: bajar el PDF, partirlo en chunks,
generar los embeddings y buscar en pgvector.

**Comunicación:** `IngestaController` → `IngestaService` (en `service/rag/`) → `repository/` (escribe chunks + vectores).
El retrieval lo usan `TutorService`, `EvaluacionService` y `GeneracionService` llamando a `RetrievalService`.

| Clase | Qué hace |
|---|---|
| `IngestaService` | Orquesta la ingesta: descarga doc → extrae texto → chunkea → embede → persiste. Se ejecuta como job asíncrono (encola en `queue/producer/`) |
| `ChunkingService` | Parte el texto con solapamiento. Parámetros en `application.yml`, no hardcodeados |
| `EmbeddingService` | Genera el vector. Implementa la interfaz `ProveedorEmbeddings` (que permite cambiar de DJL a API sin tocar el servicio) |
| `RetrievalService` | Búsqueda semántica: toma una query, genera su embedding, consulta pgvector, devuelve los N chunks más relevantes del `curso_cohorte_id` correcto |

> **`curso_cohorte_id` va en cada chunk.** Si no está desde el principio, el retrieval no puede
> acotar la búsqueda al curso correcto y contamina resultados entre cursos.

---

#### `service/evaluacion/` — M3 · Evaluador

**Qué va acá:** El juez principal. Evalúa cómo el alumno usó la IA según la rúbrica versionada.
Es **asíncrono** (alta latencia, bajo volumen, alto riesgo si falla — modifica XP).

**Comunicación:** `queue/worker/` llama a `EvaluacionService` cuando desencola un trabajo de
tipo `evaluacion`. El resultado se publica por `event/publisher/` como `score_de_ia_calculado`.

| Clase | Qué hace |
|---|---|
| `EvaluacionService` | Orquesta: carga rúbrica → arma prompt desde `resources/prompts/evaluacion/` → llama a `LlmGateway` con `funcion="evaluador"` → agrega scores de las 5 dimensiones → persiste resultado |
| `RubricaLoader` | Carga la rúbrica versionada desde BD (nunca hardcodeada — RF-IA-21/29) |
| `ScoreAggregator` | Combina los puntajes de cada dimensión en el `score_agregado` 0–100 con confianza |

> **El evaluador NO tiene fallback de modelo** (RF-IA-25). Si el modelo falla, el score se computa
> neutro y la entrega se acepta igual (RF-IA-27). Un modelo distinto al registrado es una violación
> del contrato, no una degradación aceptable.

---

#### `service/calibracion/` — M4 · Calibración

**Qué va acá:** El proceso que valida que el evaluador acuerde con los docentes.
Bloquea la activación de un curso si no está aprobada (Tema 02 depende del `GET /ai/calibracion/{id}`).

**Comunicación:** `CalibracionController` → `CalibracionService` → encola en `queue/producer/`.
El worker ejecuta, publica `calibracion_aprobada` o `calibracion_fuera_de_tolerancia` por el bus.

| Clase | Qué hace |
|---|---|
| `CalibracionService` | Coordina el runner: carga golden set → pasa cada caso por `EvaluacionService` → compara contra PAR-14 → guarda resultado y veredicto |
| `GoldenSetLoader` | Carga y valida el golden set de la BD (elaborado por los docentes, no por el equipo) |
| `DriftDetector` | Detecta deriva entre la calibración actual y la anterior (RF-IA-32). Si el modelo cambia, se dispara automáticamente vía `event/consumer/` al recibir `modelo_llm_cambiado` |

---

#### `service/moderacion/` — M5 · Moderación de chat

**Qué va acá:** El moderador de mensajes de Tema 11. Es **sincrónico** (latencia < 300 ms, ADR-012).
Se ejecuta en **dos capas**: la clásica (listas + heurísticas) y el clasificador externo.

**Comunicación:** `AiController` (POST /ai/moderador, modo sync) → `ModeracionService`. NO pasa
por la cola. El Tema 11 no entrega el mensaje al destinatario hasta recibir el 200.

| Clase | Qué hace |
|---|---|
| `ModeracionService` | Orquesta las dos capas: primero capa clásica, solo si no resuelve → clasificador externo vía `LlmGateway` |
| `CapaClasicaService` | Detectores determinísticos: lista de términos con nivel, heurística de frecuencia (spam), detección de bloque de código (integridad académica), detección de base64. Resuelve en < 1 ms sin salir del proceso |
| `ListaTerminosLoader` | Carga y versiona la lista desde BD (la versión se devuelve en `version_lista` del response, RF-IA-13/25) |

> **La capa clásica resuelve la mayoría de los casos sin tocar el LLM.** El clasificador solo se
> invoca para el residuo contextual (acoso sin léxico explícito). El campo `origen` en el response
> (`lista | heuristica | clasificador`) permite medir esa proporción.

---

#### `service/tutor/` — M6 · Tutor

**Qué va acá:** El chat educativo. Es **sincrónico** (latencia < 2 s, con SSE para streaming).
Depende de M1 (gateway), M2 (RAG para contexto) y M5 (guardarraíl anti-fuga de salida).

| Clase | Qué hace |
|---|---|
| `TutorService` | Recibe la pregunta → pide contexto relevante a `RetrievalService` → arma el prompt con ese contexto → llama a `LlmGateway` con `funcion="tutor"` → el gateway aplica el `OutputAntiLeakGuard` antes de devolver |

> **El guardarraíl anti-fuga (PAR-11, 70% de similitud) vive en `service/gateway/guard/`**, no
> en `TutorService`. El servicio del tutor no sabe que existe: el gateway lo aplica siempre.
> Si falla, el gateway pide regenerar la respuesta — el tutor solo ve el resultado final.

---

#### `service/generacion/` — M7a · Generador de desafíos

**Qué va acá:** Genera enunciados de desafíos a partir de un blueprint. Es **asíncrono**
(latencia alta, volumen muy bajo, hay revisión humana posterior).

| Clase | Qué hace |
|---|---|
| `GeneracionService` | Carga blueprint → genera por slot → valida salida contra schema JSON → persiste borrador |
| `BlueprintLoader` | Carga la plantilla del tipo de desafío (no el contenido) |
| `SlotValidator` | Valida que cada slot generado tenga el formato correcto antes de persistir |

---

#### `service/correccion/` — M7b · Corrector

**Qué va acá:** El segundo juez. Evalúa la respuesta abierta de un alumno. Es **asíncrono**
(latencia alta, volumen medio, alto riesgo — es una nota).

| Clase | Qué hace |
|---|---|
| `CorreccionService` | Valida la entrega → arma prompt desde `resources/prompts/correccion/` → llama a `LlmGateway` con `funcion="corrector"` → persiste score |

---

### `repository/` — Capa de acceso a datos

**Qué va acá:** Interfaces de Spring Data JPA. Una por entidad. Nada más.
Nunca tienen lógica de negocio: las queries complejas van en métodos con `@Query`.

| Clase | Tabla que gestiona |
|---|---|
| `EvaluacionRepository` | `evaluaciones` (score, confianza, estado, trace_id, tokens…) |
| `JobRepository` | `jobs` (estado de cada trabajo asíncrono, posicion_en_cola) |
| `ChunkRepository` | `chunks` (texto + vector pgvector + `curso_cohorte_id`) |
| `CalibracionRepository` | `calibraciones` (resultado, veredicto, deriva por dimensión) |
| `GoldenSetRepository` | `golden_set_casos` (casos de prueba elaborados por los docentes) |
| `FuncionModeloRepository` | `funcion_modelo_config` (tabla función → proveedor + modelo + versión, RF-IA-23/24) |
| `QuotaRepository` | Lectura/escritura de contadores (se prefiere Redis directo desde `QuotaStore`) |

---

### `entity/` — Entidades JPA

**Qué va acá:** Clases `@Entity` que mapean 1:1 con las tablas de Postgres.
Solo las usan `repository/` y `service/`. **Nunca salen del microservicio**: el controller
siempre devuelve DTOs, nunca entidades.

> **Regla de oro: `curso_cohorte_id` va en TODA entidad** que tenga alcance de curso.
> `EvaluacionEntity`, `ChunkEntity`, `CalibracionEntity`, `GoldenSetCasoEntity`, `JobEntity`.
> Si no está desde el primer migration, agregarla después es una migración de datos.

---

### `queue/` — Cola interna Redis (M8)

**Por qué Redis y no el bus de la cátedra:** La cola interna es **diseño nuestro** y no viola
ninguna regla. El bus de la cátedra es para comunicación entre microservicios; la cola Redis es
para que los workers procesen trabajos largos sin bloquear el HTTP handler.

**`queue/producer/`** — Encola trabajos

| Clase | Qué hace |
|---|---|
| `JobProducer` | Serializa un `JobPayload` y lo empuja a la cola Redis correcta (una por tipo: `evaluacion`, `ingesta`, `calibracion`, `generacion`, `correccion`) |
| `JobPayload` | Clase sellada con los datos mínimos para que el worker retome el trabajo |

**`queue/worker/`** — Consume la cola

| Clase | Qué hace |
|---|---|
| `EvaluacionWorker` | Desencola `JobPayload` de tipo evaluación → llama a `EvaluacionService` → al terminar encola el resultado en `event/publisher/` |
| `IngestaWorker` | Ídem para ingesta de documentos |
| `CalibracionWorker` | Ídem para calibración |
| `GeneracionWorker` | Ídem para generación |
| `CorreccionWorker` | Ídem para corrección |

> **Los workers son la misma imagen Docker que el HTTP server, con distinto comando.**
> Escalar el pico es `docker compose up --scale worker=6`. Sin código nuevo.

---

### `event/` — Bus de eventos (M8)

**Comunicación con el exterior.** Todo lo asíncrono que cruza fronteras de microservicio.

**`event/publisher/`** — Lo que publicamos al bus

| Clase | Evento que publica |
|---|---|
| `ScorePublisher` | `score_de_ia_calculado` — el Tema 03 aplica el XP con esto |
| `CalibracionPublisher` | `calibracion_aprobada` / `calibracion_fuera_de_tolerancia` |
| `IngestaPublisher` | `ingesta_completada` con el reporte de calidad |
| `IncidentePublisher` | `incidente_de_jailbreak` — el Tema 12 lo muestra al ADMIN |

**`event/consumer/`** — Lo que consumimos del bus

| Clase | Evento que consume | Qué hace |
|---|---|---|
| `IntentoConsumer` | `intento_cerrado` (Tema 03) | Encola una evaluación en `queue/producer/` |
| `CursoConsumer` | `curso_archivado` (Tema 02) | Frena trabajos pendientes de ese `curso_cohorte_id` |
| `ModeloConsumer` | `modelo_llm_cambiado` (Tema 12) | Dispara recalibración automática (RF-IA-32) |

---

### `config/` — Configuración Spring Boot (M8)

**Qué va acá:** Clases `@Configuration` que inicializan beans de infraestructura.
No tienen lógica de negocio.

| Clase | Qué configura |
|---|---|
| `DataSourceConfig` | Datasource, JPA, pool de conexiones (HikariCP) |
| `RedisConfig` | `RedisTemplate`, serialización, TTL de las colas |
| `EurekaConfig` | Registro dinámico en Service Discovery de la cátedra |
| `Resilience4jConfig` | Circuit breaker + retry para los adapters del LLM (RF-IA-27) |
| `LlmProperties` | Bindea la sección `llm:` de `application.yml` (endpoints, timeouts, api-keys como env vars) |
| `SecurityConfig` | Validación del token interno entre microservicios |
| `WebConfig` | CORS, filtros, `trace_id` propagado desde el header del API Gateway |

---

## `resources/` — Archivos de configuración y artefactos versionados

### `resources/contracts/`

OpenAPI 3.x por función. **Escribir el contrato antes de implementar.** El Tema 02 (y los demás)
arrancan contra un mock generado desde estos archivos mientras el equipo construye.

| Archivo | Función |
|---|---|
| `ai-v1.yaml` | El sobre común `POST /ai/{funcion}` + los 5 GETs |
| `moderacion-v1.yaml` | Detalle del moderador (ya existe) |

### `resources/prompts/`

Los prompts viven en archivos, **no en el código** (RF-IA-29: un solo criterio para todos los
modelos, sin variantes por código). Esto también mitiga el ciclo lento de Java: editar un
prompt no requiere recompilar ni redeployar.

Cada carpeta tiene dos archivos por versión:

| Archivo | Qué es |
|---|---|
| `system-v1.txt` | System prompt de la función (instrucciones al modelo) |
| `user-v1.txt` | Plantilla del user prompt con placeholders (`{texto_alumno}`, `{rubrica}`, etc.) |

La versión del prompt se guarda junto con cada evaluación (RF-IA-25). Así se puede reproducir
exactamente qué prompt produjo cada score.

### `resources/db/migration/`

Scripts Flyway. Se ejecutan en orden al levantar el servicio. Nunca se modifican una vez
aplicados: si hay que cambiar algo, se agrega un script nuevo.

| Archivo | Qué hace |
|---|---|
| `V1__init.sql` | Tablas base: `jobs`, `evaluaciones`, `calibraciones`, `funcion_modelo_config` |
| `V2__pgvector.sql` | Extensión `pgvector`, tabla `chunks` con columna `vector(1536)` |
| `V3__golden_set.sql` | Tabla `golden_set_casos` |
| `V4__quota.sql` | Tabla `quota_diaria` (backup de Redis; Redis es la fuente de verdad) |

> **`curso_cohorte_id` en V1.** Si se olvidó, hay que agregarlo en V2 con una columna nullable
> y luego hacerla NOT NULL en V3 después de migrar los datos — ese trabajo no existe si está desde V1.

---

## Reglas de comunicación entre capas

```
controller  →  service         ✅  la única dirección permitida
service     →  repository      ✅
service     →  queue/producer  ✅  para trabajos diferidos
service     →  event/publisher ✅  al terminar un trabajo
event/consumer → service       ✅  el consumer delega al service
queue/worker  → service        ✅  el worker delega al service

controller  →  repository      ❌  el controller no toca la BD
controller  →  entity          ❌  el controller no conoce entidades JPA
service     →  controller      ❌  el servicio no sabe que existe HTTP
repository  →  service         ❌  el repo no tiene lógica
queue/worker → controller      ❌  el worker no emite HTTP responses
event        ↔ queue           ❌  no se hablan entre sí
```

---

## Orden de implementación

```
1. controller/  +  dto/  (stub que devuelve mocks hardcodeados)
   → El Tema 02 puede arrancar contra el mock de GET /ai/calibracion/{id}
   → El Tema 11 puede arrancar contra el mock de POST /ai/moderador

2. service/gateway/  (M1 — sin esto nadie llama al LLM)
   → adapter/ + quota/ + guard/
   → Primero un solo adapter (el proveedor que eligió el equipo)
   → La tabla funcion_modelo_config permite cambiar sin deploy

3. service/moderacion/  (M5 — capa clásica primero, sin LLM)
   → CapaClasicaService funciona sola; el clasificador se agrega después
   → Desbloquea al Tema 11 con respuestas reales (no mock)

4. service/evaluacion/  (M3 — el núcleo del producto)
   → Requiere service/gateway/ funcionando
   → Requiere los prompts en resources/prompts/evaluacion/

5. service/calibracion/  (M4)
   → Requiere M3
   → Desbloquea el GET /ai/calibracion/ con datos reales → Tema 02 activa cursos

6. service/rag/  (M2)
   → Ingesta + chunking + embedding + retrieval
   → Lo necesitan tutor y generador

7. service/tutor/  (M6)
   → Requiere M1 + M2 + guard/ (ya están)

8. service/generacion/ + service/correccion/  (M7)
   → Requieren M1 + M2
```
