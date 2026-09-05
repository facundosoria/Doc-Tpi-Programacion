# 22 — Informe comparativo: documentación anterior vs. alineada

**Fecha:** 2026-09-04  
**Ámbito:** documentación activa del repositorio para el equipo LLM.  
**No abarca:** `docs/importado/`, presentaciones, ni implementación Java/configuración ejecutable. Esos materiales no se modificaron como parte de la alineación documental.

## Resumen ejecutivo

La documentación anterior ya describía con profundidad una propuesta de IA para tutoría, evaluación y operaciones. Sin embargo, convivían convenciones técnicas paralelas: nombres como `ms-evaluacion-llm` o `ai-service`, rutas bajo `/ai`, trazabilidad con `trace_id`, contratos narrativos y límites de fase poco uniformes.

La documentación alineada conserva ese análisis como antecedente, pero establece un contrato vigente único: `tpi-llm` / `llm-service`, exposición privada bajo `/api/llm/**`, integración de servicios mediante Gateway, eventos compartidos en Kafka, trazabilidad estándar y contratos v1 verificables. La prioridad de decisión queda explícita: PRD, luego convenciones de `idea.pptx.pdf`, luego la documentación alineada.

Este resultado es una **migración de documentación**, no una migración de código. El esqueleto de ejemplo conserva convenciones antiguas y deberá adaptarse durante la implementación.

## Fuentes comparadas y criterio de verdad

| Orden | Fuente | Uso actual |
|---|---|---|
| 1 | `PRD-Plataforma-Gamificada-TP.pdf` | Define alcance funcional, MVP y fases. |
| 2 | `idea.pptx.pdf` | Define arquitectura transversal obligatoria: Gateway, Eureka, seguridad, rutas, nombres y pruebas. |
| 3 | `docs/00-fuentes-de-verdad-y-convenciones.md` | Traduce ambas fuentes al perímetro del equipo LLM. |
| 4 | OpenAPI/AsyncAPI v1 y matriz de trazabilidad | Son los contratos operativos y de planificación del equipo. |
| 5 | Documentación previa y ejemplos | Se mantiene como análisis o antecedente cuando contradice una fuente superior. |

## Comparación de cambios relevantes

| Tema | Documentación anterior | Documentación alineada | Efecto para el desarrollo |
|---|---|---|---|
| Jerarquía documental | El PRD, el análisis LLM y las convenciones podían consultarse en paralelo sin una regla de desempate explícita. | Se incorpora una jerarquía formal en `00-fuentes-de-verdad-y-convenciones.md`. | Toda decisión nueva debe comprobarse primero contra PRD y `idea.pptx.pdf`. |
| Identidad del componente | Aparecen `ms-evaluacion-llm`, `ai-service` y denominaciones históricas. | Repositorio `tpi-llm`; servicio canónico `llm-service`. | Configuración, artefactos, discovery, audiencias y documentación futura deben usar `llm-service`. |
| Ruta externa | Se describen rutas `/ai/*`, conversaciones y despachadores genéricos. | El prefijo canónico es `/api/llm/**`; la reserva pública es `/api/llm/public/**`. | Los controladores y el Gateway deberán abandonar `/ai/*`; no se incorporan rutas públicas sin necesidad del PRD. |
| Comunicación entre servicios | Había llamadas y contratos por consumidor, sin una delimitación única de quién invoca cada operación. | Los servicios dueños (`practice-service`, `challenges-service`, `courses-service`, `admin-service`) llaman al LLM mediante Gateway. | El frontend no invoca directamente al LLM; cada operación identifica al dueño y su scope M2M. |
| Seguridad | Se mezclaban autenticación, autorización y usos de token sin una convención completa. | Gateway autentica y propaga identidad; el microservicio autoriza por dominio. M2M usa `type=service`, `aud=llm-service` y scopes. | Se deben validar cabeceras de contexto, audiencia y scope; nunca aceptar cabeceras externas sin saneamiento del Gateway. |
| Descubrimiento y enrutamiento | El material de ejemplo no reflejaba de forma consistente el perímetro Tema 07. | Gateway es el ingreso único y las llamadas interservicio pasan por él; Eureka es sólo descubrimiento. | No se implementarán accesos directos entre microservicios ni se usará Eureka como canal de negocio. |
| API HTTP | Predominaban endpoints narrativos o un dispatcher/conversación genérica. | Se publica `llm-service-v1.openapi.yaml`, con recursos de tutoría, evaluaciones, apelaciones, golden set, calibración, trabajos y asignación de modelos. | El contrato HTTP se implementa desde OpenAPI v1 y no desde los ejemplos históricos. |
| Eventos | Había referencias a colas/buses y a asincronía sin contrato transversal único. | Se fija Kafka y `llm-service-v1.asyncapi.yaml`: consumo de `intento_cerrado.v1`, archivado de curso y cambio de modelo; publicación de scores, calibraciones e incidentes. | La integración por eventos parte de AsyncAPI; la cola interna de workers, si existe, no reemplaza Kafka compartido. |
| Trazabilidad y errores | Se utilizaban variantes como `trace_id` y `X-Trace-ID`, con formatos de error no uniformes. | Se normalizan `traceparent` y `X-Request-Id`; errores HTTP según Problem Details (RFC 7807). | Modelos, logs, eventos y respuestas deberán transportar correlación estándar. |
| Idempotencia | La necesidad se encontraba dispersa en flujos de evaluación o jobs. | OpenAPI v1 exige `Idempotency-Key` para operaciones de creación o cambio sensibles. | Los comandos y consumidores deben diseñarse para reintentos seguros. |
| Fases funcionales | RAG, ingesta, generación, corrector y moderación podían aparecer próximos al MVP en documentos de diseño. | El alcance se reordena según el PRD: tutor, evaluación, golden set, calibración y apelaciones en MVP; chat/moderación en F2; RAG/ingesta/reto personalizado/agentes en F3. Generación/corrector se difieren por no tener RF explícito. | El backlog inicial no debe comprometer funcionalidades de F2/F3 ni inferir requisitos faltantes. |
| Validación de contratos | La mayoría de los acuerdos eran narrativos. | Se agregan OpenAPI, AsyncAPI y una matriz RF→contrato→dependencia→prueba de aceptación. | Los contratos pueden revisarse y validarse antes de construir los adaptadores. |
| Estado del código de ejemplo | El README del ejemplo podía leerse como guía directa de implementación. | Se lo identifica explícitamente como prealineación/histórico. | El código no es fuente de verdad y requiere una migración técnica posterior. |

