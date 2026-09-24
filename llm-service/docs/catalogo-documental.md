# Índice global

Este índice es el mapa de navegación completo. No resume ni duplica los documentos enlazados: cada
enlace lleva al archivo o al índice de la carpeta donde vive el contenido completo.

Esta documentación incluye, decisión por decisión y sin resumir, las 166 directivas de Producto
(D-01 a D-166) sobre skills adjuntables, calibración jerárquica curso/desafío, rúbrica ponderada con
subcriterios, seguridad de skills, cuotas del evaluador, sincronización con
`courses-service`/`challenges-service` y contratos de eventos/notificaciones. La trazabilidad completa
de esas 166 decisiones frente al documento que cada una modificó vive en
[`registro/2026-09-22-skills-y-calibracion-jerarquica.md`](registro/2026-09-22-skills-y-calibracion-jerarquica.md),
[`registro/coverage-matrix-agente-A-skills-calibracion.md`](registro/coverage-matrix-agente-A-skills-calibracion.md)
y [`registro/coverage-matrix-agente-B-cuotas-contratos.md`](registro/coverage-matrix-agente-B-cuotas-contratos.md).

## Recorrido principal

| Orden | Bloque | Cuándo usarlo | Entrada |
|---:|---|---|---|
| 00 | Gobierno y evolución | Antes de decidir, cambiar una regla o resolver una contradicción. | [README](00-gobierno-y-evolucion/README.md) |
| 01 | Visión, alcance y entrega | Para entender el problema, MVP, fases y entregables. | [README](01-vision-alcance-y-entrega/README.md) |
| 02 | Arquitectura y plataforma | Antes de tocar backend, Gateway, despliegue o dependencias. | [README](02-arquitectura-y-plataforma/README.md) |
| 03 | Capacidades de IA | Para tutor, evaluación, modelos, Golden Set, rúbricas y RAG. | [README](03-capacidades-de-ia/README.md) |
| 04 | Seguridad, datos y cumplimiento | Antes de tocar prompts, datos, proveedores, cuotas o guardarraíles. | [README](04-seguridad-datos-y-cumplimiento/README.md) |
| 05 | Contratos | Antes de una comunicación HTTP o Kafka entre microservicios. | [README](contracts/README.md) |
| 06 | Operación, calidad y pruebas | Para implementar, probar, desplegar o revisar evidencia. | [README](06-operacion-calidad-y-pruebas/README.md) |
| 07 | Planificación y trabajo | Para pasar de alcance a épicas, historias, tareas y sprints. | [README](07-planificacion-y-trabajo-equipo/README.md) |
| 08 | Preguntas, investigación y sincronizaciones | Para entender antecedentes, no para sustituir una regla vigente. | [README](08-preguntas-investigacion-y-sincronizaciones/README.md) |
| 09 | Flujo de trabajo | Para ramas, PR, revisión y cierre diario. | [README](09-flujo-de-trabajo-del-equipo/README.md) |

## Índices de segundo nivel

| Necesidad concreta | Índice o documento de entrada |
|---|---|
| Entregables, evidencia y coordinación con otros equipos | [01/03 — Entregas](01-vision-alcance-y-entrega/03-entregas/README.md) |
| Reglas de Gateway, descubrimiento y comunicación micro a micro | [02/06 — Gateway y discovery](02-arquitectura-y-plataforma/06-gateway-y-discovery/README.md) |
| Golden Set, rúbricas editables y calibración | [03 — Golden Set y calibración](03-capacidades-de-ia/golden-set-y-calibracion/README.md) |
| RAG e ingesta de material | [03 — RAG e ingesta](03-capacidades-de-ia/rag-e-ingesta/README.md) |
| Estado real de código frente a lo planificado | [06/04 — Estado de implementación](06-operacion-calidad-y-pruebas/04-estado-de-implementacion/README.md) |
| Épicas, historias, tareas y sprints | [07/09 — Trabajo ejecutable](07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/README.md) |
| Plantillas oficiales de Taiga y sprint | [07/10 — Plantillas](07-planificacion-y-trabajo-equipo/10-plantillas/README.md) |
| Pendientes con cada contraparte | [07/11 — Equipos](07-planificacion-y-trabajo-equipo/11-equipos/README.md) |
| Contratos completos por microservicio/consumidor | [contracts/equipos](contracts/equipos/README.md) |
| Investigación, planes de ejecución, presentaciones, prototipos y referencias de apoyo | [08/04 — Investigación y material](08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/README.md) |
| Correcciones aprobadas | [Registro de cambios](registro/README.md) |
| Skills, subcalibración jerárquica y rúbrica con subcriterios (plan D-01–D-77, D-120–D-161) | [03 — Golden Set y calibración, docs 05 a 09](03-capacidades-de-ia/golden-set-y-calibracion/README.md) y [00 — ADR-021](00-gobierno-y-evolucion/adr/ADR-021-rubrica-jerarquica-dimensiones-extendidas-y-subcriterios-ponderables.md) |
| Seguridad y gobierno de skills (plan D-12–D-20, D-128–D-133) | [04 — Seguridad y gobierno de skills](04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md) |
| Cuotas, límites y evaluador único sin fallback (plan D-27–D-38, D-88–D-91) | [02 — Límites, seguridad de skills y evaluador](02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md) |
| Evaluación diferida, verificación periódica y degradación (plan D-86–D-119, D-162, D-163, D-166) | [06 — Evaluación diferida, verificación y degradación](06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md) |
| Sincronización con cursos/desafíos y avisos de operación (plan D-21–D-26, D-78–D-85, D-95–D-99, D-110–D-112, D-115–D-119, D-164, D-165) | [contracts/03 — Sincronización de calibración y avisos de operación](contracts/03-sincronizacion-calibracion-y-notificaciones.md) |

## Referencias rápidas del MVP

| Tema | Referencia breve | Detalle |
|---|---|---|
| Precedencia y fuentes | [00 — Gobierno y fuentes](00-gobierno-y-fuentes-de-verdad.md) | [00 — Gobierno y evolución](00-gobierno-y-evolucion/README.md) |
| Alcance actual | [01 — Alcance MVP](01-alcance-mvp.md) | [01 — Visión y entrega](01-vision-alcance-y-entrega/README.md) |
| Fronteras del servicio | [02 — Arquitectura y fronteras](02-arquitectura-y-fronteras.md) | [02 — Arquitectura y plataforma](02-arquitectura-y-plataforma/README.md) |
| Ciclos de rúbrica/calibración/evaluación | [03 — Dominio y flujos](03-dominio-y-flujos-mvp.md) | [03 — Capacidades de IA](03-capacidades-de-ia/README.md) |
| Datos sensibles y protección | [04 — Seguridad y datos](04-seguridad-y-datos-sensibles.md) | [04 — Seguridad, datos y cumplimiento](04-seguridad-datos-y-cumplimiento/README.md) |
| Criterios operativos y de pruebas | [05 — Operación y pruebas](05-operacion-y-pruebas.md) | [06 — Operación, calidad y pruebas](06-operacion-calidad-y-pruebas/README.md) |
| Contrato objetivo frente a código actual | [06 — Trazabilidad y estado](06-trazabilidad-y-estado.md) | [Estado de implementación](06-operacion-calidad-y-pruebas/04-estado-de-implementacion/README.md) |
