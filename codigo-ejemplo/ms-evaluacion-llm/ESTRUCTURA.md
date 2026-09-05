# Estructura de carpetas — ms-evaluacion-llm

> ## Migración pendiente de implementación
>
> Este documento describe el árbol previo a la alineación. Para nuevas clases, contratos y tests
> mandan [`docs/00`](../../docs/00-fuentes-de-verdad-y-convenciones.md) y
> [`docs/contracts/`](../../docs/contracts/): el destino es `llm-service`, con base path
> `/api/llm`, headers `traceparent`/`X-Request-Id`, API por recursos y Kafka para eventos.
> Las rutas `/ai/*`, `/api/conversaciones` y el campo `trace_id` que aparecen abajo son legado;
> no deben extenderse.

> Organización **por capas** del microservicio Java Spring Boot (Tema 07).
>
> Referencia de arquitectura: [02-arquitectura-y-stack.md](../../docs/02-arquitectura-y-stack.md)
> Referencia de integración: [17-mapa-de-integracion.md](../../docs/17-mapa-de-integracion.md)
> Reparto por persona y plan de la demo: [10-entregables-y-plan.md](../../docs/10-entregables-y-plan.md) Parte 2

---

## Cómo leer este documento

**El árbol es el destino, no la foto de hoy.** Este repositorio tiene construida la rebanada
de la demo (gateway + tutor); el resto de los paquetes son el lugar reservado para cuando se
construyan. Cada carpeta lleva su estado y su dueño:

| Marca | Significa |
|---|---|
| ✅ | **Existe hoy** en `src/`. Se puede leer el código |
| 🟡 | Existe **parcialmente** — hay clases, faltan otras de la tabla |
| ⬜ | **Destino.** La carpeta se crea cuando arranca ese módulo, no antes |

**Dueño** es la persona del reparto de [10](../../docs/10-entregables-y-plan.md) Parte 2 (P1–P6).
No es burocracia: es la respuesta a *«¿a quién le pregunto por esta carpeta?»* y a *«¿quién resuelve
este conflicto de merge?»*.

> **No crees paquetes Java vacíos «para que estén».** Git no versiona directorios vacíos —harían
> falta `.gitkeep` en cada uno—, y un paquete sin clases ensucia los reportes de JaCoCo
> y PMD. **El paquete nace con su primera clase.**
>
> **La excepción es `resources/`:** ahí los `.gitkeep` de `prompts/` sí valen la pena, porque
> declaran dónde va a vivir cada prompt antes de que exista y evitan que alguien lo escriba
> adentro del código. No son paquetes: no los mira ninguna herramienta de calidad.

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

### Lo que esta decisión NO te da — leelo antes de repartir el trabajo

**Las fronteras entre módulos son de negocio, no de datos.** `service/` está partido en un paquete
por módulo, pero `repository/` y `entity/` son **planos y compartidos**: la entidad de M3 y la de M2
viven en la misma carpeta y cualquiera puede inyectar cualquier repositorio.

Es una decisión consciente, no un olvido. Partir también los datos (`evaluacion/repository/`,
`rag/repository/`) es lo correcto en un sistema grande, y acá sería sobreingeniería: son cuatro
semanas, un solo esquema de base y una sola transacción.

