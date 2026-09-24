# Tareas SMART — EP-10 · Personalización y agente (F3)

> Una ficha por historia, en el mismo patrón que [`../../historias/ep-10/`](../../historias/ep-10/README.md).
> Formato del [template oficial de Tarea de la Wiki de Taiga](../../../10-plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)). Cada tarea cuelga en Taiga de su Historia de Usuario bajo EP-10.
>
> **Fuente de verdad:**
> - Épica: [`../../epicas/ep-10.md`](../../epicas/ep-10.md).
> - Fichas padre de HU: [`../../historias/ep-10/`](../../historias/ep-10/README.md).
> - Plan de sprints: [`../../38-plan-de-5-sprints.md`](../../../05-plan-de-cinco-sprints.md) y [`../../sprints/sprint-5/README.md`](../../sprints/sprint-5/README.md).
>
> **Prerrequisitos de DoR (Definition of Ready):**
> - **EP-08 (Moderación):** La respuesta de `@agente` en el chat requiere moderación previa obligatoria (`POST /moderation/v1/decisions` / `RF-CHT-09`).
> - **EP-09 (RAG):** Consulta fundamentada en el material de cátedra de la cohorte con abstención `BLOCKED_NO_SOURCE`.
> - **Contratos externos:** Acuerdos formalizados de DTOs y eventos con `chat-service` (mención y retención `RF-CHT-08`) y `challenges-service` (entrega idempotente).

---

## Índice

| Historia | Archivo | Tareas | h (plan) | Pareja | Estado |
|---|---|---:|---:|:---:|---|
| [LLM-S17-H01](../../historias/ep-10/h01.md) *(Solicitar desafío personalizado)* | [`h01.md`](h01.md) | 7 | 38 | P5+P4 | ⚪ Borrador |
| [LLM-S17-H02](../../historias/ep-10/h02.md) *(Generación durable y no duplicada)* | [`h02.md`](h02.md) | 6 | 32 | P4 | ⚪ Borrador |
| [LLM-S18-H01](../../historias/ep-10/h03.md) *(Mencionar a @agente en el chat)* | [`h03.md`](h03.md) | 6 | 30 | P3+P5 | ⚪ Borrador |
| [LLM-S18-H02](../../historias/ep-10/h04.md) *(Validar personas reales vs bots)* | [`h04.md`](h04.md) | 4 | 16 | P3 | ⚪ Borrador |
| **Total EP-10** | | **23** | **116** | | |

---

## Cómo se generaron

Con el skill [`generar-tareas`](../../../../../../.agents/skills/generar-tareas/SKILL.md): se releen los Criterios de Aceptación (CA) y los escenarios BDD de cada historia padre ([`../../historias/ep-10/`](../../historias/ep-10/README.md)), agrupados según el orden de construcción de cátedra:
1. **Contrato y amenaza:** Especificación OpenAPI, DTOs y validación de esquemas.
2. **Dominio y migración:** Entidades y tablas Flyway para persistencia durable y checkpoints.
3. **Caso de uso:** Orquestación de negocio, RAG de cohorte y abstención explícita.
4. **Adaptadores:** Clientes HTTP/REST hacia `chat-service` y `challenges-service`.
5. **Seguridad y resiliencia:** Guardarraíles, moderación síncrona, control de cuotas, descarte de bots y prevención de bucles.
6. **Observabilidad:** Métricas Micrometer de ejecución, validación y latencia.
7. **Prueba E2E y evidencia:** Tests de integración con Testcontainers verificando todos los escenarios BDD.

Cada tarea representa un paso técnico acotado a **$\le 1$ jornada laboral ($\le 8\text{ h}$)** y suma exactamente la referencia presupuestada de la historia padre en el plan de sprints.