## Cambios por activo documental

| Activo | Cambio introducido | Estado anterior que reemplaza o aclara |
|---|---|---|
| `README.md` | Añade el estado de alineación, las fuentes de verdad y accesos a los contratos actuales. | El índice no distinguía con claridad entre documentos vigentes, históricos y contratos ejecutables. |
| `docs/00-fuentes-de-verdad-y-convenciones.md` | Nuevo marco obligatorio de identidad, rutas, Gateway, seguridad, Kafka, fases y servicios pares. | No existía una traducción central de las convenciones de equipo al equipo LLM. |
| `docs/21-matriz-trazabilidad-llm.md` | Nueva matriz de requisitos, fase, contratos, dependencia y criterio de aceptación. | La cobertura de RF y la priorización se encontraban repartidas en varios documentos. |
| `docs/contracts/llm-service-v1.openapi.yaml` | Nuevo contrato HTTP v1, con autenticación, idempotencia, recursos y Problem Details. | Los endpoints previos eran narrativos o respondían a la estructura histórica `/ai`. |
| `docs/contracts/llm-service-v1.asyncapi.yaml` | Nuevo contrato Kafka v1, con tópicos, envelope y correlación. | La mensajería no tenía un contrato de intercambio compartido verificable. |
| `docs/17-mapa-de-integracion.md` | Explicita los consumidores dueños, HTTP vía Gateway y eventos Kafka; conserva lo anterior como histórico. | El mapa anterior exponía integraciones y endpoints ya no canónicos. |
| `docs/18-contratos-inter-equipos.md` | Actualiza pares, scopes, rutas y reglas de correlación; remite a los contratos v1. | Los acuerdos previos mezclaban rutas, seguridad y tecnologías de mensajería. |
| `docs/01` a `15`, `19` y `20` | Se agregan avisos de alineación en alcance, modelo, perímetro, colas, trazas, RAG, streaming y backlog. | Los textos detallados siguen siendo útiles, pero podían sugerir decisiones superadas. |
| `docs/gateway-y-discovery/README.md` | Agrega una aplicación concreta de Tema 07 para `llm-service`. | El material era transversal y no establecía el caso LLM. |
| `docs/informe-analisis-tema07-convenciones.md` | Enlaza las conclusiones del audit con los documentos y contratos vigentes. | El informe de auditoría señalaba hallazgos, pero no apuntaba al resultado de la alineación. |
| `codigo-ejemplo/**.md` | Señala que nombre, rutas y trazas del ejemplo son prealineación. | Podía inducir a copiar directamente la estructura antigua. |

## Contrato vigente resultante

### Identidad, acceso y trazabilidad

