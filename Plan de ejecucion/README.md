# Plan de ejecución de `llm-service`

Esta carpeta es el **paquete autónomo de construcción** para una persona o agente que llega por primera vez. Contiene la receta de implementación, el detalle de los 19 sprints y una copia local de las fuentes, contratos y referencias necesarias. No contiene todavía el código del producto: los primeros pasos construyen ese código dentro del repositorio.

No hace falta conocer IA, Spring, Docker o Git antes de leerla.

## Qué construimos

`llm-service` es el microservicio que asiste durante desafíos, evalúa el uso de IA, conserva evidencia académica y, más adelante, modera chat, usa material docente y responde menciones en canales.

El servicio no administra cursos, chat, usuarios ni XP. Esos efectos pertenecen a sus servicios dueños. `llm-service` genera resultados, evidencia y eventos.

| Elemento | Valor obligatorio |
|---|---|
| Servicio Eureka | `llm-service` |
| Ruta privada | `/api/llm/**` |
| HTTP entre micros | API Gateway |
| Eventos | Kafka |
| Correlación | `traceparent` y `X-Request-Id` |

Las reglas completas están en [convenciones](../docs/00-fuentes-de-verdad-y-convenciones.md). Los contratos ejecutables están en [OpenAPI](../docs/contracts/llm-service-v1.openapi.yaml) y [AsyncAPI](../docs/contracts/llm-service-v1.asyncapi.yaml).

## Qué contiene esta carpeta

| Ruta | Qué contiene | Cuándo se usa |
|---|---|---|
| `01-fase-1...` a `03-fase-3...` | Objetivo y salida de cada fase | Para entender el producto completo. |
| `06-playbook-de-construccion.md` | Arquitectura, secuencia, DoR/DoD, seguridad y pruebas comunes | Antes de implementar cualquier historia. |
| `07-backlog-ejecutable-sprints.md` | Receta atómica S1–S19, horas, gates y aceptación | Durante la Planning y ejecución. |
| `04-docker-y-pruebas.md` | Arranque, perfiles, comandos y diagnóstico | Antes de tocar código y en cada demo. |
| `05-flujo-diario-y-git.md` | Rutina Scrum, ramas, PR y evidencias | Durante el trabajo diario. |
| `referencia/docs-vigentes/` | Copia de las fuentes normativas del producto | Resolver requisitos y decisiones. |
| `referencia/contratos/` | Copia local de OpenAPI y AsyncAPI v1 | Implementar/probar integraciones. |
| `referencia/gateway-y-discovery/` | Gateway, Eureka, seguridad y comunicación | Implementar red e integración. |
| `referencia/antecedentes/` | Material importado e histórico, no normativo | Investigar; nunca usarlo para contradecir v1. |

Las copias bajo `referencia/` son un snapshot para trabajar desde esta carpeta. Si la fuente en
`docs/` cambia, comparar ambas y actualizar el snapshot mediante un PR; los contratos vigentes y
`docs/00-fuentes-de-verdad-y-convenciones.md` siempre prevalecen sobre una copia desactualizada.

## Lectura inicial y orden exacto

Seguí este orden. No hace falta leer toda la carpeta `docs/` de una vez.

1. `referencia/docs-vigentes/00-fuentes-de-verdad-y-convenciones.md`: autoridad y nomenclatura.
2. `referencia/docs-vigentes/01-problema-y-alcance.md` y `02-arquitectura-y-stack.md`: qué entra y con qué tecnologías.
3. `referencia/contratos/`: endpoints, eventos, headers y schemas que no se pueden inventar.
4. [Playbook de construcción](06-playbook-de-construccion.md): reglas que todos los agentes deben cumplir.
5. [Backlog ejecutable](07-backlog-ejecutable-sprints.md): tomar solo el sprint actualmente habilitado.
6. [Docker y pruebas](04-docker-y-pruebas.md): preparar el ambiente y ejecutar la evidencia.
7. [Instructivos APB de entorno y Docker](08-instructivos-apb-docker-env.md): `.env`, credenciales parametrizadas, Dockerfile, redes, perfiles y diagnóstico.
8. [Flujo diario y Git](05-flujo-diario-y-git.md): coordinar cambios y entregar PR.
9. El documento de fase correspondiente y su matriz en `referencia/docs-vigentes/21-matriz-trazabilidad-llm.md`.

No saltear el backlog para “adelantar” RAG, moderación o un proveedor real: los gates evitan que
un agente implemente una capacidad sin contrato, permisos, datos o servicio consumidor disponible.

| Si trabajás en… | Leé además |
|---|---|
| Tutor, evaluación o calibración | [Fase 1](01-fase-1-mvp-academico.md), [seguridad](../docs/05-seguridad.md), [rúbrica](../docs/13-rubrica-y-prompts.md). |
| Moderación | [Fase 2](02-fase-2-moderacion.md) y [funciones de IA](../docs/04-funciones-de-ia.md). |
| RAG y documentos | [Fase 3](03-fase-3-rag-y-personalizacion.md) y [almacenamiento](../docs/12-almacenamiento-e-ingesta.md). |
| Gateway o identidad | [Gateway y Discovery](../docs/gateway-y-discovery/README.md). |

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 + Spring Boot | API, reglas de negocio, workers y adaptadores. |
| Maven Wrapper | Compilar y ejecutar pruebas sin instalar Maven globalmente. |
| PostgreSQL + Flyway | Datos académicos, configuración y trabajos persistentes. |
| Docker Compose | Dependencias locales reproducibles. |
| API Gateway + Eureka | Entrada única, seguridad y descubrimiento. |
| Kafka | Eventos entre servicios. |
| Resilience4j | Timeouts, cuotas, reintentos y circuit breaker. |
| JUnit, Mockito, Testcontainers, WireMock | Pruebas unitarias, de integración y contrato. |
| Angular compartido | Interfaces de golden set, tutor, apelaciones y operación. |
| pgvector, ONNX, PDFBox, Tika, Tess4J, MinIO | RAG e ingesta de Fase 3. |

