# `codigo-ejemplo/lara-heredia-demo-llm-spring-ai/BE`

> **🟢 2026-09-12 — carpeta eliminada tras la consolidación.** Nada de este proyecto se portó
> (su guardarraíl era más débil que el de `ms-evaluacion-llm` y su prompt ya estaba duplicado
> ahí) — ver [`codigo-ejemplo/README.md`](README.md). Queda el análisis original como registro.

- **Épica más cercana:** EP-05 · Tutor seguro y guardarraíles — parcial
- **Solapamiento con `llm-service`:** ninguno

## Qué es

Demo monolítico y aislado de un "tutor socrático" con Spring AI. Un único controller REST,
`ChatController` (`.../controller/ChatController.java`), bajo `/api/conversaciones`: crear
conversación, listar conversaciones, listar mensajes, enviar mensaje del alumno (dispara la
respuesta del tutor). La lógica vive en `TutorSocraticoService`
(`.../service/TutorSocraticoService.java`): arma un prompt de sistema fijo ("tutor socrático que
nunca da código resuelto"), concatena el histórico, y llama al modelo vía `ChatClient` de
**Spring AI** (`spring-ai-starter-model-openai` 2.0.1, `GroqChatClientConfig`) apuntando a la API
de Groq como endpoint OpenAI-compatible. Persiste `Conversacion`/`Mensaje` (JPA + H2 en memoria).
Documentado con Swagger/OpenAPI. Sin autenticación, sin tests de negocio (solo el smoke test por
defecto).

## Por qué se parece a EP-05

- `TutorSocraticoService.responder()` implementa "alumno pregunta → tutor guía sin resolver",
  el objetivo textual de EP-05.
- `esJailbreak()` (lista `JAILBREAK_KEYWORDS` + `contains()`) es una forma mínima del
  guardarraíl de entrada (RF-IA-05/06/07/10).
- El `try/catch` de `resolverPregunta()` que degrada si Groq no responde es una versión ingenua
  de "si el tutor no está disponible, el alumno sigue su intento" (criterio de aceptación de
  EP-05).

## Qué le falta para cumplir EP-05

- Sin guardarraíl de **salida** anti-fuga (comparar contra la solución esperada) — eso sí existe
  en [`ms-evaluacion-llm`](ms-evaluacion-llm.md).
- Sin `curso_cohorte_id`/`usuario_ref`/`desafio_id` en las entidades — viola la regla de
  [02 · §7](../../02-arquitectura-y-stack.md) ("casi ninguna entidad existe fuera de un
  curso-cohorte").
- Sin cuota por alumno (RF-IA-22), sin `trace_id`, sin registro de incidentes de seguridad.

## No corresponde a EP-02 (AI Gateway)

Llama al modelo directo vía `ChatClient` de Spring AI, modelo hardcodeado en
`application.properties`, sin capa función→modelo editable, sin adapter agnóstico de proveedor —
contradice RF-IA-11 y el ADR-016 (usar `langchain4j`, citado en
[02 · §4](../../02-arquitectura-y-stack.md)).

## Candidatos a portar

- El **texto del system prompt** de `construirPrompt()` — prácticamente idéntico al de
  `ms-evaluacion-llm/src/main/resources/prompts/tutor/system-v1.txt`, sugiere que ambos
  proyectos comparten origen de prompt.
- La detección de jailbreak es un placeholder razonable, pero la de `ms-evaluacion-llm`
  (normalización de diacríticos) es superior — preferirla como base si se porta algo.
- **No portar la integración Spring AI/`ChatClient` tal cual**: el stack documentado usa
  `langchain4j` (ADR-016) y el patrón AI Gateway con tabla función→modelo, que este proyecto no
  implementa.

## Hallazgo de seguridad

`src/main/resources/application.properties` línea 9 tiene una **API key de Groq en texto plano
committeada** (`spring.ai.openai.api-key=gsk_...`). No se usó ni validó; señalada para
rotar/retirar del repo.