| Aspecto | Convención vigente |
|---|---|
| Repositorio / servicio | `tpi-llm` / `llm-service` |
| Prefijo externo | `/api/llm/**` |
| Ruta pública reservada | `/api/llm/public/**` |
| Acceso de otro microservicio | Siempre por API Gateway, con token M2M de audiencia `llm-service`. |
| Identidad propagada | `X-Principal-Type`, identidad/roles o servicio/scopes y, si corresponde, `X-Delegated-User`. |
| Correlación | `traceparent` y `X-Request-Id`, propagados en HTTP, logs y eventos. |
| Errores | Problem Details RFC 7807. |

### Responsabilidades por servicio par

| Servicio dueño | Responsabilidad frente a LLM | Ejemplo de scope |
|---|---|---|
| `practice-service` | Solicitar tutoría contextual. | `llm.tutor.invoke` |
| `challenges-service` | Consultar resultado de evaluación y gestionar apelación. | `llm.evaluation.read` |
| `courses-service` | Consultar calibración y evaluaciones pendientes. | `llm.calibration.read` |
| `admin-service` | Gestionar golden set, calibración, modelos y overrides. | `llm.golden-set.write` |

Los detalles de operaciones, cuerpos y respuestas están en [OpenAPI v1](contracts/llm-service-v1.openapi.yaml); la integración asíncrona se encuentra en [AsyncAPI v1](contracts/llm-service-v1.asyncapi.yaml).

## Elementos que deliberadamente no cambiaron

| Elemento | Motivo | Consecuencia |
|---|---|---|
| Código Java, controladores y configuración de ejemplo | El encargo fue alinear documentación y preparar la planificación. | Aún pueden contener `ms-evaluacion-llm`, rutas `/ai`, `trace_id` o supuestos históricos. |
| `docs/importado/` | Se decidió preservarlo como material importado. | No debe usarse como contrato vigente sin contrastarlo con las fuentes de verdad. |
| Demos y presentaciones | Están fuera del alcance documental activo acordado. | Pueden requerir actualización visual o técnica en una tarea específica. |
| Integraciones reales de pares y proveedores | Requieren coordinación e implementación de otros equipos. | Los contratos v1 fijan el objetivo, pero no prueban todavía interoperabilidad de ejecución. |

## Brecha entre documentación actual y futura implementación

La alineación reduce la ambigüedad de diseño, pero deja una migración técnica explícita. Antes de considerar `llm-service` implementado deben planificarse, como mínimo:

1. Renombrar identidad técnica, configuración de Eureka y rutas del Gateway hacia `llm-service` y `/api/llm/**`.
2. Reemplazar endpoints históricos `/ai/*` por los recursos definidos en OpenAPI v1.
3. Implementar validación del contexto propagado por Gateway, autorización por dominio, tokens M2M, audiencia y scopes.
4. Adoptar `traceparent`, `X-Request-Id` y Problem Details en respuestas, logs, persistencia y eventos; planificar la transición desde campos `trace_id` existentes.
5. Implementar consumidores y productores Kafka conforme a AsyncAPI, incluyendo correlación, deduplicación e idempotencia.
6. Resolver con los equipos pares los esquemas finales de payload, ownership de datos y ambientes de prueba de integración.
7. Construir el MVP sólo con los RF priorizados; dejar F2/F3 y generación/corrector como backlog condicionado a requisitos aprobados.

## Verificación realizada sobre la alineación documental

| Verificación | Resultado |
|---|---|
| Consistencia superficial de parches Markdown/YAML (`git diff --check`) | Sin errores de espacios ni marcadores inválidos. |
| Carga sintáctica de OpenAPI y AsyncAPI con parser YAML | Ambos archivos se leen correctamente como YAML. |
| Presencia de identidad, rutas y contratos canónicos | Confirmada en la guía de fuentes de verdad y en los dos contratos v1. |
| Pruebas de aplicación o integración | No aplican: no se modificó código ejecutable. |

## Estado para iniciar planificación

| Área | Estado | Lectura |
|---|---|---|
| Alcance y convenciones documentales | Verde | Existe una fuente de verdad explícita y un contrato de equipo LLM alineado. |
| Contratos de interfaz | Verde | OpenAPI y AsyncAPI v1 permiten descomponer historias e integraciones. |
| Compatibilidad del código de ejemplo | Ámbar | Está documentado como histórico; todavía no fue migrado. |
| Dependencias de otros equipos y proveedores | Ámbar | Los dueños y scopes están definidos, pero falta acordar/poblar integraciones reales. |
| Funciones F2/F3 y requisitos no explícitos | Diferido | No deben incluirse en el primer incremento sin una decisión posterior. |

La planificación de desarrollo debe tomar como punto de partida [la guía de fuentes de verdad](00-fuentes-de-verdad-y-convenciones.md), [la matriz de trazabilidad](21-matriz-trazabilidad-llm.md) y los contratos v1, no el código de ejemplo ni las rutas históricas.