Ningún controller, worker o frontend llama directamente a un proveedor LLM: todo pasa por el AI Gateway interno.

## Requisitos locales

Verificá estas herramientas antes de tomar una historia técnica:

```bash
git --version
docker --version
docker compose version
java -version
```

Se espera Java 21 y un entorno que provea `docker compose`. El Maven Wrapper evita instalar Maven manualmente. Si un comando falla, resolvelo antes de iniciar una tarea o registrá el bloqueo para P1.

## Estado actual y advertencia importante

`codigo-ejemplo/ms-evaluacion-llm/` y `demo/docker-compose.yml` son un laboratorio histórico. Usan H2, `ms-evaluacion-llm`, `/ai/*` y contratos anteriores. Sirven para explorar guardarraíles; no son el producto ni el ambiente de integración final.

La primera entrega crea el servicio definitivo y su entorno. No copies rutas históricas, `trace_id` ni claves de API de la demo al servicio nuevo.

## Cómo construir desde cero

1. Crear una rama `feature/s1-bootstrap` según el flujo Git.
2. Completar la primera Planning con la plantilla de [plantillas/sprint-llm.md](plantillas/sprint-llm.md): disponibilidad real, reuniones (90 h iniciales), reserva y dependencias.
3. Leer S1 en `07-backlog-ejecutable-sprints.md`; comprobar su “No iniciar sin”.
4. Implementar los paquetes en el orden numerado: contrato/amenaza → dominio/migración → caso de uso → adaptadores → seguridad/resiliencia → observabilidad → pruebas → demo.
5. Crear la aplicación definitiva, Dockerfile, Compose, `.env.example`, Flyway y healthchecks durante S1. Hasta entonces, solo la demo histórica es ejecutable.
6. Ejecutar la DoD del [playbook](06-playbook-de-construccion.md), abrir PR y registrar evidencia de CI y Docker.
7. Hacer Review con la demo funcional. Solo si el gate pasa, iniciar el siguiente sprint.
8. Repetir para S2–S10, cerrar la salida de F1 y recién entonces habilitar S11; repetir gates para F2 y F3.

### Dependencias de fase

```text
S1 → S2 → S3 → S4 → S5 → S6 → S7 → S8 → S9 → S10 (salida F1)
                                      └──────────────→ S11 → S12 → S13 (salida F2)
S14 (RAG) requiere S5 + almacenamiento autorizado → S15 → S16 → S17 → S18 → S19
```

S11 requiere además contrato de `chat-service`; S14 requiere material/ownership y pgvector;
S17 requiere formatos corregibles de `challenges-service`; S18 requiere chat, moderación y RAG.
Una dependencia externa bloqueada se registra con dueño y fecha: no se reemplaza silenciosamente
por una implementación ficticia.

## Cómo usar el plan

Cada sprint dura dos semanas y termina en un recorrido funcional. La capacidad inicial es 350 horas-persona nominales, menos 90 horas de reuniones y 52 de reserva: **208 horas para entregables**.

Antes de cada Sprint Planning, completar una copia de la [plantilla de sprint](../docs/plantillas/sprint-llm.md). Allí se registran disponibilidad, reuniones, historias, dependencias, Review con demo y retro.

| Documento | Propósito |
|---|---|
| [01 — Fase 1](01-fase-1-mvp-academico.md) | S1–S10: MVP académico. |
| [02 — Fase 2](02-fase-2-moderacion.md) | S11–S13: moderación integrada. |
| [03 — Fase 3](03-fase-3-rag-y-personalizacion.md) | S14–S19: RAG, personalización y agentes. |
| [04 — Docker y pruebas](04-docker-y-pruebas.md) | Levantar, verificar, depurar y detener entornos. |
| [05 — Flujo diario y Git](05-flujo-diario-y-git.md) | Trabajo cotidiano, ramas y Pull Requests. |
| [06 — Playbook de construcción](06-playbook-de-construccion.md) | Reglas obligatorias para agentes: arquitectura, contratos, seguridad, DoR/DoD y evidencia. |
| [07 — Backlog ejecutable](07-backlog-ejecutable-sprints.md) | Receta detallada de construcción para los 19 sprints. |
| [08 — Instructivos APB de entorno y Docker](08-instructivos-apb-docker-env.md) | `.env`, credenciales sin hardcodear, Dockerfile, redes, perfiles y diagnóstico. |

Si una decisión contradice el PRD, las convenciones o los contratos, detené el cambio y registrá la discrepancia. El orden de precedencia está en [00](../docs/00-fuentes-de-verdad-y-convenciones.md).

Para trabajar exclusivamente desde esta carpeta, usar como referencias equivalentes:
`referencia/docs-vigentes/00-fuentes-de-verdad-y-convenciones.md` y
`referencia/contratos/llm-service-v1.openapi.yaml` / `llm-service-v1.asyncapi.yaml`.
