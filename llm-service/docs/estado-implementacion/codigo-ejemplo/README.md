# `codigo-ejemplo/` — qué era y a qué épica se parecía

> **🟢 2026-09-12 — consolidado y eliminado.** Lo que servía de estos dos proyectos se portó
> (adaptado, no copiado literal) a `llm-service/`: `LlmGateway`/`GroqAdapter` → puerto + fake de
> [`LLM-S01-H10`](../ep-02/h10.md); `InputGuard`/`OutputAntiLeakGuard` → guardarraíles del tutor
> en [`ep-05/interactions.md`](../ep-05/interactions.md). `contracts/moderacion-v1.yaml` se
> preservó como borrador en
> [`docs/contracts/llm-service-v1-moderacion-borrador.yaml`](../../contracts/llm-service-v1-moderacion-borrador.yaml).
> La carpeta `codigo-ejemplo/` ya **no existe** en el repo — este archivo y los dos siguientes
> quedan como registro de qué era cada proyecto y por qué se portó lo que se portó, igual que
> `docs/entregas/decision-605f381.md` documenta una decisión ya aplicada en el código.
>
> Análisis original (previo a la consolidación) de los dos proyectos ajenos importados en
> `codigo-ejemplo/`
> ([[no-tocar-codigo-ajeno]]): qué implementan, a qué épica del [catálogo](../../epicas/README.md)
> se parecen y por qué (evidencia concreta), qué solapa con `llm-service/` (nada, hoy) y qué es
> candidato a portar. Metodología: lectura completa de ambos proyectos (`pom.xml`, config,
> controllers, servicios, entidades, prompts, contratos) cruzada contra `docs/epicas/`,
> [02 · arquitectura y stack](../../02-arquitectura-y-stack.md),
> [04 · funciones de IA](../../04-funciones-de-ia.md) y un grep de `llm-service/src` en busca de
> clases equivalentes. 2026-09-12.

## Resumen

| Proyecto | Qué es | Épica más cercana | Solapa con `llm-service` |
|---|---|---|---|
| [`lara-heredia-demo-llm-spring-ai`](lara-heredia-demo-llm-spring-ai.md) | Demo aislado de un tutor socrático con Spring AI + Groq | EP-05 (parcial — sin guardarraíl de salida) | Ninguno |
| [`ms-evaluacion-llm`](ms-evaluacion-llm.md) | Esqueleto de `llm-service` bajo su nombre anterior, con Tutor + AI Gateway + guardarraíles de entrada y salida | EP-05 (match fuerte), EP-02 (boceto de AI Gateway), EP-08 (contrato de moderación, sin código) | Ninguno |

## El dato más importante para la planificación

**`ms-evaluacion-llm` es el nombre histórico del mismo servicio que hoy es `llm-service`** —
lo aclara explícitamente [02 · línea 3](../../02-arquitectura-y-stack.md): *"La identidad
externa del servicio es `llm-service`... Este documento conserva diagramas previos con
`ms-evaluacion-llm`"*. No es un microservicio distinto: es una **rama de trabajo temprana** que
priorizó primero el Tutor + AI Gateway (M1/M6), mientras que la rama que terminó viviendo en
`llm-service/` priorizó primero Golden Set + Calibración (M3/M4, EP-03/EP-04). Ambas mitades del
producto están definidas en el mismo documento de arquitectura, pero hoy solo una
(golden set/calibración) tiene implementación real en `llm-service`.

**Ninguno de los dos proyectos tiene código que se solape con lo que ya existe en
`llm-service`** — sirven como referencia/prueba de concepto para EP-05, EP-02 y EP-08, que
todavía no tienen código propio (ver [`docs/estado-implementacion/README.md`](../README.md),
tabla de resumen). Todavía no existen `docs/historias/ep-05/` ni `docs/historias/ep-08/`, así
que ninguno de los dos proyectos tiene una historia concreta que lo reclame hoy — son insumo
para cuando esas historias se escriban.

## Candidatos a portar (ver detalle en cada archivo)

- `InputGuard`/`OutputAntiLeakGuard` de `ms-evaluacion-llm` — base testeada para el guardarraíl
  de entrada/salida que EP-05 necesita.
- `LlmGateway`/`GroqAdapter` de `ms-evaluacion-llm` — semilla para cerrar
  [`ep-02/h10.md`](../ep-02/h10.md), aunque habría que reemplazar Groq directo por `langchain4j`
  (ADR-016) y agregarle la tabla `función→modelo` que tampoco tiene.
- `contracts/moderacion-v1.yaml` de `ms-evaluacion-llm` — punto de partida documental para EP-08.
- El patrón `AiRequest<T>`/`POST /ai/{funcion}` de `ms-evaluacion-llm` — implementación de
  referencia del contrato genérico de [02 · Parte 3](../../02-arquitectura-y-stack.md).

## Hallazgo de seguridad (no relacionado con épicas)

`lara-heredia-demo-llm-spring-ai/BE/src/main/resources/application.properties` tiene una **API
key de Groq committeada en texto plano** (`spring.ai.openai.api-key=gsk_...`). No se usó ni
validó esa clave en este análisis; se señala para que se rote/retire del repo.
