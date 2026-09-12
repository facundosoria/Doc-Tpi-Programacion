# `codigo-ejemplo/ms-evaluacion-llm`

> **🟢 2026-09-12 — carpeta eliminada tras la consolidación.** `LlmGateway`/`GroqAdapter` →
> [`ep-02/h10.md`](../ep-02/h10.md); `InputGuard`/`OutputAntiLeakGuard` →
> [`ep-05/interactions.md`](../ep-05/interactions.md); `contracts/moderacion-v1.yaml` →
> [`docs/contracts/llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml).
> `TutorChatController` (CRUD de conversaciones) **no** se portó — ver
> [`ep-05/interactions.md`](../ep-05/interactions.md#qué-no-se-portó-decisión-explícita-no-descuido).
> Queda el análisis original como registro.

- **Épicas más cercanas:** EP-05 (match fuerte) · EP-02 (boceto de AI Gateway) · EP-08 (contrato
  de moderación, sin código)
- **Solapamiento con `llm-service`:** ninguno a nivel de código
- **Dato clave:** es el **nombre histórico** del mismo servicio que hoy es `llm-service`
  ([02 · línea 3](../../02-arquitectura-y-stack.md)) — ver
  [`README.md`](README.md#el-dato-más-importante-para-la-planificación).

## Qué es

Pese al nombre, el propio `pom.xml` (líneas 7–16) lo declara **"esqueleto mínimo... sin lógica
de negocio a propósito"**, para probar el pipeline de build (Spotless, PMD, JaCoCo con
exclusiones, Surefire con `excludedGroups=integracion,modelo-real` para no gastar dinero contra
proveedores reales). Implementa, con arquitectura más prolija que
[`lara-heredia-demo-llm-spring-ai`](lara-heredia-demo-llm-spring-ai.md):

- **`AiController`**: `POST /ai/tutor`, recibe `AiRequest<PayloadTutorDto>`
  (`contexto`/`payload`/`modo="sync"`/`idempotency_key`) — coincide casi literalmente con el
  contrato de [02 · Parte 3, §1](../../02-arquitectura-y-stack.md) (`POST /ai/{funcion}`).
- **`TutorChatController`**: CRUD REST bajo `/api/conversaciones`, con filtros por
  `curso_cohorte_id` y `usuario_ref` (a diferencia de lara-heredia).
- **`TutorServiceImpl`**: orquesta guardarraíl de entrada → prompt desde archivo
  (`prompts/tutor/system-v1.txt`, `user-v1.txt`) → `LlmGateway.llamar("tutor", ...)` →
  guardarraíl de salida → persistencia.
- **`LlmGateway`/`LlmGatewayImpl`/`GroqAdapter`** (`service/gateway`): interfaz agnóstica de
  proveedor + implementación que hoy solo delega a Groq. Comentario explícito en el código: *"En
  el M1 completo, aquí se consultaría la tabla `funcion_modelo_config` y se verificaría la cuota
  en Redis. Por ahora el proveedor default es Groq."* — un boceto consciente del módulo **M1 · AI
  Gateway**.
- **`InputGuard`**: jailbreak con normalización Unicode (`Normalizer.normalize(NFD)` + strip de
  diacríticos) — más robusto que el de lara-heredia.
- **`OutputAntiLeakGuard`**: detecta bloques de código largos (>8 líneas) o coincidencia literal
  contra `solucionEsperada`, fuerza respuesta socrática de reemplazo. **Única implementación
  concreta encontrada del guardarraíl de salida anti-fuga** (RF-IA-20 / EP-05).
- `GlobalExceptionHandler` con errores tipados y propagación de `X-Trace-Id`.
- `resources/contracts/moderacion-v1.yaml`: contrato OpenAPI completo (no implementado) para
  `POST /ai/moderador`, con referencias a RF-CHT-09 a 14, ADR-003/008/012 y Resilience4j.
- `prompts/{correccion,evaluacion,generacion,moderacion,tutor}/`: solo `tutor/` tiene contenido
  real — refleja el diseño de "cinco funciones de IA" de
  [04 · funciones de IA](../../04-funciones-de-ia.md).
- `GroqAdapter.generar()` devuelve una respuesta simulada si la API key es
  `mock`/`mock-key`/vacía (default en `application.yml`) — evita gasto real.
- `CorsConfig` con `allowedOriginPatterns("*")` — abierto para desarrollo; contradice
  [02 · §6](../../02-arquitectura-y-stack.md) ("no se expone a internet").

## No corresponde a EP-03/EP-04/EP-06

No hay ninguna clase de rúbrica, golden set ni scoring — esas funciones (pese al nombre del
repo) están **ausentes** acá, y en cambio sí están construidas (parcialmente) en `llm-service`.

## Candidatos a portar

- `LlmGateway`/`GroqAdapter` como semilla de [`ep-02/h10.md`](../ep-02/h10.md), reemplazando
  Groq directo por `langchain4j` (ADR-016) y agregando la tabla `función→modelo` que tampoco
  tiene.
- `InputGuard`/`OutputAntiLeakGuard` — testeados (`InputGuardTest`, `OutputAntiLeakGuardTest`),
  base concreta para el guardarraíl de EP-05.
- `contracts/moderacion-v1.yaml` — punto de partida documental para EP-08.
- El patrón `AiRequest<T>`/`AiController` (`POST /ai/{funcion}`) como referencia del contrato
  genérico de [02 · Parte 3](../../02-arquitectura-y-stack.md).
- El enfoque de testing sin gastar dinero (fallback a `mock-key`, exclusión de grupos
  `modelo-real`) — práctica transferible directamente al resto del proyecto.

## Relevancia directa para el hueco de EP-04

`LlmGateway`/`GroqAdapter` es el ejemplo más cercano en todo el repo de cómo cerrar el hueco de
[`ep-04/s03-h01.md`](../ep-04/s03-h01.md) (la calibración que nunca invoca un modelo) — aunque
haría falta adaptarlo a la interfaz que use `llm-service`, no portarlo tal cual.
