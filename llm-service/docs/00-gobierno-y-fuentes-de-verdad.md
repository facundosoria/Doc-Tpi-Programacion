# 00 — Gobierno, fuentes de verdad y términos

## Audiencia y objetivo

**Audiencia:** equipos que integran con Tema 07, Product Owner y quienes mantienen el servicio.

**Objetivo:** saber qué documento decide cada regla y usar los mismos términos al diseñar,
implementar o consumir un contrato.

## Precedencia

| Prioridad | Fuente | Decide |
|---:|---|---|
| 1 | PRD de la plataforma y decisiones de producto aprobadas explícitamente | Alcance funcional y reglas académicas. |
| 2 | Propuesta de arquitectura de la cátedra | Gateway, descubrimiento, identidad, observabilidad y comunicación entre micros. |
| 3 | Contratos canónicos en [`contracts/`](contracts/README.md) y ADR aprobados | Acuerdo técnico ejecutable de Tema 07. |
| 4 | Resto de esta documentación | Explicación, guías, trazabilidad y operación. |

Si dos fuentes del mismo nivel contradicen una regla, se abre una decisión en el registro antes de
publicar o cambiar código.

## Identidad del servicio

| Ámbito | Valor |
|---|---|
| Servicio | `llm-service` |
| Prefijo a través del Gateway | `/api/llm/**` |
| Audience M2M | `llm-service` |
| Correlación HTTP/Kafka | `traceparent` y `X-Request-Id` en headers |

El Gateway es la única puerta HTTP. Los cuerpos no contienen identificadores de trazabilidad.
Cada contrato debe especificar qué identidad delegada recibe del Gateway y qué scope exige.

## Decisión de producto vigente: rúbricas editables

La decisión aprobada por Product Owner reemplaza la regla anterior de pesos o criterios fijos:

- una rúbrica siempre tiene las cinco dimensiones obligatorias;
- un docente puede modificar criterios, anclas, prompts y pesos mientras la versión sea borrador;
- los pesos deben sumar 100 %;
- al publicar, la versión queda inmutable; una modificación posterior crea otra versión.

Esta decisión debe reflejarse en los contratos y documentos que mencionen rúbricas. No se expresa
como una adenda aislada: este es el comportamiento vigente.

## Términos canónicos

| Término | Significado |
|---|---|
| `courseCohortId` | Identificador de la cohorte/oferta concreta. Es el nombre canónico. |
| Rúbrica | Versión de criterios pedagógicos con cinco dimensiones obligatorias. |
| Golden Set | Casos de referencia con puntuación humana para calibrar el evaluador. |
| Calibración activa | Ejecución aprobada aplicada a nuevos desafíos de una cohorte. |
| Score de IA | Resultado sobre el uso del tutor de IA; no es nota académica ni XP. |
| Corrección académica | Validación de una entrega o respuesta; pertenece al servicio dueño del desafío, no a Tema 07. |