**Pero tiene un costo, y hay que administrarlo:** `repository/` y `entity/` son el punto de conflicto
de merge más probable del proyecto. Por eso **cada tabla tiene un dueño declarado** en la tabla de
[`repository/`](#repository--capa-de-acceso-a-datos), y la regla es:

> **Podés leer la entidad de otro módulo. Para cambiarle un campo, hablás con su dueño.**
> Un `ALTER` sobre una tabla ajena rompe el migration de otro y se descubre en la máquina de él, no en la tuya.

---

## Árbol completo

```
src/
├── main/
│   ├── java/ar/edu/utn/frc/tup/piv/evaluacionllm/
│   │   │                                                   estado  dueño
│   │   ├── Application.java                                   ✅     P6
│   │   │
│   │   ├── controller/                  CAPA 1 · Presentación 🟡     P6
│   │   │
│   │   ├── dto/                         El contrato HTTP
│   │   │   ├── request/                   DTOs de entrada     🟡     cada uno
│   │   │   └── response/                  DTOs de salida      🟡     cada uno
│   │   │
│   │   ├── mapper/                      DTO ↔ entity          ⬜     P6
│   │   │
│   │   ├── exception/                   Errores → HTTP        🟡     P6
│   │   │
│   │   ├── service/                     CAPA 2 · Negocio
│   │   │   ├── gateway/                   M1 · AI Gateway     🟡     P1
│   │   │   │   ├── registry/                función → modelo  ⬜     P1
│   │   │   │   ├── adapter/                 uno por proveedor 🟡     P1
│   │   │   │   ├── quota/                   contadores        ⬜     P1
│   │   │   │   ├── guard/                   guardarraíles     ✅     P5
│   │   │   │   └── log/                     log de llamadas   ⬜     P1
│   │   │   ├── rag/                       M2 · RAG            ⬜     P2
│   │   │   ├── evaluacion/                M3 · Evaluador      ⬜     P3
│   │   │   ├── calibracion/               M4 · Calibración    ⬜     P4
│   │   │   ├── moderacion/                M5 · Moderación     ⬜     P5
│   │   │   ├── tutor/                     M6 · Tutor          ✅     P6
│   │   │   ├── generacion/                M7 · Generador      ⬜     P2
│   │   │   └── correccion/                M7 · Corrector      ⬜     P3
│   │   │
│   │   ├── repository/                  CAPA 3 · Datos        🟡     compartida
│   │   │
│   │   ├── entity/                      CAPA 4 · JPA          🟡     compartida
│   │   │
│   │   ├── queue/                       CAPA 5 · Cola Redis   ⬜     P6
│   │   │   ├── producer/                  encola trabajos     ⬜     P6
│   │   │   └── worker/                    drena la cola       ⬜     P6
│   │   │
│   │   ├── event/                       CAPA 6 · Bus (M8)     ⬜     P6
│   │   │   ├── publisher/                 lo que publicamos   ⬜     P6
│   │   │   └── consumer/                  lo que consumimos   ⬜     P6
│   │   │
│   │   └── config/                      CAPA 7 · Spring (M8) 🟡     P6
│   │
│   └── resources/
│       ├── application.yml                                    ✅
│       ├── contracts/                     OpenAPI por función 🟡     P6
│       ├── prompts/                       Fuera del código
│       │   ├── tutor/                                         ✅     P6
│       │   ├── evaluacion/                                    ⬜     P3
│       │   ├── correccion/                                    ⬜     P3
│       │   ├── generacion/                                    ⬜     P2
│       │   └── moderacion/                                    ⬜     P5
│       └── db/
│           └── migration/                 Scripts Flyway      ⬜     P6
│
└── test/
    ├── java/ar/edu/utn/frc/tup/piv/evaluacionllm/
    │   └── (espeja main/ paquete por paquete: el test de una clase
    │        vive en el mismo paquete que la clase)
    └── resources/
        ├── application-test.yml           perfil de test      ⬜
        └── fixtures/                      respuestas fijas    ⬜
            ├── evaluacion/
            ├── tutor/
            └── moderacion/
```

**Los tests espejan `main/` y no tienen árbol propio.** Un `TutorServiceImplTest` vive en
`service/tutor/` igual que la clase que prueba; así el test ve los miembros package-private y
nadie tiene que decidir dónde ponerlo. No hay carpeta aparte para los tests que llaman al modelo
real: eso se resuelve con una etiqueta, y está explicado en
[Cómo se separan los tests](#cómo-se-separan-los-tests).

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

### `mapper/` — La conversión DTO ↔ entity

**Qué va acá:** las clases que traducen entre el contrato HTTP (`dto/`) y las entidades JPA
(`entity/`). Una por par, con métodos estáticos o con MapStruct.

**Por qué existe la carpeta:** porque *«la conversión la hace el service o un mapper»* es la clase de
libertad que, con seis personas, produce seis estilos distintos —y después nadie sabe dónde buscar
por qué un campo llega en null. **Decidido: el mapeo vive acá y en ningún otro lado.**

| Regla | Consecuencia |
|---|---|
| El `service` recibe y devuelve **entidades o tipos del dominio**, nunca DTOs | El negocio no depende del contrato HTTP |
| El `controller` llama al mapper para entrar y para salir | El contrato cambia sin tocar el negocio |
| El mapper **no tiene lógica**: no calcula, no valida, no consulta la BD | Si un mapeo necesita una regla, esa regla es del service |

> **Si el mapeo de un módulo es de dos campos, un `record` con un método `from(...)` alcanza.**
> No hace falta traer MapStruct hasta que duela: el objetivo es que el mapeo tenga **un** lugar,
> no que tenga framework.

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

**`service/gateway/`** (raíz) — La orquestación

| Clase | Qué hace |
|---|---|
| `LlmGateway` | Interfaz central: `llamar(funcion, prompt) → LlmRespuesta`. **Es lo único que ve el resto del servicio** |
| `LlmGatewayImpl` | Orquesta los 8 pasos: resolver función → cuota → guardarraíl de entrada → prompt → adapter → validar schema → anti-fuga → registrar |
| `EscaleraDegradacion` | Recorre los proveedores en orden ante una falla y decide cuándo parar (RF-IA-27). Vive acá y no en `adapter/`: **un adapter no puede saber que existen otros adapters** |
| `LlmRequest` / `LlmResponse` | Tipos internos del gateway. No son los DTOs del contrato HTTP |

**`service/gateway/registry/`** — La tabla función → modelo (RF-IA-23/24/35)

> **Esto es lo más valioso del servicio y por eso tiene paquete propio.** Estaba metido dentro de
> `adapter/` como una clase suelta, y es al revés: el registro **decide qué adapter se usa**, así que
> no puede vivir adentro de uno de ellos. Si esta pieza está bien hecha, RF-IA-27, RF-IA-28 y
> RF-IA-32 salen casi gratis ([02](../../docs/02-arquitectura-y-stack.md) Parte 1 §4).

| Clase | Qué hace |
|---|---|
| `FuncionModeloRegistry` | Resuelve `funcion → proveedor + modelo + versión` leyendo la BD. **Nunca hardcodeado, nunca desde `application.yml`**: lo edita un ADMIN sin deploy |
| `FuncionModeloCache` | Cachea la resolución para no pegarle a la BD en cada llamada, e invalida al recibir `modelo_llm_cambiado` |

**`service/gateway/adapter/`** — Un adapter por proveedor

| Clase | Qué hace |
|---|---|
| `LlmAdapter` | La interfaz que implementan todos. Recibe `LlmRequest`, devuelve `LlmResponse` |
| `AnthropicAdapter` | Traduce al formato de Anthropic y la respuesta al formato interno |
| `OpenAiAdapter` | Ídem para OpenAI |
| `GroqAdapter` | Ídem para Groq |
| `GoogleAdapter` | Ídem para Google Gemini |

> **Un adapter traduce y nada más.** No reintenta, no elige modelo, no cuenta cuota, no registra.
> Todo eso es del gateway. Es lo que hace que sumar un proveedor sea una clase nueva y cero cambios
> en el resto.

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

**`service/gateway/log/`** — Registro de llamadas (RF-IA-02/25/33)

**Faltaba en la versión anterior de este árbol, y es un entregable comprometido**
([10](../../docs/10-entregables-y-plan.md) Bloque 2, última fila). Sin esto no hay defensa posible
de un score: *«¿con qué modelo se calculó esta nota?»* no tiene respuesta.

| Clase | Qué hace |
|---|---|
| `LlamadaLlmLogger` | Persiste una fila por llamada: `model_id`, `model_version`, `prompt_version`, `rubric_version`, tokens de entrada y salida, costo estimado, latencia, `trace_id`, `curso_cohorte_id` |
| `CostoEstimator` | Traduce tokens a plata según el precio del modelo. El precio es dato de config, no constante en el código |

> **Lo escribe el gateway, siempre, incluso cuando la llamada falla.** Un error de proveedor
> registrado es lo que después permite demostrar que la escalera de degradación se disparó.
> Si el log lo hiciera cada service, habría cinco formatos y ninguno completo.

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

#### `service/generacion/` — M7 · Generador de desafíos

> **M7 es un solo módulo en [02](../../docs/02-arquitectura-y-stack.md) y
> [10](../../docs/10-entregables-y-plan.md); acá son dos paquetes.** No es una contradicción:
> comparten dueño distinto (P2 el generador, P3 el corrector) y no comparten código, así que
> separarlos evita que dos personas editen la misma carpeta. **Se los nombra M7, sin sufijos:**
> «M7a/M7b» no existe en ningún otro documento y confunde en las reuniones de integración.

**Qué va acá:** Genera enunciados de desafíos a partir de un blueprint. Es **asíncrono**
(latencia alta, volumen muy bajo, hay revisión humana posterior).

| Clase | Qué hace |
|---|---|
| `GeneracionService` | Carga blueprint → genera por slot → valida salida contra schema JSON → persiste borrador |
| `BlueprintLoader` | Carga la plantilla del tipo de desafío (no el contenido) |
| `SlotValidator` | Valida que cada slot generado tenga el formato correcto antes de persistir |

---

#### `service/correccion/` — M7 · Corrector

**Qué va acá:** El segundo juez. Evalúa la respuesta abierta de un alumno. Es **asíncrono**
(latencia alta, volumen medio, alto riesgo — es una nota).

| Clase | Qué hace |
|---|---|
| `CorreccionService` | Valida la entrega → arma prompt desde `resources/prompts/correccion/` → llama a `LlmGateway` con `funcion="corrector"` → persiste score |

---

### `repository/` — Capa de acceso a datos

**Qué va acá:** Interfaces de Spring Data JPA. Una por entidad. Nada más.
Nunca tienen lógica de negocio: las queries complejas van en métodos con `@Query`.

**Esta carpeta es compartida y por eso cada fila tiene dueño.** El dueño es quien decide el esquema
de esa tabla y quien escribe su migration; el resto lee sin pedir permiso y avisa antes de cambiar
un campo.

| Clase | Tabla que gestiona | Dueño |
|---|---|---|
| `EvaluacionRepository` | `evaluaciones` (score, confianza, estado, trace_id, tokens…) | P3 |
| `JobRepository` | `jobs` (estado de cada trabajo asíncrono, posicion_en_cola) | P6 |
| `ChunkRepository` | `chunks` (texto + vector pgvector + `curso_cohorte_id`) | P2 |
| `CalibracionRepository` | `calibraciones` (resultado, veredicto, deriva por dimensión) | P4 |
| `GoldenSetRepository` | `golden_set_casos` (casos de prueba elaborados por los docentes) | P4 |
| `FuncionModeloRepository` | `funcion_modelo_config` (función → proveedor + modelo + versión, RF-IA-23/24) | P1 |
| `LlamadaLlmRepository` | `llamadas_llm` (el log de RF-IA-02/25/33) | P1 |

> **No hay `QuotaRepository`, y es a propósito.** El contador de cuota vive en Redis con TTL de 24 h
> y lo maneja `QuotaStore`. Tener además un repositorio JPA de cuota garantiza que tarde o temprano
> alguien decremente en un lado y lea del otro. **Una fuente de verdad: Redis.**
> Si más adelante hace falta persistir el consumo para facturar o auditar, eso es una tabla de
> histórico que se escribe desde `gateway/log/`, no un segundo contador.

---

### `entity/` — Entidades JPA

**Qué va acá:** Clases `@Entity` que mapean 1:1 con las tablas de Postgres.
Solo las usan `repository/` y `service/`. **Nunca salen del microservicio**: el controller
siempre devuelve DTOs, nunca entidades.

> **Regla de oro: `curso_cohorte_id` va en TODA entidad** que tenga alcance de curso.
> `EvaluacionEntity`, `ChunkEntity`, `CalibracionEntity`, `GoldenSetCasoEntity`, `JobEntity`,
> `LlamadaLlmEntity`. Si no está desde el primer migration, agregarla después es una migración de
> datos ([02](../../docs/02-arquitectura-y-stack.md) Parte 1 §7).

> **`V4__quota.sql` desaparece del plan de migrations.** Estaba descrito como *«backup de Redis»*
> y no hay tal cosa: si Redis es la fuente de verdad, una tabla espejo solo puede estar desactualizada.
> El slot queda libre para `V4__llamadas_llm.sql`, que sí hace falta.

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
| `V4__llamadas_llm.sql` | Tabla `llamadas_llm`: el log de RF-IA-02/25/33 (modelo, versión, tokens, costo, latencia, `trace_id`) |

> **No hay migration de cuota.** Los contadores viven en Redis y solo ahí — ver la nota en
> [`repository/`](#repository--capa-de-acceso-a-datos).

> **`curso_cohorte_id` en V1.** Si se olvidó, hay que agregarlo en V2 con una columna nullable
> y luego hacerla NOT NULL en V3 después de migrar los datos — ese trabajo no existe si está desde V1.

---

## Cómo se separan los tests

[TESTING.md](TESTING.md) pide que los tests que llaman a la API real estén *«en un módulo / carpeta
separada»* y que **no corran en CI automático**. La carpeta separada no hace falta y además no
alcanza: lo que decide qué corre es la etiqueta de JUnit, no la ruta del archivo.

**Tres categorías, dos etiquetas:**

| Categoría | Etiqueta | Corre en el build de todos los días | Corre con `-Pcompleto` | Gasta plata |
|---|---|---|---|---|
| **Unitario** — el LLM está mockeado | *(ninguna)* | ✅ | ✅ | No |
| **Integración** — levanta Postgres y Redis | `@Tag("integracion")` | ❌ | ✅ | No |
| **Modelo real** — pega contra el proveedor | `@Tag("modelo-real")` | ❌ | ❌ | **Sí** |

**`modelo-real` está excluida en los dos perfiles y eso es deliberado.** El perfil `completo` levanta
la exclusión de `integracion` —para eso existe— pero mantiene la de `modelo-real`, porque esos tests
le pegan a la API del proveedor. Un test que gasta plata sin que nadie lo pida no se descubre hasta
que llega la factura.

Para correrlos, a mano y a sabiendas:

```bash
./mvnw test -Dgroups=modelo-real -DexcludedGroups= -DtestFailureIgnore=false
```

> **Etiquetá bien y no te confíes de la carpeta.** `integracion` significa *«necesita Postgres o
> Redis»*; `modelo-real` significa *«le pega al proveedor»*. Un test que hace las dos cosas lleva las
> dos etiquetas: si solo lleva `integracion`, el build completo lo va a ejecutar.

**Dónde va cada archivo:** en el paquete espejo de la clase que prueba. La etiqueta va en la clase,
no en la carpeta:

```
service/gateway/adapter/GroqAdapterTest.java          ← unitario, mock del HTTP
service/gateway/adapter/GroqAdapterModeloRealTest.java ← @Tag("modelo-real")
service/evaluacion/EvaluacionServiceTest.java          ← unitario
repository/ChunkRepositoryTest.java                    ← @Tag("integracion"), necesita pgvector
```

**`src/test/resources/fixtures/`** guarda las respuestas fijas del modelo que pide TESTING.md: un
JSON por caso, versionado en git. Es lo que hace que el test de parseo pruebe algo real sin llamar a
nadie — y lo que permite reproducir un bug de parseo pegando la respuesta que lo rompió.

---

## Reglas de comunicación entre capas

```
controller  →  mapper          ✅  para entrar y para salir
controller  →  service         ✅  la única dirección permitida
service     →  repository      ✅
service     →  queue/producer  ✅  para trabajos diferidos
service     →  event/publisher ✅  al terminar un trabajo
event/consumer → service       ✅  el consumer delega al service
queue/worker  → service        ✅  el worker delega al service

cualquier service → gateway/LlmGateway  ✅  la ÚNICA puerta al modelo

controller  →  repository      ❌  el controller no toca la BD
controller  →  entity          ❌  el controller no conoce entidades JPA
service     →  dto/            ❌  el negocio no conoce el contrato HTTP
service     →  controller      ❌  el servicio no sabe que existe HTTP
repository  →  service         ❌  el repo no tiene lógica
mapper      →  repository      ❌  el mapper traduce, no consulta
queue/worker → controller      ❌  el worker no emite HTTP responses
event        ↔ queue           ❌  no se hablan entre sí

cualquier service → adapter/   ❌  nadie llama a un proveedor por su cuenta
gateway/adapter → registry/    ❌  un adapter no sabe que existen otros
```

> **La última regla es la que sostiene todo lo demás.** Si un service llama a `GroqAdapter` en vez
> de a `LlmGateway`, esa llamada no tiene cuota, ni guardarraíl, ni log, ni degradación — y la tabla
> función→modelo deja de ser la verdad. Es el mismo argumento del AI Gateway, pero adentro del
> equipo ([10](../../docs/10-entregables-y-plan.md) Parte 2 §1).

---

## Orden de implementación

**Son dos órdenes distintos y conviene no mezclarlos.** El de la demo lo manda el calendario de
cuatro semanas; el del producto lo mandan las dependencias entre módulos y los equipos que nos
esperan.

### Orden de la demo — las cuatro semanas de [10](../../docs/10-entregables-y-plan.md) Parte 2 §4

Este es el orden que hay que seguir ahora. **Fuera de alcance en la demo:** `queue/`, `event/`,
`config/EurekaConfig`, el fallback entre proveedores y el componente Angular. Se usa el Swagger UI
que genera springdoc.

| Semana | Qué se construye | Paquetes | Quién |
|---|---|---|---|
| **1** | `docker compose up` levanta el servicio y una llamada real responde | `config/` · `service/gateway/` con **un** adapter · `controller/` | P1, P6 |
| **2** | Ingesta de un PDF → chunks con metadata → embeddings → búsqueda | `service/rag/` · `entity/ChunkEntity` · `repository/ChunkRepository` | P2 |
| **3** | Generar 5 preguntas desde ese PDF, con salida estructurada y validación | `service/generacion/` · `resources/prompts/generacion/` | P2, P3 |
| **4** | Corregir una respuesta y evaluar una transcripción | `service/correccion/` · `service/evaluacion/` · `service/gateway/guard/` | P3, P5 |

> **Semana 1 es la que destraba a los otros cuatro.** Hasta que exista
> `LlmGateway.llamar(funcion, prompt)` andando contra un proveedor real, P2, P3, P4 y P5 no tienen
> sobre qué construir. Es la única semana donde el paralelismo no ayuda.

### Orden del producto — cuando el alcance es el completo

```
1. controller/  +  dto/  +  mapper/   (stub que devuelve mocks hardcodeados)
   → El Tema 02 arranca contra el mock de GET /ai/calibracion/{id}
   → El Tema 11 arranca contra el mock de POST /ai/moderador
   → Se publica el OpenAPI de resources/contracts/ ANTES de implementar

2. service/gateway/   (M1 — sin esto nadie llama al LLM)
   → registry/ primero: la tabla funcion→modelo es lo que permite cambiar sin deploy
   → después adapter/ (uno solo), quota/, guard/, log/
   → EscaleraDegradacion se agrega recién con el segundo adapter

3. service/rag/       (M2)
   → Ingesta + chunking + embedding + retrieval
   → Lo necesitan tutor, evaluador y generador: adelantarlo desbloquea a tres personas

4. service/evaluacion/ (M3 — el núcleo del producto)
   → Requiere M1 y los prompts en resources/prompts/evaluacion/

5. service/calibracion/ (M4)
   → Requiere M3
   → Desbloquea el GET /ai/calibracion/ con datos reales → el Tema 02 activa cursos

6. service/tutor/     (M6)
   → Requiere M1 + M2 + gateway/guard/

7. service/generacion/ + service/correccion/  (M7)
   → Requieren M1 + M2

8. service/moderacion/ (M5)
   → Va último: el chat es Fase 2 del PRD y no existe todavía
   → Lo que sí se entrega temprano es su CONTRATO (resources/contracts/moderacion-v1.yaml),
     para que el Tema 11 no cierre el suyo sin nuestros campos
```

> **La moderación bajó del puesto 3 al 8 respecto de la versión anterior de este documento.**
> El puesto 3 asumía que desbloqueaba al Tema 11, pero
> [10](../../docs/10-entregables-y-plan.md) Bloque 3 es explícito: **el chat es Fase 2 y se
> construye cuando el chat exista.** Lo que el Tema 11 necesita ya no es el código, es el `.yaml`
> —y ese está entregado.
