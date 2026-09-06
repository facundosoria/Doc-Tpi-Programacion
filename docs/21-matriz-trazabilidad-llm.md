# 21 — Matriz de trazabilidad y preparación de desarrollo LLM

> Índice operativo para transformar el PRD y las convenciones en historias, contratos y pruebas.

> **Plan de ejecución:** [23 · Construcción del producto LLM](23-plan-construccion-producto-llm.md)
> distribuye estos requisitos en tres fases y 19 sprints con reuniones presupuestadas. La fase
> sigue determinando cuándo se habilita cada función; documentar F2/F3 no las adelanta al MVP.

| Área / requisito | Fase | Evidencia documental | Dependencia | Prueba de aceptación |
|---|---|---|---|---|
| RF-IA-01/02/04/06/07/19/20 — tutor seguro y registro | MVP | OpenAPI: `POST /tutor/interactions`; [00](00-fuentes-de-verdad-y-convenciones.md) | `practice-service` aporta contexto validado y solución esperada para anti-fuga | No entrega solución; bloquea/regenera salida similar; registra interacción. |
| RF-IA-12 a 18/25 — evaluador, detalle y apelación | MVP | OpenAPI: evaluaciones, apelaciones y overrides | `challenges-service` publica `intento_cerrado.v1` | Cinco dimensiones, confianza, justificación, auditoría append-only y nunca XP. |
| RF-IA-30 a 36 — golden set y calibración | MVP | OpenAPI: golden sets y calibrations; AsyncAPI | Docentes, `courses-service`, `admin-service` | Curso sin calibración aprobada no pasa de draft a activo; sin override. |
| RF-IA-27/34 — cálculo diferido | MVP | AsyncAPI y consulta de pendientes | `challenges-service`, `courses-service` | Entrega aceptada ante caída; evento diferido; cierre bloqueado mientras existan pendientes. |
| RF-IA-22/23/24/35 — cuota y modelos | MVP | OpenAPI: model assignments | `admin-service` | 429 con `Retry-After`; cambio de modelo auditado sin cambio de código. |
| RF-NFR-01/09/10 — retención y auditoría | MVP | [07](07-datos-y-terminos.md) y contratos v1 | T&C y política de plataforma | Sin hard delete académico; auditoría y retención verificables. |
| RF-NFR-03/04 — 120 sesiones y fallas | MVP | [06](06-operacion-e-ingenieria.md) | Gateway, Kafka y proveedores | Carga concurrente y degradación sin bloquear entregas. |
| RF-CHT-09 a 14 — moderación | Fase 2 | Contrato reservado en roadmap | `chat-service` | Se planifica al incorporar chat; no es requisito de release MVP. |
| RF-IA-08 y RF-DES-05 — RAG e IA personalizada | Fase 3 | [04](04-funciones-de-ia.md), roadmap | Cursos y docentes | No se implementa ni expone endpoint durante MVP. |
| Corrector / parciales generados | Fuera de MVP | ADR pendiente | Product Owner | Requiere RF y owner antes de entrar al backlog. |

## Orden de preparación

1. Publicar y acordar los contratos v1 con los cuatro pares.
2. Entregar mocks de estado de calibración y pendientes.
3. Instrumentar la metadata de interacción antes de habilitar tutoría.
4. Entregar golden set y calibración antes del inicio académico.
5. Convertir esta matriz en historias con criterio de aceptación y prueba enlazada.

## Correspondencia con el plan de producto completo

| Cobertura | Incrementos previstos | Dependencia de cierre |
|---|---|---|
| Golden set base y por curso; habilitación del modelo y del curso | S1–S4, F1 | Referencias humanas y bloqueo real en cursos. |
| Tutor seguro, registro e indisponibilidad | S5, F1 | Contexto/solución autorizados de práctica y tratamiento neutro en negocio. |
| Evaluación asíncrona, score diferido y bloqueo de cierre | S6, F1 | Eventos y aplicación de resultados en desafíos/cursos. |
| Apelación, revisión, confianza y auditoría | S7, F1 | Bandeja docente, evidencia e integración de resoluciones. |
| Cambio de modelo, deriva, cuotas y consumo | S8–S9, F1 | Configuración auditada, calibración y alertas. |
| Operación, retención académica, recuperación y carga | S10, F1; regresión en S13/S19 | Recorridos reales y evidencia de recuperación. |
| Moderación, incidentes, revisión y retención del chat | S11–S13, F2 | Chat integrado y política de degradación aprobada. |
| RAG, formatos y ciclo de vida documental | S14–S16, F3 | Material autorizado, fuentes, versiones e índice aislado por cohorte. |
| Desafíos personalizados | S17, F3 | Formatos corregibles y recompensas del motor existente. |
| Agente por mención — RF-CHT-05/08 | S18, F3 | Invocación explícita, moderación y retención de interacción IA. |
| Operación completa del producto | S19, F3 | Funciones de las tres fases integradas y recuperables. |
| Corrector LLM y generación de parciales | Excluidos del plan acordado | Su incorporación requiere cambio de alcance y requisitos aprobados. |

Las pruebas se implementan con cada incremento; los sprints de operación no aplazan la seguridad,
idempotencia ni regresión. Los estados de ejecución y la evidencia se registran en la
[plantilla de sprint](plantillas/sprint-llm.md).
