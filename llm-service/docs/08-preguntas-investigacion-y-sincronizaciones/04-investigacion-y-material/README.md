# 04 — Investigación y material de apoyo

Este material se preserva completo como antecedente técnico, didáctico o de presentación. No es la
fuente normativa para implementar: una decisión vigente se confirma en `00 — Gobierno`, una
integración en `contracts/` y un requisito de calidad en `06 — Operación`.

## Cómo navegar

| Si buscás… | Entrada |
|---|---|
| Especificación técnica con diagramas Mermaid (alcance, arquitectura, seguridad, evaluación, costos, persistencia, ADR, glosario) | [especificacion-tecnica/](especificacion-tecnica/README.md) |
| Planes de ejecución, uno por punto normativo de la Sección 15 del PRD | [planes-de-ejecucion/](planes-de-ejecucion/README.md) |
| Investigación sobre inyección de prompts y jailbreak, a partir de informes de IBM | [investigacion-jailbreak/](investigacion-jailbreak/README.md) |
| Preparación de defensa oral: pregunta → respuesta → por qué sí → por qué no | [fundamentos-defensa-oral/](fundamentos-defensa-oral/) |
| Alcance, coordinación entre equipos, planificación y glosario de integración | [gestion-roadmap-equipo/](gestion-roadmap-equipo/) |
| Rúbrica, anclas, prompts y especificación de funciones/jueces (borrador) | [rubricas-prompts-borrador/](rubricas-prompts-borrador/) |
| Presentaciones y demos visuales | [presentaciones/](presentaciones/README.md) |
| Prototipos del Golden Set | [prototipos/](prototipos/README.md) |
| Propuesta de Sprint 1 | [recomendaciones/s1-propuesto.md](recomendaciones/s1-propuesto.md) |
| Referencias externas y material completo | [referencias/](referencias/README.md) |

Usá estos archivos para entender el origen de una idea, preparar una demo o recuperar una decisión.
Si encontrás una contradicción, no corrijas el antecedente: abrí una corrección sobre el documento
vigente y registrala en [`registro/`](../../registro/README.md).

## Sobre `especificacion-tecnica/`, `planes-de-ejecucion/`, `investigacion-jailbreak/`, `fundamentos-defensa-oral/`, `gestion-roadmap-equipo/` y `rubricas-prompts-borrador/`

Son seis conjuntos de documentos de investigación y preparación propios del equipo, cada uno con su
propio estilo y numeración interna. Varios usan nombres de servicio o de bus de eventos anteriores
a la nomenclatura vigente (por ejemplo `ms-evaluacion-llm` en vez de `llm-service`, o RabbitMQ en
vez de Kafka): esas equivalencias de terminología están anotadas al inicio de cada archivo donde
corresponde. El detalle completo de equivalencias y de las contradicciones detectadas entre este
material y la documentación vigente está registrado en
[`registro/2026-09-22-integracion-material-investigacion.md`](../../registro/2026-09-22-integracion-material-investigacion.md).

`rubricas-prompts-borrador/01_RUBRICA_ANCLAS_Y_PROMPTS_COMPLETOS.md` se declara explícitamente
**BORRADOR**: se conserva como antecedente operativo, no como versión final — la especificación
funcional vigente de Golden Set y calibración permite pesos editables por curso, a diferencia de
los pesos fijos que describe este borrador.
